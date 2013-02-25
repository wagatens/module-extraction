package uk.ac.liv.moduleextraction.chaindependencies;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.liv.moduleextraction.testing.AcyclicChecker;
import uk.ac.liv.moduleextraction.util.DefinitorialDepth;
import uk.ac.liv.moduleextraction.util.ModulePaths;
import uk.ac.liv.moduleextraction.util.ModuleUtils;
import uk.ac.liv.ontologyutils.axioms.AxiomSplitter;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;


public class ChainDependencies extends HashMap<OWLClass, DependencySet>{

	private static final long serialVersionUID = 5599458330117570660L;

	private HashMap<OWLClass, OWLLogicalAxiom> lhsLookup;
	
	public ChainDependencies() {
		lhsLookup = new HashMap<OWLClass, OWLLogicalAxiom>();
	}
	
	public void updateDependenciesWith(List<OWLLogicalAxiom> sortedAxioms){
		for(Iterator<OWLLogicalAxiom> it = sortedAxioms.iterator(); it.hasNext();)
			updateDependenciesWith(it.next());
	}
	
	public void updateDependenciesWith(OWLLogicalAxiom axiom){

		OWLClass name = (OWLClass) AxiomSplitter.getNameofAxiom(axiom);
		OWLClassExpression definition = AxiomSplitter.getDefinitionofAxiom(axiom);
		/* Map names to axioms - used for calculating chains of dependencies */
		lhsLookup.put(name, axiom);

		/*Calculate dependencies */
		DependencySet axiomDeps = new DependencySet();
		addImmediateDependencies(definition,axiomDeps);
		updateFromDefinition(definition, axiomDeps);
		put(name, axiomDeps);
	}
	
	/* Find an axiom which was used in calculating the dependencies based on it's name. */
	public OWLLogicalAxiom lookup(OWLClass cls) {
		return lhsLookup.get(cls);
	}

	private void addImmediateDependencies(OWLClassExpression definition, DependencySet axiomDeps) {
		for(OWLEntity e : definition.getSignature()){
			if(!e.isTopEntity() && !e.isBottomEntity())
				axiomDeps.add(new Dependency(e));
		}
	}

	private void updateFromDefinition(OWLClassExpression definition, DependencySet axiomDeps) {
		for(OWLClass cls : ModuleUtils.getNamedClassesInSignature(definition)){
			DependencySet clsDependencies = get(cls);
			if(clsDependencies != null)
				axiomDeps.mergeWith(cls, clsDependencies);
		}
	}
	
	@Override public void clear() {
		lhsLookup.clear();
		super.clear();
	}
 
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(OWLClass cls : keySet()){
			builder.append(cls + "=" + get(cls) + "\n");
		}
		return builder.toString();
	}
	
	public static void main(String[] args) {
		OWLOntology ont = OntologyLoader.loadOntology(
				ModulePaths.getOntologyLocation() + "moduletest/chaindeps2.krss");
		

		DefinitorialDepth defDeps = new DefinitorialDepth(ont);
		ChainDependencies deps = new ChainDependencies();
		
		for(OWLLogicalAxiom ax : defDeps.getDefinitorialSortedList()){
			System.out.println(ax);
			deps.updateDependenciesWith(ax);
		}
		System.out.println();
		System.out.println(deps);
	}
	
}
