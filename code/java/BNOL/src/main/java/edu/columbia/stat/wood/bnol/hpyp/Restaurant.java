/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol.hpyp;

import edu.columbia.stat.wood.bnol.util.MersenneTwisterFast;
import edu.columbia.stat.wood.bnol.util.MutableDouble;
import edu.columbia.stat.wood.bnol.util.Pair;
import edu.columbia.stat.wood.bnol.util.SampleWithoutReplacement;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Arrays;

/**
 * Restaurant object for a HPYP model.
 * @author nicholasbartlett
 */

public class Restaurant extends TIntObjectHashMap<Restaurant> {
    
    private int customers, tables;
    private Restaurant parent;
    private MutableDouble concentration, discount;
    private TIntObjectHashMap<TSA> tableArrangements;

    /***********************constructor methods********************************/

    /**
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

    /***********************public methods*************************************/
    
    /**
     * Indicates if there are any customers in the restaurant.
     * @return true if no customers in restaurant, else false
     */
    public boolean isEmptyRestaurant(){
        return customers == 0;
    }

    /**
     * Calculates the probability of given type in the restaurant
     * @param type type to consider
     * @return probability
     */
    public double probability(int type){
        double parentProbability = parent.probability(type);
        
        if(customers == 0){
            return parentProbability;
        } else {
            TSA tsa = tableArrangements.get(type);
            double customersOfType = 0.0, tablesOfType = 0.0, d = discount.value(), c = concentration.value();
            
            if(tsa != null){
                customersOfType = tsa.customers;
                tablesOfType = tsa.tables;
            }
            
            return (customersOfType - d * tablesOfType + parentProbability * (d * (double) tables + c)) / ((double) customers + c);
        }
    }
    
    /**
     * Seats token of given type in restaurant.
     * @param type type of token to seat
     * @param rng random number generator
     */
    public void seat(int type, MersenneTwisterFast rng){
        double parentProbability = parent.probability(type);

        TSA tsa = tableArrangements.get(type);
        if(tsa == null){
            tsa = new TSA();
            tableArrangements.put(type, tsa);
        }

        if(tsa.seat(parentProbability, concentration.value(), discount.value(), tables, rng)){
            tables++;
            parent.seat(type, rng);
        }
        customers++;
    }

    /**
     * Un-seats token of given type in restaurant
     * @param type type of token to unseat
     * @param rng random number generator
     */
    public void unseat(int type, MersenneTwisterFast rng){
        TSA tsa = tableArrangements.get(type);

        if(tsa.unseat(rng)){
            tables--;
            parent.unseat(type,rng);
        }
        
        customers--;
    }

    /**
     * Draw a random sample from this restaurant and seat the customer in the
     * restaurant.
     * @param low low edge of slice
     * @param high high edge of slice
     * @param keyOrder type ordering for slice sampling
     * @param rng random number generator
     * @return random sample drawn
     */
    public int draw(double low, double high, int[] keyOrder, MersenneTwisterFast rng){
        double randomNumber, cuSum = 0.0;
        
        if(keyOrder !=  null){
            randomNumber = rng.nextDouble() * (high - low) + low;
            for(int i = 0; i < keyOrder.length; i++){
                cuSum += probability(keyOrder[i]);
                if(cuSum > randomNumber){
                    seat(keyOrder[i], rng);
                    return keyOrder[i];
                }
            }
            throw new RuntimeException("should not get to here");
        } else {
            TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
            randomNumber = rng.nextDouble();
            while (iterator.hasNext()) {
                iterator.advance();
                cuSum = iterator.value().draw(cuSum, randomNumber, discount.value(), concentration.value(), customers);
                if (cuSum > randomNumber) {
                    customers++;
                    return iterator.key();
                }
            }
        }

        int draw = parent.draw(low, high, keyOrder, rng);
        TSA tsa = tableArrangements.get(draw);
        if(tsa != null){
            tsa.addNewTable();
        }else {
            tsa = new TSA();
            tsa.addNewTable();
            tableArrangements.put(draw, tsa);
        }

        customers++;
        tables++;

        return draw;
    }

    /**
     * Generates a random sample from this restaurant, but does not seat the
     * customer.
     * @param low low edge of slice
     * @param high high edge of slice
     * @param keyOrder type ordering for slice sampling
     * @param rng random number generator
     * @return random type generated
     */
    public int generate(double low, double high, int[] keyOrder, MersenneTwisterFast rng){
        double randomNumber, cuSum = 0.0;

        if(keyOrder !=  null){
            randomNumber = rng.nextDouble() * (high - low) + low;
            for(int i = 0; i < keyOrder.length; i++){
                cuSum += probability(keyOrder[i]);
                if(cuSum > randomNumber){
                    return keyOrder[i];
                }
            }
            throw new RuntimeException("should not get to here");
        } else {
            randomNumber = rng.nextDouble();
            TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
            while (iterator.hasNext()) {
                iterator.advance();
                cuSum = iterator.value().generate(cuSum, randomNumber, discount.value(), concentration.value(), customers);
                if (cuSum > randomNumber) {
                    return iterator.key();
                }
            }
        }

        return parent.generate(low, high, keyOrder, rng);
    }

    /**
     * Sample the seating arrangements
     */
    public void sampleSeatingArrangements(MersenneTwisterFast rng){

        Pair<int[], int[]> randomCustomerOrder = randomCustomerOrder(rng);

        if(randomCustomerOrder == null){
            return;
        }

        int[] typeOrder = randomCustomerOrder.first();
        int[] tableOrder = randomCustomerOrder.second();

        TSA tsa;
        double parentProbability;
        int type;

        for(int i = 0; i < typeOrder.length; i++){
            type = typeOrder[i];

            tsa = tableArrangements.get(type);

            if(tsa.unseat(tableOrder[i])){
                tables--;
                parent.unseat(type, rng);
            }
            customers--;

            parentProbability = parent.probability(type);

            if(tsa.seat(parentProbability, concentration.value(), discount.value(), this.tables, rng)){
                tables++;
                parent.seat(type, rng);
            }
            customers++;
        }

        assert checkCounts();
        removeZeros();
    }

    /**
     * Gets the log likelihood contribution from this restaurant.
     * @return log likelihood of this restaurant
     */
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

    /**
     * Calls the remove zeros method for each TSA object in the map.
     */
    public void removeZeros(){
        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            iterator.value().removeZeros();
        }
    }

    /**
     * Checks that the counts for the individual TSA objects add up totals
     * recorded in the object.  Also calls check counts for each TSA object.
     * @return true if internal counts are consistent, else false
     */
    public boolean checkCounts(){
        int c = 0, t = 0;
        
        TIntObjectIterator<Restaurant> iterator = this.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            c += iterator.value().tables;
        }

        if(!isEmpty()){
            assert customers == c;
        }

        c = 0;

        for(TSA tsa : tableArrangements.valueCollection()){
            tsa.checkCounts();
            c += tsa.customers;
            t += tsa.tables;
        }

        assert c == customers : "customer count incorrect : c = " + c + " : customers = " + customers;
        assert t == tables : "table count incorrect : t = " + t + " : tables = " + tables;

        return c == customers && t == tables;
    }

    /**
     * Shows a string representation of the restaurant.
     * @return string representation of the restaurant for easy viewing
     */
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

    /***********************private methods************************************/

    /**
     * Gets the total number of customers that need sampling
     * @return number of customers which need sampling
     */
    private int customersToSample(){
        int customersToSample = 0, customersOfType;

        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while(iterator.hasNext()){
            iterator.advance();
            customersOfType = iterator.value().customers;

            if(customersOfType > 1){
                customersToSample += customersOfType;
            }
        }

        return customersToSample;
    }

    /**
     * Gets a pair of lists which indicate the type and table index of each
     * token to sample
     * @param rng random number generator
     * @return type and table order for sampling
     */
    private Pair<int[], int[]> randomCustomerOrder(MersenneTwisterFast rng) {
        int customersToSample = customersToSample();

        if (customersToSample == 0) {
            return null;
        }

        int[] randomOrder = SampleWithoutReplacement.sampleWithoutReplacement(customersToSample, rng);
        int[] typeOrder = new int[customersToSample], tableOrder = new int[customersToSample];

        
        int type, randomIndex, index = 0;
        int[] sa;
        TSA tsa;

        TIntObjectIterator<TSA> iterator = tableArrangements.iterator();
        while (iterator.hasNext()) {
            iterator.advance();

            type = iterator.key();
            tsa = iterator.value();
            
            if (tsa.customers > 1) {
                sa = tsa.sa;
                for (int table = 0; table < sa.length; table++) {
                    for (int cust = 0; cust < sa[table]; cust++) {
                        randomIndex = randomOrder[index++];
                        typeOrder[randomIndex] = type;
                        tableOrder[randomIndex] = table;
                    }
                }
            }
        }

        assert index == customersToSample : "index = " + index + " : customers to sample = " + customersToSample;

        return new Pair(typeOrder, tableOrder);
    }
    
    private static class TSA {

        public int customers, tables;
        public int[] sa;

        /***********************constructor methods****************************/
        public TSA(){
            customers = 0;
            tables = 0;
        }

        /***********************public methods*********************************/

        /**
         * Seats a customer of the type associated with this object and
         * indicates if a new table is formed, i.e. a customer will need to be
         * seated in the parent node.
         * @param parentProbability parent probability of the type
         * @param concentration concentration parameter
         * @param discount discount parameter
         * @param totalTables total number of tables in the node
         * @param rng random number generator
         * @return true if a new table is created, else false
         */
        public boolean seat(double parentProbability, double concentration, double discount, double totalTables, MersenneTwisterFast rng){
            boolean newTable;

            ifstatement:
            if(customers == 0){

                assert tables == 0 : "customers is zero, but tables is " + tables;

                if(sa != null && sa.length > 0){
                    sa[0] = 1;
                } else {
                    sa = new int[]{1};
                }
                
                newTable = true;
            } else {
                double totalWeight = (double) customers - discount * (double) tables + parentProbability * (discount * (double) totalTables + concentration);
                double randomNumber = rng.nextDouble();
                double cuSum = 0.0;
                int zeroIndex = -1;

                for(int table = 0; table < sa.length; table++){

                    if(sa[table] == 0){
                        zeroIndex = table;
                        continue;
                    }

                    cuSum += ((double) sa[table] - discount) / totalWeight;
                    if(cuSum > randomNumber){
                        sa[table]++;
                        newTable = false;
                        break ifstatement;
                    }
                }

                assert cuSum >= 0.0 && cuSum <= randomNumber : "cumulative sum is " + cuSum + " random number is " + randomNumber;

                newTable = true;
                
                if (zeroIndex > -1) {
                    sa[zeroIndex] = 1;
                } else {
                    int[] newsa = new int[sa.length + 1];
                    System.arraycopy(sa, 0, newsa, 0, sa.length);
                    newsa[sa.length] = 1;

                    sa = newsa;
                }
            }

            customers++;
            if(newTable){
                tables++;
            }
            return newTable;
        }

        /**
         * Un-seats a customer of the type associated with the TSA and indicates
         * if a table was deleted in the process.
         * @param rng random number generator
         * @return true of a table was deleted, else false.
         */
        public boolean unseat(MersenneTwisterFast rng){
            double randomNumber = rng.nextDouble();
            double cuSum = 0.0;
            boolean emptyTable = false;

            assert customers > 0 : "To unseat customers there must be customers to unseat";

            for(int table = 0; table < sa.length; table++){
                cuSum += (double) sa[table] / (double) customers;
                if(cuSum > randomNumber){
                    sa[table]--;

                    assert sa[table] >= 0 : "Table size must be >= 0";

                    if(sa[table] == 0){
                        emptyTable = true;
                    }
                    break;
                }
            }

            if(cuSum <= randomNumber){
                throw new RuntimeException("this should never happen, cuSum = " + cuSum + ", randomNumber = " + randomNumber);
            }
            
            customers--;
            if(emptyTable){
                tables--;
            }
            return emptyTable;
        }

        /**
         * Un-seats a customer sitting a given table index and indicates if the
         * table is empty after.  This method is used during sampling.
         * @param table index of table from which to un-seat a customer
         * @return true if the table is made empty, else false
         */
        public boolean unseat(int table){
            boolean emptyTable = false;
            
            sa[table]--;

            assert sa[table] >= 0 : "table size must be >= 0";

            if(sa[table] == 0){
                emptyTable = true;
                tables--;
            }
            customers--;

            return emptyTable;
        }

        /**
         * Draws a random sample from this object or not depending on the input.
         * If a sample is drawn a customer is seated.
         * @param cuSum current value of the cumulative sum
         * @param randomNumber random number for sample
         * @param discount discount parameter value
         * @param concentration concentration parameter value
         * @param totalCustomers total customers in the restaurant
         * @return cumulative sum after passing through this object
         */
        public double draw(double cuSum, double randomNumber, double discount, double concentration, double totalCustomers) {
            for (int i = 0; i < sa.length; i++) {
                if (sa[i] > 0) {
                    cuSum += ((double) sa[i] - discount) / (totalCustomers + concentration);
                    if (cuSum > randomNumber) {
                        sa[i]++;
                        customers++;
                        break;
                    }
                }
            }
            return cuSum;
        }

        /**
         * Generates a random sample from this object or not, but does not seat
         * the customer afterwards.
         * @param cuSum current value of the cumulative sum
         * @param randomNumber random number for sample
         * @param discount discount parameter value
         * @param concentration concentration parameter value
         * @param totalCustomers total customers in the restaurant
         * @return cumulative sum after passing through this object
         */
        public double generate(double cuSum, double randomNumber, double discount, double concentration, double totalCustomers){
            for (int i = 0; i < sa.length; i++) {
                if (sa[i] > 0) {
                    cuSum += ((double) sa[i] - discount) / (totalCustomers + concentration);
                    if (cuSum > randomNumber) {
                        break;
                    }
                }
            }
            return cuSum;
        }

        /**
         * Adds a new table to the arrangement.
         */
        public void addNewTable() {
            if (sa == null || sa.length == 0) {
                sa = new int[]{1};
                customers = 1;
                tables = 1;
                return;
            } else {
                int zeroIndex = -1;
                for (int i = 0; i < sa.length; i++) {
                    if (sa[i] == 0) {
                        zeroIndex = i;
                        break;
                    }
                }

                if (zeroIndex > -1) {
                    sa[zeroIndex] = 1;
                } else {
                    int[] newsa = new int[sa.length + 1];
                    System.arraycopy(sa, 0, newsa, 0, sa.length);
                    newsa[sa.length] = 1;
                    sa = newsa;
                }

                customers++;
                tables++;
            }
        }

        /**
         * Gets the log likelihood contribution of the table configuration for
         * the type associated with this object.
         * @param discount value of the discount parameter
         * @return score contribution
         */
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

        /**
         * Removes the zero entries from the seating arrangement vector sa.
         */
        public void removeZeros() {
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

        /**
         * Method to check that the customer and table counts are in agreement
         * with the internal vector.
         * @return true of agreement, else false
         */
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

            return c == customers && t == tables;
        }
    }
}
