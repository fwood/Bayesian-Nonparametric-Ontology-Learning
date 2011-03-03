/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.hdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

/**
 *
 * @author nicholasbartlett
 */
public class BinaryExpansionDistribution extends DiscreteDistribution{

    private double b;
    private HashMap<Integer,MutableInt> counts;

    public BinaryExpansionDistribution(double b){
        if(b <= 0.0 || b > 1.0){
            throw new IllegalArgumentException("b must be in (0,1.0]");
        }
        this.b = b;
        counts = new HashMap<Integer, MutableInt>();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void adjustPseudoCount(int type, int adjustment){};

    /**
     * {@inheritDoc}
     */
    @Override
    public void sample() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public double score() {
        double score = 0d;
        for (Entry<Integer, MutableInt> entry : counts.entrySet()) {
            score += entry.getValue().value() * Math.log(probability(entry.getKey()));
        }
        return score;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double probability(int type) {
        double prob;
        if (type == 0){
            prob = 0.0;
        } else {
            prob = Math.pow((1d - b) / 2, 31 - Integer.numberOfLeadingZeros(type)) * b;
        }

        return prob;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int generate(Random rng) {
        double p0 = (1d - b) / 2;
        double p1 = 1d - b;
        double r;

        int sample = 1;
        for (int i = 0; i < 31; i++) {
            r = rng.nextDouble();
            if(r < p0) {
                sample <<= 1;
            } else if (r < p1) {
                sample <<= 1;
                sample++;
            } else {
                break;
            }
        }
        return sample;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<IntDoublePair> iterator() {
        return new Iter();
    }

    /**
     * Iterator for this particular distribution.
     */
    private class Iter implements Iterator<IntDoublePair> {
        
        private int next = 1;
        private double sum = 0d;

        @Override
        public boolean hasNext() {
            return next != 0;
        }

        @Override
        public IntDoublePair next() {
            IntDoublePair n;
            if (next != -1) {
                double p = Math.pow((1d - b) / 2d, 31 - Integer.numberOfLeadingZeros(next)) * b;
                 n = new IntDoublePair(next, p);
                next++;
                sum += p;
            } else {
                n = new IntDoublePair(next, 1d - sum);
            }
            return n;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
