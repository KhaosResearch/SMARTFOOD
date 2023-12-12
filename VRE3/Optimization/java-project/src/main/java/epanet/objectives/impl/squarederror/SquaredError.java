package epanet.objectives.impl.squarederror;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.addition.epanet.hydraulic.io.AwareStep;
import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Node;

import epanet.objectives.FitnessFunction;

public abstract class SquaredError implements FitnessFunction {
    protected Map<String, Double[]> realPressures;
    protected Network net;

    public SquaredError(Network net, Map<String, Double[]> realPressures) {
        this.realPressures = realPressures;
        this.net = net;
    }

    protected Map<Node, Double[]> getErrors(HydraulicReader hydReader) {
        
        Map<Node, Double[]> errors = new HashMap<>();

        PropertiesMap pMap = this.net.getPropertiesMap();
        for (Map.Entry<String, Double[]> real : this.realPressures.entrySet()) {
            Node node = this.net.getNode(real.getKey());
            int id = new ArrayList<>(this.net.getNodes()).indexOf(node);
            Double[] nodeErrors = new Double[real.getValue().length];
            int cnt = 0;

            try {
                for (long time = pMap.getRstart(); time <= pMap.getDuration(); time += pMap.getRstep()) {
                    AwareStep step = hydReader.getStep(time);
                    double simulatedPressure = step.getNodePressure(id, node, this.net.getFieldsMap());
                    nodeErrors[cnt] = Math.pow(simulatedPressure - real.getValue()[cnt], 2);
                    cnt ++;
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }

            errors.put(node, nodeErrors);
        }

        return errors;
    }
    
}
