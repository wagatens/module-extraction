package uk.ac.liv.moduleextraction.experiments;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.print.attribute.standard.SheetCollate;



import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;


import uk.ac.liv.moduleextraction.main.ModuleExtractor;
import uk.ac.liv.moduleextraction.qbf.QBFSolverException;
import uk.ac.liv.moduleextraction.reloading.DumpExtractionToDisk;
import uk.ac.liv.moduleextraction.reloading.ReloadExperimentFromDisk;
import uk.ac.liv.moduleextraction.signature.SignatureGenerator;
import uk.ac.liv.moduleextraction.util.ModulePaths;
import uk.ac.liv.moduleextraction.util.ModuleUtils;
import uk.ac.liv.ontologyutils.axioms.AxiomExtractor;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

public class ExtractionComparision {

	OWLOntologyManager manager;
	OWLOntology ontology;
	ModuleExtractor moduleExtractor;
	SyntacticLocalityModuleExtractor syntaxModExtractor;
	private final ScheduledExecutorService scheduler =
			Executors.newScheduledThreadPool(1);

	private String experimentName = "temp";

	private Set<OWLClass> signature;

	private DumpExtractionToDisk dump;
	private ScheduledFuture<?> dumpHandle;
	private ReloadExperimentFromDisk reloader;

	private int syntaticSize = 0;

	private boolean resumingExperiments = false;

	public ExtractionComparision(OWLOntology ontology, Set<OWLClass> sig, String name) {
		AxiomExtractor extractor = new AxiomExtractor();
		this.experimentName = name;
		this.signature = sig;
		this.ontology = extractor.extractInclusionsAndEqualities(ontology);
		System.out.println("Extracted inclusions and equalities only");
	}

	public ExtractionComparision(String experimentLocation) throws IOException{
		this.experimentName = experimentLocation.replaceFirst(".*/([^/?]+).*", "$1");
		this.resumingExperiments = true;
		reloader = new ReloadExperimentFromDisk(experimentLocation);
		this.signature = reloader.getSignature();
	}


	public void compareExtractionApproaches() throws IOException, QBFSolverException, OWLOntologyStorageException, OWLOntologyCreationException{	
		/* The tests use the same signature (just OWLClass) 
		 * but one must be converted to OWLEntities as expected 
		 * by the OWLAPI*/

		File experimentLocation = new File(ModulePaths.getOntologyLocation() + "/Results/" + experimentName  + "/" + "experiment-results");
		if(experimentLocation.exists()){
			System.out.println("Already complete");
			return;
		}


		long startTime = System.currentTimeMillis();

		Set<OWLClass> classSignature = signature;

		Set<OWLLogicalAxiom> syntacticModule = null;
		if(!resumingExperiments){
			manager = OWLManager.createOWLOntologyManager();
			syntaxModExtractor = new SyntacticLocalityModuleExtractor(manager, ontology, ModuleType.STAR);
			Set<OWLEntity> entitySignature = 
					new HashSet<OWLEntity>(classSignature);

			Set<OWLAxiom> syntacticOntology = syntaxModExtractor.extract(entitySignature);

			OWLXMLOntologyFormat owlFormat = new OWLXMLOntologyFormat();
			manager.saveOntology(manager.createOntology(syntacticOntology),owlFormat, 
					IRI.create(new File(ModulePaths.getOntologyLocation() + "/Results/" + experimentName  + "/" + "syntacticModule.owl")));

			syntacticModule = getLogicalAxioms(syntacticOntology);

			/* Store the size here as the semantic approach is destructive */
			this.moduleExtractor = new ModuleExtractor(syntacticModule,signature);
		}
		else{
			this.moduleExtractor = new ModuleExtractor(reloader.getTerminology(), reloader.getModule(),signature);
			syntacticModule = reloader.getSyntacticModule();
		}


		syntaticSize = syntacticModule.size();
		System.out.println("Syntsize: " + syntaticSize);


		this.dump = new DumpExtractionToDisk(
				experimentName,moduleExtractor.getTerminology(), 
				moduleExtractor.getModule(), signature);

		this.dumpHandle = scheduler.scheduleAtFixedRate(dump,
				0, 30, TimeUnit.MINUTES);

		Set<OWLLogicalAxiom> semanticModule = moduleExtractor.extractModule();
		writeResults(semanticModule);

		System.out.println(ModuleUtils.getTimeAsHMS(System.currentTimeMillis() - startTime));

	}

	public void writeResults(Set<OWLLogicalAxiom> semanticModule) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(ModulePaths.getOntologyLocation() + "/Results/" + experimentName  + "/" + "experiment-results", false));
		
		writer.write("#Signature Size\t Syntactic Size\t Synt->Semantic Size\n");
		writer.write(signature.size() + ":" + syntaticSize + ":" +semanticModule.size() + "\n");
		writer.flush();
		writer.close();

		/* Dump the results one last time before finishing */
		new Thread(dump).run();

		/* Finish the scheduling dumps */
		dumpHandle.cancel(true);
		scheduler.shutdownNow();
	}


	public Set<OWLLogicalAxiom> getLogicalAxioms(Set<OWLAxiom> axioms){
		HashSet<OWLLogicalAxiom> result = new HashSet<OWLLogicalAxiom>();
		for(OWLAxiom ax : axioms){
			if(ax.isLogicalAxiom())
				result.add((OWLLogicalAxiom) ax);
		}
		return result;
	}



}