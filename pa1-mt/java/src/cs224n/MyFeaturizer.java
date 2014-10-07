package edu.stanford.nlp.mt.decoder.feat;

import java.util.List;

import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.decoder.feat.RuleFeaturizer;
import edu.stanford.nlp.util.Generics;

/**
 * A rule featurizer.
 */
public class MyFeaturizer implements RuleFeaturizer<IString, String> {

  protected final static String[] junkWords = {"le", "la", "les", "de", "of", "en", "in", "a", "from", "the"};
  protected final static String[] numbers = {"1", "2", "3", "4", "5", "5", "6", "7", "8", "9", "0"};
  protected final static String[] punct = {",", ".", ";", ":"};

  protected int countHits(String phrase, String[] arr) {
    int result = 0;
    for (String sub : arr) {
      if (phrase.contains(sub))
        result++;
    }
    return result+1; // "sort of laplace smoothing"
  }

  @Override
  public void initialize() {
    // Do any setup here.
  }

  @Override
  public List<FeatureValue<String>> ruleFeaturize(Featurizable<IString, String> f) {  
    // TODO: Return a list of features for the rule. Replace these lines
    // with your own feature.
    List<FeatureValue<String>> features = Generics.newLinkedList();
    // features.add(new FeatureValue<String>( String.format("TGTD:%d", f.targetPhrase.size()), 1.0));
    // features.add(new FeatureValue<String>( String.format("SCTD:%d", f.sourcePhrase.size()), 1.0));
    features.add(new FeatureValue<String>( String.format("COMB:%d_%d", f.targetPhrase.size(),f.sourcePhrase.size()), 1.0));
 
    // int d = Math.abs(f.sourcePhrase.size()-f.targetPhrase.size());
    // if (d < 3)
    //   features.add(new FeatureValue<String>("DONUT",1.0)); // f.sourcePhrase.size() > 2
    // features.add(new FeatureValue<String>(String.format("DIST:%d", d),1.0)); // f.sourcePhrase.size() > 2
    
    // if (f.targetPhrase.toString().contains(",")&&f.sourcePhrase.toString().contains(","))
    //   features.add(new FeatureValue<String>("GOODCOMMAS",1.0));
    // else if ((f.targetPhrase.toString().contains(",")&&!f.sourcePhrase.toString().contains(","))||(!f.targetPhrase.toString().contains(",")&&f.sourcePhrase.toString().contains(",")))
    //   features.add(new FeatureValue<String>("BADCOMMAS",1.0));

    // if (f.targetPhrase.toString().contains(".")&&f.sourcePhrase.toString().contains("."))
    //   features.add(new FeatureValue<String>("GOODPERIOD",1.0));
    // else if ((f.targetPhrase.toString().contains(".")&&!f.sourcePhrase.toString().contains("."))||(!f.targetPhrase.toString().contains(".")&&f.sourcePhrase.toString().contains(".")))
    //   features.add(new FeatureValue<String>("BADPERIOD",1.0));

    features.add(new FeatureValue<String>("PUNC_RATIO", (double)countHits(f.sourcePhrase.toString(), punct)/countHits(f.targetPhrase.toString(), punct) ));

    // if (countHits(f.targetPhrase.toString(), numbers) == countHits(f.sourcePhrase.toString(), numbers)) {
    //   features.add(new FeatureValue<String>("MATCHINGNUMS",1.0));
    // }

    // features.add(new FeatureValue<String>("JUNK-RATIO", (double)(countHits(f.targetPhrase.toString(), junkWords))/countHits(f.sourcePhrase.toString(), junkWords) ));

    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
