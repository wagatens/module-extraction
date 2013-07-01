package uk.ac.liv.moduleextraction.extractor;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.liv.moduleextraction.signature.SigManager;
import uk.ac.liv.moduleextraction.signature.SignatureGenerator;
import uk.ac.liv.moduleextraction.util.ModulePaths;
import uk.ac.liv.moduleextraction.util.ModuleUtils;
import uk.ac.liv.ontologyutils.axioms.ELValidator;
import uk.ac.liv.ontologyutils.axioms.SupportedAxiomVerifier;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

public class LovelyFunTimeExtractor implements Extractor {

	private OWLOntology ontology;
	private SupportedAxiomVerifier verifier;
	private OWLOntologyManager manager;

	public LovelyFunTimeExtractor(OWLOntology ont) {
		this.ontology = ont;
		this.manager = ont.getOWLOntologyManager();
		this.verifier = new SupportedAxiomVerifier();
	}
	
	@Override
	public Set<OWLLogicalAxiom> extractModule(Set<OWLEntity> signature) {
		
		Set<OWLLogicalAxiom> module = ontology.getLogicalAxioms();
		boolean sizeChanged = true;
		
		int moduleSize = module.size();
		int iterations = 0;
		
		while(sizeChanged){
			
			Set<OWLEntity> origSig = new HashSet<OWLEntity>(signature);
			
			module = extractStarModule(createOntologyFromLogicalAxioms(module), origSig);
			
			moduleSize = module.size();
			
			Set<OWLLogicalAxiom> starMod = new HashSet<OWLLogicalAxiom>(module);
			
			Set<OWLLogicalAxiom> unsupported = getUnsupportedAxioms(module);
			module.removeAll(unsupported);
			
			
			module  = extractSemanticModule(createOntologyFromLogicalAxioms(module), unsupported, origSig);
			
			starMod.removeAll(module);
			
			
			int newModuleSize = module.size(); 
			
			if(moduleSize== newModuleSize){
				sizeChanged = false;
			}
			moduleSize = newModuleSize;
			iterations++;
			
		}
		

		
		return module;
	}
	
	private Set<OWLLogicalAxiom> extractStarModule(OWLOntology ontology, Set<OWLEntity> signature){
		SyntacticLocalityModuleExtractor 
		extractor = new SyntacticLocalityModuleExtractor(manager, ontology, ModuleType.STAR);
		
		Set<OWLLogicalAxiom> module = ModuleUtils.getLogicalAxioms(extractor.extract(signature));
		manager.removeOntology(ontology);
		return module;
	}
	
	private Set<OWLLogicalAxiom> extractSemanticModule(OWLOntology ontology, Set<OWLLogicalAxiom> existingmodule, Set<OWLEntity> signature){
		EquivalentToTerminologyExtractor extractor = new EquivalentToTerminologyExtractor(ontology);
		Set<OWLLogicalAxiom> module = extractor.extractModule(existingmodule, signature);
		manager.removeOntology(ontology);
		return module;
	}

	private Set<OWLLogicalAxiom> getUnsupportedAxioms(Set<OWLLogicalAxiom> module){
		Set<OWLLogicalAxiom> unsupported = new HashSet<OWLLogicalAxiom>();
		for(OWLLogicalAxiom axiom : module){
			if(!verifier.isSupportedAxiom(axiom)){
				unsupported.add(axiom);
			}
		}
		return unsupported;
	}
	
	private OWLOntology createOntologyFromLogicalAxioms(Set<OWLLogicalAxiom> axioms){
		Set<OWLAxiom> newOntAxioms = new HashSet<OWLAxiom>();
		newOntAxioms.addAll(axioms);
		OWLOntology ont = null;
		try {
			ont = manager.createOntology(newOntAxioms);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		return ont;
	}
	
	@Override
	public Set<OWLLogicalAxiom> extractModule(
			Set<OWLLogicalAxiom> existingModule, Set<OWLEntity> signature) {
		return null;
	}
	
	public static void main(String[] args) throws IOException {
		
		OWLOntology ont = OntologyLoader.loadOntologyInclusionsAndEqualities(ModulePaths.getOntologyLocation() + "/NCI/Thesaurus_08.09d.OWL");
		ELValidator valid = new ELValidator();
		
		for(OWLLogicalAxiom ax : ont.getLogicalAxioms()){
			if(!valid.isELAxiom(ax)){
				System.out.println(ax);
			}
		}
		//LovelyFunTimeExtractor fun = new LovelyFunTimeExtractor(ont);

		


	}

}
