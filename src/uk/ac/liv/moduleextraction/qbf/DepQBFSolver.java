package uk.ac.liv.moduleextraction.qbf;


import com.google.common.base.Stopwatch;
import depqbf4j.DepQBF4J;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import uk.ac.liv.moduleextraction.extractor.NDepletingModuleExtractor;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;
import uk.ac.liv.ontologyutils.util.ModulePaths;
import uk.ac.liv.ontologyutils.util.ModuleUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class DepQBFSolver {

    //CONSIDER USING TROVE (or other high performance library) for unboxed primitive collections
    private Collection<Integer> universal;
    private Collection<Integer> existential;
    private final Set<int[]> clauses;
    private File qbfFile;
    private HashMap<Integer, Integer> remapping;

    public DepQBFSolver(Collection<Integer> universal, Collection<Integer> existential, Set<int[]> clauses) {
        this.universal = universal;
        this.existential = existential;
        this.clauses = clauses;
        this.remapping = new HashMap<>(universal.size() + existential.size());
        remapNumbering();
        constructQBFProblem();
    }

    /* Map variables contiguously from 1, otherwise some QBF solvers complain */
    private void remapNumbering(){

        int startingNumber = 1;
        for(Integer a : universal){
            remapping.put(a, startingNumber);
            startingNumber++;
        }
        for(Integer e : existential){
            remapping.put(e, startingNumber);
            startingNumber++;
        }
    }


    private void constructQBFProblem() {

        qbfFile = null;
        try {
            qbfFile = File.createTempFile("qbf", ".qdimacs", new File("/tmp/"));
            System.out.println(qbfFile.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(new FileWriter(qbfFile));


            int variables = universal.size() + existential.size();


            bw.write("p cnf " + variables + " " + clauses.size() + "\n");

            //Universal
            bw.write("a ");
            for(Integer a : universal){
                bw.write(remapping.get(a) + " ");
            }
            bw.write(0 + "\n");

            //Existential
            bw.write("e ");
            for(Integer e : existential){
                bw.write(remapping.get(e) + " ");
            }
            bw.write(0 + "\n");

            for(int[] clause : clauses){
                for(int i : clause){
                    if(i > 0){
                        bw.write(remapping.get(i) + " ");
                    }
                    else{
                        bw.write(-remapping.get(Math.abs(i)) + " ");
                    }

                }
                bw.write(0 + "\n");
            }

            bw.flush();
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public boolean isSatisfiable() throws QBFSolverException, IOException {
        QBFSolver solver = new QBFSolver();
        return solver.isSatisfiable(qbfFile);
    }

    public boolean DEPQBFisSatisfiable() throws QBFSolverException, IOException {
        boolean returnCode;
        byte exitCode  = DepQBF4J.sat();
        switch (exitCode){
            case DepQBF4J.RESULT_SAT:
                returnCode = true;
                break;
            case DepQBF4J.RESULT_UNSAT:
                returnCode = false;
                break;
            default:
                if(exitCode == DepQBF4J.RESULT_UNKNOWN){
                    System.err.println("The result is unknown");
                }
                else{
                    System.err.println("There was an error with the QBF solver, EXIT CODE " + exitCode);
                }
                File qbf = File.createTempFile("qbf",".qdimacs",new File("/tmp/"));
                DepQBF4J.printToFile(qbf.getAbsolutePath());
                System.err.println("QBF file dumped to: " + qbf.getAbsolutePath());
                throw new QBFSolverException();

        }
        return returnCode;

    }

    private void DEPQBFconstructQBFProblem(){
        int nestingLevel = 1;
        DepQBF4J.create();
        DepQBF4J.configure("--dep-man=simple");

        if(!universal.isEmpty()){
            DepQBF4J.newScopeAtNesting(DepQBF4J.QTYPE_FORALL, nestingLevel++);
            for(int forall : universal){
                DepQBF4J.add(forall);
            }
            DepQBF4J.add(0);
        }

        if(!existential.isEmpty()){
            DepQBF4J.newScopeAtNesting(DepQBF4J.QTYPE_EXISTS, nestingLevel++);
            for(int exists : existential){
                DepQBF4J.add(exists);
            }
            DepQBF4J.add(0);
        }

        for(int[] clause : clauses){
            for(int val : clause){
                DepQBF4J.add(val);
            }
            DepQBF4J.add(0);
        }
    }


    public void delete(){
        DepQBF4J.delete();
    }

    public static void main(String[] args) {

//        HashSet<Integer> uni = Sets.newHashSet(21,34,41,541);
//        HashSet<Integer> ex = Sets.newHashSet(9, 47);
//
//        HashSet<int[]> clauses = new HashSet<>();
//        int[] first = {21, 9, 541};
//        clauses.add(first);
//
//        DepQBFSolver solver = new DepQBFSolver(uni,ex,clauses);


        OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ModulePaths.getOntologyLocation() + "/OWL-Corpus-All/qbf-only/5ce22965-fc92-4d0d-b9f0-97643fcdb42f_mpound.owl-QBF");
        System.out.println(ont.getLogicalAxiomCount());

        NDepletingModuleExtractor extractor = new NDepletingModuleExtractor(1,ont.getLogicalAxioms());

         Set<OWLLogicalAxiom> randomSample = ModuleUtils.generateRandomAxioms(ont.getLogicalAxioms(), 1);

            Stopwatch samplewatch = Stopwatch.createStarted();
            for(OWLLogicalAxiom axiom : randomSample){

                Set<OWLEntity> sig = axiom.getSignature();
                extractor.extractModule(sig);


            }
            samplewatch.stop();
            System.out.println(samplewatch);

        ont.getOWLOntologyManager().removeOntology(ont);
        ont = null;



    }
}
