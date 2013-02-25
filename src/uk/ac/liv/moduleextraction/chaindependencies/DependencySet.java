package uk.ac.liv.moduleextraction.chaindependencies;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;

public class  DependencySet extends HashSet<Dependency>{

	private static final long serialVersionUID = 5147803484884184934L;

	public Set<OWLEntity> asOWLEntities(){
		Set<OWLEntity> entities = new HashSet<OWLEntity>();
		for(Dependency d : this){
			entities.add(d.getValue());
		}
		return entities;
	}
	
	public Dependency getDependencyFor(OWLEntity e) {
		Dependency toFind = new Dependency(e);
		if(contains(toFind)){
			for(Dependency d : this){
				if(d.equals(toFind))
					return d;
			}
		}
		return null;
	}

	public void mergeWith(OWLClass name, DependencySet dependencySet){
		for(Dependency d : dependencySet){
			Dependency toAdd = null; 
			if(contains(d)){
				toAdd = alreadyContains(d, toAdd);
			}
			else{
				toAdd = addNew(d);
			}
			toAdd.addOrigin(name);
		}
	}

	private Dependency addNew(Dependency d) {
		Dependency toAdd;
		toAdd = new Dependency(d);
		add(toAdd);
		return toAdd;
	}

	private Dependency alreadyContains(Dependency d, Dependency toAdd) {
		for(Dependency selfDep: this){
			if(d.equals(selfDep))
				toAdd = selfDep;
		}
		toAdd.addOrigins(d.getOrigins());
		return toAdd;
	}

}
