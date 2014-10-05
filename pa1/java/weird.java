package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;
import java.util.Arrays;

/**
 * IBM Model 2 implementation.
 * 
 * @author John Miller
 * @author Catalin Voss
 */
public class IBMModel2 implements WordAligner {
  private static final double kConvergenceTol = 1e-7; // TODO: check this is a good value
  private static final double kMaxIter = 100;

  // t(e,f) = p(e|f) = Pr([target]|[source])
  // q(j | i, l, m)
  private CounterMap<String, String> t = null;
  private CounterMap<List<Integer>, Integer> q = null;

  // Counters
  private CounterMap<String, String> sourceTargetCounts = null;
  private CounterMap<List<Integer>, Integer> alignmentCounts = null;

  @Override
  public Alignment align(SentencePair sentencePair) {
    Alignment alignment = new Alignment();
    
    List<String> targets = sentencePair.getTargetWords();
    for (int i = 0; i < targets.size(); i++) {
      String target = targets.get(i);
      List<String> sources = sentencePair.getSourceWords();
      sources.add(WordAligner.NULL_WORD);
      // TODO: move out length
      
      double p_max = 0.0;
      int j_max = 0;

      for (int j = 0; j < sources.size(); j++) {
      // for (int j = 0; j <= sources.size(); j++) {
        // String source = (j < sources.size()) ? sources.get(j) : WordAligner.NULL_WORD;
        String source = sources.get(j);
        // List<Integer> tuple = Arrays.asList(i, sources.size(), targets.size());
        List<Integer> tuple = Arrays.asList(i, sources.size()-1, targets.size());
        double p = q.getCount(tuple, j) * t.getCount(source, target);
        if (p > p_max) {
          p_max = p;
          j_max = j;
        }
      }
        
      // if (j_max != sources.size()) // skip NULL
      if (j_max != sources.size()-1) // skip NULL
        alignment.addPredictedAlignment(i, j_max);
      else
        System.out.println("NULL ASSIGN!");
    }
    
    return alignment;
  }

  @Override
  public void train(List<SentencePair> trainingData) {
    // Initialize vars
    sourceTargetCounts = new CounterMap<String, String>();
    alignmentCounts = new CounterMap<List<Integer>, Integer>();
    q = new CounterMap<List<Integer>, Integer>();

    // Initialize probabilities. This first one adds NULL words!
    IBMModel1 model1 = new IBMModel1();
    model1.train(trainingData);
    t = model1.getT();

    addNullWord(trainingData);

    for (SentencePair pair : trainingData){
      // Initialize the alignment counters and q parameters
      int source_len = pair.getSourceWords().size();
      int target_len = pair.getTargetWords().size();
      for (int i = 0; i < target_len; i++) {
        // List<Integer> tuple = Arrays.asList(i, source_len, target_len);
        List<Integer> tuple = Arrays.asList(i, source_len-1, target_len);

        for (int j = 0; j < source_len; j++) {
        // for (int j = 0; j <= source_len; j++) {
          q.setCount(tuple, j, Math.random()+1e-7); // make sure it's not 0
        }
      }
    }
    q = Counters.conditionalNormalize(q);
    // for (List<Integer> tuple : q.keySet()) {
    //   assert Math.abs(q.getCounter(tuple).totalCount()-1) < 10e-04;
    // }

    // Run EM-Algorithm
    System.out.println("Running EM-Algorithm for IBM Model 2");
    for (int it = 0; it < kMaxIter; it++) {
      estimate(trainingData);
      double score = maximize();
      System.out.println("EM "+it+" score: "+score);

      if (score < kConvergenceTol)
        break;
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
      int source_len = sources.size();
      int target_len = targets.size();
      
      for (int i = 0; i < target_len; i++) {
        String target = targets.get(i);
        // List<Integer> tuple = Arrays.asList(i, source_len, target_len);
        List<Integer> tuple = Arrays.asList(i, source_len-1, target_len);

        // Compute denominator sum
        double normalizer = 0.0;
        for (int j = 0; j < source_len; j++) {
        // for (int j = 0; j <= source_len; j++) {
          String source = sources.get(j);
          // String source = (j < source_len) ? sources.get(j) : WordAligner.NULL_WORD;
          normalizer += (t.getCount(source, target) * q.getCount(tuple, j)); // Neither t nor q should have zero entries
        }
        for (int j = 0; j < source_len; j++) {
        // for (int j = 0; j <= source_len; j++) {
          String source = sources.get(j);
          // String source = (j < source_len) ? sources.get(j) : WordAligner.NULL_WORD;
          double numerator = (q.getCount(tuple, j)*t.getCount(source, target));
          double delta = numerator/normalizer;
          sourceTargetCounts.incrementCount(source, target, delta);
          alignmentCounts.incrementCount(tuple, j, delta);
        }
      }
    }
  }

  /**
   * M-step
   * @return current convergence score (difference from previous step)
   */
  private double maximize() {
    // This is the same thing as keeping track of and dividing
    // by source counts and prior counts respectively
    CounterMap<String, String> t_new = Counters.conditionalNormalize(sourceTargetCounts);
    CounterMap<List<Integer>, Integer> q_new = Counters.conditionalNormalize(alignmentCounts);
    
    // Evaluate
    double score_1 = computeSquareDiff(t, t_new);
    double score_2 = computeSquareDiff(q, q_new);

    t = t_new;
    q = q_new;

    return (score_1+score_2)/2;
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

    for (List<Integer> key : alignmentCounts.keySet()) {
      for (Integer value : alignmentCounts.getCounter(key).keySet())
        alignmentCounts.setCount(key, value, 0.0);
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
