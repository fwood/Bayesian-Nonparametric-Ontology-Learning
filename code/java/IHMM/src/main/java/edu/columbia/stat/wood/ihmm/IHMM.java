/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.ihmm;

import edu.columbia.stat.wood.stickbreakinghpyp.HPYP;
import edu.columbia.stat.wood.stickbreakinghpyp.util.BinaryContext;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntBinaryExpansionDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDoublePair;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MersenneTwisterFast;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MutableDouble;
import edu.columbia.stat.wood.stickbreakinghpyp.util.RND;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 *
 * @author nicholasbartlett
 */
public class IHMM {

    public static void main(String[] args) {
        long seed = new java.util.Random().nextLong();
        System.out.println("seed = " + seed);

        IHMM hmm = new IHMM();
        RND.setRNG(hmm.rng = new MersenneTwisterFast(seed));

        hmm.artificialGenerate(8000);
        int[] truth = hmm.s;

        System.out.println(Arrays.toString(hmm.s));
        //System.out.println(Arrays.toString(hmm.y));

        hmm.initStates();
        hmm.initTransitionMatrix(new IntBinaryExpansionDistribution(0.4));
        hmm.initLikelihood(4);

        //System.out.println(Arrays.toString(hmm.s));

        for (int i = 0; i++ < 250;) {
            hmm.sample();
            System.out.println(hmm.score());// + ", " + Arrays.toString(hmm.s));
        }

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
        }
    }

    public void artificialGenerate(int n) {
        int vocabSize = 4;

        s = new int[n];
        y = new int[n];

        double[] state_00 = new double[]{1d, 0d, 0d, 0d};
        double[] state_01 = new double[]{0d, 1d, 0d, 0d};

        double[] state_0 = new double[]{0.5, 0.5, 0d, 0d};

        double[] state_10 = new double[]{0d, 0d, 1d, 0d};
        double[] state_11 = new double[]{0d, 0d, 0d, 1d};

        double[] state_1 = new double[]{0d, 0d, 0.5, 0.5};
        
        double[] state_ = new double[]{0.25, 0.25, 0.25, 0.25};

        s[0] = 1;
        for (int i = 1; i < n; i++) {
            if (rng.nextDouble() <= 0.05) {
                s[i] = s[i - 1];
            } else {
                s[i] = (s[i - 1] + 1) % 8;
                if (s[i] == 0) {
                    s[i]++;
                }
            }
        }

        double[] pmf;
        double cuSum, randomNumber;
        top_for:
        for (int i = 0; i < n; i++) {
            pmf = null;
            switch (s[i]) {
                case 1:
                    pmf = state_;
                    break;
                case 2:
                    pmf = state_0;
                    break;
                case 3:
                    pmf = state_1;
                    break;
                case 4:
                    pmf = state_00;
                    break;
                case 5:
                    pmf = state_01;
                    break;
                case 6:
                    pmf = state_10;
                    break;
                case 7:
                    pmf = state_11;
                    break;
            }

            if (pmf == null) {
                System.out.println(s[i]);
            }

            cuSum = 0d;
            randomNumber = rng.nextDouble();
            for (int j = 0; j < vocabSize; j++) {
                cuSum += pmf[j];
                if (cuSum > randomNumber) {
                    y[i] = j;
                    continue top_for ;
                }
            }
            System.out.println("r = " + randomNumber + "cuSum = " + cuSum);
            throw new RuntimeException("should not get here");
        }
    }

    /*****************************REAL CODE STARTS HERE************************/
    
    public int[] y;
    public MersenneTwisterFast rng;
    
    private int[] s;
    private double[] u;
    private HPYP transitionMatrix;
    private HPYP likelihood;

    public void initStates() {
        s = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = (int) (10 * rng.nextDouble()) + 1;
        }
    }

    public void initTransitionMatrix(IntDiscreteDistribution baseDistribution) {
        MutableDouble[] conc = new MutableDouble[]{new MutableDouble(0.4), new MutableDouble(3.8)};
        MutableDouble[] disc = new MutableDouble[]{new MutableDouble(.2d), new MutableDouble(0.4d)};

        transitionMatrix = new HPYP(disc, conc, baseDistribution, 1d, 100d);

        transitionMatrix.adjustCount(null, s[0], 1);
        for (int i = 1; i < s.length; i++) transitionMatrix.adjustCount(s[i - 1], s[i], 1);

        for (int i = 0; i < 100; i++) transitionMatrix.sampleWeights();
    }

    private void initLikelihood(int vocabSize) {
        
        MutableDouble[] conc = new MutableDouble[]{new MutableDouble(10), new MutableDouble(30)};
        MutableDouble[] disc = new MutableDouble[]{new MutableDouble(.8d), new MutableDouble(0.9d)};

        likelihood = new HPYP(disc, conc, new IntUniformDiscreteDistribution(vocabSize), 1d, 100d);

        for(int i = 0; i < s.length; i++) likelihood.adjustCount(BinaryContext.toExpansion(s[i]), y[i], 1);

        for (int i = 0; i < 100; i++) likelihood.sampleWeights();
    }

    private double likelihood(int state, int observation) {
        return likelihood.probability(BinaryContext.toExpansion(state), observation);
    }

    public void sample() {

        sampleAuxiliary(1d, 1000d);
        sampleStates();
        for (int i = 0; i < 10; i++) likelihood.sampleWeights();
        for (int i = 0; i < 10; i++) transitionMatrix.sampleWeights();
    }

    public double score() {
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
        likelihood.adjustCount(BinaryContext.toExpansion(s[0]), y[0], -1);
        likelihood.adjustCount(BinaryContext.toExpansion(sNew[0]), y[0], 1);
        for (int i = 1; i < s.length; i++) {
            transitionMatrix.adjustCount(s[i - 1], s[i], -1);
            transitionMatrix.adjustCount(sNew[i - 1], sNew[i], 1);
            likelihood.adjustCount(BinaryContext.toExpansion(s[i]), y[i], -1);
            likelihood.adjustCount(BinaryContext.toExpansion(sNew[i]), y[i], 1);
        }

        s = sNew;
    }

    private void normalizeAndMultByY(HashMap<Integer, MutableDouble> fe, int index) {
        double m = 0d;
        for (Entry<Integer, MutableDouble> entry : fe.entrySet()) {
            entry.getValue().timesEquals(likelihood(entry.getKey(), y[index]));
            m = m > entry.getValue().value() ? m : entry.getValue().value();
        }

        assert m > 0d : "index = " + index;

        for (MutableDouble p : fe.values()) {
            p.timesEquals(1d / m);
        }
    }
    
    private class DecreasingStates {

        private Iterator<IntDoublePair> iter;
        public int[] types = new int[0];
        public double[] probs = new double[0];

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
                    newTypes.add(pair.i);
                    newProbs.add(pair.d);
                    if (pair.d <= minProb) {
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
