package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;

/**
 * IBM Model 2 implementation.
 * 
 * @author John Miller
 * @author Catalin Voss
 */

public class IBMModel2 implements WordAligner {
	//JM: Temporary convergence criteria... need to experiment to see
	//  	what gives us the best result
	private static final int max_iterations = 30; 

	// NULL word
  private final static String NULLWORD = "--NULL--";

  private CounterMap<String, String> t_params; //map of t[e|f] parameters
	private CounterMap<String, String> source_target_counts; //map of c[e, f] counts
	private Counter<String> source_counts;	//map of c[f] counts

	//JM: Is there a better way to compactly represent these counters?
	private CounterMap<Integer, Integer[]> q_params; 	//map of q[j | i, l, m] parameters
	private CounterMap<Integer, Integer[]>  align_len_count; //map of c[j | i, l, m] counts
	private CounterMap<Integer, Integer> sent_len_count; 	//map of c[l, m] counts
	

  @Override
  public Alignment align(SentencePair sentencePair) { 
    Alignment alignment = new Alignment();	

    List<String> target_sentence = sentencePair.getTargetWords();
    List<String> source_sentence = sentencePair.getSourceWords();
    source_sentence.add(NULLWORD);

 		//TODO:: FINISH THE IMPLEMENTING THE ALIGNMENT ALGORITHM
 		// This should be almost identical to MODEL1, taking into account
 		// the q() parameters in the argmax.

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

  	//TODO:: RESET THE COUNTS FOR c(j | i, l m) counters
  }

 	private void initialize_parameters (List<SentencePair> trainingData) {
  	for (SentencePair pair : trainingData){

  		//Initialize the alignment counters and q parameters
  		int source_len = pair.getSourceWords().size();
  		int target_len = pair.getTargetWords().size();
  		sent_len_count.setCount(source_len, target_len, 0.);

  		for (int i = 0; i < source_len; i++) {
  			for (int j = 0; j < target_len; j++) {
  				Integer [] prior = {i, source_len, target_len};
  				align_len_count.setCount(j, prior, 0.);
  				q_params.setCount(j, prior, Math.random());
  			}
  		}

  		//Initialize the translation counters
  		for (String source : pair.getSourceWords()){
  			for (String target : pair.getTargetWords()){
  				source_target_counts.setCount(source, target, 0);
  				source_counts.setCount(source, 0);
  				t_params.setCount(target, source, 0);
  			}
  		}
  	}

  	//initialize the t[e | f] parameters using model 1
  	//JM: I haven't done real OO programming before... is this a reasonable
  	// way to do this??
  	IBMModel1 model1 = new IBMModel1 ();
  	model1.initialize_t_parameters (trainingData, source_target_counts, 
  																	source_counts, t_params);
 	}

 	private void estimate_parameters (List<SentencePair> trainingData) {
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
						normalize_val += t_params.getCount(target, prior); //UPDATE TO INCLUDE THE q() parameters
					}

				  double delta = t_params.getCount(target, source) / normalize_val; //UPDATE TO INCLUDE the q() parameters in the numerator


					source_target_counts.incrementCount(source, target, delta);
					source_counts.incrementCount(source, delta);
				}
			}
		}

 	}

 	private boolean update_parameters () {
 		boolean converged = true;

 		//update the t[e, f] parameters
 		for (String target : t_params.keySet()){
 			for (String source: t_params.getCounter(target).keySet()) {
  			double next_val = source_target_counts.getCount(source, target) 
  										 		/ source_counts.getCount(source);

  			//TODO:: Experiment with the convergence conditions
  			double prev_val = t_params.getCount(target, source);	
  			if (Math.abs(prev_val - next_val) > 10e-04){
  				converged = false;
  			}

  			t_params.setCount(target, source, next_val);
  		}
 		}

 		//TODO:: update the q[j | i, l, m] parameters

 		return converged;
 	}

  @Override
  public void train(List<SentencePair> trainingData) {
    //Initialize data structures
    source_target_counts = new CounterMap<String, String> ();
    source_counts = new Counter<String> ();
    t_params = new CounterMap<String, String> ();
    q_params = new CounterMap<Integer, Integer[]> ();
	  align_len_count = new CounterMap<Integer, Integer[]> ();
	 	sent_len_count = new CounterMap<Integer, Integer> (); 


		add_null_words (trainingData);

    //Initialize parameters
    initialize_parameters (trainingData);

    //Run the E-M algorithm until converges (experiment with convergence)
  	for (int num_iters = 0; num_iters < max_iterations; num_iters++) {
  		//set all of the counters back to 0
  		reset_counts (); //need to modify this once the c(j | i, l, m stuff is there)

  		//estimation step
  		estimate_parameters (trainingData);

  		//maximization step (break if the M-step converged)
  		if (update_parameters())
  			break;
  	}
  }

}