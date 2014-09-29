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
	private CounterMap<String, String> t_map; //i.e. stores the map of t[f|e] parameters

  @Override
  public Alignment align(SentencePair sentencePair) {
		Alignment alignment = new Alignment();	

    List<String> targetWords = sentencePair.getTargetWords();
    List<String> sourceWords = sentencePair.getSourceWords();


    //TODO:: FINISH IMPLEMENTING ME!!!!


  	return alignment;
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



  //JM: evenutally we should experiment with what type of uniform
  //initialization gives us the best result.
  // JM: Optimization--don't need to loop through the training set 
  // 			Can wait until the value is needed to initialize
  private void initialize_parameters (List<SentencePair> trainingData){
  	for (SentencePair pair : trainingData){
  		for (String source : pair.getSourceWords()){
  			for (String target : pair.getTargetWords()){
  				source_target_counts.setCount(source, target, 0);
  				source_counts.setCount(source, 0);
  				t_map.setCount(target, source, uniform_initial_param);
  			}
  		}
  	}
  }

  private void update_paramaters (){
  	for(String target : t_map.keySet()) {
  		for (String source: t_map.getCounter(target).keySet()) {
  			double next_val = source_target_counts.getCount(source, target) 
  										 		/ source_counts.getCount(source);
  			t_map.setCount(target, source, next_val);
  		}
  	}
 	}

 	//TODO:: NEED TO HANDLE NULL WORDS!
  @Override
  public void train(List<SentencePair> trainingData){ 
  	source_target_counts = new CounterMap<String, String>();
		source_counts = new Counter<String>();
		t_map = new CounterMap<String, String>(); 

  	initialize_parameters (trainingData);

  	//TODO:: UPDATE STOPPING CRITERIA 
  	for (int num_iters = 0; num_iters < max_iterations; num_iters++){
  		//reset all of the counts to 0
  		reset_counts ();

  		//estimation step
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
  						normalize_val += t_map.getCount(target, prior);
  					}

  				  double delta = t_map.getCount(target, source) / normalize_val;

  					source_target_counts.incrementCount(source, target, delta);
  					source_counts.incrementCount(source, delta);
  				}
  			}
  		}

  		//maximization step
  		update_paramaters ();
  	}
  }

}