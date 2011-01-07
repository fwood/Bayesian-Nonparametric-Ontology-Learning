/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.hpyp.IntHPYP;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.Pair;
import gnu.trove.list.array.TByteArrayList;

/**
 *
 * @author nicholasbartlett
 */

public class StateFactory {
/*
    private Node baseNode;
    private IntTreeDiscreteDistribution baseDist;

    public StateFactory(MutableDouble b){
        if(b.value() > 1.0 || b.value() < 0.0){
            throw new IllegalArgumentException("b must be in 0 - 1");
        }
        baseDist = new IntTreeDiscreteDistribution(b);
        baseNode = new Node(baseDist);
    }
    
    public double logProbability(byte[] s, Integer phi){
        double ll = 0.0;
        Integer[] context = new Integer[]{phi};

        Node currentNode = baseNode;
        for(int i = 0; i < s.length; i++){
            ll += Math.log(currentNode.probability(context, s[i]));
            currentNode = currentNode.get(s[i]);
            if(currentNode == null){
                for(int j = i+1; j < s.length; j++){
                    ll += Math.log(baseDist.probability(0));
                }
                break;
            }
        }
        
        if(currentNode != null){
            ll += Math.log(currentNode.probability(context, 2));
        } else {
            ll += Math.log(baseDist.probability(2));
        }
        
        return ll;
    }

    public byte[] generate(Integer phi){
        TByteArrayList out = new TByteArrayList();
        byte key;
        Node currentNode = baseNode;
        Integer[] machineState = new Integer[]{phi};

        while(true){
            key = (byte) currentNode.generate(machineState);

            if(key == 2){
                break;
            }
            
            out.add(key);
            currentNode = currentNode.get(key);
        }

        return out.toArray();
    }

    public byte[] generate(Machine m, double low, double high){
        
        return null;
    }

    public byte[] draw(Integer phi, boolean buffer){
        TByteArrayList out = new TByteArrayList();
        byte key;
        Node currentNode = baseNode;
        Integer[] machineState = new Integer[]{phi};

        while(true){
            key = (byte) currentNode.draw(machineState, buffer);

            if(key == 2){
                break;
            }

            out.add(key);
            currentNode = currentNode.get(key);
        }

        return out.toArray();
    }

    
    private class Node extends IntHPYP<Integer> {
        private Pair<Node,Node> children = new Pair<Node,Node>();

        public Node(IntTreeDiscreteDistribution baseDist){
            super(1, new MutableDouble[]{new MutableDouble(1),new MutableDouble(1)}, new MutableDouble[]{new MutableDouble(1),new MutableDouble(1)}, baseDist);
        }

        public Node get(byte key){
            if(key == 0){
                if(children.first() == null){
                    children.setFirst(new Node(baseDist));
                }
                return children.first();
            } else if (key == 1){
                if(children.second() == null){
                    children.setSecond(new Node(baseDist));
                }
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
    */
}
