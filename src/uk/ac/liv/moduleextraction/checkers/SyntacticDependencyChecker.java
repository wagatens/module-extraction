package uk.ac.liv.moduleextraction.checkers;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;

import uk.ac.liv.moduleextraction.chaindependencies.ChainDependencies;
import uk.ac.liv.moduleextraction.chaindependencies.Dependency;
import uk.ac.liv.moduleextraction.chaindependencies.DependencySet;
import uk.ac.liv.moduleextraction.datastructures.LinkedHashList;
import uk.ac.liv.ontologyutils.axioms.AxiomSplitter;

public class SyntacticDependencyChecker {
	
	Set<OWLLogicalAxiom> axiomsWithDeps;

	public boolean hasSyntacticSigDependency(LinkedHashList<OWLLogicalAxiom> W, ChainDependencies dependsW, Set<OWLEntity> signatureAndSigM){
		
		OWLLogicalAxiom lastAdded = W.getLast();

		OWLClass axiomName = (OWLClass) AxiomSplitter.getNameofAxiom(lastAdded);
		axiomsWithDeps = new HashSet<OWLLogicalAxiom>();
		
		boolean result = false;
		
		if(!signatureAndSigM.contains(axiomName))
			return result;
		else{
			DependencySet axiomDepends = dependsW.get(axiomName);
			Set<OWLEntity> intersectEntites = axiomDepends.asOWLEntities();
			intersectEntites.retainAll(signatureAndSigM);

			if(!intersectEntites.isEmpty()){
				HashSet<OWLClass> toFind = new HashSet<OWLClass>();
				for(OWLEntity e : intersectEntites){
					Dependency intersectDep = axiomDepends.getDependencyFor(e);
					for(OWLEntity origin : intersectDep.getOrigins()){
						if(origin.isOWLClass()){
							toFind.add((OWLClass) origin);
						}
					}
				}
				
				for(OWLLogicalAxiom ax : W){
					OWLClass name = (OWLClass) AxiomSplitter.getNameofAxiom(ax);
					if(toFind.contains(name)){
						axiomsWithDeps.add(ax);
					}
				}
				
				
				
				result = true;
				axiomsWithDeps.add(lastAdded);
				dependsW.clear();
			}
			return result;
		
		}

	}
	
	public Set<OWLLogicalAxiom> getAxiomsWithDependencies(){
		return axiomsWithDeps;
	}

}	


