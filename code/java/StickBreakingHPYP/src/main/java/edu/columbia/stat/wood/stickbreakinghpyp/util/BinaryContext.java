/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.stickbreakinghpyp.util;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author nicholasbartlett
 */
public class BinaryContext implements Serializable {

    private static final long serialVersionUID = 1;

    private int[] value;

    /****************Constructor Methods***************************************/

    public BinaryContext(int[] value){
        this.value = value;
    }

    /****************Public Methods********************************************/

    public int getInt(){
        return BinaryContext.toInt(value);
    }

    public int[] getValue(){
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Arrays.hashCode(this.value);
        return hash;
    }

    @Override
    public boolean equals(Object o){
        if(o == null){
            return false;
        } else if(o.getClass() == getClass()){
            return Arrays.equals(value, ((BinaryContext) o).getValue());
        } else {
            return false;
        }
    }

    /****************Static Methods********************************************/

    public static int[] toExpansion(int integer){
        if (integer <= 0){
            throw new IllegalArgumentException("integer must be positive, not " + integer);
        } else {
            String binaryString = Integer.toBinaryString(integer);
            int[] expansion = new int[binaryString.length() - 1];
            for (int i = 0; i < expansion.length; i++){
                if (binaryString.charAt(i + 1) == '0'){
                    expansion[i] = 0;
                } else {
                    expansion[i] = 1;
                }
            }
            return expansion;
        }
    }

    public static int toInt(int[] expansion){
        int ret = 1;
        for (int bit : expansion){
            ret <<= 1;
            ret += bit;
        }
        return ret;
    }
}
