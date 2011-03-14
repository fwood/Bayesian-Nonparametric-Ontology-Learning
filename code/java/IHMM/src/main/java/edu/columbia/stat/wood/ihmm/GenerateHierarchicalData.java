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
public class GenerateHierarchicalData extends GenerateData {

    @Override
    public Pair<int[], int[]> generate(int n, Random rng) {
        int[] s = new int[n];
        int[] y = new int[n];

        s[0] = (int) (rng.nextDouble() * 7) + 1;
        for (int i = 1; i < n; i++) {
            s[i] = s[i-1];
            if (rng.nextDouble() < 0.1) s[i]++;
            if (s[i] == 8) s[i] = 1;
        }

        double[] state_00 = new double[]{1d, 0d, 0d, 0d};
        double[] state_01 = new double[]{0d, 1d, 0d, 0d};
        double[] state_0 = new double[]{0.5, 0.5, 0d, 0d};
        double[] state_10 = new double[]{0d, 0d, 1d, 0d};
        double[] state_11 = new double[]{0d, 0d, 0d, 1d};
        double[] state_1 = new double[]{0d, 0d, 0.5, 0.5};
        double[] state_ = new double[]{0.25, 0.25, 0.25, 0.25};

        double[] pmf;

        for (int i = 0; i < n; i++) {
            pmf = null;
            switch (s[i]) {
                case 1:
                    pmf = state_;
                    break;
                case 2:
                    pmf = state_0;
                    break;
                case 3:
                    pmf = state_1;
                    break;
                case 4:
                    pmf = state_00;
                    break;
                case 5:
                    pmf = state_01;
                    break;
                case 6:
                    pmf = state_10;
                    break;
                case 7:
                    pmf = state_11;
                    break;
                default:
                    throw new RuntimeException("should not happen");
            }
            
            double cuSum = 0d;
            double r = rng.nextDouble();
            while (cuSum <= r) cuSum += pmf[y[i]++];
        }

        return new Pair(s,y);
    }
    

}
