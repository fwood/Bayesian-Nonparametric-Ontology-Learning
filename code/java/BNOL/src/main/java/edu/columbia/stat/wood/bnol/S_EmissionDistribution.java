/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.IntHPYP;
import edu.columbia.stat.wood.bnol.util.GammaDistribution;
import edu.columbia.stat.wood.bnol.util.IntTreeDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.MutableInt;
import edu.columbia.stat.wood.bnol.util.Pair;
import gnu.trove.list.array.TIntArrayList;

/**
 * Emission distribution for the latent binary state variables used in BNOL.
 * @author nicholasbartlett
 */

public class S_EmissionDistribution {

    private Node baseNode;
    private IntTreeDiscreteDistribution baseDist;
    private MutableDouble[] discounts, concentrations;
    private GammaDistribution concentrationPrior;
    private MersenneTwisterFast rng = new MersenneTwisterFast(5);
    
    /***********************constructor methods********************************/

    /**
     * Basic constructor method.  In the method are hard coded priors which could
     * be changed if these are not working well.
     * @param b initial b used as the base distribution on the emissions at each node
     */
    public S_EmissionDistribution(MutableDouble b){
        if(b.value() > 1.0 || b.value() < 0.0){
            throw new IllegalArgumentException("b must be in 0 - 1");
        }

        discounts = new MutableDouble[]{new MutableDouble(0.8), new MutableDouble(0.9)};
        concentrations = new MutableDouble[]{new MutableDouble(8.0), new MutableDouble(1.0)};

        concentrationPrior = new GammaDistribution(1.0,100.0);
        baseDist = new IntTreeDiscreteDistribution(b);
        baseNode = new Node();
    }

    public S_EmissionDistribution(MutableDouble b, double d0, double d1, double c0, double c1){
        if(b.value() > 1.0 || b.value() < 0.0){
            throw new IllegalArgumentException("b must be in 0 - 1");
        }

        discounts = new MutableDouble[]{new MutableDouble(d0), new MutableDouble(d1)};
        concentrations = new MutableDouble[]{new MutableDouble(c0), new MutableDouble(c1)};
        concentrationPrior = new GammaDistribution(1.0,100.0);
        baseDist = new IntTreeDiscreteDistribution(b);
        baseNode = new Node();
    }

    /***********************public methods*************************************/

    /**
     * Gets the log probability of a given emission s from a given machine state.
     * @param machineState given machine state
     * @param s emitted s value, int vector of 0,1
     * @return log probability of s in context of machineState
     */
    public double logProbability(int machineState, int[] s){
        int[] context = new int[]{machineState};
        Node currentNode = baseNode;

        double logProbability = 0d;
        for(int i = 0; i < s.length; i++){
            logProbability += Math.log(currentNode.prob(context, s[i]));
            currentNode = currentNode.get(s[i]);
        }

        return logProbability + Math.log(currentNode.prob(context, -1));
    }

    /**
     * Generate a binary state vector from the appropriate emission distribution
     * given the machine state and using the slice provided.
     * @param machineState machine state
     * @param low low edge of slice
     * @param high high edge of slice
     * @return sampled vector
     */
    public Pair<int[], Double> generate(int machineState, double low, double high){
        TIntArrayList out = new TIntArrayList();

        int[] context = new int[]{machineState};

        Node currentNode = baseNode;
        
        int emission;
        double cuSum, cuSumLast;
        double randomNumber = rng.nextDouble() * (high - low) + low;
        double startRandomNumber = randomNumber;
        while(true){
            emission = -2;
            cuSum = 0.0;
            cuSumLast = cuSum;
            for(int i = 0; i < 3; i++){
                cuSum += currentNode.prob(context, ++emission);
                if(cuSum > randomNumber){
                    break;
                }
                cuSumLast = cuSum;
            }

            assert cuSum > randomNumber;

            if (emission == -1) {
                break;
            } else {
                out.add(emission);
                currentNode = currentNode.get(emission);

                // shift numbers based on expansion of section chosen
                double expandFactor = 1.0 / (cuSum - cuSumLast);
                high = high >= cuSum ? 1.0 : ((high - cuSumLast) * expandFactor);
                low = low <= cuSumLast ? 0.0 : ((low - cuSumLast) * expandFactor);
                randomNumber = (randomNumber - cuSumLast) * expandFactor;

                assert high <= 1.0;
                assert low >= 0.0;
                assert randomNumber >= low && randomNumber <= high : randomNumber + ", " + low + ", " + high;
            }
        }

        assert emission == -1;
        
        return new Pair(out.toArray(), startRandomNumber);
    }

    /**
     * Seats all the counts in the emission vector s with the given machine
     * state.
     * @param machineState machine state
     * @param s emission
     */
    public void seat(int machineState, int[] s){
        int[] context = new int[]{machineState};
        Node currentNode = baseNode;

        for (int i = 0; i < s.length; i++){
            currentNode.seat(context, s[i]);
            currentNode = currentNode.get(s[i]);
        }

        currentNode.seat(context, -1);
    }

    /**
     * Un-seats the counts in the emission vector s with the given machine
     * state.
     * @param machineState machine state
     * @param s emission
     */
    public void unseat(int machineState, int[] s){
        int[] context = new int[]{machineState};
        Node currentNode = baseNode;

        for (int i = 0; i < s.length; i++){
            currentNode.unseat(context, s[i]);
            currentNode = currentNode.get(s[i]);
        }

        currentNode.unseat(context, -1);
    }

    /**
     * Gets the joint log likelihood of the data and model.
     * @return joint log likelihood
     */
    public double score(){
        double[] score = scoreByDepth();
        return score[0] + score[1];
    }

    /**
     * Samples all the HPYP objects in the tree a given number times and returns
     * the joint log likelihood of the data and model.
     * @param sweeps number of Gibb's sweeps
     * @param temp temperature of sampling steps
     * @return joint log likelihood
     */
    public double sample(int sweeps, double temp){
        for(int i = 0; i < (sweeps - 1); i++){
            sampleSeatingArrangements(baseNode);
            sampleHyperParameters(temp);
        }
        sampleSeatingArrangements(baseNode);
        //System.out.print(discounts[0].value() + ", " + discounts[1].value() +
          //      ", " + concentrations[0].value() + ", " + concentrations[1].value() + ", ");
        return sampleHyperParameters(temp);
    }

    /**
     * Removes nodes from the tree which are empty.  Also, calls for the removal
     * of empty HPYP nodes at each non-empty node on the tree.
     */
    public void removeEmptyNodes(){
        removeEmptyNodes(baseNode);
    }

    /**
     * Indicator that there are no counts in the distribution tree
     * @return true if the distribution tree is empty, else false
     */
    public boolean isEmpty(){
        return baseNode.isEmpty();
    }

    /**
     * Gets the number of nodes in the tree.
     * @return node count
     */
    public int nodeCount(){
        MutableInt nodeCount = new MutableInt(0);
        nodeCount(baseNode, nodeCount);
        return nodeCount.value();
    }

    /***********************private methods************************************/

    /**
     * Recursive function to count the number of nodes in this tree.
     * @param currentNode current node of recursion
     * @param count running count of nodes
     */
    private void nodeCount(Node currentNode, MutableInt count){
        if(currentNode.left != null){
            nodeCount(currentNode.left, count);
        }

        if(currentNode.right != null){
            nodeCount(currentNode.right, count);
        }

        count.increment();
    }

    /**
     * Recursive function to remove empty nodes.
     * @param currentNode current node in recursion
     */
    private void removeEmptyNodes(Node currentNode){
        if(currentNode.left != null){
            if(currentNode.left.isEmpty()){
                currentNode.left = null;
            } else {
                currentNode.left.removeEmptyNodes();
                removeEmptyNodes(currentNode.left);
            }
        }

        if(currentNode.right != null){
            if(currentNode.right.isEmpty()){
                currentNode.right = null;
            } else {
                currentNode.right.removeEmptyNodes();
                removeEmptyNodes(currentNode.right);
            }
        }
    }

    /**
     * Recursive method to sample seating arrangements of each node.  Also, if
     * there are empty nodes they are pruned in this method call.
     * @param currentNode current node in recursion
     */
    private void sampleSeatingArrangements(Node currentNode){
        if(currentNode.left != null){
            if(currentNode.left.isEmpty()){
                currentNode.left = null;
            } else {
                sampleSeatingArrangements(currentNode.left);
            }
        }

        if(currentNode.right != null){
            if(currentNode.right.isEmpty()){
                currentNode.right = null;
            } else {
                sampleSeatingArrangements(currentNode.right);
            }
        }

        currentNode.sampleSeatingArrangements();
    }

    /**
     * Samples the hyper parameters.
     * @param temp temperature of sampling
     * @return joint score of the whole tree
     */
    private double sampleHyperParameters(double temp){
        double[] c = new double[]{concentrations[0].value(), concentrations[1].value()};
        double[] d = new double[]{discounts[0].value(), discounts[1].value()};
        double stdDiscounts = .05, stdConcentrations = 3, r;

        double[] score = scoreByDepth();
        
        // propose for discounts
        for (int i = 0; i < 2; i++) {
            discounts[i].plusEquals(stdDiscounts * rng.nextGaussian());
            if (discounts[i].value() <= 0.0 || discounts[i].value() >= 1.0) {
                discounts[i].set(d[i]);
            }
        }

        // score proposals
        double[] proposedScore = scoreByDepth();

        // accept for discounts
        for(int i = 0; i < 2; i++){
            r = Math.exp(proposedScore[i] - score[i]);
            r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);
            if(rng.nextDouble() < r){
                score[i] = proposedScore[i];
            } else {
                discounts[i].set(d[i]);
            }
        }
        
        // propose for concentrations
        for (int i = 0; i < 2; i++){
            concentrations[i].plusEquals(stdConcentrations * rng.nextGaussian());
            if(concentrations[i].value() <= 0.0){
                concentrations[i].set(c[i]);
            }
        }

        // score proposals
        proposedScore = scoreByDepth();

        // accept for concentrations
        for(int i = 0; i < 2; i++){
            r = Math.exp(proposedScore[i] - score[i]);
            r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);
            if(rng.nextDouble() < r){
                score[i] = proposedScore[i];
            } else {
                concentrations[i].set(c[i]);
            }
        }

        // sum the final score
        double scalarScore = 0.0;
        for(double s : score){
            scalarScore += s;
        }

        return scalarScore;
    }

    /**
     * Scores the tree by depth.  Here, depth refers to the depths of each HPYP
     * object on the tree.
     * @return vector of scores for depths 0 and 1
     */
    private double[] scoreByDepth(){
        double[] score= new double[2];

        scoreByDepth(baseNode, score);
        score[0] += concentrationPrior.logProportionalToDensity(concentrations[0].value());
        score[1] += concentrationPrior.logProportionalToDensity(concentrations[1].value());
        
        return score;
    }

    /**
     * Recursive method to score the tree by depth, adding together all the by
     * depth scores for each HPYP node.
     * @param currentNode current node of recursion
     * @param score container vector for score
     */
    private void scoreByDepth(Node currentNode, double[] score){
        if(currentNode.left != null){
            if(currentNode.left.isEmpty()){
                currentNode.left = null;
            } else {
                scoreByDepth(currentNode.left, score);
            }
        }

        if(currentNode.right != null){
            if(currentNode.right.isEmpty()){
                currentNode.right = null;
            } else {
                scoreByDepth(currentNode.right, score);
            }
        }

        double[] hpypScore = currentNode.scoreByDepth(false);
        score[0] += hpypScore[0];
        score[1] += hpypScore[1];
    }
    
    /***********************private classes************************************/

    private class Node extends IntHPYP {

        private Node left, right;

        /***********************constructor methods****************************/
        public Node(){
            super(discounts, concentrations, baseDist, concentrationPrior);
        }

        /***********************public methods*********************************/

        /**
         * Gets the child node associated with the given key.  0 refers to the
         * left child, 1 refers to the right child. If there is no child then
         * one is created.
         * @param key 0 or 1 key corresponding to left and right children
         * @return child
         */
        public Node get(int key){
            if(key == 0){
                if(left == null){
                    left = new Node();
                }
                return left;
            } else if (key == 1){
                if(right == null){
                    right = new Node();
                }
                return right;
            } else {
                throw new IllegalArgumentException("key must be 0 or 1, not " + key);
            }
        }
    }


    /*
    public static void main(String[] args) {
        S_EmissionDistribution ed = new S_EmissionDistribution(new MutableDouble(0.1));

        double low = 0.7, high = 0.8;
        Pair<int[], Double> emission = ed.generate(0, low, high);
        System.out.println(Arrays.toString(emission.first()));

        emission = ed.generate(0, low, high);
        System.out.println(Arrays.toString(emission.first()));

        emission = ed.generate(0, low, high);
        System.out.println(Arrays.toString(emission.first()));

        emission = ed.generate(0, low, high);
        System.out.println(Arrays.toString(emission.first()));

        emission = ed.generate(0, low, high);
        System.out.println(Arrays.toString(emission.first()));
    }    */
}

