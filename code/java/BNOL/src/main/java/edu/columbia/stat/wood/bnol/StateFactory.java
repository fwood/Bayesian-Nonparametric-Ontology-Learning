/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.HPYP;
import edu.columbia.stat.wood.bnol.util.IntArrayDiscreteDistribution;
import edu.columbia.stat.wood.bnol.util.IntGeometricDistribution;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.Pair;

/**
 *
 * @author nicholasbartlett
 */

public class StateFactory {

    private Node baseNode;
    private double b;

    public StateFactory(double b){
        baseNode = new Node(b);
        if(b > 1.0 || b < 0.0){
            throw new IllegalArgumentException("b must be in 0 - 1");
        }
        this.b = b;
    }
    
    public double logProbability(byte[] s, Machine m){
        double ll = 0.0;
        Machine[] context = new Machine[]{m};

        Node currentNode = baseNode;
        for(int i = 0; i < s.length; i++){
            ll += Math.log(currentNode.probability(context, s[i]));
            currentNode = currentNode.get(s[i]);
            if(currentNode == null){
                for(int j = i+1; j < s.length; j++){
                    ll += Math.log((1 - b) / 2);
                }
                break;
            }
        }
        
        if(currentNode != null){
            ll += Math.log(currentNode.probability(context, 2));
        } else {
            ll += Math.log(b);
        }
        
        return ll;
    }

    public byte[] generate(Machine m){
        



        return null;
    }

    private class Node extends HPYP<Machine> {
        private Pair<Node,Node> children = new Pair<Node,Node>();

        public Node(double b){
            super(1, new MutableDouble[]{new MutableDouble(1),new MutableDouble(1)}, new MutableDouble[]{new MutableDouble(1),new MutableDouble(1)}, new IntArrayDiscreteDistribution(new double[]{(1-b)/2, (1-b)/2,b}));
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
