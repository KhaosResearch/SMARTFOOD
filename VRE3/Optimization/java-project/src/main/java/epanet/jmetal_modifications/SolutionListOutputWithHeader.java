package epanet.jmetal_modifications;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.fileoutput.FileOutputContext;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;

import epanet.StaticUtils;

public class SolutionListOutputWithHeader extends SolutionListOutput {
    private String[] fitnessFunctionsLabels;
    private String[] pipeLabels;
    private double factor;
    private double[] diameters;
    private Map<String, Integer> order;

    public SolutionListOutputWithHeader(List<? extends Solution<?>> solutionList, String[] fitnessFunctionsLabels, String[] pipeLabels, double factor, double[] diameters, Map<String, Integer> order) {
        super(solutionList);
        this.fitnessFunctionsLabels = fitnessFunctionsLabels;
        this.pipeLabels = pipeLabels;
        this.factor = factor;
        this.diameters = diameters;
        this.order = order;
    }

    @Override
    public void printVariablesToFile(FileOutputContext context, List<? extends Solution<?>> solutionList) {
        BufferedWriter bufferedWriter = context.getFileWriter();

        try {
            if (solutionList.size() > 0) {

                for (int k = 0; k < order.size(); k++){
                    String variable = StaticUtils.getKeyByValue(order, k);
                    bufferedWriter.write((k == 0 ? "" : ",") + variable + "-" + String.join("," + variable + "-", pipeLabels));
                }
                bufferedWriter.newLine();

                int numberOfVariables = pipeLabels.length;
                for (int i = 0; i < solutionList.size(); i++) {

                    for (int k = 0; k < order.size(); k++){
                        boolean last = k == order.size()-1;
                        String variable = StaticUtils.getKeyByValue(order, k);
                        switch (variable){
                            case "roughness":
                                for (int j = 0; j < numberOfVariables - 1; j++) {
                                    bufferedWriter.write("" + (double)solutionList.get(i).variables().get(j + k*numberOfVariables)/factor + context.getSeparator());
                                }
                                bufferedWriter.write("" + (double)solutionList.get(i).variables().get((1 + k) * numberOfVariables - 1)/factor + (last ? "" : context.getSeparator()));
                                break;
                            case "minorloss":
                                for (int j = 0; j < numberOfVariables - 1; j++) {
                                    bufferedWriter.write("" + ((double)(solutionList.get(i).variables().get(j + k*numberOfVariables)) * Math.pow(diameters[j], 4.0) / 0.02517) + context.getSeparator());
                                }
                                bufferedWriter.write("" + ((double)(solutionList.get(i).variables().get((1 + k) * numberOfVariables - 1)) * Math.pow(diameters[numberOfVariables - 1], 4.0) / 0.02517) + (last ? "" : context.getSeparator()));
                                break;
                            default:
                                throw new RuntimeException("Variable " + variable + " is not implemented");
                        }
                    }

                    bufferedWriter.newLine();
                }
                
            }

            bufferedWriter.close();
        } catch (IOException e) {
            throw new JMetalException("Error writing data ", e);
        }
    }

    @Override
    public void printObjectivesToFile(FileOutputContext context, List<? extends Solution<?>> solutionList){
        BufferedWriter bufferedWriter = context.getFileWriter();

        try {
            if (solutionList.size() > 0) {
                bufferedWriter.write(String.join(",", fitnessFunctionsLabels));
                bufferedWriter.newLine();
                int numberOfObjectives = solutionList.get(0).objectives().length;
                for (int i = 0; i < solutionList.size(); i++) {
                    for (int j = 0; j < numberOfObjectives - 1; j++) {
                        bufferedWriter.write(solutionList.get(i).objectives()[j] + context.getSeparator());
                    }
                    bufferedWriter.write("" + solutionList.get(i).objectives()[numberOfObjectives - 1]);
                    bufferedWriter.newLine();
                }
            }

            bufferedWriter.close();
        } catch (IOException e) {
            throw new JMetalException("Error printing objectives to file: ", e);
        }
    }
    
}