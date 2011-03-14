/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.stickbreakinghpyp.util;

/**
 * Container object for one int value and one double value.  The object is
 * immutable and the hashcode and value only reflect the underlying values
 * in the container.
 * @author nicholasbartlett
 */
public class IntDoublePair {

    /**
     * Raw values in the container.
     */
    private int i;
    private double d;

    /**
     * Creates an IntDobulePair with the specified values
     * @param i int value
     * @param d double value
     */
    public IntDoublePair(int i, double d) {
        this.i = i;
        this.d = d;
    }

    /**
     * Null constructor.
     */
    public IntDoublePair() {
    }

    /**
     * Gets the underlying int value.
     * @return underlying int value
     */
    public int intValue() {
        return i;
    }

    /**
     * Gets the underlying double value.
     * @return underlying double value
     */
    public double doubleValue() {
        return d;
    }

    /**
     * Overrides hashcode so that it only reflects the values housed in this
     * container object.
     * @return hashcode value
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.i;
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.d) ^ (Double.doubleToLongBits(this.d) >>> 32));
        return hash;
    }

    /**
     * Overrides equals method so that it only reflects the values housed in
     * this container object.
     * @param object comparison object
     * @return true of equal to object, else false
     */
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
