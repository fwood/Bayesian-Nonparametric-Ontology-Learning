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
import java.io.PrintStream;
import java.util.Arrays;

/**
 *
 * @author nicholasbartlett
 */
public class BNOL {
    
    public static void main(String[] args){
        //int[] words, int alphabetSize, int H, double b, double pForMachineStates, double pForMachineTransitions){
        BNOL bnol = new BNOL(new int[10], 1000, 10, 0.2, 0.05, 0.05);
        Pair<int[][], int[]> pair = bnol.generateFromScratch(10000, 1000, 10, 0.2, 0.5, 0.5);


        BNOL b = new BNOL(pair.second(), 1000, 10, 0.5, 0.05, 0.05);
        for(int i = 0; i < 100; i++){
            System.out.println(b.sample(1000) + ", " + b.meanAndVarEmissionLength()[0] + ", " + b.meanAndVarEmissionLength()[1]);
        }

        for(int i = 0; i < 100; i++){
            System.out.println(b.sample(100) + ", " + b.meanAndVarEmissionLength()[0] + ", " + b.meanAndVarEmissionLength()[1]);
        }

        for(int i = 0; i < 100; i++){
            System.out.println(b.sample(10) + ", " + b.meanAndVarEmissionLength()[0] + ", " + b.meanAndVarEmissionLength()[1]);
        }

        for(int i = 0; i < 1000; i++){
            System.out.println(b.sample(1) + ", " + b.meanAndVarEmissionLength()[0] + ", " + b.meanAndVarEmissionLength()[1]);
        }
        
        b.printEmissions(System.out);
    }


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

    private double ll_emissions = 0.0;
    private double ll_machines = 0.0;
    private double ll_machine_transitions = 0.0;
    private double ll_words = 0.0;

    /***********************constructor methods********************************/

    /**
     * Constructor method to initialize parameters of BNOL.
     * @param words word data
     * @param alphabetSize size of alphabet over words
     * @param H length of context dependence in machines
     * @param b parameter in (0,1) determining a priori complexity of word distribution
     * @param pForMachineStates probability of success for geometric distribution at base of machines
     * @param pForMachineTransitions probability of success for geometric distribution at base of machine transitions
     */
    public BNOL(int[] words, int alphabetSize, int H, double b, double pForMachineStates, double pForMachineTransitions){
        machines = new TIntObjectHashMap();
        emissionDistributions = new S_EmissionDistribution(new MutableDouble(b), .1, .2, 1, 1);

        machineKeys = new int[words.length];
        emissions = new int[words.length][];
        this.words = words;
        
        MutableDouble[] conc = new MutableDouble[2];
        MutableDouble[] disc = new MutableDouble[2];

        conc[0] = new MutableDouble(20);
        conc[1] = new MutableDouble(5);

        disc[0] = new MutableDouble(0.90);
        disc[1] = new MutableDouble(0.95);

        machineTransitions = new IntHPYP(disc, conc, new IntGeometricDistribution(pForMachineTransitions, 0), new GammaDistribution(1d,100d));
        
        conc = new MutableDouble[11];
        disc = new MutableDouble[11];

        conc[0] = new MutableDouble(10);
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

        disc[0] = new MutableDouble(.1);
        disc[1] = new MutableDouble(.2);
        disc[2] = new MutableDouble(.3);
        disc[3] = new MutableDouble(.4);
        disc[4] = new MutableDouble(.5);
        disc[5] = new MutableDouble(.6);
        disc[6] = new MutableDouble(.7);
        disc[7] = new MutableDouble(.8);
        disc[8] = new MutableDouble(.9);
        disc[9] = new MutableDouble(.95);
        disc[10] = new MutableDouble(.95);

        wordDistribution = new IntHPYP(disc, conc, new IntUniformDiscreteDistribution(alphabetSize), new GammaDistribution(1d,100d));

        this.H = H;
        this.b = b;
        this.pForMachineStates = pForMachineStates;

        initialize();
    }

    /***********************public methods*************************************/


    public double[] meanAndVarEmissionLength(){
        double m = 0.0, var = 0.0;
        for(int i = 0; i < words.length; i++){
            m += emissions[i].length;
            var += Math.pow(emissions[i].length, 2d);
        }

        m /= words.length;
        var = var / words.length - Math.pow(m, 2);

        return new double[]{m,var};
    }

    public double sample(double temp){
        sampleMachineKeys(temp);

        ll_machine_transitions = machineTransitions.sample(temp);

        ll_machines = sampleMachines(temp);

        ll_emissions = emissionDistributions.sample(10, temp);

        sampleEmissions(temp);

        wordDistribution.sampleSeatingArrangements(10);
        ll_words = wordDistribution.score(true); //wordDistribution.sample(temp);
        
        return ll_emissions + ll_words + ll_machines + ll_machine_transitions;
    }
    
    /**
     * Utility method to print the emission to a print stream
     * @param printStream print stream to print emissions to
     */
    public void printEmissions(PrintStream printStream){
        for(int i = 0; i < words.length; i++){
            printStream.println(Arrays.toString(emissions[i]));
        }
    }

    /**
     * Cycle through emissions and sample each one with a given temperature.
     * @param temp temperature of sampling step
     */
    public void sampleEmissions(double temp){
        for(int i = 0; i < words.length; i++){
            sampleEmission(i);
        }
    }

    /**
     * Cycles through the machines and sample them with a given temperature.
     * @param temp temperature of sampling step
     * @return joint score of all the machines.
     */
    public double sampleMachines(double temp){
        double score = 0.0;
        TIntObjectIterator<Machine> iterator = machines.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            iterator.value().sample(emissions, machineKeys, emissionDistributions, 1, temp);

            assert iterator.value().checkCounts();

            score += iterator.value().score(emissions, emissionDistributions, machineKeys);
        }
        return score;
    }

    /**
     * Cycles through the machine keys and samples them with a given temperature.
     * @param temp temperature of sampling step
     */
    public void sampleMachineKeys(double temp){
        for(int i = 0; i < words.length; i++){
            sampleMachineKey(i, temp);
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
     * @return pair, first of which is generated binary emission vectors, second of which is words
     */
    public Pair<int[][], int[]> generateFromScratch(int length, int alphabetSize, int H,  double b, double pForMachineStates, double pForMachineTransitions){
        TIntObjectHashMap<Machine> machines = new TIntObjectHashMap();
        S_EmissionDistribution emissionDistributions = new S_EmissionDistribution(new MutableDouble(b), .3, .7, 10, .5);

        int[] machineKeys = new int[length];
        int[][] emissions = new int[length][];
        int[] words = new int[length];
        

        MutableDouble[] conc = new MutableDouble[2];
        MutableDouble[] disc = new MutableDouble[2];

        conc[0] = new MutableDouble(20);
        conc[1] = new MutableDouble(5);

        disc[0] = new MutableDouble(0.90);
        disc[1] = new MutableDouble(0.95);

        HPYP machineTransitions = new IntHPYP(disc, conc, new IntGeometricDistribution(pForMachineTransitions, 0), new GammaDistribution(1d,100d));

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

        disc[0] = new MutableDouble(.2);
        disc[1] = new MutableDouble(.3);
        disc[2] = new MutableDouble(.4);
        disc[3] = new MutableDouble(.5);
        disc[4] = new MutableDouble(.6);
        disc[5] = new MutableDouble(.7);
        disc[6] = new MutableDouble(.8);
        disc[7] = new MutableDouble(.9);
        disc[8] = new MutableDouble(.95);
        disc[9] = new MutableDouble(.95);
        disc[10] = new MutableDouble(.95);

        HPYP wordDistribution = new IntHPYP(disc, conc, new IntUniformDiscreteDistribution(alphabetSize), new GammaDistribution(1d,100d));

        machineKeys[0] = machineTransitions.draw(null);
        Machine m = new Machine(machineKeys[0], H, pForMachineStates);
        machines.put(machineKeys[0], m);
        int machineState = m.get(emissions, 0);
        emissions[0] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
        emissionDistributions.seat(machineState, emissions[0]);

        for(int i = 1; i < length; i++){
            machineKeys[i] = machineTransitions.draw(machineKeys[i-1]);
            m = machines.get(machineKeys[i]);
            if(m == null){
                machines.put(machineKeys[i], m = new Machine(machineKeys[i], H, pForMachineStates));
            }
            machineState = m.get(emissions, i);

            emissions[i] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
            emissionDistributions.seat(machineState, emissions[i]);

            words[i] = wordDistribution.draw(emissions[i]);
            //System.out.println(machineState);
        }

        return new Pair(emissions, words);
    }

    /***********************private methods************************************/

    /**
     * Initializes the arrays, assuming the words are set.
     */
    private void initialize(){
        // draw first machine key, create that machine, and get the machine state
        machineKeys[0] = machineTransitions.draw(null);
        int machineState = machineGet(machineKeys[0]).get(emissions, 0);

        // generate first emission and seat it in the emission distribution
        emissions[0] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
        emissionDistributions.seat(machineState, emissions[0]);

        // repeat the generative process forwards until the end
        for(int i = 1; i < words.length; i++){
            //draw next machine key and get the machine state
            machineKeys[i] = machineTransitions.draw(machineKeys[i - 1]);
            machineState = machineGet(machineKeys[i]).get(emissions, i);

            // draw the next emission
            emissions[i] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
            emissionDistributions.seat(machineState, emissions[i]);
        }
    }

    /**
     * Sample the emission indexed by the given index using a slice sampler.
     * @param index index of emission to sample.
     */
    private void sampleEmission(int index){
        double low = 0.0, high = 1.0;

        int[] currentEmission = emissions[index];

        int thisMachineState = machineGet(machineKeys[index]).get(emissions, index);

        int[] currentMachineStates = new int[Math.min(H , words.length -  1 - index)];
        for(int i = 0; i < currentMachineStates.length; i++){
            currentMachineStates[i] = machineGet(machineKeys[index + 1 + i]).get(emissions, index + 1 + i);
        }
        
        double adjustment = - emissionProbability(index) - Math.log(wordDistribution.prob(emissions[index], words[index])) + Math.log(100d);
        double proposedProb;
        double randomNumber = rng.nextDouble() * 100d;

        // do slice sampling
        while(true){
            Pair<int[], Double> pair = emissionDistributions.generate(thisMachineState, low, high);
            emissions[index] = pair.first();

            proposedProb = Math.exp(emissionProbability(index) + Math.log(wordDistribution.prob(emissions[index], words[index])) + adjustment);

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

        int[] newMachineStates = new int[Math.min(H, words.length - 1 - index)];
        for(int i = 0; i < newMachineStates.length; i++){
            newMachineStates[i] = machineGet(machineKeys[index + 1 + i]).get(emissions, index + 1 + i);
        }

        emissionDistributions.unseat(thisMachineState, currentEmission);
        emissionDistributions.seat(thisMachineState, emissions[index]);

        for(int i = 0; i < newMachineStates.length; i++){
            if(newMachineStates[i] != currentMachineStates[i]){
                emissionDistributions.unseat(currentMachineStates[i],emissions[index + 1 + i]);
                emissionDistributions.seat(newMachineStates[i],emissions[index + 1 + i]);
            }
        }
    }

    /**
     * Gets the log probability of the markov blanket of emissions for a
     * specified index.
     * @param index index around which to get markov blanket log probability
     * @return log probability of markov blanket
     */
    private double emissionProbability(int index){
        int i = 0;
        double prob = 0.0;
        while(i < H && (index + i) < words.length){
            prob += emissionDistributions.logProbability(machineGet(machineKeys[index + i]).get(emissions, index + i), emissions[index + i]);
            i++;
        }
        return prob;
    }

    /**
     * Figures out which int array is larger.  Works because last entry of any
     * emission is a -1.
     * @param a first int array
     * @param b second int array
     * @return 0 if a is larger, 1 if be is larger
     */
    private int compareEmissionAToB(int[] a, int[] b) {
        if (a.length < b.length) {
            for (int i = 0; i < a.length; i++) {
                if (a[i] > b[i]) {
                    return 0;
                } else if (b[i] > a[i]) {
                    return 1;
                }
            }
            return 1;
        } else {
            for (int i = 0; i < b.length; i++) {
                if (a[i] > b[i]) {
                    return 0;
                } else if (b[i] > a[i]) {
                    return 1;
                }
            }
            return 0;
        }
    }

    /**
     * Gets the machine associated with the key if it exists, otherwise a new
     * machine is added to the map with the given key and returned.
     * @param key key
     * @return machine mapped to by key, or new machine
     */
    private Machine machineGet(int key){
        Machine value = machines.get(key);
        if(value == null){
            value = new Machine(key, H, pForMachineStates);
            machines.put(key,value);
        }

        return value;
    }

    /**
     * Sample the machine key at a given index with a certain temperature.
     * @param index index of machine key to sample
     * @param temp temperature of sampling step
     */
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

            int currentMachineState = machineGet(currentKey).get(emissions, index);
            int proposedMachineState = machineGet(proposedKey).get(emissions, index);

            double r = Math.log(machineTransitions.prob(proposedKey, nextKey));
            r += emissionDistributions.logProbability(proposedMachineState, emissions[index]);
            r -= Math.log(machineTransitions.prob(currentKey, nextKey));
            r -= emissionDistributions.logProbability(currentMachineState, emissions[index]);

            r = Math.exp(r);
            r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

            if(rng.nextBoolean(r)){

                if(currentMachineState != proposedMachineState){
                    emissionDistributions.unseat(currentMachineState, emissions[index]);
                    emissionDistributions.seat(proposedMachineState, emissions[index]);
                }

                machineKeys[index] = proposedKey;

                machineTransitions.unseat(previousKey, currentKey);
                machineTransitions.unseat(currentKey, nextKey);

                machineTransitions.seat(previousKey, proposedKey);
                machineTransitions.seat(proposedKey, nextKey);
            }
        }
    }

    /**
     * Sample the first machine key with a given temperature.
     * @param temp temperature of sampling step
     */
    private void sampleFirstMachineKey(double temp) {
        int currentKey = machineKeys[0], nextKey = machineKeys[1];
        int proposedKey = machineTransitions.generate(null);

        int currentMachineState = machineGet(currentKey).get(emissions, 0);
        int proposedMachineState = machineGet(proposedKey).get(emissions, 0);

        double r = Math.log(machineTransitions.prob(proposedKey, nextKey));
        r += emissionDistributions.logProbability(proposedMachineState, emissions[0]);
        r -= Math.log(machineTransitions.prob(currentKey, nextKey));
        r -= emissionDistributions.logProbability(currentMachineState, emissions[0]);
        
        r = Math.exp(r);
        r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

        if (rng.nextBoolean(r)) {

            if(currentMachineState != proposedMachineState){
                emissionDistributions.unseat(currentMachineState, emissions[0]);
                emissionDistributions.seat(proposedMachineState, emissions[0]);
            }

            machineKeys[0] = proposedKey;

            machineTransitions.unseat(null, currentKey);
            machineTransitions.unseat(currentKey, nextKey);

            machineTransitions.seat(null, proposedKey);
            machineTransitions.seat(proposedKey, nextKey);
        }
    }

    /**
     * Sample the last machine key with a given temperature.
     * @param temp temperature of sampling step
     */
    private void sampleLastMachineKey(double temp){
        int index = words.length - 1;
        int previousKey = machineKeys[index - 1];
        int currentKey = machineKeys[index];

        int proposedKey = machineTransitions.generate(previousKey);

        int currentMachineState = machineGet(currentKey).get(emissions, index);
        int proposedMachineState = machineGet(proposedKey).get(emissions, index);

        double r = emissionDistributions.logProbability(machineGet(proposedKey).get(emissions, index), emissions[index]);
        r -= emissionDistributions.logProbability(machineGet(currentKey).get(emissions, index), emissions[index]);

        r = Math.exp(r);
        r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

        if (rng.nextBoolean(r)) {

            if(currentMachineState != proposedMachineState){
                emissionDistributions.unseat(currentMachineState, emissions[index]);
                emissionDistributions.seat(proposedMachineState, emissions[index]);
            }

            machineKeys[index] = proposedKey;

            machineTransitions.unseat(previousKey, currentKey);

            machineTransitions.seat(previousKey, proposedKey);
        }
    }

    private boolean checkEmissionDistributions(){
        int machineState;
        for(int i = 0; i < words.length; i++){
            machineState = machineGet(machineKeys[i]).get(emissions, i);
            emissionDistributions.unseat(machineState, emissions[i]);
        }

        if(emissionDistributions.isEmpty()){
            for(int i = 0; i < words.length; i++){
                machineState = machineGet(machineKeys[i]).get(emissions, i);
                emissionDistributions.seat(machineState, emissions[i]);
            }

            return true;
        } else {
            return false;
        }
    }
}
