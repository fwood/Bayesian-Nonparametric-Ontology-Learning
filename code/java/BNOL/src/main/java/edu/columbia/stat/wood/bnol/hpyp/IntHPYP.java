/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.bnol.hpyp;

import edu.columbia.stat.wood.bnol.util.GammaDistribution;
import edu.columbia.stat.wood.bnol.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.math.MathException;

/**
 * Int based implementation of the fully instantiated HPYP.  Both contexts and
 * types are assumed to be int typed.
 * @author nicholasbartlett
 */
public class IntHPYP extends HPYP {

    private MersenneTwisterFast rng;
    private Restaurant ecr, root;
    private MutableDouble[] discounts;
    private MutableDouble[] concentrations;
    private GammaDistribution concentrationPrior;

    /***********************constructor methods********************************/

    /**
     * Constructor method.
     * @param depth depth of tree
     * @param discounts array of depth specific discounts
     * @param concentrations array of depth specific concentrations
     * @param baseDistribution base distribution
     * @param concentrationPrior prior on concentrations
     */
    public IntHPYP(MutableDouble[] discounts, MutableDouble[] concentrations, IntDiscreteDistribution baseDistribution, GammaDistribution concentrationPrior) {
        if (baseDistribution == null) {
            throw new IllegalArgumentException("base distribution must be specified");
        }

        if(concentrationPrior == null){
            throw new IllegalArgumentException("a prior for the concentration parameters must be specified");
        }

        if(discounts != null && discounts.length == 0){
            this.discounts = discounts;
        } else {
            this.discounts = new MutableDouble[]{new MutableDouble(0.5)};
        }

        if(concentrations != null && concentrations.length == 0){
            this.concentrations = concentrations;
        } else {
            this.concentrations = new MutableDouble[]{new MutableDouble(8)};
        }

        if(this.concentrations.length != this.discounts.length){
            throw new IllegalArgumentException("concentrations and discounts must have same length");
        }

        this.concentrationPrior = concentrationPrior;
        root = new RootRestaurant(baseDistribution);
        ecr = new Restaurant(root, this.concentrations[0], this.discounts[0]);
        rng = new MersenneTwisterFast(3);
    }

    /***********************public methods*************************************/

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty(){
        return ecr.isEmptyRestaurant();
    }

    /**
     * {@inheritDoc}
     */
    public double prob(int[] context, int type){
        return get(context).probability(type);
    }

    /**
     * {@inheritDoc}
     */
    public void seat(int[] context, int type){
        get(context).seat(type,rng);
    }

    /**
     * {@inheritDoc}
     */
    public void unseat(int[] context, int type){
        get(context).unseat(type,rng);
    }

    /**
     * {@inheritDoc}
     */
    public int generate(int[] context, double low, double high, int[] keyOrder) {
        return get(context).generate(low, high, keyOrder, rng);
    }

    /**
     * {@inheritDoc}
     */
    public int draw(int[] context, double low, double high, int[] keyOrder) {
        int draw = get(context).draw(low, high, keyOrder, rng);
        //drawHistory.add(new Pair(context, draw));
        return draw;
    }

    /**
     * {@inheritDoc}
     */
    public void sampleSeatingArrangements(int sweeps){
        if(sweeps <= 0){
            throw new IllegalArgumentException("Sweeps must be positive");
        }

        for(int i = 0; i < sweeps; i++){
            sampleSeatingArrangements(ecr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double sampleHyperParameters(int sweeps){
        if(sweeps <= 0){
            throw new IllegalArgumentException("Sweeps must be positive");
        }

        for(int i = 1; i < sweeps; i++){
            sampleHyperParams();
        }
        return sampleHyperParams();
    }

    /**
     * {@inheritDoc}
     */
    public double sample(int sweeps){
        if(sweeps <= 0){
            throw new IllegalArgumentException("Sweeps must be positive");
        }
        
        for(int i = 1; i < sweeps; i++){
            sampleSeatingArrangements(1);
            sampleHyperParams();
        }
        
        sampleSeatingArrangements(1);
        return sampleHyperParams();
    }

    /**
     * {@inheritDoc}
     */
    public double score(boolean withHyperParameters) {
        double score = score(ecr) + root.score();
        if(withHyperParameters){
            score += scoreHyperParameters();
        }
        return score;
    }

    /**
     * {@inheritDoc}
     */
    public double[] scoreByDepth(boolean withHyperParameters) {
        double[] score = new double[discounts.length];
        
        if (withHyperParameters) {
            for (int i = 0; i < (score.length); i++) {
                score[i] = concentrationPrior.logProportionalToDensity(concentrations[i].value());
            }
        }

        scoreByDepth(ecr, 0, score);

        return score;
    }

    /**
     * {@inheritDoc}
     */
    public void removeEmptyNodes() {
        removeEmptyNodes(ecr);
    }

    /**
     * {@inheritDoc}
     */
    public TObjectIntHashMap<int[]> getImpliedData(){
        TObjectIntHashMap<int[]> impliedData = new TObjectIntHashMap();
        getImpliedData(ecr, impliedData, new int[0]);
        return impliedData;
    }

    /**
     * Prints discount values, vector formatted.
     */
    public void printDiscounts() {
        System.out.format("Discounts = [%.2f",discounts[0].value());
        for (int i = 1; i < discounts.length; i++) {
            System.out.format(", %.2f",discounts[i].value());
        }
        System.out.println("]");
    }

    /**
     * Prints concentration values, vector formatted.
     */
    public void printConcentrations() {
        System.out.format("Concentrations = [%.2f", concentrations[0].value());
        for (int i = 1; i < concentrations.length; i++) {
            System.out.format(", %.2f", concentrations[i].value());
        }
        System.out.println("]");
    }

    /***********************private methods************************************/

    /**
     * Retrieves the restaurant indexed by a given context.
     * @param context context indexing restaurant
     * @return restaurant indexed by context
     */
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

    /**
     * Gets the discount object at the given depth.
     * @param depth depth of discount desired
     * @return discount at depth
     */
    private MutableDouble getDiscount(int depth){
        if (depth >= discounts.length) {
            return discounts[discounts.length -1];
        } else {
            return discounts[depth];
        }
    }

    /**
     * Gets the concentration object at the given depth.
     * @param depth depth of concentration desired
     * @return concentration at depth
     * @throws MathException
     */
    private MutableDouble getConcentration(int depth){
        if (depth >= concentrations.length){
            return concentrations[concentrations.length - 1];
        } else {
            return concentrations[depth];
        }
    }

    /**
     * Recursive function which adds the log likelihood contribution of a
     * restaurant to the appropriate element of the score by depth vector.
     * @param currentRestaurant current restaurant
     * @param currentDepth current depth
     * @param score score vector for the different depths
     */
    private void scoreByDepth(Restaurant currentRestaurant, int currentDepth, double[] score) {
        TIntObjectIterator<Restaurant> iterator = currentRestaurant.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            scoreByDepth(iterator.value(), currentDepth + 1, score);
        }

        if(currentDepth >= score.length){
            score[score.length - 1] += currentRestaurant.score();
        } else {
            score[currentDepth] += currentRestaurant.score();
        }
    }

    /**
     * Recursive function to calculate the joint log likelihood contribution
     * from all of the seating arrangements.
     * @param currentRestaurant
     * @return joint log likelihood contribution from seating arrangements
     */
    private double score(Restaurant currentRestaurant) {
        double score = 0.0;

        TIntObjectIterator<Restaurant> iterator = currentRestaurant.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            score += score(iterator.value());
        }

        return score + currentRestaurant.score();
    }

    /**
     * Gets the score contribution from the concentration and discount parameters
     * @return score contribution from the concentration and discount parameters
     */
    private double scoreHyperParameters(){
        double score = 0.0;
        for(MutableDouble c : concentrations){
            score += concentrationPrior.logProportionalToDensity(c.value());
        }

        return score;
    }

    /**
     * Recursive function to sample the seating arrangements of all the
     * nodes on the tree.
     * @param currentRestaurant current restaurant in recursion
     */
    private void sampleSeatingArrangements(Restaurant currentRestaurant) {
        TIntObjectIterator<Restaurant> iterator = currentRestaurant.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            sampleSeatingArrangements(iterator.value());
        }

        currentRestaurant.sampleSeatingArrangements(rng);
    }

    /**
     * Recursive function which executes the removal of nodes with no customers.
     * @param currentRestaurant
     */
    private void removeEmptyNodes(Restaurant currentRestaurant){
        TIntObjectIterator<Restaurant> iterator = currentRestaurant.iterator();
        while(iterator.hasNext()){
            iterator.advance();

            if(iterator.value().isEmptyRestaurant()){
                iterator.remove();
            } else {
                removeEmptyNodes(iterator.value());
            }
        }
    }

    /**
     * Metropolis step for sampling concentration and discount parameters.  A
     * flat prior is assumed for the discount parameters.
     * @return joint log likelihood of model
     */
    private double sampleHyperParams(){
        double stdDiscounts = .01,stdConcentrations = .2;
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

            if (rng.nextDouble() < r) {
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

            if (rng.nextDouble() < r) {
                score += afterScore[i];
            } else {
                score += currentScore[i];
                concentrations[i].set(currentConcentrations[i]);
            }
        }

        return score + root.score();
    }

    /**
     * Recursive function to get the imlied data in this HPYP object.
     * @param currentRestaurant current restaurant of the recursion
     * @param impliedData implied data
     * @param thisContext context at current restaurant
     */
    private void getImpliedData(Restaurant currentRestaurant, TObjectIntHashMap<int[]> impliedData, int[] thisContext){
        
        TIntObjectIterator<Restaurant> iterator = currentRestaurant.iterator();
        while(iterator.hasNext()){
            iterator.advance();

            int[] childContext = new int[thisContext.length + 1];
            System.arraycopy(thisContext, 0, childContext, 0, thisContext.length);
            childContext[thisContext.length] = iterator.key();

            getImpliedData(iterator.value(), impliedData, childContext);
        }
        
        impliedData.put(thisContext, currentRestaurant.impliedData());
    }
    
    
    public static void main(String[] args) throws IOException{
        File f = new File("/Users/nicholasbartlett/Documents/np_bayes/data/pride_and_prejudice/pride_and_prejudice.txt");

        int depth = 5;
        int[] context = new int[depth];

        BufferedInputStream bis = null;
        IntHPYP hpyp = new IntHPYP(null, null, new IntUniformDiscreteDistribution(256), new GammaDistribution(1,100));

        try{
            bis = new BufferedInputStream(new FileInputStream(f));

            for(int i = 0; i < depth; i++){
                context[depth - i -1] = bis.read();
            }

            int next;
            while((next = bis.read()) > -1){
                hpyp.seat(context, next);

                for(int i = depth - 1; i > 0; i--){
                    context[i] = context[i - 1];
                }
                context[0] = next;
            }
            //System.out.println(hpyp.root);
            //System.out.println(hpyp.ecr);
            //System.out.println(hpyp.ecr.size());
            System.out.println(hpyp.score(true));
            for(int i = 0; i < 25; i++){
                System.out.println(hpyp.sample());
                hpyp.printConcentrations();
                hpyp.printDiscounts();
                System.out.println("i = " + i + "\n");
            }

            int length = 1000;
            int[] sample = new int[length];
            for(int i = 0; i < length; i++){
                context = new int[i];
                for(int j = 0; j < i; j++){
                    context[j] = sample[i - 1 - j];
                }

                sample[i] = hpyp.generate(context);
            }

            for(int i = 0; i < length; i++){
                System.out.print((char) sample[i]);
            }

            hpyp.removeEmptyNodes();
            for(int i = 0; i < 2; i++){
                System.out.println(hpyp.sample());
                hpyp.printConcentrations();
                hpyp.printDiscounts();
                System.out.println("i = " + i + "\n");
            }

            System.out.println();
            TObjectIntHashMap<int[]> impliedData = hpyp.getImpliedData();
            System.out.println(impliedData.size());
            TObjectIntIterator<int[]> iterator = impliedData.iterator();
            for(int i = 0; i < 100; i++){
                iterator.advance();
                System.out.print(Arrays.toString(iterator.key()) + ", ");
                System.out.println(iterator.value());
            }

        } finally {
            bis.close();
        }
    }
    
}
