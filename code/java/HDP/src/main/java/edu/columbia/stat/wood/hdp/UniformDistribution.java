/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.hdp;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author nicholasbartlett
 */
public class UniformDistribution extends DiscreteDistribution {

    private final double p;
    private HashMap<Integer,MutableInt> counts;
    private int alphabetSize;
    

    public UniformDistribution (int alphabetSize) {
        this.alphabetSize = alphabetSize;
        p = 1d / (double) alphabetSize;
        counts = new HashMap<Integer,MutableInt>();
    }

    @Override
    public double probability(int type) {
        return p;
    }

    @Override
    public void adjustObservedCount(int type, int adjustment) {
        MutableInt mi = counts.get(type);
        if (mi == null) {
            counts.put(type, mi = new MutableInt(0));
        }

        mi.plusEquals(adjustment);

        if (mi.value() == 0) {
            counts.remove(type);
        } else if (mi.value() < 0) {
            throw new RuntimeException("Cannot remove so many observations of type " + type);
        }
    }

    @Override
    public void adjustPseudoCount(int type, int adjustment){};

    @Override
    public void sample() {}

    @Override
    public double score() {
        double score = 0d;
        double logp = Math.log(p);
        for (MutableInt count : counts.values()) {
            score += count.value() * p;
        }
        return score;
    }

    @Override
    public Iterator<IntDoublePair> iterator() {
        return new UniformIterator();
    }


    private class UniformIterator implements Iterator<IntDoublePair> {
        private int next = 0;

        @Override
        public boolean hasNext() {
            return next < alphabetSize;
        }

        @Override
        public IntDoublePair next() {
            return new IntDoublePair(next++, p);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
