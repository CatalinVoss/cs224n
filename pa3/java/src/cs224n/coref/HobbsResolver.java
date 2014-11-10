package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.coref.Sentence.Token;
import cs224n.coref.Pronoun;
import cs224n.util.*;
import cs224n.coref.StopWords;
import cs224n.ling.*;

import java.util.ArrayList;
import java.util.*;

public class HobbsResolver {

  // Returns whether m1 resolves to m2 or the other way around
  public static boolean matchesMentions(Mention m1, Mention m2) {
    return ((Pronoun.isSomePronoun(m1.headWord())&&(resolve(m1) == m2))||(Pronoun.isSomePronoun(m2.headWord())&&(resolve(m2) == m1)));
  }

  /**
   * Recuresively traverses up the tree until it stops
   */
  private static Tree<String> findUpward(Tree<String> startNode, LinkedList<Pair<String,Integer>> path, String[] labels, List<Tree<String>> downTraversal) {
    int start = downTraversal.lastIndexOf(startNode);
    if (start == -1) {
      // Should never get here
      throw new IllegalArgumentException("The downTraversal list does not contain root as an element.");
    }

    // Iterate in reverse starting at start node
    for (int i = start - 1; i >= 0; i--) {
      final Tree<String> node = (Tree<String>) downTraversal.get(i);
      if (Arrays.asList(labels).contains(node.getLabel())) {
        // Construct inverse path
        // TODO: this can be sped up
        path = node.pathToIndex(node.getPreOrderTraversal().lastIndexOf(startNode)); // note that this method assumes traversal from left to right over children downwards

        // Return node
        return node;
      }
    }

    // Should never get here
    // assert(john has a huge penis); // will always fail
    return null;
  }

  /**
   * @param includeLeft
   *        set to Boolean.TRUE to bound on right (include left), Boolean.FALSE to bound on left (include right), and null to bound nowhere
   */
  public static List<Tree<String>> boundedBFS(Tree<String> root, LinkedList<Pair<String,Integer>> boundary, Boolean includeLeft) {
    List<Tree<String>> result = new ArrayList<Tree<String>>();

    // If no root, throw
    if (root == null)
      throw new IllegalArgumentException("Called boundedBFS on null root.");

    Queue<Tree<String>> queue = new LinkedList<Tree<String>>();
    
    // Add children that are consistent with the path to the queue
    boolean isLeft = true;
    for (Tree<String> child : root.getChildren()) {
      boolean badChild = false;
        if (includeLeft != null) {
        for (Pair<String,Integer> p : boundary) {
          if (p.getSecond() == child.uniqueIndex) {
            badChild = true;
            break;
          }
        }
        if (badChild) {
          isLeft = false;
          continue; // this label is on the path, so continue (unless includeLeft == null of course). After this we are going to be to the right
        }
      }

      if ((includeLeft == null)||(isLeft && includeLeft == Boolean.TRUE)||(!isLeft && includeLeft == Boolean.FALSE)) {
          queue.add(child);
      }
    }

    // Breadth first search
    while (!queue.isEmpty()) {
      Tree<String> t = queue.remove();
      result.add(t);
      for (Tree<String> child : t.getChildren())
        queue.add(child);
    }
    return result;
  }

  /**
   * Returns the first NP tree it finds in the list. Returns null if it can't find any.
   */
  private static Tree<String> findFirstNP(List<Tree<String>> trees) {
    for (Tree<String> t : trees) {
      if (t.getLabel().equals("NP")) {
        return t;
      }
    }
    return null;
  }

  public static Tree<String> searchPrev(Mention m) {
    for (int i = m.doc.sentences.lastIndexOf(m.sentence)-1; i >= 0; i--) {
        Tree<String> result = findFirstNP(boundedBFS(m.doc.sentences.get(i).parse, null, null));
    }
    return null;
  }

  /**
   * Finds the mention in a document that the passed mention m points to
   */
  public static Mention resolve(Mention m) { // TODO: take out doc and make this dependent on sentence
    if (!Pronoun.isSomePronoun(m.headWord())) {
      throw new IllegalArgumentException("Mr. Hobb(e)s can only deal with pronouns. Calvin: Life's a lot more fun when you aren't responsible for your actions.");
    }

    // TODO: we can generalize this to return multiple weighted candidates (if we have to go to further steps, give them less weight)

    List<Tree<String>> sentenceTraversal = m.sentence.parse.getPreOrderTraversal();
    Tree<String> result = null;

    // Step 1: Begin at the NP immediately dominating the pronoun.
    Tree<String> start = findUpward(m.parse, null, new String[] {"NP"}, sentenceTraversal);

    // Step 2: Go up tree to first NP or S. Call this X, and the path p.
    LinkedList<Pair<String,Integer>> p = null;
    Tree<String> X = findUpward(start, p, new String[] {"S", "NP"}, sentenceTraversal);

    // Step 3: Traverse all branches below X to the left of p, left-to-right, breadth-first.
    //         Propose as antecedent any NP that has a NP or S between it and X.
    if (result == null) {
      result = findFirstNP(boundedBFS(X, p, true));
    }

    // Step 4: If X is the highest S in the sentence, traverse the parse trees of the previous sentences in the order of recency.
    //         Traverse each tree left-to-right, breadth first. When an NP is encountered, propose as antecedent.
    //         If X not the highest node, go to step 5.
    if (result == null && X == m.sentence.parse) {
      result = searchPrev(m);
    }

    // Step 5: From node X, go up the tree to the first NP or S. Call it X, and the path p.
    if (result == null) {
      X = findUpward(X, p, new String[] {"S", "NP"}, sentenceTraversal);
    }

    // Step 6: If X is an NP and the path p to X came from a non-head phrase of X (a specifier or adjunct, such as a possessive, PP,
    //         apposition, or relative clause), propose X as antecedent.
    if (result == null && X != null) {
      boolean goodPath = true;
      for (Pair<String,Integer> pair : p) { // TODO: check this is right.
        if (p.getFirst().equals("NP"))
          goodPath = false;
      }
      if (goodPath)
        result = X;
    }

    // Step 7: Traverse all branches below X to the left of the path, in a left-to-right, breadth first manner.
    //         Propose any NP encountered as the antecedent.
    if (result == null && X != null) {
      result = findFirstNP(boundedBFS(X, p, true));
    }

    // Step 8: If X is an S node, traverse all branches of X to the right of the path but do not go below any NP or S encountered.
    //         Propose any NP as the antecedent.
    if (result == null && X != null && X.getLabel().equals("S")) {
      result = findFirstNP(boundedBFS(X, p, false));
    }

    // Step 9: Go to step 4 (single iteration).
    if (result == null && X == m.sentence.parse) {
      result = searchPrev(m);
    }

    // Convert result tree to mention
    for (Mention m2 : m.doc.getMentions()) {
      if (m2.equals(m))
        continue;
      if (m2.parse.equals(result))
        return m2;
    }
    return null;  
  }
}