/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.util;

import java.util.Iterator;

/**
 * Geometric distribution over offset - infty with probability of failure q
 * 
 * @author nicholasbartlett
 */

public class IntGeometricDistribution implements IntDiscreteDistribution {

    private double q;
    private int offset;

    /**
     * Constructor method, takes the probability of success and an offset.  The
     * returned random variable is the number of failures plus the offset.
     * @param p
     * @param offset
     */
    public IntGeometricDistribution(double p, int offset){
        q = 1.0 - p;
        this.offset = offset;
    }

    /**
     * {@inheritDoc}
     */
    public double probability(int type) {
        type -= offset;
        if (type >= 0){
            return Math.pow(q, type) * (1-q);
        } else {
            return 0.0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int generate(MersenneTwisterFast rng){
        double randomNumber = rng.nextDouble(), cuSum = 0.0, prob = 1.0 - q;
        int sample = -1;

        while(cuSum <= randomNumber && sample >= -1){
            cuSum += prob;
            prob *= q;
            sample++;
        }

        if(cuSum <= randomNumber){
            throw new RuntimeException("Something bad happened, cuSum = " + cuSum + ", randomNumber = " + randomNumber);
        }

        return sample + offset;
    }

    public Iterator<Pair<Integer, Double>> iterator() {
        return new GeoIterator();
    }

    private class GeoIterator implements Iterator<Pair<Integer,Double>> {

        private IntGeometricDistribution geoDist = new IntGeometricDistribution(1.0 - q, offset);
        private int next = 0 + offset;

        public boolean hasNext() {
            return true;
        }

        public Pair<Integer, Double> next() {
            return new Pair<Integer,Double>(next,geoDist.probability(next++));
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }
}
