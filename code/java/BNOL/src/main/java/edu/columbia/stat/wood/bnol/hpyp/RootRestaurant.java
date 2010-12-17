/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.hpyp;

import edu.columbia.stat.wood.bnol.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableInt;
import edu.columbia.stat.wood.bnol.util.Pair;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Iterator;

/**
 *
 * @author nicholasbartlett
 */
public class RootRestaurant extends Restaurant {

    public IntDiscreteDistribution baseDistribution;
    private TIntObjectHashMap<MutableInt> customerCount;

    public RootRestaurant(IntDiscreteDistribution baseDistribution){
        this.baseDistribution = baseDistribution;
        customerCount = new TIntObjectHashMap<MutableInt>();
    }

    @Override
    public double probability(int type){
        return baseDistribution.probability(type);
    }

    @Override
    public void seat(int type,MersenneTwisterFast rng){
        MutableInt c = customerCount.get(type);
        if(c == null){
            c = new MutableInt(1);
            customerCount.put(type, c);
        } else {
            c.increment();
        }
    }

    @Override
    public void unseat(int type,MersenneTwisterFast rng){
        MutableInt c = customerCount.get(type);
        c.decrement();

        assert c.value() >= 0;
    }

    @Override
    public int draw(MersenneTwisterFast rng){
        Iterator<Pair<Integer,Double>> iterator = baseDistribution.iterator();
        double r = rng.nextDouble();
        double cuSum = 0.0;
        Pair<Integer,Double> pair;
        int draw = -1;
        while(iterator.hasNext()){
            pair = iterator.next();
            cuSum += pair.second().doubleValue();
            if(cuSum > r){
                draw = pair.first().intValue();
                break;
            }
        }

        assert draw != -1;

        MutableInt cnt = customerCount.get(draw);
        if(cnt == null){
            customerCount.put(draw,new MutableInt(1));
        } else {
            cnt.increment();
        }

        return draw;
    }

    @Override
    public void sampleSeatingArrangements(MersenneTwisterFast rng){}

    @Override
    public double score(){
        double score = 0.0;
        TIntObjectIterator<MutableInt> iterator = customerCount.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            score += (double) iterator.value().value() * Math.log(baseDistribution.probability(iterator.key()));
        }
        return score;
    }

    @Override
    public boolean checkCounts(){
        return true;
    }

    @Override
    public void removeZeros(){}
}
