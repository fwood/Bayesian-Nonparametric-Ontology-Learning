/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp.util;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

/**
 * Int discrete distribution interface.
 * @author nicholasbartlett
 */
public abstract class IntDiscreteDistribution implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * Gets the probability of certain type.
     * @param type int type
     * @return probability of this type, this should be in [0, 1.0]
     */
    abstract public double probability(int type);

    /**
     * Generates a random sample from the discrete distribution.
     * @param rng random number generator
     * @return generated int sample
     */
    abstract public int generate(Random rng);

    /**
     * Gets an iterator over Integer Double pairs such that the Double value is the
     * probability of the Integer value in the distribution.  The assumption is that
     * the iterator will iterate over the unique types in this distribution which
     * are given probability mass.  If we sum the Double values over all the elements
     * in the iterator the result should be 1.0.
     * @return iterator
     */
    abstract public Iterator<IntDoublePair> iterator();

}
