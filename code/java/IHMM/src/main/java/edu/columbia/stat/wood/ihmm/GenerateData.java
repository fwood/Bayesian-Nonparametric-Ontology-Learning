/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.ihmm;

import edu.columbia.stat.wood.stickbreakinghpyp.util.Pair;
import java.util.Random;

/**
 *
 * @author nicholasbartlett
 */
public abstract class GenerateData {

    public abstract Pair<int[], int[]> generate(int n, Random rng);

}
