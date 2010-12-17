/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.util;

import java.util.Iterator;

/**
 * Geometric distribution over 0 - infty with probability of failure q
 * 
 * @author nicholasbartlett
 */

public class IntGeometricDistribution implements IntDiscreteDistribution {

    private double q;

    public IntGeometricDistribution(double q){
        this.q = q;
    }

    public double probability(int type) {
        if (type >= 0){
            return Math.pow(q, type) * (1-q);
        } else {
            return 0.0;
        }
    }

    public Iterator<Pair<Integer, Double>> iterator() {
        return new GeoIterator();
    }

    private class GeoIterator implements Iterator<Pair<Integer,Double>> {

        private IntGeometricDistribution geoDist = new IntGeometricDistribution(q);
        private int next = 0;

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
