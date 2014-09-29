package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;

/**
 * PMI Model implementation.
 * 
 * @author John Miller
 * @author Catalin Voss
 */
public class PMIModel implements WordAligner {
  // Counter containers
  // CV: I suppose these are made to be efficient, but indexing by index would be faster;
  //     also no reason for those to be doubles...
  private CounterMap<String,String> sourceTargetCounts;
  private Counter<String> sourceCounts; // i.e. french count
  private Counter<String> targetCounts; // i.e. english count

  // NULL word
  private final static String NULLWORD = "--NULL--";

  @Override  
  public Alignment align(SentencePair pair) {
    Alignment alignment = new Alignment();

    // Extract sequences from pair
    List<String> targetWords = pair.getTargetWords();
    List<String> sourceWords = pair.getSourceWords();
    sourceWords.add(NULLWORD);

    // Per piazza (https://piazza.com/class/hyxho2urgyd6bz?cid=36), we're looking for alignment for each target word.
    // CV: for efficiency's sake, those don't have to be recomputed, but....

    for (int i = 0; i < targetWords.size(); i++) {
      String target = targetWords.get(i);
      double p_target =  targetCounts.getCount(target)/targetCounts.totalCount();
      int align_total = (int)sourceTargetCounts.totalCount();
      double max = 0;
      int best = -1;

      for (int j = 0; j < sourceWords.size(); j++) {
        String source = sourceWords.get(j);
        double p_source = sourceCounts.getCount(source)/sourceCounts.totalCount();
        double p_align = sourceTargetCounts.getCount(source, target)/align_total;
        double score = p_align/(p_source*p_target);

        if (score > max) {
          max = score;
          if (source.equals(NULLWORD))
            best = -1;
          else 
            best = j;
        }
      }

      if (best == -1) {
        // TODO: sort out NULL alignment. Will currently never hit here.
        System.out.println("The world is flat");
        // NULL-alignment is implicit.
        // If for some target word ei, our alignment model determines that the null alignment is better than any an alignment to any source word fj, we should just not add an alignment for ei (i.e., not make a call to Assignment.addPredictedAlignment)
        // Per piazza: https://piazza.com/class/hyxho2urgyd6bz?cid=42 
      } else {
        alignment.addPredictedAlignment(i, best);
      }
    }

    return alignment;
  }

  @Override
  public void train(List<SentencePair> trainingPairs) {
    sourceTargetCounts = new CounterMap<String,String>();
    sourceCounts = new Counter<String>();
    targetCounts = new Counter<String>();

    for (SentencePair pair : trainingPairs) {
      // Extract sequences from pair
      List<String> targetWords = pair.getTargetWords();
      List<String> sourceWords = pair.getSourceWords();
      sourceWords.add(NULLWORD);

      // CV: Can we assume they are nonzero length?
      // Update counts
      for (String source : sourceWords) {
        sourceCounts.incrementCount(source, 1.0);
        for (String target : targetWords) {
          sourceTargetCounts.incrementCount(source, target, 1.0); // this increments the counter by 1.0
          targetCounts.incrementCount(target, 1.0);
        }
      }
    }
  }

}