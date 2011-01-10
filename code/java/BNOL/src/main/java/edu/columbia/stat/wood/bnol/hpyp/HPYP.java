/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.hpyp;

/**
 * 
 * @author nicholasbartlett
 * 
 * Abstract class to set up a nice interface for the HPYP object used in this
 * project.
 * 
 */
public abstract class HPYP {

    /**
     * Get the probability of a given type in a given context.
     * @param context context
     * @param type type to get probability of
     * @return probability of type in given context
     */
    abstract public double prob(int[] context, int type);

    /**
     * Seat the given type in the given context.
     * @param context context in which to seat type
     * @param type type to seat
     */
    abstract public void seat(int[] context, int type);

    /**
     * Unseat the given type in the given context.
     * @param context context in which to unseat
     * @param type type to unseat
     */
    abstract public void unseat(int[] context, int type);

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

    /**
     * Generates a random sample from the predictive distribution at the node
     * indexed by the given context.
     * @param context context in which to generate a random sample
     * @return random sample
     */
    public int generate(int[] context){
        return generate(context, 0.0, 1.0, null);
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

    /**
     * Commits the customers created during the draw steps up until the previous
     * commit or revert.
     */
    abstract public void commitDraw();

    /**
     * Removes customers created during the draw steps up until the previous
     * commit or revert.
     */
    abstract public void revertDraw();

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
     * @return joint log likelihood
     */
    abstract public double sampleHyperParameters(int sweeps);

    /**
     * Sample concentration and discount parameters once.
     * @return joint log likelihood
     */
    public double sampleHyperParameters(){
        return sampleHyperParameters(1);
    }

    /**
     * Sample seating arrangements intermittently with the concentrations and
     * discount parameters a given number of times.
     * @param sweeps number of sweeps to pass through
     * @return joint log likelihood
     */
    abstract public double sample(int sweeps);

    /**
     * Sample seating arrangements intermittently with the concentrations and
     * discount parameters once.
     * @return joint log likelihood
     */
    public double sample(){
        return sample(1);
    }

    /**
     * Gets joint log likelihood.
     * @return joint log likelihood
     */
    abstract public double score();

    /**
     * Remove nodes/restaurants without customers.
     */
    abstract public void removeEmptyNodes();
}
