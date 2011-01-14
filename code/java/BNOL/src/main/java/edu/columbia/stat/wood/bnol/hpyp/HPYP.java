/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.hpyp;

import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Abstract class to set up a nice interface for the HPYP object used in this
 * project.
 * @author nicholasbartlett
 */
public abstract class HPYP {

    /**
     * Indicates if the entire HPYP is empty
     * @return true if empty, else false
     */
    abstract public boolean isEmpty();

    /**
     * Get the probability of a given type in a given context.
     * @param context context
     * @param type type to get probability of
     * @return probability of type in given context
     */
    abstract public double prob(int[] context, int type);

    public double prob(int context, int type){
        return prob(new int[]{context}, type);
    }

    /**
     * Seat the given type in the given context.
     * @param context context in which to seat type
     * @param type type to seat
     */
    abstract public void seat(int[] context, int type);

    public void seat(int context, int type){
        seat(new int[]{context}, type);
    }

    /**
     * Unseat the given type in the given context.
     * @param context context in which to unseat
     * @param type type to unseat
     */
    abstract public void unseat(int[] context, int type);

    public void unseat(int context, int type){
        unseat(new int[]{context}, type);
    }

    /**
     * Generates a random sample from the predictive distribution at the node
     * indexed by the given context.
     * @param context context in which to generate a random sample
     * @param low low edge of slice in [0.0, high)
     * @param high high edge of slice in (low, 1.0]
     * @param keyOrder order of types for slice sampling
     * @return random sample
     */
    abstract public int generate(int[] context, double low, double high, int[] keyOrder);

    public int generate(int context, double low, double high, int[] keyOrder){
        return generate(new int[]{context}, low, high, keyOrder);
    }

    /**
     * Draws a random sample from the predictive distribution at the node
     * indexed by the given context and updates counts in the underlying object.
     * @param context context in which to generate a random sample
     * @param low low edge of slice in [0.0, high)
     * @param high high edge of slice in (low, 1.0]
     * @param keyOrder order of types for slice sampling
     * @return random sample
     */
    abstract public int draw(int[] context, double low, double high, int[] keyOrder);

    public int draw(int context, double low, double high, int[] keyOrder){
        return draw(new int[]{context}, low, high, keyOrder);
    }

    /**
     * Generates a random sample from the predictive distribution at the node
     * indexed by the given context.
     * @param context context in which to generate a random sample
     * @return random sample
     */
    public int generate(int[] context){
        return generate(context, 0.0, 1.0, null);
    }

    public int generate(int context){
        return generate(new int[]{context});
    }
    
    /**
     * Draws a random sample from the predictive distribution at the node
     * indexed by the given context and updates counts in the underlying object.
     * @param context context in which to generate a random sample
     * @return random sample
     */
    public int draw(int[] context){
        return draw(context, 0.0, 1.0, null);
    }

    public int draw(int context){
        return draw(new int[]{context});
    }

    /**
     * Sample the seating arrangements a given number of times.
     * @param sweeps number of sweeps to pass through
     */
    abstract public void sampleSeatingArrangements(int sweeps);

    /**
     * Sample the seating arrangements once.
     */
    public void sampleSeatingArrangements(){
        sampleSeatingArrangements(1);
    }

    /**
     * Sample concentration and discount parameters a given number of times.
     * @param sweeps number of sweeps to pass through
     * @param temp temperature parameter
     * @return joint log likelihood
     */
    abstract public double sampleHyperParameters(int sweeps, double temp);

    /**
     * Sample concentration and discount parameters once.
     * @param temp temperature parameter
     * @return joint log likelihood
     */
    public double sampleHyperParameters(double temp){
        return sampleHyperParameters(1, temp);
    }

    /**
     * Sample seating arrangements intermittently with the concentrations and
     * discount parameters a given number of times.
     * @param sweeps number of sweeps to pass through
     * @param temp temperature parameter
     * @return joint log likelihood
     */
    abstract public double sample(int sweeps, double temp);

    /**
     * Sample seating arrangements intermittently with the concentrations and
     * discount parameters once.
     * @param temp temperature parameter
     * @return joint log likelihood
     */
    public double sample(double temp){
        return sample(1, temp);
    }

    /**
     * Gets joint log likelihood.
     * @param withHyperParameters if true then include prior on hyper parameters
     * @return joint log likelihood
     */
    abstract public double score(boolean withHyperParameters);

    /**
     * Calculates the joint log likelihood contributions of restaurants at each
     * depth of the tree. This method is primarily for use with sampling the
     * concentration and discount parameters. The sum of this vector plus the
     * contribution of the root restaurant gives the same result as the score
     * method.
     * @param withHyperParameters if true then include prior on hyper parameters
     * @return log likelihood contributions of restaurants at each depth of the tree
     */
    abstract public double[] scoreByDepth(boolean withHyperParameters);

    /**
     * Remove nodes/restaurants without customers.
     */
    abstract public void removeEmptyNodes();

    /**
     * Gets the data by looking at the discrepancy between counts at each
     * restaurant and it's children nodes.
     * @return map from contexts to counts
     */
    abstract public TObjectIntHashMap<int[]> getImpliedData();
}
