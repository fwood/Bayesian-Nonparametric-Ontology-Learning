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
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.MutableInt;
import edu.columbia.stat.wood.bnol.util.SampleWithoutReplacement;
import gnu.trove.list.array.TIntArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Object to represent the complex deterministic program used to generate the
 * next emission state.  The start state is 1 and all states are positive
 * integers.
 * @author nicholasbartlett
 */
public class Machine {

    private HashMap<StateEmissionPair, MutableInt> delta = new HashMap();
    private HPYP prior;
    private int key, H;
    private MersenneTwisterFast rng;

    /***********************constructor methods********************************/

    public Machine(int key, int H, double p){
        this.key = key;
        this.H = H;
        rng = new MersenneTwisterFast(7);

        // the number 11 here is totally arbitrary
        MutableDouble[] discounts = new MutableDouble[11];
        MutableDouble[] concentrations = new MutableDouble[11];
        for(int i = 0; i < discounts.length; i++){
            discounts[discounts.length - 1 - i] = new MutableDouble(Math.pow(0.7, i + 1));
            concentrations[i] = new MutableDouble(1.0);
        }
        prior = new IntHPYP(discounts, concentrations, new IntGeometricDistribution(p,1), new GammaDistribution(1,100));
    }

    /***********************public methods*************************************/

    /**
     * Gets the next machine state given the previous emissions.  Each machine
     * starts in a given start state (1) and then transitions deterministically
     * to give the output.
     * @param emissions emissions
     * @param index index of time at which we want to get the next machine state
     * @return next machine state
     */
    public int get(int[][] emissions, int index){
        return get(emissions, index, null);
    }

    /**
     * Samples the underlying machine, i.e. the delta matrix, and then samples
     * the HPYP being used as a prior on that delta matrix.
     * @param emissions entire set of emissions for data
     * @param machineKeys array of which machine is used at each step
     * @param emissionDistributions emission distributions for each machine state
     * @param sweeps number of MH sweeps
     * @param temp temperature of sampling steps
     * @return joint log likelihood
     */
    public double sample(int[][] emissions, int[] machineKeys, S_EmissionDistribution emissionDistributions, int sweeps, double temp) {
        // get indices for this particular machine
        int[] indices = getIndices(machineKeys);

        // clean delta matrix first
        clean(emissions, indices);

        double logEvidence = 0;
        for (int sweep = 0; sweep < sweeps; sweep++) {
            // sample the prior hpyp 5 times, which is an arbitrary number
            for (int j = 0; j < 5; j++) {
                prior.sample(temp);
            }
            
            // copy the keys and values of the current delta matrix for sampling
            StateEmissionPair[] keys = new StateEmissionPair[delta.size()];
            MutableInt[] values = new MutableInt[delta.size()];

            int i = 0;
            int[] randomIndex = SampleWithoutReplacement.sampleWithoutReplacement(delta.size(), rng);

            for(Entry<StateEmissionPair, MutableInt> entry : delta.entrySet()){
                keys[randomIndex[i]] = entry.getKey();
                values[randomIndex[i++]] = entry.getValue();
            }

            // go through each mapped key value pair and sample them
            logEvidence = logEvidence(emissions, emissionDistributions, indices);
            for (int j = 0; j < keys.length; j++) {
                int[] context = keys[j].emission;
                int currentValue = values[j].value();

                prior.unseat(context, currentValue);
                int proposal = prior.draw(context);
                values[j].set(proposal);
                
                double proposedLogEvidence = logEvidence(emissions, emissionDistributions, indices);

                double r = Math.exp(proposedLogEvidence - logEvidence);
                r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);
                
                if (rng.nextBoolean(r)) {
                    prior.unseat(context, proposal);
                    prior.seat(context, currentValue);
                    values[j].set(currentValue);
                    int[] oldMachineKeys = getAllMachineState(emissions, indices);

                    prior.unseat(context, currentValue);
                    prior.seat(context, proposal);
                    values[j].set(proposal);
                    int[] newMachineKeys = getAllMachineState(emissions, indices);

                    adjustEmissionsDistributions(oldMachineKeys, newMachineKeys, emissionDistributions, emissions, indices);

                    logEvidence = proposedLogEvidence;
                } else {
                    prior.unseat(context, proposal);
                    prior.seat(context, currentValue);
                    values[j].set(currentValue);
                }
            }

            assert checkCounts();

            // clean the delta matrix
            clean(emissions, indices);
        }

        return prior.sample(temp);
    }

    /**
     * Gets the joint score of the HPYP and data.
     * @param emissions emission data
     * @param emissionDistributions emission distributions
     * @param machineKeys machine keys for emissions
     * @return score
     */
    public double score(int[][] emissions, S_EmissionDistribution emissionDistributions, int[] machineKeys){
        return prior.score(true) + logEvidence(emissions, emissionDistributions, getIndices(machineKeys));
    }

    /**
     * Removes from the delta map any entries which are not used given the data.
     * @param emissions emission data
     * @param indices indices of emission from this machine
     */
    public void clean(int[][] emissions, int[] indices){
        HashMap<StateEmissionPair, MutableInt> newDelta = new HashMap();

        for(int i = 0; i < indices.length; i++){
            get(emissions, indices[i], newDelta);
        }

        for(StateEmissionPair deltaKey : delta.keySet()){
            if(newDelta.get(deltaKey) == null){
                prior.unseat(deltaKey.emission, delta.get(deltaKey).value());
            }
        }

        delta = newDelta;
        prior.removeEmptyNodes();
    }

    /**
     * Checks the counts in the machine to make sure that the number of customers
     * in the HPYP and their locations are correct given the delta map.
     * @return true if counts are in agreement
     */
    public boolean checkCounts(){
        prior.removeEmptyNodes();
        HashMap<Context, MutableInt> data = prior.getImpliedData();

        for(StateEmissionPair deltaKey : delta.keySet()){
            data.get(new Context(deltaKey.emission)).decrement();
        }

        for(MutableInt value : data.values()){
            if(value.value() != 0){
                return false;
            }
        }

        return true;
    }

    /***********************private methods************************************/

    private int[] getAllMachineState(int[][] emissions, int[] indices){
        int[] machineStates = new int[indices.length];

        for(int i = 0; i++ < indices.length;){
            machineStates[i] = get(emissions, indices[i]);
        }

        return machineStates;
    }

    private void adjustEmissionsDistributions(int[] oldMachineStates, int[] newMachineStates, S_EmissionDistribution emissionDistributions, int[][] emissions, int[] indices){
        assert oldMachineStates.length == newMachineStates.length;
        for(int i = 0; i++ < oldMachineStates.length;){
            if(oldMachineStates[i] != newMachineStates[i]){
                emissionDistributions.unseat(oldMachineStates[i], emissions[indices[i]]);
                emissionDistributions.seat(newMachineStates[i], emissions[indices[i]]);
            }
        }
    }

    /**
     * Gets the next machine state given the previous emissions.  Each machine
     * starts in a given start state (1) and then transitions deterministically
     * to give the output.
     * @param emissions emissions
     * @param index index of time at which we want to get the next machine state
     * @param newDelta hash map for new delta if this is during a cleaning step
     * @return next machine state
     */
    private int get(int[][] emissions, int index, HashMap<StateEmissionPair, MutableInt> newDelta){
        int machineState = 1;
        int contextLength = H < index ? H : index;

        for(int i = 0; i < contextLength; i++){
            machineState = deltaGet(new StateEmissionPair(machineState, emissions[index - contextLength + i]), newDelta);
        }

        return machineState;
    }

    /**
     * Gets the indices into the argument arrays which pertain to this machine.
     * @param machineKeys array of machine keys
     * @return array of indices pertaining to this machine
     */
    private int[] getIndices(int[] machineKeys){
        TIntArrayList indices = new TIntArrayList();
        for(int i = 0; i < machineKeys.length; i++){
            if(machineKeys[i] == key){
                indices.add(i);
            }
        }
        return indices.toArray();
    }

    /**
     * Does a get from the delta map, but if nothing is found it makes a draw
     * from the prior and adds it to the map.
     * @param key key to get
     * @param newDelta hash map for new delta if this is during a cleaning step
     * @return retrieved or generated value
     */
    private int deltaGet(StateEmissionPair key, HashMap<StateEmissionPair, MutableInt> newDelta){
        MutableInt value = delta.get(key);

        if(value == null){
            int machineState = prior.draw(key.emission);
            delta.put(key,value = new MutableInt(machineState));
        }

        if(newDelta != null){
            newDelta.put(key, value);
        }
        
        return value.value();
    }

    /**
     * Gets the log evidence of the particular delta configuration given the
     * emissions and the emission distributions.
     * @param emissions emission data
     * @param emissionDistributions emission distributions for each machine state
     * @param indices indices where this machine is used
     * @return log evidence
     */
    private double logEvidence(int[][] emissions, S_EmissionDistribution emissionDistributions, int[] indices){
        double logEvidence = 0.0;

        for(int i = 0; i < indices.length; i++){
            logEvidence += emissionDistributions.logProbability(get(emissions, indices[i]), emissions[indices[i]]);
        }

        return logEvidence;
    }

    /***********************private classes************************************/

    /**
     * Convenient class to hold the state emission pairs for the delta map.
     */
    private class StateEmissionPair {
        int state;
        int[] emission;

        /***********************constructor methods****************************/

        /**
         * Constructor for state emission pair which sets the internal fields.
         * @param state state
         * @param emission emission
         */
        public StateEmissionPair(int state, int[] emission){

            this.state = state;
            this.emission = emission;
        }


        /***********************public methods*********************************/

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o){
            if(o == null || o.getClass() != getClass()){
                return false;
            } else {
                StateEmissionPair oo = (StateEmissionPair) o;
                if(Arrays.equals(emission, oo.emission) && state == oo.state){
                    return true;
                } else {
                    return false;
                }
            }
        }

        /*
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + this.state;
            hash = 37 * hash + Arrays.hashCode(this.emission);
            return hash;
        }
    }
}