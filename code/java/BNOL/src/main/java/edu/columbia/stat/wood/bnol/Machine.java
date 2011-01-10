/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Object to represent the complex deterministic program used to generate the
 * next emission state.
 * @author nicholasbartlett
 */
public abstract class Machine {

    /**
     * Gets the next machine state given the previous emissions.  Each machine
     * starts in a given start state and then transitions deterministically to
     * give the output.
     * @param context trinary emissions
     * @return next machine state
     */
    abstract int get(byte[][] context);

    /**
     * Samples the underlying machine, i.e. the delta matrix, and then samples
     * the HPYP being used as a prior on that delta matrix.
     * @param emissions entire set of emissions for data
     * @param distributions map of all emission distributions for each machine state
     * @param sweeps number of MH sweeps
     * @return joint log likelihood
     */
    abstract double sample(byte[][] emissions, TIntObjectHashMap<S_EmissionDistribution> distributions, int sweeps);

    /**
     * Gets the joint score of the HPYP.
     * @return joint log likelihood
     */
    abstract double score();
}
