package Parser;
import General.Document;
import General.DocEntity;
import General.Term;
import Index.CitiesIndexer;
import Stemmer.Stemmer;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class TextParser {
    // List of all the original words of the text
    private List<String> originalWords = new ArrayList<String>(Arrays.asList("", " ", null));
    private static String stopWordsPath;
    private Map<String, Integer> MonthMap;
    // map between number and fit LETTER
    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
    private List<String> CityList = new ArrayList<>();
    // counter = the place of the current word of the originalWords
    private int counter = -1;
    private int endOfList = originalWords.size();
    static {
        suffixes.put(1_000L, "K");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "B");
    }
    private static int MrCounter = 0;


    private Stemmer st = new Stemmer();

    private synchronized static int getMrCounter() {
        return MrCounter++;
    }

    public TextParser(String stopWordsPath) {
        UpdateDates();
        TextParser.stopWordsPath = stopWordsPath;
    }

    private void UpdateDates() {
        MonthMap = new HashMap<>();
        MonthMap.put("January", 1);
        MonthMap.put("JANUARY", 1);
        MonthMap.put("Jan", 1);
        MonthMap.put("JAN", 1);
        MonthMap.put("February", 2);
        MonthMap.put("FEBRUARY", 2);
        MonthMap.put("Feb", 2);
        MonthMap.put("FEB", 2);
        MonthMap.put("March", 3);
        MonthMap.put("MARCH", 3);
        MonthMap.put("Mar", 3);
        MonthMap.put("MAR", 3);
        MonthMap.put("April", 4);
        MonthMap.put("APRIL", 4);
        MonthMap.put("Apr", 4);
        MonthMap.put("APR", 4);
        MonthMap.put("May", 5);
        MonthMap.put("MAY", 5);
        MonthMap.put("May", 5);
        MonthMap.put("MAY", 5);
        MonthMap.put("June", 6);
        MonthMap.put("JUNE", 6);
        MonthMap.put("Jun", 6);
        MonthMap.put("JUN", 6);
        MonthMap.put("July", 7);
        MonthMap.put("JULY", 7);
        MonthMap.put("Jul", 7);
        MonthMap.put("JUL", 7);
        MonthMap.put("August", 8);
        MonthMap.put("AUGUST", 8);
        MonthMap.put("Aug", 8);
        MonthMap.put("AUG", 8);
        MonthMap.put("September", 9);
        MonthMap.put("SEPTEMBER", 9);
        MonthMap.put("Sep", 9);
        MonthMap.put("SEP", 9);
        MonthMap.put("October", 10);
        MonthMap.put("OCTOBER", 10);
        MonthMap.put("Oct", 10);
        MonthMap.put("OCT", 10);
        MonthMap.put("November", 11);
        MonthMap.put("NOVEMBER", 11);
        MonthMap.put("Nov", 11);
        MonthMap.put("NOV", 11);
        MonthMap.put("December", 12);
        MonthMap.put("DECEMBER", 12);
        MonthMap.put("Dec", 12);
        MonthMap.put("DEC", 12);
    }

    // Make a list of all the original words of the text
    private List<String> cutWordsFromText(String input) {
        String[] words = input.split(" ");
        for (String word : words) {
            originalWords.add(word);
        }
        originalWords.removeAll(Arrays.asList("", " ", null));
        return originalWords;
    }

    //DATE FUNCTIONS

    //  checks if its a date
    private int MonthDateCheckMap(String word) {
        if (MonthMap.containsKey(word))
            return MonthMap.get(word);
        return -1;
    }

    // if the first word is a month , and the second word is a number
    private String addMonthAndYear(String Month, String year) {
        int month = MonthDateCheckMap(Month);
        String temp;
        if (month <= 9)
            temp = year + "-0" + month;
        else
            temp = year + "-" + month;
        return temp;
    }

    // if the first word is a day , and the second word is a month
    private String addDayAndMonth(String day, String month) {
        int curmonth = MonthDateCheckMap(month);
        if (day.length() <= 1)
            day = "0" + day;
        String temp;
        if (curmonth <= 9)
            temp = "0" + curmonth + "-" + day;
        else
            temp = curmonth + "-" + day;
        return temp;
    }

    // checks if the day is in the range of 31 days in a month
    private boolean DateDayRange(String number) {
        // illegal number
        if (isTherePsi(number))
            return false;
        // illegal number
        if (isThereDot(number))
            return false;
        if (isThereHyphen(number))
            return false;
        long num = Long.parseLong(number);
        return num < 32;
    }

    //NUMBER FUNCTIONS

    private boolean isANumber(String word) {
        try {
            Number number = NumberFormat.getInstance().parse(word);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // check for " , " in the string
    private boolean isTherePsi(String num) {
        for (int i = 0; i < num.length(); i++) {
            if (num.contains(","))
                return true;
        }
        return false;
    }

    // check for " . " in the string
    private boolean isThereDot(String num) {
        for (int i = 0; i < num.length(); i++) {
            if (num.contains("."))
                return true;
        }
        return false;
    }

    // get 129,000,000 return 129000000
    private String cleanNumberFromPsik(String number) {
        String ans = "";
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) != ',')
                ans += number.charAt(i);
        }
        return ans;
    }

    private String cleanNumberFromDot(String number) {
        String ans = "";
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) != '.')
                ans += number.charAt(i);
        }
        return ans;
    }

    private String range(String number) {
        String temp = number;
        if (isTherePsi(number))
            // get the number without ' , '
            temp = cleanNumberFromPsik(number);

        if (isThereDot(temp))
            temp = deleteNumberFromDot(temp);
        double numm = Double.parseDouble(temp);
        long num;
        try {
            num = Long.parseLong(temp);
        } catch (NumberFormatException n) {
            return number;
        }


        // System.out.println("line 210:"+temp);
        if (num < 1000)
            return "BelowTho";
        else if (num >= 1000 && num < 1000000)
            return "BelowMil";
        else if (num >= 1000000 && num < 1000000000)
            return "BelowBil";
        else
            return "AboveBil";

    }

    private String deleteNumberFromDot(String number) {
        String ans = "";
        if (number.charAt(0) == '.')
            return number.substring(1);
        for (int i = 0; number.charAt(i) != '.'; i++) {
            ans += number.charAt(i);
        }
        return ans;
    }

    // get a string and returns a long number
    private long getNumLong(String number) {
        String temp = number;
        if (isTherePsi(number))
            // get the number without ' , '
            temp = cleanNumberFromPsik(number);
        //NEED TO CHECK THE NEXT 2 LINES
        if (isThereDot(number))
            temp = cleanNumberFromDot(temp);
        return Long.parseLong(temp);
    }

    private String numberFormat(long value) {
        if (value < 1000)
            return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();

        String suffix = e.getValue();
        String ans;
        ans = Double.toString(((double) value) / divideBy) + suffix;
        int i;
        String tempAnsf = "", tempAnsSec = "";
        for (i = 0; ans.charAt(i) != '.'; i++) {
            tempAnsf += ans.charAt(i);
        }
        for (int k = i + 1; k < ans.length(); k++) {
            tempAnsSec += ans.charAt(k);
        }

        if (tempAnsSec.length() == 2 && tempAnsSec.charAt(0) == '0') {
            ans = tempAnsf + tempAnsSec.charAt(1);
        }
        ans = twoPointNum(ans);
        return ans;
    }

    // percent % functions
    // checks if its like 56% form
    private Boolean isTherePer(String word) {
        return word.contains("%");
    }

    // checks if its like 56% form
    private Boolean isNextWordPerType(String NextWord) {
        return NextWord.equals("percentage") || NextWord.equals("percent");
    }

    // RETURN the string after conversions to percent type string
    private String WorkWithPer(String word) {
        if (isTherePer(word))
            return word;
        else if (isANumber(word) && isNextWordPerType(getNextWord()))
            return word + "%";
        else {
            counter--;
            return "NotPer";
        }
    }

    // from 1010.56 to 1.01056K
    private String convertWithDotThou(String number) {
        number = cleanNumberFromPsik(number);
        String ans = "";
        int curIndex = 0;
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) == '.') {
                curIndex = i;
                break;
            }
        }

        if (curIndex <= 3)
            return number;

        int curIndexUpdate;
        curIndexUpdate = curIndex - 3;
        for (int j = 0; j < number.length(); j++) {
            if (j == curIndex)
                continue;
            if (j == curIndexUpdate) {
                ans += ".";
                ans += number.charAt(j);
            } else
                ans += number.charAt(j);
        }

        ans = twoPointNum(ans);

        return ans + "K";
    }

    // from 1012235.6 to 1.0122356M
    private String convertWithDotMil(String number) {
        if (isTherePsi(number))
            number = cleanNumberFromPsik(number);
        String ans = "";
        int curIndex = 0;
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) == '.') {
                curIndex = i;
                break;
            }
        }
        int curIndexUpdate;
        curIndexUpdate = curIndex - 6;
        for (int j = 0; j < number.length(); j++) {
            if (j == curIndex)
                continue;
            if (j == curIndexUpdate) {
                ans += ".";
                ans += number.charAt(j);
            } else
                ans += number.charAt(j);
        }
        ans = twoPointNum(ans);
        return ans + "M";
    }

    // from 1012235.6 to 1.0122356M
    private String convertWithDotBil(String number) {
        String ans = "";
        int curIndex = 0;
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) == '.') {
                curIndex = i;
                break;
            }
        }
        int curIndexUpdate;
        curIndexUpdate = curIndex - 9;
        for (int j = 0; j < number.length(); j++) {
            if (j == curIndex)
                continue;
            if (j == curIndexUpdate) {
                ans += ".";
                ans += number.charAt(j);
            } else
                ans += number.charAt(j);
        }
        ans = twoPointNum(ans);

        return ans + "B";
    }

    // Dollar functions
    // less than 1,000,000 dollars

    // if true need to send it without the $ sign to printDolPriceCorrect
    private boolean isItStartWithDollarSign(String word) {
        if (word.length() == 0 || word.length() == 1)
            return false;
        return word.charAt(0) == '$';
    }

    // checks if there is a / in the string
    private boolean isThereFrac(String word) {
        return word.contains("/");
    }

    // need to get the word from deleteDolSignFromNumber and check
    private boolean isPriceBelowMill(String word) {
        return word.length() <= 7;
    }

    // like 56 million / billion / trillion
    private boolean isSecWordAfterPriceIsAmount(String word) {
        return word.equals("million") || word.equals("billion") || word.equals("trillion");
    }

    // 17 million will be 17 M
    private String convertToFitEndBelowThousand(long num, String word) {
        return Long.toString(num);
    }

    // 1000 thousand will be 1M
    private String convertToFitEndBelowMil(long num, String word) {
        String ans = "";
        if (word.equals("million") || word.equals("Million")) {
            num = num * 1000000;
            ans = numberFormat(num);
        } else if ((word.equals("billion") || word.equals("bn"))) {
            num = num * 1000000000;
            ans = numberFormat(num);
        } else if (word.equals("trillion") || word.equals("Trillion")) {
            num = num * 1000000;
            num = num * 1000000;
            ans = numberFormat(num);
        } else if (word.equals("thousand") || word.equals("Thousand")) {
            num = num * 1000;
            ans = numberFormat(num);
        }
        return ans;
    }

    // 17 million will be 17 M
    private String convertToFitEndDol(long num, String word) {
        if (word.equals("million"))
            num = num;
        else if ((word.equals("billion") || word.equals("bn"))) {
            num = num * 1000;
        } else if (word.equals("trillion")) {
            num = num * 1000000;
        }
        return Long.toString(num);
    }

    // 17 million will be 17 M
    private String convertToFitEndDolDou(double num, String word) {
        if (word.equals("million"))
            num = num;
        else if ((word.equals("billion") || word.equals("bn"))) {
            num = num * 1000;
        } else if (word.equals("trillion")) {
            num = num * 1000000;
        }
        return Double.toString(num);
    }

    // get $190 or $999,000 or $1,230,000 and print it right
    private String printDolPriceCorrect(String word) {
        if (isItStartWithDollarSign(word)) {
            word = word.substring(1);
        }
        if (isPriceBelowMill(word))
            return word + " Dollars";
        String cleanWord = cleanNumberFromPsik(word);
        //delete the $sign
        double num = Double.parseDouble(cleanWord);
        Long numt = (long) num;
        String ans = numberFormat(numt);
        String correctLetter = ans.substring(ans.length() - 1);
        if (correctLetter.equals("K"))
            return word + " Dollars";

        return ans.substring(0, ans.length() - 1) + " " + correctLetter + " Dollars";
    }

    // for checking strings like $2.980.77
    private int howManyDot(String st) {
        int cou = 0;
        for (int i = 0; i < st.length(); i++) {
            if (st.charAt(i) == '.')
                cou++;
        }
        return cou;
    }

    private boolean IsItStopWordHash(String input) {
        if (stopWordsPath == null)
            return false;
        DBInfo dbInfo = DBInfo.instance();
        return dbInfo.stopWordsHash.contains(input);
    }

    // RANGE WORDS
    private boolean isThereHyphen(String str) {
        return str.contains("-");
    }

    // add the words between the hyphens to an array
    private List divideFromHyphen(String str) {
        int lastHyphen = 0;
        List<String> hyphenString = new ArrayList<>();
        // counter the amount of hyphens in the string
        for (int k = 0; k < str.length(); k++) {
            if (str.charAt(k) == '-') {
                String toAdd = str.substring(lastHyphen, k);
                hyphenString.add(toAdd);
                lastHyphen = k + 1;
            }
        }
        hyphenString.add(str.substring(lastHyphen, str.length()));
        return hyphenString;
    }

    // convert list of words to one string
    private String ConvertListToStr(List<String> lis) {
        return String.join(" ", lis);
    }

    //TWO MORE RULES
    // checks if there is . , ; ? in the end of the word
    private boolean isThereSignEnd(String str) {
        if (str.length() == 0 || str.length() == 1)
            return false;
        return (str.charAt(str.length() - 1) == '.') || (str.charAt(str.length() - 1) == ',') ||
                (str.charAt(str.length() - 1) == '!') || (str.charAt(str.length() - 1) == ';')
                || (str.charAt(str.length() - 1) == '?') ||
                (str.charAt(str.length() - 1) == ':');
    }

    //clean the string from the sign in the end of it
    private String cleanWordFromEndingSign(String word) {
        int k = 0;
        for (int i = word.length() - 1; i >= 0; i--) {
            if (word.charAt(i) == '.' || word.charAt(i) == '`' || word.charAt(i) == ')' || word.charAt(i) == ']' || word.charAt(i) == '-')
                k++;
            else
                break;
        }
        return word.substring(0, word.length() - k);
    }

    // checks if word is like [Bor] or (Bor)
    private boolean isWordStartsAndOrEndWithSog(String word) {
        return word.length() != 0 && word.length() != 1 && (((word.charAt(0) == '[') || (word.charAt(word.length() - 1) == ']')) || ((word.charAt(0) == '(') || (word.charAt(word.length() - 1) == ')')));
    }

    // like [BOG] return BOG
    private String cleanWordFromSog(String word) {
        if ((word.charAt(0) == '[' && word.charAt(word.length() - 1) == ']') || (word.charAt(0) == '(' && word.charAt(word.length() - 1) == ')'))
            word = word.substring(1, word.length() - 1);

        if ((word.charAt(0) == '[') || (word.charAt(0) == '('))
            word = word.substring(1);
        if ((word.charAt(word.length() - 1) == ']') || (word.charAt(word.length() - 1) == ')'))
            word = word.substring(0, word.length() - 1);

        return word;
    }

    //GET NEXT WORD TO WORK WITH
    // return the next substring
    private String getNextWord() {
        counter++;
        if (counter < endOfList) {
            String ans = originalWords.get(counter);
            if (ans.length() > 2 && ans.charAt(0) == '(' && ans.charAt(ans.length() - 1) == '.')
                ans = ans.substring(1, ans.length() - 2);

            if (ans.length() == 2 && ans.charAt(0) == '(' && ans.charAt(ans.length() - 1) == '.')
                ans = ans.substring(1, ans.length() - 1);
            int s = ans.length();
            for (int k = 0; k < s; k++) {
                if (isWordBeginWithTrash(ans))
                    ans = ans.substring(1);
                else if (!isWordBeginWithTrash(ans))
                    break;
            }
            s = ans.length();
            // System.out.println("ans length:"+ans.length());
            for (int k = 0; k < s; k++) {
                if (isWordEndsWithTrash(ans))
                    ans = ans.substring(0, ans.length() - 1);
                else if (!isWordEndsWithTrash(ans))
                    break;
            }
            return ans;
        }
        return "";
    }

    public List<Term> Parse(String input, Document document, boolean ToStem) {
        if (input == null)
            return null;
        originalWords.clear();
        counter = -1;
        List<String> lo = new ArrayList<>();
        List<String> tokk = new ArrayList<>();
        Map<String, Integer> ParserSet = new HashMap<String, Integer>();
        ParserSet.clear();

        FirstParse(input, ParserSet, lo, document, ToStem);
        if (lo.size() > 0) {
            // function that get a list of words and convert it to string
            String toSend = "";
            toSend = ConvertListToStr(lo);
            toSend = DeleteProblemsWords(toSend);
            FirstParse(toSend, ParserSet, tokk, document, ToStem);
        }
        List<Term> ParserTerms = new ArrayList<>();
        Iterator<Map.Entry<String, Integer>> iter = ParserSet.entrySet().iterator();
        int MaxFre = 1, DocumentLength = 0;
        String mostFrequenWord="";
        List<DocEntity> docEntityParse = new ArrayList<>();

        while (iter.hasNext()) {
            Map.Entry<String, Integer> entry = iter.next();
            Term InsertTerm = new Term(entry.getKey());
            if (entry.getKey().length() == 1)
                continue;
            InsertTerm.setCount(entry.getValue());

            InsertTerm.setJunk(isThereJunkAfterParsing(entry.getKey()));
            InsertTerm.setCap(isCapital(entry.getKey()));
            ParserTerms.add(InsertTerm);

            // firstUpdate the entities list, no formula firstUpdate yet - just the tf
            if (isCapital(entry.getKey())) {
                docEntityParse.add(new DocEntity(entry.getKey(), (double) entry.getValue()));
            }

            // firstUpdate MaxFrequency
            if (entry.getValue() > MaxFre) {
                MaxFre = entry.getValue();
                mostFrequenWord=entry.getKey();
            }
            // firstUpdate the length of the document
            DocumentLength += entry.getValue();
        }
        // firstUpdate the value of the entity and add to the document entities list
        for (DocEntity ent : docEntityParse) {
            ent.setValue(ent.getValue() / DocumentLength);
        }
        //sorting the Entities by size of formula
        docEntityParse.sort(Comparator.comparing(DocEntity::getValue).reversed());
        for (DocEntity ent : docEntityParse) {
            //add the entity to the document
            document.AddEntity(ent);
        }
        document.setMaxTf(MaxFre);
        document.setTf(DocumentLength);
        document.setUniqueCount(ParserSet.size());
        document.setMostFrquenWord(mostFrequenWord);
        //System.out.println("document:"+document.getMostFrquenWord());
        //System.out.println("most frq word is:"+mostFrequenWord);
        UpdateVectorSize(ParserTerms, document);
        return ParserTerms;
    }

    private void UpdateVectorSize(List<Term> terms, Document document) {
        double size = 0;
        for (Term term : terms)
            size += Math.pow((double) (term.getCount() / document.getMaxTf()), 2);
        document.setVectorSize(Math.sqrt(size));
    }

    private boolean isCapital(String word) {
        return word.length() > 1 && word.charAt(0) >= 'A' && word.charAt(0) <= 'Z';
    }

    private String DeleteProblemsWords(String toSend) {
        String ans = "";
        for (int i = 0; i < toSend.length(); i++) {
            if (toSend.charAt(i) != '-') {
                ans += toSend.charAt(i);
            }
            if (toSend.charAt(i) == '-') {
                ans += " ";
            }
        }
        return ans;
    }

    //private void FirstParse(String input,List<String> Tokens,List<String> secRound){
    private void FirstParse(String input, Map<String, Integer> Tokens, List<String> secRound, Document doc, boolean ToStem) {
        //get a list of words
        cutWordsFromText(input);
        int sizeOfOriginal = originalWords.size();
        //counter = 0;
        endOfList = sizeOfOriginal;
        while (counter < sizeOfOriginal - 1) {
            String current = getNextWord();
            // ignore 1 letter words
            if (current.length() == 1)
                continue;
            // checking if word begins and ends with [] ()
            if (isWordStartsAndOrEndWithSog(current)) {
                current = cleanWordFromSog(current);
            }

            if (isWordIsTrash(current)) {
                continue;
            }

            if (isThereTwoConseHyphen(current)) {
                List<String> temp = new ArrayList<>();
                temp = WorkWithTwoConseHyphen(current);
                secRound.addAll(temp);
                continue;
            }


            // checking if there is a . , ; ? in the end of the word
            if (isThereSignEnd(current)) {
                current = cleanWordFromEndingSign(current);
            }

            // CHECK FOR BETWEEN 8 AND 24
            if (current.equals("between") || current.equals("Between")) {
                String nexf = getNextWord();
                String nexs = getNextWord();
                String next = getNextWord();
                //ok found the pattern of between 8 and 24
                if (isANumber(nexf) && isANumber(next) && nexs.equals("and")) {
                    secRound.add(nexf);
                    secRound.add(next);
                    String ans = current + " " + nexf + " " + nexs + " " + next;
                    addToMap(ans, Tokens, ToStem, doc);
                    continue;
                } else {
                    counter = counter - 3;
                }
            }

            // First Priority - delete stop words
            if (IsItStopWordHash(current.toLowerCase())) {
                continue;
            }
            // NOT A STOP WORD
            else {
                // check if the word is a number
                if (isANumber(current)) {
                    if (isThereLettersInNum(current) && !isThereHyphen(current)) {
                        addToMap(current, Tokens, ToStem, doc);
                        continue;
                    }

                    if (isThereFrac(current)) {
                        addToMap(current, Tokens, ToStem, doc);
                        continue;
                    }

                    // Check for 16-24
                    if (isThereHyphen(current)) {
                        List<String> hyphendivid  = divideFromHyphen(current);
                        int size = hyphendivid.size();
                        String toAdd = "";
                        // like word-word
                        if (size == 2) {
                            toAdd += hyphendivid.get(0);
                            toAdd += "-";
                            toAdd += hyphendivid.get(1);
                            secRound.add(hyphendivid.get(0));
                            secRound.add(hyphendivid.get(1));
                            addToMap(toAdd, Tokens, ToStem, doc);
                            continue;
                        }
                        // like 2.5-million-jank-slp
                        if (size > 2) {
                            for (int j = 0; j < size; j++) {
                                secRound.add(hyphendivid.get(j));
                            }
                            continue;
                        }
                    }

                    // check if it is a PERCENT TYPE of number
                    String checkPer = WorkWithPer(current);

                    if (!checkPer.equals("NotPer")) {
                        addToMap(checkPer, Tokens, ToStem, doc);
                        continue;
                    }

                    // CHECKS FOR DOLLARS TYPE NUMBER
                    String theNextWord = getNextWord();
                    counter = counter - 1;

                    //check for 100 billion U.S. dollars
                    if (theNextWord.equals("billion") || theNextWord.equals("million") ||
                            theNextWord.equals("trillion")) {
                        counter = counter + 1;
                        String thirdWord = getNextWord();
                        String forthWord = getNextWord();
                        // found 100 billion U.S. dollars
                        if (thirdWord.equals("U.S.") && forthWord.equals("dollars")) {
                            long num = getNumLong(current);
                            String ans = convertToFitEndDol(num, theNextWord);
                            ans = twoPointNum(ans);
                            ans = ans + " M Dollars";
                            addToMap(ans, Tokens, ToStem, doc);
                            continue;
                        }
                        counter = counter - 3;
                    }

                    // check for 28 m Dollars
                    // m or bn
                    if (theNextWord.equals("m") || theNextWord.equals("bn")) {
                        counter = counter + 1;
                        String thirdWord = getNextWord();
                        // we found 28 bn Dollars type
                        if (thirdWord.equals("Dollars")) {
                            if (theNextWord.equals("m")) {
                                current = twoPointNum(current);
                                addToMap(current + " M Dollars", Tokens, ToStem, doc);
                                continue;
                            }
                            // bn
                            else {
                                long num = getNumLong(current);
                                String ans = convertToFitEndDol(num, theNextWord);
                                ans = twoPointNum(ans);
                                ans = ans + " M Dollars";
                                addToMap(ans, Tokens, ToStem, doc);
                                continue;
                            }
                        }
                        counter = counter - 2;
                    }

                    // check for 22 3/4 Dollars
                    if (isThereFrac(theNextWord) && isANumber(theNextWord) && !isThereLettersInNum(theNextWord)) {
                        counter = counter + 1;
                        String tempNextWord = getNextWord();
                        // means we found 22 3/4 Dollars
                        if (tempNextWord.equals("Dollars")) {
                            current = twoPointNum(current);
                            String ans = current + " " + theNextWord + " " + tempNextWord;
                            secRound.add(theNextWord);
                            secRound.add(tempNextWord);
                            secRound.add(current);
                            addToMap(ans, Tokens, ToStem, doc);
                            continue;
                        }

                        // like 22 4/5
                        counter = counter - 1;
                        secRound.add(theNextWord);
                        current = twoPointNum(current);
                        secRound.add(current);
                        addToMap(current + " " + theNextWord, Tokens, ToStem, doc);
                        continue;
                    }

                    // like 49 Dollars
                    if (theNextWord.equals("Dollars")) {
                        counter = counter + 1;
                        long num = getNumLong(current);
                        if (num < 1000000) {
                            current = twoPointNum(current);
                            String ans = current + " Dollars";
                            addToMap(ans, Tokens, ToStem, doc);
                            continue;
                        }
                        //like 1000000 Dollars
                        else {
                            num = num / 1000000;
                            String ans = num + " M Dollars";
                            //  Tokens.add(ans);
                            addToMap(ans, Tokens, ToStem, doc);
                            continue;
                        }
                    }

                    // CHECKS OVER NORMAL NUMBER
                    String numRange = range(current);
                    // checking for normal number below 1000
                    if (numRange.equals("BelowTho")) {
                        theNextWord = getNextWord();
                        //DATE CHECK
                        // CHECKING FOR DATE FORMAT LIKE 29 MAY
                        if (MonthDateCheckMap(theNextWord) != -1 && DateDayRange(current)) {
                            // System.out.println("##### DATE CHECK GOOD");
                            String ans = addDayAndMonth(current, theNextWord);
                            // Tokens.add(ans);
                            addToMap(ans, Tokens, ToStem, doc);
                            continue;
                        }

                        // like 20 dogs
                        if (!isANumber(theNextWord) && !((theNextWord.equals("Thousand") || theNextWord.equals("Million") ||
                                theNextWord.equals("Billion") ||
                                theNextWord.equals("Trillion") || theNextWord.equals("trillion")
                                || theNextWord.equals("thousand") || theNextWord.equals("billion") ||
                                theNextWord.equals("million")))) {
                            counter = counter - 1;
                            current = twoPointNum(current);
                            addToMap(current, Tokens, ToStem, doc);
                            continue;
                        }
                        // the next word after the number is also a number or a size - like 24 2/3
                        else {
                            //beacuse we did getNextWord before
                            counter--;
                            // 28 2/3
                            if (isThereFrac(theNextWord)) {
                                String addWord = current + " " + theNextWord;
                                addToMap(addWord, Tokens, ToStem, doc);
                                continue;
                            }
                            if (theNextWord.equals("Thousand") || theNextWord.equals("thousand") ||
                                    theNextWord.equals("Million") || theNextWord.equals("million") ||
                                    theNextWord.equals("Billion") || theNextWord.equals("billion") ||
                                    theNextWord.equals("Trillion") || theNextWord.equals("trillion")) {
                                String answ = convertToFitEndBelowThousand(getNumLong(current), theNextWord);
                                answ = twoPointNum(answ);
                                if (theNextWord.equals("Thousand") || theNextWord.equals("thousand")) {
                                    addToMap(answ + "K", Tokens, ToStem, doc);
                                    counter++;
                                    continue;
                                }

                                if (theNextWord.equals("Billion") || theNextWord.equals("billion")) {
                                    addToMap(answ + "B", Tokens, ToStem, doc);
                                    counter++;
                                    continue;
                                }
                                if (theNextWord.equals("Trillion") || theNextWord.equals("trillion")) {
                                    addToMap(answ + "00" + "B", Tokens, ToStem, doc);
                                    counter++;
                                    continue;
                                }

                                addToMap(answ + "M", Tokens, ToStem, doc);
                                counter++;
                                continue;
                            }
                        }
                        addToMap(current, Tokens, ToStem, doc);
                        continue;
                    }
                    //NUMBER IN RANGE OF 1000-1000000
                    else if (numRange.equals("BelowMil")) {
                        String NextWor = getNextWord();
                        // LIKE 240,000 Thousand
                        if (NextWor.equals("Thousand") || NextWor.equals("Million") ||
                                NextWor.equals("Billion") ||
                                NextWor.equals("Trillion")) {
                            String answ = convertToFitEndBelowMil(getNumLong(current), NextWor);
                            answ = twoPointNum(answ);
                            addToMap(answ, Tokens, ToStem, doc);
                            continue;
                        }
                        // because we did getNext
                        counter--;
                        String ne = getNextWord();
                        // like 180,000
                        if (!isANumber(ne) || !isThereFrac(ne)) {
                            counter = counter - 1;
                            // if the number is with dot like 1010.56
                            if (!isThereDot(current)) {
                                // convert the string to long number
                                long numbe = getNumLong(current);
                                // convert to the fit type of string
                                String ans = numberFormat(numbe);
                                addToMap(ans, Tokens, ToStem, doc);
                                continue;
                            }

                            // if the number is like 1010.56
                            else if (isThereDot(current)) {
                                current = cleanNumberFromPsik(current);
                                addToMap(convertWithDotThou(current), Tokens, ToStem, doc);
                                continue;
                            }
                        }
                        long numbe = getNumLong(current);
                        // convert to the fit type of string
                        String ans = numberFormat(numbe);
                        addToMap(ans, Tokens, ToStem, doc);
                        counter = counter - 1;
                        continue;
                    }


                    //NUMBER IN RANGE OF 1000000-1000000000
                    else if (numRange.equals("BelowBil")) {
                        // LIKE 240,000 Thousand
                        String NextWor = getNextWord();
                        // LIKE 240,000 Thousand
                        if (NextWor.equals("Thousand") || NextWor.equals("Million") ||
                                NextWor.equals("Billion") ||
                                NextWor.equals("Trillion")) {

                            String answ = convertToFitEndBelowMil(getNumLong(current), NextWor);
                            answ = twoPointNum(answ);
                            addToMap(answ, Tokens, ToStem, doc);
                            continue;
                        }
                        // because we did getNext
                        counter--;
                        // like 180,000,000
                        if (!isANumber(getNextWord())) {
                            counter = counter - 1;
                            // if the number is with dot like 1010.56
                            if (!isThereDot(current)) {
                                // convert the string to long number
                                long numbe = getNumLong(current);
                                // convert to the fit type of string
                                String ans = numberFormat(numbe);
                                addToMap(ans, Tokens, ToStem, doc);
                                continue;
                            }
                            // if the number is like 1010.56
                            else {
                                addToMap(convertWithDotMil(current), Tokens, ToStem, doc);
                                continue;
                            }
                        }
                    }

                    //NUMBER IN RANGE above 1000000000
                    else if (numRange.equals("AboveBil")) {
                        // LIKE 240,000,000,000 Thousand
                        String NextWor = getNextWord();
                        // LIKE 240,000 Thousand
                        if (NextWor.equals("Thousand") || NextWor.equals("Million") ||
                                NextWor.equals("Billion") ||
                                NextWor.equals("Trillion")) {
                            String answ = convertToFitEndBelowThousand(getNumLong(current), NextWor);
                            addToMap(answ, Tokens, ToStem, doc);
                            continue;
                        }
                        // because we did getNext
                        counter--;
                        // like 180,000,000
                        if (!isANumber(getNextWord())) {
                            counter = counter - 1;
                            // if the number is with dot like 101535340.56
                            if (!isThereDot(current)) {
                                long numbe = getNumLong(current);
                                String ans = numberFormat(numbe);
                                addToMap(ans, Tokens, ToStem, doc);
                                continue;
                            }
                            // if the number is like 1010.56
                            else {
                                addToMap(convertWithDotBil(current), Tokens, ToStem, doc);
                                continue;
                            }
                        }
                    }
                    // number is larger than long number
                    else {
                        addToMap(current, Tokens, ToStem, doc);
                        continue;
                    }
                }
                // not a number
                else {
                    // check for hyphen
                    // yes there is " - "
                    if (isThereHyphen(current)) {
                        List<String> hyphendivid = new ArrayList<>();
                        hyphendivid = divideFromHyphen(current);
                        int size = hyphendivid.size();
                        String toAdd = "";
                        // like word-word
                        if (size == 2) {
                            toAdd += hyphendivid.get(0);
                            toAdd += "-";
                            toAdd += hyphendivid.get(1);
                            secRound.add(hyphendivid.get(0));
                            secRound.add(hyphendivid.get(1));
                            addToMap(toAdd, Tokens, ToStem, doc);
                            continue;
                        }
                        // like word-word-word
                        else if (size == 3) {
                            toAdd += hyphendivid.get(0);
                            toAdd += "-";
                            toAdd += hyphendivid.get(1);
                            toAdd += "-";
                            toAdd += hyphendivid.get(2);
                            addToMap(toAdd, Tokens, ToStem, doc);
                            secRound.add(hyphendivid.get(0));
                            secRound.add(hyphendivid.get(1));
                            secRound.add(hyphendivid.get(2));
                            continue;
                        }
                    }

                    //DOLLAR CHECK
                    boolean dollarOrNot = isItStartWithDollarSign(current);
                    //begin with dollar sign
                    if (dollarOrNot) {
                        // like $5pp
                        if (isThereLettersInNum(current.substring(1)) || howManyDot(current.substring(1)) > 1) {
                            addToMap(current, Tokens, ToStem, doc);
                            addToMap(current.substring(1), Tokens, ToStem, doc);
                            continue;
                        }

                        String nexW = getNextWord();
                        // like $180,000
                        if (!isSecWordAfterPriceIsAmount(nexW)) {
                            counter = counter - 1;
                            String ans = printDolPriceCorrect(current);
                            addToMap(ans, Tokens, ToStem, doc);
                            continue;
                        }
                        // like 20 million
                        else if (isSecWordAfterPriceIsAmount(nexW)) {
                            String ans = cleanNumberFromPsik(current.substring(1));
                            Double num = Double.parseDouble(ans);
                            ans = convertToFitEndDolDou(num, nexW);
                            ans = ans + " M Dollars";
                            addToMap(ans, Tokens, ToStem, doc);
                            continue;
                        }
                    }

                    // DATE CHECK
                    int isItDate = MonthDateCheckMap(current);
                    // it is a month
                    if (isItDate != -1) {
                        String nextWord = getNextWord();
                        if (isThereLettersInNum(nextWord)) {
                            addToMap(current, Tokens, ToStem, doc);
                            counter = counter - 1;
                            continue;
                        }
                        counter = counter - 1;
                        // like June 12
                        if (isANumber(nextWord) && (DateDayRange(nextWord))) {
                            String ans = addDayAndMonth(nextWord, current);
                            addToMap(ans, Tokens, ToStem, doc);
                            counter = counter + 1;
                            continue;
                        }

                        // like June 1989
                        else if (isANumber(nextWord) && (nextWord.length() == 4) &&
                                (!isThereDot(nextWord) &&
                                        !isThereDot(nextWord))) {
                            String ans = addMonthAndYear(current, nextWord);
                            addToMap(ans, Tokens, ToStem, doc);
                            counter = counter + 1;
                            continue;
                        }
                    }
                    if (isCapital(current)) {
                        String nextw = getNextWord();
                        if (isCapital(nextw) && CitiesIndexer.isCity(current + "," + nextw)) {
                            secRound.add(nextw);
                            secRound.add(current);
                            addToMap(current + "," + nextw, Tokens, ToStem, doc);
                            checkCity(current + "," + nextw, doc.getName(), counter - 1);
                            continue;
                        }
                        counter = counter - 1;
                    }
                    // check for john--asd-zz----fa
                    if (isThereTwoConseHyphen(current)) {
                        List<String> temp = new ArrayList<>();
                        temp = WorkWithTwoConseHyphen(current);
                        secRound.addAll(temp);
                        continue;
                    }
                    addToMap(current, Tokens, ToStem, doc);
                    checkCity(current, doc.getName(), counter);
                    continue;
                }
            }
        }
    }

    // checks if the number contain letters
    private boolean isThereLettersInNum(String current) {
        boolean ans = false;
        for (int i = 0; i < current.length(); i++) {
            if (current.charAt(i) != '0' && current.charAt(i) != '1' && current.charAt(i) != '2' && current.charAt(i) != '3'
                    && current.charAt(i) != '4' && current.charAt(i) != '5' && current.charAt(i) != '6'
                    && current.charAt(i) != '7' && current.charAt(i) != '8' && current.charAt(i) != '9'
                    && current.charAt(i) != '.' && current.charAt(i) != ',') {
                ans = true;
                break;
            }
        }
        return ans;
    }

    //like ?word//
    private boolean isWordBeginWithTrash(String current) {
        return !((current.charAt(0) == '$' && current.length() > 1 && isDigit(current.charAt(1))) || isLetter(current.charAt(0)) || isDigit(current.charAt(0)));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    // like word?
    private boolean isWordEndsWithTrash(String current) {
        return !(isLetter(current.charAt(current.length() - 1)) || isDigit(current.charAt(current.length() - 1)));
    }

    private boolean isWordIsTrash(String current) {
        if (current.length() <= 1)
            return false;

        boolean firstCho = (current.equals("--") || current.equals("</F>"));
        boolean secCho = isWordStartsAndOrEndWithSog(current);
        boolean thirdCho = current.charAt(0) == '-' || current.charAt(0) == '*' || current.charAt(0) == '|';
        return firstCho || secCho || (thirdCho && isWordContainOnlyOneLet(current));
    }

    // check if word is like aaaaa -----
    private boolean isWordContainOnlyOneLet(String current) {
        for (int i = 1; i < current.length(); i++) {
            if (current.charAt(i) != current.charAt(i - 1))
                return false;
        }
        return true;
    }

    //**************************
    private void addToMap(String str, Map<String, Integer> ParserSet, boolean ToStem, Document doc) {
        if (str.length() == 0 || str.length() == 1)
            return;
        if (str.toUpperCase().equals("MR"))
            getMrCounter();
        if (ToStem) {
            boolean cap=str.charAt(0)>='A' && str.charAt(0)<='Z';
            str = st.stem(str);
            if(cap)
                str=str.toUpperCase();
        }

        doc.setLength(doc.getLength() + 1);

        if (ParserSet.containsKey(str)) {
            // value is the term frequency
            int value = ParserSet.get(str) + 1;
            ParserSet.remove(str);
            ParserSet.put(str, value);
        } else
            ParserSet.put(str, 1);
    }

    // check if the word after parsing contains unwanted characters
    private boolean isThereJunkAfterParsing(String word) {
        for (int i = 0; i < word.length(); i++) {
            if ((word.charAt(i) >= 'a' && word.charAt(i) <= 'z') ||
                    (word.charAt(i) >= 'A' && word.charAt(i) <= 'Z') ||
                    (word.charAt(i) >= '0' && word.charAt(i) <= '9') ||
                    (word.charAt(i) == '-' || word.charAt(i) == '$'))
                continue;
            else
                return true;
        }
        return false;
    }

    private String twoPointNum(String num) {
        if (howManyDotinNum(num) == 1 && isThereDot(num)) {
            boolean lastChar = num.charAt(num.length() - 1) == 'k' || num.charAt(num.length() - 1) == 'K' ||
                    num.charAt(num.length() - 1) == 'b' || num.charAt(num.length() - 1) == 'B' ||
                    num.charAt(num.length() - 1) == 'm' || num.charAt(num.length() - 1) == 'M';
            int dotIndex = getDotIndex(num);
            if (num.length() < 5)
                return num;
            if (num.length() - dotIndex - 1 > 1 && lastChar)
                return num.substring(0, dotIndex + 3) + num.substring(num.length() - 1);
            if (num.length() - dotIndex - 1 > 1 && !lastChar)
                return num.substring(0, dotIndex + 3);
            return num;
        }
        return num;
    }

    private int howManyDotinNum(String num) {
        int cou = 0;
        for (int i = 0; i < num.length(); i++) {
            if (num.charAt(i) == '.')
                cou++;
        }
        return cou;
    }

    // like 12.234 return 2
    private int getDotIndex(String num) {
        for (int i = 0; i < num.length(); i++) {
            if (num.charAt(i) == '.')
                return i;
        }
        return 0;
    }

    // check for john--trap
    private boolean isThereTwoConseHyphen(String stri) {
        for (int i = 0; i < stri.length() - 1; i++) {
            if (stri.charAt(i) == '-' && stri.charAt(i + 1) == '-')
                return true;
        }
        return false;
    }

    //john--asd--kpt || johna-asd----kpt
    private List<String> WorkWithTwoConseHyphen(String S) {
        List<String> list = new ArrayList<>();
        String temp = "";
        for (int i = 0; i < S.length(); i++) {
            if (S.charAt(i) == '-')
                temp += " ";
            else
                temp += S.charAt(i);
        }
        //split the words
        String[] elements = temp.split(" ");
        for (int i = 0; i < elements.length; i++) {
            if (!(elements[i].equals("")))
                list.add(elements[i]);
        }
        return list;
    }

    // if the term is a city - adding to CityList
    private void checkCity(String city, String DocName, int place) {
        if (isCapital(city) && CitiesIndexer.isCity(city)) {
            // add the city to the list
            CityList.add(city + "," + DocName + "," + place);
        }
    }

    public List<String> getCities() {
        return CityList;
    }

    private static class DBInfo {
        static private DBInfo _instance = null;
        BufferedReader reader;
        Set<String> stopWordsHash = new HashSet<String>();

        DBInfo() {
            try {
                reader = new BufferedReader(new FileReader(TextParser.stopWordsPath + "\\stop_words.txt"));
                String line = reader.readLine();
                while (line != null) {
                    stopWordsHash.add(line);
                    // read next line
                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        static private DBInfo instance() {
            if (_instance == null) {
                _instance = new DBInfo();
            }
            return _instance;
        }
    }
}

