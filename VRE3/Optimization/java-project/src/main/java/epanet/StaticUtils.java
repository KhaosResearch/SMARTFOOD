package epanet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.io.FileWriter;
import java.io.IOException;

import org.addition.epanet.hydraulic.HydraulicSim;
import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.network.FieldsMap.Type;
import org.addition.epanet.network.io.input.InputParser;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Link.LinkType;
import org.addition.epanet.network.Network;
import org.addition.epanet.util.ENException;

import epanet.objectives.FitnessFunction;
import epanet.objectives.impl.squarederror.impl.SEHomogeneity;
import epanet.objectives.impl.squarederror.impl.SSE;
import epanet.objectives.impl.variable.impl.VariableHomogeneity;

public final class StaticUtils {

    public static Network readInpFile(File inpFile, Logger log) {
        // Create the necessary variables to be able to import the input map
        InputParser parserINP = InputParser.create(Network.FileType.INP_FILE, log);
        Network net = new Network();

        // Read input INP file and parse its contents to network
        try {
            parserINP.parse(net, inpFile);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return net;
    }

    public static double calculateSD(Double[] numArray) {

        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for(double num : numArray) {
            sum += num;
        }

        double mean = sum/length;
        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);

    }

    public static HydraulicReader makeSimulation(Double[] x, File inpFile, Logger log, Map<String, Integer> order) {
        // Make a copy of our network
        Network tempNet = readInpFile(inpFile, log);

        // Include new values
        for (String key : order.keySet()) {
            Collection<Link> pipes = getPipesFromNet(tempNet);
            Iterator<Link> pipeIterator = pipes.iterator();
            switch (key) {
                case "roughness":
                    for (int i = 0; i < pipes.size(); i++) {
                        pipeIterator.next().setRoughness(x[i + (order.get(key) * pipes.size())]);
                    }
                    break;
                case "minorloss":
                    for (int i = 0; i < pipes.size(); i++) {
                        pipeIterator.next().setKm(x[i + (order.get(key) * pipes.size())]);
                    }
                    break;
                default:
                    throw new RuntimeException("Variable " + key + " is not implemented");
            }
            
        }

        // Run simulation
        HydraulicReader hydReader;
        try {

            // Manage negative pressures warnings
            tempNet.getPropertiesMap().setMessageflag(false);

            // We create binary file where the execution results are stored
            String name = UUID.randomUUID().toString();
            File hydFile = File.createTempFile(name, "bin");

            // We created the hydraulic simulation class
            HydraulicSim hydSim = new HydraulicSim(tempNet, log);

            // We run the simulation providing it with the temporary output file
            hydSim.simulate(hydFile);

            // We access the results through a specific Reader of the package
            hydReader = new HydraulicReader(new RandomAccessFile(hydFile, "r"));

            // Delete temp file
            hydFile.delete();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return hydReader;
    }

    public static FitnessFunction getBasicFitnessFunction(String str, Map<String, Integer> order, Network net, Collection<Link> pipes, Map<String, Double[]> realPressures) {
        /** 
         * Function to return a basic FitnessFunction object based on a identifier string 
         */
        
        FitnessFunction res;
        String variable;
        switch (str.toLowerCase()) {
            case "sse":
                res = new SSE(net, realPressures);
                break;
            case "sehomogeneity":
                res = new SEHomogeneity(net, realPressures);
                break;
            case "roughnesshomogeneity":
                variable = "roughness";
                if (!order.containsKey(variable)) {
                    throw new RuntimeException("The homogeneity of a variable that is not intended to be optimised cannot be measured. The function " 
                        + str.toLowerCase() + " has been chosen but the variable " + variable + " has not been specified.");
                }
                res = new VariableHomogeneity(pipes, variable, order.get(variable));
                break;
            case "minorlosshomogeneity":
                variable = "minorloss";
                if (!order.containsKey(variable)) {
                    throw new RuntimeException("The homogeneity of a variable that is not intended to be optimised cannot be measured. The function " 
                        + str.toLowerCase() + " has been chosen but the variable " + variable + " has not been specified.");
                }
                res = new VariableHomogeneity(pipes, variable, order.get(variable));
                break;
            default:
                throw new RuntimeException("The evaluation term " + str + " is not implemented.");
        }
        return res;
    }

    public static FitnessFunction getCompositeFitnessFunction(String formula, Map<String, Integer> order, Network net, Collection<Link> pipes, Map<String, Double[]> realPressures) {
        /**
         * Function to return a composite FitnessFunction object based on a formula string
         */
        
        FitnessFunction function;

        String[] subformulas = formula.split("\\+");
        if (subformulas.length == 1 && subformulas[0].split("\\*").length == 1) {
            function = getBasicFitnessFunction(formula, order, net, pipes, realPressures);
        } else {
            FitnessFunction[] functions = new FitnessFunction[subformulas.length];
            Double[] weights = new Double[subformulas.length];
            double totalWeight = 0;

            for (int j = 0; j < subformulas.length; j++) {
                String[] tuple = subformulas[j].split("\\*");
                if (tuple.length != 2) {
                    throw new RuntimeException("Function specified with improper formatting. Remember to separate the name of the terms by the symbol +, and assign their weight by preceding them with a decimal followed by the symbol *.");
                }

                functions[j] = getBasicFitnessFunction(tuple[1], order, net, pipes, realPressures);
                try {
                    weights[j] = Double.parseDouble(tuple[0]);
                    totalWeight += weights[j];
                } catch (Exception e) {
                    throw new RuntimeException("The weight " + tuple[0] + " assigned to term " + tuple[1] + " is invalid.");
                }
            }

            if (Math.abs(totalWeight - 1.0) > 0.01) {
                throw new RuntimeException("The weights of all the terms in the formula must add up to 1. Currently total " + totalWeight);
            }

            function = (Double[] x, HydraulicReader hydReader) -> {
                double res = 0;
                for (int j = 0; j < functions.length; j++) {
                    res += weights[j] * functions[j].run(x, hydReader);
                }
                return res;
            };
        }

        return function;
    }

    public static Collection<Link> getPipesFromNet(Network net){
        Collection<Link> links = new HashSet<>(net.getLinks());
        links.removeIf(link -> (link.getType() != LinkType.PIPE));
        Collection<Link> pipes = new ArrayList<>();
        links.forEach(link -> {pipes.add(net.getLink(link.getId()));});
        return pipes;
    }

    public static double getRoughnessFactor(Network net){
        String flowUnits;
        try {
            flowUnits = net.getFieldsMap().getField(Type.FLOW).getUnits();
        } catch (ENException e) {
            throw new RuntimeException(e.getMessage());
        }

        double factor = 1.0;
        switch(flowUnits){
            case "LPS":
                break;
            case "GPM":
                factor = 0.01;
                break;
            default:
                throw new RuntimeException("Flow unit " + flowUnits + " not implemented");
        }

        return factor;
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Map<String, Double[]> readPressureFile(File pressureFile){
        Map<String, Double[]> map = new HashMap<String, Double[]>();

        try {
            Scanner sc = new Scanner(pressureFile);
            while(sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] splitLine = line.split(",");
                Double[] pressures = new Double[splitLine.length - 1];
                for (int i = 0; i < splitLine.length-1; i++){
                    pressures[i] = Double.parseDouble(splitLine[i+1]);
                }
                map.put(splitLine[0], pressures);
            }
            sc.close();
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException(fnfe.getMessage());
        }

        return map;
    }

    public static void writeFitnessEvolution(String strFile, Map<String, Double[]> fitnessEvolution) {
        /**
         * This function is responsible for writing the evolution of the 
         * fitness values in an output txt file specified as parameter
         */
        try {
            File outputFile = new File(strFile);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));

            for (Map.Entry<String, Double[]> entry : fitnessEvolution.entrySet()) {
                String strVector = Arrays.toString(entry.getValue());
                bw.write(strVector.substring(1, strVector.length() - 1) + "\n");
            }

            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
