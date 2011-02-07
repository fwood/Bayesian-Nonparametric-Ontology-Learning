/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp.util;

import java.io.Serializable;

/**
 * Container class for int which allows the underlying value to be set.
 * @author nicholasbartlett
 */

public class MutableInt implements Serializable{

    static final long serialVersionUID = 1;

    private int i;

    /**
     * @param value value of underlying int
     */
    public MutableInt(int value){
        i = value;
    }

    /**
     * Allows the underlying value to be set.
     * @param value int value to assign this object
     */
    public void set(int value){
        i = value;
    }

    /**
     * Gets the int value of this object.
     * @return int value of this object
     */
    public int value(){
        return i;
    }

    /**
     * Increments the underlying value by 1.
     */
    public void increment(){
        i++;
    }

    /**
     * Decrements the underlying value by 1.
     */
    public void decrement(){
        i--;
    }

    /**
     * Adds increment to the underlying value.
     * @param increment amount to add
     */
    public void plusEquals(int increment){
        i += increment;
    }

    /**
     * Gets a deep copy of this object.
     * @return deep copy of this object
     */
    public MutableInt deepCopy(){
        return new MutableInt(i);
    }

    /**
     * Overrides hash code to only reflect underlying int value.
     * @return hash code
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + this.i;
        return hash;
    }

    /**
     * Overrides the equals() method to only reflect the underlying int value.
     * @param o object to compare to
     * @return true if equal, else false
     */
    @Override
    public boolean equals(Object o){
        if(o == null){
            return false;
        } else if(o.getClass() != this.getClass()){
            return false;
        } else if(((MutableInt) o).value() == i){
            return true;
        } else {
            return false;
        }
    }
}
