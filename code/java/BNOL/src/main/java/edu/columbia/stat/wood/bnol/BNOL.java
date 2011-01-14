/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.HPYP;
import edu.columbia.stat.wood.bnol.hpyp.IntHPYP;
import edu.columbia.stat.wood.bnol.util.GammaDistribution;
import edu.columbia.stat.wood.bnol.util.IntGeometricDistribution;
import edu.columbia.stat.wood.bnol.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.Pair;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 *
 * @author nicholasbartlett
 */
public class BNOL {

    private TIntObjectHashMap<Machine> machines;
    private S_EmissionDistribution emissionDistributions;

    private int[] machineKeys;
    private int[][] emissions;
    private int[] words;

    private HPYP machineTransitions;
    private HPYP wordDistribution;

    private int H;
    private double b, pForMachineStates;

    private MersenneTwisterFast rng = new MersenneTwisterFast(9);

    public BNOL(int[] words, int alphabetSize, int H, double b, double pForMachineStates, double pForMachineTransitions){
        machines = new TIntObjectHashMap();
        emissionDistributions = new S_EmissionDistribution(new MutableDouble(b));

        machineKeys = new int[words.length];
        emissions = new int[words.length][];
        this.words = words;
        

        MutableDouble[] conc = new MutableDouble[2];
        MutableDouble[] disc = new MutableDouble[2];

        conc[0] = new MutableDouble(20);
        conc[1] = new MutableDouble(5);

        disc[0] = new MutableDouble(0.90);
        disc[1] = new MutableDouble(0.95);

        machineTransitions = new IntHPYP(disc, conc, new IntGeometricDistribution(pForMachineTransitions, 0), new GammaDistribution(1,100));
        
        conc = new MutableDouble[11];
        disc = new MutableDouble[11];

        conc[0] = new MutableDouble(20);
        conc[1] = new MutableDouble(2);
        conc[2] = new MutableDouble(1);
        conc[3] = new MutableDouble(1);
        conc[4] = new MutableDouble(1);
        conc[5] = new MutableDouble(1);
        conc[6] = new MutableDouble(1);
        conc[7] = new MutableDouble(1);
        conc[8] = new MutableDouble(1);
        conc[9] = new MutableDouble(1);
        conc[10] = new MutableDouble(1);

        disc[0] = new MutableDouble(.8);
        disc[1] = new MutableDouble(.9);
        disc[2] = new MutableDouble(.95);
        disc[3] = new MutableDouble(.95);
        disc[4] = new MutableDouble(.95);
        disc[5] = new MutableDouble(.95);
        disc[6] = new MutableDouble(.95);
        disc[7] = new MutableDouble(.95);
        disc[8] = new MutableDouble(.95);
        disc[9] = new MutableDouble(.95);
        disc[10] = new MutableDouble(.95);

        wordDistribution = new IntHPYP(disc, conc, new IntUniformDiscreteDistribution(alphabetSize), new GammaDistribution(1,100));

        this.H = H;
        this.b = b;
        this.pForMachineStates = pForMachineStates;
    }

    public void printEmissions(PrintStream ps){
        for(int i = 0; i < words.length; i++){
            ps.println(Arrays.toString(emissions[i]));
        }
    }

    private void initialize(){
        machineKeys[0] = machineTransitions.draw(null);
        int machineState = machineGet(machineKeys[0]).get(emissions, 0);
        emissions[0] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
        emissionDistributions.seat(machineState, emissions[0]);

        for(int i = 1; i < words.length; i++){
            machineKeys[i] = machineTransitions.draw(new int[]{machineKeys[i - 1]});
            machineState = machineGet(machineKeys[i]).get(emissions, i);
            emissions[i] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
            emissionDistributions.seat(machineState, emissions[i]);
        }
    }

    public void sampleEmissions(double temp){
        for(int i = 0; i < words.length; i++){
            sampleEmission(i);
        }
    }

    private void sampleEmission(int index){
        double low = 0.0, high = 1.0;
        double currentProb = emissionProbability(index) + wordEvidence(emissions[index], words[index]);
        double proposedProb = currentProb - 1;
        double randomNumber = rng.nextDouble() * currentProb;

        int[] currentEmission = emissions[index];
        int machineState = machineGet(machineKeys[index]).get(emissions, index);

        while(true){
            Pair<int[], Double> pair = emissionDistributions.generate(machineState, low, high);
            emissions[index] = pair.first();
            proposedProb = emissionProbability(index) + wordEvidence(emissions[index], words[index]);

            if(proposedProb < randomNumber){
                int comparison = compareEmissionAToB(emissions[index], currentEmission);
                if(comparison == 0){
                    high = pair.second();
                } else {
                    low = pair.second();
                }
            } else {
                break;
            }
        }

        emissionDistributions.unseat(machineState, currentEmission);
        emissionDistributions.seat(machineState, emissions[index]);
    }

    /**
     * Figures out which int array is larger.
     * @param a first int array
     * @param b second int array
     * @return 0 if a is larger, 1 if be is larger
     */
    private int compareEmissionAToB(int[] a, int[] b){
        int index = 0;
        while(true){
            if(a[index] > b[index]){
                return 0;
            } else if(b[index] > a[index]){
                return 1;
            }
            index++;
        }
    }

    private double emissionProbability(int index){
        int i = 0;
        double prob = 0.0;
        while(i < H && (index + i) < words.length){
            prob += emissionDistributions.logProbability(machineGet(machineKeys[index + i]).get(emissions, index + i), emissions[index + i]);
            i++;
        }
        return prob;
    }

    private double wordEvidence(int[] emission, int word){
        assert emission[emission.length - 1] == -1 : "last element of emission should be -1";

        int[] context = new int[emission.length - 1];
        System.arraycopy(emission, 0, context, 0, context.length);
        return Math.log(wordDistribution.prob(context, word));
    }

    private Machine machineGet(int key){
        Machine value = machines.get(key);
        if(value == null){
            value = new Machine(key, H, pForMachineStates);
            machines.put(key,value);
        }

        return value;
    }

    private double sampleMachines(double temp){
        double score = 0.0;
        TIntObjectIterator<Machine> iterator = machines.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            iterator.value().sample(emissions, machineKeys, emissionDistributions, 1, temp);
            score += iterator.value().score(emissions, emissionDistributions, machineKeys);
        }
        return score;
    }

    private void sampleMachineKeys(double temp){
        for(int i = 0; i < words.length; i++){
            sampleMachineKey(i, temp);
        }
    }

    private void sampleMachineKey(int index, double temp){
        if(index == 0){
            sampleFirstMachineKey(temp);
        } else if (index == (machineKeys.length - 1)){
            sampleLastMachineKey(temp);
        } else {
            int previousKey = machineKeys[index - 1];
            int currentKey = machineKeys[index];
            int nextKey = machineKeys[index + 1];

            int proposedKey = machineTransitions.generate(previousKey);
            double r = Math.log(machineTransitions.prob(proposedKey, nextKey));
            r += emissionDistributions.logProbability(machineGet(proposedKey).get(emissions, index), emissions[index]);
            r -= Math.log(machineTransitions.prob(currentKey, nextKey));
            r -= emissionDistributions.logProbability(machineGet(currentKey).get(emissions, index), emissions[index]);

            r = Math.exp(r);
            r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

            if(rng.nextBoolean(r)){
                machineKeys[index] = proposedKey;

                machineTransitions.unseat(previousKey, currentKey);
                machineTransitions.unseat(currentKey, nextKey);

                machineTransitions.seat(previousKey, proposedKey);
                machineTransitions.seat(proposedKey, nextKey);
            }
        }
    }

    private void sampleFirstMachineKey(double temp) {
        int currentKey = machineKeys[0], nextKey = machineKeys[1];
        int proposedKey = machineTransitions.generate(null);
        
        double r = Math.log(machineTransitions.prob(proposedKey, nextKey));
        r += emissionDistributions.logProbability(machineGet(proposedKey).get(emissions, 0), emissions[0]);
        r -= Math.log(machineTransitions.prob(currentKey, nextKey));
        r -= emissionDistributions.logProbability(machineGet(currentKey).get(emissions, 0), emissions[0]);
        
        r = Math.exp(r);
        r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

        if (rng.nextBoolean(r)) {
            machineKeys[0] = proposedKey;

            machineTransitions.unseat(null, currentKey);
            machineTransitions.unseat(currentKey, nextKey);

            machineTransitions.seat(null, proposedKey);
            machineTransitions.seat(proposedKey, nextKey);
        }
    }

    private void sampleLastMachineKey(double temp){
        int index = words.length - 1;
        int previousKey = machineKeys[index - 1];
        int currentKey = machineKeys[index];

        int proposedKey = machineTransitions.generate(previousKey);
        double r = emissionDistributions.logProbability(machineGet(proposedKey).get(emissions, index), emissions[index]);
        r -= emissionDistributions.logProbability(machineGet(currentKey).get(emissions, index), emissions[index]);

        r = Math.exp(r);
        r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

        if (rng.nextBoolean(r)) {
            machineKeys[index] = proposedKey;

            machineTransitions.unseat(previousKey, currentKey);

            machineTransitions.seat(previousKey, proposedKey);
        }
    }

    /**
     * Generates data from scratch from the model.
     * @param length number of data points
     * @param alphabetSize word alphabet size
     * @param H length of context dependence
     * @param b parameter controlling complexity of emission distributions, higher is a lower complexity, b must be in (0.0, 1.0]
     * @param pForMachineStates parameter for geometric distribution
     * @param pForMachineTransitions parameter for geometric distribution
     * @return generated binary emission vectors
     */
    public int[][] generateFromScratch(int length, int alphabetSize, int H,  double b, double pForMachineStates, double pForMachineTransitions){
        int[] words = new int[length];
        int[][] emissions = new int[length][];
        int[] machineKeys = new int[length];

        TIntObjectHashMap<Machine> machines = new TIntObjectHashMap();
        machines.put(0, new Machine(0, H, pForMachineStates));

        S_EmissionDistribution emissionDistributions = new S_EmissionDistribution(new MutableDouble(b));

        MutableDouble[] concentrations = new MutableDouble[2];
        MutableDouble[] discounts = new MutableDouble[2];

        concentrations[0] = new MutableDouble(10);
        concentrations[1] = new MutableDouble(1);

        discounts[0] = new MutableDouble(0.2);
        discounts[1] = new MutableDouble(0.6);
        
        HPYP machineTransitions = new IntHPYP(discounts, concentrations, new IntGeometricDistribution(pForMachineTransitions, 0), new GammaDistribution(1,100));

        MutableDouble[] wordConc = new MutableDouble[11];
        MutableDouble[] wordDisc = new MutableDouble[11];
        for(int i = 0; i < wordDisc.length; i++){
            wordDisc[wordDisc.length - 1 - i] = new MutableDouble(Math.pow(0.9, i + 1));
            wordDisc[i] = new MutableDouble(1.0);
        }

        HPYP wordDistribution = new IntHPYP(wordDisc, wordConc, new IntUniformDiscreteDistribution(alphabetSize), new GammaDistribution(1,100));

        machineKeys[0] = 0;

        int machineState;
        for(int i = 0; i < length; i++){
            machineState = machines.get(machineKeys[i]).get(emissions, i);

            emissions[i] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
            emissionDistributions.seat(machineState, emissions[i]);

            words[i] = wordDistribution.draw(emissions[i]);

            if (i < length - 1){
                machineKeys[i + 1] = machineTransitions.draw(new int[]{machineKeys[i]});
                if(machines.get(machineKeys[i+1]) == null){
                    machines.put(machineKeys[i+1], new Machine(machineKeys[i+1], H, pForMachineStates));
                }
            }
        }

        return emissions;
    }

    public static void main(String[] args){
        //int[] words, int alphabetSize, int H, double b, double pForMachineStates, double pForMachineTransitions){
        BNOL bnol = new BNOL(new int[1000], 1000, 10, 0.2, 0.05, 0.05);
        bnol.initialize();

        

        //bnol.printEmissions(System.out);
        //bnol.printMachineKeys(System.out);
        //bnol.generateFromScratch(100, 0.3, 0.1, 0.1);
    }
}
