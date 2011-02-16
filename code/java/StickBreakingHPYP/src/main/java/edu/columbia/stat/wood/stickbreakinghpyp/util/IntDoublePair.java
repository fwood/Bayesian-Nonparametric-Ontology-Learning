/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.stickbreakinghpyp.util;

/**
 *
 * @author nicholasbartlett
 */
public class IntDoublePair {

    public int i;
    public double d;

    public IntDoublePair(int i, double d) {
        this.i = i;
        this.d = d;
    }

    public IntDoublePair() {}

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.i;
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.d) ^ (Double.doubleToLongBits(this.d) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        } else if (object.getClass() == getClass()) {
            IntDoublePair castObject = (IntDoublePair) object;
            if (castObject.i == i && castObject.d == d) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


}
