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
	//JM: Temporary convergence criteria... need to experiment to see
	//  	what gives us the best result
	private static final int max_iterations = 30; 

  private IBMModel1 model1;

	// NULL word
  private final static String NULLWORD = "--NULL--";

  private CounterMap<String, String> t_params; //map of t[e|f] parameters
	private CounterMap<String, String> source_target_counts; //map of c[e, f] counts
	private Counter<String> source_counts;	//map of c[f] counts

	//JM: Is there a better way to compactly represent these counters?
	private CounterMap<List<Integer>, Integer> q_params; 	//map of q[j | i, l, m] parameters
	private CounterMap<List<Integer>, Integer>  align_len_count; //map of c[j | i, l, m] counts
	private CounterMap<Integer, Integer> sent_len_count; 	//map of c[l, m] counts
	

  @Override
  public Alignment align(SentencePair sentencePair) { 
    Alignment alignment = new Alignment();	

    List<String> target_sentence = sentencePair.getTargetWords();
    List<String> source_sentence = sentencePair.getSourceWords();
    source_sentence.add(NULLWORD);

    int target_len = target_sentence.size();
    int source_len = source_sentence.size();

 		for (int i = 0; i < target_len; i++) {
    	String target_word = target_sentence.get(i);

    	double curr_max = 0.0;
    	int best_idx = 0;
    	for (int j = 0; j < source_len; j++) {
    		String source_word = source_sentence.get(j);
    		double t_param = t_params.getCount(target_word, source_word);

    		List<Integer> prior = Arrays.asList(i, source_len, target_len);
    		double q_param = q_params.getCount(prior, j);

    		double prob = t_param * q_param;
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
    // TODO: create reset method for counter maps instead of this junk

  	for (String key : source_target_counts.keySet()) {
  		for (String value : source_target_counts.getCounter(key).keySet()){
  			source_target_counts.setCount (key, value, 0.);
  		}
  	}

  	for (String key : source_counts.keySet()){
  		source_counts.setCount (key, 0.);
  	}

  	//reset the counts for the c(l, m) counters
  	for(int source_len : sent_len_count.keySet()) {
  		for(int target_len : sent_len_count.getCounter(source_len).keySet()){
  			sent_len_count.setCount(source_len, target_len, 0.);
  		}
  	}

  	//reset the counts for the c(j | i, l, m) counters
  	for (List<Integer> prior : align_len_count.keySet()) {
  		for (int j : align_len_count.getCounter(prior).keySet()) {
  			align_len_count.setCount(prior, j, 0.);
  		}	
  	}
  }

 	private void initialize_parameters (List<SentencePair> trainingData) {
  	for (SentencePair pair : trainingData){
  		// Initialize the alignment counters and q parameters
  		int source_len = pair.getSourceWords().size()+1;
  		int target_len = pair.getTargetWords().size();
  		// sent_len_count.setCount(source_len, target_len, 0.); // not necessary, done at every iteration

  		for (int i = 0; i < target_len; i++) {
  			for (int j = 0; j < source_len; j++) {
  				List<Integer> prior = Arrays.asList(i, source_len, target_len);
  				// align_len_count.setCount(prior, j, 0.); // not necessary, done at every iteration
  				q_params.setCount(prior, j, Math.random());
  			}
  		}

  		// Normalize each of the q parameters
  		for (List<Integer> prior : q_params.keySet()) {
        // CV: object-oriented layout would be like this
        //     -> q_params.getCounter(prior).normalize()
        // TODO: non-assignable; write our own!
        q_params.getCounter(prior).normalize();

        assert Math.abs(q_params.getCounter(prior).totalCount()-1) < 10e-04;
  		}
  	}

  	// Initialize the t[e | f] parameters using model 1
  	model1 = new IBMModel1 ();
    model1.train(trainingData);
    t_params = model1.t_map;
 	}

 	private void estimate_parameters (List<SentencePair> trainingData) {
 		for (SentencePair pair : trainingData) {
			//extract words from training data
			List<String> source_sentence = pair.getSourceWords ();
			List<String> target_sentence = pair.getTargetWords ();

			int source_len = source_sentence.size();
			int target_len = target_sentence.size();

			for (int i = 0; i < target_len; i++){	
				String target = target_sentence.get(i); // ith word in the target sentence; we need alignment for each one of those

				//compute normalization term
				double normalize_val = 0.;
        List<Integer> prior = Arrays.asList(i, source_len, target_len);
        
        System.out.println("this value should be 1: "+q_params.getCounter(prior).totalCount());

				for (int j = 0; j < source_len; j++) {

          // System.out.println("poop; "+prior.toString()+q_params.getCounter(prior).totalCount());

					String source = source_sentence.get(j);
          // System.out.println("t: "+t_params.getCount(target, source)+" q: "+q_params.getCount(prior, j));
					normalize_val += t_params.getCount(target, source) * 
														q_params.getCount(prior, j);
				}

        // System.out.println("norm: "+normalize_val);

				//iterate over the source sentence
				for (int j = 0; j < source_len; j++){
					String source = source_sentence.get(j);
					double delta = (t_params.getCount(target, source) *
													 q_params.getCount(prior, j)) / normalize_val;

          // System.out.println("delta: "+delta);

					//update the counters
					source_target_counts.incrementCount(source, target, delta);
					source_counts.incrementCount(source, delta);
					align_len_count.incrementCount(prior, j, delta);
	 				sent_len_count.incrementCount(source_len, target_len, delta);
				}	
			}
		}
	}


 	private boolean update_parameters () {
 		boolean converged = true;

 		//update the t[e, f] parameters
    // assert: t_params size properly initialized
 		for (String target : t_params.keySet()){
 			for (String source: t_params.getCounter(target).keySet()) {
  			double next_val = source_target_counts.getCount(source, target) 
  										 		/ source_counts.getCount(source);

  			//TODO:: Experiment with different convergence conditions here
  			double prev_val = t_params.getCount(target, source);	
  			if (Math.abs(prev_val - next_val) > 10e-04){
  				converged = false;
  			}

  			t_params.setCount(target, source, next_val);
  		}
 		}

 		//iterate over all possible l, m combinations in the training data
 		for (int source_len : sent_len_count.keySet()) {
 			for (int target_len : sent_len_count.getCounter(source_len).keySet()){
 				for (int i = 0; i < target_len; i++) {
          List<Integer> prior = Arrays.asList(i, source_len, target_len);
 					for (int j = 0; j < source_len; j++) {
 						double next_val = align_len_count.getCount(prior, j) /
 															sent_len_count.getCount (source_len, target_len);

 						//TODO:: Experiment with different convergence conditions here
  					double prev_val = q_params.getCount(prior, j);	
  					if (Math.abs(prev_val - next_val) > 10e-04){
  						converged = false;
  					}

						q_params.setCount(prior, j, next_val);
					}
          // System.out.println("this value should be 1: "+q_params.getCounter(prior).totalCount());
          // assert q_params.getCounter(prior).totalCount == 1
 				}
 			}
 		}

 		return converged;
 	}

  @Override
  public void train(List<SentencePair> trainingData) {
    // Initialize data structures
    source_target_counts = new CounterMap<String, String> ();
    source_counts = new Counter<String> ();
    t_params = new CounterMap<String, String> ();
    q_params = new CounterMap<List<Integer>, Integer> ();
	  align_len_count = new CounterMap<List<Integer>, Integer> ();
	 	sent_len_count = new CounterMap<Integer, Integer> (); 

    // Initialize parameters using Model 1
    // This will add NULL word to all source sentences in data
    initialize_parameters (trainingData);

    // System.out.println("Initialized params: t_params.keySet().size(): "+t_params.keySet().size());


    // // System.out.println("1: "+q_params.toString());

   //  Run the E-M algorithm until converges (experiment with this criteria)
  	for (int num_iters = 0; num_iters < max_iterations; num_iters++) {
  		//set all of the counters back to 0
  		reset_counts (); 
          // System.out.println("2: "+q_params.toString());


  		// stimation step
  		estimate_parameters (trainingData);

          // // System.out.println("3: "+q_params.toString());


  		//maximization step (break if the M-step converged)
  		if (update_parameters())
  			break;
  	}
  }

}