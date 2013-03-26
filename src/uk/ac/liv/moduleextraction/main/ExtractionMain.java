package uk.ac.liv.moduleextraction.main;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.liv.moduleextraction.extractor.SyntacticFirstModuleExtraction;
import uk.ac.liv.moduleextraction.qbf.QBFSolverException;
import uk.ac.liv.moduleextraction.signature.SigManager;
import uk.ac.liv.moduleextraction.signature.SignatureGenerator;
import uk.ac.liv.moduleextraction.util.ModulePaths;
import uk.ac.liv.moduleextraction.util.ModuleUtils;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

public class ExtractionMain {

	@SuppressWarnings("unused")
	public static void main(String[] args) {


		//OWLOntology ont = OntologyLoader.loadOntology("/LOCAL/wgatens/Ontologies/Bioportal/NOTEL/Terminologies/Acyclic/Big/LiPrO-converted");
		OWLOntology ont = OntologyLoader.loadOntology(ModulePaths.getOntologyLocation() + "moduletest/chaintest1.krss");
		System.out.println("Loaded Ontology");

		//System.out.println(ont);

		SignatureGenerator gen = new SignatureGenerator(ont.getLogicalAxioms());
		SigManager sigManager = new SigManager(new File(ModulePaths.getSignatureLocation() + "/insepSigs"));

		OWLDataFactory f = OWLManager.getOWLDataFactory();
		OWLClass a = f.getOWLClass(IRI.create(ont.getOntologyID() + "#A"));
		OWLClass b = f.getOWLClass(IRI.create(ont.getOntologyID() + "#B"));

		Set<OWLEntity> signature = new HashSet<OWLEntity>();
		signature.add(a);
		signature.add(b);
		
		//sigManager.readFile()
		
		
		Set<OWLEntity> sig = signature;

		SyntacticLocalityModuleExtractor syntaxModExtractor = 
				new SyntacticLocalityModuleExtractor(OWLManager.createOWLOntologyManager(), ont, ModuleType.STAR);
		Set<OWLLogicalAxiom> starModule = ModuleUtils.getLogicalAxioms(syntaxModExtractor.extract(sig));

		int starSize = starModule.size();


		Set<OWLLogicalAxiom> syntfirstExtracted = null;
		System.out.println("|Signature|: " + sig);

		try {
			long startTime = System.currentTimeMillis();
			SyntacticFirstModuleExtraction syntmod = new SyntacticFirstModuleExtraction(ont.getLogicalAxioms(), sig);
			syntfirstExtracted = syntmod.extractModule();
			System.out.println("Time taken: " + ModuleUtils.getTimeAsHMS(System.currentTimeMillis() - startTime));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (QBFSolverException e) {
			e.printStackTrace();
		}

		System.out.println("Star module size: " + starSize);
		System.out.println("Synsize: " + syntfirstExtracted.size());
		//System.out.println("QBF Checks " + InseperableChecker.getTestCount());
		System.out.println();
	}

}