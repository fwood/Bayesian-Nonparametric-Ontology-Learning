/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.HPYP;
import edu.columbia.stat.wood.bnol.hpyp.IntHPYP;
import edu.columbia.stat.wood.bnol.util.Context;
import edu.columbia.stat.wood.bnol.util.GammaDistribution;
import edu.columbia.stat.wood.bnol.util.IntGeometricDistribution;
import edu.columbia.stat.wood.bnol.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.MutableInt;
import edu.columbia.stat.wood.bnol.util.Pair;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author nicholasbartlett
 */
public class BNOL implements Serializable{
    
    public static void main(String[] args) throws IOException{
        BNOL b = new BNOL(new int[0], new int[0], 5000, 10, .1, 0.2, 0.2, .5, .8);
        b.generateFromScratch(10000,0);

        System.out.println("In this test we sampled from the prior and then fixed the actual emissions \n"
                + "and sampled only the machine keys and machines");

        System.out.println(b.score() + ", " + b.machines.size());
        for(int i = 0; i < 1000; i++){
            System.out.println(b.sample(1.0) + ", " + b.machines.size());
        }
    }


    public static void main0(String[] args) throws IOException{

        //ProcessCHILDES pc = new ProcessCHILDES(new File("/home/bartlett/BNOL/_CHILDES.parsed.txt"));
        /*ProcessCHILDES pc = new ProcessCHILDES(new File("/Users/nicholasbartlett/Documents/np_bayes/Bayesian_Nonparametric_Ontology_Learning/data/_CHILDES.parsed.txt"));

        int[] allWords = pc.get(1100);
        int[] words = new int[1000];
        System.arraycopy(allWords,0, words,0,1000);
        int[] pWords = new int[100];
        System.arraycopy(allWords,1000,words, 0, 100);

        BNOL b = new BNOL(words, pWords, pc.dictionary().size(),10, .1, 0.2, 0.2, .8, .9);
        b.initialize();*/

        BNOL b = new BNOL(new int[0], new int[0], 5000, 10, .1, 0.2, 0.2, .5, .8);
        b.generateFromScratch(10000,0);

        b.emissionDistributions.concentrations = new MutableDouble[]{new MutableDouble(0.01), new MutableDouble(0.01)};
        b.emissionDistributions.discounts = new MutableDouble[]{new MutableDouble(0.01), new MutableDouble(0.01)};

        System.out.println("For this set of samples we generate from scration 10k observations with context \n"
                + "length of 10 and b = .1.  Then, we set the discounts and concentrations for the s_emission distribution \n"
                + "really small (0.01) and sample the model starting at the truth, not sampling the hyper parameters of \n"
                + "either the word emission distribution or the s_emission distribution.  The first line is the truth \n"
                + "and is prior to any sampling steps");

        System.out.println(b.score() + ", " + b.uniqueEmbeddings());
        for(int i = 0; i < 1000; i++){
            System.out.println(b.sample(1.0) + ", " + b.uniqueEmbeddings());
        }

        //b.generateFromScratch(10000, 100);
        //System.out.println((b.logProbTest(100) / Math.log(2) / 100d) + ", " + b.uniqueEmbeddings());
        
        /*System.out.println();
        for(int i = 0; i < 100; i++){
            System.out.println((b.logProbTest(100) / Math.log(2) / 100d) + ", " + b.uniqueEmbeddings() + ", " + b.sample(1.0));
        }*/
        

        
        /*
        BNOL b = new BNOL(pc.get(10000), new int[0], pc.dictionary().size(), 10, 1d / 7d, 0.2, 0.2, .8, .9);
        b.initialize();

        ObjectOutputStream out = null;

        System.out.println(b.score() + ", " + b.uniqueEmbeddings() + ", -1" );
        for (int i = 0; i < 10000; i++){
            System.out.println(b.sample(1.0) + ", " + b.uniqueEmbeddings() + ", " + i);
            if(i % 10 == 0){
                out = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/_CHILDES_short.ser")));
                out.writeObject(b);
                out.close();
            }
        }*/
    }

    public static void mainA(String[] args) throws IOException{
        //int[] words, int alphabetSize, int H, double b, double pForMachineStates, double pForMachineTransitions){
        BNOL bb = new BNOL(new int[0], new int[0], 5000, 10, 1d / 7d, 0.2, 0.2, .3, .6);
        bb.generateFromScratch(10000,0);

        int[][] trueEmissions = new int[bb.emissions.length][];
        for(int i = 0; i < bb.emissions.length; i++){
            trueEmissions[i] = new int[bb.emissions[i].length];
            System.arraycopy(bb.emissions[i], 0, trueEmissions[i], 0, bb.emissions[i].length);
        }

        BNOL b = new BNOL(bb.words, new int[0], 5000, 10, 1d / 7d, 0.2, 0.2, .8, .9);
        b.initialize();

        double[] ac1 = bb.accuracyCompleteness(trueEmissions);
        double[] ac2 = b.accuracyCompleteness(trueEmissions);

        System.out.println(bb.score() + ", " + bb.uniqueEmbeddings() + ", " + ac1[0] + ", " + ac1[1] + ", " + b.score() + ", " + b.uniqueEmbeddings() + ", " + ac2[0] + ", " + ac2[1] + ", ,");

        int output = 0;
        ObjectOutputStream out_truth = null;
        ObjectOutputStream out_rand = null;

        double temp = 1000;
        /*for(int i = 0; i < 100; i++){
            if(i % 10 == 0){
                out_truth = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/truth_" + output)));
                out_rand = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/rand_" + output)));
                out_truth.writeObject(bb);
                out_rand.writeObject(b);
                out_truth.close();
                out_rand.close();
                output++;
            }

            System.out.println(bb.sample(temp) + ", " + bb.uniqueEmbeddings() + ", " + ac1[0] + ", " + ac1[1] + ", " + b.sample(temp) + ", " + b.uniqueEmbeddings() + ", " + ac2[0] + ", " + ac2[1] + ", " + temp + ", " + i);
            ac1 = bb.accuracyCompleteness(trueEmissions);
            ac2 = b.accuracyCompleteness(trueEmissions);
        }*/

        temp = 100;
        for(int i = 0; i < 100; i++){
            if(i % 10 == 0){
                out_truth = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/truth_" + output)));
                out_rand = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/rand_" + output)));
                out_truth.writeObject(bb);
                out_rand.writeObject(b);
                out_truth.close();
                out_rand.close();
                output++;
            }

            System.out.println(bb.sample(temp) + ", " + bb.uniqueEmbeddings() + ", " + ac1[0] + ", " + ac1[1] + ", " + b.sample(temp) + ", " + b.uniqueEmbeddings() + ", " + ac2[0] + ", " + ac2[1] + ", " + temp + ", " + i);
            ac1 = bb.accuracyCompleteness(trueEmissions);
            ac2 = b.accuracyCompleteness(trueEmissions);
        }

        temp = 10;
        for(int i = 0; i < 100; i++){
            if(i % 10 == 0){
                out_truth = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/truth_" + output)));
                out_rand = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/rand_" + output)));
                out_truth.writeObject(bb);
                out_rand.writeObject(b);
                out_truth.close();
                out_rand.close();
                output++;
            }

            System.out.println(bb.sample(temp) + ", " + bb.uniqueEmbeddings() + ", " + ac1[0] + ", " + ac1[1] + ", " + b.sample(temp) + ", " + b.uniqueEmbeddings() + ", " + ac2[0] + ", " + ac2[1] + ", " + temp + ", " + i);
            ac1 = bb.accuracyCompleteness(trueEmissions);
            ac2 = b.accuracyCompleteness(trueEmissions);
        }

        temp = 1;
        for(int i = 0; i < 10000; i++){
            if(i % 10 == 0){
                out_truth = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/truth_" + output)));
                out_rand = new ObjectOutputStream(new FileOutputStream(new File("/home/bartlett/BNOL/rand_" + output)));
                out_truth.writeObject(bb);
                out_rand.writeObject(b);
                out_truth.close();
                out_rand.close();
                output++;
            }
            System.out.println(bb.sample(temp) + ", " + bb.uniqueEmbeddings() + ", " + ac1[0] + ", " + ac1[1] + ", " + b.sample(temp) + ", " + b.uniqueEmbeddings() + ", " + ac2[0] + ", " + ac2[1] + ", " + temp + ", " + i);
            ac1 = bb.accuracyCompleteness(trueEmissions);
            ac2 = b.accuracyCompleteness(trueEmissions);
        }
    }

    private HashMap<Integer, Machine> machines;
    private S_EmissionDistribution emissionDistributions;

    public int[] machineKeys;
    public int[][] emissions;
    public int[] words;

    public int[] pMachineKeys;
    public int[][] pEmissions;
    public int[] pWords;

    private HPYP machineTransitions;
    private HPYP wordDistribution;

    private int H;
    private double b, pForMachineStates;

    public static MersenneTwisterFast rng = new MersenneTwisterFast(1);

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
    public BNOL(int[] trainingWords, int[] testWords, int alphabetSize, int H, double b, double pForMachineStates, double pForMachineTransitions, double d0, double d1){
        machines = new HashMap();
        emissionDistributions = new S_EmissionDistribution(b, d0, d1, 10, 1);

        machineKeys = new int[trainingWords.length];
        emissions = new int[trainingWords.length][];
        words = trainingWords;

        pMachineKeys = new int[testWords.length];
        pEmissions = new int[testWords.length][];
        pWords = testWords;
        
        MutableDouble[] conc = new MutableDouble[2];
        MutableDouble[] disc = new MutableDouble[2];

        conc[0] = new MutableDouble(20);
        conc[1] = new MutableDouble(5);

        disc[0] = new MutableDouble(0.8);
        disc[1] = new MutableDouble(0.9);

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
        disc[1] = new MutableDouble(.1);
        disc[2] = new MutableDouble(.2);
        disc[3] = new MutableDouble(.3);
        disc[4] = new MutableDouble(.4);
        disc[5] = new MutableDouble(.5);
        disc[6] = new MutableDouble(.6);
        disc[7] = new MutableDouble(.7);
        disc[8] = new MutableDouble(.8);
        disc[9] = new MutableDouble(.9);
        disc[10] = new MutableDouble(.95);

        /*disc[0] = new MutableDouble(.7);
        disc[1] = new MutableDouble(.8);
        disc[2] = new MutableDouble(.85);
        disc[3] = new MutableDouble(.9);
        disc[4] = new MutableDouble(.9);
        disc[5] = new MutableDouble(.9);
        disc[6] = new MutableDouble(.9);
        disc[7] = new MutableDouble(.95);
        disc[8] = new MutableDouble(.95);
        disc[9] = new MutableDouble(.95);
        disc[10] = new MutableDouble(.95);*/

        wordDistribution = new IntHPYP(disc, conc, new IntUniformDiscreteDistribution(alphabetSize), new GammaDistribution(1d,100d));

        this.H = H;
        this.b = b;
        this.pForMachineStates = pForMachineStates;
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

    public int uniqueEmbeddings(){
        HashSet<Context> embeddings = new HashSet();

        for(int[] embedding : emissions){
            embeddings.add(new Context(embedding));
        }
        
        return embeddings.size();
    }

    public double score(){
        ll_machine_transitions = machineTransitions.score(true);

        ll_machines = 0;
        for(Machine m : machines.values()){
            ll_machines += m.score(emissions, emissionDistributions, machineKeys);
        }

        ll_emissions = emissionDistributions.score();

        ll_words = wordDistribution.score(true);

        return ll_emissions + ll_words + ll_machines + ll_machine_transitions;
    }

    public double sample(double temp){
        sampleMachineKeys(temp);

        ll_machine_transitions = machineTransitions.sample(temp);

        ll_machines = sampleMachines(temp);

        ll_emissions = emissionDistributions.sampleSeatingArrangements(); //emissionDistributions.sample(10, temp);

        sampleEmissions(temp);

        ll_words = wordDistribution.sample(temp);
        wordDistribution.sampleSeatingArrangements();
        ll_words = wordDistribution.score(true);

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
            sampleEmission(i, temp);
        }
    }

    /**
     * Cycles through the machines and sample them with a given temperature.
     * @param temp temperature of sampling step
     * @return joint score of all the machines.
     */
    public double sampleMachines(double temp){
        double score = 0.0;
        for(Machine machine : machines.values()){
            machine.sample(emissions, machineKeys, emissionDistributions, 1, temp);

            assert machine.checkCounts();

            score += machine.score(emissions, emissionDistributions, machineKeys);
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
    public void generateFromScratch(int length, int testLength){
        
        machineKeys = new int[length];
        emissions = new int[length][];
        words = new int[length];

        int machineState;
        for(int i = 0; i < length; i++){
            if (i == 0){
                machineKeys[0] = machineTransitions.draw(null);
            } else {
                machineKeys[i] = machineTransitions.draw(machineKeys[i-1]);
            }
            machineState = machineGet(machineKeys[i]).get(emissions, i);

            // draw next emission
            emissions[i] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
            emissionDistributions.seat(machineState, emissions[i]);

            // draw and seat word
            words[i] = wordDistribution.draw(emissions[i]);
        }

        pMachineKeys = new int[testLength];
        pEmissions = new int[testLength][];
        pWords = new int[testLength];

        for(int i = 0; i < testLength; i++){
            if (i == 0){
                pMachineKeys[i] = machineTransitions.generate(machineKeys[length - 1]);
            } else {
                pMachineKeys[i] = machineTransitions.generate(pMachineKeys[i - 1]);
            }
            machineState = machineGet(machineKeys[i]).get(emissions, i);

            pEmissions[i] = emissionDistributions.generate(machineState, 0d, 1d).first();

            pWords[i] = wordDistribution.generate(pEmissions[i]);
        }
    }

    /**
     * Initializes the arrays, assuming the words are set.
     */
    public void initialize(){
        int machineState;
        // repeat the generative process forwards until the end
        for(int i = 0; i < words.length; i++){
            //draw next machine key and get the machine state
            if(i == 0){
                machineKeys[0] = machineTransitions.draw(null);
            } else {
                machineKeys[i] = machineTransitions.draw(machineKeys[i - 1]);
            }
            machineState = machineGet(machineKeys[i]).get(emissions, i);

            // draw the next emission
            emissions[i] = emissionDistributions.generate(machineState, 0.0, 1.0).first();
            emissionDistributions.seat(machineState, emissions[i]);

            //seat word
            wordDistribution.seat(emissions[i], words[i]);
        }
    }

    public double[] accuracyCompleteness(int[][] trueEmissions){
        assert trueEmissions.length == emissions.length;

        Pair<MutableInt, MutableInt> aParent = new Pair(new MutableInt(0), new MutableInt(0));
        Pair<MutableInt, MutableInt> cParent = new Pair(new MutableInt(0), new MutableInt(0));

        Pair<ArrayList<MutableInt>, ArrayList<MutableInt>> aOverlap = new Pair(new ArrayList(), new ArrayList());
        Pair<ArrayList<MutableInt>, ArrayList<MutableInt>> cOverlap = new Pair(new ArrayList(), new ArrayList());

        int maxLengthEmission = 0;
        for(int i = 0; i < emissions.length; i++){
            maxLengthEmission = maxLengthEmission > emissions[i].length ? maxLengthEmission : emissions[i].length;
            maxLengthEmission = maxLengthEmission > trueEmissions[i].length ? maxLengthEmission : trueEmissions[i].length;
        }

        for(int i = 0; i < maxLengthEmission; i++){
            aOverlap.first().add(new MutableInt(0));
            aOverlap.second().add(new MutableInt(0));
            cOverlap.first().add(new MutableInt(0));
            cOverlap.second().add(new MutableInt(0));
        }

        for(int i = 0; i < emissions.length; i++){
            for(int j = (i + 1); j < emissions.length; j++){

                int overlap0 = overlap(emissions[i], emissions[j]);
                int overlap1 = overlap(trueEmissions[i], trueEmissions[j]);

                for(int k = 0; k < overlap0; k++){
                    aOverlap.first().get(k).increment();
                    if(k <= overlap1){
                        aOverlap.second().get(k).increment();
                    }
                }

                for(int k = 0; k < overlap1; k++){
                    cOverlap.second().get(k).increment();
                    if(k <= overlap0){
                        cOverlap.first().get(k).increment();
                    }
                }

                int isParent0 = isParent(emissions[i], emissions[j]);
                int isParent1 = isParent(trueEmissions[i], trueEmissions[j]);

                if(isParent0 != -1){
                    aParent.first().increment();
                    if(isParent1 == isParent0){
                        aParent.second().increment();
                    }
                }
                
                if(isParent1 != -1){
                    cParent.second().increment();
                    if(isParent1 == isParent0){
                        cParent.first().increment();
                    }
                }
            }
        }

        double a = ((double) aParent.second().value() )/ ((double) aParent.first().value());
        double c = ((double) cParent.first().value() )/ ((double) cParent.second().value());

        return new double[]{a,c};
    }
    
    public double logProbTest(int numberForwardSamples){
        int[][] emissions = new int[pWords.length + H][];
        for(int i = 0; i < H; i++){
            emissions[i] = this.emissions[this.emissions.length - H + i];
        }

        int[] machineKeys = new int[pWords.length + H];
        System.arraycopy(this.machineKeys, this.machineKeys.length - H, machineKeys, 0, H);
        

        int machineKey, machineState;
        int[] embedding;
        double probWord, logProb = 0.0, p = 0.0, q, maxP;
        
        for(int i = 0; i < pWords.length; i++){
            p = 0.0;
            maxP = 0.0;
            for(int j = 0; j < numberForwardSamples; j++){
                machineKey = machineTransitions.generate(machineKeys[H-1 + i]);
                embedding = emissionDistributions.generate(machineGet(machineKey).get(emissions, H + i), 0, 1).first();
                
                q = wordDistribution.prob(embedding, pWords[i]);
                p += q;
                if(p > maxP){
                    machineKeys[H + i] = machineKey;
                    emissions[H + i] = embedding;
                }
            }
            p = p / (double) numberForwardSamples;
            logProb += Math.log(p);
        }

        System.arraycopy(machineKeys, H, pMachineKeys, 0, pMachineKeys.length);
        System.arraycopy(emissions, H, pEmissions, 0, pMachineKeys.length);

        return logProb;
    }

    /***********************private methods************************************/

    private int overlap(int[] a, int[] b){
        if(a.length == 0 && b.length == 0){
            return 0;
        } else {
            int l = Math.min(a.length, b.length);
            int overlap =  -1;
            for(int i = 0; i < l; i++){
                if(a[i] == b[i]){
                    overlap++;
                }
            }
            return overlap;
        }
    }

    private int isParent(int[] a, int[] b){
        if(a.length == b.length){
            return -1;
        } else {
            int minLength = Math.min(a.length, b.length);
            for(int i = 0; i < minLength; i++){
                if(a[i] != b[i]){
                    return -1;
                }
            }
            
            if(a.length > b.length){
                return 0;
            } else {
                return 1;
            }
        }
    }

    /**
     * Sample the emission indexed by the given index using a slice sampler.
     * @param index index of emission to sample.
     */
    private void sampleEmission(int index, double temp){
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
            proposedProb = 100d * Math.pow(proposedProb / 100d, 1d / temp);

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

        if (!Arrays.equals(currentEmission, emissions[index])) {
            int[] newMachineStates = new int[Math.min(H, words.length - 1 - index)];
            for (int i = 0; i < newMachineStates.length; i++) {
                newMachineStates[i] = machineGet(machineKeys[index + 1 + i]).get(emissions, index + 1 + i);
            }

            emissionDistributions.unseat(thisMachineState, currentEmission);
            emissionDistributions.seat(thisMachineState, emissions[index]);

            wordDistribution.unseat(currentEmission, words[index]);
            wordDistribution.seat(emissions[index], words[index]);

            for (int i = 0; i < newMachineStates.length; i++) {
                if (newMachineStates[i] != currentMachineStates[i]) {
                    emissionDistributions.unseat(currentMachineStates[i], emissions[index + 1 + i]);
                    emissionDistributions.seat(newMachineStates[i], emissions[index + 1 + i]);
                }
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
        double logProb = 0.0;
        while(i < H && (index + i) < words.length){
            logProb += emissionDistributions.logProbability(machineGet(machineKeys[index + i]).get(emissions, index + i), emissions[index + i]);
            i++;
        }
        return logProb;
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

    /*private boolean checkEmissionDistributions(){
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
    }*/

    private boolean checkWordDistribution(){
        for(int i = 0; i < words.length; i++){
            wordDistribution.unseat(emissions[i], words[i]);
        }

        if (wordDistribution.isEmpty()){
            for(int i = 0; i < words.length; i++){
                wordDistribution.seat(emissions[i], words[i]);
            }
            return true;
        } else {
            return false;
        }
    }
}
