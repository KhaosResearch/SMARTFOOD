package epanet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.addition.epanet.network.Network;
import org.addition.epanet.network.structures.Link;
import org.apache.commons.lang.ArrayUtils;
import org.testng.annotations.Test;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;

import epanet.problem.Problem;
import io.netty.util.internal.ThreadLocalRandom;

public class TestComponent {
    @Test
    void correctSSEEvaluationCasePivot() {
        genericEvaluation(
            "../data/pivot.inp", 
            "../data/pivot_default_pressures.csv", 
            "SSE", 
            null, 
            0.0
        );
    }

    @Test
    void correctSSEEvaluationCaseTutorial() {
        genericEvaluation(
            "../data/tutorial.inp", 
            "../data/tutorial_default_pressures.csv", 
            "SSE", 
            null, 
            0.0
        );
    }

    @Test
    void correctSSEEvaluationCaseVillar() {
        genericEvaluation(
            "../data/villar.inp", 
            "../data/villar_default_pressures.csv",
            "SSE", 
            null, 
            0.0
        );
    }

    @Test
    void correctVariableHomogeneityEvaluationCasePivot() {
        Logger log = Logger.getLogger("Test EPANET");
        log.setLevel(Level.OFF);
        Network net = StaticUtils.readInpFile(new File("../data/pivot.inp"), log);

        for (int i = 0; i < 20; i++) {
            double[] randoms = ThreadLocalRandom.current().doubles(181).toArray();
            Double[] wrapRandoms = ArrayUtils.toObject(randoms);
            List<Double> solutionValues = Arrays.asList(wrapRandoms);

            Double[] change = new Double[randoms.length];
            Collection<Link> pipes = StaticUtils.getPipesFromNet(net);
            Iterator<Link> pipeIterator = pipes.iterator();
            for (int j = 0; j < pipes.size(); j++) {
                change[j] = Math.abs(1 - randoms[j]/pipeIterator.next().getRoughness());
            }

            genericEvaluation(
                "../data/pivot.inp", 
                "../data/pivot_default_pressures.csv", 
                "RoughnessHomogeneity", 
                solutionValues, 
                StaticUtils.calculateSD(change)
            );
        }
    }

    @Test
    void correctVariableHomogeneityEvaluationCaseTutorial() {
        Logger log = Logger.getLogger("Test EPANET");
        log.setLevel(Level.OFF);
        Network net = StaticUtils.readInpFile(new File("../data/tutorial.inp"), log);

        for (int i = 0; i < 20; i++) {
            double[] randoms = ThreadLocalRandom.current().doubles(6).toArray();
            Double[] wrapRandoms = ArrayUtils.toObject(randoms);
            List<Double> solutionValues = Arrays.asList(wrapRandoms);

            Double[] change = new Double[randoms.length];
            Collection<Link> pipes = StaticUtils.getPipesFromNet(net);
            Iterator<Link> pipeIterator = pipes.iterator();
            for (int j = 0; j < pipes.size(); j++) {
                change[j] = Math.abs(1 - randoms[j]/pipeIterator.next().getRoughness());
            }

            genericEvaluation(
                "../data/tutorial.inp", 
                "../data/tutorial_default_pressures.csv", 
                "RoughnessHomogeneity", 
                solutionValues, 
                StaticUtils.calculateSD(change)
            );
        }
    }

    @Test
    void correctVariableHomogeneityEvaluationCaseVillar() {
        Logger log = Logger.getLogger("Test EPANET");
        log.setLevel(Level.OFF);
        Network net = StaticUtils.readInpFile(new File("../data/villar.inp"), log);

        for (int i = 0; i < 20; i++) {
            double[] randoms = ThreadLocalRandom.current().doubles(85).toArray();
            Double[] wrapRandoms = ArrayUtils.toObject(randoms);
            List<Double> solutionValues = Arrays.asList(wrapRandoms);

            Double[] change = new Double[randoms.length];
            Collection<Link> pipes = StaticUtils.getPipesFromNet(net);
            Iterator<Link> pipeIterator = pipes.iterator();
            for (int j = 0; j < pipes.size(); j++) {
                change[j] = Math.abs(1 - randoms[j]/pipeIterator.next().getRoughness());
            }

            genericEvaluation(
                "../data/villar.inp", 
                "../data/villar_default_pressures.csv", 
                "RoughnessHomogeneity", 
                solutionValues, 
                StaticUtils.calculateSD(change)
            );
        }
    }

    @Test
    void correctSEHomogeneityEvaluationCasePivot() {
        genericEvaluation(
            "../data/pivot.inp", 
            "../data/pivot_default_pressures.csv", 
            "SEHomogeneity", 
            null, 
            0.0
        );
    }

    @Test
    void correctSEHomogeneityEvaluationCaseTutorial() {
        genericEvaluation(
            "../data/tutorial.inp", 
            "../data/tutorial_default_pressures.csv", 
            "SEHomogeneity", 
            null, 
            0.0
        );
    }

    @Test
    void correctSEHomogeneityEvaluationCaseVillar() {
        genericEvaluation(
            "../data/villar.inp", 
            "../data/villar_default_pressures.csv", 
            "SEHomogeneity", 
            null, 
            0.0
        );
    }

    void genericEvaluation(String inpPath, String pressurePath, String fitnessFunction, List<Double> solutionValues, double expectedFitnessValue) {
        File inpFile = new File(inpPath);
        File pressureFile = new File(pressurePath);
        Problem problem = new Problem(inpFile, StaticUtils.readPressureFile(pressureFile), "Roughness", "0.2", fitnessFunction);

        Bounds<Double> bounds = Bounds.create(0.0, 1.0);
        List<Bounds<Double>> listOfBounds = new ArrayList<>();
        for (int i = 0; i < problem.getNumberOfVariables(); i++) listOfBounds.add(bounds);

        DefaultDoubleSolution solution = new DefaultDoubleSolution(1, 0, listOfBounds);
        for (int i = 0; i < problem.getNumberOfVariables(); i++) {
            solution.variables().set(i, solutionValues == null ? problem.roughnessFactor * problem.getNet().getLink(problem.getPipesIds()[i]).getRoughness() : solutionValues.get(i));
        }

        DoubleSolution sol = problem.evaluate(solution);
        System.out.println("Obtained: " + sol.objectives()[0] + " Expected: " + expectedFitnessValue);
        assert(sol.objectives()[0] == expectedFitnessValue);

        for (File f : (new File("./")).listFiles()) {
            if (f.getName().startsWith(inpFile.getName() + ".log")) {
                f.delete();
            }
        }
    }
}
