package Ranker;

import General.Document;
import General.Term;
import Index.DocumentIndexer;
import Process.*;
import java.util.*;

/**
 * Gives a rank to a document according to a query.
 * The ranking can be made with semantic context, or not.
 */
public class Ranker {
    private double b=0.25, k=0.2;
    /**
     * Rank the documents according to the terms in the query.
     * @param termsAndDocs Mapping document to map of term to tf.
     * @return List of documents sorted by their rank.
     */
    public List<Document> Rank(Map<Document, Map<Term, Integer>> termsAndDocs, Map<Term, Integer> query){
        List<Document> rankedDocuments=new ArrayList<>();
        Map<Document, Double> ranks=new HashMap<>();
        double rank, w;
        Term term;

        /**BM25**/
        for(Map.Entry entry: termsAndDocs.entrySet()) {
            Document document = (Document) entry.getKey();
            rank=0;
            for(Map.Entry termTF : termsAndDocs.get(document).entrySet()){
                term=(Term)termTF.getKey();
                if(DataProcessor.original.contains(term.getName()))
                    w=0.6;
                else w=0.4;
                rank+=w*bm25(term.getDf(), (Integer) termTF.getValue(), document.getMaxTf());
            }
            if(ranks.containsKey(document))
                ranks.put(document, ranks.get(document)+rank);
            else ranks.put(document, rank);
        }

        sortList(ranks, rankedDocuments);
        return rankedDocuments;
    }

    /**
     * Calculate the rank of a document based on bm25 formula, according to one term.
     * @param df DF of the current term
     * @param tf TF of the current term in the current document
     * @param maxTF The length of the current document
     * @return The rank of the document, according to the current term.
     */
    private double bm25(int df, int tf, int maxTF){
        int N = DocumentIndexer.NumOfDocs();
        double ntf=(double)tf/maxTF;
        double avgdl=DocumentIndexer.GetAvgdl();
        double log=(double)N/df;
        double idf=Math.log(log)/Math.log(10), nominator=ntf*(k+1), d1avgdl=maxTF/avgdl;
        return idf*nominator/(ntf+k*(1-b+b*d1avgdl));
    }

    /**
     * Sorts the documents list according to the every document's rank in ranks.
     * @param ranks Document to it's rank.
     * @param documents The list to put the sorted documents in.
     */
    private void sortList(Map<Document, Double> ranks, List<Document> documents){
        double maxRank=0;
        while(documents.size()<ranks.size() && documents.size()<50){
            for(double currentRank : ranks.values()){
                if(currentRank>maxRank)
                    maxRank=currentRank;
            }
            for(Map.Entry entry : ranks.entrySet())
                if((double)entry.getValue()==maxRank){
                    documents.add((Document) entry.getKey());
                    ranks.remove(entry.getKey());
                    maxRank=0;
                    break;
                }
        }
    }
}
