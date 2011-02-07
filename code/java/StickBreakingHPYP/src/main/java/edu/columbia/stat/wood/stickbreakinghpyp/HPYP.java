/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp;

import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MersenneTwisterFast;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MutableDouble;
import edu.columbia.stat.wood.stickbreakinghpyp.util.RND;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author nicholasbartlett
 */
public class HPYP {

    public static void main(String[] args) throws IOException, ClassNotFoundException{
        //File f = new File("/Users/nicholasbartlett/Documents/np_bayes/data/alice_in_wonderland/alice_in_wonderland.txt");
        File f = new File("/home/bartlett/HPYP/alice_in_wonderland.txt");

        int depth = 5;
        int[] context = new int[depth];

        BufferedInputStream bis = null;

        MutableDouble[] conc = new MutableDouble[depth + 1];
        MutableDouble[] disc = new MutableDouble[depth + 1];

        for(int i = 0; i < disc.length; i++){
            disc[disc.length - 1 - i] = new MutableDouble(Math.pow(0.9, i + 1));
            conc[i] = new MutableDouble(1.0);
        }

        HPYP hpyp = new HPYP(disc, conc, new IntUniformDiscreteDistribution(256), 1,100);

        try{
            bis = new BufferedInputStream(new FileInputStream(f));

            for(int i = 0; i < depth; i++){
                context[depth - i - 1] = bis.read();
            }

            int next;
            while((next = bis.read()) > -1){
                hpyp.adjustCount(context, next,1);

                for(int i = depth - 1; i > 0; i--){
                    context[i] = context[i - 1];
                }
                context[0] = next;
            }

            for (int i = 0; i < 1000; i++) {
                hpyp.sampleWeights();
                System.out.println(hpyp.score());
            }
            

        } finally {
            if(bis != null){
                bis.close();
            }       
        }
    }


    private MersenneTwisterFast rng;
    private Restaurant ecr;
    private BaseRestaurant root;
    private MutableDouble[] discounts;
    private MutableDouble[] concentrations;
    private double alpha, beta;

    public HPYP(MutableDouble[] discounts, MutableDouble[] concentrations, IntDiscreteDistribution baseDistribution, double alpha, double beta) {
        if (baseDistribution == null) {
            throw new IllegalArgumentException("base distribution must be specified");
        }

        if(discounts != null && discounts.length != 0){
            this.discounts = discounts;
        } else {
            this.discounts = new MutableDouble[]{new MutableDouble(0.5)};
        }

        if(concentrations != null && concentrations.length != 0){
            this.concentrations = concentrations;
        } else {
            this.concentrations = new MutableDouble[]{new MutableDouble(8)};
        }

        if(this.concentrations.length != this.discounts.length){
            throw new IllegalArgumentException("concentrations and discounts must have same length");
        }

        this.alpha = alpha;
        this.beta = beta;
        root = new BaseRestaurant(baseDistribution);
        ecr = new Restaurant(root, this.concentrations[0], this.discounts[0]);
        rng = new MersenneTwisterFast(3);
        RND.setRNG(rng);
    }

    public void adjustCount(int[] context, int type, int multiplicity) {
        get(context).adjustCount(type, multiplicity);
    }

    public void adjustCount(int context, int type, int multiplicity) {
        get(context).adjustCount(type, multiplicity);
    }

    public double score(){
        return root.score() + ecr.scoreSubtree();
    }

    public void sampleWeights() {
        ecr.sampleSubtree(rng);
    }

    public double[] scoreByDepth(){
        double[] score = new double[discounts.length];
        score[0] += root.score();
        ecr.scoreByDepth(score, 0);
        return score;
    }
    
    private Restaurant get(int context) {
        Restaurant child = ecr.get(context);
        if (child == null) {
            child = new Restaurant(ecr, getConcentration(1), getDiscount(1));
            ecr.put(context, child);
        }
        return child;
    }

    private Restaurant get(int[] context) {
        if (context == null || context.length == 0) {
            return ecr;
        } else {
            int restaurantDepth = context.length;
            int currentDepth = 0;
            Restaurant child, current = ecr;

            while (currentDepth < restaurantDepth) {
                child = current.get(context[currentDepth]);

                if (child == null) {
                    break;
                }

                current = child;
                currentDepth++;
            }

            while (currentDepth < restaurantDepth) {
                child = new Restaurant(current, getConcentration(currentDepth + 1), getDiscount(currentDepth + 1));
                current.put(context[currentDepth++], child);

                current = child;
            }

            return current;
        }
    }

    private MutableDouble getDiscount(int depth){
        if (depth >= discounts.length) {
            return discounts[discounts.length -1];
        } else {
            return discounts[depth];
        }
    }

    private MutableDouble getConcentration(int depth){
        if (depth >= concentrations.length){
            return concentrations[concentrations.length - 1];
        } else {
            return concentrations[depth];
        }
    }
}
