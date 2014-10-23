package cs224n.assignment;

import cs224n.ling.Tree;
import cs224n.util.*;
import java.util.*;
import cs224n.assignment.*;

/**
 * CKY PCFG Parser
 * @author John Miller
 * @author Catalin Voss
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    private Interner interner = new Interner ();

    private List<IdentityHashMap<Object, Double>> score;
    private List<IdentityHashMap<Object, Triplet<Integer, String, String>>> back;

    private final int kIndexBase = -1;
    private final String kRootNode = "ROOT";

    public void train(List<Tree<String>> trainTrees) {
        List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
        for (Tree<String> tree : trainTrees)
            { annotatedTrees.add(TreeAnnotations.annotateTree(tree)); }
        lexicon = new Lexicon(annotatedTrees);
        grammar = new Grammar(annotatedTrees);
    }

    private int getIndex (int x, int y) {
        return x + (y*(y-1)/2);
    }

    private void addToTree (Tree<String> parseTree, int start, int end, String prevRule) {
        Triplet<Integer, String, String> ruleSplit = back.get(getIndex(start, end)).get(interner.intern(prevRule));
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        if (ruleSplit == null) { // we've reached a terminal
            return;
        } else if (ruleSplit.getFirst() != kIndexBase) { //binary case
            Tree<String> leftChild = new Tree<String> (ruleSplit.getSecond());
            addToTree(leftChild, start, ruleSplit.getFirst(), ruleSplit.getSecond());
            Tree<String> rightChild = new Tree<String> (ruleSplit.getThird());
            addToTree(rightChild, ruleSplit.getFirst(), end, ruleSplit.getThird());

            children.add (leftChild);
            children.add (rightChild);
        } else { //unary case or terminals
            Tree<String> child = null;
            if (prevRule.equals(ruleSplit.getSecond()))
                child = new Tree<String> (ruleSplit.getSecond());
            else {
                child = new Tree<String> (ruleSplit.getSecond());
                addToTree (child, start, end, ruleSplit.getSecond());
            }
            children.add (child);
        }
        parseTree.setChildren(children);
    }

    /* Initialize the data structures */
    private void initializeTable (int len) {
      IdentityHashMap<Object, Double> s;
      IdentityHashMap<Object, Triplet<Integer, String, String>> b;
      for (int i = 0; i < len*(len+1)/2; i++) {
          s = new IdentityHashMap<Object, Double>();
          b = new IdentityHashMap<Object, Triplet<Integer, String, String>>();
          score.add(s);
          back.add(b);
      }
    }

    private void handleUnaries (IdentityHashMap<Object, Double> s, 
                                IdentityHashMap<Object, Triplet<Integer, 
                                String, String>> b) {
      Boolean added = true;
      while (added) {
        added = false;
        Set<Object> keySet = new HashSet<Object>(s.keySet()); // to avoid getting java.util.ConcurrentModificationException
        for (Object B : keySet) {                    
          if (s.get(B) > 0) {
            List<Grammar.UnaryRule> rules = grammar.getUnaryRulesByChild(B.toString());
            for (Grammar.UnaryRule unary : rules) {
                double prob = s.get(B) * unary.getScore();

              if (!s.containsKey(interner.intern(unary.getParent())) || prob > s.get(interner.intern(unary.getParent()))) {
                  added = true;
                  s.put(interner.intern(unary.getParent()), prob);
                  b.put(interner.intern(unary.getParent()), new Triplet<Integer, String, String>(kIndexBase, unary.getChild(), null));
              }
            }
          }
        }
      }
    }

    private void parseBase (List<String> sentence) {
      IdentityHashMap<Object, Double> s;
      IdentityHashMap<Object, Triplet<Integer, String, String>> b;
      for (int i = 0; i < sentence.size(); i++) {
        s = score.get(getIndex(i, i+1));
        b = back.get(getIndex(i, i+1));
        
        for (String tag : lexicon.getAllTags()) { // TODO: We can optimize by having getAllTags take a string word argument and only return the ones relevant to that word
            s.put(interner.intern(tag), lexicon.scoreTagging(sentence.get(i), tag));
            b.put(interner.intern(tag), new Triplet<Integer, String, String>(kIndexBase, sentence.get(i), null));
        }

        handleUnaries (s, b);

        score.set(getIndex(i,i+1), s);
        back.set(getIndex(i,i+1), b);
      }
    }

    private void parseCKY (List<String> sentence) {
      IdentityHashMap<Object, Double> s;
      IdentityHashMap<Object, Triplet<Integer, String, String>> b;
      for (int span = 2; span <= sentence.size(); span++) {
        for (int begin = 0; begin <= sentence.size()-span; begin++) {
          int end = begin+span;
          s = score.get(getIndex(begin,end));
          b = back.get(getIndex(begin, end));

          // Binary
          for (int split = begin+1; split <= end-1; split++) {
            IdentityHashMap<Object, Double> lscores = score.get(getIndex(begin,split));
            IdentityHashMap<Object, Double> rscores = score.get(getIndex(split,end)); 

            for (Object tag : lscores.keySet()) {
              for (Grammar.BinaryRule r : grammar.getBinaryRulesByLeftChild(tag.toString())) {
                if (!rscores.containsKey(interner.intern(r.getRightChild())))
                    continue;

                double prob = rscores.get(interner.intern(r.getRightChild()))*lscores.get(interner.intern(r.getLeftChild()))*r.getScore();
                if (!s.containsKey(interner.intern(r.getParent())) || prob > s.get(interner.intern(r.getParent()))) {
                    s.put(interner.intern(r.getParent()), prob);
                    b.put(interner.intern(r.getParent()), new Triplet<Integer, String, String>(split, r.getLeftChild(), r.getRightChild()));
                }
              }
            }
          }

          handleUnaries (s, b);

          score.set(getIndex(begin,end), s);
          back.set(getIndex(begin,end), b);
        }
      }
    }

    public Tree<String> getBestParse (List<String> sentence) {
      // Init datastructure
      int N = sentence.size()+1;
      score = new ArrayList< IdentityHashMap<Object, Double> >(N*(N+1)/2);
      back = new ArrayList<IdentityHashMap<Object, Triplet<Integer, String, String>>>(N*(N+1)/2);

      initializeTable(N);
      parseBase(sentence);
      parseCKY(sentence);

      Tree<String> parseTree = new Tree<String>(kRootNode);
      addToTree (parseTree, 0, sentence.size(), kRootNode);
      return TreeAnnotations.unAnnotateTree(parseTree);
    }
}