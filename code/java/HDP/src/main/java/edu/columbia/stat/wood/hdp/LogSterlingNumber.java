/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.hdp;

import java.util.HashMap;

/**
 *
 * @author nicholasbartlett
 */
public class LogSterlingNumber {

    private HashMap<IntPair, Double> map;

    public LogSterlingNumber() {
        map = new HashMap<IntPair,Double>();
    }
    
    public double get(int n, int m) {
        assert n >= m : "n = " + n + " : m = " + m;
        assert n > 0 : "n must be larger than 0, n = " + n;
        assert m > 0 : "m must be larger than 0, m = " + m;

        IntPair key = new IntPair(n, m);
        Double lsn = map.get(key);

        if (lsn == null) {
            if (n == m) {
                lsn = new Double(0.0);
            } else if (m == 1){
                lsn = new Double(Math.log(n - 1) + get(n-1,m));
            } else {
                double p1 = Math.log(n - 1) + get(n - 1, m);
                double p2 = get(n - 1, m - 1);
                if(p1 > p2){
                    lsn = new Double(Math.log(1.0 + Math.exp(p2 - p1)) + p1);
                } else {
                    lsn = new Double(Math.log(1.0 + Math.exp(p1 - p2)) + p2);
                }
            }
            map.put(key, lsn);
        }

        return lsn.doubleValue();
    }

    private class IntPair {

        private int f;
        private int s;

        public IntPair(int first, int second) {
            f = first;
            s = second;
        }

        public int first() {
            return f;
        }

        public int second() {
            return s;
        }

        public IntPair copy() {
            return new IntPair(f, s);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 73 * hash + this.f;
            hash = 73 * hash + this.s;
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o.getClass().equals(this.getClass())) {
                return ((IntPair) o).first() == f && ((IntPair) o).second() == s;
            } else {
                return false;
            }
        }
    }
}
