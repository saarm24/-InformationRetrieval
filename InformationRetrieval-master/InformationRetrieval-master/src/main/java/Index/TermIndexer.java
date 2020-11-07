//TODO:
// 3. document maxtf and unique words - in docindex
// 5. capitalized capitalizedTerms
// 7. test runtime of WritePostingList
// 9. save only doc name and tf, and another index for docs
package Index;

import Files.ReadFile;
import General.Document;
import General.Term;
import javafx.util.Pair;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gets lists of terms from every document. Creates posting list for each term
 * in every file.
 */
public class TermIndexer {
    private static Map<String, TermDictionaryEntry> dictionary = new ConcurrentHashMap<>(1000000);
    private static String folderPath, postingPath, dictionaryPath;
    private static int fileNum = 0;
    private static final Integer Dlock = 0, numLock = 0;
    private Map<String, Pair<Term, LinkedList<Document>>> index = new HashMap<>();
    private int postingFileNum;

    /**
     * Clear all the static variables.
     */
    public synchronized static void ClearIndex() {
        dictionary = new HashMap<>(1000000);
        folderPath = postingPath = dictionaryPath = null;
        fileNum = 0;
    }

    /**
     * Ctor.
     *
     * @param folderPath The Folder to put the index files in.
     */
    public TermIndexer(String folderPath) {
        postingFileNum = GetFileNum();
        if (TermIndexer.folderPath == null) {
            TermIndexer.folderPath = folderPath;
            postingPath = folderPath + "\\TermPosting";
            dictionaryPath = folderPath + "\\TermDictionary";
            new File(postingPath).mkdir();
            new File(dictionaryPath).mkdir();
        }
    }

    /**
     * Updating the index.
     *
     * @param terms    A list of capitalizedTerms from a document.
     * @param document The document which the capitalizedTerms are from.
     */
    public void HandleDocument(List<Term> terms, Document document) {
        for (Term newTerm : terms) {
            newTerm.setDf(1);
            Document d = new Document(document.getName());
            d.setTf(newTerm.getCount());
            d.setMaxTf(document.getMaxTf());
            d.setUniqueCount(terms.size());
            if (index.containsKey(newTerm.getName())) {
                Term existTerm = index.get(newTerm.getName()).getKey();
                existTerm.setDf(existTerm.getDf()+1);
                if (existTerm.isCap() != newTerm.isCap())
                    existTerm.setCap(false);
                existTerm.setCount(existTerm.getCount() + newTerm.getCount());
                index.get(newTerm.getName()).getValue().add(d);
            } else {
                LinkedList l = new LinkedList<>();
                l.add(d);
                index.put(newTerm.getName(), new Pair<>(newTerm, l));
            }
        }
    }

    /**
     * firstUpdate the dictionary from the current index.
     * Write the current index as a posting list to a file.
     */
    public void BuildDictionary() {
        String postingFile = postingPath + "\\" + postingFileNum + ".txt";
        UpdateDictionary(String.valueOf(postingFileNum));
        WritePostingList(postingFile);
    }

    /**
     * firstUpdate the dictionary from the current index.
     *
     * @param file The file which the posting list (current index) is going to be write to.
     */
    private void UpdateDictionary(String file) {
        int i = 0;
        for (String name : index.keySet()) {
            synchronized (Dlock) {
                TermDictionaryEntry termDictionaryEntry = dictionary.get(name);
                if (termDictionaryEntry == null) {
                    LinkedList<Pair<String, Integer>> list = new LinkedList<>();
                    list.add(new Pair(file, i++));
                    dictionary.put(index.get(name).getKey().getName(), new TermDictionaryEntry(index.get(name).getKey().getDf(), index.get(name).getKey().getCount(), index.get(name).getKey().isCap(), index.get(name).getKey().getJunk(), list));
                } else {
                    termDictionaryEntry.postingLists.add(new Pair(file, i++));
                    termDictionaryEntry.df += index.get(name).getKey().getDf();
                    termDictionaryEntry.count += index.get(name).getKey().getCount();
                    termDictionaryEntry.cap = termDictionaryEntry.cap && index.get(name).getKey().isCap();
                }
            }
        }
    }

    /**
     * Builds a posting list and send it to WriteToFile.
     * A list structure: term||Document name|tf|e.g
     *
     * @param file The file which the posting list (current index) is going to be write to.
     */
    private void WritePostingList(String file) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String name : index.keySet()) {
            stringBuilder.append(index.get(name).getKey().getName()).append("||");
            for (Document d : index.get(name).getValue()) {
                stringBuilder.append(d.getName()).append("|").append(d.getTf()).append("|");
            }
            stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), "");
            stringBuilder.append(System.getProperty("line.separator"));
        }
        ReadFile.WriteToFile(file, stringBuilder);
    }

    /**
     * Writes the term's dictionary to a file.
     * the file structure: term||capitalized|count|df|has junk|a posting list file name, line|e.g
     */
    public static void WriteDictionaryToFile() {
        Map<String, TermDictionaryEntry> tmp = new TreeMap<>();
        tmp.putAll(dictionary);
        dictionary = tmp;
        StringBuilder str = new StringBuilder();
        TermDictionaryEntry entry;
        int i=0;
        for (String term : dictionary.keySet()) {
            entry = dictionary.get(term);
            str.append(term).append("||");
            if (entry.cap)
                str.append(1);
            else str.append(0);
            str.append("|").append(entry.count).append("|");
            str.append(entry.df).append("|");
            if (entry.junk)
                str.append(1).append("|");
            else str.append(0).append("|");
            for (Pair<String, Integer> pair : entry.postingLists)
                str.append(pair.getKey()).append(",").append(pair.getValue()).append("|");
            str.replace(str.length() - 1, str.length(), "");
            str.append(System.getProperty("line.separator"));
            i++;
            if(i%10000==0) {
                ReadFile.WriteToFile(dictionaryPath + "\\dictionary.txt", str);
                str=new StringBuilder();
            }
        }
        if(i%10000!=0)
            ReadFile.WriteToFile(dictionaryPath + "\\dictionary.txt", str);
    }

    /**
     * @return The next file name of a posting list.
     */
    private static int GetFileNum() {
        synchronized (numLock) {
            return ++fileNum;
        }
    }

    /**
     * @return TermDictionary as a sorted set.
     */
    public static TreeSet<String> GetSortedTerms() {
        TreeSet<String> ans = new TreeSet<>(String::compareToIgnoreCase);
        ans.addAll(dictionary.keySet());
        return ans;
    }

    /**
     * Retrieving the dictionary from the file.
     * the file structure: term||capitalized|count|df|has junk|a posting list file name, line|e.g
     */
    public void ReadDictionaryFromFile() {
        long start, end;
        start = System.currentTimeMillis();
        String pl, lineFromFile, tmp;
        List<StringBuilder> values = new ArrayList<>();
        Term term;
        LinkedList<Pair<String, Integer>> documents;
        Integer plLine;
        TermDictionaryEntry tde;
        try {
            BufferedReader bw = new BufferedReader(new FileReader(folderPath + "\\TermDictionary\\dictionary.txt"));
            Queue<StringBuilder> lines = new LinkedList<>();
            while ((lineFromFile = bw.readLine()) != null)
                lines.add(new StringBuilder(lineFromFile));
            StringBuilder line;
            while (lines.size() > 0) {
                line = lines.remove();
                GetValues(line, values);
                term = new Term(values.get(0).toString());
                documents = new LinkedList<>();
                for (int i = 5; i < values.size(); i++) {
                    tmp = values.get(i).toString();
                    pl = tmp.substring(0, tmp.indexOf(","));
                    plLine = Integer.valueOf(tmp.substring(pl.length() + 1));
                    documents.add(new Pair<>(pl, plLine));
                }
                term.setCap("1".equals(values.get(1).toString()));
                term.setCount(Integer.valueOf(values.get(2).toString()));
                term.setDf(Integer.valueOf(values.get(3).toString()));
                if (Integer.valueOf(values.get(4).toString()) == 0)
                    term.setJunk(false);
                else term.setJunk(true);
                tde = new TermDictionaryEntry(Integer.valueOf(values.get(3).toString()), Integer.valueOf(values.get(2).toString()), term.isCap(), term.getJunk(), documents);
                dictionary.put(values.get(0).toString(), tde);
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }
        end = System.currentTimeMillis();
        System.out.println("Terms: " + (end - start) / 1000);
    }

    private void GetValues(StringBuilder sb, List<StringBuilder> values) {
        values.clear();
        int i, j;
        for (i = 1, j = 0; i < sb.length(); i++)
            if (sb.charAt(i) == sb.charAt(i + 1) && sb.charAt(i) == '|') {
                values.add(new StringBuilder(sb.substring(j, i)));
                i += 2;
                j = i;
                break;
            }
        for (; i < sb.length(); i++)
            if (sb.charAt(i) == 124) {
                values.add(new StringBuilder(sb.substring(j, i)));
                j = i + 1;
            }
        values.add(new StringBuilder(sb.substring(j, i)));
    }

    public static int NumOfTerms() {
        return dictionary.size();
    }

    /**
     * Wraps a dictionary entry: for a term, keeps it's count,df and
     * posting list which the term is in, and where the term is at the posting list (line).
     */
    private static class TermDictionaryEntry {
        private int df, count;
        private boolean cap, junk;
        private LinkedList<Pair<String, Integer>> postingLists;

        TermDictionaryEntry(int df, int count, boolean cap, boolean junk, LinkedList<Pair<String, Integer>> postingLists) {
            this.df = df;
            this.count = count;
            this.cap = cap;
            this.junk = junk;
            this.postingLists = postingLists;
        }

        public int getDf() {
            return df;
        }

        public int getCount() {
            return count;
        }

        public boolean isCap() {
            return cap;
        }

        public boolean isJunk() {
            return junk;
        }

        public LinkedList<Pair<String, Integer>> getPostingLists() {
            return postingLists;
        }
    }

    public static TermDictionaryEntry getTermByString(String query){
        if (!dictionary.containsKey(query))
            return null;
        else {
            return dictionary.get(query);
        }
    }
    public static int getDfOfTerm(String word){
        if (!dictionary.containsKey(word))
            return -1;
        else {
            return dictionary.get(word).getDf();
        }
    }
    public static int getCountOfTerm(String word){
        if (!dictionary.containsKey(word))
            return -1;
        else {
            return dictionary.get(word).getCount();
        }
    }
    public static boolean getCapOfTerm(String word){
        if (!dictionary.containsKey(word))
            return false;
        else {
            return dictionary.get(word).isCap();
        }
    }
    public static boolean getJunkOfTerm(String word){
        if (!dictionary.containsKey(word))
            return false;
        else {
            return dictionary.get(word).isJunk();
        }
    }

    // get the posting list of termEntry by query
    public static LinkedList<Pair<String, Integer>> getPostingListOfQuery(String query){
        TermDictionaryEntry ans= getTermByString(query);
        if (ans==null) {
            return null;
        }
        else
            return ans.getPostingLists();
    }
}
