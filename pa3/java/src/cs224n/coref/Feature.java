package cs224n.coref;

import cs224n.util.Pair;

import java.util.Set;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public interface Feature {

  //-----------------------------------------------------------
  // TEMPLATE FEATURE TEMPLATES
  //-----------------------------------------------------------
  public static class PairFeature implements Feature {
    public final Pair<Feature,Feature> content;
    public PairFeature(Feature a, Feature b){ this.content = Pair.make(a, b); }
    public String toString(){ return content.toString(); }
    public boolean equals(Object o){ return o instanceof PairFeature && ((PairFeature) o).content.equals(content); }
    public int hashCode(){ return content.hashCode(); }
  }

  public static abstract class Indicator implements Feature {
    public final boolean value;
    public Indicator(boolean value){ this.value = value; }
    public boolean equals(Object o){ return o instanceof Indicator && o.getClass().equals(this.getClass()) && ((Indicator) o).value == value; }
    public int hashCode(){ 
    	return this.getClass().hashCode() ^ Boolean.valueOf(value).hashCode(); }
    public String toString(){ 
    	return this.getClass().getSimpleName() + "(" + value + ")"; }
  }

  public static abstract class IntIndicator implements Feature {
    public final int value;
    public IntIndicator(int value){ this.value = value; }
    public boolean equals(Object o){ return o instanceof IntIndicator && o.getClass().equals(this.getClass()) && ((IntIndicator) o).value == value; }
    public int hashCode(){ 
    	return this.getClass().hashCode() ^ value; 
    }
    public String toString(){ return this.getClass().getSimpleName() + "(" + value + ")"; }
  }

  public static abstract class BucketIndicator implements Feature {
    public final int bucket;
    public final int numBuckets;
    public BucketIndicator(int value, int max, int numBuckets){
      this.numBuckets = numBuckets;
      bucket = value * numBuckets / max;
      if(bucket < 0 || bucket >= numBuckets){ throw new IllegalStateException("Bucket out of range: " + value + " max="+max+" numbuckets="+numBuckets); }
    }
    public boolean equals(Object o){ return o instanceof BucketIndicator && o.getClass().equals(this.getClass()) && ((BucketIndicator) o).bucket == bucket; }
    public int hashCode(){ return this.getClass().hashCode() ^ bucket; }
    public String toString(){ return this.getClass().getSimpleName() + "(" + bucket + "/" + numBuckets + ")"; }
  }

  public static abstract class Placeholder implements Feature {
    public Placeholder(){ }
    public boolean equals(Object o){ return o instanceof Placeholder && o.getClass().equals(this.getClass()); }
    public int hashCode(){ return this.getClass().hashCode(); }
    public String toString(){ return this.getClass().getSimpleName(); }
  }

  public static abstract class StringIndicator implements Feature {
    public final String str;
    public StringIndicator(String str){ this.str = str; }
    public boolean equals(Object o){ return o instanceof StringIndicator && o.getClass().equals(this.getClass()) && ((StringIndicator) o).str.equals(this.str); }
    public int hashCode(){ return this.getClass().hashCode() ^ str.hashCode(); }
    public String toString(){ return this.getClass().getSimpleName() + "(" + str + ")"; }
  }

  public static abstract class SetIndicator implements Feature {
    public final Set<String> set;
    public SetIndicator(Set<String> set){ this.set = set; }
    public boolean equals(Object o){ return o instanceof SetIndicator && o.getClass().equals(this.getClass()) && ((SetIndicator) o).set.equals(this.set); }
    public int hashCode(){ return this.getClass().hashCode() ^ set.hashCode(); }
    public String toString(){
      StringBuilder b = new StringBuilder();
      b.append(this.getClass().getSimpleName());
      b.append("( ");
      for(String s : set){
        b.append(s).append(" ");
      }
      b.append(")");
      return b.toString();
    }
  }
  
  /*
   * TODO: If necessary, add new feature types
   */

  //-----------------------------------------------------------
  // REAL FEATURE TEMPLATES
  //-----------------------------------------------------------

  public static class CoreferentIndicator extends Indicator {
    public CoreferentIndicator(boolean coreferent){ super(coreferent); }
  }

  /*Mentions are an exact match */
  public static class ExactMatch extends Indicator {
    public ExactMatch(boolean exactMatch){ super(exactMatch); }
  }

  /* Number of mentions between the candidate and the previous */
  public static class NumMentionsToPrev extends IntIndicator {
    public NumMentionsToPrev(int numMentionsToPrev) { super(numMentionsToPrev); }
  }

  /*Number of sentences between the candidate and the previous */
  public static class NumSentenceToPrev extends IntIndicator {
    public NumSentenceToPrev (int numSentenceToPrev) { super(numSentenceToPrev); }
  }

  /* Gender of the mentions agree */
  public static class GenderEntityAgreement extends Indicator {
    public GenderEntityAgreement(boolean genderEntityAgreement) {super(genderEntityAgreement); }
  }

  /*Candidate mention is a pronoun */
  public static class CandidateIsPronoun extends Indicator {
    public CandidateIsPronoun(boolean isPronoun) {super(isPronoun); }
  }

  /*Previous mention is a pronoun*/
  public static class OnPrixIsPronoun extends Indicator {
    public OnPrixIsPronoun (boolean isPronoun) {super(isPronoun); }
  } 

  /* Both the candidate and the previous mention are pronouns or are 
     both not pronouns */
  public static class MatchingPronouns extends Indicator {
    public MatchingPronouns (boolean bothPronouns) { super(bothPronouns); }
  }

  /* Candidate is a name */
  public static class CandidateIsName extends Indicator {
    public CandidateIsName(boolean candidateIsName) { super(candidateIsName); }
  }

  /*Previous mention is a name */
  public static class OnPrixIsName extends Indicator {
    public OnPrixIsName (boolean onPrixIsName) { super(onPrixIsName); }
  }

  /* Both the candidate and the previous mention are names or not*/
  public static class MatchingNames extends Indicator {
    public MatchingNames (boolean bothNames) {super(bothNames); }
  }

  /* Candidate is a named entity */
  public static class CandidateEntity extends StringIndicator {
    public CandidateEntity (String candEntity) { super(candEntity); }
  }

    /* Previous mention is a named entity */
  public static class OnPrixEntity extends StringIndicator {
    public OnPrixEntity (String onPrixEntity) { super(onPrixEntity); }
  }

   /* Both the candidate and the previous mention are named entities or not
     named entitiy*/
  public static class MatchingEntities extends Indicator {
    public MatchingEntities (boolean match)  { super(match); }
  }

  /* String feature for each candidate lemma */
  public static class CandidateLemma extends StringIndicator {
    public CandidateLemma (String candidateLemma) { super(candidateLemma); }
  }

  /* String feature for each previous mention lemma */
  public static class OnPrixLemmma extends StringIndicator {
    public OnPrixLemmma (String onPrixLemmma) { super(onPrixLemmma); }
  }

  /*Candidate and previous lemmas match */
  public static class MatchingLemmas extends Indicator {
    public MatchingLemmas (boolean match)  { super(match); }
  }

  /*String indicator for candidate part of speech */
  public static class CandidatePos extends StringIndicator {
    public CandidatePos (String candidatePos) { super(candidatePos); }
  }

  /*String indicator for previous part of speech */
  public static class OnPrixPos extends StringIndicator {
    public OnPrixPos (String onPrixPos) { super(onPrixPos); }
  }

  /*Candidate and previous have the same part of speech*/
  public static class MatchingPos extends Indicator {
    public MatchingPos (boolean match)  { super(match); }
  }

  /*Candidate and previous mention have exactly matching heads*/
  public static class ExactHeadMatch extends Indicator {
    public ExactHeadMatch (boolean match) {super(match);}
  }

  /*Integer feature for number of time head words are coreferent during 
    training data (estimated at training time)*/ 
  public static class CoreferentHeadCount extends IntIndicator {
    public CoreferentHeadCount (int numReferences) {super(numReferences);}
  }

  /* Cross product between exact head match feature and candidate pronoun feature*/
  public static class MatchingNotPronoun extends PairFeature {
    public MatchingNotPronoun (Feature exactHeadMatch, Feature candidateIsPronoun) {super(exactHeadMatch, candidateIsPronoun);}
  }

  /* Any mention in the candidate cluster has a head word match with the target*/
  public static class ClusterHeadMatch extends Indicator {
    public ClusterHeadMatch (boolean match) {super(match);}
  }

  /* Candidate and mention have the same feature*/
  public static class SameSpeaker extends Indicator {
    public SameSpeaker (boolean same) {super(same);}
  }

  /*Mentions are marked coreferent by hobbs algorithm... TODO: add once Hobbs is
    finished*/
  public static class Hobbs extends Indicator {
    public Hobbs (boolean matches) {super(matches); }
  }
}
