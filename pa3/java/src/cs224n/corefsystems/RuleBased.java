package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.coref.Sentence.Token;
import cs224n.util.*;
import cs224n.coref.StopWords;
import cs224n.ling.*;
import cs224n.coref.Pronoun;
import cs224n.coref.Util;

import java.util.ArrayList;
import java.util.*;

/**
 * Multi-pass sieve parser
 * 
 * Partially inspired by the multi-pass sieve parser of Raghunathan et. al.
 * http://www.surdeanu.info/mihai/papers/emnlp10.pdf
 *
 */
public class RuleBased implements CoreferenceSystem {
  HashMap<Mention, ClusteredMention> clusterMap;
  CounterMap<String, String> coreferentHeads = null;
  private static final Double kCorefHeadThresh = 1.0;

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
    coreferentHeads = new CounterMap<String, String>();

    for (Pair<Document, List<Entity>> pair : trainingData) {
      Document doc = pair.getFirst();
      List<Entity> clusters = pair.getSecond();
      List<Mention> mentions = doc.getMentions();

      // For all coreferent mention pairs
      for (Entity e : clusters) {
        for (Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()) { // iterable and order-sensitive; BA and AB will both show up
          // Exclude he/she/it/etc.
          // if (!Pronoun.isSomePronoun(mentionPair.getFirst().headWord()) && !Pronoun.isSomePronoun(mentionPair.getSecond().headWord()))
            coreferentHeads.incrementCount(mentionPair.getFirst().headWord(), mentionPair.getSecond().headWord(), 1.0);
        }
      }
    }
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
    //put all mentions into map as singleton clusters
    clusterMap = new HashMap<Mention, ClusteredMention>();
    for (Mention m : doc.getMentions()) {
      clusterMap.put(m, m.markSingleton());
    }

    // Run multi-layer sieve
    pass1(clusterMap); // Exact match
    pass2(clusterMap); // Hobbs algorithm
    pass3(clusterMap); // Strict head matching
    pass4(clusterMap); // Head matching with relaxed compatible modifiers
    pass5(clusterMap); // Head matching with relaxed word inclusion

    // /* Not included in the final system to improve B3 performance */
    // // pass6(clusterMap); //Observed coreferent head matching

    pass7(clusterMap); // Mention resolution
    pass8(clusterMap); // pronouns
    
    ArrayList<ClusteredMention> mentions =  new ArrayList<ClusteredMention>(clusterMap.values());

    return mentions;
  }

  /**
   * Merges clusters given by two entities in order
   */
  void mergeClusters(Mention mention1, Mention mention2) {
    ClusteredMention m1 = clusterMap.get(mention1);
    ClusteredMention m2 = clusterMap.get(mention2);

    for (Mention m : m2.entity.mentions) {
      m.removeCoreference();
      clusterMap.put(m, m.markCoreferent(m1));
    }
  }

  void pass1(HashMap<Mention, ClusteredMention> mentions) {
  	//stores mapping
    Map<String, Mention> clusters = new HashMap<String, Mention>();

    for (Mention m : mentions.keySet()){
      // Get its text
      String mentionString = m.gloss();

      // If we've seen this text before
      if (clusters.containsKey(mentionString)){     // Exact match
        // Merge mentions
        mergeClusters(m, clusters.get(mentionString));
      } else {
        // Make note of it
        clusters.put(mentionString,m);
      }
    }

  }

   //runs Hobbs algorithm
  void pass2(HashMap<Mention, ClusteredMention> mentions) {
    for (Mention m1 : mentions.keySet()) {
      for (Mention m2 : mentions.keySet()) {
        if ((m1 == m2) || (m1.doc.indexOfMention(m1) > m1.doc.indexOfMention(m2)))
          continue;
        if (HobbsResolver.matchesMentions(m1, m2)) {
          mergeClusters (m1, m2);
        }
      }
    }
  }

  //returns true if the mention head word matches any head word in the antecedent
  private static boolean clusterHeadMatch (Mention m1, ClusteredMention cluster) {
    String mentionHead = m1.headWord();
    for (Mention m : cluster.entity.mentions) {
      if (m.headWord().equals(mentionHead)) {
        return true;
      }
    }
    return false;
  }

  //returns true if all of the non-stop words in the mention cluster are
  //included in the non-stop words of the antecedent cluster 
  private static boolean wordInclusion(ClusteredMention m1, ClusteredMention m2) {
    
    //get all nonstop words in the antecedent cluster
    HashSet<String> m2Words = new HashSet<String>();
    for (Mention m : m2.entity.mentions) {
      for (String word : m.text()) {
        if (!StopWords.isSomeStopWord(word)) {
          m2Words.add(word);
        }
      }
    }

    //check if any nonstop words of the mention appear in the antecedent
    for (Mention mention : m1.entity.mentions) {
      for (String word : mention.text()) {
        if (!StopWords.isSomeStopWord(word) && !m2Words.contains(word)) {
          return false;
        }
      }
    }

    return true;  
  }

  private static boolean isAdj (Token t) {
    String tag = t.posTag();
    return tag.equals ("JJ") || tag.equals ("JJR") || tag.equals("JJS");
  }

  //return true if the mention's modifiers are all included in the modifiers 
  //of the antecedent candidate
  private static boolean compatibleModifiers(Mention m1, Mention m2) {
    //only nouns or adjectives
    List<Token> m1Tokens = m1.sentence.tokens.subList(m1.beginIndexInclusive, m1.endIndexExclusive);
    List<Token> m2Tokens = m2.sentence.tokens.subList(m2.beginIndexInclusive, m2.endIndexExclusive);

    for(Token tok : m1Tokens) {
      if ((tok.isNoun() || isAdj(tok)) && !m2Tokens.contains(tok)) {
        return false;
      }
    }
    return true;
  }

  // Strict Head Matching... enforces clusterHeadMatch, compatible modifiers
  // and word inclusion
  void pass3(HashMap<Mention, ClusteredMention> mentions) {
    for (Mention m1 : mentions.keySet ()) {
      for (Mention m2 : mentions.keySet ()) {
        if (clusterHeadMatch(m1, mentions.get(m2))) {
          if (wordInclusion (mentions.get(m1), mentions.get(m2))) {
            if (compatibleModifiers(m1, m2)) {
                mergeClusters(m1, m2);
            }
          }
        }
      }
    }
  }

  //Head matching... relaxes compatible modifier constraint for head matching
  void pass4(HashMap<Mention, ClusteredMention> mentions) {
     for (Mention m1 : mentions.keySet ()) {
      for (Mention m2 : mentions.keySet ()) {
        if (clusterHeadMatch(m1, mentions.get(m2))) {
          if (wordInclusion (mentions.get(m1), mentions.get(m2))) {
                mergeClusters(m1, m2);
          }
        }
      }
    }
  }

  //Head matching.... relaxes word inclusion constraint
  void pass5(HashMap<Mention, ClusteredMention> mentions) {
    for (Mention m1 : mentions.keySet ()) {
      for (Mention m2 : mentions.keySet ()) {
        if (clusterHeadMatch(m1, mentions.get(m2))) {
          mergeClusters(m1, m2);
        }
      }
    }
  }

  //returns true iff both agree on gender, number, person, named entity, and lemma
  boolean mentionsAgree (Mention m1, Mention m2) {
    Pair<Boolean,Boolean> gender = Util.haveGenderAndAreSameGender (m1, m2);
    Pair<Boolean,Boolean> number = Util.haveNumberAndAreSameNumber (m1, m2);

    boolean genderAgree = (gender.getFirst() && gender.getSecond()) || !gender.getFirst();
    boolean numberAgree = (number.getFirst() && number.getSecond()) || !number.getFirst();
  
    boolean personAgree = true;
    if (Pronoun.isSomePronoun(m1.gloss()) && Pronoun.isSomePronoun(m2.gloss())) {
      if (!(m1.headToken().isQuoted() || m2.headToken().isQuoted())) {
        Pronoun p1 = Pronoun.valueOrNull(m1.gloss());
        Pronoun p2 = Pronoun.valueOrNull(m2.gloss());
        if (p1 != null && p2 != null) {
          personAgree = (p1.speaker == p2.speaker);
        }
      }
    }

    boolean nerAgree = m1.headToken().nerTag().equals(m2.headToken().nerTag());
    boolean lemmasAgree = m1.headToken().lemma().equals(m2.headToken().lemma());

    return genderAgree && numberAgree && personAgree && nerAgree && lemmasAgree;
  
  }

  boolean observedHeadPair (Mention m1, Mention m2) {
    return coreferentHeads.getCount(m1.headWord(), m2.headWord()) > kCorefHeadThresh;
  }

  void pass6(HashMap<Mention, ClusteredMention> mentions) {
    for (Mention m1 : mentions.keySet()) {
      for (Mention m2 : mentions.keySet()) {
        if (observedHeadPair(m1, m2)) {
           mergeClusters(m1, m2);
        }
      }
    }
  }

  //mention matching... merges clusters if two mentions satisfy mention agreement
  //requirements for gender, number, person, ner, and lemma
  void pass7(HashMap<Mention, ClusteredMention> mentions) {
    for (Mention m1 : mentions.keySet()) {
      for (Mention m2 : mentions.keySet()) {
        if (mentionsAgree(m1, m2)){
          mergeClusters(m1, m2);
        }
      }
    }
  }

   //return true if the pronouns agree
  boolean pronounsAgree (Mention m1, Mention m2) {
    Pronoun p1 = Pronoun.valueOrNull(m1.gloss());
    Pronoun p2 = Pronoun.valueOrNull(m2.gloss());
    if (p1 == null || p2 == null) {
      return false;
    }

    return p1.gender == p2.gender && p1.speaker == p2.speaker && p1.plural == p2.plural;
  }

  //pronoun specific matching... merges two pronouns if they have the same
  //gender, speaker, and number (e.g. is plural)
  void pass8(HashMap<Mention, ClusteredMention> mentions) {
    for (Mention m1 : mentions.keySet()) {
      for (Mention m2 : mentions.keySet()) {
        if (Pronoun.isSomePronoun(m1.gloss()) && Pronoun.isSomePronoun(m2.gloss())) {
          if (pronounsAgree(m1, m2)){
            mergeClusters(m1, m2);
          } 
        }
      }
    }
  }
}
