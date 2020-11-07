package Searcher;

import General.Document;
import Index.CitiesIndexer;
import Index.DocumentIndexer;
import Index.TermIndexer;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import Process.DataProcessor;


import static Index.TermIndexer.getPostingListOfQuery;

public class Searcher {
    private String PathToSearch ="";
    private CitiesIndexer ci = new CitiesIndexer(null);//"C:\\Users\\saarm\\Desktop\\IR\\sec\\Posting");

    public void setPathToSearch(String pathToSearch) {
        PathToSearch = pathToSearch;
    }

    public void searchUp(List<String> cities) {
        List<String> queryAfterParsing= DataProcessor.queryAfterParse;
        // NAIVE SEARCH
        List<Document> naiveListDocs;
        List<Document> AllNaiveListDocs = new ArrayList<>();
        Set<String> naiveDocName = new HashSet<>();

        // make the document list of all the corpus
        for (String queryWord : queryAfterParsing) {
            naiveListDocs = readDocFromPosting(queryWord, getPostingListOfQuery(queryWord));
            if (naiveListDocs != null) {
                AllNaiveListDocs.addAll(naiveListDocs);
            }
        }
        // firstUpdate the set of docs names
        for(Document d:AllNaiveListDocs) {
            naiveDocName.add(d.getName());
        }
        // work with cities
        // if cities is not null checking
        Set<String> AllcitiesFound=new HashSet<>();
        List<String> DocsNameOfSpecificCity ;
        // make the document list of all the relevant cities
        if (cities!=null) {
            for (String c : cities) {
                DocsNameOfSpecificCity = getDocsNamesOfCity(c);
                if(DocsNameOfSpecificCity!=null)
                    AllcitiesFound.addAll(DocsNameOfSpecificCity);
            }
        }

        if (cities==null) {
            DataProcessor.keepOnlyRelevant(naiveDocName, false);
            System.out.println("End query");
        }
        // there are cities
        else{
            // keep only common values in naiveDocName
            naiveDocName = checkEquality(naiveDocName, AllcitiesFound);
            //firstUpdate the data map
            DataProcessor.keepOnlyRelevant(naiveDocName, true);
            System.out.println("End query");
        }
    }

    private List<Document> readDocFromPosting(String word, LinkedList<Pair<String, Integer>> postLists){
        String name;
        int lineNum;
        String line;
        List<String> docNames;
        List<String> docNamesAfter=new ArrayList<>();
        List<Document> docToRet=new ArrayList<>();

        if (postLists==null) {
            return null;
        }
        BufferedReader bw;
        for (Pair<String, Integer> postList : postLists) {
            //name = postList.getKey();
            lineNum = postList.getValue();
            try {
                bw = new BufferedReader(new FileReader(PathToSearch + "\\TermPosting\\" + postList.getKey() + ".txt"));
                for (int k = 0; (line = bw.readLine()) != null; k++)
                    if (k == lineNum)
                        break;
                /*docNames = getDocNameFromPosting(word, line);
                for (String n : docNames) {
                    docNamesAfter.add(n);
                }*/
                docNamesAfter.addAll(getDocNameFromPosting(word, line));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String na:docNamesAfter){
            //Document doc=getDocumentByName(na);
            docToRet.add(getDocumentByName(na));
        }
        return docToRet;
    }

    private List<String> getDocNameFromPosting(String word, String lineNum){
        int firstIndex=0,lastIndex=0;
        List<String> namesBefore=new ArrayList<>();
        List<String> docNameToRet=new ArrayList<>();
        // divide the lineNum in posting list into separate words
        for (int i=0;i<lineNum.length();i++){
            if (lineNum.charAt(i)!='|') {
                continue;
            }
            else {
                namesBefore.add(lineNum.substring(firstIndex+1,i));
                firstIndex=i;
                lastIndex=i+1;
                continue;
            }
        }
        // add the last word in the lineNum
        namesBefore.add(lineNum.substring(lastIndex,lineNum.length()));
        // get for each document in the posting - the name of the document
        for (int k=2;k<namesBefore.size();k+=2){
            // add the name of the word
            docNameToRet.add(namesBefore.get(k));
            DataProcessor.firstUpdate(word, namesBefore.get(k), namesBefore.get(k+1));
        }
        return docNameToRet;
    }

    //return the names of the documents for specific city
    private List<String> getDocsNamesOfCity(String cityToCheck){
        //load the cities indexer
        ci.ReadDictionaryFromFile();
        List<String> DocsNamesBefore;
        List<String> DocsNamesAfter=new ArrayList<>();
        //city exists
        if (CitiesIndexer.isCityExists(cityToCheck)){
            DocsNamesBefore=CitiesIndexer.getDocsNamesOfCity(cityToCheck);
            //fix the names of the documents
            for (String aDocsNamesBefore : DocsNamesBefore) DocsNamesAfter.add(RemovePsik(aDocsNamesBefore));
            return DocsNamesAfter;
        }
        return null;
    }

    // function that get the set of ducuments which found for query
    //and set of documents found for the cities
    //and returns a set of the common document's names
    private Set<String> checkEquality(Set<String> naiveDoc, Set<String> citiesDocs){
        // keeps only the equal values between the 2 sets
        // the only values that will keep in naiveDoc are the values
        //which also exists in the citiesDocs
        if (citiesDocs.size() == 0)
            return naiveDoc;

        naiveDoc.retainAll(citiesDocs);
        return naiveDoc;
    }

    // for each document name get the document
    private Document getDocumentByName(String name){
        return DocumentIndexer.getDocByName(name);
    }

    //static method
    public static Document getDocument(String name){
        return DocumentIndexer.getDocByName(name);
    }

    //get a string with , and remove it
    private String RemovePsik(String cityDoc){
        StringBuilder ans= new StringBuilder();
        for (int i=0;i<cityDoc.length();i++){
            if (cityDoc.charAt(i)==',')
                break;
            ans.append(cityDoc.charAt(i));
        }
        return ans.toString();
    }
}
