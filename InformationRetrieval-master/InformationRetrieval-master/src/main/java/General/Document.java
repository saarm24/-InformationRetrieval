package General;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Representing a document in a file from the corpus.
 */
public class Document {
    private String name;
    private Dictionary<String, String> tags=new Hashtable<>(2);
    private int maxTf=0, tf,length=0;
    private int uniqueCount =0;
    private Set<DocEntity> entities=new TreeSet<>(Comparator.comparingDouble(DocEntity::getValue).reversed());
    private double vectorSize;
    private String mostFrquenWord;

    public void setMostFrquenWord(String mostFrquenWord) {

        this.mostFrquenWord = mostFrquenWord;
    }

    public String getMostFrquenWord() {
        return mostFrquenWord;
    }

    public void setVectorSize(double size){this.vectorSize=size; }

    public double getVectorSize(){return vectorSize;}

    public void setTf(int tf){this.tf=tf; }

    public int getTf() {
        return tf;
    }

    public int getMaxTf() {
        return maxTf;
    }

    public int getLength(){return length;}

    public int getUniqueCount() {
        return uniqueCount;
    }

    public void setUniqueCount(int uniqueCount) {
        this.uniqueCount = uniqueCount;
    }

    public Document(String name) {
        this.name = name;
    }

    /**
     * Puts a tag with it's value in the dictionary.
     * @param tag A tag in the document.
     * @param value The value of a tag.
     */
    public void addTag(String tag, String value){
        tags.put(tag, value);
    }

    /**
     * Returning the value of a tag. In case the tag is "TEXT", the value will be deleted to save memory.
     * @param tag A tag in the document.
     * @return The value of a tag.
     */
    public String getTag(String tag){
        String ans=null;
        try{
            ans=tags.get(tag);
            if(tag.equals("TEXT"))
                tags.remove(tag);
        }
        catch (Exception e){}
        return ans;
    }

    public void setMaxTf(int maxTf) {
        this.maxTf = maxTf;
    }

    public void setLength(int Length){this.length=Length;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(name, document.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return name;
    }

    public void AddEntity(DocEntity entity){ entities.add(entity);}

    public Set<DocEntity> getEntities(){return entities;}

    //return the list of the entities of document in a one line string
    public String getEntitiesByString(){
        StringBuilder ans= new StringBuilder();
        if(entities.size()<1)
            return "";
        int i=0;
        DecimalFormat df=new DecimalFormat("#.###");
        for (DocEntity dc:entities) {
            ans.append(dc.getName()).append(",").append(df.format(dc.getValue())).append(" | ");
            i++;
            if(i>4)
                break;
        }
        ans.replace(ans.length()-3, ans.length(), "");
        return ans.toString();
    }
}
