/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.util;

import java.util.Iterator;

/**
 *
 * @author nicholasbartlett
 */
public class IntArrayDiscreteDistribution implements IntDiscreteDistribution{

    private double[] p;

    public IntArrayDiscreteDistribution(double[] p){
        this.p = p;
    }

    public double probability(int type) {
        return p[type];
    }

    public Iterator<Pair<Integer, Double>> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
