/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp;

import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDiscreteDistribution;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDoublePair;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MersenneTwisterFast;
import edu.columbia.stat.wood.stickbreakinghpyp.util.MutableInt;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 *
 * @author nicholasbartlett
 */
public class BaseRestaurant extends Restaurant {

    private IntDiscreteDistribution baseDistribution;
    private HashMap<Integer, MutableInt> counts;

    public BaseRestaurant(IntDiscreteDistribution baseDistribution) {
        super(null,null,null);
        this.baseDistribution = baseDistribution;
        counts = new HashMap<Integer, MutableInt>();
    }

    @Override
    public double probability(int type) {
        return baseDistribution.probability(type);
    }

    @Override
    public void adjustCount(int type, int multiplicity) {
        MutableInt value = counts.get(type);

        if (value == null) {
            value = new MutableInt(0);
            counts.put(type, value);
        }

        value.plusEquals(multiplicity);
        if (value.value() == 0) {
            counts.remove(type);
        }
    }

    @Override
    public boolean sample(MersenneTwisterFast rng) {
        throw new RuntimeException("Yikes, this should never get called");
    }

    @Override
    public double score() {
        double score = 0d;

        for (Entry<Integer, MutableInt> entry : counts.entrySet()) {
            score += ((double) entry.getValue().value()) * Math.log(baseDistribution.probability(entry.getKey()));
        }

        return score;
    }

    @Override
    public String toString() {
        String string = "Base Restaurant \n";
        for (Entry<Integer, MutableInt> entry : counts.entrySet()) {
            string += "Type = " + entry.getKey() + ", Count = " + entry.getValue().value() + "\n";
        }
        return string;
    }

    public Iterator<IntDoublePair> iterator() {
        return baseDistribution.iterator();
    }
}
