package epanet.objectives.impl.squarederror.impl;

import java.util.Map;

import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.structures.Node;

import epanet.StaticUtils;
import epanet.objectives.impl.squarederror.SquaredError;

public class SEHomogeneity extends SquaredError {

    public SEHomogeneity(Network net, Map<String, Double[]> realPressures) {
        super(net, realPressures);
    }

    @Override
    public double run(Double[] x, HydraulicReader hydReader) {

        Map<Node, Double[]> errors = super.getErrors(hydReader);
        Double[] means = new Double[errors.size()];

        int cnt = 0;
        for (Map.Entry<Node, Double[]> pair : errors.entrySet()){
            double sum = 0.0;
            for (Double err : pair.getValue()) {
                sum += err;
            }
            means[cnt] = sum/pair.getValue().length;
            cnt++;
        }

        return StaticUtils.calculateSD(means);
    }
    
}
