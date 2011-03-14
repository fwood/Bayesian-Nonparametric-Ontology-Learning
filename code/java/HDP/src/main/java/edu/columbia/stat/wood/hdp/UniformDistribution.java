/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.hdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 *
 * @author nicholasbartlett
 */
public class UniformDistribution extends DiscreteDistribution {

    private final double p;
    private HashMap<Integer,MutableInt> counts;
    private int alphabetSize;
    private int offset;
    

    public UniformDistribution (int alphabetSize) {
        this.alphabetSize = alphabetSize;
        p = 1d / (double) alphabetSize;
        counts = new HashMap<Integer,MutableInt>();
        this.offset = 0;
    }
    
    public UniformDistribution(int alphabetSize, int offset) {
        this.alphabetSize = alphabetSize;
        p = 1d / (double) alphabetSize;
        counts = new HashMap<Integer,MutableInt>();
        this.offset = offset;
    }
    

    @Override
    public double probability(int type) {
        if (type >= offset && type < (offset + alphabetSize)) {
            return p;
        } else { 
            return 0d;
        }
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
        for (Entry<Integer, MutableInt> entry : counts.entrySet()){
            score += entry.getValue().value() * Math.log(probability(entry.getKey()));
        }
        return score;
    }

    @Override
    public Iterator<IntDoublePair> iterator() {
        return new UniformIterator();
    }

    private class UniformIterator implements Iterator<IntDoublePair> {
        private int next = offset;

        @Override
        public boolean hasNext() {
            return next < (alphabetSize + offset);
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
