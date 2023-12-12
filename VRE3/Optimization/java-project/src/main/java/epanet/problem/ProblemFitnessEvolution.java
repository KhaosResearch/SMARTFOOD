package epanet.problem;

import java.io.File;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import com.google.common.util.concurrent.AtomicDoubleArray;

public class ProblemFitnessEvolution extends Problem {
    // Fitness evolution
    private AtomicDoubleArray progressiveValues;
    private ArrayList<Double>[] generationFitness;
    private AtomicInteger parallelCount;
    private int populationSize;

    public ProblemFitnessEvolution(File inpFile, Map<String, Double[]> realPressures, String strVariables, String strVarLimits, String strFitnessFormulas) {
        super(inpFile, realPressures, strVariables, strVarLimits, strFitnessFormulas);
        
        // Fitness evolution
        this.parallelCount = new AtomicInteger();
        this.populationSize = 0;
        this.progressiveValues = new AtomicDoubleArray(this.getNumberOfObjectives());
        this.generationFitness = new ArrayList[this.getNumberOfObjectives()];
        for (int i = 0; i < this.getNumberOfObjectives(); i++) {
            this.progressiveValues.set(i, Double.MAX_VALUE);
            this.generationFitness[i] = new ArrayList<>();
        }
    }

    @Override
    public DoubleSolution createSolution() {
        DoubleSolution solution = super.createSolution();
        this.populationSize += 1;
        return solution;
    }

    @Override
    public DoubleSolution evaluate(DoubleSolution solution) {

        DoubleSolution result = super.evaluate(solution);

        int cnt = parallelCount.incrementAndGet();
        for (int i = 0; i < this.getNumberOfObjectives(); i++){
            double currentMin = progressiveValues.get(i);
            if (solution.objectives()[i] < currentMin) {
                progressiveValues.compareAndSet(i, currentMin, solution.objectives()[i]);
            }
            if (cnt % populationSize == 0){
                generationFitness[i].add(progressiveValues.get(i));
            }
        }

        return result;

    }

    public Map<String, Double[]> getFitnessEvolution() {
        Map<String, Double[]> fitnessEvolution = new HashMap<String, Double[]>();
        for (int i = 0; i < this.getNumberOfObjectives(); i++) {
            fitnessEvolution.put("F" + i, this.generationFitness[i].toArray(new Double[this.generationFitness[i].size()]));
        }
        return fitnessEvolution;
    }
}
