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

public class RunnerVariableOptimization {
    public static void main(String[] args) {
        CrossoverOperator<DoubleSolution> crossover;
        MutationOperator<DoubleSolution> mutation;
        NaryTournamentSelection<DoubleSolution> selection ;
        Replacement<DoubleSolution> replacement;
        Termination termination;
        Problem problem;

        File inpFile;
        File pressureFile;
        String strVariables;
        String strVarLimits;
        String strFitnessFormulas;
        int populationSize;
        int maxEvaluations;
        String strAlgorithm;
        double crossoverProbability;
        double mutationProbability;
        String outVarFile;
        String outFunFile;
        String outFitnessEvolutionFile;
        if (args.length == 13) {
            inpFile = new File(args[0]);
            pressureFile = new File(args[1]);
            strVariables = args[2];
            strVarLimits = args[3];
            strFitnessFormulas = args[4];
            populationSize = Integer.parseInt(args[5]);
            maxEvaluations = Integer.parseInt(args[6]);
            strAlgorithm = args[7];
            crossoverProbability = Double.parseDouble(args[8]);
            mutationProbability = Double.parseDouble(args[9]);
            outVarFile = args[10];
            outFunFile = args[11];
            outFitnessEvolutionFile = args[12];
        } else {
            throw new RuntimeException("13 input parameters must be provided: inpFile, pressureFile, strVariables, strVarLimits, strFitnessFormulas, populationSize, maxEvaluations, strAlgorithm, crossoverProbability, mutationProbability, outVarFile, outFunFile and outFitnessEvolutionFile.");
        }

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
            if (strAlgorithm.equals("GA-AsyncParallel")) {
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
            .setVarFileOutputContext(new DefaultFileOutputContext(outVarFile, ","))
            .setFunFileOutputContext(new DefaultFileOutputContext(outFunFile, ","))
            .print();

        Map<String, Double[]> fitnessEvolution = ((ProblemFitnessEvolution) problem).getFitnessEvolution();
        StaticUtils.writeFitnessEvolution(outFitnessEvolutionFile, fitnessEvolution);

        System.out.println("Threads used: " + numOfThreads);
        System.out.println("Total execution time: " + computingTime + "ms");
            
        System.exit(0);
    }
}
