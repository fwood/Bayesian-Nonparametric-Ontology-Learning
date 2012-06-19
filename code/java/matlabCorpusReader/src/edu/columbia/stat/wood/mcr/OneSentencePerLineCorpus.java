/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.mcr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.lang.Integer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author fwood
 */
public class OneSentencePerLineCorpus {

    HashMap<String, Integer> dictionary = new HashMap<String, Integer>(20000);
    HashMap<Integer, String> inverseDictionary = new HashMap<Integer, String>(20000);
    HashMap<Integer, HashSet<TypePair>> firstToPair = new HashMap<Integer, HashSet<TypePair>>(25000);
    HashMap<Integer, Integer> typeCount = new HashMap<Integer, Integer>(25000);
    HashMap<Integer, HashSet<TypePair>> secondToPair = new HashMap<Integer, HashSet<TypePair>>(25000);
    HashMap<TypePair, Integer> pairCount = new HashMap<TypePair, Integer>(25000);
    HashSet<TypePair> allTypePairs = new HashSet<TypePair>(1000000);
    List<Entry<Integer, Integer>> sortedTypeCount = null;
    HashMap<Integer, Integer> mostCommonTypes = new HashMap<Integer, Integer>(300);
    int numMostCommonTypes = 300;

    public OneSentencePerLineCorpus(String fileName) throws Exception {
        LineNumberReader lr = new LineNumberReader(new FileReader(new File(fileName)));

        int currentTypeIndex = 0;
        int lineNumber = 0;
        String line = null;

        while ((line = lr.readLine()) != null) {
            // System.out.println("line: "+lineNumber);
            String[] result = line.split("[\\s]+");
            for (int x = 0; x < result.length - 1; x++) {
                Integer typefirst = dictionary.get(result[x]);
                Integer typesecond = dictionary.get(result[x + 1]);
                if (typefirst == null) {
                    typefirst = currentTypeIndex++;
                    dictionary.put(result[x], typefirst);
                    inverseDictionary.put(typefirst, result[x]);
                    typeCount.put(typefirst, 0);
                    firstToPair.put(typefirst, new HashSet<TypePair>());
                }
                if (typesecond == null) {
                    typesecond = currentTypeIndex++;
                    dictionary.put(result[x + 1], typesecond);
                    inverseDictionary.put(typesecond, result[x + 1]);
                    typeCount.put(typesecond, 0);
                    secondToPair.put(typesecond, new HashSet<TypePair>());
                }
                TypePair tp = new TypePair(typefirst, typesecond);
                if (!allTypePairs.contains(tp)) {
                    allTypePairs.add(tp);
                    HashSet<TypePair> hstp = firstToPair.get(typefirst);
                    if (hstp == null) {
                        hstp = new HashSet<TypePair>();
                        firstToPair.put(typefirst, hstp);
                    }
                    hstp.add(tp);
                    hstp = secondToPair.get(typesecond);
                    if (hstp == null) {
                        hstp = new HashSet<TypePair>();
                        secondToPair.put(typesecond, hstp);
                    }
                    hstp.add(tp);
                    pairCount.put(tp, 0);
                }
                int currentCount = pairCount.get(tp);
                pairCount.put(tp, currentCount + 1);
                int currentTypeCount = typeCount.get(typefirst);
                typeCount.put(typefirst, currentTypeCount + 1);
                if (x == (result.length - 2)) {
                    currentTypeCount = typeCount.get(typesecond);
                    typeCount.put(typesecond, currentTypeCount + 1);
                }
            }
            lineNumber++;
        }

        sortedTypeCount = MapUtilities.sortByValue(typeCount);

        int i = 0;
        for (Entry<Integer, Integer> e : sortedTypeCount) {
            if (i < numMostCommonTypes) {
                mostCommonTypes.put(e.getKey(), i++);
            } else {
                break;
            }
        }

    }

    public double[] singleEmbedding(int typeOfInterest) {
        double[] embedding = new double[mostCommonTypes.size() * 2];

        Set<TypePair> stp = firstToPair.get(typeOfInterest);
        double ftotal = 0.0;
        if (stp != null) {
            for (TypePair tp : stp) {
                int first = tp.getFirst();
                int second = tp.getSecond();
                int count = pairCount.get(tp);
                assert first == typeOfInterest : "first should be same as type of interest";
                Integer ind = 0;
                if ((ind = mostCommonTypes.get(second)) != null) {
                    ftotal += count;
                    embedding[ind] += count;
                }
            }
        }

        double stotal = 0.0;
        stp = secondToPair.get(typeOfInterest);
        if (stp != null) {

            for (TypePair tp : secondToPair.get(typeOfInterest)) {
                int first = tp.getFirst();
                int second = tp.getSecond();
                int count = pairCount.get(tp);
                assert second == typeOfInterest : "second should be same as type of interest";

                Integer ind = 0;
                if ((ind = mostCommonTypes.get(first)) != null) {
                    stotal += count;
                    embedding[ind + numMostCommonTypes] += count;
                }
            }
        }
        if (ftotal != 0.0) {
            double colsum = 0.0;
            for (int c = 0; c < (mostCommonTypes.size()); c++) {
                colsum += embedding[c];
            }
                        assert colsum == ftotal : "totals should match";

            for (int c = 0; c < (mostCommonTypes.size()); c++) {
                embedding[c] /= (colsum * 2);
            }
        }
        if (stotal != 0.0) {
            double colsum = 0.0;

            for (int c = mostCommonTypes.size(); c < (mostCommonTypes.size() * 2); c++) {
                colsum += embedding[c];
            }
                        assert colsum == stotal : "totals should match";

            for (int c = mostCommonTypes.size(); c < (mostCommonTypes.size() * 2); c++) {
                embedding[c] /= (colsum * 2);
            }
        }

        if((stotal+ftotal)==0.0) 
            System.out.println("shit two");

        return embedding;

    }

    public double[][] embeddings() {
        double[][] embedding = new double[typeCount.size()][mostCommonTypes.size() * 2];

        for (Entry<TypePair, Integer> e : pairCount.entrySet()) {
            int first = e.getKey().getFirst();
            int second = e.getKey().getSecond();
            int count = e.getValue().intValue();

            Integer ind = 0;
            if ((ind = mostCommonTypes.get(second)) != null) {
                embedding[first][ind] += count;
            }
            ind = 0;
            if ((ind = mostCommonTypes.get(first)) != null) {
                embedding[second][ind * 2] += count;
            }

        }

        for (int r = 0; r < typeCount.size(); r++) {
            double colsum = 0.0;
            for (int c = 0; c < (mostCommonTypes.size()); c++) {
                colsum += embedding[r][c];
            }
            for (int c = 0; c < (mostCommonTypes.size()); c++) {
                embedding[r][c] /= (colsum * 2);
            }
            colsum = 0.0;
            for (int c = mostCommonTypes.size(); c < (mostCommonTypes.size() * 2); c++) {
                colsum += embedding[r][c];
            }
            for (int c = mostCommonTypes.size(); c < (mostCommonTypes.size() * 2); c++) {
                embedding[r][c] /= (colsum * 2);
            }
        }

        return embedding;

    }

    public static void main(String[] args) throws Exception {
        OneSentencePerLineCorpus corpus = new OneSentencePerLineCorpus("/Users/fwood/Projects/Bayesian-Nonparametric-Ontology-Learning/data/_CHILDES.parsed.txt");
        for (Entry<Integer, Integer> e : corpus.sortedTypeCount) {
            System.out.println(corpus.inverseDictionary.get(e.getKey()) + " " + e.getValue());
        }

        PrintStream ps = new PrintStream(new FileOutputStream(new File("/Users/fwood/Projects/Bayesian-Nonparametric-Ontology-Learning/data/chater_features.csv")));


        int i = 0;
        for (Entry<Integer, Integer> e : corpus.sortedTypeCount) {
            int r = e.getKey();
            int count = e.getValue();
            ps.print(corpus.inverseDictionary.get(r) + ", ");

            double[] features = corpus.singleEmbedding(r);
            for (int c = 0; c < features.length; c++) {
                ps.format("%.3f, ", new Double(features[c]));
            }
            ps.print("\n");
        }



       /* for (int r = 0; r < corpus.dictionary.size(); r++) {
            ps.print(corpus.inverseDictionary.get(r) + ", ");
            if (r == 10) {
                System.out.println("shit");
            }
            double[] features = corpus.singleEmbedding(r);
            for (int c = 0; c < features.length; c++) {
                ps.format("%.3f, ", new Double(features[c]));
            }
            ps.print("\n");
        } */
        ps.flush();
        ps.close();

    }
}
