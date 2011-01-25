/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.util;

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
            String binaryString = Integer.toBinaryString(type);

            prob = Math.pow((1d - b) / 2, binaryString.length() - 1);
            prob *= b;
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
    public Iterator<Pair<Integer, Double>> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
