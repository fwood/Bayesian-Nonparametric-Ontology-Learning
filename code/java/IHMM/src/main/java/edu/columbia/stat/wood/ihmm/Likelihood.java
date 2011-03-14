/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.ihmm;

import edu.columbia.stat.wood.hdp.DiscreteDistribution.IntDoublePair;
import java.util.Iterator;

/**
 *
 * @author nicholasbartlett
 */
public abstract class Likelihood {

    public abstract void sample();

    public abstract double probability(int state, int observation);

    public abstract void adjustCount(int state, int observation, int multiplicity);

    //public abstract Iterator<IntDoublePair> iterator(int state);

    public abstract double score();
}
