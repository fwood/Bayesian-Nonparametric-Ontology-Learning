/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp;

import edu.columbia.stat.wood.stickbreakinghpyp.Restaurant.SortedPartialDistributionIterator;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntDoublePair;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author nicholasbartlett
 */
public class DistributionIterator implements Iterator<IntDoublePair> {
    private SortedPartialDistributionIterator iter1;
    private Iterator<IntDoublePair> iter2;
    private int iterIndex;
    private HashSet<Integer> keysAlreadyReturned;
    private double multiplier;
    private IntDoublePair next;

    public DistributionIterator(SortedPartialDistributionIterator iter1, Iterator<IntDoublePair> iter2) {
        this.iter1 = iter1;
        this.iter2 = iter2;

        iterIndex = 1;
        keysAlreadyReturned = iter1.keys();
        multiplier = iter1.probBackOffToBase();
        updateNext();
    }

    public boolean hasNext() {
        return next != null;
    }

    public IntDoublePair next() {
        IntDoublePair n = next;
        updateNext();
        return n;
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private void updateNext() {
        if (iterIndex == 1) {
            if (iter1.hasNext()) {
                next = iter1.next();
            } else {
                iterIndex = 2;
            }
        }

        outer_if:
        if (iterIndex == 2) {
            while (iter2.hasNext()) {
                next = iter2.next();
                if (!keysAlreadyReturned.contains(next.intValue())) {
                    next = new IntDoublePair(next.intValue(), multiplier * next.doubleValue());
                    break outer_if;
                }
            }
            next = null;
        }
    }
}
