package epanet.objectives.impl.squarederror.impl;

import java.util.Map;

import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.structures.Node;

import epanet.objectives.impl.squarederror.SquaredError;

public class SSE extends SquaredError {

    public SSE(Network net, Map<String, Double[]> realPressures) {
        super(net, realPressures);
    }

    @Override
    public double run(Double[] x, HydraulicReader hydReader) {

        double sumSquaredError = 0.0;

        Map<Node, Double[]> errors = super.getErrors(hydReader);
        for (Map.Entry<Node, Double[]> pair : errors.entrySet()){
            for (Double err : pair.getValue()) {
                sumSquaredError += err;
            }
        }

        return sumSquaredError;
    }

}
