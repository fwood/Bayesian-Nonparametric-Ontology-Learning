/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.mcr;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author fwood
 */
public class Embeddings {
    
    //DirectedSparseGraph<Integer, String> tree = new DirectedSparseGraph<Integer, String>();

    HashMap<String,boolean[]> embeddings = new HashMap<String,boolean[]>(10000);
    HashMap<String,double[]> featureMap = new HashMap<String,double[]>(10000);
    ArrayList<String> featureWord = new ArrayList<String>(30000);
    double[][] features;

    public Set<String> words() {return embeddings.keySet();}
    public boolean[] embedding(String word) {return embeddings.get(word);}


    public Embeddings(String embeddingsFileName, String featuresFilename) throws Exception {
        LineNumberReader lr = new LineNumberReader(new FileReader(new File(embeddingsFileName)));
        // read in the embeddings (binary tree embeddings)
        String line = null;

        while ((line = lr.readLine()) != null) {
            // System.out.println("line: "+lineNumber);
            String[] result = line.split("[\\s|,]+");
            boolean[] path = new boolean[result.length-1]; // lazy and buggy
            String word = result[0];
            String pathstr = ":";
            for (int x = 1; x < result.length; x++) {
                path[x-1] = Boolean.valueOf(result[x].equals("1"));
                pathstr = pathstr+(path[x-1]?"1,":"0,");
            }
            System.out.println(word+pathstr+"\n");
            embeddings.put(word, path);
        }
        lr.close();

        // read in the features for the whole vocabulary
        lr = new LineNumberReader(new FileReader(new File(featuresFilename)));
        line = null;

        while ((line = lr.readLine()) != null) {
            // System.out.println("line: "+lineNumber);
            String[] result = line.split("[\\s,]+");
            double[] feature = new double[result.length-1];
            String word = result[0];
            this.featureWord.add(word);
            for (int x = 1; x < result.length; x++) {
                feature[x-1] = Double.valueOf(result[x]);
            }
            featureMap.put(word, feature);
        }
        lr.close();

        features = new double[featureWord.size()][];

        for(int w = 0; w<featureWord.size(); w++) {
            double[] cimb = featureMap.get(featureWord.get(w));
            features[w] = cimb;
            //System.arraycopy(features[w], 0, cimb , 0, cimb.length);
        }
        
        for(int w = 0; w<featureWord.size(); w++) {
            boolean[] path = embeddings.get(featureWord.get(w));
            if(path != null)  // if the embedding has been computed, we're golden
                continue;
            
            // otherwise, go off and find the closest word in feature space
            // and use that embedding for the unknown word

            String minFeatureDistanceWord = "";
            double minDistance = Double.MAX_VALUE;
            // go through the known embeddings
            for(String ww : embeddings.keySet()) {
                // get the feature vector of the already embedded word
                double[] featureVectorOfEmbeddedWord = featureMap.get(ww);
                double[] featureVectorOfWordForWhichWeDontKnowTheEmbedding = featureMap.get(featureWord.get(w));
                
                // compute distance between features and check to see if it is minimum
                
                double distww = 0;
                
                assert featureVectorOfEmbeddedWord.length == featureVectorOfWordForWhichWeDontKnowTheEmbedding.length : "Two feature vector lengths don't match";
                for (int j = 0; j < featureVectorOfEmbeddedWord.length;j++) {
                    distww+= featureVectorOfEmbeddedWord[j]*featureVectorOfWordForWhichWeDontKnowTheEmbedding[j];
                }
                
                if(distww < minDistance) {
                    minDistance = distww;
                    minFeatureDistanceWord = ww;
                }
            }
            
            // use the embedding for the unknown type from the type that most closely matches in feature space
            embeddings.put(featureWord.get(w), embeddings.get(minFeatureDistanceWord));
            
        }




    }

    public static void main(String[] args) throws Exception {
        Embeddings embeddings = new Embeddings("/Users/fwood/Projects/Bayesian-Nonparametric-Ontology-Learning/data/chater_embeddings.csv","/Users/fwood/Projects/Bayesian-Nonparametric-Ontology-Learning/data/chater_features.csv");

        // some test stuff

        int numWordsWithEmbeddings = 0;
        for(String word : embeddings.words()) {
                    String output = word+": ";
                    numWordsWithEmbeddings++;
            boolean[] embedding = embeddings.embedding(word);
            for(int j = 0; j<embedding.length; j++) {
                output = output + (embedding[j]?"0,":"1,");

            }
            System.out.println(output);
        }
        System.out.println("Num words with embeddings : "+numWordsWithEmbeddings);
        assert embeddings.words().size() == embeddings.featureWord.size() : "Some embeddings not computed";

    }
}
