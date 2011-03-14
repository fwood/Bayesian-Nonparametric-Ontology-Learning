/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.ihmm;

import edu.columbia.stat.wood.hdp.DiscreteDistribution.IntDoublePair;
import edu.columbia.stat.wood.hdp.RND;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;

/**
 *
 * @author nicholasbartlett
 */
public class MultinomialLikelihood extends Likelihood{

    private HashMap<Integer, double[]> distributions;
    private HashMap<Integer, int[]> counts;
    int alphabetSize;
    double alpha = 1;
    int states;

    public MultinomialLikelihood(int alphabetSize, int states) {
        distributions = new HashMap<Integer, double[]>();
        counts = new HashMap<Integer,int[]>();

        double[] params = new double[alphabetSize];
        for (int i = 0; i < alphabetSize; i++) {
            params[i] = alpha / (double) alphabetSize;
        }

        for (int i = 1; i <= states; i++) {
            counts.put(i,new int[alphabetSize]);
            distributions.put(i, RND.sampleDirichlet(params));
        }

        this.states = states;
        this.alphabetSize = alphabetSize;
    }

    public int count() {
        int count = 0;
        for (int[] c : counts.values()) {
            for (int cc : c) {
                count += cc;
            }
        }
        return count;
    }

    public void print(int states) {
        for (int i = 1; i <= states; i++) {
            double[] dist = distributions.get(i);
                System.out.println(Arrays.toString(dist));
        }
    }

    @Override
    public void sample() {
        double[] params;
        double[] sample;

        int[] count;
        for (Entry<Integer, int[]> entry : counts.entrySet()){
            count = entry.getValue();
            params = new double[alphabetSize];
            for (int i = 0; i < alphabetSize; i++) {
                params[i] = count[i] + alpha / (double) alphabetSize;
            }
            sample = RND.sampleDirichlet(params);
            System.arraycopy(sample, 0, distributions.get(entry.getKey()), 0, alphabetSize);
        }
    }

    @Override
    public double probability(int state, int observation) {
        return distributions.get(state)[observation];
    }

    @Override
    public void adjustCount(int state, int observation, int multiplicity) {
        counts.get(state)[observation] += multiplicity;
        assert counts.get(state)[observation] >= 0;
    }

    public Iterator<IntDoublePair> iterator(int state) {
        TreeSet<IntDoublePair> set = new TreeSet<IntDoublePair>(new IntDoublePairComparator());
        double[] distribution = distributions.get(state);

        if (distribution != null) {
            for (int i = 0; i < alphabetSize; i++) {
                set.add(new IntDoublePair(i,distribution[i]));
            }
        }

        return set.descendingIterator();
    }

    @Override
    public double score() {
        double[] params = new double[alphabetSize];
        double score = 0d;

        for (int i = 0; i < alphabetSize; i++) {
            params[i] = alpha / (double) alphabetSize;
        }

        int[] count;
        double[] distribution;
        for (Entry<Integer, int[]> entry : counts.entrySet()){
            count = entry.getValue();
            distribution = distributions.get(entry.getKey());

            score += RND.logDirichletLikelihood(distribution, params);

            for (int i = 0; i < alphabetSize; i++) {
                score += Math.log(distribution[i]) * (double) count[i];
            }
        }

        return score;
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
