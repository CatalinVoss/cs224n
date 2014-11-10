package cs224n.coref;

import java.util.HashSet;
import java.util.Set;

public enum StopWords {
  STOPWORD;
  private static String[] stopWordList = new String[]{ "a", "about", "above", "above",
    "across", "after", "afterwards", "again", "against", "all",
    "almost", "alone", "along", "already", "also", "although",
    "always", "am", "among", "amongst", "amoungst", "amount", "an",
    "and", "another", "any", "anyhow", "anyone", "anything", "anyway",
    "anywhere", "are", "around", "as", "at", "back", "be", "became",
    "because", "become", "becomes", "becoming", "been", "before",
    "beforehand", "behind", "being", "below", "beside", "besides",
    "between", "beyond", "both", "bottom", "but", "by", "call",
    "can", "cannot", "cant", "co", "com", "con", "could", "couldnt", "cry",
    "de", "describe", "detail", "do", "done", "down", "due", "during",
    "each", "eg", "eight", "either", "eleven", "else", "elsewhere",
    "empty", "enough", "etc", "even", "ever", "every", "everyone",
    "everything", "everywhere", "except", "few", "fifteen", "fify",
    "fill", "find", "fire", "first", "five", "for", "former",
    "formerly", "forty", "found", "four", "free", "from", "front", "full",
    "further", "get", "give", "go", "had", "has", "hasnt", "have",
    "he", "hence", "her", "here", "hereafter", "hereby", "herein",
    "hereupon", "hers", "herself", "him", "himself", "his", "how",
    "however", "hundred", "ie", "if", "in", "inc", "indeed",
    "interest", "into", "is", "it", "its", "itself", "keep", "last",
    "latter", "latterly", "least", "less", "ltd", "made", "many",
    "may", "me", "meanwhile", "might", "mill", "mine", "more",
    "moreover", "most", "mostly", "move", "much", "must", "my",
    "myself", "name", "namely", "neither", "net", "never", "nevertheless",
    "next", "nine", "no", "nobody", "none", "noone", "nor", "not",
    "nothing", "now", "nowhere", "of", "off", "often", "on", "once",
    "one", "only", "onto", "or", "org", "other", "others", "otherwise", "our",
    "ours", "ourselves", "out", "over", "own", "part", "per",
    "perhaps", "please", "put", "rather", "re", "same", "see", "seem",
    "seemed", "seeming", "seems", "serious", "several", "she",
    "should", "show", "side", "since", "sincere", "six", "sixty", "so",
    "some", "somehow", "someone", "something", "sometime", "sometimes",
    "somewhere", "still", "such", "system", "take", "ten", "than",
    "that", "the", "their", "them", "themselves", "then", "thence",
    "there", "thereafter", "thereby", "therefore", "therein",
    "thereupon", "these", "they", "thickv", "thin", "third", "this",
    "those", "though", "three", "through", "throughout", "thru",
    "thus", "to", "together", "too", "top", "toward", "towards",
    "twelve", "twenty", "two", "un", "under", "until", "up", "upon",
    "us", "very", "via", "was", "we", "well", "were", "what",
    "whatever", "when", "whence", "whenever", "where", "whereafter",
    "whereas", "whereby", "wherein", "whereupon", "wherever",
    "whether", "which", "while", "whither", "who", "whoever", "whole",
    "whom", "whose", "why", "will", "wikipedia", "with", "within", "without",
    "would", "yet", "you", "your", "yours", "yourself", "yourselves",
    "the" };

    private static Set<String> stopWordSet = new HashSet<String>();

    /**
     * Determine whether a candidate String is a stop Word
     * @param cand The candidate String
     * @return true if the String is a stop Word
     */
    public static boolean isSomeStopWord(String cand){
      return stopWordSet.contains(cand.toLowerCase());
    }

    static {
      for(String stopWord : stopWordList){
        stopWordSet.add(stopWord);
      }
    }
}
