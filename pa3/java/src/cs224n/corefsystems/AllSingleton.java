package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.Pair;

import java.util.ArrayList;
import java.util.*;

public class AllSingleton implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// No training, silly.
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();

    // For each mention
    for (Mention m : doc.getMentions()) {
      // Create a new singleton cluster
      ClusteredMention newCluster = m.markSingleton();
      mentions.add(newCluster);
    }

    // Return mentions
    return mentions;
	}
}
