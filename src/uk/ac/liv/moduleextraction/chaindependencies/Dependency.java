package uk.ac.liv.moduleextraction.chaindependencies;

import java.util.HashSet;

import org.semanticweb.owlapi.model.OWLEntity;

public class Dependency {
	private OWLEntity value;
	private HashSet<OWLEntity> origins;
	
	public Dependency(OWLEntity value) {
		this.value = value;
		this.origins = new HashSet<OWLEntity>();
	}
	
	public OWLEntity getValue() {
		return value;
	}
	
	public void addOrigin(OWLEntity origin){
		origins.add(origin);
	}
	
	@Override
	public String toString() {
		return value.toString() + origins;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Dependency){
			Dependency d = (Dependency) obj;
			return d.getValue().equals(value);
		}
		return value.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return value.getIRI().hashCode();
	}
}
