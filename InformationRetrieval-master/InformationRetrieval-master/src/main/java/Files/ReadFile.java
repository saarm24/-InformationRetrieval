package Files;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import Index.CitiesIndexer;
import Index.DocumentIndexer;
import Index.TermIndexer;
import Parser.TextParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Used to get the documents from the data set and send them to Parser and TermIndexer classes.
 */
public class ReadFile {
    private ExecutorService pool = Executors.newFixedThreadPool(4);
    private String DataSetPath, PostingListsPath;
    private boolean stemming;

    /**
     * Ctor.
     * @param DataSetPath The data set folder path.
     * @param PostingListsPath The posting lists folder path.
     * @param stemming To stem or not to stem.
     */
    public ReadFile(String DataSetPath, String PostingListsPath, boolean stemming) {
        TermIndexer.ClearIndex();
        CitiesIndexer.ClearIndex();
        DocumentIndexer.ClearIndex();
        if(stemming) {
            this.DataSetPath = DataSetPath;
            this.PostingListsPath = PostingListsPath+"\\PostingWithStemming";
            this.stemming = stemming;
        }
        else {
            this.DataSetPath = DataSetPath;
            this.PostingListsPath = PostingListsPath+"\\Posting";
            this.stemming = stemming;
        }
        new File(this.PostingListsPath).mkdir();
    }

    /**
     * Extract the documents from the data set folder, along with their tags, and send
     * it to the indexers.
     * @throws Exception When the chosen data set is not a folder or when corpus or stop words are missing.
     */
    public int[] Read() throws Exception {
        long start=System.currentTimeMillis(), end, e;
        File dataSetFolder=new File(DataSetPath);
        File corpus, stop_Words;
        int[] ansParams=new int[6];
        if(!dataSetFolder.isDirectory())
            throw new Exception();

        ArrayList<File> Files=new ArrayList<>();
        ArrayList<File> DataSet = new ArrayList<>(Arrays.asList(Objects.requireNonNull(dataSetFolder.listFiles())));
        corpus=getFile("corpus", DataSet);
        stop_Words =getFile("stop_words.txt", new ArrayList<>(Arrays.asList(Objects.requireNonNull(corpus.listFiles()))));

        if(corpus==null || stop_Words ==null || !corpus.isDirectory() || !stop_Words.isFile())
            throw new Exception();

        getFilesFromCorpus(corpus, Files);
        for(File file:Files){
            pool.execute(() -> HandleFile(file));
        }
        pool.shutdown();
        while(true) {
            try {
                if (pool.awaitTermination(100, TimeUnit.MILLISECONDS))
                    break;
            } catch (Exception ignored) {
            }
        }
        CitiesIndexer.UpdateDictionary();
        CitiesIndexer.UpdatePopulationIn(new TextParser(DataSetPath));
        CitiesIndexer.WriteDictionaryToFile();
        e=System.currentTimeMillis();
        ansParams[3]=DocumentIndexer.NumOfDocs();
        ansParams[4]=CitiesIndexer.NumOfCities();
        ansParams[5]=(int)((e-start)/1000);
        TermIndexer.WriteDictionaryToFile();
        DocumentIndexer.WriteDictionaryToFile();
        end=System.currentTimeMillis();
        ansParams[0]=DocumentIndexer.NumOfDocs();
        ansParams[1]=TermIndexer.NumOfTerms();
        ansParams[2]=(int)((end-start)/1000);
        return ansParams;
    }

    /**
     * Runs a thread that read the file.
     * @param file A file to read.
     */
    private void HandleFile(File file){
        TermIndexer termIndexer=new TermIndexer(PostingListsPath);
        CitiesIndexer citiesIndexer=new CitiesIndexer(PostingListsPath);
        DocumentIndexer documentIndexer=new DocumentIndexer(PostingListsPath);
        TextParser textParser=new TextParser(DataSetPath+"\\corpus");
        for(General.Document d:getDocuments(file)){
            try {
                termIndexer.HandleDocument(textParser.Parse(d.getTag("TEXT"), d, stemming), d);
                citiesIndexer.HandleDocument(textParser.getCities(), d);
                documentIndexer.AddDocument(d);
            }catch (Exception e){
                System.err.println(e);
                System.exit(2);
            }
        }
        termIndexer.BuildDictionary();
    }

    /**
     * Searching and returning a file from the data set (as list of files).
     * @param name The name of the wanted file.
     * @param folder The data set as a list of files.
     * @return The wanted file or null if missing.
     */
    private synchronized File getFile(String name, ArrayList<File> folder){
        for (File file:folder) {
            if(file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("\\")+1).equals(name))
                return file;
        }
        return null;
    }

    /**
     * Puts all the files that contains the document from the corpus folder in a list.
     * @param folder The corpus folder.
     * @param Documents A list that holds all the file from the corpus.
     */
    private synchronized void getFilesFromCorpus(File folder, ArrayList<File> Documents){
        for (File file: Objects.requireNonNull(folder.listFiles())) {
            if(file.isDirectory()){
                Documents.addAll(Arrays.asList(Objects.requireNonNull(file.listFiles())));
            }
            else Documents.add(file);
        }
    }

    /**
     * Get the documents from a file from the corpus.
     * @param file The file from the corpus.
     * @return A list of Documents.
     */
    private synchronized ArrayList<General.Document> getDocuments(File file){
        Document innerFile = null;
        try {
            innerFile = Jsoup.parse(file, "UTF8");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }

        ArrayList<General.Document> ans=new ArrayList<>();
        Elements documents=innerFile.select("DOC");

        for(Element doc:documents){
            General.Document d=new General.Document(doc.getElementsByTag("DOCNO").text());

            if(doc.getElementsByTag("HEADER").select("DATE1").hasText())
                d.addTag("DATE1", doc.getElementsByTag("HEADER").select("DATE1").text());

            if(doc.getElementsByTag("HEADER").select("F[P=104]").hasText())
                d.addTag("City", doc.getElementsByTag("HEADER").select("F[P=104]").text());
            else if(doc.select("F[P=104]").hasText())
                d.addTag("City", doc.getElementsByTag("F[P=104]").text());

            if(doc.select("F[P=104]").hasText())
                d.addTag("City", doc.select("F[P=104]").text());

            String text=doc.select("TEXT").text();

            if(("".equals(text))){
                text= GetAllTags(doc.children());
            }
            if(text!= null && !"".equals(text)){
                if(text.contains("[Text]"))
                    text = text.substring(text.indexOf("[Text]"));
            }

            if(doc.getElementsByTag("TEXT").select("F[P=105]").hasText()){
                String language=doc.getElementsByTag("TEXT").select("F[P=105]").text();
                if((language=GetLanguage(language))!=null)
                    d.addTag("Language", language);
            }

            d.addTag("TEXT", text);

            ans.add(d);
        }
        return ans;
    }

    /**
     * Get the first string in the tag.
     * @param text The text in tag F[P=104]
     * @return String if legit language, null else.
     */
    private synchronized String GetLanguage(String text){
        if(text.charAt(0)>='A' && text.charAt(0)<='Z'){
            text=text.split(" ")[0];
            return text;
        }
        return null;
    }

    /**
     * Get the text tag by checking recursively all the tags in an element.
     * @param elements Element to search in.
     * @return Text tag's content.
     */
    private synchronized static String GetAllTags(Elements elements) {
        for(Element el:elements) {
            if(el.tagName().toLowerCase().equals("text") || el.tagName().toLowerCase().equals("graphic"))
                return BreakElement(el);
            GetAllTags(el.children());
        }
        return "";
    }

    /**
     * Get all the values from an element's children elements.
     * @param el
     * @return
     */
    private synchronized static String BreakElement(Element el) {
        if(el.childNodeSize()<1)
            return el.text();
        String text="";
        for(Element e:el.children())
            text+=e.text()+" ";
        return text;
    }

    /**
     * Read a file of queries.
     * @param path Path to the file to read.
     * @return List of lists. Every list contains the query number, the title and the description.
     */
    public static List<List<String>> ReadQuery(String path){
        File file=new File(path);
        if(!file.exists() && !file.isDirectory())
            return null;

        ArrayList<List<String>> ans=new ArrayList<>();
        String lineFromFile;
        StringBuilder cleanLine;
        Queue<String> lines=new LinkedList<>();
        try {
            BufferedReader bw = new BufferedReader(new FileReader(path));
            while ((lineFromFile = bw.readLine()) != null) {
                lines.add(lineFromFile);
            }
            String line;
            while(lines.size()>0) {
                line = lines.remove();
                if(line.contains("top")){
                    List<String> tmpList=new LinkedList<>();
                    while(!line.contains("num"))
                        line=lines.remove();
                    cleanLine=new StringBuilder(line);
                    cleanLine.replace(0,cleanLine.indexOf(":")+1, "");
                    cleanString(cleanLine);
                    tmpList.add(cleanLine.toString());

                    while(!line.contains("title"))
                        line=lines.remove();
                    cleanLine=new StringBuilder(line);
                    cleanString(cleanLine);
                    tmpList.add(cleanLine.substring(8));

                    while(!line.contains("desc"))
                        line=lines.remove();
                    line=lines.remove();
                    if(line.contains("document"))
                        line=line.substring(line.indexOf("document")+9);
                    StringBuilder desc= new StringBuilder(line);

                    line=lines.remove();
                    while(!line.contains("narr")) {
                        desc.append(line);
                        line = lines.remove();
                    }
                    cleanString(desc);
                    tmpList.add(desc.toString());

                    line = lines.remove();
                    while(!line.contains("top"))
                        line = lines.remove();
                    ans.add(tmpList);
                }
            }
            bw.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
        return ans;
    }

    /**
     * Writes string to a file.
     *
     * @param filePath The file to write to.
     * @param str The string to write to the file.
     */
    public static void WriteToFile(String filePath, StringBuilder str) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true));
            bw.append(str);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void cleanString(StringBuilder sb){
        while(sb.charAt(0)==' ')
            sb.replace(0, 1, "");
        while(sb.charAt(sb.length()-1)==' ')
            sb.replace(sb.length()-1, sb.length(), "");
    }
}
