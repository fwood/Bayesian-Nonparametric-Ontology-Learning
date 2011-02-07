/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp.util;

import java.io.Serializable;

/**
 *
 * @author nicholasbartlett
 */
public class DoubleArrayList implements Serializable {
    private int l = 100;
    private double[] value = new double[l];
    private int index = 0;

    public void add(double d) {
        if (index == l) {
            double[] old_value = value;
            value = new double[l + 100];
            System.arraycopy(old_value, 0, value, 0, l);
            l += 100;
        }
        value[index++] = d;
    }

    public double[] toArray() {
        double[] array = new double[index];
        System.arraycopy(value, 0, array, 0, index);
        return array;
    }
}
