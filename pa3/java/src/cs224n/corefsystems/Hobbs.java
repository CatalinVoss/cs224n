package cs224n.corefsystems;

import java.util.Collection;
import java.util.ArrayList;
import java.util.*;

import cs224n.coref.*;
import cs224n.util.*;
import cs224n.ling.*;


public class Hobbs {

    enum TreeSide {
        LEFT,
        RIGHT,
        ALL
    }

    enum Label {
        None,
        NP,
        SorNP,
    }

    private static Mention candidateTreeToMention(Tree<String> candidate, Document doc, Mention orig) {
        assert (orig.parse != candidate);
        Mention result = null;
        for (Mention m : doc.getMentions()) {
            if (m == orig)
                continue;
            Tree<String> t = m.parse;
            boolean match = (candidate.equals(t));
            if (match) {
                result = m;
                break;
            }
        }
        return result;
    }

    private static Map<Tree<String>, Tree<String> > createSentenceMap(Tree<String> tree)
    {
        Map<Tree<String>, Tree<String> > map = new HashMap<Tree<String>, Tree<String>>();
        createSentenceMapHelper(map, tree);
        return map;
    }

    private static void createSentenceMapHelper(Map<Tree<String>, Tree<String> > map, Tree<String> parent)
    {
        if (parent == null)
            return;
        for (Tree<String> child : parent.getChildren()) {
            map.put(child, parent);
            createSentenceMapHelper(map, child);
        }
    }

    private static Tree<String> findParent(Tree<String> start, Label stopAt,
                                    Map<Tree<String>, Tree<String> > map,
                                    Set<Tree<String> > path) {
        if (path != null) path.clear();
        while (true) {
            if (path != null) path.add(start);

            start = map.get(start);
            if (start == null)
                break;

            if (start.getLabel().equals("NP") ||
                (stopAt == Label.SorNP && start.getLabel().equals("S")))
                    return start;
        }
        return null;
    }

    private static List<Tree<String> > getSideBFS(Tree<String> start,
                                           Set<Tree<String> >excludePath,
                                           TreeSide side) {
        List<Tree<String> > list = new ArrayList<Tree<String>>();
        if (start == null)
            return list;

        Queue<Tree<String> > q = new LinkedList<Tree<String>>();
        //enqueue children
        boolean beforePath = true;
        for (Tree<String> child : start.getChildren()) {
            if (side != TreeSide.ALL && excludePath.contains(child)) {
                beforePath = false;
                continue;
            }

            if ((side == TreeSide.ALL) ||
                (beforePath && side == TreeSide.LEFT) ||
                (!beforePath && side == TreeSide.RIGHT)) {
                q.add(child);
            }
        }

        while (!q.isEmpty()) {
            Tree<String> t = q.remove();
            list.add(t);
            for (Tree<String> child : t.getChildren()) {
                q.add(child);
            }
        }
        return list;
    }

    private static Tree<String> containsNP(List<Tree<String> > list) {
        for (Tree<String> t : list) {
            if (t.getLabel().equals("NP"))
                return t;
        }
        return null;
    }

    private static boolean isNonHeadPhrase(Set<Tree<String> > path) {
        if (path == null)
            return false;

        for (Tree<String> t : path) {
            if (t == null) continue;
            if (!t.getLabel().equals("NP"))
                return false;
        }
        return true;
    }

    private static Mention searchPreviousSentences(Document doc, Mention m) {
        int sentenceIndex = doc.sentences.lastIndexOf(m.sentence);
        for (int i = sentenceIndex; i >= 0; i--) {
            Tree<String> prevSentence = doc.sentences.get(i).parse;
            Tree<String> candidate = containsNP(getSideBFS(prevSentence,null,TreeSide.ALL));
            if (candidate != null)
                return candidateTreeToMention(candidate, doc, m);
        }
        return null;
    }

    public static Mention getHobbsCandidate(Document doc, Mention pronoun) {

        //hobbs algorithm only does pronoun resoution
        if (!Pronoun.isSomePronoun(pronoun.headWord()))
            return null;

        Sentence sentence = pronoun.sentence;
        Map<Tree<String>, Tree<String> > sentenceToParent = createSentenceMap(sentence.parse);
        Set<Tree<String> > p = new HashSet<Tree<String>>();
        Tree<String> candidate;
            // System.out.println("Begin at: "+pronoun.parse.toString());

        //step 1
        Tree<String> firstNP = findParent(pronoun.parse, Label.NP, sentenceToParent, null);

        if (firstNP != null)
            System.out.println("Good hobbs step 1: "+firstNP.toString());
        else
            System.out.println("Good hobbs step 1: null");

        //step 2
        Tree<String> X = findParent(firstNP, Label.SorNP, sentenceToParent, p);

        //step 3
        candidate = containsNP(getSideBFS(X,p,TreeSide.LEFT));
        if (candidate != null)
            return candidateTreeToMention(candidate, doc, pronoun);

        if (X == sentence.parse) {
            //step 4
            return searchPreviousSentences(doc, pronoun);
        }

        else {
            //step 5
            X = findParent(X, Label.SorNP, sentenceToParent, p);

            //step 6
            if (X != null && isNonHeadPhrase(p)) {
                return candidateTreeToMention(X, doc, pronoun);
            }

            //step 7
            candidate = containsNP(getSideBFS(X,p,TreeSide.LEFT));
            if (candidate != null)
                return candidateTreeToMention(candidate, doc, pronoun);

            //step 8
            if (X != null && X.getLabel().equals("S")) {
                candidate = containsNP(getSideBFS(X, p, TreeSide.RIGHT));
                if (candidate != null)
                    return candidateTreeToMention(candidate, doc, pronoun);
            }

            //step 9
            return searchPreviousSentences(doc, pronoun);


        }

    }


}
