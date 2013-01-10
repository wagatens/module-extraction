package uk.ac.liv.moduleextraction.signature;


import java.util.Set;


import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.liv.moduleextraction.checkers.DefinitorialDependencies;
import uk.ac.liv.moduleextraction.util.DefinitorialDepth;
import uk.ac.liv.moduleextraction.util.ModulePaths;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;


public class SignatureAnalyser {

	private Set<OWLLogicalAxiom> logicalAxioms;
	private DefinitorialDepth definitorialDepth;
	private DefinitorialDependencies dependencies;

	public SignatureAnalyser(Set<OWLLogicalAxiom> axioms) {
		this.logicalAxioms = axioms;
		this.definitorialDepth = new DefinitorialDepth(logicalAxioms);
		this.dependencies = new DefinitorialDependencies(logicalAxioms);
	}

	public int averageDefinitorialDepth(Set<OWLClass> signature){
		int totalDepth = 0;

		for(OWLClass cls : signature)
			totalDepth += definitorialDepth.lookup(cls);

		return totalDepth/signature.size();
	}

	public int averageDependencySize(Set<OWLClass> signature){
		int totalSize = 0;
		for(OWLClass cls : signature)
			totalSize += dependencies.getDependenciesFor(cls).size();
		
		return totalSize/signature.size();
	}

	public int averageDistanceBetweenConcepts(Set<OWLClass> signature){
		return 0;
	}

	public static void main(String[] args) {
		OWLOntology ont = 
				OntologyLoader.loadOntology(ModulePaths.getOntologyLocation()+"NCI/nci-08.09d-terminology.owl");
		SignatureGenerator generator = new SignatureGenerator(ont.getLogicalAxioms());
		SignatureAnalyser analyser =  new SignatureAnalyser(ont.getLogicalAxioms());
		
		for (int i = 0; i < 100; i++) {
			Set<OWLClass> randomSig = generator.generateRandomClassSignature(100);
			System.out.println(analyser.averageDependencySize(randomSig));
		}


		
	}




}