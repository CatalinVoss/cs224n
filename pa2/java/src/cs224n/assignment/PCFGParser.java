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

    private List<HashMap<String, Double>> score;
    private List<HashMap<String, Triplet<Integer, String, String>>> back;

    private final int kIndexBase = -1;
    private final String kRootNode = "ROOT";

    public void train(List<Tree<String>> trainTrees) {
        List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
        for (Tree<String> tree : trainTrees)
            { annotatedTrees.add(TreeAnnotations.annotateTree(tree)); }
        lexicon = new Lexicon(annotatedTrees);
        grammar = new Grammar(annotatedTrees);
    }

    private int getIndex(int x, int y) {
        return x + (y*(y-1)/2);
    }

    private void addToTree (Tree<String> parseTree, int start, int end, String prevRule) {
        Triplet<Integer, String, String> ruleSplit = back.get(getIndex(start, end)).get(prevRule);
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

    public Tree<String> getBestParse(List<String> sentence) {
        // Init datastructure
        int N = sentence.size()+1;
        score = new ArrayList< HashMap<String, Double> >(N*(N+1)/2);
        back = new ArrayList<HashMap<String, Triplet<Integer, String, String>>>(N*(N+1)/2);

        HashMap<String, Double> s;
        HashMap<String, Triplet<Integer, String, String>> b;
        for (int i = 0; i < N*(N+1)/2; i++) { // TODO: fix this ugly mess.
            s = new HashMap<String, Double>();
            b = new HashMap<String, Triplet<Integer, String, String>>();
            score.add(s);
            back.add(b);
        }

        for (int i = 0; i < sentence.size(); i++) {
            s = new HashMap<String, Double>();
            b = new HashMap<String, Triplet<Integer, String, String>>();
            
            for (String tag : lexicon.getAllTags()) { // TODO: We can optimize by having getAllTags take a string word argument and only return the ones relevant to that word
                s.put(tag, lexicon.scoreTagging(sentence.get(i), tag));
                b.put(tag, new Triplet<Integer, String, String>(kIndexBase, sentence.get(i), null));
            }

            Boolean added = true;
            while (added) {
                added = false;
                    Set<String> keySet = new HashSet<String>(s.keySet()); // to avoid getting java.util.ConcurrentModificationException
                    for (String B : keySet) {                    if (s.get(B) > 0) {
                        List<Grammar.UnaryRule> rules = grammar.getUnaryRulesByChild(B);
                        for (Grammar.UnaryRule unary : rules) {
                            double prob = s.get(B) * unary.getScore();

                            if (!s.containsKey(unary.getParent()) || prob > s.get(unary.getParent())) {
                                added = true;
                                s.put(unary.getParent(), prob);
                                b.put(unary.getParent(), new Triplet<Integer, String, String>(kIndexBase, unary.getChild(), null));
                            }
                        }
                    }
                }
            }

            score.set(getIndex(i,i+1), s);
            back.set(getIndex(i,i+1), b);
        }

        for (int span = 2; span <= sentence.size(); span++) {
            for (int begin = 0; begin <= sentence.size()-span; begin++) {
                int end = begin+span;
                s = new HashMap<String, Double>();
                b = new HashMap<String, Triplet<Integer, String, String>>();

                // Binary
                for (int split = begin+1; split <= end-1; split++) {
                    HashMap<String, Double> lscores = score.get(getIndex(begin,split)); // TODO: check this
                    HashMap<String, Double> rscores = score.get(getIndex(split,end));   // TODO: check this

                    for (String tag : lscores.keySet()) {
                        for (Grammar.BinaryRule r : grammar.getBinaryRulesByLeftChild(tag)) {
                            if (!rscores.containsKey(r.getRightChild()))
                                continue;

                            double prob = rscores.get(r.getRightChild())*lscores.get(r.getLeftChild())*r.getScore();
                            if (!s.containsKey(r.getParent()) || prob > s.get(r.getParent())) {
                                s.put(r.getParent(), prob);
                                b.put(r.getParent(), new Triplet<Integer, String, String>(split, r.getLeftChild(), r.getRightChild()));
                            }
                        }
                    }
                }

                // Unary rules
                Boolean added = true;
                while (added) {
                    added = false;
                    Set<String> keySet = new HashSet<String>(s.keySet()); // to avoid getting java.util.ConcurrentModificationException
                    for (String B : keySet) {
                        if (s.get(B) > 0) {
                            List<Grammar.UnaryRule> rules = grammar.getUnaryRulesByChild(B);
                            for (Grammar.UnaryRule unary : rules) {
                                double prob = s.get(B) * unary.getScore();
                                if (!s.containsKey(unary.getParent()) || prob > s.get(unary.getParent())) {
                                    added = true;
                                    s.put(unary.getParent(), prob);
                                    b.put(unary.getParent(), new Triplet<Integer, String, String>(kIndexBase, unary.getChild(), null));
                                }
                            }
                        }
                    }
                }

                score.set(getIndex(begin,end), s);
                back.set(getIndex(begin,end), b);
            }
        }

        Tree<String> parseTree = new Tree<String>(kRootNode);
        addToTree (parseTree, 0, sentence.size(), kRootNode);
        return TreeAnnotations.unAnnotateTree(parseTree);
    }
}