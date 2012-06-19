/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.mcr;

/**
 *
 * @author fwood
 */
public class TypePair {
    int first;
    int second;

    public TypePair(int first, int second) {
        this.first = first;
        this.second = second;
    }

    public int[] types() {
        int[] ret = new int[2];
        ret[0] = first;
        ret[1] = second;
        return ret;
    }

    public int getFirst() {
        return first;
    }

    public void setFirst(int first) {
        this.first = first;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TypePair other = (TypePair) obj;
        if (this.first != other.first) {
            return false;
        }
        if (this.second != other.second) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.first;
        hash = 67 * hash + this.second;
        return hash;
    }



}
