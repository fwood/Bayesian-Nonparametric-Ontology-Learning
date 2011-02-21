/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp;

import edu.columbia.stat.wood.stickbreakinghpyp.util.MersenneTwisterFast;
import edu.columbia.stat.wood.stickbreakinghpyp.util.RND;
import java.io.Serializable;

/**
 *
 * @author nicholasbartlett
 */
public class TypeWeights implements Serializable{

    public double[] weights;
    public int[] assignments;
    public int count;

    public TypeWeights(){
        count = 0;
        weights = new double[0];
    }

    public void adjustCount(int multiplicity) {
        count += multiplicity;
        if (count < 0) {
            throw new RuntimeException("Cannot remove so many counts of this type");
        }
    }

    public double totalWeight(){
        double totalWeight = 0d;
        for (double weight : weights) {
            totalWeight += weight;
        }
        return totalWeight;
    }

    public int sampleAssignments(double parentProbability, double probabilityOfBackOff, MersenneTwisterFast rng){
        assignments = new int[weights.length];

        double randomNumber, cuSum, totalProb = totalWeight() + parentProbability * probabilityOfBackOff;
        int index, innovativeSeatings = 0, zeroCount = weights.length;

        // assign every count
        outer_for:
        for (int c = 0; c < count; c++) {
            randomNumber = rng.nextDouble();
            cuSum = 0d;
            index = 0;
            for (double weight : weights) {
                cuSum += weight / totalProb;
                if (cuSum > randomNumber) {
                    if (assignments[index] == 0) {
                        zeroCount--;
                    }
                    assignments[index]++;
                    continue outer_for;
                }
                index++;
            }
            assert index == assignments.length;
            innovativeSeatings++;
        }

        // remove zeros
        assert zeroCount >= 0;
        if (zeroCount > 0) {
            int[] oldAssignments = assignments;
            assignments = new int[oldAssignments.length - zeroCount];
            index = 0;
            for (int c : oldAssignments) {
                if (c != 0) {
                    assignments[index++] = c;
                }
            }
            assert index == assignments.length;
        }
        
        return innovativeSeatings;
    }

    public void sampleWeights(double totalWeight, double discount){
        double[] dirichletParameters = new double[assignments.length];
        for (int i = 0; i < assignments.length; i++){
            dirichletParameters[i] = (double) assignments[i] - discount;
        }

        weights = RND.sampleDirichlet(dirichletParameters);

        for (int i = 0; i < weights.length; i++) {
            weights[i] *= totalWeight;
        }
    }

    public double score(double discount) {
        double score = 0.0;

        for (int table : assignments) {
            if (table > 0) {
                for (int customer = 1; customer < table; customer++) {
                    score += Math.log((double) customer - discount);
                }
            }
        }

        return score;
    }
}
