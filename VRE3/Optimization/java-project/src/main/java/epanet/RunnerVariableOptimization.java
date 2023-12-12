package epanet;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.addition.epanet.network.structures.Link;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.algorithm.multiobjective.smpso.SMPSOBuilder;
import org.uma.jmetal.example.AlgorithmRunner;
import org.uma.jmetal.experimental.componentbasedalgorithm.algorithm.singleobjective.geneticalgorithm.GeneticAlgorithm;
import org.uma.jmetal.experimental.componentbasedalgorithm.catalogue.replacement.Replacement;
import org.uma.jmetal.experimental.componentbasedalgorithm.catalogue.replacement.impl.MuPlusLambdaReplacement;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.operator.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedGeneticAlgorithm;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.MultiThreadedSolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.termination.Termination;
import org.uma.jmetal.util.termination.impl.TerminationByEvaluations;

import epanet.jmetal_modifications.SolutionListOutputWithHeader;
import epanet.problem.Problem;
import epanet.problem.ProblemFitnessEvolution;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "RunnerVariableOptimization", description = "Run Variable Optimization using specified parameters.", mixinStandardHelpOptions = true)
public class RunnerVariableOptimization implements Runnable {

    @Option(names = {"--inp-file"}, description = "Path to the INP file with the scenario whose variables want to be optimized to match real pressures with simulated.", required = true)
    private File inpFile;

    @Option(names = {"--pressure-file"}, description = "CSV file with the real pressures of the irrigation system.", required = true)
    private File pressureFile;

    @Option(names = {"--str-variables"}, description = "Irrigation system variables separated by semicolon that want to be optimized. Possible values: Roughness, Minorloss", defaultValue = "Roughness;Minorloss")
    private String strVariables;

    @Option(names = {"--str-var-limits"}, description = "Parts per unit that represents the possible margin of values that each variable can take with respect to the pre-existing ones in the .inp input file. The value -1 represents the absence of limits for the variable in question.", defaultValue = "0.2;-1")
    private String strVarLimits;

    @Option(names = {"--str-fitness-formulas"}, description = "Objectives to optimize separated by semicolon. Possible values: SSE, SEHomogeneity, RoughnessHomogeneity, MinorlossHomogeneity", defaultValue = "SSE;SEHomogeneity;RoughnessHomogeneity")
    private String strFitnessFormulas;

    @Option(names = {"--population-size"}, description = "Population size", defaultValue = "100")
    private int populationSize;

    @Option(names = {"--max-evaluations"}, description = "Max number of evaluations", defaultValue = "25000")
    private int maxEvaluations;

    @Option(names = {"--str-algorithm"}, description = "Algorithm as a string. Possible values: GA-AsyncParallel (mono-objective), NSGAII-AsyncParallel (multi-objective), SMPSO-SyncParallel (multi-objective)", defaultValue = "SMPSO-SyncParallel")
    private String strAlgorithm;

    @Option(names = {"--crossover-probability"}, description = "Crossover probability", defaultValue = "0.9")
    private double crossoverProbability;

    @Option(names = {"--mutation-probability"}, description = "Mutation probability", defaultValue = "0.1")
    private double mutationProbability;

    @Option(names = {"--output-folder"}, description = "Output folder", defaultValue = "/mnt/shared")
    private String outputFolder;

    @Override
    public void run() {
        CrossoverOperator<DoubleSolution> crossover;
        MutationOperator<DoubleSolution> mutation;
        NaryTournamentSelection<DoubleSolution> selection ;
        Replacement<DoubleSolution> replacement;
        Termination termination;
        Problem problem;

        /** Set number of threads */
        int numOfThreads = Runtime.getRuntime().availableProcessors();

        /** Instantiate problem */
        problem = new ProblemFitnessEvolution(inpFile, StaticUtils.readPressureFile(pressureFile), strVariables, strVarLimits, strFitnessFormulas);

        /** Set the crossover operator. */
        double crossoverDistributionIndex = 20.0;
        crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

        /** Set the mutation operator. */
        double mutationDistributionIndex = 20.0;
        mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

        /** Start selection operator. */
        selection = new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

        /**
         * Declare variable to contain the runtime and another to store the last
         * generation of individuals.
         */
        long computingTime;
        List<DoubleSolution> population;

        /** Instantiate some variables needed for the different algorithms. */
        termination = new TerminationByEvaluations(maxEvaluations);
        replacement = new MuPlusLambdaReplacement<>(new ObjectiveComparator<>(0));
        int offspringPopulationSize = populationSize;

        /** Configure the specified evolutionary algorithm. */
        if (problem.getNumberOfObjectives() == 1) {
            if (strAlgorithm.equals("GA-SingleThread")) {
                /** Instantiate the evolutionary algorithm. */
                GeneticAlgorithm<DoubleSolution> algorithm = new GeneticAlgorithm<DoubleSolution>(
                        problem,
                        populationSize,
                        offspringPopulationSize,
                        selection,
                        crossover,
                        mutation,
                        termination);

                /** Execute the designed evolutionary algorithm. */
                algorithm.run();

                /** Extract the total execution time. */
                computingTime = algorithm.getTotalComputingTime();

                /** Extract the population of the last iteration. */
                population = SolutionListUtils.getNonDominatedSolutions(algorithm.getResult());

            } else if (strAlgorithm.equals("GA-AsyncParallel")) {
                /** Activate stopwatch. */
                long initTime = System.currentTimeMillis();

                /** Instantiate the evolutionary algorithm. */
                AsynchronousMultiThreadedGeneticAlgorithm<DoubleSolution> algorithm = new AsynchronousMultiThreadedGeneticAlgorithm<DoubleSolution>(
                        numOfThreads,
                        problem,
                        populationSize,
                        crossover,
                        mutation,
                        selection,
                        replacement,
                        termination);

                /** Execute the designed evolutionary algorithm. */
                algorithm.run();

                /** Stop stopwatch and calculate the total execution time. */
                long endTime = System.currentTimeMillis();
                computingTime = endTime - initTime;

                /** Extract the population of the last iteration. */
                population = SolutionListUtils.getNonDominatedSolutions(algorithm.getResult());

            } else {
                throw new RuntimeException(
                        "The algorithm " + strAlgorithm + " is not available for single-objetive problems.");
            }
        } else {
            if (strAlgorithm.equals("NSGAII-SingleThread")) {
                /** Instantiate the evolutionary algorithm. */
                Algorithm<List<DoubleSolution>> algorithm = new NSGAIIBuilder<>(problem, crossover, mutation,
                        populationSize)
                        .setSelectionOperator(selection)
                        .setMaxEvaluations(maxEvaluations)
                        .build();

                /** Execute the designed evolutionary algorithm. */
                AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

                /** Extract the total execution time. */
                computingTime = algorithmRunner.getComputingTime();

                /** Extract the population of the last iteration. */
                population = algorithm.getResult();

            } else if (strAlgorithm.equals("NSGAII-AsyncParallel")) {
                /** Activate stopwatch. */
                long initTime = System.currentTimeMillis();

                /** Instantiate the evolutionary algorithm. */
                AsynchronousMultiThreadedNSGAII<DoubleSolution> algorithm = new AsynchronousMultiThreadedNSGAII<DoubleSolution>(
                        numOfThreads,
                        problem,
                        populationSize,
                        crossover,
                        mutation,
                        termination);

                /** Execute the designed evolutionary algorithm. */
                algorithm.run();

                /** Stop stopwatch and calculate the total execution time. */
                long endTime = System.currentTimeMillis();
                computingTime = endTime - initTime;

                /** Extract the population of the last iteration. */
                population = SolutionListUtils.getNonDominatedSolutions(algorithm.getResult());

            } else if (strAlgorithm.equals("SMPSO-SingleThread")) {
                /** Create archive */
                BoundedArchive<DoubleSolution> archive = new CrowdingDistanceArchive<>(populationSize);

                /** Instantiate the evolutionary algorithm. */
                Algorithm<List<DoubleSolution>> algorithm = new SMPSOBuilder(problem, archive)
                        .setMutation(mutation)
                        .setMaxIterations(maxEvaluations / populationSize)
                        .setSwarmSize(populationSize)
                        .setSolutionListEvaluator(new SequentialSolutionListEvaluator<DoubleSolution>())
                        .build();

                /** Execute the designed evolutionary algorithm. */
                AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

                /** Extract the total execution time. */
                computingTime = algorithmRunner.getComputingTime();

                /** Extract the population of the last iteration. */
                population = algorithm.getResult();

            } else if (strAlgorithm.equals("SMPSO-SyncParallel")) {
                /** Create archive */
                BoundedArchive<DoubleSolution> archive = new CrowdingDistanceArchive<>(populationSize);

                /** Instantiate the evaluator */
                SolutionListEvaluator<DoubleSolution> evaluator = new MultiThreadedSolutionListEvaluator<DoubleSolution>(
                        numOfThreads);

                /** Instantiate the evolutionary algorithm. */
                Algorithm<List<DoubleSolution>> algorithm = new SMPSOBuilder(problem, archive)
                        .setMutation(mutation)
                        .setMaxIterations(maxEvaluations / populationSize)
                        .setSwarmSize(populationSize)
                        .setSolutionListEvaluator(evaluator)
                        .build();

                /** Execute the designed evolutionary algorithm. */
                AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

                /** Extract the total execution time. */
                computingTime = algorithmRunner.getComputingTime();

                /** Extract the population of the last iteration. */
                population = algorithm.getResult();

                /** Stop the evaluator */
                evaluator.shutdown();

            } else {
                throw new RuntimeException(
                        "The algorithm " + strAlgorithm + " is not available for multi-objetive problems.");
            }
        }

        Collection<Link> pipes = problem.getPipes();
        Iterator<Link> pipeIterator = pipes.iterator();
        double[] diameters = new double[pipes.size()];
        for (int i = 0; i < diameters.length; i++) {
            diameters[i] = pipeIterator.next().getDiameter();
        }
        
        new SolutionListOutputWithHeader(population, strFitnessFormulas.split(";"), problem.getPipesIds(), problem.roughnessFactor, diameters, problem.getOrder())
            .setVarFileOutputContext(new DefaultFileOutputContext(outputFolder + "/VAR.csv", ","))
            .setFunFileOutputContext(new DefaultFileOutputContext(outputFolder + "/FUN.csv", ","))
            .print();

        Map<String, Double[]> fitnessEvolution = ((ProblemFitnessEvolution) problem).getFitnessEvolution();
        StaticUtils.writeFitnessEvolution(outputFolder + "/fitness_evolution.txt", fitnessEvolution);

        System.out.println("Threads used: " + numOfThreads);
        System.out.println("Total execution time: " + computingTime + "ms");
            
        System.exit(0);
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new RunnerVariableOptimization());
        commandLine.execute(args);
    }
}
