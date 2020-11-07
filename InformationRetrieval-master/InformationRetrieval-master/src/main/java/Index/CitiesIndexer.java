package Index;

import Files.DBConnection;
import Files.ReadFile;
import General.Document;
import Parser.TextParser;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Index of the cities in the corpus.
 */
public class CitiesIndexer{
    private static Map<String, CitiesDictionaryEntry> dictionary = new ConcurrentHashMap<>(400);
    private static Map<String, Pair<String, String>> countriesInDB=new HashMap<>();
    private static String folderPath, postingPath, dictionaryPath;
    private static Set<String> citiesInTags=new HashSet<>();
    private static Map<String, String> citiesInDB=new HashMap<>();
    private static final Integer CTLock =0, CCLock=0, DBLock=0;
    private static HashSet<String> candidateCities=new HashSet<>(20000);

    /**
     * Ctor.
     *
     * @param folderPath The Folder to put the index files in.
     */
    public CitiesIndexer(String folderPath) {
        if (CitiesIndexer.folderPath == null) {
            CitiesIndexer.folderPath =folderPath;
            postingPath = folderPath + "\\CitiesPosting";
            dictionaryPath = folderPath + "\\CitiesDictionary";
            new File(postingPath).mkdir();
            new File(dictionaryPath).mkdir();
            synchronized (DBLock) {
                if (citiesInDB.size() < 1 || countriesInDB.size() < 1)
                    BuildCitiesAndCountriesFromDB();
            }
        }
    }

    /**
     * Clear all the static variables.
     */
    public synchronized static void ClearIndex(){
        dictionary=new ConcurrentHashMap<>(400);
        folderPath=postingPath=dictionaryPath=null;
        citiesInTags=new HashSet<>();
        candidateCities=new HashSet<>(20000);
    }

    /**
     * Checks if a term refers to a city.
     * @param city The term to check.
     * @return True if the term is a city, false elsewhere.
     */
    public static boolean isCity(String city){
        synchronized (DBLock) {
            if (citiesInDB.size() < 1 || countriesInDB.size() < 1)
                BuildCitiesAndCountriesFromDB();
        }
        if(city==null || "".equals(city))
            return false;
        boolean ans=false;
        try{
            ans=citiesInDB.containsKey(city.toUpperCase());
        }catch (Exception e){}
        return ans;
    }

    /**
     * Builds citiesInDB from the DB.
     */
    private synchronized static void BuildCitiesAndCountriesFromDB(){
        citiesInDB=DBConnection.GetAllCities();
        countriesInDB=DBConnection.GetAllCountries();
    }

    /**
     * Enters a city in cities as a candidate to be saved, enters a document city as a valid city.
     * @param document A document the cities's values are from.
     */
    public void HandleDocument(List<String> candidateCities, Document document) {
        if (document != null) {
            String city = "";
            try {
                city = document.getTag("City").toUpperCase();
            } catch (Exception e) {}
            if (city != null && !"".equals(city)) {
                String[] citySplit = city.split(" ");
                if (citySplit.length > 1) {
                    if (citySplit[1].charAt(0) >= 'A' && citySplit[1].charAt(0) <= 'Z' &&
                            isCity((citySplit[0] + " " + citySplit[1]).toUpperCase()))
                        city = (citySplit[0] + " " + citySplit[1]).toUpperCase();
                    else city = citySplit[0].toUpperCase();
                }
                synchronized (CTLock) {
                    citiesInTags.add(city);
                }
                synchronized (CCLock) {
                    CitiesIndexer.candidateCities.add(city + "," + document.getName() + ",Tag");
                }
                document.addTag("City", city);
            }
        }
        if(candidateCities!=null)
            synchronized (CCLock) {
                CitiesIndexer.candidateCities.addAll(candidateCities);
            }
    }

    /**
     * Compare cities from the dictionary with the cities in the valid cities set, and remove those
     * who are not matching.
     */
    public static void UpdateDictionary(){
        synchronized (CCLock) {
            synchronized (CTLock) {
                for (String candidate : candidateCities) {
                    String city = candidate.split(",")[0].toUpperCase();
                    if (citiesInTags.contains(city) && citiesInDB.containsKey(city)) {
                        String country, currency, population;
                        country=citiesInDB.get(city);
                        currency=countriesInDB.get(country).getKey();
                        population=countriesInDB.get(country).getValue();
                        CitiesDictionaryEntry cde=new CitiesDictionaryEntry(country,currency,population);
                        if (!dictionary.containsKey(city)) {
                            cde.AddDocument(candidate.split(",")[1] + "," + candidate.split(",")[2]);
                            dictionary.put(city, cde);
                        } else
                            dictionary.get(city).AddDocument(candidate.split(",")[1] + "," + candidate.split(",")[2]);
                    }
                }
            }
        }
    }

    public static void UpdatePopulationIn(TextParser textParser){
        for(CitiesDictionaryEntry cde:dictionary.values()){
            try{
                cde.population=textParser.Parse(cde.population, new Document(""), false).get(0).getName();
            }catch (Exception e){}
        }
    }

    /**
     * Writes the cities's dictionary to a file.
     * the file structure: city||country|currency|population|a document|e.g
     */
    public static void WriteDictionaryToFile() {
        StringBuilder str=new StringBuilder();
        CitiesDictionaryEntry entry;
        for (String city : dictionary.keySet()) {
            entry = dictionary.get(city);
            str.append(city).append("||").append(entry.country).append("|").append(entry.currency).append("|").append(entry.population).append("|");
            for (String doc : entry.documents)
                str.append(doc).append("|");
            str.replace(str.length()-1, str.length(), "");
            str.append(System.getProperty("line.separator"));
        }
        ReadFile.WriteToFile(dictionaryPath + "\\dictionary.txt", str);
        ReadFile.WriteToFile(postingPath + "\\1.txt", str);
    }

    /**
     * Wraps a dictionary entry: for a city, keeps it's country, currency, population and
     * a posting list which the city is in, and where the city is at the posting list (line).
     */
    private static class CitiesDictionaryEntry{
        private String country, currency, population;
        private List<String> documents=new LinkedList<>();

        public CitiesDictionaryEntry(String country, String currency, String population) {
            this.country = country;
            this.currency = currency;
            this.population = population;
        }

        public void AddDocument(String document){
            synchronized (documents){
                documents.add(document);
            }
        }
    }

    /**
     *
     * @return citiesInDB as a sorted set.
     */
    public static TreeSet<String> GetSortedCities(){
        TreeSet<String> ans=new TreeSet<>();
        ans.addAll(dictionary.keySet());
        return ans;
    }

    /**
     * Retrieving the dictionary from the file.
     * the file structure: city|country|currency|population|a document|e.g
     */
    public void ReadDictionaryFromFile(){
        String line, city, country, currency, population;
        List<String> values=new ArrayList<>();
        CitiesDictionaryEntry cde;
        try {
            BufferedReader bw = new BufferedReader(new FileReader(folderPath+"\\CitiesDictionary\\dictionary.txt"));
            while((line=bw.readLine())!=null) {
                GetValues(line, values);
                if(values.size()>3) {
                    city = values.get(0);
                    country = values.get(1);
                    currency = values.get(2);
                    population = values.get(3);
                    cde = new CitiesDictionaryEntry(country, currency, population);
                    for (int i = 4; i < values.size(); i++)
                        cde.documents.add(values.get(i));
                    dictionary.put(city, cde);
                }
            }
            bw.close();
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(4);
        }
    }

    private void GetValues(String sb, List<String> values){
        values.clear();
        int i, j;
        for(i=1, j=0; i<sb.length(); i++)
            if(sb.charAt(i)==sb.charAt(i+1) && sb.charAt(i)=='|') {
                values.add(sb.substring(j, i));
                i+=2;
                j=i;
                break;
            }
        for(;i<sb.length(); i++)
            if(sb.charAt(i)==124) {
                values.add(sb.substring(j, i));
                j=i+1;
            }
        values.add(sb.substring(j, i));
    }

    public static int NumOfCities(){
        return dictionary.size();
    }

    // returns if the city exists
    public static boolean isCityExists(String city){
        return dictionary.containsKey(city);
    }

    // returns the documents names list of a city
    public static List<String> getDocsNamesOfCity(String city){
        return dictionary.get(city).documents;
    }

}
