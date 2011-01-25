/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.HPYP;
import edu.columbia.stat.wood.bnol.hpyp.IntHPYP;
import edu.columbia.stat.wood.bnol.util.BinaryContext;
import edu.columbia.stat.wood.bnol.util.Context;
import edu.columbia.stat.wood.bnol.util.GammaDistribution;
import edu.columbia.stat.wood.bnol.util.IntBinaryExpansionDistribution;
import edu.columbia.stat.wood.bnol.util.IntGeometricDistribution;
import edu.columbia.stat.wood.bnol.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.MutableInt;
import edu.columbia.stat.wood.bnol.util.Pair;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 *
 * @author nicholasbartlett
 */

public class BNOL implements Serializable{

    
    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException{
        BNOL b = new BNOL(null, 50000, .1);
        b.generateFromScratch(10000);
        System.out.println(b.uniqueEmbeddings());

        System.out.println(b.score());
        for(int i = 0; i < 100; i++){
            System.out.print(b.sample(1.0));
            System.out.println(", " + b.uniqueEmbeddings());
        }

        //System.out.println(b.checkWordDistribution());
        //b.printEmbeddings();
    }

    public int[] keys;
    public int[][] embeddings;
    public int[] words;

    private HPYP transitions;
    private HPYP wordDistribution;

    public static MersenneTwisterFast rng = new MersenneTwisterFast(1);

    /***********************constructor methods********************************/

    public BNOL(int[] words, int alphabetSize, double b){
        if(words == null){
            words = new int[0];
        }

        keys = new int[words.length];
        embeddings = new int[words.length][];
        this.words = words;

        MutableDouble[] conc = new MutableDouble[2];
        MutableDouble[] disc = new MutableDouble[2];

        conc[0] = new MutableDouble(20);
        conc[1] = new MutableDouble(5);

        disc[0] = new MutableDouble(0.8);
        disc[1] = new MutableDouble(0.9);

        transitions = new IntHPYP(disc, conc, new IntBinaryExpansionDistribution(b), new GammaDistribution(1d,100d));
        
        conc = new MutableDouble[11];
        disc = new MutableDouble[11];

        conc[0] = new MutableDouble(10);
        conc[1] = new MutableDouble(2);
        conc[2] = new MutableDouble(1);
        conc[3] = new MutableDouble(1);
        conc[4] = new MutableDouble(1);
        conc[5] = new MutableDouble(1);
        conc[6] = new MutableDouble(1);
        conc[7] = new MutableDouble(1);
        conc[8] = new MutableDouble(1);
        conc[9] = new MutableDouble(1);
        conc[10] = new MutableDouble(1);

        disc[0] = new MutableDouble(.1);
        disc[1] = new MutableDouble(.1);
        disc[2] = new MutableDouble(.2);
        disc[3] = new MutableDouble(.3);
        disc[4] = new MutableDouble(.4);
        disc[5] = new MutableDouble(.5);
        disc[6] = new MutableDouble(.6);
        disc[7] = new MutableDouble(.7);
        disc[8] = new MutableDouble(.8);
        disc[9] = new MutableDouble(.9);
        disc[10] = new MutableDouble(.95);

        wordDistribution = new IntHPYP(disc, conc, new IntUniformDiscreteDistribution(alphabetSize), new GammaDistribution(1d,100d));
    }

    /***********************public methods*************************************/

    public int uniqueEmbeddings(){
        HashSet<Integer> countSet = new HashSet();
        for (int key : keys){
            countSet.add(key);
        }
        
        return countSet.size();
    }

    public HashMap<BinaryContext, MutableInt> getUniqueEmbedding(){
        HashMap<BinaryContext, MutableInt> ue = new HashMap();

        MutableInt value;
        for(int[] e : embeddings){
            value = ue.get(new BinaryContext(e));
            if(value == null){
                value = new MutableInt(0);
                ue.put(new BinaryContext(e), value);
            }
            value.increment();
        }

        return ue;
    }

    public void printEmbeddings(){
        HashMap<BinaryContext, MutableInt> ue = getUniqueEmbedding();
        for (Entry<BinaryContext, MutableInt> e : ue.entrySet()){
            System.out.print(Arrays.toString(e.getKey().getValue()));
            System.out.println(" --- > " + e.getValue().value());
        }
    }

    public double score(){
        double score = transitions.score(true);
        score += wordDistribution.score(true);

        return score;
    }

    public double sample(double temp){
        transitions.sample(temp);
        sampleKeys(temp);
        wordDistribution.sample(temp);

        return score();
    }

    public void sampleKeys(double temp){
        for(int i = 0; i < words.length; i++){
            sampleKey(i, temp);
        }
    }

    public void generateFromScratch(int length){
        keys = new int[length];
        embeddings = new int[length][];
        words = new int[length];

        for (int i = 0; i < length; i++){
            if(i == 0){
                keys[0] = transitions.draw(null);
            } else {
                keys[i] = transitions.draw(keys[i - 1]);
            }

            embeddings[i] = BinaryContext.toExpansion(keys[i]);
            words[i] = wordDistribution.draw(embeddings[i]);
        }
    }

    public void initialize(){
        for(int i = 0; i < words.length; i++){
            if(i == 0){
                keys[0] = transitions.draw(null);
            } else {
                keys[i] = transitions.draw(keys[i - 1]);
            }

            embeddings[i] = BinaryContext.toExpansion(keys[i]);
            assert embeddings[i] != null;
            wordDistribution.seat(embeddings[i], words[i]);
        }
    }

    private void sampleKey(int index, double temp){
        if(index == 0){
            sampleFirstKey(temp);
        } else if (index == (keys.length - 1)){
            sampleLastKey(temp);
        } else {
            int previousKey = keys[index - 1];
            int currentKey = keys[index];
            int nextKey = keys[index + 1];

            int proposedKey = transitions.generate(previousKey);

            double r = Math.log(transitions.prob(proposedKey, nextKey));
            r += Math.log(wordDistribution.prob(BinaryContext.toExpansion(proposedKey), words[index]));
            r -= Math.log(transitions.prob(currentKey, nextKey));
            r -= Math.log(wordDistribution.prob(embeddings[index], words[index]));

            r = Math.exp(r);
            r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

            if(rng.nextBoolean(r)){
                keys[index] = proposedKey;

                transitions.unseat(previousKey, currentKey);
                transitions.unseat(currentKey, nextKey);

                transitions.seat(previousKey, proposedKey);
                transitions.seat(proposedKey, nextKey);

                wordDistribution.unseat(embeddings[index], words[index]);
                embeddings[index] = BinaryContext.toExpansion(proposedKey);
                wordDistribution.seat(embeddings[index], words[index]);
            }
        }
    }

    private void sampleFirstKey(double temp) {
        int currentKey = keys[0], nextKey = keys[1];
        int proposedKey = transitions.generate(null);

        double r = Math.log(transitions.prob(proposedKey, nextKey));
        r += Math.log(wordDistribution.prob(BinaryContext.toExpansion(proposedKey), words[0]));
        r -= Math.log(transitions.prob(currentKey, nextKey));
        r -= Math.log(wordDistribution.prob(embeddings[0], words[0]));

        r = Math.exp(r);
        r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

        if (rng.nextBoolean(r)) {
            keys[0] = proposedKey;

            transitions.unseat(null, currentKey);
            transitions.unseat(currentKey, nextKey);

            transitions.seat(null, proposedKey);
            transitions.seat(proposedKey, nextKey);

            wordDistribution.unseat(embeddings[0], words[0]);
            embeddings[0] = BinaryContext.toExpansion(proposedKey);
            wordDistribution.seat(embeddings[0], words[0]);
        }
    }

    private void sampleLastKey(double temp){
        int index = words.length - 1;
        int previousKey = keys[index - 1];
        int currentKey = keys[index];

        int proposedKey = transitions.generate(previousKey);

        double r = Math.log(wordDistribution.prob(BinaryContext.toExpansion(proposedKey), words[index]));
        r -= Math.log(wordDistribution.prob(embeddings[index], words[index]));

        r = Math.exp(r);
        r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

        if (rng.nextBoolean(r)) {
            keys[index] = proposedKey;

            transitions.unseat(previousKey, currentKey);
            transitions.seat(previousKey, proposedKey);

            wordDistribution.unseat(embeddings[index], words[index]);
            embeddings[index] = BinaryContext.toExpansion(proposedKey);
            wordDistribution.seat(embeddings[index], words[index]);
        }
    }

    private boolean checkWordDistribution(){
        for(int i = 0; i < words.length; i++){
            wordDistribution.unseat(embeddings[i], words[i]);
        }

        if (wordDistribution.isEmpty()){
            for(int i = 0; i < words.length; i++){
                wordDistribution.seat(embeddings[i], words[i]);
            }
            return true;
        } else {
            return false;
        }
    }
   
}
