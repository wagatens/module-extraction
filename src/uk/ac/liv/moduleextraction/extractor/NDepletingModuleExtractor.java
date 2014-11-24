package uk.ac.liv.moduleextraction.extractor;

import org.semanticweb.owlapi.model.*;
import uk.ac.liv.moduleextraction.chaindependencies.AxiomDependencies;
import uk.ac.liv.moduleextraction.checkers.ELAxiomChainCollector;
import uk.ac.liv.moduleextraction.checkers.NElementInseparableChecker;
import uk.ac.liv.moduleextraction.experiments.SupportedExpressivenessFilter;
import uk.ac.liv.moduleextraction.qbf.NElementSeparabilityAxiomLocator;
import uk.ac.liv.moduleextraction.qbf.QBFSolverException;
import uk.ac.liv.moduleextraction.storage.DefinitorialAxiomStore;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;
import uk.ac.liv.ontologyutils.ontologies.OntologyCycleVerifier;
import uk.ac.liv.ontologyutils.util.ModulePaths;
import uk.ac.liv.ontologyutils.util.ModuleUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class NDepletingModuleExtractor implements Extractor {

	private final DefinitorialAxiomStore axiomStore;
	private Set<OWLLogicalAxiom> module;
	private Set<OWLEntity> sigUnionSigM;
	private final NElementInseparableChecker inseparableChecker;
	private long qbfChecks = 0;
	private final ELAxiomChainCollector chainCollector;
	private AxiomDependencies dependT;
	private List<OWLLogicalAxiom> allAxioms;
	private Set<OWLLogicalAxiom> cycleCausing = new HashSet<OWLLogicalAxiom>();
	private Set<OWLLogicalAxiom> expressive;
	private ArrayList<OWLLogicalAxiom> acyclicAxioms;

	private final int DOMAIN_SIZE;

	private NDepletingModuleExtractor(int domain_size, OWLOntology ontology) {
		this(domain_size,ontology.getLogicalAxioms());
	}

	public NDepletingModuleExtractor(int domain_size, Set<OWLLogicalAxiom> ontology){
		this.DOMAIN_SIZE = domain_size;
		this.axiomStore = new DefinitorialAxiomStore(ontology);
		this.inseparableChecker = new NElementInseparableChecker(domain_size);
		this.chainCollector = new ELAxiomChainCollector();
	}

	@Override
	public Set<OWLLogicalAxiom> extractModule(Set<OWLEntity> signature) {
		return extractModule(new HashSet<OWLLogicalAxiom>(),signature);
	}

	private OWLLogicalAxiom findSeparableAxiom(boolean[] terminology)
			throws IOException, QBFSolverException, ExecutionException {

		NElementSeparabilityAxiomLocator locator =
				new NElementSeparabilityAxiomLocator(DOMAIN_SIZE,axiomStore.getSubsetAsArray(terminology),sigUnionSigM);

		OWLLogicalAxiom insepAxiom = locator.getSeparabilityCausingAxiom();
		qbfChecks += locator.getCheckCount();

		return insepAxiom;
	}

	@Override
	public Set<OWLLogicalAxiom> extractModule(Set<OWLLogicalAxiom> existingModule, Set<OWLEntity> signature) {

		boolean[] terminology = axiomStore.allAxiomsAsBoolean();
		allAxioms = axiomStore.getSubsetAsList(terminology);

		SupportedExpressivenessFilter filter = new SupportedExpressivenessFilter();
		expressive = filter.getUnsupportedAxioms(allAxioms);
		allAxioms.removeAll(expressive);

		//Can only determine if there is a cycle after removing any expressive axioms
		OntologyCycleVerifier verifier = new OntologyCycleVerifier(allAxioms);
		if(verifier.isCyclic()){
			cycleCausing = verifier.getCycleCausingAxioms();
			/* All axioms is now the acyclic subset of the ontology 
			after removing unsupported or cycle causing axioms */
			allAxioms.removeAll(cycleCausing);
		}

		dependT = new AxiomDependencies(new HashSet<OWLLogicalAxiom>(allAxioms));
		acyclicAxioms = dependT.getDefinitorialSortedAxioms();


		module = existingModule;
		sigUnionSigM = ModuleUtils.getClassAndRoleNamesInSet(existingModule);
		sigUnionSigM.addAll(signature);

		try {
			//Apply the rules to everything
			applyRules(terminology);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (QBFSolverException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return module;
	}

	void applyRules(boolean[] terminology) throws IOException, QBFSolverException, ExecutionException {
		moveELChainsToModule(acyclicAxioms, terminology, axiomStore);	

		if(inseparableChecker.isSeparableFromEmptySet(axiomStore.getSubsetAsList(terminology), sigUnionSigM)){
			OWLLogicalAxiom axiom = findSeparableAxiom(terminology);
			module.add(axiom);
			removeAxiom(terminology, axiom);
			sigUnionSigM.addAll(axiom.getSignature());
			qbfChecks += inseparableChecker.getTestCount();
			applyRules(terminology);
		}
	}

	private void moveELChainsToModule(List<OWLLogicalAxiom> acyclicAxioms, boolean[] terminology, DefinitorialAxiomStore axiomStore){
		boolean change = true;
		while(change){
			change = false;
			for (int i = 0; i < acyclicAxioms.size(); i++) {
				OWLLogicalAxiom chosenAxiom = acyclicAxioms.get(i);
				if(chainCollector.hasELSyntacticDependency(chosenAxiom, dependT, sigUnionSigM)){
					change = true;
					ArrayList<OWLLogicalAxiom> chain = 
							chainCollector.collectELAxiomChain(acyclicAxioms, i, terminology, axiomStore, dependT, sigUnionSigM);

					module.addAll(chain);
					acyclicAxioms.removeAll(chain);
				}
			}

		}
	}


	public long getQBFCount(){
		return qbfChecks;
	}

	void removeAxiom(boolean[] terminology, OWLLogicalAxiom axiom){
		axiomStore.removeAxiom(terminology, axiom);
		allAxioms.remove(axiom);
		cycleCausing.remove(axiom);
		acyclicAxioms.remove(axiom);
		expressive.remove(axiom);
	}

	public static void main(String[] args) {
		//OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ModulePaths.getOntologyLocation() + "/examples/nbroken2.owl");
		OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ModulePaths.getOntologyLocation() + "/examples/nice.krss");
		System.out.println("LOADED");

		OWLDataFactory f = ont.getOWLOntologyManager().getOWLDataFactory();

		ModuleUtils.remapIRIs(ont,"X");

		for(OWLLogicalAxiom ax : ont.getLogicalAxioms()){
			System.out.println(ax);
		}

		HashSet<OWLEntity> signature = new HashSet<OWLEntity>();
		OWLClass a = f.getOWLClass(IRI.create("X#A"));
		OWLClass b = f.getOWLClass(IRI.create("X#B"));
		OWLClass c = f.getOWLClass(IRI.create("X#C"));
		OWLObjectProperty r = f.getOWLObjectProperty(IRI.create("X#r"));
		signature.add(a);
		//signature.add(b);
		//signature.add(c);
		signature.add(r);


		NDepletingModuleExtractor one = new NDepletingModuleExtractor(2,ont.getLogicalAxioms());
		System.out.println("M: " + one.extractModule(signature));


	}



}