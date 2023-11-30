package epanet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.addition.epanet.hydraulic.HydraulicSim;
import org.addition.epanet.hydraulic.io.AwareStep;
import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.io.input.InputParser;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Node;
import org.apache.commons.io.FilenameUtils;

public class RunnerSimulation {
    public static void main(String[] args) {

        // Extract input parameters
        File inpFile;
        String outputFilename;
        File varFile = null;
        String strVariables = "";
        if (args.length > 1 && args.length < 5) {
            inpFile = new File(args[0]);
            outputFilename = args[1];
            if (args.length > 3) {
                varFile = new File(args[2]);
                strVariables = args[3];
            }
        } else {
            throw new RuntimeException("At least two input parameters must be provided: the path to the INP file with the scenario to be analyzed and output file name");
        }

        // Check output filename extension
        String extExp = args.length > 3 ? "zip" : "csv";
        String extPro = FilenameUtils.getExtension(outputFilename);
        if (! extExp.equals(extPro)){
            throw new RuntimeException("The extension of the specified output file was not as expected. Provided: " + extPro + ", expected: " + extExp);
        }

        // Create the necessary variables to be able to import the input map
        Logger log = Logger.getLogger("Test EPANET");
        InputParser parserINP = InputParser.create(Network.FileType.INP_FILE, log);
        Network net = new Network();

        // Read input INP file and parse its contents to network
        try {
            parserINP.parse(net, inpFile);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // Extract property map
        PropertiesMap pMap = net.getPropertiesMap();

        // Get varfile variables if specified
        int reps = 1;
        double[][] variableValues = new double[0][0];
        if (args.length > 3) {
            ArrayList<double[]> tmpVariableValues = new ArrayList<double[]>();
            try {
                Scanner sc = new Scanner(varFile);
                sc.nextLine();
                while(sc.hasNextLine()) {
                    String line = sc.nextLine();
                    String[] splitLine = line.split(",");
                    double[] array = new double[splitLine.length];
                    for (int i = 0; i < splitLine.length; i++) {
                        array[i] = Double.parseDouble(splitLine[i]);
                    }
                    tmpVariableValues.add(array);
                }
                sc.close();
                reps = tmpVariableValues.size();
            } catch (FileNotFoundException fnfe) {
                throw new RuntimeException(fnfe.getMessage());
            }
            variableValues = new double[reps][tmpVariableValues.get(0).length];
            for (int i = 0; i < reps; i++) {
                variableValues[i] = tmpVariableValues.get(i);
            }
        }

        // For each repetition (1 if there is only one INP file and n if there is varFile) the simulation is run
        for (int i = 0; i < reps; i++) {

            // Insert variables if varFile specified
            if (args.length > 3) {
                String[] variables = strVariables.split(";");
                for (int k = 0; k < variables.length; k++) {
                    Collection<Link> pipes = StaticUtils.getPipesFromNet(net);
                    Iterator<Link> pipeIterator = pipes.iterator();

                    switch (variables[k].toLowerCase()) {
                        case "roughness":
                            for (int j = 0; j < pipes.size(); j++) {
                                pipeIterator.next().setRoughness(StaticUtils.getRoughnessFactor(net) * variableValues[i][j + k*pipes.size()]);
                            }
                            break;
                        case "minorloss":
                            for (int j = 0; j < pipes.size(); j++) {
                                Link pipe = pipeIterator.next();
                                pipe.setKm(variableValues[i][j + k*pipes.size()] / (Math.pow(pipe.getDiameter(), 4.0) / 0.02517));
                            }
                            break;
                        default:
                            throw new RuntimeException("Variable " + variables[k] + " is not implemented");
                    }
                }
            }

            // Run simulation and extract pressure values from the result
            try {
                File hydFile = File.createTempFile("hydSim", "bin");
                HydraulicSim hydSim = new HydraulicSim(net, log);
                hydSim.simulate(hydFile);

                HydraulicReader hydReader = new HydraulicReader(new RandomAccessFile(hydFile, "r"));

                File outputFile = null;
                if (args.length > 3) {
                    outputFile = new File(FilenameUtils.removeExtension(outputFilename) + "_" + i + ".csv");
                } else {
                    outputFile = new File(outputFilename);
                }

                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
                for (Node node : net.getNodes()) {
                    ArrayList<String> row = new ArrayList<>();
                    row.add(node.getId());
                    int id = new ArrayList<>(net.getNodes()).indexOf(node);
                    for (long time = pMap.getRstart(); time <= pMap.getDuration(); time += pMap.getRstep()) {
                        AwareStep step = hydReader.getStep(time);
                        row.add(String.valueOf(step.getNodePressure(id, node, net.getFieldsMap())));
                    }
                    bw.write(String.join(",", row) + "\n");
                }
                bw.flush();
                bw.close();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        // If a varFile has been provided, all csv's are compressed into a single zip file.
        if (args.length > 3) {
            try {
                FileOutputStream fos = new FileOutputStream(outputFilename);
                ZipOutputStream zos = new ZipOutputStream(fos);
    
                for (int i = 0; i < reps; i++) {
                    String csvFilename = FilenameUtils.removeExtension(outputFilename) + "_" + i + ".csv";
                    File csvFile = new File(csvFilename);
                    zos.putNextEntry(new ZipEntry(csvFile.getName()));
    
                    byte[] bytes = Files.readAllBytes(Paths.get(csvFilename));
                    zos.write(bytes, 0, bytes.length);
                    zos.closeEntry();

                    csvFile.delete();
                }
    
                zos.close();
    
            } catch (FileNotFoundException ex) {
                System.err.println("A file does not exist: " + ex);
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex);
            }
        }
    }
}