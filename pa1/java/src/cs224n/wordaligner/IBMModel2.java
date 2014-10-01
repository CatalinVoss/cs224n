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

	private CounterMap<String, String> source_target_counts; //map of c[e, f] counts
	private Counter<String> source_counts;	//map of c[f] counts
	private CounterMap<String, String> t_map; //map of t[e|f] parameters

	//TODO:: need to store q(j | i, l, m) parameters and counts

  @Override
  public Alignment align(SentencePair sentencePair) { 
    Alignment alignment = new Alignment();	

    List<String> target_sentence = sentencePair.getTargetWords();
    List<String> source_sentence = sentencePair.getSourceWords();
    source_sentence.add(NULLWORD);

 		//TODO:: FINISH THE ALIGNMENT ALGORITHMS

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
 		//initialize the counters and the parameters
  	for (SentencePair pair : trainingData){
  		for (String source : pair.getSourceWords()){
  			for (String target : pair.getTargetWords()){
  				source_target_counts.setCount(source, target, 0);
  				source_counts.setCount(source, 0);
  				t_map.setCount(target, source, 0);

  				//TODO:: Randomly INITIALIZE THE c(j | i, l m) counters
  			}
  		}
  	}

  	//initialize the t[e | f] parameters using model 1
  	//JM: I haven't done real OO programming before... is this a reasonable
  	// way to do this??
  	IBMModel1 model1 = new IBMModel1 ();
  	model1.initialize_t_parameters (trainingData, source_target_counts, 
  																	source_counts, t_map);

 	}

  @Override
  public void train(List<SentencePair> trainingData) {
    //Initialize data structures
    source_target_counts = new CounterMap<String, String> ();
    source_counts = new Counter<String> ();
    t_map = new CounterMap<String, String> ();

    add_null_words (trainingData);

    //Initialize parameters (includes running model 1 to get t[f|e])
    initialize_parameters (trainingData);

    //Run the E-M algorithm until converges (experiment with convergence)
  	for (int num_iters = 0; num_iters < max_iterations; num_iters++) {
  		//set all of the counters back to 0
  		reset_counts (); //need to modify this once the c(j | i, l, m stuff is there)

  		//estimation step


  		//maximization step (break if the M-step converged)
  	}


  }

}