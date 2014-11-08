package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.*;

import java.util.ArrayList;
import java.util.*;

public class BetterBaseline implements CoreferenceSystem {
  // For each cluster counts the number of headword pairs that appeared together: (A,B,C,D) => AB++, AC++, AD++, BC++, BD++, ... (the transitive closure of this subgraph)
  CounterMap<String, String> coreferentHeads = null;

  private static final Set<String> kPronouns = new HashSet<String>(Arrays.asList( 
    new String[] {"I", "he", "she", "it", "they", "them", "that", "this", "we", "us", "you", "her", "him"}
  ));
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
          if (!kPronouns.contains(mentionPair.getFirst().headWord()) && !kPronouns.contains(mentionPair.getSecond().headWord()))
            coreferentHeads.incrementCount(mentionPair.getFirst().headWord(), mentionPair.getSecond().headWord(), 1.0);
        }
      }
    }
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {

    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
    Map<String,Entity> clusters = new HashMap<String,Entity>();

    // For each mention
    for (Mention m : doc.getMentions()){
      String head = m.headWord();

      Set<String> corefs = coreferentHeads.getCounter(head).keySet();
      boolean added = false;
      for (String s : corefs) {
        if (coreferentHeads.getCount(head,s) >= kCorefHeadThresh) {
          if (clusters.containsKey(s)) { // if we've seen this alt head s before
            mentions.add(m.markCoreferent(clusters.get(s)));
            added = true;
            break;
          }
        }
      }
      if (!added) {
        if (clusters.containsKey(head)) { // if we've seen this head before
          mentions.add(m.markCoreferent(clusters.get(head)));
        } else { // Else create a new singleton cluster)
          ClusteredMention newCluster = m.markSingleton();
          mentions.add(newCluster);
          clusters.put(head,newCluster.entity);
        }
      }

    } // end for Mention m

    return mentions;
  }
}
