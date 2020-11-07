package TermVector;

import Files.DBConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Represent a term's vector, according to GloVe.
 */
public class TermVector {
    public static Map<String, Double> termSize=new HashMap<>();
    private static Map<String, double[]> termVector=new HashMap<>();
    private static int dim=50;
    private static String path;

    /**
     * Building the vector for the first time.
     */
    public static void BuildVectors(){
        if(termSize.size()>0)
            return;
        if(path==null)
            UpdatePath();
        BufferedReader bw;
        String lineFromFile, term;
        double size;
        double[] values;
        //Queue<StringBuilder> lines = new LinkedList<>();
        try {
            bw = new BufferedReader(new FileReader(path));
            while ((lineFromFile = bw.readLine()) != null) {
                //lines.add(new StringBuilder(lineFromFile));
                StringBuilder line=new StringBuilder(lineFromFile);
                //line = lines.remove();
                term = GetTerm(line);
                values = ReadValues(line);
                size = 0;
                for (double value : values)
                    size += Math.pow(value, 2);
                size = Math.sqrt(size);
                termSize.put(term, size);
                termVector.put(term, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void UpdatePath(){
        final Class<?> referenceClass = DBConnection.class;
        final URL url =
                referenceClass.getProtectionDomain().getCodeSource().getLocation();
        String path = null;
        try {
            path = new File(url.toURI()).getParentFile().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        /**/
        path=path.substring(0, path.lastIndexOf('\\'));
        path=path.substring(0, path.lastIndexOf('\\'));
        /**/
        TermVector.path=path+"\\resources\\glove.6B."+dim+"d.txt";
    }

    /**
     * Get the first word in the line, meaning the term. Also, cut the term from the line.
     * @param line The line from the vectors file.
     * @return The term as string.
     */
    private static String GetTerm(StringBuilder line) {
        StringBuilder sb=new StringBuilder();
        int i;
        for (i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ')
                break;
            sb.append(line.charAt(i));
        }
        line.replace(0, i+1, "");
        return sb.toString().toLowerCase();
    }

    /**
     * Get the vector values.
     * @param line The line from the vector file, without the term.
     * @return List of values.
     */
    private static double[] ReadValues(StringBuilder line) {
        int i, j, k;
        double[] values=new double[dim];
        double value;
        for (i=0, j=0, k=0; i < line.length(); i++)
            if (line.charAt(i) == 32) {
                value=Double.parseDouble(line.substring(j, i));
                values[k]=value;
                j = i + 1;
                k++;
            }
        values[k]=Double.parseDouble(line.substring(j, i));
        return values;
    }

    /**
     * Get the rank of two terms as cosine similarity.
     * @param term1 The first term.
     * @param term2 The second term.
     * @return The rank of the terms, 0 if one of them is not in the model, 1 if they are equals.
     */
    public static double GetRank(String term1, String term2){
        if(term1.equals(term2))
            return 1;
        if(!termVector.containsKey(term1) || !termVector.containsKey(term2))
            return 0;
        double innerProduct=0;
        int vectorSize = 50;
        for(int i = 0; i< vectorSize; i++)
            innerProduct += termVector.get(term1)[i] * termVector.get(term2)[i];
        innerProduct=innerProduct/(termSize.get(term1)*termSize.get(term2));
        return innerProduct;
    }

    public static Set<String> GetTerms() {
        return termVector.keySet();
    }
}
