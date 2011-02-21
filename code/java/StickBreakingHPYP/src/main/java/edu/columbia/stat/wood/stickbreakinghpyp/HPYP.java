/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp;

import edu.columbia.stat.wood.stickbreakinghpyp.Restaurant.SortedPartialDistributionIterator;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDoublePair;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MersenneTwisterFast;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MutableDouble;
import edu.columbia.stat.wood.stickbreakinghpyp.util.RND;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 *
 * @author nicholasbartlett
 */
public class HPYP {

    public static void main(String[] args) throws IOException, ClassNotFoundException{
        File f = new File("/Users/nicholasbartlett/Documents/np_bayes/data/alice_in_wonderland/alice_in_wonderland.txt");
        //File f = new File("/home/bartlett/HPYP/alice_in_wonderland.txt");

        int depth = 5;
        int[] context = new int[depth];

        BufferedInputStream bis = null;

        MutableDouble[] conc = new MutableDouble[depth + 1];
        MutableDouble[] disc = new MutableDouble[depth + 1];

        for(int i = 0; i < disc.length; i++){
            disc[disc.length - 1 - i] = new MutableDouble(Math.pow(0.9, i + 1));
            conc[i] = new MutableDouble(0.01);
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

            //hpyp.printConcentrations();
            //hpyp.printDiscounts();

            for (int i = 0; i < 1000; i++) {
                hpyp.sampleWeights();
                System.out.println(hpyp.score());
                //hpyp.printDiscounts();
            }

            /*
            Iterator<IntDoublePair> iter = hpyp.iterator(new int[]{101, 116, 116, 97, 104});
            double s = 0d;
            while(iter.hasNext()) {
                IntDoublePair pr = iter.next();
                System.out.println(pr.i + ", " + (char) pr.i + ", " + pr.d);
                s += pr.d;
            }
            System.out.println(s);*/
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

    public double probability(int[] context, int type) {
        return get(context).probability(type);
    }

    public double probability(int context, int type) {
        return get(context).probability(type);
    }

    public Iterator<IntDoublePair> iterator(int[] context) {
        return new DistributionIterator((SortedPartialDistributionIterator) get(context).partialSortedDistribution(),root.iterator());
    }

    public Iterator<IntDoublePair> iterator(int context) {
        return new DistributionIterator((SortedPartialDistributionIterator) get(context).partialSortedDistribution(),root.iterator());
    }

    public double score(){
        return root.score() + ecr.scoreSubtree();
    }

    public double sample() {
        ecr.sampleSubtree(rng);
        return sampleHyperParams();
    }

    public void sampleWeights() {
        ecr.sampleSubtree(rng);
    }
    
    public double[] scoreByDepth(boolean withHyperParams){
        double[] score = new double[discounts.length];
        score[0] += root.score();
        ecr.scoreByDepth(score, 0);

        if(withHyperParams) {
            for (int i = 0; i < discounts.length; i++) {
                score[i] += RND.logGammaLikelihood(concentrations[i].value(), alpha, beta);
            }
        }
        
        return score;
    }

    public void printDiscounts() {
        System.out.format("Discounts = [%.2f",discounts[0].value());
        for (int i = 1; i < discounts.length; i++) {
            System.out.format(", %.2f",discounts[i].value());
        }
        System.out.println("]");
    }

    public void printConcentrations() {
        System.out.format("Concentrations = [%.2f", concentrations[0].value());
        for (int i = 1; i < concentrations.length; i++) {
            System.out.format(", %.2f", concentrations[i].value());
        }
        System.out.println("]");
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

    private double sampleHyperParams(){
        double stdDiscounts = .07, stdConcentrations = 0d;
        double[] currentScore = scoreByDepth(true);

        // get the current values
        double[] currentDiscounts = new double[discounts.length];
        double[] currentConcentrations = new double[concentrations.length];
        for (int i = 0; i < discounts.length; i++) {
            currentDiscounts[i] = discounts[i].value();
            currentConcentrations[i] = concentrations[i].value();
        }

        // make proposals for discounts
        for (int i = 0; i < discounts.length; i++) {
            discounts[i].plusEquals(stdDiscounts * rng.nextGaussian());
            if (discounts[i].value() >= 1.0 || discounts[i].value() <= 0.0) {
                discounts[i].set(currentDiscounts[i]);
            }
        }

        // get score given proposals
        double[] afterScore = scoreByDepth(true);

        // choose to accept or reject each proposal
        for (int i = 0; i < discounts.length; i++) {
            double r = Math.exp(afterScore[i] - currentScore[i]);
            r = r < 1.0 ? r : 1.0;

            if (rng.nextBoolean(r)) {
                currentScore[i] = afterScore[i];
            } else {
                discounts[i].set(currentDiscounts[i]);
            }
        }

        //make proposals for concentrations
        for (int i = 0; i < discounts.length; i++) {
            concentrations[i].plusEquals(stdConcentrations * rng.nextGaussian());
            if (concentrations[i].value() <= 0.0) {
                concentrations[i].set(currentConcentrations[i]);
            }
        }

        // get score given proposals
        afterScore = scoreByDepth(true);

        // choose to accept or reject each proposal
        double score = 0.0;
        for (int i = 0; i < discounts.length; i++) {
            double r = Math.exp(afterScore[i] - currentScore[i]);
            r = r < 1.0 ? r : 1.0;

            if (rng.nextBoolean(r)) {
                score += afterScore[i];
            } else {
                score += currentScore[i];
                concentrations[i].set(currentConcentrations[i]);
            }
        }

        return score + root.score();
    }
}
