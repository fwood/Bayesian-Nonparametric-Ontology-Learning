/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.stickbreakinghpyp;

import edu.columbia.stat.wood.stickbreakinghpyp.util.DoubleArrayList;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDoublePair;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MersenneTwisterFast;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MutableDouble;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MutableInt;
import edu.columbia.stat.wood.stickbreakinghpyp.util.RND;
import edu.columbia.stat.wood.stickbreakinghpyp.util.SampleWithoutReplacement;
import java.util.Arrays;
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
public class Restaurant extends HashMap<Integer, Restaurant> {
    
    private Restaurant parent;
    private double probabilityOfBackOff;
    private HashMap<Integer, TypeWeights> tableWeights;
    public MutableDouble concentration, discount;

    /***************constructor methods****************************************/
    public Restaurant(Restaurant parent, MutableDouble concentration, MutableDouble discount) {
        this.parent = parent;
        this.concentration = concentration;
        this.discount = discount;

        probabilityOfBackOff = 1d;
        tableWeights = new HashMap<Integer, TypeWeights>();
    }

    /***************public methods*********************************************/
    /**
     * Gets the probability of a given type.
     * @param type type for which probability is desired
     * @return probability of type
     */
    public double probability(int type) {
        double probability = probabilityOfBackOff * parent.probability(type);

        TypeWeights value = tableWeights.get(type);
        if (value != null) {
            probability += value.totalWeight();
        }

        return probability;
    }

    /**
     * Adjusts the count of the given type by a given amount/
     * @param type type to adjust count of
     * @param multiplicity amount to adjust count by
     */
    public void adjustCount(int type, int multiplicity) {
        if (multiplicity == 0) {
            return;
        } else {
            TypeWeights value = tableWeights.get(type);

            if (value == null) {
                value = new TypeWeights();
                tableWeights.put(type, value);
            }

            value.adjustCount(multiplicity);
        }
    }

    /**
     * Gibbs samples the restaurant.
     * @param rng random number generator
     */
    public boolean sample(MersenneTwisterFast rng) {
        int[] types = new int[tableWeights.size()];
        int[] innovationCounts = new int[types.length];
        double[] parentProbabilities = new double[types.length];

        // go through map and sample assignments of each piece
        int key, index = 0, tables = 0, count = 0;
        TypeWeights value;
        double parentProb;
        for (Entry<Integer, TypeWeights> entry : tableWeights.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();

            types[index] = key;
            count += value.count;
            
            if (value.count == 0) {
                value.assignments = new int[0];
            } else if (value.count == 1) {
                value.assignments = new int[]{1};
            } else {
                parentProb = parent.probability(key);
                parentProbabilities[index] = parentProb;
                innovationCounts[index] = value.sampleAssignments(parentProb, probabilityOfBackOff, rng);
            }

            tables += value.weights.length;
            index++;
        }

        if (count == 0) {
            return true;
        } else {
            // get a seating arrangment for the innovation seatings
            int[][] innovationSeatings = sampleSeatingArrangements(parentProbabilities, innovationCounts, tables, 10, rng);

            // get the dirichlet parameters to divide up the weight between the types
            // and remove types with 0 counts
            double[] dirichletParameters = new double[types.length + 1];
            tables = 0;

            for (int i = 0; i < types.length; i++) {
                value = tableWeights.get(types[i]);

                if (value.count == 0) {
                    tableWeights.remove(types[i]);
                    parent.adjustCount(types[i], -1 * value.weights.length);
                    dirichletParameters[i] = -1d;
                } else {
                    if (innovationSeatings[i].length > 0) {
                        int[] old_assignments = value.assignments;
                        value.assignments = new int[old_assignments.length + innovationSeatings[i].length];
                        System.arraycopy(old_assignments, 0, value.assignments, 0, old_assignments.length);
                        System.arraycopy(innovationSeatings[i], 0, value.assignments, old_assignments.length, innovationSeatings[i].length);
                    }

                    parent.adjustCount(types[i], value.assignments.length - value.weights.length);
                    tables += value.assignments.length;
                    dirichletParameters[i] = value.count - discount.value() * (double) value.assignments.length;
                }
            }

            dirichletParameters[dirichletParameters.length - 1] = discount.value() * (double) (tables + 1) + concentration.value();
            double[] dirichletSample = RND.sampleDirichlet(dirichletParameters);

            probabilityOfBackOff = dirichletSample[dirichletSample.length - 1];
            for (int i = 0; i < types.length; i++) {
                if (dirichletSample[i] != Double.NEGATIVE_INFINITY) {
                    assert dirichletSample[i] > 0d;
                    tableWeights.get(types[i]).sampleWeights(dirichletSample[i], discount.value());
                }
            }
            return false;
        }
    }

     /*public double score(){
        double score = 0.0, d = discount.value(), c = concentration.value();

        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            score += iterator.value().score(d);
        }

        for(int table = 1; table < tables; table++){
            score += Math.log((double) table * d + c);
        }

        for(int customer = 1; customer < customers; customer++){
            score -= Math.log((double) customer + c);
        }

        return score;
    }*/

    public double score(){
        double disc = discount.value();
        double conc = concentration.value();
        double score = 0d;

        int tables = 0;
        int customers = 0;
        int[] assignments;
        
        for (TypeWeights value : tableWeights.values()) {
            score += value.score(disc);

            customers += value.count;
            tables += value.assignments.length;
        }

        for(int table = 1; table < tables; table++){
            score += Math.log((double) table * disc + conc);
        }

        for(int customer = 1; customer < customers; customer++){
            score -= Math.log((double) customer + conc);
        }

        return score;
    }

    public void scoreByDepth(double[] score, int currentDepth) {
        for (Restaurant child : values()) {
            child.scoreByDepth(score, currentDepth + 1);
        }

        if (currentDepth >= score.length) {
            score[score.length - 1] += score();
        } else {
            score[currentDepth] += score();
        }
    }

    public double scoreSubtree() {
        double score = score();
        for (Restaurant child : values()) {
            score += child.scoreSubtree();
        }
        return score;
    }

    public boolean sampleSubtree(MersenneTwisterFast rng) {
        HashSet<Integer> removeKeys = new HashSet<Integer>();

        for (Entry<Integer,Restaurant> entry : entrySet()) {
            Integer key = entry.getKey();
            Restaurant child = entry.getValue();
            if (child.sampleSubtree(rng)) {
                removeKeys.add(key);
            }
        }

        for (Integer key : removeKeys) {
            remove(key);
        }
        
        return sample(rng);
    }

    public boolean checkCounts() {
        if (isEmpty()) {
            return true;
        } else {
            HashMap<Integer, MutableInt> map = new HashMap<Integer, MutableInt>();
            for (Entry<Integer, TypeWeights> entry : tableWeights.entrySet()) {
                map.put(entry.getKey(), new MutableInt(entry.getValue().count));
            }

            boolean check = true;

            for (Restaurant child : values()) {

                for (Entry<Integer, TypeWeights> entry : child.tableWeights.entrySet()) {
                    map.get(entry.getKey()).plusEquals(-1 * entry.getValue().weights.length);
                }
                
                check = child.checkCounts();
            }

            for (MutableInt mi : map.values()) {
                if (mi.value() != 0) {
                    check = false;
                }
            }
            return check;
        }
    }

    @Override
    public String toString() {
        String string = "";
        double probSum = 0d;
        for (Entry<Integer, TypeWeights> entry : tableWeights.entrySet()) {
            string += "Type = " + entry.getKey() + " \n";
            string += "Weights = " + Arrays.toString(entry.getValue().weights) + "\n";
            string += "Assignments = " + Arrays.toString(entry.getValue().assignments) + "\n";
            probSum += entry.getValue().totalWeight();
        }
        string += "Probability of backoff = " + probabilityOfBackOff + ", total prob = " + (probabilityOfBackOff + probSum);

        return string;
    }

    public Iterator<IntDoublePair> partialSortedDistribution() {
        return new SortedPartialDistributionIterator();
    }

    private int[][] sampleSeatingArrangements(double[] parentProbabilities, int counts[], int totalTables, int iterations, MersenneTwisterFast rng){
        int tables = totalTables;
        int customersToSample = 0;
        TSA[] seatings = new TSA[counts.length];
        int[][] seatingArrangements = new int[counts.length][];

        // count total number of customers
        for (int i = 0; i < counts.length; i++){
            assert counts[i] >= 0;
            if (counts[i] > 1) {
                customersToSample += counts[i];
                seatings[i] = new TSA();
            } else if (counts[i] == 0) {
                seatingArrangements[i] = new int[0];
            } else {
                seatingArrangements[i] = new int[]{1};
                tables++;
            }
        }

        // seat customers in random order
        int[] randomIndexOrder = getRandomIndexOrder(counts, customersToSample, rng);
        for (int index : randomIndexOrder) {
            if (seatings[index].seat(parentProbabilities[index], tables, rng)){
                tables++;
            }
        }

        // do gibs sampling
        for (int i = 0; i < iterations; i++) {
            randomIndexOrder = getRandomIndexOrder(counts, customersToSample, rng);
            
            for (int index : randomIndexOrder) {
                if (seatings[index].unseat(rng)) {
                    tables--;
                }
                
                if (seatings[index].seat(parentProbabilities[index], tables, rng)) {
                    tables++;
                }
            }
        }

        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 1) {
                seatingArrangements[i] = seatings[i].getSeatingArrangement();
            }
        }
        
        return seatingArrangements;
    }

    private static int[] getRandomIndexOrder(int[] counts, int customersToSample, MersenneTwisterFast rng) {
        int[] randomIndexOrder = new int[customersToSample];
        int[] randomOrder = SampleWithoutReplacement.sampleWithoutReplacement(customersToSample, rng);

        int index = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 1) {
                for (int c = 0; c < counts[i]; c++) {
                    randomIndexOrder[randomOrder[index++]] = i;
                }
            }
        }

        assert index == customersToSample;

        return randomIndexOrder;
    }

    private class TSA {
        private int[] sa = null;
        private int t = 0;
        private int c = 0;
        private double disc = discount.value();
        private double conc = concentration.value();

        public boolean seat(double parentProb, int tables, MersenneTwisterFast rng){
            if (sa == null) {
                sa = new int[]{1};
                c++;
                t++;
                return true;
            }
            
            double totalWeight = (double) c - disc * (double) t + (disc * (double) tables + conc) * parentProb;
            double cuSum = 0.0;
            double randomNumber = rng.nextDouble();
            int zeroIndex = -1;

            for (int i = 0; i < sa.length; i++) {
                if (sa[i] > 0) {
                    cuSum += ((double) sa[i] - disc) / totalWeight;
                    if (cuSum > randomNumber) {
                        sa[i]++;
                        c++;
                        return false;
                    }
                } else {
                    zeroIndex = i;
                }
            }

            if (zeroIndex > -1) {
                sa[zeroIndex] = 1;
            } else {
                int[] old_sa = sa;
                sa = new int[sa.length + 1];
                System.arraycopy(old_sa, 0, sa, 0, old_sa.length);
                sa[old_sa.length] = 1;
            }
            c++;
            t++;
            return true;
        }

        public boolean unseat(MersenneTwisterFast rng){
            double totalWeight = c;
            double randomNumber = rng.nextDouble();
            double cuSum = 0d;

            for (int i = 0; i < sa.length; i++){
                cuSum += ((double) sa[i]) / totalWeight;
                if (cuSum > randomNumber) {
                    sa[i]--;
                    assert sa[i] >= 0;
                    if (sa[i] == 0) {
                        c--;
                        t--;
                        return true;
                    } else {
                        c--;
                        return false;
                    }
                }
            }
            throw new RuntimeException("Yikes! Should never get here");
        }

        public int[] getSeatingArrangement(){
            if (t < sa.length) {
                int[] seatingArrangement = new int[t];
                int index = 0;
                for (int count : sa) {
                    if (count != 0) {
                        seatingArrangement[index++] = count;
                    }
                }
                assert index == t;
                return seatingArrangement;
            } else {
                return sa;
            }
        }
    }

    public static class IntDoublePairComparator implements Comparator<IntDoublePair> {
        public int compare(IntDoublePair a, IntDoublePair b) {
            if (a.d < b.d) {
                return -1 ;
            } else if (a.d > b.d) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    
    public class SortedPartialDistributionIterator implements Iterator<IntDoublePair> {
        
        private HashSet<Integer> keys;
        private Iterator<IntDoublePair> iter;

        public SortedPartialDistributionIterator() {
            TreeSet<IntDoublePair> spd = new TreeSet<IntDoublePair>(new IntDoublePairComparator());
            keys = new HashSet<Integer>();

            for (Integer key : tableWeights.keySet()) {
                spd.add(new IntDoublePair(key,probability(key)));
                keys.add(key);
            }

            Restaurant p = parent;
            while (p.parent != null) {
                for (Integer key : p.tableWeights.keySet()) {
                    if (!keys.contains(key)) {
                        spd.add(new IntDoublePair(key,probability(key)));
                        keys.add(key);
                    }
                }
                p = p.parent;
            }

            iter = spd.descendingIterator();
        }

        public HashSet<Integer> keys() {
            return keys;
        }
        
        public double probBackOffToBase() {
            double prob = probabilityOfBackOff;
            Restaurant r = parent;

            while (r.parent != null) {
                prob *= r.probabilityOfBackOff;
                r = r.parent;
            }
            
            return prob;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public IntDoublePair next() {
            return iter.next();
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
