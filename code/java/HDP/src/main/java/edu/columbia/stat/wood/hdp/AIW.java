/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.hdp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * @author nicholasbartlett
 */
public class AIW {

    public static int[] aiw() throws IOException {
        File f = new File("/Users/nicholasbartlett/Documents/np_bayes/data/alice_in_wonderland/alice_in_wonderland.txt");
        int[] aiw = new int[(int) f.length()];

        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(f));
            int b;
            int index = 0;
            while ((b = bis.read()) > -1){
                aiw[index++] = b;
            }
        } finally {
            if (bis != null) {
                bis.close();
            }
        }

        return aiw;
    }
}
