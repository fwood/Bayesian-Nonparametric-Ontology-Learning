/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.hpyp;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Arrays;

/**
 *
 * @author nicholasbartlett
 */

public class Restaurant extends Pair<Restaurant, Restaurant> {

    /**
     * total number of customers and tables in the restaurant
     */
    private int customers, tables;

    /**
     * parent of restaurant
     */
    private Restaurant parent;

    /**
     * concentration and discount parameters for this restaurant
     */
    private MutableDouble concentration, discount;

    /**
     * hashmap of table arrangements
     */
    private TIntObjectHashMap<TSA> tableArrangements;

    /**
     * Basic constructor
     *
     * @param parent restaurant parent
     * @param concentration concentration parameter
     * @param discount discount parameter
     */
    public Restaurant(Restaurant parent, MutableDouble concentration, MutableDouble discount){
        this.parent = parent;
        this.concentration = concentration;
        this.discount = discount;

        tableArrangements = new TIntObjectHashMap<TSA>();
        customers = 0;
        tables = 0;
    }

    /**
     * Null constructor
     */
    public Restaurant(){}

    public Restaurant get(byte key){
        if(key == 1){
            return this.second();
        } else {
            return this.first();
        }
    }

    public void put(byte key, Restaurant r){
        if(key == 1){
            this.setSecond(r);
        } else{
            this.setFirst(r);
        }
    }

    public boolean isEmpty(){
        return this.first() == null && this.second() == null;
    }

    public Restaurant[] values(){
        Restaurant r1 = null, r2 = null;
        if(r1 != null && r2 != null){
            return new Restaurant[]{r1,r2};
        } else {
            if(r1 != null){
                return new Restaurant[]{r1};
            } else {
                if(r2 != null){
                    return new Restaurant[]{r2};
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Gets the total number of customers
     *
     * @return total number of customers in restaurant
     */
    public int customers(){return customers;}

    /**
     * Calculates the probability of given type in the restaurant
     *
     * @param type type to consider
     * @return probability
     */
    public double probability(int type){
        double pp = parent.probability(type);
        
        if(customers == 0){
            return pp;
        } else {
            TSA tsa = tableArrangements.get(type);
            double tc = 0, tt = 0, d = discount.value(), c = concentration.value();
            
            if(tsa != null){
                tc = tsa.customers;
                tt = tsa.tables;
            }
            
            return (tc - d * tt + pp * (d * (double) tables + c)) / ((double) customers + c);
        }
    }

    /**
     * Seats token of given type in restaurant
     *
     * @param type type of token to seat
     */
    public void seat(int type, MersenneTwisterFast rng){
        double pp = parent.probability(type);

        TSA tsa = tableArrangements.get(type);
        if(tsa == null){
            tsa = new TSA();
            tableArrangements.put(type, tsa);
        }

        if(tsa.seat(pp, concentration.value(), discount.value(), tables,rng)){
            tables++;
            parent.seat(type,rng);
        }
        customers++;
    }

    /**
     * Unseats token of given type in restaurant
     *
     * @param type type of token to unseat
     */
    public void unseat(int type,MersenneTwisterFast rng){
        TSA tsa = tableArrangements.get(type);

        assert tsa != null : "tsa should not be null if trying to unseat someone";

        if(tsa.unseat(rng)){
            tables--;
            parent.unseat(type,rng);
        }
        
        customers--;
    }

    /*
     * Make a draw from this restaurant, updating the appropriate counts
     */
    public int draw(MersenneTwisterFast rng){
        double r = rng.nextDouble();
        double cuSum = 0.0;
        
        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            cuSum = iterator.value().draw(cuSum, r, discount.value(), concentration.value(), customers);
            if(cuSum > r){
                customers++;
                return iterator.key();
            }
        }
        
        int draw = parent.draw(rng);
        TSA tsa = tableArrangements.get(draw);
        if(tsa != null){
            tsa.addNewTable();
        }else {
            tsa = new TSA();
            tsa.customers = 1;
            tsa.tables = 1;
            tsa.sa = new int[]{1};
            tableArrangements.put(draw, tsa);
        }

        customers++;
        tables++;

        return draw;
    }

    /**
     * Gets the total number of customers that need sampling
     *
     * @return number of customers which need sampling
     */
    private int customersToSample(){
        int customersToSample = 0, c;

        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while(iterator.hasNext()){

            iterator.advance();
            c = iterator.value().customers;

            if(c > 1){
                customersToSample += c;
            }
        }

        return customersToSample;
    }

    /**
     * Gets a pair of lists which indicate the type and table index of each
     * token to sample
     *
     * @return pair of arrays
     */
    private Pair<int[], int[]> randomCustomerOrder(MersenneTwisterFast rng) {
        int customersToSample = customersToSample();

        if (customersToSample == 0) {
            return null;
        }

        int[] randomOrder = SampleWithoutReplacement.sampleWithoutReplacement(customersToSample, rng);
        int[] types = new int[customersToSample];
        int[] tables = new int[customersToSample];

        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        int type;
        int randomIndex;
        int index = 0;
        int[] sa;
        TSA tsa;
        
        while (iterator.hasNext()) {
            iterator.advance();

            type = iterator.key();
            tsa = iterator.value();
            if (tsa.customers > 1) {
                sa = tsa.sa;

                for (int table = 0; table < sa.length; table++) {
                    for (int cust = 0; cust < sa[table]; cust++) {
                        randomIndex = randomOrder[index++];
                        types[randomIndex] = type;
                        tables[randomIndex] = table;

                    }
                }
            }
        }

        assert index == customersToSample : "index = " + index + " : customers to sample = " + customersToSample;

        return new Pair(types, tables);
    }

    /**
     * Sample the seating arrangements
     */
    public void sampleSeatingArrangements(MersenneTwisterFast rng){

        Pair<int[], int[]> randomCustomerOrder = randomCustomerOrder(rng);

        if(randomCustomerOrder == null){
            return;
        }
        
        int[] types = randomCustomerOrder.first();
        int[] tables = randomCustomerOrder.second();

        TSA tsa;
        double pp;
        int type;

        for(int i = 0; i < types.length; i++){
            type = types[i];

            tsa = tableArrangements.get(type);

            if(tsa.unseat(tables[i])){
                this.tables--;
                parent.unseat(type,rng);
            }
            customers--;

            pp = parent.probability(type);

            if(tsa.seat(pp, concentration.value(), discount.value(), this.tables,rng)){
                this.tables++;
                parent.seat(type,rng);
            }
            customers++;
        }
    }

    public double score(){
        double score = 0.0, d = discount.value(), c = concentration.value();

        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            score += iterator.value().score(d);
        }

        for(int table = 1; table < tables; table++){
            score += Math.log((double) table * d + c);
        }

        for(int customer = 1; customer < customers; customer++){
            score -= Math.log((double) customer + c);
        }
        
        return score;
    }

    public boolean checkCounts(){
        int c = 0, t = 0;

        for(TSA tsa : tableArrangements.valueCollection()){
            tsa.checkCounts();
            c += tsa.customers;
            t += tsa.tables;
        }

        assert c == customers : "customer count incorrect : c = " + c + " : customers = " + customers;
        assert t == tables : "table count incorrect : t = " + t + " : tables = " + tables;

        return true;
    }

    public void removeZeros(){
        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            iterator.value().removeZeros();
        }
    }

    @Override
    public String toString() {
        String toStr = "Concentration: " + concentration.value() + "\n" +
                       "Discount: " + discount.value() + "\n";
        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            if (iterator.value().customers != 0) {
                toStr = toStr + iterator.key() + "->" + Arrays.toString(iterator.value().sa) + "\n";
            }
        }
        return toStr;
    }

    private static class TSA {
        public int customers, tables;
        public int[] sa;

        public TSA(){
            customers = 0;
            tables = 0;
        }

        public boolean seat(double pp, double concentration, double discount, double totalTables, MersenneTwisterFast rng){
            if(customers == 0){
                if(sa != null){
                    sa[0] = 1;
                } else {
                    sa = new int[]{1};
                }
                customers = 1;
                tables = 1;
                return true;
            } else {
                double tw = (double) customers - discount * (double) tables + pp * (discount * totalTables + concentration);
                double r = rng.nextDouble();
                double cuSum = 0.0;
                int zeroIndex = -1;

                for(int table = 0; table < sa.length; table++){

                    if(sa[table] == 0){
                        zeroIndex = table;
                    }

                    cuSum += ((double) sa[table] - discount) / tw;
                    if(cuSum > r){
                        sa[table]++;
                        customers++;
                        return false;
                    }
                }

                if(cuSum <= r){

                    if(zeroIndex > -1){
                        sa[zeroIndex] = 1;
                    } else {
                        int[] newsa = new int[sa.length + 1];
                        System.arraycopy(sa,0,newsa, 0, sa.length);
                        newsa[sa.length] = 1;

                        sa = newsa;
                    }

                    customers++;
                    tables++;
                    return true;
                }
            }
            throw new RuntimeException("Should not make it to here.");
        }

        public boolean unseat(MersenneTwisterFast rng){
            double tw = customers;
            double r = rng.nextDouble();
            double cuSum = 0.0;

            assert customers > 0 : "To unseat customers there must be customers to unseat";

            for(int table = 0; table < sa.length; table++){
                cuSum += ((double) sa[table]) / tw;
                if(cuSum > r){
                    sa[table]--;

                    assert sa[table] >= 0 : "Table size must be >= 0";

                    customers--;
                    if(sa[table] == 0){
                        tables--;
                        return true;
                    } else {
                        return false;
                    }
                }
            }

            throw new RuntimeException("Should never get to this point");
        }

        public boolean unseat(int table){
            sa[table]--;
            customers--;

            assert sa[table] >= 0 : "table size must be >= 0";

            if(sa[table] == 0){
                tables--;
                return true;
            } else {
                return false;
            }
        }

        public double draw(double cuSum, double r, double discount, double concentration, double totalCustomers){
            for(int i = 0; i < sa.length; i++){
                cuSum += ((double) sa[i] - discount) / (totalCustomers + concentration);
                if(cuSum > r){
                    sa[i]++;
                    customers++;
                    break;
                }
            }
            return cuSum;
        }

        public void addNewTable(){
            int[] newsa = new int[sa.length + 1];
            System.arraycopy(sa,0,newsa,0,sa.length);
            newsa[sa.length] = 1;
            sa = newsa;
            customers++;
            tables++;
        }

        public double score(double discount){
            double score = 0.0;
            
            for(int table : sa){
                if(table > 0){
                    for(int customer = 1; customer < table; customer++){
                        score += Math.log((double) customer - discount);
                    }
                }
            }
            
            return score;
        }

        public boolean checkCounts(){
            int c = 0, t = 0;
            for(int table : sa){
                c += table;
                if(table > 0){
                    t++;
                }
            }
            
            assert c == customers : "customer count is not correct : c =  " + c + " : customers = " + customers ;
            assert t == tables : "table count is not correct : t = " + t + " : tables = " + tables;
            
            return true;
        }

        public void removeZeros() {
            assert checkCounts();

            if (sa.length != tables) {

                int[] newsa = new int[tables];
                int t = 0;
                for (int table : sa) {
                    if (table > 0) {
                        newsa[t++] = table;
                    }
                }

                sa = newsa;
            }
        }
    }
}
