package epanet.objectives;

import org.addition.epanet.hydraulic.io.HydraulicReader;

public interface FitnessFunction {
    public double run(Double[] x, HydraulicReader hydReader);
}