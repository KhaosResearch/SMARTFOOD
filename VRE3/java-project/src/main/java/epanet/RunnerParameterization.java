package epanet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.gde3.GDE3Builder;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.algorithm.multiobjective.smpso.SMPSOBuilder;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.GenerateBoxplotsWithR;
import org.uma.jmetal.lab.experiment.component.impl.GenerateFriedmanHolmTestTables;
import org.uma.jmetal.lab.experiment.component.impl.GenerateHtmlPages;
import org.uma.jmetal.lab.experiment.component.impl.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.component.impl.GenerateReferenceParetoSetAndFrontFromDoubleSolutions;
import org.uma.jmetal.lab.experiment.component.impl.GenerateWilcoxonTestTablesWithR;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.operator.crossover.impl.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import epanet.problem.Problem;

public class RunnerParameterization {
    private static final int INDEPENDENT_RUNS = 7;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new JMetalException("It is necessary to specify at least one problem id");
        }
        String[] networkIds = args;
        String experimentBaseDirectory = "./pareto_fronts";

        File[] inpFiles = new File[networkIds.length];
        File[] pressureFiles = new File[networkIds.length];
        for (int i = 0; i < networkIds.length; i++) {
            inpFiles[i] = new File(networkIds[i] + ".inp");
            pressureFiles[i] = new File(networkIds[i] + "_real_pressures.csv");
        }

        String strVariables = "Roughness;Minorloss";
        String strVarLimits = "0.2;-1";
        String strFitnessFormulas = "SSE;RoughnessHomogeneity";

        List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
        for (int i = 0; i < networkIds.length; i++) {
            Problem problem = new Problem(inpFiles[i], StaticUtils.readPressureFile(pressureFiles[i]), strVariables, strVarLimits, strFitnessFormulas);
            problem.setName(new File(networkIds[i]).getName());
            problemList.add(new ExperimentProblem<>(problem));
        }

        List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
            configureAlgorithmList(problemList);

        Experiment<DoubleSolution, List<DoubleSolution>> experiment =
                new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("ComputingReferenceParetoFronts-villar")
                        .setAlgorithmList(algorithmList)
                        .setProblemList(problemList)
                        .setExperimentBaseDirectory(experimentBaseDirectory)
                        .setOutputParetoFrontFileName("FUN")
                        .setOutputParetoSetFileName("VAR")
                        .setReferenceFrontDirectory(experimentBaseDirectory + "/ComputingReferenceParetoFronts-villar/referenceFronts")
                        .setIndicatorList(Arrays.asList(
                                new Epsilon(),
                                new Spread(),
                                new GenerationalDistance(),
                                new PISAHypervolume(),
                                new InvertedGenerationalDistance(),
                                new InvertedGenerationalDistancePlus()))
                        .setIndependentRuns(INDEPENDENT_RUNS)
                        .setNumberOfCores(Runtime.getRuntime().availableProcessors())
                        .build();

        new ExecuteAlgorithms<>(experiment).run();
        new GenerateReferenceParetoSetAndFrontFromDoubleSolutions(experiment).run();
        new ComputeQualityIndicators<>(experiment).run();
        new GenerateLatexTablesWithStatistics(experiment).run();
        new GenerateFriedmanHolmTestTables<>(experiment).run();
        new GenerateWilcoxonTestTablesWithR<>(experiment).run();
        new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(2).run();
        new GenerateHtmlPages<>(experiment).run() ;
    }

    static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
          List<ExperimentProblem<DoubleSolution>> problemList) {

        List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
        int populationSize = 100;
        int maxEvaluations = 500000;

        for (int run = 0; run < INDEPENDENT_RUNS; run++) {

            // 1. NSGAII
            for (ExperimentProblem<DoubleSolution> experimentProblem : problemList) {
                Algorithm<List<DoubleSolution>> algorithm 
                    = new NSGAIIBuilder<>(experimentProblem.getProblem(), 
                                        new SBXCrossover(0.9, 20.0), 
                                        new PolynomialMutation(0.1, 20.0), 
                                        populationSize)
                        .setSelectionOperator(new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>()))
                        .setMaxEvaluations(maxEvaluations)
                        .build();
                algorithms.add(new ExperimentAlgorithm<>(algorithm, "NSGAII-C090-M010", experimentProblem, run));
            }

            // 2. SMPSO
            for (ExperimentProblem<DoubleSolution> experimentProblem : problemList) {
                BoundedArchive<DoubleSolution> archive = new CrowdingDistanceArchive<>(populationSize);
                Algorithm<List<DoubleSolution>> algorithm 
                    = new SMPSOBuilder((DoubleProblem) experimentProblem.getProblem(), archive)
                        .setMutation(new PolynomialMutation(0.1, 20.0))
                        .setMaxIterations(maxEvaluations / populationSize)
                        .setSwarmSize(populationSize)
                        .setSolutionListEvaluator(new SequentialSolutionListEvaluator<DoubleSolution>())
                        .build();
                algorithms.add(new ExperimentAlgorithm<>(algorithm, "SMPSO-M010", experimentProblem, run));
            }

            // 3. MOEAD
            for (ExperimentProblem<DoubleSolution> experimentProblem : problemList) {
                Algorithm<List<DoubleSolution>> algorithm
                    = new MOEADBuilder(experimentProblem.getProblem(), MOEADBuilder.Variant.MOEAD)
                        .setCrossover(new DifferentialEvolutionCrossover())
                        .setMutation(new PolynomialMutation(0.1, 20.0))
                        .setMaxEvaluations(maxEvaluations)
                        .setPopulationSize(populationSize)
                        .setResultPopulationSize(populationSize)
                        .setNeighborhoodSelectionProbability(0.9)
                        .setMaximumNumberOfReplacedSolutions(2)
                        .setNeighborSize(20)
                        .build();
                algorithms.add(new ExperimentAlgorithm<>(algorithm, "MOEAD-M010", experimentProblem, run));
            }

            // 4. GDE3
            for (ExperimentProblem<DoubleSolution> experimentProblem : problemList) {
                Algorithm<List<DoubleSolution>> algorithm
                    = new GDE3Builder((DoubleProblem) experimentProblem.getProblem())
                        .setCrossover(new DifferentialEvolutionCrossover())
                        .setMaxEvaluations(maxEvaluations)
                        .setPopulationSize(populationSize)
                        .setSolutionSetEvaluator(new SequentialSolutionListEvaluator<>())
                        .build();
                algorithms.add(new ExperimentAlgorithm<>(algorithm, "GDE3", experimentProblem, run));
            }
        }
        return algorithms;
    }
}