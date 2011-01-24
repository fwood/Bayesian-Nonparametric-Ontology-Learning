/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.IntHPYP;
import edu.columbia.stat.wood.bnol.util.GammaDistribution;
import edu.columbia.stat.wood.bnol.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.MutableInt;
import edu.columbia.stat.wood.bnol.util.Pair;
import gnu.trove.TIntArrayList;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 *
 * @author nicholasbartlett
 */
public class S_EmissionDistribution implements Externalizable {

    /*
    public static void main(String[] args) {
        S_EmissionDistribution2 ed = new S_EmissionDistribution2(new MutableDouble(0.1), 15);

        double low = 0.300001, high = .300004;
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
    }*/

    private Node baseNode;
    private IntUniformDiscreteDistribution baseDist;
    public MutableDouble[] discounts, concentrations;
    private GammaDistribution concentrationPrior;
    private MersenneTwisterFast rng = new MersenneTwisterFast(5);
    private int length;

    public S_EmissionDistribution(double b, int length){
        if(b > 1.0 || b < 0.0){
            throw new IllegalArgumentException("b must be in 0 - 1");
        }

        discounts = new MutableDouble[]{new MutableDouble(0.8), new MutableDouble(0.9)};
        concentrations = new MutableDouble[]{new MutableDouble(8.0), new MutableDouble(1.0)};

        concentrationPrior = new GammaDistribution(1.0,100.0);
        baseDist = new IntUniformDiscreteDistribution(2);
        baseNode = new Node();
        this.length = length;
    }

    public S_EmissionDistribution(double b, int length, double d0, double d1, double c0, double c1){
        if(b > 1.0 || b < 0.0){
            throw new IllegalArgumentException("b must be in 0 - 1");
        }

        discounts = new MutableDouble[]{new MutableDouble(d0), new MutableDouble(d1)};
        concentrations = new MutableDouble[]{new MutableDouble(c0), new MutableDouble(c1)};
        concentrationPrior = new GammaDistribution(1.0,100.0);
        baseDist = new IntUniformDiscreteDistribution(2);
        baseNode = new Node();
        this.length = length;
    }

    public S_EmissionDistribution(){};

    public double logProbability(int machineState, int[] s){
        assert s.length == length;
        int[] context = new int[]{machineState};
        Node currentNode = baseNode;

        double logProbability = 0d;
        for(int i = 0; i < s.length; i++){
            logProbability += Math.log(currentNode.prob(context, s[i]));
            if (i < (s.length - 1)) currentNode = currentNode.get(s[i]);
        }

        return logProbability;
    }

    public Pair<int[], Double> generate(int machineState, double low, double high){
        TIntArrayList out = new TIntArrayList();

        int[] context = new int[]{machineState};

        Node currentNode = baseNode;

        int emission;
        double cuSum, cuSumLast;
        double randomNumber = rng.nextDouble() * (high - low) + low;
        double startRandomNumber = randomNumber;
        double expandFactor;
        for(int j = 0; j < length; j++){
            cuSum = currentNode.prob(context, 0);

            if(cuSum > randomNumber){
                emission = 0;
                cuSumLast = 0.0;
            } else {
                emission = 1;
                cuSumLast = cuSum;
                cuSum = 1.0;
            }

            out.add(emission);
            if (j < (length - 1)) currentNode = currentNode.get(emission);

            expandFactor = 1.0 / (cuSum - cuSumLast);

            high = high >= cuSum ? 1.0 : ((high - cuSumLast) * expandFactor);
            low = low <= cuSumLast ? 0.0 : ((low - cuSumLast) * expandFactor);
            randomNumber = (randomNumber - cuSumLast) * expandFactor;

            assert high <= 1.0;
            assert low >= 0.0;
            assert randomNumber >= low && randomNumber <= high : randomNumber + ", " + low + ", " + high;
        }

        return new Pair(out.toNativeArray(), startRandomNumber);
    }

    public void seat(int machineState, int[] s){
        int[] context = new int[]{machineState};
        Node currentNode = baseNode;

        for (int i = 0; i < s.length; i++){
            currentNode.seat(context, s[i]);
            if (i < (s.length - 1)) currentNode = currentNode.get(s[i]);
        }
    }

    public void unseat(int machineState, int[] s){
        int[] context = new int[]{machineState};
        Node currentNode = baseNode;

        for (int i = 0; i < s.length; i++){
            currentNode.unseat(context, s[i]);
            if (i < (s.length - 1)) currentNode = currentNode.get(s[i]);
        }
    }

    public double score(){
        double[] score = scoreByDepth();
        return score[0] + score[1];
    }

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

    public void removeEmptyNodes(){
        removeEmptyNodes(baseNode);
    }

    public boolean isEmpty(){
        return baseNode.isEmpty();
    }

    public int nodeCount(){
        MutableInt nodeCount = new MutableInt(0);
        nodeCount(baseNode, nodeCount);
        return nodeCount.value();
    }

    public double sampleSeatingArrangements(){
        sampleSeatingArrangements(baseNode);
        return score();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(baseDist);
        out.writeInt(length);
        out.writeObject(concentrations);
        out.writeObject(discounts);
        out.writeObject(concentrationPrior);
        out.writeObject(rng);
        baseNode.serializeOut(out);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        baseDist = (IntUniformDiscreteDistribution) in.readObject();
        length = in.readInt();
        concentrations = (MutableDouble[]) in.readObject();
        discounts = (MutableDouble[]) in.readObject();
        concentrationPrior = (GammaDistribution) in.readObject();
        rng = (MersenneTwisterFast) in.readObject();
        baseNode = new Node(0);
        baseNode.serializeIn(in);
    }

    /***********************private methods************************************/
    
    private void nodeCount(Node currentNode, MutableInt count){
        if(currentNode.left != null){
            nodeCount(currentNode.left, count);
        }

        if(currentNode.right != null){
            nodeCount(currentNode.right, count);
        }

        count.increment();
    }

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

    private double[] scoreByDepth(){
        double[] score= new double[2];

        scoreByDepth(baseNode, score);
        score[0] += concentrationPrior.logProportionalToDensity(concentrations[0].value());
        score[1] += concentrationPrior.logProportionalToDensity(concentrations[1].value());

        return score;
    }

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
            super(discounts, concentrations, baseDist, null);
        }

        public Node(int i){
            super(discounts, concentrations, null);
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

        private int[] keys(){
            int[] keys;
            if (left != null && right != null){
                keys = new int[]{0,1};
            } else if (left != null){
                keys = new int[]{0};
            } else if (right != null){
                keys = new int[]{1};
            } else {
                keys = new int[0];
            }

            return keys;
        }

        private Node getSerialize(int key){
            if(key == 0){
                if(left == null){
                    left = new Node(0);
                }
                return left;
            } else if (key == 1){
                if(right == null){
                    right = new Node(0);
                }
                return right;
            } else {
                throw new IllegalArgumentException("key must be 0 or 1, not " + key);
            }
        }

        public void serializeOut(ObjectOutput out) throws IOException{
            writeExternalNoHyperParams(out);
            int[] keys = keys();
            out.writeInt(keys.length);
            for(int key : keys){
                out.writeInt(key);
                get(key).serializeOut(out);
            }
        }

        public void serializeIn(ObjectInput in) throws ClassNotFoundException, IOException{
            readExternalNoHyperParams(in, concentrations, discounts, null);

            int size = in.readInt();
            for(int i = 0; i < size; i++){
                int key = in.readInt();
                getSerialize(key).serializeIn(in);
            }
        }
    }
}
