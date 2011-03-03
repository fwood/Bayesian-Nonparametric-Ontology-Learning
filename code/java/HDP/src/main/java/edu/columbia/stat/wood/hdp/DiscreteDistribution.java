/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.hdp;

import edu.columbia.stat.wood.hdp.DiscreteDistribution.IntDoublePair;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/**
 *
 * @author nicholasbartlett
 */
public abstract class DiscreteDistribution implements Iterable<IntDoublePair> {

    /**
     * Gets the probability of a given type in this distribution.
     * @param type
     * @return probability of that type
     */
    public abstract double probability(int type);

    /**
     * Adjusts the number of observed values recorded for a given type.
     * @param type
     * @param adjustment adjustment value (positive or negative)
     */
    public abstract void adjustObservedCount(int type, int adjustment);

    /**
     * Adjusts the number of values attributed to this object, but not directly
     * observed.
     * @param type
     * @param adjustment adjustment value
     */
    public abstract void adjustPseudoCount(int type, int adjustment);

    /**
     * Samples this discrete distribution and returns the log likelihood of the
     * the sampled distribution.
     * @return joint score of sampled distribution
     */
    public abstract void sample();
    
    /**
     * Gets the joint likelihood of data observed in this discrete distribution 
     * along with the likelihood of the current distribution representation.
     * @return
     */
    public abstract double score();

    /**
     * Gets an iterator over the pairs of type values and their probabilities
     * for this discrete distribution.
     * @return iterator over type probability pairs
     */
    @Override
    public abstract Iterator<IntDoublePair> iterator();

    /**
     * Generates a random variate from this discrete distribution.
     * @param rng random number generator
     * @return random sample
     */
    public int generate(Random rng) {
        double cuSum = 0d, r = rng.nextDouble();
        for(IntDoublePair idp : this) {
            cuSum += idp.doubleValue();
            if(cuSum > r) {
                return idp.intValue();
            }
        }
        throw new RuntimeException("This should not happen, likely means that"
                + " the iterator over the probability distribution does not"
                + " sum to one.  Current value of the cumulative sum is "
                + cuSum + ", and the random number used for this generation"
                + " is" + r);
    }

    /**
     * Gets an iterator with a set of excluded types.
     * @param exclusionSet set of types to exclude from iterator return
     * @return iterator over non-excluded types
     */
    public Iterator<IntDoublePair> iterator(HashSet<Integer> exclusionSet) {
        return new IteratorWithExclusionSet(exclusionSet, iterator());
    }

    /**
     * Container object for one int value and one double value.  The object is
     * immutable and the hashcode and value only reflect the underlying values
     * in the container.
     */
    public static class IntDoublePair {
        /**
         * Raw values in the container.
         */
        private int i;
        private double d;

        /**
         * Creates an IntDobulePair with the specified values
         * @param i int value
         * @param d double value
         */
        public IntDoublePair(int i, double d) {
            this.i = i;
            this.d = d;
        }

        /**
         * Null constructor.
         */
        public IntDoublePair() {}

        /**
         * Gets the underlying int value.
         * @return underlying int value
         */
        public int intValue() {
            return i;
        }

        /**
         * Gets the underlying double value.
         * @return underlying double value
         */
        public double doubleValue(){
            return d;
        }

        /**
         * Overrides hashcode so that it only reflects the values housed in this
         * container object.
         * @return hashcode value
         */
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + this.i;
            hash = 79 * hash + (int) (Double.doubleToLongBits(this.d) ^ (Double.doubleToLongBits(this.d) >>> 32));
            return hash;
        }

        /**
         * Overrides equals method so that it only reflects the values housed in
         * this container object.
         * @param object comparison object
         * @return true of equal to object, else false
         */
        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            } else if (object.getClass() == getClass()) {
                IntDoublePair castObject = (IntDoublePair) object;
                if (castObject.i == i && castObject.d == d) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * General implementation of a wrapper to make an iterator into an iterator
     * with an exclusion set.
     */
    private static class IteratorWithExclusionSet implements Iterator<IntDoublePair> {

        HashSet<Integer> exclusionSet;
        IntDoublePair next;
        Iterator<IntDoublePair> baseIterator;

        public IteratorWithExclusionSet(HashSet<Integer> exclusionSet, Iterator<IntDoublePair> baseIterator) {
            this.exclusionSet = exclusionSet;
            this.baseIterator = baseIterator;

            if (baseIterator.hasNext()) {
                next = baseIterator.next();
            } else {
                next = null;
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public IntDoublePair next() {
            IntDoublePair currentNext = next;

            if (baseIterator.hasNext()) {
                next = baseIterator.next();
                while (exclusionSet.contains(next.intValue())){
                    if (baseIterator.hasNext()) {
                        next = baseIterator.next();
                    } else {
                        next = null;
                        break;
                    }
                }
            } else {
                next = null;
            }

            return currentNext;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
