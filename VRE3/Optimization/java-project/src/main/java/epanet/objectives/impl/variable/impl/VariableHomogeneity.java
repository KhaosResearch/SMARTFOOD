package epanet.objectives.impl.variable.impl;

import java.util.Collection;
import java.util.Iterator;

import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.network.structures.Link;

import epanet.StaticUtils;
import epanet.objectives.FitnessFunction;

public class VariableHomogeneity implements FitnessFunction {
    private int position;
    private double[] initialValues;

    public VariableHomogeneity(Collection<Link> pipes, String variable, int position){

        // Get initial values of the variable in original network
        double[] initialValues = new double[pipes.size()];
        Iterator<Link> pipeIterator = pipes.iterator();
        for (int i = 0; i < pipes.size(); i++) {
            switch (variable) {
                case "roughness":
                    initialValues[i] = pipeIterator.next().getRoughness(); 
                    break;
                case "minorloss":
                    initialValues[i] = pipeIterator.next().getKm();
                    break;
                default:
                    throw new RuntimeException("Variable " + variable + " is not implemented");
            }
        }
        
        this.position = position;
        this.initialValues = initialValues;
    }

    @Override
    public double run(Double[] x, HydraulicReader hydReader) {
        int len = initialValues.length;
        Double[] change = new Double[len];
        for (int i = 0; i < len; i++) {
            change[i] = Math.abs(1 - x[i + (position * len)]/initialValues[i]);
        }

        return StaticUtils.calculateSD(change);
    }

}
