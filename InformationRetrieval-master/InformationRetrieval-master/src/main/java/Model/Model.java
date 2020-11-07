package Model;

import Files.ReadFile;
import General.Document;
import General.Term;
import Index.CitiesIndexer;
import Index.DocumentIndexer;
import Index.TermIndexer;
import Ranker.Ranker;
import Searcher.Searcher;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import Process.DataProcessor;
import TermVector.TermVector;

public class Model {
    private String corpusAdd;
    private String stopWordAdd;
    private String postingAdd;
    private boolean toStem;
    private CitiesIndexer ci;
    private DocumentIndexer di;
    private TermIndexer ti;
    private ReadFile rf;
    private boolean toSemantic;

    private List<String> citiesAdded=new ArrayList<>();
    private boolean Entity;
    private String pathToSearcher;
    private boolean isLoaded;
    private static int loadCounter=0;


    public boolean isEntity() {
        return Entity;
    }

    private String saveResultsPath;

    public void setSaveResultsPath(String saveResultsPath) {
        this.saveResultsPath = saveResultsPath;
    }

    public void setPathToSearcher(String pathToSearcher) {
        this.pathToSearcher = pathToSearcher;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public String getAddressesInfo(){
        if (toStem)
            return postingAdd+"\\"+"PostingWithStemming";
        else
            return postingAdd+"\\"+"Posting";
    }

    public void setEntity(boolean entity) {
        Entity = entity;
    }

    public boolean checkRead(){
        if ( postingAdd == null || postingAdd.equals(""))
            return false;
        File f;
        if (toStem) {
            f = new File(postingAdd + "\\PostingWithStemming");
        }
        else {
            f = new File(postingAdd + "\\Posting");
        }
        return f.exists();
    }

    public void load() {
        try{
            CitiesIndexer.ClearIndex();
            DocumentIndexer.ClearIndex();
            TermIndexer.ClearIndex();
            isLoaded=true;
            if(ci==null || di==null || ti==null || loadCounter==1){
                String add="";
                if (toStem)
                    add="PostingWithStemming";
                else
                    add="Posting";
                ci=new CitiesIndexer(postingAdd+"\\"+add);
                ti= new TermIndexer(postingAdd+"\\"+add);
                di= new DocumentIndexer(postingAdd+"\\"+add);
            }
            loadCounter=1;
            ti.ReadDictionaryFromFile();
            di.ReadDictionaryFromFile();
            ci.ReadDictionaryFromFile();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public Model() {
        this.corpusAdd = "";
        this.stopWordAdd = "";
        this.postingAdd = "";
        this.toStem = false;
    }

    public void setCorpusAdd(String corpusAdd) {
        this.corpusAdd = corpusAdd;
    }

    public void setStopWordAdd(String stopWordAdd) {
        this.stopWordAdd = stopWordAdd;
    }

    public void setToStem(boolean toStem) {
        this.toStem = toStem;
    }

    public void setToSemantic(boolean toSemantic) {
        this.toSemantic = toSemantic;
    }

    public void setPostingAdd(String postingAdd) {
        this.postingAdd = postingAdd;
    }

    public int[] create() throws Exception{
        int[] info=new int[6];
        rf = new ReadFile(corpusAdd, postingAdd, toStem);
        info=rf.Read();
        //CitiesIndexer
        ci = new CitiesIndexer(postingAdd);
        //DocumentIndexer
        di = new DocumentIndexer(postingAdd);
        //TermIndexer
        ti = new TermIndexer(postingAdd);
        isLoaded=true;
        if(toStem)
            setPathToSearcher(postingAdd+"\\PostingWithStemming");
        else setPathToSearcher(postingAdd+"\\Posting");
        return info;
    }

    public boolean checkValid() {
        return !corpusAdd.equals("") && !stopWordAdd.equals("") && !postingAdd.equals("");
    }

    public boolean checkPathToReset() {
        return postingAdd != null && !postingAdd.equals("");
    }

    public void reset(){
        String path = postingAdd;
        if (!toStem) {
            path = path + "\\Posting";
        }
        else {
            path = path + "\\PostingWithStemming";
        }
        File currentFile = new File(path);
        deleteFolder(currentFile);
        CitiesIndexer.ClearIndex();
        ci = null;
        DocumentIndexer.ClearIndex();
        di = null;
        TermIndexer.ClearIndex();
        ti = null;
        rf=null;
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public TreeSet<String> getSortedTerms() {
        return TermIndexer.GetSortedTerms();
    }

    public List<String> GetLanguages() {
        Set<String> lanSet = DocumentIndexer.GetLanguages();
        List<String> languages = new ArrayList<>();
        languages.addAll(lanSet);
        return languages;
    }

    public TreeSet<String> getCities(){
        return CitiesIndexer.GetSortedCities();
    }

    public List<Document> RunQuery(String query, List<String> cities){
        Searcher searcher=new Searcher();
        searcher.setPathToSearch(pathToSearcher);
        Ranker ranker=new Ranker();
        DataProcessor.GetQueryData(query, toStem);
        if(toSemantic)
            UpdateQueryBySemantic(query);
        searcher.searchUp(cities);
        return ranker.Rank(DataProcessor.termAndDocument, DataProcessor.QueryTf);
    }

    public List<String> getCitiesAdded() {
        return citiesAdded;
    }

    private void UpdateQueryBySemantic(String query){
        TermVector.BuildVectors();
        double rank, delta=0.95;
        HashSet<String> semanticRelated=new HashSet<>();
        Term QTerm;
        for(Map.Entry entry: DataProcessor.QueryTf.entrySet()){
            QTerm = (Term) entry.getKey();
            if(!TermVector.termSize.containsKey(QTerm.getName().toLowerCase()))
                continue;
            for(String GTerm : TermVector.termSize.keySet()) {
                if(GTerm.equals(QTerm.getName().toLowerCase()))
                    continue;
                rank = TermVector.GetRank(QTerm.getName(), GTerm);
                if(Math.abs(rank)>delta)
                    semanticRelated.add(GTerm);
            }
        }
        for(String term : semanticRelated)
            DataProcessor.updateTfOfQuery(term, 1);
    }

    TermVector tv=new TermVector();

    public void addCity(String city){
        citiesAdded.add(city);
    }

    public void delCity(String city){
        if (citiesAdded.contains(city)){
            citiesAdded.remove(city);
        }
    }

    // return the names of the documents after processed the query - with or without entities
    public List<String> docsToDisplay(List<Document> dList){
        List<String> docWithInfo=new ArrayList<>();
        // no info about entities of documents needed
        if (!Entity){
            for (Document d:dList)
                docWithInfo.add(d.getName());
        }
        else{
            for (Document d : dList){
                docWithInfo.add(d.getName()+" Entities: "+d.getEntitiesByString());
            }
        }
        return docWithInfo;
    }

    public void writeResults(String numOfQuery, List<String>resultDocs){
        if (saveResultsPath!=null){
            StringBuilder sb=new StringBuilder();
            for (String doc:resultDocs){
                sb.append(numOfQuery).append(" ").append("1").append(" ").append(doc).append(" ").append("1").append(" ").append("2").append(" ").append("a").append("\n");
            }
            ReadFile.WriteToFile(saveResultsPath+"\\results.txt",sb);
        }
    }

    public boolean isToStem() {
        return toStem;
    }
}