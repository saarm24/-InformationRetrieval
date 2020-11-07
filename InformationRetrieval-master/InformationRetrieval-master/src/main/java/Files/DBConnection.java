package Files;
/**
 *  Cities and countries database connection class
 */

import javafx.util.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class DBConnection {
    private static Connection conn = null;
    private static String resourcesPath =null;

    /**
     * Creates a connection to the DB
     */
    private synchronized static void Connect() {
        try {
            if(resourcesPath==null) {
                final Class<?> referenceClass = DBConnection.class;
                final URL url =
                        referenceClass.getProtectionDomain().getCodeSource().getLocation();
                resourcesPath = new File(url.toURI()).getParentFile().toString();
                /**/
                resourcesPath=resourcesPath.substring(0, resourcesPath.lastIndexOf('\\'));
                //resourcesPath=resourcesPath.substring(0, resourcesPath.lastIndexOf('\\'));
                /**/
                if (resourcesPath.contains("out"))
                    System.out.println(resourcesPath);
            }
            conn = DriverManager.getConnection("jdbc:sqlite:"+resourcesPath+"\\resources\\CitiesDB.db");
        } catch ( Exception e ) {
            e.printStackTrace();
            System.out.println("Connect");
            System.exit(9);
        }
    }

    private synchronized static void Disconnect(){
        try {
            if(conn!=null && !conn.isClosed())
                conn.close();
        } catch (SQLException e) {
            System.out.println("GetAllCities conn.close()");
            System.exit(9);
        }
    }

    /**
     * Creats the DB for the first time.
     */
    public synchronized static void create(){
        Connect();
        String s1="CREATE TABLE IF NOT EXISTS Cities (\n" +
                "\tCityId int AUTO_INCREMENT NOT NULL ,\n" +
                "\tCountryID smallint NOT NULL ,\n" +
                "\tCity varchar (45) NOT NULL ,\n" +
                "\tPRIMARY KEY(CityId)\n" +
                "\t); \n" +
                "TRUNCATE TABLE Cities;\n",
                s2="CREATE TABLE IF NOT EXISTS Countries (\n" +
                        "\tCountryId smallint AUTO_INCREMENT NOT NULL ,\n" +
                        "\tCountry varchar (50) NOT NULL ,\n" +
                        "\tCapital varchar (25) NULL ,\n" +
                        "\tCurrency varchar (30) NULL ,\n" +
                        "\tCurrencyCode varchar (3) NULL ,\n" +
                        "\tPopulation bigint NULL ,\n" +
                        "\tPRIMARY KEY(CountryId)\n" +
                        "\t);\n" +
                        "TRUNCATE TABLE Countries;\n";
        try {
            PreparedStatement ps1 = conn.prepareStatement(s1);
            PreparedStatement ps2 = conn.prepareStatement(s2);
            ps1.executeUpdate();
            ps2.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        BuildCountries();
        BuildCities();
        Disconnect();
    }

    /**
     *
     * @return A map of all the cities in the DB.
     */
    public synchronized static Map<String, String> GetAllCities(){
        //synchronized (lock) {
        Connect();
        Map<String, String> cities = null;
        String selectQ = "SELECT City, Country\n" +
                "FROM Cities JOIN Countries\n" +
                "ON Cities.CountryID=Countries.CountryID;";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQ)) {
            cities = new HashMap<>(28000);
            while (rs.next()) {
                String city = rs.getString("City"), country=rs.getString("Country");
                city = city.substring(1, city.length() - 1);
                country=country.substring(1, country.length() - 1);
                cities.put(city.toUpperCase(), country.toUpperCase());
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("GetAllCities");
        }
        Disconnect();
        return cities;
        //}
    }

    /**
     * Builds the cities table.
     */
    private synchronized static void BuildCities(){
        Connect();
        try {
            BufferedReader bw =
                    new BufferedReader(new FileReader("resources\\geoworldmap\\cities.txt"));
            String line=bw.readLine(), insertCities =
                    "INSERT INTO Cities(CityId,CountryID,City) VALUES(?,?,?)";
            List<String> lines=new LinkedList<>();
            while((line=bw.readLine())!=null)
                lines.add(line);
            for(String line1:lines){
                PreparedStatement ps=conn.prepareStatement(insertCities);
                String[] values=line1.split(",");
                ps.setString(1, values[0]);
                ps.setString(2, values[1]);
                ps.setString(3, values[3]);
                ps.executeUpdate();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            System.exit(9);
        }
        Disconnect();
    }

    /**
     * Build the coutries table.
     */
    private synchronized static void BuildCountries(){
        try {
            Connect();
            BufferedReader bw =
                    new BufferedReader(new FileReader("resources\\geoworldmap\\countries.txt"));
            String line=bw.readLine(), insertCountries ="INSERT INTO Countries(CountryId," +
                    "Country,Capital,Currency,CurrencyCode,Population) VALUES(?,?,?,?,?,?)";
            List<String> lines=new LinkedList<>();
            while((line=bw.readLine())!=null)
                lines.add(line);
            for(String line1:lines){
                PreparedStatement ps=conn.prepareStatement(insertCountries);
                String[] values=line1.split(",");
                ps.setString(1, values[0]);
                ps.setString(2, values[1]);
                ps.setString(3, values[7]);
                ps.setString(4, values[11]);
                ps.setString(5, values[12]);
                ps.setString(6, values[13]);
                ps.executeUpdate();
            }
        } catch (IOException | SQLException e) {
            System.out.println("BuildCountries");
            System.exit(9);
        }
        Disconnect();
    }

    /**
     * Get a city's data from the DB.
     * @param city The city to search for.
     * @return CitiesDictionaryEntry - country, currency, population.
     */
    public synchronized static List<String> readCity(String city){
        //synchronized (lock) {
        Connect();
        List<String> ans=new ArrayList<>(3);
        String selectQ = "SELECT Country, Currency, Population\n" +
                "FROM Countries JOIN(SELECT CountryID \n" +
                "FROM Cities\n" +
                "WHERE City LIKE '\"" + city.toUpperCase() + "\"') AS T\n" +
                "ON Countries.CountryID=T.CountryID;";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQ)) {
            ans.add(rs.getString("Country"));
            ans.add(rs.getString("Currency"));
            ans.add(rs.getString("Population"));
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println("readCity");
        }
        Disconnect();
        return ans;
        //}
    }

    /**
     *
     * @return A map of countries to it's currency and population (as a pair).
     */
    public synchronized static Map<String,Pair<String, String>> GetAllCountries() {
        //synchronized (lock) {
        Connect();
        Map<String,Pair<String, String>> countries = null;
        String selectQ = "SELECT Country, Currency, Population\n" +
                "FROM Countries;";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQ)) {
            countries = new HashMap<>(275);
            while (rs.next()) {
                String country = rs.getString("Country"), currency=rs.getString("Currency"),
                        population=rs.getString("Population");
                country = country.substring(1, country.length() - 1).toUpperCase();
                currency = currency.substring(1, currency.length() - 1).toUpperCase();
                population = population.substring(1, population.length() - 1).toUpperCase();
                countries.put(country, new Pair<>(currency,population));
            }
        } catch (SQLException e) {
            //System.out.println(e.getMessage());
            System.out.println("GetAllCountries");
            System.exit(9);
        }
        Disconnect();
        return countries;
        //}
    }

    public synchronized static Set<String> GetCapitals(){
        Connect();
        HashSet<String> capitals = null;
        String selectQ = "SELECT Capital\n" +
                "FROM Countries;";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQ)) {
            capitals = new HashSet<>(275);
            while (rs.next()) {
                String capital=rs.getString("Capital");
                capital = capital.substring(1, capital.length() - 1).toUpperCase();
                capitals.add(capital);
            }
        } catch (SQLException e) {
            //System.out.println(e.getMessage());
            System.out.println("GetAllCountries");
            System.exit(9);
        }
        Disconnect();
        return capitals;
    }
}