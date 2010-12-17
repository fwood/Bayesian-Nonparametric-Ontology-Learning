/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.bnol.hpyp;

import edu.columbia.stat.wood.bnol.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;

/**
 *
 * @author nicholasbartlett
 */
public class HPYP<F> {

    private MersenneTwisterFast RNG;
    private Restaurant<F> ecr, root;
    private MutableDouble[] discounts;
    private MutableDouble[] concentrations;

    public HPYP(int depth, MutableDouble[] discounts, MutableDouble[] concentrations, IntDiscreteDistribution baseDist) {
        if (depth < 0) {
            throw new IllegalArgumentException("depth must be >= 0");
        }

        if (discounts != null) {
            this.discounts = discounts;
            this.concentrations = concentrations;
        } else {
            this.discounts = new MutableDouble[depth + 1];
            this.concentrations = new MutableDouble[depth + 1];

            this.discounts[0] = new MutableDouble(0.5);
            this.concentrations[0] = new MutableDouble(0.5);

            for (int i = 1; i <= depth; i++) {
                this.discounts[i] = new MutableDouble(0.8);
                this.concentrations[i] = new MutableDouble(0.5);
            }
        }

        IntDiscreteDistribution baseDistribution;
        if(baseDist!= null){
            baseDistribution = baseDist;
        } else {
            baseDistribution = new IntUniformDiscreteDistribution(256);
        }
        
        root = new RootRestaurant(baseDistribution);
        ecr = new Restaurant<F>(root, this.concentrations[0], this.discounts[0]);
        RNG = new MersenneTwisterFast(3);
    }

    public int draw(F[] context){
        return get(context).draw(RNG);
    }

    public Restaurant<F> get(F[] context) {
        if (context == null || context.length == 0) {
            return ecr;
        } else {
            int index = context.length - 1;
            int depth = discounts.length - 1 < context.length ? discounts.length - 1 : context.length;
            int d = 0;
            Restaurant<F> c, r = ecr;


            while (d < depth) {
                c = r.get(context[index]);

                if (c == null) {
                    break;
                }

                r = c;
                d++;
                index--;
            }

            while (d < depth) {
                d++;

                c = new Restaurant<F>(r, concentrations[d], discounts[d]);
                r.put(context[index], c);

                r = c;
                index--;
            }

            return r;
        }
    }

    public double probability(F[] context, int type){
        return get(context).probability(type);
    }

    public void unseat(F[] context, int type){
        get(context).unseat(type,RNG);
    }

    public void seat(F[] context, int type){
        get(context).seat(type,RNG);
    }

    private boolean checkCounts(Restaurant<F> r) {
        for(Restaurant<F> child : r.values()){
            checkCounts(child);
        }

        r.checkCounts();
        return true;
    }

    public void sampleSeatingArrangments() {
        sampleSeatingArrangments(ecr);
    }

    private void sampleSeatingArrangments(Restaurant<F> r) {
        for(Restaurant<F> child : r.values()){
            sampleSeatingArrangments(child);
        }
         
        r.sampleSeatingArrangements(RNG);
        r.removeZeros();
    }

    public double sampleHyperParams(double stdDiscounts, double stdConcentrations) {
        double[] currentScore = scoreByDepth();

        // get the current values
        double[] currentDiscounts = new double[discounts.length];
        double[] currentConcentrations = new double[concentrations.length];
        for (int i = 0; i < discounts.length; i++) {
            currentDiscounts[i] = discounts[i].value();
            currentConcentrations[i] = concentrations[i].value();
        }

        // make proposals for discounts
        for (int i = 0; i < discounts.length; i++) {
            discounts[i].plusEquals(stdDiscounts * RNG.nextGaussian());
            if (discounts[i].value() >= 1.0 || discounts[i].value() <= 0.0) {
                discounts[i].set(currentDiscounts[i]);
            }
        }

        // get score given proposals
        double[] afterScore = scoreByDepth();

        // choose to accept or reject each proposal
        for (int i = 0; i < discounts.length; i++) {
            double r = Math.exp(afterScore[i] - currentScore[i]);

            if (RNG.nextDouble() < r) {
                currentScore[i] = afterScore[i];
            } else {
                discounts[i].set(currentDiscounts[i]);
            }
        }

        //make proposals for concentrations
        for (int i = 0; i < concentrations.length; i++) {
            concentrations[i].plusEquals(stdConcentrations * RNG.nextGaussian());
            if (concentrations[i].value() <= 0.0) {
                concentrations[i].set(currentConcentrations[i]);
            }
        }

        // get score given proposals
        afterScore = scoreByDepth();

        // choose to accept or reject each proposal
        double score = 0.0;
        for (int i = 0; i < concentrations.length; i++) {
            double r = Math.exp(afterScore[i] - currentScore[i]);

            if (RNG.nextDouble() < r) {
                score += afterScore[i];
            } else {
                score += currentScore[i];
                concentrations[i].set(currentConcentrations[i]);
            }
        }

        return score + root.score();
    }

    private double[] scoreByDepth() {
        double[] score = new double[discounts.length];
        scoreByDepth(ecr, 0, score);
        return score;
    }

    private void scoreByDepth(Restaurant<F> r, int depth, double[] score) {
        for(Restaurant<F> child : r.values()) {
            scoreByDepth(child, depth + 1, score);
        }

        score[depth] += r.score();
    }

    public double score() {
        return score(ecr) + root.score();
    }

    private double score(Restaurant<F> r) {
        double score = 0.0;

        for (Restaurant<F> child : r.values()) {
            score += score(child);
        }

        return score + r.score();
    }

    public void printDiscounts() {
        System.out.print("Discounts = [" + discounts[0].value());
        for (int i = 1; i < discounts.length; i++) {
            System.out.print(", " + discounts[i].value());
        }
        System.out.println("]");
    }

    public void printConcentrations() {
        System.out.print("Concentrations = [" + concentrations[0].value());
        for (int i = 1; i < concentrations.length; i++) {
            System.out.print(", " + concentrations[i].value());
        }
        System.out.println("]");
    }
}
