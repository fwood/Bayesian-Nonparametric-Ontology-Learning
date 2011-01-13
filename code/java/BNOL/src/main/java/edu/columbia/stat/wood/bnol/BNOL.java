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
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author nicholasbartlett
 */
public class BNOL {

    private TIntObjectHashMap<Machine> machines;
    private S_EmissionDistribution emissionDistributions;

    private int[][] emissions;
    private int[] machineKeys;
    private int[] words;

    private HPYP machineTransitions;
    private HPYP wordDistribution;

    public BNOL(int[] words, int alphabetSize, int H, double b, double pForMachineStates, double pForMachineTransitions){
        this.words = words;
        emissions = new int[words.length][];
        machineKeys = new int[words.length];
        
        machines = new TIntObjectHashMap();
        machines.put(0,new Machine(0, H, pForMachineStates));

        MutableDouble[] concentrations = new MutableDouble[2];
        MutableDouble[] discounts = new MutableDouble[2];

        concentrations[0] = new MutableDouble(10);
        concentrations[1] = new MutableDouble(1);

        discounts[0] = new MutableDouble(0.2);
        discounts[1] = new MutableDouble(0.6);

        machineTransitions = new IntHPYP(discounts, concentrations, new IntGeometricDistribution(pForMachineTransitions, 0), new GammaDistribution(1,100));
        emissionDistributions = new S_EmissionDistribution(new MutableDouble(b));

        MutableDouble[] wordConc = new MutableDouble[11];
        MutableDouble[] wordDisc = new MutableDouble[11];
        for(int i = 0; i < wordDisc.length; i++){
            wordDisc[wordDisc.length - 1 - i] = new MutableDouble(Math.pow(0.9, i + 1));
            wordDisc[i] = new MutableDouble(1.0);
        }

        wordDistribution = new IntHPYP(discounts, concentrations, new IntUniformDiscreteDistribution(alphabetSize), new GammaDistribution(1,100));
    }

    /**
     * Generates data from scratch from the model.
     * @param length number of data points
     * @param alphabetSize word alphabet size
     * @param H length of context dependence
     * @param b parameter controlling complexity of emission distributions, higher is a lower complexity, b must be in (0.0, 1.0]
     * @param pForMachineStates parameter for geometric distribution
     * @param pForMachineTransitions parameter for geometric distribution
     * @return
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

            emissions[i] = emissionDistributions.generate(machineState, 0.0, 1.0);
            emissionDistributions.seat(machineState, emissions[i]);

            words[i] = wordDistribution.draw(emissions[i]);

            if (i < length - 1){
                machineKeys[i + 1] = machineTransitions.draw(new int[]{machineKeys[i]});
                if(machines.get(machineKeys[i+1]) == null){
                    machines.put(machineKeys[i+1], new Machine(machineKeys[i+1], H, pForMachineStates));
                }
                //System.out.println("machine = " + machineKeys[i+1]);
            }
            
            //System.out.println("machine state = " + machineState);
            //System.out.println("emission = " + Arrays.toString(emissions[i]));
            //System.out.println("word = " + words[i] + "\n");
        }

        return emissions;
    }

    /*
    public static void main(String[] args){
        BNOL bnol = new BNOL(new int[10], 100, 0.3, 0.05, 0.05);
        bnol.generateFromScratch(100, 0.3, 0.1, 0.1);
    }*/
}
