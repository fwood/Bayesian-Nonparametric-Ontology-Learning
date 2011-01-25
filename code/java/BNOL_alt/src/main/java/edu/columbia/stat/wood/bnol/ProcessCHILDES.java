/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.bnol;

import edu.columbia.stat.wood.bnol.util.MutableInt;
import edu.columbia.stat.wood.bnol.util.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author nicholasbartlett
 */
public class ProcessCHILDES {

    private File data;
    private HashMap<String, Integer> encoder = new HashMap();
    private HashMap<Integer,String> dictionary = new HashMap();
    private int size;

    public ProcessCHILDES(File data) throws IOException{
        this.data = data;
        initialize();
    }

    public int size(){
        return size;
    }

    public HashMap<Integer, String> dictionary(){
        return dictionary;
    }

    public int[] get(int length) throws IOException{
        if(length > size){
            length = size;
        }

        int[] d = new int[length];

        CHILDESIterator iter = new CHILDESIterator();
        for(int i = 0; i < length; i++){
            d[i] = iter.next();
        }

        iter.close();
        
        return d;
    }

    private void initialize() throws IOException{
        BufferedReader br = null;
        size = 0;

        try {
            br = new BufferedReader(new FileReader(data));

            String line;
            while ((line = br.readLine()) != null){
                String[] words = line.trim().split("[\\s]+");
                for(String word : words){
                    size++;
                    word = word.toLowerCase();
                    if(encoder.get(word) == null){
                        dictionary.put(encoder.size(), word);
                        encoder.put(word, encoder.size());
                    }
                }
            }
        } finally {
            if (br != null){
                br.close();
            }
        }
    }

    private class CHILDESIterator {
        private String[] words;
        private int index = 0;
        private BufferedReader br;
        private HashMap<String, Integer> enc = encoder;
        
        public CHILDESIterator() throws FileNotFoundException, IOException{
            br = new BufferedReader(new FileReader(data));
            words = br.readLine().split("[\\s]+");
        }

        public boolean hasNext(){
            return index < words.length;
        }

        public int next() throws IOException{
            int next = enc.get(words[index++].toLowerCase());

            while(index == words.length){
                String line = br.readLine();
                if(line == null){
                    return next;
                } else {
                    words = line.trim().split("[\\s]+");
                    index = 0;
                }
            }
            return next;
        }

        public void close() throws IOException{
            br.close();
        }
    }

    public static void main(String[] args) throws IOException{
        File data = new File("/Users/nicholasbartlett/Documents/np_bayes/Bayesian_Nonparametric_Ontology_Learning/data/_CHILDES.parsed.txt");
        ProcessCHILDES pc = new ProcessCHILDES(data);
        //int[] d = pc.get(10);
        //System.out.println(Arrays.toString(d));
        System.out.println(pc.size());
        System.out.println(pc.dictionary().size());
        /*for(int word : d){
            System.out.println(pc.dictionary().get(word));
        }*/

        File f = new File("/Users/nicholasbartlett/Documents/np_bayes/Bayesian_Nonparametric_Ontology_Learning/data/token_counts.txt");

        BufferedReader br = null;
        HashMap<String, MutableInt> countMap = new HashMap();
        try{
            br = new BufferedReader(new FileReader(f));
            String line;
            while((line = br.readLine()) != null){
                line = line.trim();
                line = line.toLowerCase();
                String[] words = line.split("[\\s]+");
               
                String key = words[1];
                int count = Integer.valueOf(words[0]).intValue();

                MutableInt value = countMap.get(key);
                if(value == null){
                    countMap.put(key, new MutableInt(count));
                } else {
                    value.plusEquals(count);
                }
            }
        } finally {
            if (br != null) br.close();
        }

        for(String key : countMap.keySet()){
            Integer remove = pc.encoder.remove(key);
            if(remove == null){
                System.out.println(key);
            }
        }

        System.out.println("---------------------------------------------------");

        for(String key : pc.encoder.keySet()){
            System.out.println(key);
        }

        System.out.println(countMap.size());
    }
}
