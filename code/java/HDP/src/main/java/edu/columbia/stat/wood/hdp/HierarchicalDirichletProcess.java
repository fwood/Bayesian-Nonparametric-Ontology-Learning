/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.hdp;

import edu.columbia.stat.wood.hdp.DiscreteDistribution.IntDoublePair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

/**
 *
 * @author nicholasbartlett
 */
public class HierarchicalDirichletProcess implements Iterable<DirichletProcessDistribution> {

    private MapNode root;
    private DiscreteDistribution base;
    private MutableDouble[] concentrations;
    private Random rng;
    private double concentrationPriorMean;

    public HierarchicalDirichletProcess(MutableDouble[] concentrations, DiscreteDistribution baseDistribution, double concentrationPriorMean, Random rng) {
        this.concentrations = concentrations;
        this.concentrationPriorMean = concentrationPriorMean;
        base = baseDistribution;
        root = new MapNode(new DirichletProcessDistribution(baseDistribution, getConcentration(0)));
        this.rng = rng;
        RND.setRNG(rng);
    }

    public HierarchicalDirichletProcess(MutableDouble[] concentrations, DiscreteDistribution baseDistribution, double concentrationPriorMean) {
        this(concentrations, baseDistribution, concentrationPriorMean, new Random());
    }

    public HierarchicalDirichletProcess(double[] concentrations, DiscreteDistribution baseDistribution, double concentrationPriorMean) {
        MutableDouble[] conc = new MutableDouble[concentrations.length];
        for (int i = 0; i < concentrations.length; i++) {
            conc[i] = new MutableDouble(concentrations[i]);
        }

        this.concentrations = conc;
        this.concentrationPriorMean = concentrationPriorMean;
        base = baseDistribution;
        root = new MapNode(new DirichletProcessDistribution(baseDistribution, getConcentration(0)));
        rng = new Random();
        RND.setRNG(rng);
    }

    /**
     * Adjusts the count of the given type in the given context.
     * @param context context
     * @param type type
     * @param multiplicity amount to add
     */
    public void adjustCount(int[] context, int type, int multiplicity) {
        get(context).adjustObservedCount(type, multiplicity);
    }

    public void adjustCount(int context, int type, int multiplicity) {
        get(context).adjustObservedCount(type, multiplicity);
    }

    /**
     * Gets the probability of the type in the given context.
     * @param context context
     * @param type type of interest
     * @return probability of type
     */
    public double probability(int[] context, int type) {
        return get(context).probability(type);
    }

    public double probability(int context, int type) {
        return get(context).probability(type);
    }

    /**
     * Gets an iterator over the distribution at the given context.
     * @param context context
     * @return iterator
     */
    public Iterator<IntDoublePair> iterator(int[] context) {
        return get(context).iterator();
    }

    public Iterator<IntDoublePair> iterator(int context) {
        return get(context).iterator();
    }

    /**
     * Finds the joint score of the model and data.
     * @return score
     */
    public double score() {
        double score = base.score();
        for (DirichletProcessDistribution dist : this) {
            score += dist.score();
        }
        return score;
    }

    /**
     * Samples all the discrete distributions in the map.
     */
    public void sample() {
        for (DirichletProcessDistribution dist : this) {
            dist.sample();
        }
        sampleConcentrations(1d);
    }

    /**
     * Gets an iterator over Dirichlet Process Distributions in this tree, the
     * order of return is a DFS.
     * @return iterator over distributions in this tree in DFS order
     */
    @Override
    public Iterator<DirichletProcessDistribution> iterator() {
        ArrayList<DirichletProcessDistribution> list = new ArrayList<DirichletProcessDistribution>(100000);
        getListOfDistributions(root, list);
        return list.iterator();
    }

    /**
     * Print formatted concentration values for this HDP.
     */
    public void printConcentrations() {
        System.out.format("Concentrations = [%.2f", concentrations[0].value());
        for (int i = 1; i < concentrations.length; i++) {
            System.out.format(", %.2f", concentrations[i].value());
        }
        System.out.println("]");
    }

    
    public void print(){
        System.out.print("null --> " + "[" + root.distribution.probability(1));
        for (int i = 2; i <= 20; i++) {
            System.out.print(", " + root.distribution.probability(i));
        }
        System.out.println("]");

        for (int i = 1; i <= 20; i++) {
            DiscreteDistribution dist = get(i);
            System.out.print(dist.probability(1));
            for (int j = 2; j <= 20; j++) {
                System.out.print(", " + dist.probability(j));
            }
            System.out.println();
        }
    }

    /**
     * Gets a list of distributions in the tree in DFS order.
     * @param node current node of recursion
     * @param list container list of distributions
     */
    private void getListOfDistributions(MapNode node, ArrayList<DirichletProcessDistribution> list) {
        MapNode value;
        ArrayList<Integer> removeList = new ArrayList<Integer>();
        for (Entry<Integer, MapNode> entry : node.entrySet()) {
            value = entry.getValue();
            if (value.distribution.isEmpty()) {
                removeList.add(entry.getKey());
            } else {
                getListOfDistributions(value, list);
            }
        }

        for (Integer key : removeList) {
            node.remove(key);
        }

        list.add(node.distribution);
    }

    /**
     * Gets the concentration associated with the give depth
     * @param depth depth for needed concentration
     * @return concentration
     */
    private MutableDouble getConcentration(int depth) {
        assert depth >= 0;
        if (depth > (concentrations.length - 1)) {
            return concentrations[concentrations.length - 1];
        } else {
            return concentrations[depth];
        }
    }

    /**
     * Gets the dirichlet process distribution associated with the context.
     * @param context context
     * @return dirichlet process distribution
     */
    private DirichletProcessDistribution get(int[] context) {
        if (context == null || context.length == 0) {
            return root.distribution;
        } else {
            MapNode current = root;
            MapNode child = null;

            int childDepth = 1;
            for (int key : context) {
                child = current.get(key);
                if (child == null) {
                    child = new MapNode(new DirichletProcessDistribution(current.distribution, getConcentration(childDepth)));
                    current.put(key, child);
                }
                childDepth++;
                current = child;
            }
            return current.distribution;
        }
    }

    private DirichletProcessDistribution get(int context) {
        MapNode node = root.get(context);
        if (node == null) {
            node = new MapNode(new DirichletProcessDistribution(root.distribution, getConcentration(1)));
            root.put(context, node);
        }
        return node.distribution;
    }

    /**
     * Sample the concentration parameters using a Normal proposal combined with
     * the MH ratio.
     * @param proposalStandardDeviation proposal standard deviation
     */
    private void sampleConcentrations(double proposalStandardDeviation) {
        double[] concentrationProposals = new double[concentrations.length];
        double[] ratio = new double[concentrations.length];
        for (int i = 0; i < concentrations.length; i++) {
            concentrationProposals[i] = concentrations[i].value() + proposalStandardDeviation * rng.nextGaussian();
            if (concentrationProposals[i] <= 0d) {
                concentrationProposals[i] = concentrations[i].value();
            }

            ratio[i] = (concentrations[i].value() - concentrationProposals[i]) / concentrationPriorMean;
        }

        getLogMetropolisRatio(concentrationProposals, ratio, root, 0);

        double r;
        for (int i = 0; i < concentrations.length; i++) {
            r = Math.exp(ratio[i]);
            if (rng.nextDouble() < r) {
                concentrations[i].set(concentrationProposals[i]);
            }
        }
    }

    /**
     * Calculate the by depth log metropolis hastings ratio for sampling the
     * concentration values
     * @param concentrationProposals concentration proposals
     * @param ratio calculated by depth log ratios
     * @param node current node of recursion
     * @param depth depth of current node
     */
    private void getLogMetropolisRatio(double[] concentrationProposals, double[] ratio, MapNode node, int depth) {
        for (MapNode child : node.values()) {
            getLogMetropolisRatio(concentrationProposals, ratio, child, depth + 1);
        }

        double concentrationProposal;
        if (depth >= concentrationProposals.length) {
            concentrationProposal = concentrationProposals[concentrationProposals.length - 1];
        } else {
            concentrationProposal = concentrationProposals[depth];
        }

        ratio[depth] += node.distribution.logMetropolisRatio(concentrationProposal);
    }

    /**
     * Node class for tree.
     */
    private static class MapNode extends HashMap<Integer, MapNode> {

        public DirichletProcessDistribution distribution;

        public MapNode(DirichletProcessDistribution distribution) {
            this.distribution = distribution;
        }
    }

        /*
    public static void main(String[] args) throws IOException {
        HierarchicalDirichletProcess hdp = new HierarchicalDirichletProcess(new double[]{5d, 15d, 20d, 25d}, new UniformDistribution(4,1), 100d);
        int[] aiw = AIW.aiw();
        hdp.init(aiw, 3);

        for (int i = 0; i < 100; i++) {
            hdp.sample();
            System.out.print(hdp.score() + ", ");
            hdp.printConcentrations();
        }

        System.out.println();

        for (int i = 0; i < 100; i++) {
            hdp.sampleConcentrations(0.2d);
            System.out.print(hdp.score() + ", ");
            hdp.printConcentrations();
        }

        System.out.println();

        for (int i = 0; i < 100; i++) {
            hdp.sample();
            System.out.print(hdp.score() + ", ");
            hdp.printConcentrations();
        }
    }

    public void init(int[] s, int depth) {
        for (int i = 0; i < depth; i++) {
            int[] context = new int[i];
            for (int j = 0; j < i; j++) context[j] = s[(i - 1) - j];
            adjustCount(context, s[i], 1);
        }

        int[] context = new int[depth];
        for (int i = depth; i < s.length; i++) {
            for (int j = 0; j < depth;j++) context[j] = s[(i - 1) - j];
            adjustCount(context,s[i],1);
        }
    }
     */
}
