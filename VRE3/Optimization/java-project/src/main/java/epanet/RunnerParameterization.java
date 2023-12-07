package epanet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import epanet.problem.Problem;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "RunnerParameterization", description = "Run Parameterization with specified network IDs.", mixinStandardHelpOptions = true)
public class RunnerParameterization implements Runnable {

    @Option(names = {"--input-zip"}, description = "ZIP file with the different .inp (irrigation systems) and .csv (real pressures). The files will be paired for sharing the same prefix.", required = true)
    private String inputZip;

    @Option(names = {"--output-dir"}, description = "Folder where the ZIP output file will be generated.", defaultValue = "/mnt/shared/")
    private String outputDir;

    @Option(names = {"--str-variables"}, description = "Irrigation system variables separated by semicolon that want to be optimized to match real pressures with simulated. Possible values: Roughness, Minorloss", defaultValue = "Roughness;Minorloss")
    private String strVariables;

    @Option(names = {"--str-var-limits"}, description = "Parts per unit that represents the possible margin of values that each variable can take with respect to the pre-existing ones in the .inp input file. The value -1 represents the absence of limits for the variable in question.", defaultValue = "0.2;-1")
    private String strVarLimits;

    @Option(names = {"--str-fitness-formulas"}, description = "Objectives to optimize separated by semicolon. Possible values: SSE, SEHomogeneity, RoughnessHomogeneity, MinorlossHomogeneity", defaultValue = "SSE;SEHomogeneity")
    private String strFitnessFormulas;

    @Option(names = {"--independent-runs"}, description = "Number of independent runs", defaultValue = "7")
    private int independentRuns;

    @Override
    public void run() {

        List<File> inpFiles = new ArrayList<>();
        List<File> pressureFiles = new ArrayList<>();
        try {
            ZipFile zipFile = new ZipFile(inputZip);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while(entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.endsWith(".inp")) {
                    String pressureFileName = entryName.replace(".inp", "_real_pressures.csv");
                    if (zipFile.getEntry(pressureFileName) != null) {
                        File inpFile = new File(System.getProperty("java.io.tmpdir"), entryName);
                        File pressureFile = new File(System.getProperty("java.io.tmpdir"), pressureFileName);
                        Files.copy(zipFile.getInputStream(entry), inpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Files.copy(zipFile.getInputStream(zipFile.getEntry(pressureFileName)), pressureFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        inpFiles.add(inpFile);
                        pressureFiles.add(pressureFile);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
        for (int i = 0; i < inpFiles.size(); i++) {
            Problem problem = new Problem(inpFiles.get(i), StaticUtils.readPressureFile(pressureFiles.get(i)), strVariables, strVarLimits, strFitnessFormulas);
            problem.setName(FilenameUtils.removeExtension(inpFiles.get(i).getName()));
            problemList.add(new ExperimentProblem<>(problem));
        }

        List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
            configureAlgorithmList(problemList, independentRuns);

        String experimentBaseDirectory = outputDir + "/parameterization";
        Experiment<DoubleSolution, List<DoubleSolution>> experiment =
                new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("ComputingReferenceParetoFronts")
                        .setAlgorithmList(algorithmList)
                        .setProblemList(problemList)
                        .setExperimentBaseDirectory(experimentBaseDirectory)
                        .setOutputParetoFrontFileName("FUN")
                        .setOutputParetoSetFileName("VAR")
                        .setReferenceFrontDirectory(experimentBaseDirectory + "/ComputingReferenceParetoFronts/referenceFronts")
                        .setIndicatorList(Arrays.asList(
                                new Epsilon(),
                                new Spread(),
                                new GenerationalDistance(),
                                new PISAHypervolume(),
                                new InvertedGenerationalDistance(),
                                new InvertedGenerationalDistancePlus()))
                        .setIndependentRuns(independentRuns)
                        .setNumberOfCores(Runtime.getRuntime().availableProcessors() - 1)
                        .build();

        try {
            new ExecuteAlgorithms<>(experiment).run();
            new GenerateReferenceParetoSetAndFrontFromDoubleSolutions(experiment).run();
            new ComputeQualityIndicators<>(experiment).run();
            new GenerateLatexTablesWithStatistics(experiment).run();
            new GenerateFriedmanHolmTestTables<>(experiment).run();
            new GenerateWilcoxonTestTablesWithR<>(experiment).run();
            new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(2).run();
            new GenerateHtmlPages<>(experiment).run() ;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Compress the experiment base folder and delete it
        try {
            zipFolder(experimentBaseDirectory, experimentBaseDirectory + ".zip");
            FileUtils.deleteDirectory(new File(experimentBaseDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new RunnerParameterization());
        commandLine.execute(args);
    }

    public static void zipFolder(String sourceFolderPath, String zipFilePath) throws IOException {
        File sourceFolder = new File(sourceFolderPath);
        File zipFile = new File(zipFilePath);

        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(zipFile)) {
            zos.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
            FileUtils.copyDirectory(sourceFolder, zipFile);
            zos.finish();
        }
    }

    static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
          List<ExperimentProblem<DoubleSolution>> problemList, int independentRuns) {

        List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
        int populationSize = 100;
        int maxEvaluations = 500000;

        for (int run = 0; run < independentRuns; run++) {

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