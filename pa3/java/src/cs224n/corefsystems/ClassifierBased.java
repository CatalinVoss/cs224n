package cs224n.corefsystems;

import cs224n.coref.*;
import cs224n.util.Pair;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Triple;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import edu.stanford.nlp.util.logging.StanfordRedwoodConfiguration;
import cs224n.util.*;

import java.text.DecimalFormat;
import java.util.*;

import static edu.stanford.nlp.util.logging.Redwood.Util.*;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class ClassifierBased implements CoreferenceSystem {

	private static <E> Set<E> mkSet(E[] array){
		Set<E> rtn = new HashSet<E>();
		Collections.addAll(rtn, array);
		return rtn;
	}

	CounterMap<String, String> coreferentHeads = null;

	private static final Set<Object> ACTIVE_FEATURES = mkSet(new Object[]{

			/*
			 * TODO: Create a set of active features
			 */

			Feature.ExactMatch.class,
			// Feature.NumMentionsToPrev.class,
			// Feature.NumSentenceToPrev.class,
			// Feature.GenderEntityAgreement.class,
			// Feature.CandidateIsPronoun.class,  
			// Feature.OnPrixIsPronoun.class,
			// Feature.MatchingPronouns.class,
			// Feature.CandidateIsName.class,
			// Feature.OnPrixIsName.class,
			// Feature.MatchingNames.class,
			// Feature.OnPrixEntity.class,
			// Feature.CandidateEntity.class,
			// Feature.MatchingEntities.class,
			// Feature.CandidateLemma.class,
			// Feature.OnPrixLemmma.class,
			// Feature.MatchingLemmas.class,
			// Feature.CandidatePos.class,
			// Feature.OnPrixPos.class, 
			// Feature.MatchingPos.class,
			// Feature.ExactHeadMatch.class,
			// Feature.CoreferentHeadCount.class, 
			// //Feature.ClusterHeadMatch.class,
			// Feature.SameSpeaker.class,

			//skeleton for how to create a pair feature
			// Pair.make(Feature.ExactHeadMatch.class,	Feature.MatchingPronouns.class),
	});

	private static int getMentionsToPrev (Mention m1, Mention m2) {
		return m1.doc.indexOfMention(m2) - m1.doc.indexOfMention(m1);
	}

	private static int getSentencesToPrev (Mention m1, Mention m2) {
		List<Sentence> sentenceList = m1.doc.sentences;
		return sentenceList.indexOf(m2.sentence) - sentenceList.indexOf(m1.sentence);
	}

	private static boolean genderEntityAgreement (Mention m, Entity entity) {
		Pair<Boolean, Boolean> pair =  Util.haveGenderAndAreSameGender(m, entity);
		return pair.getFirst() && pair.getSecond();
	}

	//returns true if the mention head word matches any head word in the antecedent
  private static boolean clusterHeadMatch (Mention m1, Entity cluster) {
    String mentionHead = m1.headWord();
    for (Mention m : cluster) {
      if (m.headWord().equals(mentionHead)) {
        return true;
      }
    }
    return false;
  }


	private LinearClassifier<Boolean,Feature> classifier;

	public ClassifierBased(){
		StanfordRedwoodConfiguration.setup();
		RedwoodConfiguration.current().collapseApproximate().apply();
	}

	public FeatureExtractor<Pair<Mention,ClusteredMention>,Feature,Boolean> extractor = new FeatureExtractor<Pair<Mention, ClusteredMention>, Feature, Boolean>() {
		private <E> Feature feature(Class<E> clazz, Pair<Mention,ClusteredMention> input, Option<Double> count){
			
			//--Variables
			Mention onPrix = input.getFirst(); //the first mention (referred to as m_i in the handout)
			Mention candidate = input.getSecond().mention; //the second mention (referred to as m_j in the handout)
			Entity candidateCluster = input.getSecond().entity; //the cluster containing the second mention


			//--Features
			if(clazz.equals(Feature.ExactMatch.class)){
				//(exact string match)
				return new Feature.ExactMatch(onPrix.gloss().equals(candidate.gloss()));
			} else if (clazz.equals(Feature.NumMentionsToPrev.class)) {
				return new Feature.NumMentionsToPrev(getMentionsToPrev(onPrix, candidate));
			} else if (clazz.equals(Feature.NumSentenceToPrev.class)) {
				return new Feature.NumSentenceToPrev(getSentencesToPrev(onPrix, candidate));
			} else if (clazz.equals(Feature.GenderEntityAgreement.class)) {
				return new Feature.GenderEntityAgreement(genderEntityAgreement(onPrix, candidateCluster));
			} else if (clazz.equals(Feature.CandidateIsPronoun.class)) {
				return new Feature.CandidateIsPronoun(Pronoun.isSomePronoun(candidate.gloss()));
			} else if (clazz.equals(Feature.OnPrixIsPronoun.class)) {
				return new Feature.OnPrixIsPronoun(Pronoun.isSomePronoun(onPrix.gloss()));
			} else if (clazz.equals(Feature.MatchingPronouns.class)) {
				return new Feature.MatchingPronouns(Pronoun.isSomePronoun(onPrix.gloss()) == Pronoun.isSomePronoun(candidate.gloss()));
			} else if (clazz.equals(Feature.CandidateIsName.class)) {
				return new Feature.CandidateIsName(Name.isName(candidate.gloss()));
			} else if (clazz.equals(Feature.OnPrixIsName.class)) {
				return new Feature.OnPrixIsName(Name.isName(onPrix.gloss()));
			} else if (clazz.equals(Feature.MatchingNames.class)) {
				return new Feature.MatchingNames(Name.isName(candidate.gloss()) == Name.isName(onPrix.gloss()));
			} else if (clazz.equals(Feature.OnPrixEntity.class)) {
				return new Feature.OnPrixEntity(onPrix.headToken().nerTag());
			} else if (clazz.equals(Feature.CandidateEntity.class)) {
				return new Feature.CandidateEntity(candidate.headToken().nerTag());
			} else if (clazz.equals(Feature.MatchingEntities.class)) {
				return new Feature.MatchingEntities(candidate.headToken().nerTag().equals(onPrix.headToken().nerTag()));
			} else if (clazz.equals(Feature.CandidateLemma.class)) {
				return new Feature.CandidateLemma(candidate.headToken().lemma());
			} else if (clazz.equals(Feature.OnPrixLemmma.class)) {
				return new Feature.OnPrixLemmma(onPrix.headToken().lemma());
			} else if (clazz.equals(Feature.MatchingLemmas.class)) {
				return new Feature.MatchingLemmas(onPrix.headToken().lemma().equals(candidate.headToken().lemma()));
			} else if (clazz.equals(Feature.CandidatePos.class)) {
				return new Feature.CandidatePos(candidate.headToken().posTag());
			} else if (clazz.equals(Feature.OnPrixPos.class)) {
				return new Feature.OnPrixPos(onPrix.headToken().posTag());
			} else if (clazz.equals(Feature.MatchingPos.class)) {
				return new Feature.MatchingPos(onPrix.headToken().posTag().equals(candidate.headToken().posTag()));
			} else if (clazz.equals(Feature.ExactHeadMatch.class)) {
				return new Feature.ExactHeadMatch(onPrix.headToken().equals(candidate.headToken()));
			} else if (clazz.equals(Feature.CoreferentHeadCount.class)) {
				return new Feature.CoreferentHeadCount((int) coreferentHeads.getCount(onPrix.headToken().toString(), candidate.headToken().toString()));
			} else if (clazz.equals(Feature.ClusterHeadMatch.class)) {
				return new Feature.ClusterHeadMatch(clusterHeadMatch(onPrix, candidateCluster));
			} else if (clazz.equals(Feature.SameSpeaker.class)) {
				return new Feature.SameSpeaker (onPrix.headToken().speaker().equals(candidate.headToken().speaker()));
			}
				//else if(clazz.equals(Feature.NewFeature.class) {
				/*
				 * TODO: Add features to return for specific classes. Implement calculating values of features here.
				 */
			else {
				throw new IllegalArgumentException("Unregistered feature: " + clazz);
			}
		}

		@SuppressWarnings({"unchecked"})
		@Override
		protected void fillFeatures(Pair<Mention, ClusteredMention> input, Counter<Feature> inFeatures, Boolean output, Counter<Feature> outFeatures) {
			//--Input Features
			for(Object o : ACTIVE_FEATURES){
				if(o instanceof Class){
					//(case: singleton feature)
					Option<Double> count = new Option<Double>(1.0);
					Feature feat = feature((Class) o, input, count);
					if(count.get() > 0.0){
						inFeatures.incrementCount(feat, count.get());
					}
				} else if(o instanceof Pair){
					//(case: pair of features)
					Pair<Class,Class> pair = (Pair<Class,Class>) o;
					Option<Double> countA = new Option<Double>(1.0);
					Option<Double> countB = new Option<Double>(1.0);
					Feature featA = feature(pair.getFirst(), input, countA);
					Feature featB = feature(pair.getSecond(), input, countB);
					if(countA.get() * countB.get() > 0.0){
						inFeatures.incrementCount(new Feature.PairFeature(featA, featB), countA.get() * countB.get());
					}
				}
			}

			//--Output Features
			if(output != null){
				outFeatures.incrementCount(new Feature.CoreferentIndicator(output), 1.0);
			}
		}

		@Override
		protected Feature concat(Feature a, Feature b) {
			return new Feature.PairFeature(a,b);
		}
	};

	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		startTrack("Training");

		coreferentHeads = new CounterMap<String, String>();

    for (Pair<Document, List<Entity>> pair : trainingData) {
      Document doc = pair.getFirst();
      List<Entity> clusters = pair.getSecond();
      List<Mention> mentions = doc.getMentions();
      // For all coreferent mention pairs
      for (Entity e : clusters) {
        for (Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()) { // iterable and order-sensitive; BA and AB will both show up
          // Exclude he/she/it/etc.
          //if (!Pronoun.isSomePronoun(mentionPair.getFirst().headWord()) && !Pronoun.isSomePronoun(mentionPair.getSecond().headWord()))
            coreferentHeads.incrementCount(mentionPair.getFirst().headWord(), mentionPair.getSecond().headWord(), 1.0);
        }
      }
    }

		//--Variables
		RVFDataset<Boolean, Feature> dataset = new RVFDataset<Boolean, Feature>();
		LinearClassifierFactory<Boolean, Feature> fact = new LinearClassifierFactory<Boolean,Feature>();
		//--Feature Extraction
		startTrack("Feature Extraction");
		for(Pair<Document,List<Entity>> datum : trainingData){
			//(document variables)
			Document doc = datum.getFirst();
			List<Entity> goldClusters = datum.getSecond();
			List<Mention> mentions = doc.getMentions();
			Map<Mention,Entity> goldEntities = Entity.mentionToEntityMap(goldClusters);
			startTrack("Document " + doc.id);
			//(for each mention...)
			for(int i=0; i<mentions.size(); i++){
				//(get the mention and its cluster)
				Mention onPrix = mentions.get(i);
				Entity source = goldEntities.get(onPrix);
				if(source == null){ throw new IllegalArgumentException("Mention has no gold entity: " + onPrix); }
				//(for each previous mention...)
				int oldSize = dataset.size();
				for(int j=i-1; j>=0; j--){
					//(get previous mention and its cluster)
					Mention cand = mentions.get(j);
					Entity target = goldEntities.get(cand);
					if(target == null){ throw new IllegalArgumentException("Mention has no gold entity: " + cand); }
					//(extract features)
					Counter<Feature> feats = extractor.extractFeatures(Pair.make(onPrix, cand.markCoreferent(target)));
					//(add datum)
					dataset.add(new RVFDatum<Boolean, Feature>(feats, target == source));
					//(stop if
					if(target == source){ break; }
				}
				//logf("Mention %s (%d datums)", onPrix.toString(), dataset.size() - oldSize);
			}
			endTrack("Document " + doc.id);
		}
		endTrack("Feature Extraction");
		//--Train Classifier
		startTrack("Minimizer");
		this.classifier = fact.trainClassifier(dataset);
		endTrack("Minimizer");
		//--Dump Weights
		startTrack("Features");
		//(get labels to print)
		Set<Boolean> labels = new HashSet<Boolean>();
		labels.add(true);
		//(print features)
		for(Triple<Feature,Boolean,Double> featureInfo : this.classifier.getTopFeatures(labels, 0.0, true, 10000, true)){
			Feature feature = featureInfo.first();
			Boolean label = featureInfo.second();
			Double magnitude = featureInfo.third();
			log(FORCE,new DecimalFormat("0.000").format(magnitude) + " [" + label + "] " + feature);
		}
		end_Track("Features");
		endTrack("Training");
	}

	public List<ClusteredMention> runCoreference(Document doc) {
		//--Overhead
		startTrack("Testing " + doc.id);
		//(variables)
		List<ClusteredMention> rtn = new ArrayList<ClusteredMention>(doc.getMentions().size());
		List<Mention> mentions = doc.getMentions();
		int singletons = 0;
		//--Run Classifier
		for(int i=0; i<mentions.size(); i++){
			//(variables)
			Mention onPrix = mentions.get(i);
			int coreferentWith = -1;
			//(get mention it is coreferent with)
			for(int j=i-1; j>=0; j--){
				ClusteredMention cand = rtn.get(j);
				boolean coreferent = classifier.classOf(new RVFDatum<Boolean, Feature>(extractor.extractFeatures(Pair.make(onPrix, cand))));
				if(coreferent){
					coreferentWith = j;
					break;
				}
			}
			//(mark coreference)
			if(coreferentWith < 0){
				singletons += 1;
				rtn.add(onPrix.markSingleton());
			} else {
				//log("Mention " + onPrix + " coreferent with " + mentions.get(coreferentWith));
				rtn.add(onPrix.markCoreferent(rtn.get(coreferentWith)));
			}
		}
		//log("" + singletons + " singletons");
		//--Return
		endTrack("Testing " + doc.id);
		return rtn;
	}

	private class Option<T> {
		private T obj;
		public Option(T obj){ this.obj = obj; }
		public Option(){};
		public T get(){ return obj; }
		public void set(T obj){ this.obj = obj; }
		public boolean exists(){ return obj != null; }
	}
}
