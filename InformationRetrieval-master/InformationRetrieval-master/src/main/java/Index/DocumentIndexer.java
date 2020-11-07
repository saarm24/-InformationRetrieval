package Index;

import Files.ReadFile;
import General.DocEntity;
import General.Document;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Index for all the document in the corpus.
 */
public class DocumentIndexer {
    private static Map<String, Document> dictionary = new ConcurrentHashMap<>(500000);
    private static String folderPath, postingPath, dictionaryPath;
    private static double avgdl, totalLen;

    public DocumentIndexer(String folderPath) {
        if (DocumentIndexer.folderPath == null) {
            DocumentIndexer.folderPath =folderPath;
            postingPath = folderPath + "\\DocumentsPosting";
            dictionaryPath = folderPath + "\\DocumentsDictionary";
            new File(postingPath).mkdir();
            new File(dictionaryPath).mkdir();
        }
    }

    /**
     * Clear all the static variables.
     */
    public synchronized static void ClearIndex(){
        dictionary=new ConcurrentHashMap<>(500000);
        folderPath=postingPath=dictionaryPath=null;
    }

    /**
     * Add a document to the corpus.
     * @param document A document to add.
     */
    public void AddDocument(Document document){
        Document d = new Document(document.getName());
        d.setMaxTf(document.getMaxTf());
        d.setUniqueCount(document.getUniqueCount());
        d.setLength(document.getLength());
        d.setMostFrquenWord(document.getMostFrquenWord());
        for(DocEntity entity : document.getEntities())
            d.AddEntity(entity);
        try {
            d.addTag("City", document.getTag("City"));
        }catch (Exception e){}
        try {
            d.addTag("Language", document.getTag("Language"));
        }catch (Exception e){}
        dictionary.put(document.getName(), d);
        UpdateAvgdl(d.getLength());
    }

    /**
     * Writes the documents's dictionary to a file.
     * the file structure: document name||maxTF|unique count|length|city|entity, Rank|e.g
     */
    public static void WriteDictionaryToFile() {
        StringBuilder str=new StringBuilder();
        for (String name : dictionary.keySet()) {
            Document doc=dictionary.get(name);
            str.append(name).append("||").append(doc.getMaxTf()).append("|").
                    append(doc.getUniqueCount()).append("|").append(doc.getLength());
            try{
                String s=doc.getTag("City");
                if(s!=null && !"".equals(s))
                    str.append("|").append(doc.getTag("City"));
            }catch (Exception e){}
            int i=0;
            DecimalFormat df=new DecimalFormat("#.###");
            for(DocEntity p: doc.getEntities()) {
                str.append("|").append(p.getName()).append(",").append(df.format(p.getValue()));
                i++;
                if(i>4)
                    break;
            }
           // System.out.println("docMostFrequentWordIs:"+doc.getMostFrquenWord());
            str.append("|").append(doc.getMostFrquenWord()).append("!");
            str.replace(str.length(), str.length(), "");
            str.append(System.getProperty("line.separator"));
        }
        ReadFile.WriteToFile(dictionaryPath + "\\dictionary.txt", str);
    }

    /**
     * the file structure: document name||maxTF|unique count|city|entities
     */
    public void ReadDictionaryFromFile() {
        String lineFromFile;
        int maxFT, uniqueCount, length;
        Queue<StringBuilder> values=new LinkedList<>();
        Queue<StringBuilder> lines=new LinkedList<>();
        Document doc;
        try {
            BufferedReader bw = new BufferedReader(new FileReader(folderPath + "\\DocumentsDictionary\\dictionary.txt"));
            while ((lineFromFile = bw.readLine()) != null) {
                lines.add(new StringBuilder(lineFromFile));
            }
            StringBuilder line;
            while(lines.size()>0) {
                line = lines.remove();
                GetValues(line, values);

                doc = new Document(values.remove().toString());
                maxFT = Integer.valueOf(values.remove().toString());
                uniqueCount = Integer.valueOf(values.remove().toString());
                length = Integer.valueOf(values.remove().toString());

                doc.setMaxTf(maxFT);
                doc.setUniqueCount(uniqueCount);
                doc.setLength(length);
                while(values.size()>0){
                    StringBuilder value=values.remove();
                   // System.out.println("value is:"+value);
                    if(value.indexOf(",")!=-1){
                        try{
                            String name=value.substring(0, value.lastIndexOf(","));
                            double rank=Double.parseDouble(value.substring(name.length()+1));
                            doc.AddEntity(new DocEntity(name, rank));
                        }catch (Exception e){}
                    }else if(value.indexOf("!")!=-1) {
                       // System.out.println("check:"+value.substring(0,value.length()-1));
                        doc.setMostFrquenWord(value.substring(0,value.length()-1));
                    }else{
                        doc.addTag("City", value.toString());
                    }
                }
                dictionary.put(doc.getName(), doc);
                UpdateAvgdl(length);
            }
            bw.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }
    }

    private void GetValues(StringBuilder sb, Queue<StringBuilder> values){
        values.clear();
        int i, j;
        for(i=1, j=0; i<sb.length(); i++)
            if(sb.charAt(i)==sb.charAt(i+1) && sb.charAt(i)=='|') {
                values.add(new StringBuilder(sb.substring(j, i)));
                i+=2;
                j=i;
                break;
            }
        for(;i<sb.length(); i++)
            if(sb.charAt(i)==124) {
                values.add(new StringBuilder(sb.substring(j, i)));
                j=i+1;
            }
        values.add(new StringBuilder(sb.substring(j, i)));
    }

    public static int NumOfDocs(){
        return dictionary.size();
    }

    public static Set<String> GetLanguages(){
        Set<String> ans=new TreeSet<>();
        String tmp;
        for(Document d:dictionary.values())
            try{
                if((tmp=d.getTag("Language"))!=null && !tmp.equals(""))
                    ans.add(tmp);
            }catch (Exception e){}
        return ans;
    }

    public static Document getDocByName(String name){
        if (dictionary.containsKey(name)) {
            return dictionary.get(name);
        }
        return null;
    }

    private synchronized static void UpdateAvgdl(int len){
        totalLen+=len;
        avgdl=totalLen/dictionary.size();
    }

    public static double GetAvgdl(){
        return avgdl;
    }
}
