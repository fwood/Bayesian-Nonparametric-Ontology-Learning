/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.hpyp;

import edu.columbia.stat.wood.bnol.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableInt;
import edu.columbia.stat.wood.bnol.util.Pair;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * An override of the Restaurant class to put at the very base of the tree to
 * facilitate the end of the many recursive methods which run up the tree from
 * its nodes.
 * @author nicholasbartlett
 */
public class RootRestaurant extends Restaurant {

    static final long serialVersionUID = 1 ;

    private IntDiscreteDistribution baseDistribution;
    private HashMap<Integer, MutableInt> customerCount;

    /***********************constructor methods********************************/

    public RootRestaurant(IntDiscreteDistribution baseDistribution){
        super(null, null, null);
        this.baseDistribution = baseDistribution;
        customerCount = new HashMap();
    }

    public RootRestaurant(){
        super(null, null, null);
    }

    /***********************public methods*************************************/

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmptyRestaurant(){
        int customers = 0;
        for(MutableInt value : customerCount.values()){
            customers += value.value();
        }

        return customers == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double probability(int type){
        return baseDistribution.probability(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seat(int type, MersenneTwisterFast rng){
        MutableInt count = customerCount.get(type);
        if(count == null){
            count = new MutableInt(1);
            customerCount.put(type, count);
        } else {
            count.increment();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unseat(int type, MersenneTwisterFast rng){
        MutableInt count = customerCount.get(type);
        count.decrement();

        assert count.value() >= 0;
        
        if(count.value() == 0){
            customerCount.remove(type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int draw(double low, double high, int[] keyOrder, MersenneTwisterFast rng){
        double cuSum = 0.0, randomNumber;
        
        if(keyOrder != null){
            randomNumber = rng.nextDouble() * (high - low) + low;
            for(int key : keyOrder){
                cuSum += baseDistribution.probability(key);
                if(cuSum > randomNumber){
                    seat(key, rng);
                    return key;
                }
            }
        } else {
            int sample = baseDistribution.generate(rng);
            seat(sample, rng);
            return sample;
        }

        throw new RuntimeException("You should never get to here since you should always generate a random number");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int generate(double low, double high, int[] keyOrder, MersenneTwisterFast rng){
        double cuSum = 0.0, randomNumber;
        
        if(keyOrder != null){
            randomNumber = rng.nextDouble() * (high - low) + low;
            for(int key : keyOrder){
                cuSum += baseDistribution.probability(key);
                if(cuSum > randomNumber){
                    return key;
                }
            }
        } else {
           return baseDistribution.generate(rng);
        }

        throw new RuntimeException("You should never get to here since you should always generate a random number");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sampleSeatingArrangements(MersenneTwisterFast rng){
        throw new RuntimeException("This method should never be called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double score(){
        double score = 0.0;
        for(Entry<Integer, MutableInt> entry : customerCount.entrySet()){
            score += (double) entry.getValue().value() * Math.log(baseDistribution.probability(entry.getKey().intValue()));
        }
        return score;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeZeros(){
        throw new RuntimeException("This method should never be called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkCounts(){
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String toStr = "Root Restaurant : \n";
        for(Entry<Integer, MutableInt> entry : customerCount.entrySet()){
            toStr = toStr + entry.getKey().intValue() + "->" + entry.getValue().value() + "\n";
        }
        
        return toStr;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int impliedData(){
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException{
        out.writeObject(baseDistribution);
        out.writeObject(customerCount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException{
        baseDistribution = (IntDiscreteDistribution) in.readObject();
        customerCount = (HashMap<Integer, MutableInt>) in.readObject();
    }
}
