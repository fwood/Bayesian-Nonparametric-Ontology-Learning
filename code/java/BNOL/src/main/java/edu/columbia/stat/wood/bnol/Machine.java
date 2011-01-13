/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.HPYP;
import edu.columbia.stat.wood.bnol.hpyp.IntHPYP;
import edu.columbia.stat.wood.bnol.util.GammaDistribution;
import edu.columbia.stat.wood.bnol.util.IntGeometricDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.SampleWithoutReplacement;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import java.util.Arrays;

/**
 * Object to represent the complex deterministic program used to generate the
 * next emission state.  The start state is 1 and all states are positive
 * integers.
 * @author nicholasbartlett
 */
public class Machine {

    private TObjectIntHashMap<StateEmissionPair> delta;
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
            discounts[discounts.length - 1 - i] = new MutableDouble(Math.pow(0.9, i));
            concentrations[i] = new MutableDouble(1.0);
        }
        prior = new IntHPYP(discounts, concentrations, new IntGeometricDistribution(p,1), new GammaDistribution(1,100));
    }

    /***********************public methods*************************************/

    /**
     * Gets the next machine state given the previous emissions.  Each machine
     * starts in a given start state (1) and then transitions deterministically
     * to give the output.
     * @param emissions trinary emissions
     * @param index index of time at which we want to get the next machine state
     * @return next machine state
     */
    public int get(int[][] emissions, int index){
        return get(emissions, index, false);
    }

    /**
     * Gets the next machine state given the previous emissions.  Each machine
     * starts in a given start state (1) and then transitions deterministically
     * to give the output.
     * @param emissions trinary emissions
     * @param index index of time at which we want to get the next machine state
     * @param used if true then mark those delta entries which are used
     * @return next machine state
     */
    public int get(int[][] emissions, int index, boolean used){
        int machineState = 1; 
        int contextLength = H < index ? H : index;
        
        for(int i = 0; i < contextLength; i++){
            machineState = deltaGet(new StateEmissionPair(machineState, emissions[index - contextLength + i], used), used);
        }

        return machineState;
    }

    /**
     * Samples the underlying machine, i.e. the delta matrix, and then samples
     * the HPYP being used as a prior on that delta matrix.
     * @param emissions entire set of emissions for data
     * @param machineKeys array of which machine is used at each step
     * @param distributions map of all emission distributions for each machine state
     * @param sweeps number of MH sweeps
     * @return joint log likelihood
     */
    public double sample(int[][] emissions, int[] machineKeys, S_EmissionDistribution emissionDistributions, int sweeps) {
        // clean delta matrix first
        clean(emissions, machineKeys);

        // get indices for this particular machine
        int[] indices = getIndices(machineKeys);

        for (int sweep = 0; sweep < sweeps; sweep++) {
            // sample the prior hpyp 5 times, which is an arbitrary number
            for (int j = 0; j < 5; j++) {
                prior.sample();
            }
            
            // copy the keys and values of the current delta matrix for sampling
            StateEmissionPair[] keys = new StateEmissionPair[delta.size()];
            int[] values = new int[delta.size()];

            int i = 0;
            int[] randomIndex = SampleWithoutReplacement.sampleWithoutReplacement(delta.size(), rng);

            TObjectIntIterator<StateEmissionPair> iterator = delta.iterator();
            while (iterator.hasNext()) {
                iterator.advance();

                keys[randomIndex[i]] = iterator.key();
                values[randomIndex[i++]] = iterator.value();
            }

            // go through each mapped key value pair and sample them
            double logEvidence = logEvidence(emissions, emissionDistributions, indices);
            for (int j = 0; j < keys.length; j++) {
                int[] context = keys[j].emission;

                prior.unseat(context, values[j]);
                int proposal = prior.draw(context);
                delta.put(keys[j], proposal);
                
                double proposedLogEvidence = logEvidence(emissions, emissionDistributions, indices);

                double r = Math.exp(proposedLogEvidence - logEvidence);
                r = r < 1.0 ? r : 1.0;
                
                if (rng.nextBoolean(r)) {
                    logEvidence = proposedLogEvidence;
                } else {
                    prior.unseat(context, proposal);
                    prior.seat(context, values[j]);
                    delta.put(keys[j], values[j]);
                }
            }

            // clean the delta matrix
            clean(emissions, machineKeys);
        }

        return prior.sample();
    }

    /**
     * Gets the joint score of the HPYP.
     * @return joint log likelihood
     */
    public double score(){
        return prior.score(true);
    }

    /**
     * Removes from the delta map any entries which are not used given the data.
     * @param emissions emission data
     * @param machineKeys indicator of which machine is being used at each time step
     */
    public void clean(int[][] emissions, int[] machineKeys){
        int[] indices = getIndices(machineKeys);
        for(int i = 0; i < indices.length; i++){
            get(emissions, indices[i], true);
        }

        delta.retainEntries(new StateEmissionPairUnseat());
        prior.removeEmptyNodes();
    }

    /**
     * Checks the counts in the machine to make sure that the number of customers
     * in the HPYP and their locations are correct given the delta map.
     * @return true if counts are in agreement
     */
    public boolean checkCounts(){
        prior.removeEmptyNodes();
        TObjectIntHashMap<int[]> data = prior.getImpliedData();
        
        TObjectIntIterator<StateEmissionPair> iterator = delta.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            
            data.adjustValue(iterator.key().emission, -1);
        }

        TObjectIntIterator<int[]> iterator1 = data.iterator();
        while(iterator1.hasNext()){
            iterator1.advance();
            if(iterator1.value() != 0){
                return false;
            }
        }

        return true;
    }

    /***********************private methods************************************/

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
     * @return retrieved or generated value
     */
    private int deltaGet(StateEmissionPair key, boolean used){
        int value = delta.get(key);

        if(value == 0){
            value = prior.draw(key.emission);
            delta.put(key,value);
        } else if(used){
            key.used = true;
            delta.put(key, value);
        }
        
        return value;
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
        boolean used;

        /***********************constructor methods****************************/

        /**
         * Constructor for state emission pair which sets the internal fields.
         * @param state state
         * @param emission emission
         */
        public StateEmissionPair(int state, int[] emission, boolean used){

            assert(emission[emission.length-1] == 2) : "last element of s must be 2";

            this.used = used;
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

    /**
     * Class to implement a procedure used for cleaning the delta map.
     */
    private class StateEmissionPairUnseat implements TObjectIntProcedure<StateEmissionPair> {

        /**
         * If the state emission pair is not being used it is removed from the
         * map and un-seated in the prior.  If it is being used it is retained
         * in the map, though used is set to false for future iterations.
         * @param key key
         * @param machineState machine state
         * @return true if retained in the map
         */
        public boolean execute(StateEmissionPair key, int machineState) {
            if(!key.used){
                prior.unseat(key.emission,machineState);
                return false;
            } else {
                key.used = false;
                return true;
            }
        }
    }
}
