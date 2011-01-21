/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.bnol.hpyp;

import edu.columbia.stat.wood.bnol.util.Context;
import edu.columbia.stat.wood.bnol.util.GammaDistribution;
import edu.columbia.stat.wood.bnol.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.IntUniformDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.MutableInt;
import gnu.trove.iterator.TIntObjectIterator;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import org.apache.commons.math.MathException;

/**
 * Int based implementation of the fully instantiated HPYP.  Both contexts and
 * types are assumed to be int typed.
 * @author nicholasbartlett
 */
public class IntHPYP extends HPYP {

    static final long serialVersionUID = 1 ;

    private MersenneTwisterFast rng;
    private Restaurant ecr;
    private RootRestaurant root;
    private MutableDouble[] discounts;
    private MutableDouble[] concentrations;
    private GammaDistribution concentrationPrior;

    /***********************constructor methods********************************/

    /**
     * Constructor method.
     * @param discounts array of depth specific discounts
     * @param concentrations array of depth specific concentrations
     * @param baseDistribution base distribution
     * @param concentrationPrior prior on concentrations
     */
    public IntHPYP(MutableDouble[] discounts, MutableDouble[] concentrations, IntDiscreteDistribution baseDistribution, GammaDistribution concentrationPrior) {
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

        this.concentrationPrior = concentrationPrior;
        root = new RootRestaurant(baseDistribution);
        ecr = new Restaurant(root, this.concentrations[0], this.discounts[0]);
        rng = new MersenneTwisterFast(3);
    }

    public IntHPYP(){};

    public IntHPYP(MutableDouble[] discounts, MutableDouble[] concentrations, GammaDistribution concentrationPrior) {
        this.discounts = discounts;
        this.concentrations = concentrations;
        this.concentrationPrior = concentrationPrior;
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
    public double sampleHyperParameters(int sweeps, double temp){
        if(sweeps <= 0){
            throw new IllegalArgumentException("Sweeps must be positive");
        }

        for(int i = 1; i < sweeps; i++){
            sampleHyperParams(temp);
        }
        return sampleHyperParams(temp);
    }

    /**
     * {@inheritDoc}
     */
    public double sample(int sweeps, double temp){
        if(sweeps <= 0){
            throw new IllegalArgumentException("Sweeps must be positive");
        }
        
        for(int i = 1; i < sweeps; i++){
            sampleSeatingArrangements(1);
            sampleHyperParams(temp);
        }
        
        sampleSeatingArrangements(1);
        return sampleHyperParams(temp);
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
    public HashMap<Context, MutableInt> getImpliedData(){
        HashMap<Context, MutableInt> impliedData = new HashMap();
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


    /*private MersenneTwisterFast rng;
    private Restaurant ecr, root;
    private MutableDouble[] discounts;
    private MutableDouble[] concentrations;
    private GammaDistribution concentrationPrior;*/
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(rng);
        out.writeObject(concentrations);
        out.writeObject(discounts);
        out.writeObject(concentrationPrior);
        out.writeObject(root);
        ecr.serializeOut(out);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        rng = (MersenneTwisterFast) in.readObject();
        concentrations = (MutableDouble[]) in.readObject();
        discounts = (MutableDouble[]) in.readObject();
        concentrationPrior = (GammaDistribution) in.readObject();
        root = (RootRestaurant) in.readObject();
        ecr = new Restaurant(root, concentrations[0], discounts[0]);
        ecr.serializeIn(in, 0, discounts, concentrations);
    }

    public void writeExternalNoHyperParams(ObjectOutput out) throws IOException{
        out.writeObject(rng);
        out.writeObject(root);
        ecr.serializeOut(out);
    }

    public void readExternalNoHyperParams(ObjectInput in, MutableDouble[] concentrations, MutableDouble[] discounts, GammaDistribution concentrationPrior) throws ClassNotFoundException, IOException {
        rng = (MersenneTwisterFast) in.readObject();
        this.concentrations = concentrations;
        this.discounts = discounts;
        this.concentrationPrior = concentrationPrior;
        root = (RootRestaurant) in.readObject();
        ecr = new Restaurant(root, concentrations[0], discounts[0]);
        ecr.serializeIn(in, 0, this.discounts, this.concentrations);
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
    private double sampleHyperParams(double temp){
        double stdDiscounts = .07,stdConcentrations = .7;
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
            r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

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
            r = Math.pow(r < 1.0 ? r : 1.0, 1.0 / temp);

            if (rng.nextBoolean(r)) {
                score += afterScore[i];
            } else {
                score += currentScore[i];
                concentrations[i].set(currentConcentrations[i]);
            }
        }

        return score + root.score();
    }

    /**
     * Recursive function to get the implied data in this HPYP object.
     * @param currentRestaurant current restaurant of the recursion
     * @param impliedData implied data
     * @param thisContext context at current restaurant
     */
    private void getImpliedData(Restaurant currentRestaurant, HashMap<Context, MutableInt> impliedData, int[] thisContext){
        TIntObjectIterator<Restaurant> iterator = currentRestaurant.iterator();
        while(iterator.hasNext()){
            iterator.advance();

            int[] childContext = new int[thisContext.length + 1];
            System.arraycopy(thisContext, 0, childContext, 0, thisContext.length);
            childContext[thisContext.length] = iterator.key();

            getImpliedData(iterator.value(), impliedData, childContext);
        }

        int count = currentRestaurant.impliedData();
        if(count > 0){
            impliedData.put(new Context(thisContext), new MutableInt(count));
        }
    }
    
    
    public static void main(String[] args) throws IOException, ClassNotFoundException{
        File f = new File("/Users/nicholasbartlett/Documents/np_bayes/data/alice_in_wonderland/alice_in_wonderland.txt");
        //File f = new File("/home/bartlett/BNOL/alice_in_wonderland.txt");

        int depth = 10;
        int[] context = new int[depth];

        BufferedInputStream bis = null;

        MutableDouble[] conc = new MutableDouble[depth + 1];
        MutableDouble[] disc = new MutableDouble[depth + 1];

        for(int i = 0; i < disc.length; i++){
            disc[disc.length - 1 - i] = new MutableDouble(Math.pow(0.9, i + 1));
            conc[i] = new MutableDouble(1.0);
        }

        IntHPYP hpyp = new IntHPYP(disc, conc, new IntUniformDiscreteDistribution(256), new GammaDistribution(1,100));

        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;

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

            System.out.println(hpyp.score(true));

            oos = new ObjectOutputStream(new FileOutputStream(new File("/Users/nicholasbartlett/Desktop/hpyp.out")));

            oos.writeObject(hpyp);

            oos.close();
 
            ois = new ObjectInputStream(new FileInputStream(new File("/Users/nicholasbartlett/Desktop/hpyp.out")));


            IntHPYP h = (IntHPYP) ois.readObject();

            System.out.println(h.score(true));

            hpyp.printConcentrations();
            hpyp.printDiscounts();
            h.printConcentrations();
            h.printDiscounts();

            for(int i = 0; i < 10; i++){
                System.out.println(hpyp.sample(1));
                System.out.println(h.sample(1));
                System.out.println();
            }
            hpyp.printConcentrations();
            hpyp.printDiscounts();
            h.printConcentrations();
            h.printDiscounts();

        } finally {
            if(bis != null){
                bis.close();
            }
            ois.close();
            
        }
    }

}
