/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.util;

import java.util.Arrays;

/**
 *
 * @author nicholasbartlett
 */
public class Context {
    private int[] value;

    public Context(int[] val){
        value = val;
    }

    public int[] value(){
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Arrays.hashCode(value);
        return hash;
    }

    @Override
    public boolean equals(Object o){
        if(o == null){
            return false;
        } else if(o.getClass() == getClass()){
            return Arrays.equals(value, ((Context) o).value());
        } else {
            return false;
        }
    }

}
