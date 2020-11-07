package Process;

import General.Document;
import General.Term;
import Index.TermIndexer;
import java.util.*;
import Parser.TextParser;
import Searcher.Searcher;

public class DataProcessor {
    public static Map<String, Map<Term, Integer>> initialTermsAndDocumentsNames =new HashMap<>();
    // firstUpdate the tf in the query string
    public static Map<Term, Integer> QueryTf=new HashMap<>();

    public static Set<String> original=new HashSet<>();

    public static Map<Document, Map<Term, Integer>> termAndDocument=new HashMap<>();

    public static List<String> queryAfterParse=new LinkedList<>();

    // initial update for initial map
    public static void firstUpdate(String word, String document, String tf) {
        Term t=wordToTerm(word);
        String convertedToString = String.valueOf(tf);
        int termTf = Integer.parseInt(convertedToString);
        if (!initialTermsAndDocumentsNames.containsKey(document)){
            Map<Term,Integer> temp=new HashMap<>();
            temp.put(t,termTf);
            initialTermsAndDocumentsNames.put(document,temp);
        }
        else if (initialTermsAndDocumentsNames.containsKey(document)){
            initialTermsAndDocumentsNames.get(document).put(t,termTf);
        }
    }

    //firstUpdate the map to keep only relevant items
    public static void keepOnlyRelevant(Set<String> docsNamesToKeep, boolean withCities){
        if(withCities)
            for (Map.Entry<String, Map<Term, Integer>> entry : initialTermsAndDocumentsNames.entrySet())
                termAndDocument.put(Searcher.getDocument(entry.getKey()),entry.getValue());
        else {
            for (Map.Entry<String, Map<Term, Integer>> entry : initialTermsAndDocumentsNames.entrySet())
                // relevant document
                if (docsNamesToKeep.contains(entry.getKey()))
                    termAndDocument.put(Searcher.getDocument(entry.getKey()), entry.getValue());
        }
        initialTermsAndDocumentsNames=new HashMap<>();
    }

    // get name of term and the tf of the term in the query, and add
    // the term and tf to the map
    public static void updateTfOfQuery(String word,int tf){
        Term ans=wordToTerm(word);
        QueryTf.put(ans,tf);
        queryAfterParse.add(word);
    }
    // get name of term , and return the term with all the information about it

    private static Term wordToTerm(String word){
        int df=TermIndexer.getDfOfTerm(word);
        int tf=TermIndexer.getCountOfTerm(word);
        boolean cap=TermIndexer.getCapOfTerm(word);
        boolean junk=TermIndexer.getJunkOfTerm(word);
        Term ans=new Term(word);
        ans.setDf(df);
        ans.setCount(tf);
        ans.setCap(cap);
        ans.setJunk(junk);
        return ans;
    }

    public static void GetQueryData(String query, boolean toStem){
        //get the query after parsing
        List<String> q = queryAfterParse(query, toStem);
        // firstUpdate tf of query word in the query
        for (String word:q) {
            int tf=tfOfWordInQuery(word,q);
            DataProcessor.updateTfOfQuery(word,tf);
        }
    }

    // get the original query and return list of strings after parsing the query
    public static List<String> queryAfterParse(String query, boolean toStem){
        List<Term> afterParseTerm;
        List<String> afterParseString=new ArrayList<>();
        TextParser textPar=new TextParser(null);
        afterParseTerm= textPar.Parse(query,new Document("name"),toStem);
        for (Term temp:afterParseTerm){
            afterParseString.add(temp.getName());
        }
        return afterParseString;
    }

    //get specific word in the query, and list of words in query,
    // and calculate the tf of the word in the query
    private static int tfOfWordInQuery(String word, List<String> queryAfterParsing){
        int tf=0;
        for (String aQueryAfterParsing : queryAfterParsing) {
            if (aQueryAfterParsing.equals(word))
                tf++;
        }
        return tf;
    }

    public static void Clear() {
        initialTermsAndDocumentsNames=new HashMap<>();
        QueryTf=new HashMap<>();
        termAndDocument=new HashMap<>();
        queryAfterParse=new LinkedList<>();
    }
}
