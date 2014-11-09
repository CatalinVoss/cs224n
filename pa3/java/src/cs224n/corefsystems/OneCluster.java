package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.Pair;

import java.util.ArrayList;
import java.util.*;

public class OneCluster implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// Nothing to do here because our approach is naive.
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {

    ClusteredMention singleCluster = null;
    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();

    // For each mention
    for (Mention m : doc.getMentions()) { //TODO: Change this to addAll, which was given in the starter code.
      // Mark the first mention as the single cluster
      if (singleCluster == null) {
        singleCluster = m.markSingleton();
        mentions.add(singleCluster);
      } else {
        // Add all other mentions to that cluster
        mentions.add(m.markCoreferent(singleCluster.entity));
      }
    }

    return mentions;
	}

}
