/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.util;

import java.util.Iterator;

/**
 * Class to implement base distribution specific to the s emission distribution
 * used in BNOL.
 * @author nicholasbartlett
 */
public class IntTreeDiscreteDistribution implements IntDiscreteDistribution {
    private MutableDouble b;

    /***********************constructor methods********************************/

    public IntTreeDiscreteDistribution(MutableDouble b){
        this.b = b;
    }

    /***********************public methods*************************************/

    /**
     * Finds the probability of the given type.  Types with positive probability
     * are 0, 1 , and -1.  A 0 indicates a left turn down the tree, a 1 indicates
     * a right turn and a -1 indicates that the emission stops at that given
     * node.
     * @param type type to find probability of
     * @return probability of given type
     */
    public double probability(int type) {
        if(type == 0 || type == 1){
            return (1.0 - b.value()) / 2.0;
        } else if (type == -1){
            return b.value();
        } else {
            throw new IllegalArgumentException("Type must be 0, 1, or -1, not " + type);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int generate(MersenneTwisterFast rng){
        double randomNumber = rng.nextDouble(), cuSum = 0.0;
        int sample = -2;

        while(cuSum <= randomNumber && sample < 2){
            cuSum += probability(++sample);
        }

        if(cuSum <= randomNumber || cuSum > 1.0){
            throw new RuntimeException("Something bad happened, cuSum = " + cuSum + ", randomNumber = " + randomNumber);
        }

        return sample;
    }

    /**
     * Gets an iterator over the type, probability pairs of this distribution.
     * @return iterator
     */
    public Iterator<Pair<Integer, Double>> iterator() {
        return new IT();
    }

    /***********************private classes************************************/
    
    private class IT implements Iterator<Pair<Integer, Double>> {
        private int calls = -1;
        private double db = b.value();
    
        /***********************public methods*********************************/

        /**
         * Finds if there is another object to iterate over.
         * @return true if there is another object, else false
         */
        public boolean hasNext() {
            return calls < 2;
        }

        /**
         * Gets the next object in the iterator.
         * @return the next pair
         */
        public Pair<Integer, Double> next() {
            if(calls < 2){
                return new Pair(calls,probability(calls++));
            } else {
                throw new RuntimeException("No more values to iterator over");
            }
        }

        /**
         * Unsupported method!
         */
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
