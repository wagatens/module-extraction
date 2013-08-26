package uk.ac.liv.moduleextraction.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.base.Stopwatch;

import uk.ac.liv.moduleextraction.extractor.IteratingExtractor;
import uk.ac.liv.ontologyutils.expressions.ALCValidator;
import uk.ac.liv.ontologyutils.expressions.ELValidator;
import uk.ac.liv.ontologyutils.util.ModuleUtils;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

public class IteratingExperiment implements Experiment {

	private SyntacticLocalityModuleExtractor starExtractor;
	private IteratingExtractor iteratingExtractor;
	private int starSize = 0;
	private int itSize = 0;
	private Set<OWLLogicalAxiom> starModule;
	private Set<OWLLogicalAxiom> itModule;

	private static HashMap<OWLLogicalAxiom, Integer> differenceMap = new HashMap<OWLLogicalAxiom, Integer>();

	public IteratingExperiment(OWLOntology ont) {
		OWLOntologyManager manager = ont.getOWLOntologyManager();
		this.starExtractor = new SyntacticLocalityModuleExtractor(manager, ont, ModuleType.STAR);
		this.iteratingExtractor = new IteratingExtractor(ont);
	}

	public static HashMap<OWLLogicalAxiom, Integer> getDifferenceMap(){
		return differenceMap;
	}

	@Override
	public void performExperiment(Set<OWLEntity> signature) {
		
		

		Stopwatch stopwatch = new Stopwatch().start();
		//Compute the star module on it's own
		Set<OWLAxiom> starAxioms = starExtractor.extract(signature);
		System.out.println(stopwatch.stop());
		starModule = ModuleUtils.getLogicalAxioms(starAxioms);
		starSize = starModule.size();
		

		Stopwatch stopwatch2 = new Stopwatch().start();
		//And then the iterated one 
		itModule = iteratingExtractor.extractModule(signature);
		itSize = itModule.size();
		System.out.println(stopwatch2.stop());
//		

	
		

		
		System.out.println();



	}
	
	

	@Override
	public void writeMetrics(File experimentLocation) throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(experimentLocation.getAbsoluteFile() + "/" + "experiment-results", false));

		writer.write("StarSize, IteratedSize, Difference, QBFChecks, StarExtractions, AmexExtractions" + "\n");
		writer.write(starSize + "," + itSize + "," + ((starSize == itSize) ? "0" : "1") + "," +  iteratingExtractor.getQBFChecks() + "," +
				iteratingExtractor.getStarExtractions() + "," + iteratingExtractor.getAmexExtrations() + "\n");
		writer.flush();
		writer.close();

	}


}
