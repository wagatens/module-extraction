package qbf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import loader.OntologyLoader;


import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;


import convertors.ALCtoPropositionalConvertor;
import convertors.ClauseSettoSATClauses;
import convertors.OWLOntologyToClauseSet;

import formula.PropositionalFormula;


import satclauses.ClauseSet;
import satclauses.NumberMap;
import signature.SignatureGenerator;
import util.ModulePaths;
import util.ModuleUtils;

public class QBFFileWriter {

	/* Factories and convertors */
	private static 	OWLDataFactory factory = OWLManager.getOWLDataFactory();
	private static ALCtoPropositionalConvertor convertor = new ALCtoPropositionalConvertor();
	/* Caches ALC->Clause conversion so need to remain static */
	private static OWLOntologyToClauseSet ontologyConvertor = new OWLOntologyToClauseSet();

	/* File writing structures */
	private ArrayList<String> toWrite;
	static String FILE_TO_WRITE;

	/*OWL Structures*/
	private Set<OWLLogicalAxiom> ontology;
	private HashSet<OWLEntity> classesNotInSignature;
	private Set<OWLClass> signature;

	/*QBF Structures */
	private ClauseSet ontologyAsClauseSet;
	private ClauseSettoSATClauses clauseSetConvertor;
	private NumberMap numberMap;
	private IVec<IVecInt> clauses;

	public QBFFileWriter(Set<OWLLogicalAxiom> ontology, Set<OWLClass> signature) {
		FILE_TO_WRITE =  ModulePaths.getQBFSolverLocation() + "Files/qbf" + System.currentTimeMillis() + ".qdimacs";
		this.toWrite = new ArrayList<String>();
		this.ontology = ontology;
		this.signature = signature;
		this.classesNotInSignature = new HashSet<OWLEntity>();

		convertOntologyToQBFClauses();
		populateSignatures();
	}

	private void convertOntologyToQBFClauses(){
		this.ontologyAsClauseSet = ontologyConvertor.convertOntology(ontology);
		this.clauseSetConvertor = new ClauseSettoSATClauses(ontologyAsClauseSet);
		this.numberMap = clauseSetConvertor.getNumberMap();
		this.clauses = new ClauseSettoSATClauses(ontologyAsClauseSet).convert();
	}

	private void populateSignatures() {
		classesNotInSignature.addAll(ModuleUtils.getEntitiesInSet(ontology));
		classesNotInSignature.removeAll(signature);

		/* Remove Top and Bottom classes */
		classesNotInSignature.remove(factory.getOWLThing());
		classesNotInSignature.remove(factory.getOWLNothing());
	}

	public File generateQBFProblem() throws IOException{
		return createQBFFile(createStringsToWrite());
	}

	private void writeHeaders() {
		//toWrite.add("c " + numberMap + "\n");
		toWrite.add("p cnf " + ontologyAsClauseSet.getVariables().size() + " " + ontologyAsClauseSet.getClauses().size() + "\n");
	}

	private List<String> createStringsToWrite(){
		writeHeaders();
		writeUniversalQuantifiers();
		writeExistentialQuantifiers();
		writeClauses();

		/* Clear possibly large number map */
		numberMap.clear();
		return toWrite;
	}



	private File createQBFFile(List<String> list){
		File file = new File(FILE_TO_WRITE);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file.getAbsoluteFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedWriter writer = new BufferedWriter(fileWriter);

		try{
			for(String s : list){
				writer.write(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try{
				fileWriter.flush();
				writer.flush();
				fileWriter.close();
				writer.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return file;
	}


	private void writeUniversalQuantifiers() {
		if(!signature.isEmpty()){
			toWrite.add("a ");
			for(OWLClass cls : signature){
				PropositionalFormula clsAsVar = convertor.visit(cls);
				toWrite.add(numberMap.get(clsAsVar) + " ");
			}
			toWrite.add("0\n");
		}
	}

	private void writeExistentialQuantifiers() {
		if(!classesNotInSignature.isEmpty()){
			toWrite.add("e ");
			for(OWLEntity ent : classesNotInSignature){
				PropositionalFormula clsAsVar = convertor.convert(ent);
				toWrite.add(numberMap.get(clsAsVar) + " ");
			}
			toWrite.add("0\n");
		}
	}

	private void writeClauses() {
		Iterator<IVecInt> vectorsIterator = clauses.iterator();

		while(vectorsIterator.hasNext()){
			IteratorInt intIterator =  vectorsIterator.next().iterator();
			while(intIterator.hasNext())
				toWrite.add(intIterator.next() + " ");

			toWrite.add("0" + "\n");
		}
	}


	public static void main(String[] args) {
		OWLOntology ont = OntologyLoader.loadOntology("/home/william/PhD/Ontologies/NCI/expr/nci-08.09d-terminology.owl");
		//OWLOntology ont = OntologyLoader.loadOntology("/home/william/PhD/Ontologies/interp/diff.krss");

		for(int i=0; i<=10; i++){
			Set<OWLLogicalAxiom> smaller = ModuleUtils.generateRandomAxioms(ont.getLogicalAxioms(), 200);
			Set<OWLClass> sig = new SignatureGenerator(smaller).generateRandomClassSignature(30);

			try {
				System.out.println(new QBFSolver().isSatisfiable(new QBFFileWriter(smaller, sig).generateQBFProblem()));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (QBFSolverException e) {
				e.printStackTrace();
			}
		}

	}

}