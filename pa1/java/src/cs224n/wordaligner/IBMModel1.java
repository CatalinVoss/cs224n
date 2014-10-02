package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;

/**
 * IBM Model 1 implementation.
 * 
 * @author John Miller
 * @author Catalin Voss
 */
public class IBMModel1 implements WordAligner {

	private static final int max_iterations = 30; //temporary convergence condition

	// NULL word
  private final static String NULLWORD = "--NULL--";

  private double default_val;
  private boolean first_time;

	private CounterMap<String, String> source_target_counts;
	private Counter<String> source_counts;
	public CounterMap<String, String> t_params; // i.e. stores the map of t[e|f] parameters

	/**
	 * Following the Collins handout, for English word e_i, the alignment
	 * variable a_i is given by
	 * 
	 * 	a_i = argmax_{j \in {0, ..., l}} {t(e_i | f_j)},
	 *
	 * because each of the q() terms in the expression is a constant and 
	 * doesn't affect the maximum.
	 */
  @Override
  public Alignment align(SentencePair sentencePair) {
		Alignment alignment = new Alignment();	

    List<String> target_sentence = sentencePair.getTargetWords();
    List<String> source_sentence = sentencePair.getSourceWords();
    source_sentence.add(NULLWORD);

    for (int i = 0; i < target_sentence.size(); i++) {
    	String target_word = target_sentence.get(i);

    	double curr_max = 0.0;
    	int best_idx = 0;
    	for (int j = 0; j < source_sentence.size(); j++) {
    		String source_word = source_sentence.get(j);
    		double prob = t_params.getCount(source_word,target_word);

    		//update the new most likely alignment
    		if (prob > curr_max) {
    			curr_max = prob;
    			if (source_word.equals(NULLWORD)){
    				best_idx = 0;
    			} else {
    				best_idx = j;
    			}
    		}
    	}
    	alignment.addPredictedAlignment(i, best_idx);
    }

  	return alignment;
  }

  //add a NULL word to each of the source sentences in the training set
 	private void add_null_words (List<SentencePair> trainingData) {
 		for (SentencePair pair : trainingData) {
 			List<String> source = pair.getSourceWords ();
 			source.add(NULLWORD);
 		}
 	}

  // Resets each of the counter elements to 0. Called before each iteration
  // of EM during training to ensure counts reflect current parameter 
  // values.
  private void reset_counts () {
  	for (String key : source_target_counts.keySet()) {
  		for (String value : source_target_counts.getCounter(key).keySet()){
  			source_target_counts.setCount (key, value, 0.);
  		}
  	}

  	for (String key : source_counts.keySet()){
  		source_counts.setCount (key, 0.);
  	}
  }

  //Initializes the Counters and the t() parameters based on TRAININGDATA
  //JM: Could probably optimize to avoid looping through the data twice
  private void initialize_parameters (List<SentencePair> trainingData){
  	//initialize the counters
  	for (SentencePair pair : trainingData){
  		for (String source : pair.getSourceWords()){
  			for (String target : pair.getTargetWords()){
  				source_target_counts.setCount(source, target, 0);
  				source_counts.setCount(source, 0);
  			}
  		}
  	}	

  	//Uniformly initializes the t() paramaters t(e,f) = 1/(|F|), where |F|
  	//is the number of distinct words f \in F.
  	// double initial_val = 7353; //1.0 / (source_counts.keySet().size());

    int num_targets = 0;
    for (String source : source_target_counts.keySet()){
      for (String target : source_target_counts.getCounter (source).keySet()){
        num_targets++;
      }
    }
    default_val = 1.0 / double(num_targets);

  	// for(SentencePair pair : trainingData) {
  	// 	for (String source : pair.getSourceWords()) {
  	// 		for (String target : pair.getTargetWords()) {
  	// 			t_params.setCount(source, target, initial_val);
  	// 		}
  	// 	}
  	// }

   //  t_params = Counters.conditionalNormalize(t_params);

    // for (String source : t_params.keySet()) {
    //   System.out.println("t value should be 1: "+t_params.getCounter(source).totalCount());
    // }
  }

 	 private boolean update_paramaters (){
 	 	boolean converged = true;
  	for(String source : t_params.keySet()) {
  		for (String target : t_params.getCounter(source).keySet()) {
  			double next_val = source_target_counts.getCount(source, target) 
  										 		/ source_counts.getCount(source);

  			//repeat until probabilities for all words don't change
  			// by more than 10e-4 on some iteration
  			double prev_val = t_params.getCount(source, target);	
  			if (Math.abs(prev_val - next_val) > 10e-04){
  				converged = false;
  			}

  			t_params.setCount(source, target, next_val);
  		}
  	}

  	return converged;
 	}

 	private void estimate_parameters (List<SentencePair> trainingData){
		for (SentencePair pair : trainingData) {
			//extract words from training data
			List<String> source_sentence = pair.getSourceWords ();
			List<String> target_sentence = pair.getTargetWords ();

			//JM: we can optimize to avoid computing the
			//  	normalization constant each time
			for (String source : source_sentence){	
				for (String target : target_sentence){

					double normalize_val = 0;
					for (String prior : source_sentence) {
            if (first_time){
              normalize_val += default_val;
            } else {
              normalize_val += t_params.getCount(prior, target);
            }
					}

				  double delta = t_params.getCount(source, target) / normalize_val;

					source_target_counts.incrementCount(source, target, delta);
					source_counts.incrementCount(source, delta);
				}
			}
		}
 	}

  //NOTE: This functions actually adds --NULL-- tokens to each of the 
  //source sentences in the training set.
  @Override
  public void train(List<SentencePair> trainingData){ 
  	source_target_counts = new CounterMap<String, String>();
		source_counts = new Counter<String>();
		t_params = new CounterMap<String, String>(); 

		add_null_words (trainingData);

  	initialize_parameters (trainingData);
    first_time = true;

  	//TODO:: Experiment with convergence criteria 
  	for(int num_iters = 0; num_iters < max_iterations; num_iters++){
  		//reset all of the counts to 0
  		reset_counts ();

  		//estimation step
  		estimate_parameters (trainingData);
      first_time = false;

  		//maximization step, returns TRUE if the M step converged
  		if (update_paramaters ())
  			break;
  	}
  }


  // //Allows Model2 to use the t(e | f) parameters from model1 as initialized
  // //values... does not add NULL tokens to the training data, but assumes
  // //the calling function has already done so
  // //
  // // JM: There should be a cleaner/clearer/more space/time efficient way 
  // //     to do this
  // public void initialize_t_parameters (List<SentencePair> trainingData, 
  //                                      CounterMap<String, String> source_target_counts_, 
  //                                      Counter<String> source_counts_, 
  //                                      CounterMap<String, String> t_params_) {
  //   source_target_counts = source_target_counts_;
  //   source_counts = source_counts_;
  //   t_params = t_params_;

  //   initialize_parameters (trainingData);

  //   //TODO:: Experiment with convergence criteria 
  //   for(int num_iters = 0; num_iters < max_iterations; num_iters++){
  //     //reset all of the counts to 0
  //     reset_counts ();

  //     //estimation step
  //     estimate_parameters (trainingData);

  //     //maximization step, returns TRUE if the M step converged
  //     if (update_paramaters ())
  //       break;
  //   }
  // }
}