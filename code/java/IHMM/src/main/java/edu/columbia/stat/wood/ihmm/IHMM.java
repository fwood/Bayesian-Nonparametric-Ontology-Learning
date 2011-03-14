/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.ihmm;

import edu.columbia.stat.wood.hdp.DiscreteDistribution;
import edu.columbia.stat.wood.hdp.DiscreteDistribution.IntDoublePair;
import edu.columbia.stat.wood.hdp.HierarchicalDirichletProcess;
import edu.columbia.stat.wood.hdp.RND;
import edu.columbia.stat.wood.hdp.UniformDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MutableDouble;
import edu.columbia.stat.wood.stickbreakinghpyp.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

/**
 *
 * @author nicholasbartlett
 */
public class IHMM {

    
    public static void main(String[] args) {
        int n = 10000;
        System.out.println("n = " + n);

        long seed = new java.util.Random().nextLong();
        System.out.println("seed = " + seed);

        IHMM hmm = new IHMM();
        hmm.rng = new Random();
        Pair<int[],int[]> data = (new Generate4StateSynthetic()).generate(n, hmm.rng);

        hmm.s = data.first();
        hmm.y = data.second();
        hmm.initStates();

        //System.out.println("obs = " + Arrays.toString(hmm.y));
        //System.out.println();
        
        //System.out.println("states = " + Arrays.toString(hmm.s));
        
        hmm.initTransitionMatrix(new UniformDistribution(20,1));
        hmm.initLikelihood();

        //System.out.println();
        //((MultinomialLikelihood) hmm.likelihood).print(20);
        System.out.println();
        //hmm.transitionMatrix.print();

        System.out.println();
        for (int i = 0; i++ < 1000;) {
            hmm.sample();
            System.out.println(hmm.score());// + ", " + ((MultinomialLikelihood) hmm.likelihood).count());
        }
        
        ((MultinomialLikelihood) hmm.likelihood).print(20);

        System.out.println(Arrays.toString(hmm.s));

        /*
        HashSet<Integer> states = new HashSet<Integer>();
        for (int state : hmm.s) {
            states.add(state);
        }

        for (Integer state : states) {
            System.out.print("State = " + Arrays.toString(BinaryContext.toExpansion(state)) + ", [" + hmm.likelihood(state,0));
            for (int i = 1; i < 4; i++) {
                System.out.print(", " + hmm.likelihood(state,i));
            }
            System.out.println("]");
        }*/
    }
     

    /*****************************REAL CODE STARTS HERE************************/
    
    public int[] y;
    public Random rng;
    
    private int[] s;
    private double[] u;
    private HierarchicalDirichletProcess transitionMatrix;
    private Likelihood likelihood;

    public void initStates() {
        s = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = (int) (20 * rng.nextDouble()) + 1;
        }
    }

    public void initTransitionMatrix(DiscreteDistribution baseDistribution) {
        transitionMatrix = new HierarchicalDirichletProcess(new double[]{20d, 30d}, baseDistribution, 100d);

        transitionMatrix.adjustCount(null, s[0], 1);
        for (int i = 1; i < s.length; i++) transitionMatrix.adjustCount(s[i - 1], s[i], 1);
        for (int i = 0; i < 20; i++) transitionMatrix.sample();
    }

    private void initLikelihood() {
        likelihood = new MultinomialLikelihood(3,20);
        
        for (int i = 0; i < s.length; i++) likelihood.adjustCount(s[i], y[i], 1);
        for (int i = 0; i < 20; i++) likelihood.sample();
    }

    public void sample() {
        int beta = 1;
        double cuSum = 0.1, r = rng.nextDouble();
        double p = 0.1;
        double q = 0.9;

        while (r >= cuSum) {
            p *= q;
            cuSum += p;
            beta++;
        }

        System.out.print(beta + ", ");

        sampleAuxiliary(1d, (double) beta);
        sampleStates();
        for (int i = 0; i < 10; i++) likelihood.sample();
        for (int i = 0; i < 10; i++) transitionMatrix.sample();
    }

    public double score() {
        //System.out.print(transitionMatrix.score() + ", " + likelihood.score() + ", ");
        return transitionMatrix.score() + likelihood.score();
    }

    private void sampleAuxiliary(double alpha, double beta) {
        if (u == null) {
            u = new double[y.length];
        }

        u[0] = RND.sampleBeta(alpha, beta) * transitionMatrix.probability(null, s[0]);
        for (int i = 1; i < u.length; i++) {
            u[i] = RND.sampleBeta(alpha, beta) * transitionMatrix.probability(s[i - 1], s[i]);
        }
        
        //u = new double[y.length];
    }

    private void sampleStates() {
        DecreasingStates value;
        Integer key;
        MutableDouble prob;

        HashMap<Integer, DecreasingStates> probabilityMap = new HashMap<Integer, DecreasingStates>();
        HashMap<Integer, MutableDouble>[] forwardFilter = new HashMap[s.length];

        value = new DecreasingStates(transitionMatrix.iterator(null));
        value.fillDown(u[0]);
        probabilityMap.put(null, value);

        HashMap<Integer, MutableDouble> filterEntry = new HashMap<Integer, MutableDouble>();
        for (int i = 0; i < value.probs.length; i++) {
            filterEntry.put(value.types[i], new MutableDouble(value.probs[i]));
        }
        normalizeAndMultByY(filterEntry, 0);
        forwardFilter[0] = filterEntry;

        for (int i = 1; i < s.length; i++) {
            filterEntry = new HashMap<Integer, MutableDouble>();
            for (Entry<Integer, MutableDouble> entry : forwardFilter[i - 1].entrySet()) {
                key = entry.getKey();
                value = probabilityMap.get(key);
                if (value == null) {
                    value = new DecreasingStates(transitionMatrix.iterator(key));
                    probabilityMap.put(key, value);
                }
                value.fillDown(u[i]);

                for (int j = 0; j < value.probs.length; j++) {
                    if (value.probs[j] > u[i]) {
                        prob = filterEntry.get(value.types[j]);
                        if (prob == null) {
                            prob = new MutableDouble(0d);
                            filterEntry.put(value.types[j], prob);
                        }

                        prob.plusEquals(entry.getValue().value() * value.probs[j]);
                    } else {
                        break;
                    }
                }
            }
            normalizeAndMultByY(filterEntry, i);
            forwardFilter[i] = filterEntry;
        }

        /*
        for (HashMap<Integer, MutableDouble> fe : forwardFilter){
            System.out.print(fe.get(1).value());
            for (int i = 2; i <= 20; i++) {
                System.out.print(", " + fe.get(i).value());
            }
            System.out.println();
        }
        */

        // now do backwards sample
        double total = 0d, cuSum, r, pp;
        for (MutableDouble p : forwardFilter[s.length - 1].values()) {
            total += p.value();
        }

        cuSum = 0d;
        r = rng.nextDouble();
        int[] sNew = new int[s.length];
        for (Entry<Integer, MutableDouble> entry : forwardFilter[s.length - 1].entrySet()) {
            cuSum += entry.getValue().value() / total;
            if (cuSum > r) {
                sNew[s.length - 1] = entry.getKey();
                break;
            }
        }

        for (int i = (s.length - 2); i > -1; i--) {
            total = 0d;
            for (Entry<Integer, MutableDouble> entry : forwardFilter[i].entrySet()) {
                pp = transitionMatrix.probability(entry.getKey(), sNew[i + 1]);
                pp = pp > u[i + 1] ? pp : 0d;
                entry.getValue().timesEquals(pp);
                total += entry.getValue().value();
            }

            assert total > 0d;

            cuSum = 0d;
            r = rng.nextDouble();
            for (Entry<Integer, MutableDouble> entry : forwardFilter[i].entrySet()) {
                cuSum += entry.getValue().value() / total;
                if (cuSum > r) {
                    sNew[i] = entry.getKey();
                    break;
                }
            }
        }

        transitionMatrix.adjustCount(null, s[0], -1);
        transitionMatrix.adjustCount(null, sNew[0], 1);
        likelihood.adjustCount(s[0], y[0], -1);
        likelihood.adjustCount(sNew[0], y[0], 1);

        for (int i = 1; i < s.length; i++) {
            transitionMatrix.adjustCount(s[i - 1], s[i], -1);
            transitionMatrix.adjustCount(sNew[i - 1], sNew[i], 1);
            
            likelihood.adjustCount(s[i], y[i], -1);
            likelihood.adjustCount(sNew[i], y[i], 1);
        }

        s = sNew;
    }

    private void normalizeAndMultByY(HashMap<Integer, MutableDouble> fe, int index) {
        double m = 0d;
        for (Entry<Integer, MutableDouble> entry : fe.entrySet()) {
            entry.getValue().timesEquals(likelihood.probability(entry.getKey(), y[index]));
            m = m > entry.getValue().value() ? m : entry.getValue().value();
        }

        assert m > 0d : "index = " + index;

        for (MutableDouble p : fe.values()) {
            p.timesEquals(1d / m);
        }
    }
    
    private class DecreasingStates {

        public int[] types = new int[0];
        public double[] probs = new double[0];

        private Iterator<IntDoublePair> iter;

        public DecreasingStates(Iterator<IntDoublePair> iterator) {
            iter = iterator;
        }

        public void fillDown(double minProb) {
            if (probs.length > 0 && probs[probs.length - 1] <= minProb) {
                return;
            } else {
                ArrayList<Integer> newTypes = new ArrayList<Integer>();
                ArrayList<Double> newProbs = new ArrayList<Double>();
                IntDoublePair pair;
                while (iter.hasNext()) {
                    pair = iter.next();
                    newTypes.add(pair.intValue());
                    newProbs.add(pair.doubleValue());
                    if (pair.doubleValue() <= minProb) {
                        break;
                    }
                }

                int[] old_types = types;
                double[] old_probs = probs;

                types = new int[types.length + newTypes.size()];
                probs = new double[probs.length + newProbs.size()];
                System.arraycopy(old_types, 0, types, 0, old_types.length);
                System.arraycopy(old_probs, 0, probs, 0, old_probs.length);

                int index = old_types.length;
                for (Integer type : newTypes) {
                    types[index++] = type;
                }

                index = old_probs.length;
                for (Double prob : newProbs) {
                    probs[index++] = prob;
                }
            }
        }
    }
}
