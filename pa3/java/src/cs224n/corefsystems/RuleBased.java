package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.coref.Sentence.Token;
import cs224n.util.*;
import cs224n.coref.StopWords;
import cs224n.ling.*;

import java.util.ArrayList;
import java.util.*;

/**
 * Pass-based parser
 */
public class RuleBased implements CoreferenceSystem {
  // For each cluster counts the number of headword pairs that appeared together: (A,B,C,D) => AB++, AC++, AD++, BC++, BD++, ... (the transitive closure of this subgraph)
  CounterMap<String, String> coreferentHeads = null;
  HashMap<Mention, ClusteredMention> clusterMap;
  private static final Double kCorefHeadThresh = 1.0;

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
    coreferentHeads = new CounterMap<String, String>();

    for (Pair<Document, List<Entity>> pair : trainingData) {
      Document doc = pair.getFirst();
      List<Entity> clusters = pair.getSecond();
      List<Mention> mentions = doc.getMentions();

      // System.out.println(doc.prettyPrint(clusters));

      // For all coreferent mention pairs
      for (Entity e : clusters) {
        for (Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()) { // iterable and order-sensitive; BA and AB will both show up
          // Exclude he/she/it/etc.
          coreferentHeads.incrementCount(mentionPair.getFirst().headWord(), mentionPair.getSecond().headWord(), 1.0);
        }
      }
    }
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
    // Turn all the mentions into a list of clusters (singletons to start with)
    // List<Entity> clusters = new ArrayList();
    clusterMap = new HashMap<Mention, ClusteredMention>();
    for (Mention m : doc.getMentions()) {
      // Create a singleton entity
      // These have to occur in order that they appear text wise. In later phases we will make
      // use of the fact that all of the phases keep the entity order consistent, that is that
      // if we add two components we will create links to the first one.
      clusterMap.put(m, m.markSingleton());
      // clusters.add(m.entity);
    }

    // Run passes of rules
    pass1(clusterMap);
    // pass2(clusterMap); //eventually should handle apposition, i-within-i, etc.
    pass3(clusterMap);
    pass4(clusterMap);
    pass5(clusterMap);

    ArrayList<ClusteredMention> mentions =  new ArrayList<ClusteredMention>(clusterMap.values());

    return mentions;

    // // Create returnable mentions from the clusters
    // for (Entity e : clusters) {
    //   Iterator<Mention> iterator = e.iterator();
    //   while (iterator.hasNext()) {
    //     // Make clustered mention
    //     ClusteredMention clustered = new ClusteredMention(iterator.next(), e);
    //     // TODO: the e's here only have the cluster mentions as children. Perhaps re-add the entire document if the testing code expects that.
    //     mentions.add(clustered);
    //   }
    // }
    // // TODO: if this doesn't work well enough then only consider text-wise first word of any cluster in subsequent phases
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

    // for (Mention m : m1.entity.mentions)
    //   m.removeCoreference();
    // for (Mention m : m2.entity.mentions)
    //   m.removeCoreference();

    // m1.entity.addAll(m2.entity.mentions);
    // m2.entity.addAll(m1.entity.mentions);
    // System.out.println ("next");

    // Iterator<Mention> iterator = m2.entity.iterator();
    // while (iterator.hasNext()) {
    //   Mention m = iterator.next();
    //   m2 = m2.mention.markCoreferent(m);
    // }

    // Iterator<Mention> i = m1.entity.iterator();
    // while (i.hasNext()) {
    //   Mention m = i.next();
    //   m1 = m1.mention.markCoreferent(m);
    // }


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

  // void pass2(List<ClusteredMention> mentions) {
  //   // call addAll on entity

  // }

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
    HashSet<String> m2Words = new HashSet<String>();
    for(Mention m : m2.entity.mentions) {
      for (String word : m.text()) {
        if (!StopWords.isSomeStopWord(word)) {
          m2Words.add(word);
        }
      }
    }


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

  //return true if the mention’s modifiers are all included in the modifiers 
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

  //TODO: THIS DOESN'T ACTUALLY WORK RIGHT NOW!
  // returns true if the two mentions are not in an i- within-i construct, 
  // i.e., one cannot be a child NP in the other’s NP constituent
  // private static boolean iWithini (Mention m1, Mention m2) {
  //   Constituent<String> m1Span = new Constituent<String>(m1.headToken().posTag(), m1.beginIndexInclusive, m1.endIndexExclusive);
  //   Constituent<String> m2Span = new Constituent<String>(m2.headToken().posTag(), m2.beginIndexInclusive, m2.endIndexExclusive);


  //   List<Constituent<String>> m1Consituents =   m1.parse.toConstituentList();
  //   for (Constituent<String> constit : m1Consituents) {
  //     if (constit == m1Span) {
  //       System.out.println ("This actually worked")
  //       return false;
  //     }
  //   }

  //   List<Constituent<String>> m2Consituents =   m2.parse.toConstituentList();
  //   for (Constituent<String> constit : m2Consituents) {
  //     if (constit == m2Span) {
  //       return false;
  //     }
  //   }


  //   return true;

  //   // List<Constituent<String>> m2Consituents =  parse2.toConstituentList();

  // }


  //Strict Head Matching (pass 3 of http://www.surdeanu.info/mihai/papers/emnlp10.pdf)
  void pass3(HashMap<Mention, ClusteredMention> mentions) {
    for (Mention m1 : mentions.keySet ()) {
      for (Mention m2 : mentions.keySet ()) {
        if (clusterHeadMatch(m1, mentions.get(m2))) {
          if (wordInclusion (mentions.get(m1), mentions.get(m2))) {
            if (compatibleModifiers(m1, m2)) {
              //if (!iWithini(m1, m2)) {
                mergeClusters(m1, m2);
              //}
            }
          }
        }
      }
    }
  }

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


  void pass5(HashMap<Mention, ClusteredMention> mentions) {
    for (Mention m1 : mentions.keySet ()) {
      for (Mention m2 : mentions.keySet ()) {
        if (clusterHeadMatch(m1, mentions.get(m2))) {
          if (compatibleModifiers(m1, m2)) {
              //if (!iWithini(m1, m2)) {
              mergeClusters(m1, m2);
              //}
          }
        }
      }
    }
  }


  // /**
  //  * Merges clusters given by two entities in order:
  //  * We discard the entity of m2 and map all elements in it to the entity of m1.
  //  * This updates not only m2 but also all other entries in the array of mentions
  //  * (that m2 presumably comes from) that share an entity with m2.
  //  */
  // void mergeClusters(ClusteredMention m1, ClusteredMention m2) {
  //   // Get an iterator over all the mentions in m2's entity
  //   // The API we're given unfortunately doesn't allow us to access the mention list
  //   // directly.
  //   Iterator<Mention> iterator = m2.entity.iterator();
  //   while (iterator.hasNext()) {
  //     Mention m = iterator.next();
  //     m2.entity.add(m);
  //   }
  // }
}
