package uk.ac.liv.moduleextraction.checkers;


import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import uk.ac.liv.moduleextraction.chaindependencies.AxiomDependencies;
import uk.ac.liv.moduleextraction.chaindependencies.DependencySet;
import uk.ac.liv.moduleextraction.signature.SignatureGenerator;
import uk.ac.liv.moduleextraction.storage.DefinitorialAxiomStore;
import uk.ac.liv.ontologyutils.axioms.AtomicLHSAxiomVerifier;
import uk.ac.liv.ontologyutils.axioms.AxiomSplitter;
import uk.ac.liv.ontologyutils.axioms.AxiomStructureInspector;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;
import uk.ac.liv.ontologyutils.util.ModulePaths;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ExtendedLHSSigExtractor {

	private Set<OWLEntity> signatureDependencies = new HashSet<OWLEntity>();

	AxiomDependencies dependencies;
    AtomicLHSAxiomVerifier verifier = new AtomicLHSAxiomVerifier();


	public HashSet<OWLLogicalAxiom> getLHSSigAxioms(boolean[] terminology,
			DefinitorialAxiomStore axiomStore, Set<OWLEntity> sigUnionSigM, AxiomDependencies dependT) {


		this.dependencies = dependT;
		HashSet<OWLLogicalAxiom> lhsSigT = new HashSet<OWLLogicalAxiom>();
		generateSignatureDependencies(axiomStore.getSubsetAsList(terminology), sigUnionSigM);
		Set<OWLClass> sharedOrRepeated = getSharedOrRepeatedNames(axiomStore.getSubsetAsList(terminology));

		for (int i = 0; i < terminology.length; i++) {
			if(terminology[i]){
				OWLLogicalAxiom axiom = axiomStore.getAxiom(i);
                if(!verifier.isSupportedAxiom(axiom)){
					lhsSigT.add(axiom);
				}
				else{
					OWLClass name = (OWLClass) AxiomSplitter.getNameofAxiom(axiom);
					if(sigUnionSigM.contains(name) || isInSigDependencies(name) || sharedOrRepeated.contains(name)){
						lhsSigT.add(axiom);
					}
				}
			}
		}


		return lhsSigT;
	}

	public HashSet<OWLLogicalAxiom> getLHSSigAxioms(List<OWLLogicalAxiom> sortedOntology, Set<OWLEntity> sigUnionSigM, AxiomDependencies depends){

		HashSet<OWLLogicalAxiom> lhsSigT = new HashSet<OWLLogicalAxiom>();
		this.dependencies = depends;

		generateSignatureDependencies(sortedOntology, sigUnionSigM);
		Set<OWLClass> sharedOrRepeated = getSharedOrRepeatedNames(sortedOntology);

		for(OWLLogicalAxiom axiom : sortedOntology){
			if(!verifier.isSupportedAxiom(axiom)){
				lhsSigT.add(axiom);
			}
			else{
				OWLClass name = (OWLClass) AxiomSplitter.getNameofAxiom(axiom);
				if(sigUnionSigM.contains(name) || isInSigDependencies(name) || sharedOrRepeated.contains(name)){
					lhsSigT.add(axiom);
				}
			}

		}
		return lhsSigT;
	}


	private void generateSignatureDependencies(List<OWLLogicalAxiom> terminologyAxioms, Set<OWLEntity> signature) {
		AxiomStructureInspector inspector = new AxiomStructureInspector(terminologyAxioms);
		for(OWLEntity sigConcept : signature){

			Set<OWLLogicalAxiom> toCheck = new HashSet<OWLLogicalAxiom>();
			if(sigConcept instanceof OWLClass){
				toCheck.addAll(inspector.getDefinitions((OWLClass) sigConcept));
				toCheck.addAll(inspector.getPrimitiveDefinitions((OWLClass) sigConcept));
			}

			for(OWLLogicalAxiom axiom : toCheck){
				DependencySet sigDeps = dependencies.get(axiom);
				if(sigDeps != null){
					signatureDependencies.addAll(sigDeps);
				}
			}

		}
	}

	private boolean isInSigDependencies(OWLClass name){
		return signatureDependencies.contains(name);
	}

	private Set<OWLClass> getSharedOrRepeatedNames(List<OWLLogicalAxiom> terminologyAxioms){
		AxiomStructureInspector structInspector = new AxiomStructureInspector(terminologyAxioms);
		Set<OWLClass> sharedNames = structInspector.getSharedNames();
		Set<OWLClass> repeatedEquivalances = structInspector.getNamesWithRepeatedEqualities();

		Set<OWLClass> toAddToLhs = new HashSet<OWLClass>();
		toAddToLhs.addAll(sharedNames);
		toAddToLhs.addAll(repeatedEquivalances);


		return toAddToLhs;

	}

	public static void main(String[] args) {
		File ontloc = new File(ModulePaths.getOntologyLocation() + "/semantic-only/Genomic-CDS-core");
		OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ontloc.getAbsolutePath());
		System.out.println("|O|: " + ont.getLogicalAxiomCount());
		AxiomDependencies dependencies = new AxiomDependencies(ont);
		DefinitorialAxiomStore store = new DefinitorialAxiomStore(dependencies.getDefinitorialSortedAxioms());
		System.out.println("Done dependencies");
		ExtendedLHSSigExtractor extractor = new ExtendedLHSSigExtractor();

		SignatureGenerator gen = new SignatureGenerator(ont.getLogicalAxioms());
		Set<OWLEntity> sig = gen.generateRandomSignature(75);

		System.out.println("Sig: " + sig.size());
		Set<OWLLogicalAxiom> lhs = extractor.getLHSSigAxioms(store.allAxiomsAsBoolean(), store, sig, dependencies);
		System.out.println("LHS: " + lhs.size());
	}


}
