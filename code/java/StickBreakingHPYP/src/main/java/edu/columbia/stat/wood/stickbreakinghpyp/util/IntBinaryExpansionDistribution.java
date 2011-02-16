/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp.util;

import java.util.Arrays;
import java.util.Iterator;

/**
 *
 * @author nicholasbartlett
 */
public class IntBinaryExpansionDistribution extends IntDiscreteDistribution {

    private double b;

    public IntBinaryExpansionDistribution(double b){
        if(b <= 0.0 || b > 1.0){
            throw new IllegalArgumentException("b must be in (0,1.0]");
        }
        this.b = b;
    }

    @Override
    public double probability(int type) {
        double prob;
        if (type <= 0){
            prob = 0.0;
        } else {
            prob = Math.pow((1d - b) / 2, 31 - Integer.numberOfLeadingZeros(type)) * b;
        }

        return prob;
    }

    @Override
    public int generate(MersenneTwisterFast rng) {
        double p0 = (1d - b) / 2;
        double p1 = 1d - b;
        double r;

        int sample = 1;
        for (int i = 0; i < 30; i++) {
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

    @Override
    public Iterator<IntDoublePair> iterator() {
        return new Iter();
    }

    private class Iter implements Iterator<IntDoublePair> {
        
        private int next = 1;

        public boolean hasNext() {
            return next > 0;
        }

        public IntDoublePair next() {
            IntDoublePair n = new IntDoublePair(next, Math.pow((1d - b) / 2d, 31 - Integer.numberOfLeadingZeros(next)) * b);
            next++;
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
