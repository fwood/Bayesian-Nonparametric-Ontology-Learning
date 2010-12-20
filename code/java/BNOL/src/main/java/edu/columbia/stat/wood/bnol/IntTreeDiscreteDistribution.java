/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.Pair;
import java.util.Iterator;

/**
 *
 * @author nicholasbartlett
 */
public class IntTreeDiscreteDistribution implements IntDiscreteDistribution {
    private MutableDouble b;

    public IntTreeDiscreteDistribution(MutableDouble b){
        this.b = b;
    }

    public double probability(int type) {
        if(type == 0 || type == 1){
            return (1.0 - b.value()) / 2.0;
        } else if (type == 2){
            return b.value();
        } else {
            throw new IllegalArgumentException("Type must be 0 or 1, not " + type);
        }
    }

    public Iterator<Pair<Integer, Double>> iterator() {
        return new IT();
    }

    private class IT implements Iterator<Pair<Integer, Double>> {
        int calls = 0;
        double db = b.value();

        public boolean hasNext() {
            return calls < 3;
        }

        public Pair<Integer, Double> next() {
            if(calls < 3){
                return new Pair(calls,probability(calls++));
            } else {
                throw new RuntimeException("No more values to iterator over");
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
