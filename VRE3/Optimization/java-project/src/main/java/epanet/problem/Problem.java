package epanet.problem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.structures.Link;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import epanet.StaticUtils;
import epanet.objectives.FitnessFunction;

public class Problem extends AbstractDoubleProblem {
    private File inpFile;
    private Network net;
    private Logger log;
    private Map<String, Integer> order;
    public double roughnessFactor;
    protected FitnessFunction[] fitnessFunctions;
    private Collection<Link> pipes;

    public Problem (File inpFile, Map<String, Double[]> realPressures, String strVariables, String strVarLimits, String strFitnessFormulas) {

        // Logger
        Logger log = Logger.getLogger("EA EPANET");
        log.setLevel(Level.OFF);

        // Configure logger
        FileHandler fileHandler;
        try {  
            fileHandler = new FileHandler("./" + inpFile.getName() + ".log");  
            log.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();  
            fileHandler.setFormatter(formatter); 
            log.setUseParentHandlers(false);

        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }
        
        // Read INP file
        Network net = StaticUtils.readInpFile(inpFile, log);

        // Save log, inpFile and network as attributes of our problem class so that they can be consulted during the evaluation
        this.log = log;
        this.inpFile = inpFile;
        this.net = net;

        // Get conversion factors
        this.roughnessFactor = StaticUtils.getRoughnessFactor(net);

        // Check if variables are implemented and record their order in the chromosome
        String[] variables = strVariables.split(";");
        Map<String, Integer> order = new HashMap<String, Integer>();
        ArrayList<String> implementedVariables = new ArrayList<>(Arrays.asList("roughness", "minorloss"));
        for (int i = 0; i < variables.length; i++) {
            String variable = variables[i].toLowerCase();
            if (!implementedVariables.contains(variable)) {
                throw new RuntimeException("Variable " + variable + " is not implemented");
            }
            order.put(variable, i);
        }
        this.order = order;

        // We establish the usual parameters of an optimization problem
        // Variables
        this.pipes = StaticUtils.getPipesFromNet(net);
        setNumberOfVariables(variables.length * this.pipes.size());

        // Objectives
        String[] formulas = strFitnessFormulas.split(";");
        this.fitnessFunctions = new FitnessFunction[formulas.length];
        for (int i = 0; i < formulas.length; i++) {
            this.fitnessFunctions[i] = StaticUtils.getCompositeFitnessFunction(formulas[i], order, net, pipes, realPressures);
        }
        setNumberOfObjectives(this.fitnessFunctions.length);

        // Name
        setName("Problem");

        // Lower and upper limits of values
        // Check that the length of the limits is the same as the length of the variables.
        String[] limits = strVarLimits.split(";");
        if (variables.length != limits.length) {
            throw new RuntimeException("The number of limits must be equal to the number of variables.");
        }

        // Instantiate the jMetal bounds vectors
        List<Double> lowerLimit = new ArrayList<>(getNumberOfVariables());
        List<Double> upperLimit = new ArrayList<>(getNumberOfVariables());

        // For each variable, limits are set
        for (int i = 0; i < variables.length; i++) {
            double limit = Double.parseDouble(limits[i]);
            double upperFactor, lowerFactor;
            if (limit == -1) {
                upperFactor = 500;
                lowerFactor = 0.002;
            } else if (limit > 0 && limit < 1) {
                upperFactor = 1.0 + limit;
                lowerFactor = 1.0 - limit;
            } else {
                throw new RuntimeException("Limits are percentage values bounded between 0 and 1 (except for specifying no limit with the value -1)");
            }
            for (Link pipe: this.pipes){
                if (variables[i].equalsIgnoreCase("roughness")) {
                    double simRoughness = this.roughnessFactor * pipe.getRoughness();
                    lowerLimit.add(simRoughness*lowerFactor);
                    upperLimit.add(simRoughness*upperFactor);
                } else if (variables[i].equalsIgnoreCase("minorloss")) {
                    double simMinorloss = pipe.getKm();
                    lowerLimit.add(simMinorloss*lowerFactor);
                    upperLimit.add(simMinorloss*upperFactor);
                }
            };
        }
        setVariableBounds(lowerLimit, upperLimit);
    }

    @Override
    public DoubleSolution evaluate(DoubleSolution solution) {
        
        Double[] x = new Double[getNumberOfVariables()];
        for (int i = 0; i < getNumberOfVariables(); i++) {
            x[i] = solution.variables().get(i);
        }

        HydraulicReader hydReader = StaticUtils.makeSimulation(x, inpFile, log, order);
        for (int i = 0; i < fitnessFunctions.length; i++){
            solution.objectives()[i] = fitnessFunctions[i].run(x, hydReader);
        }

        return solution;
    }

    public Network getNet() {
        return net;
    }

    public Map<String, Integer> getOrder() {
        return order;
    }

    public Collection<Link> getPipes() {
        return pipes;
    }

    public String[] getPipesIds(){
        String[] res = new String[this.pipes.size()];
        Iterator<Link> pipeIterator = this.pipes.iterator();
        for (int i = 0; i < res.length; i++) {
            res[i] = pipeIterator.next().getId();
        }
        return res;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }
    
}
