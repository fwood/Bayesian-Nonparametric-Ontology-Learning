/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.ihmm;

import edu.columbia.stat.wood.hdp.BinaryContext;
import edu.columbia.stat.wood.stickbreakinghpyp.HPYP;
import edu.columbia.stat.wood.stickbreakinghpyp.util.IntBinaryExpansionDistribution;

/**
 *
 * @author nicholasbartlett
 */
public class HPYPLikelihood extends Likelihood {

    private HPYP hpyp;

    public HPYPLikelihood(){
        double[] concentrations = new double[]{10d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d};
        double[] discounts = new double[]{0.1, 0.1, 0.2, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};

        hpyp = new HPYP(concentrations, discounts, new IntBinaryExpansionDistribution(0.2), 10);
    }

    @Override
    public void sample() {
        hpyp.sampleWeights();
    }

    @Override
    public double probability(int state, int observation) {
        return hpyp.probability(BinaryContext.toExpansion(state), observation);
    }

    @Override
    public void adjustCount(int state, int observation, int multiplicity) {
        hpyp.adjustCount(BinaryContext.toExpansion(state), observation, multiplicity);
    }

    @Override
    public double score() {
        return hpyp.score();
    }

}
