/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.hdp;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;

/**
 *
 * @author nicholasbartlett
 */
public class DirichletProcessDistribution extends DiscreteDistribution {

    private DiscreteDistribution baseDistribution;
    private HashMap<Integer, NMW> counts;
    private MutableDouble concentration;
    private double weightForBaseDistribution;
    
    public DirichletProcessDistribution(DiscreteDistribution baseDistribution, MutableDouble concentration) {
        this.baseDistribution = baseDistribution;
        this.concentration = concentration;
        counts = new HashMap<Integer,NMW>();
        weightForBaseDistribution = 1d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double probability(int type) {
        NMW nmw = counts.get(type);
        if (nmw == null) {
            return weightForBaseDistribution * baseDistribution.probability(type);
        } else {
            return nmw.w + weightForBaseDistribution * baseDistribution.probability(type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void adjustObservedCount(int type, int adjustment) {
        NMW nmw = counts.get(type);
        if (nmw == null) {
            counts.put(type, nmw = new NMW());
            nmw.m = 1;
            baseDistribution.adjustPseudoCount(type, 1);
        }

        nmw.n_o += adjustment;
        
        if (nmw.n() == 0) {
            baseDistribution.adjustPseudoCount(type, -nmw.m);
            counts.remove(type);
        } else if (nmw.n() < 0) {
            throw new RuntimeException("Cannot remove so many counts of type " + type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void adjustPseudoCount(int type, int adjustment) {
        NMW nmw = counts.get(type);
        if (nmw == null) {
            counts.put(type, nmw = new NMW());
            nmw.m = 1;
            baseDistribution.adjustPseudoCount(type, 1);
        }

        nmw.n_p += adjustment;

        if (nmw.n() == 0) {
            baseDistribution.adjustPseudoCount(type, -nmw.m);
            counts.remove(type);
        } else if (nmw.n() < 0) {
            throw new RuntimeException("Cannot remove so many counts of type " + type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sample() {
        // sample weights
        double sum = 0d;
        double gamrndForBaseDistribution;

        for (NMW nmw : counts.values()) {
            nmw.w = RND.sampleGamma(nmw.n(), 1d);
            sum += nmw.w;
        }

        gamrndForBaseDistribution = RND.sampleGamma(concentration.value(), 1d);
        sum += gamrndForBaseDistribution;

        double factor = 1d / sum;
        for (NMW nmw : counts.values()) {
            nmw.w *= factor;
        }

        weightForBaseDistribution = gamrndForBaseDistribution * factor;

        // sample tables
        int old_m, key;
        NMW value;
        double conc = concentration.value();
        for (Entry<Integer, NMW> entry : counts.entrySet()) {
            value = entry.getValue();
            key = entry.getKey();
            
            old_m = value.m;
            value.m = RND.sampleDirichletProcessTables(value.n(), conc, baseDistribution.probability(key));

            baseDistribution.adjustPseudoCount(key, value.m - old_m);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double score() {
        double score = 0d;
        double conc = concentration.value();
        double baseProb;
        double baseSum = 0d;
        double sum = 0d;
        double prob;
        int key, index =0 ;
        NMW nmw;

        double[] dirichletParameters = new double[counts.size() + 1];
        double[] probabilityDistribution = new double[counts.size() + 1];

        for (Entry<Integer, NMW> entry : counts.entrySet()) {
            key = entry.getKey();
            nmw = entry.getValue();

            baseProb = baseDistribution.probability(key);
            baseSum += baseProb;

            prob = baseProb * weightForBaseDistribution + nmw.w;
            sum += prob;

            score += nmw.n_o * Math.log(prob);
            dirichletParameters[index] = conc * baseProb;
            
            probabilityDistribution[index++] = prob;
        }

        dirichletParameters[index] = conc * (1d - baseSum);
        probabilityDistribution[index] = 1d - sum;

        score += RND.logDirichletLikelihood(probabilityDistribution, dirichletParameters);

        return score;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<IntDoublePair> iterator() {
        TreeSet<IntDoublePair> set = new TreeSet<IntDoublePair>(new IntDoublePairComparator());
        HashSet<Integer> baseExclusionSet = new HashSet<Integer>();

        int key;
        for (Entry<Integer, NMW> entry : counts.entrySet()) {
            key = entry.getKey();
            baseExclusionSet.add(key);
            set.add(new IntDoublePair(key, entry.getValue().w + weightForBaseDistribution * baseDistribution.probability(key)));
        }

        return new InterleavedIterator(set.descendingIterator(), new IteratorTimesScalar(baseDistribution.iterator(baseExclusionSet), weightForBaseDistribution));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<IntDoublePair> iterator(HashSet<Integer> exclusionSet) {
        TreeSet<IntDoublePair> set = new TreeSet<IntDoublePair>(new IntDoublePairComparator());
        HashSet<Integer> baseExclusionSet = new HashSet<Integer>();

        int key;
        for (Entry<Integer, NMW> entry : counts.entrySet()) {
            key = entry.getKey();
            baseExclusionSet.add(key);
            if (!exclusionSet.contains(key)) {
                set.add(new IntDoublePair(key, entry.getValue().w + weightForBaseDistribution * baseDistribution.probability(key)));
            }
        }

        return new InterleavedIterator(set.descendingIterator(), new IteratorTimesScalar(baseDistribution.iterator(baseExclusionSet), weightForBaseDistribution));
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    public double logMetropolisRatio(double concentrationProposal) {
        double conc = concentration.value();
        double baseProb;
        double baseSum = 0d;
        double sum = 0d;
        double prob;
        int key, index =0 ;
        NMW nmw;

        double[] dirichletParametersProposal = new double[counts.size() + 1];
        double[] dirichletParameters = new double[counts.size() + 1];
        double[] probabilityDistribution = new double[counts.size() + 1];

        for (Entry<Integer, NMW> entry : counts.entrySet()) {
            key = entry.getKey();
            nmw = entry.getValue();

            baseProb = baseDistribution.probability(key);
            baseSum += baseProb;

            prob = baseProb * weightForBaseDistribution + nmw.w;
            sum += prob;

            dirichletParametersProposal[index] = concentrationProposal * baseProb;
            dirichletParameters[index] = conc * baseProb;
            probabilityDistribution[index++] = prob;
        }

        dirichletParametersProposal[index] = concentrationProposal * (1d - baseSum);
        dirichletParameters[index] = conc * (1d - baseSum);
        probabilityDistribution[index] = 1d - sum;

        return RND.logDirichletLikelihood(probabilityDistribution, dirichletParametersProposal)
               - RND.logDirichletLikelihood(probabilityDistribution, dirichletParameters);
    }

    public double logMHRatio(double proposalConcentration) {
        double alphaStar = proposalConcentration;
        double alpha = concentration.value();
        double logGammaAlphaStar = RND.logGamma(alphaStar);
        double logGammaAlpha = RND.logGamma(alpha);
        double logAlphaRatio = Math.log(alphaStar) - Math.log(alpha);

        double logMHRatio = counts.size() * (logGammaAlphaStar -  logGammaAlpha);

        int m, n;
        for (NMW nmw : counts.values()) {
            m = nmw.m;
            n = nmw.n();
            logMHRatio += m * (logAlphaRatio) - RND.logGamma(n + alphaStar) + RND.logGamma(n + alpha);
        }

        return logMHRatio;
    }

    /**
     * Container object for one int value and one double value.  The object is
     * mutable and the hashcode and value only reflect the underlying values
     * in the container.
     */
    private static class NMW {
        /**
         * Raw values in the container.
         */
        public int n_o = 0;
        public int n_p = 0;
        public int m = 0;
        public double w = 0;

        public int n() {
            return n_o + n_p;
        }
    }

    private static class IteratorTimesScalar implements Iterator<IntDoublePair> {

        private Iterator<IntDoublePair> iterator;
        private double scalar;

        public IteratorTimesScalar(Iterator<IntDoublePair> iterator, double scalar) {
            this.iterator = iterator;
            this.scalar = scalar;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public IntDoublePair next() {
            IntDoublePair rawNext = iterator.next();
            return new IntDoublePair(rawNext.intValue(), scalar * rawNext.doubleValue());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static class InterleavedIterator implements Iterator<IntDoublePair> {

        private Iterator<IntDoublePair> iter1;
        private Iterator<IntDoublePair> iter2;
        private IntDoublePair next1;
        private IntDoublePair next2;


        public InterleavedIterator(Iterator<IntDoublePair> iter1, Iterator<IntDoublePair> iter2) {
            this.iter1 = iter1;
            this.iter2 = iter2;

            if (iter1.hasNext()) {
                next1 = iter1.next();
            } else {
                next1 = null;
            }

            if (iter2.hasNext()) {
                next2 = iter2.next();
            } else {
                next2 = null;
            }
        }

        @Override
        public boolean hasNext() {
            return next1 != null || next2 != null;
        }

        @Override
        public IntDoublePair next() {
            if (next1 == null && next2 == null) {
                return null;
            } else if (next1 == null) {
                IntDoublePair next;
                next = next2;
                if (iter2.hasNext()) {
                    next2 = iter2.next();
                } else {
                    next2 = null;
                }
                return next;
            } else if (next2 == null ) {
                IntDoublePair next;
                next = next1;
                if (iter1.hasNext()) {
                    next1 = iter1.next();
                } else {
                    next1 = null;
                }
                return next;
            }  else if (next2.doubleValue() > next1.doubleValue()) {
                IntDoublePair next;
                next = next2;
                if (iter2.hasNext()) {
                    next2 = iter2.next();
                } else {
                    next2 = null;
                }
                return next;
            } else {
                IntDoublePair next;
                next = next1;
                if (iter1.hasNext()) {
                    next1 = iter1.next();
                } else {
                    next1 = null;
                }
                return next;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static class IntDoublePairComparator implements Comparator<IntDoublePair> {

        @Override
        public int compare(IntDoublePair aObject, IntDoublePair bObject) {
            double a = aObject.doubleValue();
            double b = bObject.doubleValue();

            if (a > b) {
                return 1;
            } else if (a < b) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
