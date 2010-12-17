/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.HPYP;
import edu.columbia.stat.wood.bnol.util.IntGeometricDistribution;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.Pair;

/**
 *
 * @author nicholasbartlett
 */

public class StateFactory {

    

    private class Node extends HPYP<Machine> {
        private Pair<Node,Node> children = new Pair<Node,Node>();

        public Node(double q){
            super(1, new MutableDouble[]{new MutableDouble(1),new MutableDouble(1)}, new MutableDouble[]{new MutableDouble(1),new MutableDouble(1)}, new IntGeometricDistribution(q));
        }

        public Node get(byte key){
            if(key == 0){
                return children.first();
            } else if (key == 1){
                return children.second();
            } else {
                throw new IllegalArgumentException("key must be 0 or 1");
            }
        }

        public void put(byte key, Node child){
            if(key == 0){
                children.setFirst(child);
            } else if (key == 1){
                children.setSecond(child);
            } else {
                throw new IllegalArgumentException("key must be 0 or 1");
            }
        }

        public void remove(byte key){
            if(key == 0){
                children.setFirst(null);
            } else if (key == 1){
                children.setSecond(null);
            } else {
                throw new IllegalArgumentException("key must be 0 or 1");
            }
        }
    }
}
