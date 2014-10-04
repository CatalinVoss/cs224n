package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;
import java.util.Arrays;

/**
 * IBM Model 1 implementation.
 * 
 * @author John Miller
 * @author Catalin Voss
 */
public class IBMModel1 implements WordAligner {
  private static final double kConvergenceTol = 1e-8; // TODO: check this is a good value
  private static final double kMaxIter = 100;

  // t(e,f) = p(e|f) = Pr([target]|[source])
  private CounterMap<String, String> t = null;
  private CounterMap<String, String> sourceTargetCounts = null;

  public CounterMap<String, String> getT() {
    return t;
  }

  // Counter

  /**
   * Following the Collins handout, for English word e_i, the alignment
   * variable a_i is given by
   * 
   *  a_i = argmax_{j \in {0, ..., l}} {t(e_i | f_j)},
   *
   * because each of the q() terms in the expression is a constant and 
   * doesn't affect the maximum.
   */
  @Override
  public Alignment align(SentencePair sentencePair) {
    Alignment alignment = new Alignment();  

    List<String> target_sentence = sentencePair.getTargetWords();
    List<String> source_sentence = sentencePair.getSourceWords();
    source_sentence.add(WordAligner.NULL_WORD);

    for (int i = 0; i < target_sentence.size (); i++) {
      String target = target_sentence.get (i);

      double p_max = 0.0;
      int j_max = 0;

      for (int j = 0; j < source_sentence.size (); j++) {
        String source = source_sentence.get (j);
        double p = t.getCount(source, target);
        if (p > p_max)
        {
          p_max = p;
          j_max = j;
        }
      }

      if (j_max != source_sentence.size () - 1){
        alignment.addPredictedAlignment (i, j_max);
      } else {
        // System.out.println ("NULL ALIGN!");
      }
    }

    return alignment;
  }

  @Override
  public void train(List<SentencePair> trainingData) {
    // Initialize vars
    t = new CounterMap<String,String>();
    sourceTargetCounts = new CounterMap<String, String>();

    // Add null words. This only needs to be done once (even when running Model 2 after).
    addNullWord(trainingData);

    // Initialize probabilities
    for (SentencePair sentencePair : trainingData) {
      for (String source : sentencePair.getSourceWords())
        for (String target : sentencePair.getTargetWords())
          t.setCount(source, target, 1.0);
    }
    t = Counters.conditionalNormalize(t);

    // Run EM-Algorithm
    System.out.println("Running EM-Algorithm for IBM Model 1");
    for (int it = 0; it < kMaxIter; it++) {
      estimate(trainingData);
      double score = maximize();
      System.out.println("EM "+it+" score: "+score);

      if (score < kConvergenceTol)
        break;
    }

    // Remove null words
    for (SentencePair sentencePair: trainingData) {
      List<String> sourceWords = sentencePair.getSourceWords();
      sourceWords.remove(sourceWords.size()-1);
    }
  }

  /**
   * E-step
   */
  private void estimate(List<SentencePair> trainingData) {
    // Clean up
    resetCounts();

    // Find counts
    for (SentencePair sentencePair : trainingData) {
      List<String> sources = sentencePair.getSourceWords();
      List<String> targets = sentencePair.getTargetWords();

      // Compute sum_i t(e|f_i)
      // CV: we can optimize: don't initialize this guy at every iteration
      Counter<String> normalizer = new Counter<String>();
      for (String target : targets) {
        for (String source : sources)
          normalizer.incrementCount(target, t.getCount(source, target));
      }

      for (String target : targets) {  
        for (String source : sources) {
          double delta = t.getCount(source, target) / normalizer.getCount(target);
          sourceTargetCounts.incrementCount(source, target, delta);
        }
      }
    }
  }

  /**
   * M-step
   * @return current convergence score (difference from previous step)
   */
  private double maximize() {
    // This is the same thing as keeping track of and dividing by source counts
    CounterMap<String, String> t_new = Counters.conditionalNormalize(sourceTargetCounts);
    
    // Evaluate
    double result = computeSquareDiff(t, t_new);

    // Save
    t = t_new;

    return result;
  }

  /** HELPERS **/

  private void addNullWord(List<SentencePair> trainingData) {
    for (SentencePair pair : trainingData) {
      List<String> sourceWords = pair.getSourceWords();
      sourceWords.add(WordAligner.NULL_WORD);
    }
  }

  private void resetCounts() {
    for (String key : sourceTargetCounts.keySet()) {
      for (String value : sourceTargetCounts.getCounter(key).keySet())
        sourceTargetCounts.setCount(key, value, 0.0);
    }
  }

  private double computeSquareDiff(CounterMap old, CounterMap newMap) {
    int count = 0;
    double totalUpdate = 0.0;
    for (Object k : old.keySet()) {
      for (Object v : old.getCounter(k).keySet()) {
        double update = old.getCount(k, v) - newMap.getCount(k, v);
        totalUpdate += update*update;
        // System.out.print(totalUpdate);
        count++;
      }
    }
    return totalUpdate/count;
  }
}
