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

	private static final int max_iterations = 10;
	private static final double uniform_initial_param = 5.0;

	private CounterMap<String, String> source_target_counts;
	private Counter<String> source_counts;
	private Counter<Pair<String, String>> t_map; //i.e. stores the map of t[f|e] parameters

  @Override
  public Alignment align(SentencePair sentencePair) {
		Alignment alignment = new Alignment();	

    List<String> targetWords = pair.getTargetWords();
    List<String> sourceWords = pair.getSourceWords();


    //TODO:: FINISH IMPLEMENTING ME!!!!


  	return alignment;
  }


  // Resets each of the counter elements to 0. Called before each iteration
  // of EM during training to ensure counts reflect current parameter 
  // values.
  private void reset_counts () {
  	for (String key : source_target_counts.keySet()) {
  		for (String value : source_target_counts.getCounter(key).keySet()){
  			source_target_counts.setCount (key, value, 0);
  		}
  	}

  	for (String key : source_counts.keySet()){
  		source_counts.setCount (key, 0);
  	}
  }



  //evenutally we should experiment with what type of uniform
  //initialization gives us the best result.
  // JM: Optimization--don't need to loop through the training set 
  // 			Can wait until the value is needed to initialize
  private void initialize_parameters (List<SentencePair> trainingData){
  	for (SentencePair pair : trainingData){
  		for (String source : pair.getSourceWords()){
  			for (String target : pair.getTargetWords()){
  				source_target_counts.setCount(source, target, 0);
  				source_counts.setCount(source, 0);
  				Pair<String, String> word_pair = new Pair<String, String> (target, source);
  				t_map.setCount(word_pair, uniform_initial_param);
  			}
  		}
  	}
  }

  private void update_paramaters (){
  	for (Pair<String, String> pair : t_map.keySet()){
  		double val = source_target_counts.getCount(pair.getFirst(), 
  																							 pair.getSecond()) / 
  																							 source_counts.getCount(pair.getSecond()); 
  		t_map.setCount(pair, val);
  	}
  }

  @Override
  public void train(List<SentencePair> trainingData){ //NEED TO HANDLE NULL WORDS!!!!!
  	source_target_counts = new CounterMap<String, String>();
		source_counts = new Counter<String>();
		t_map = new Counter<Pair<String, String>>(); 

  	initialize_parameters (trainingData);

  	for (int num_iters = 0; num_iters < max_iterations; num_iters++){
  		//reset all of the counts to 0
  		reset_counts ();

  		//estimation step
  		for (SentencePair pair : trainingData) {
  			List<String> source_sentence = pair.getSourceWords ();
  			List<String> target_sentence = pair.getTargetWords ();


  			for (String source : source_sentence){	//we can probability optimize this
  				for (String target : target_sentence){

  					double normalize_val = 0;
  					for (String prior : source_sentence) {
  						Pair<String, String> temp = new Pair<String, String> (target, prior); //clean this up
  						normalize_val += t_map.getCount(temp);
  					}

  					Pair<String, String> temp = new Pair<String, String> (target, source);	//clean this up
  				  double delta = t_map.getCount(temp) / normalize_val;

  					source_target_counts.incrementCount(target, source, delta);
  					source_counts.incrementCount(source, delta);
  				}
  			}
  		}

  		//maximization step
  		update_paramaters ();
  	}

  	// System.out.println(t_map.toString());
  }

}