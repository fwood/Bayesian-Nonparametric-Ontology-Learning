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
public class Generate4StateSynthetic extends GenerateData {

    @Override
    public Pair<int[], int[]> generate(int n, Random rng) {
        int[] s = new int[n];
        int[] y = new int[n];

        s[0] = (int) (rng.nextDouble() * 4) + 1;

        for (int i = 1; i < n; i++) {
            s[i] = s[i-1];
            if (rng.nextDouble() < 0.99) s[i]++;
            if (s[i] == 5) s[i] = 1;
        }

        for (int i = 0; i < n; i++) {
            switch (s[i]) {
                case 1:
                    if (rng.nextDouble() < 0.5) y[i] = 1;
                    else y[i] = 2;
                    break;
                case 2:
                    if (rng.nextDouble() < (2d / 3d)) y[i] = 0;
                    else if(rng.nextDouble() < 0.5) y[i] = 1;
                    else y[i] = 2;
                    break;
                case 3:
                    if (rng.nextDouble() < 0.5) y[i] = 0;
                    else y[i] = 2;
                    break;
                case 4:
                    if (rng.nextDouble() < (1d / 3d)) y[i] = 0;
                    else if(rng.nextDouble() < 0.5) y[i] = 1;
                    else y[i] = 2;
                    break;
                default:
                    throw new RuntimeException("states should all be 1 - 4");
            }
        }
        return new Pair(s,y);
    }
}
