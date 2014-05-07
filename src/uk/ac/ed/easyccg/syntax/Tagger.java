package uk.ac.ed.easyccg.syntax;

import java.util.List;

import uk.ac.ed.easyccg.syntax.InputReader.InputWord;

public interface Tagger
{

  /**
   * Assigned a distribution over lexical categories for a list of words.
   * For each word in the sentence, it returns an ordered list of SyntaxTreeNode representing
   * their category assignment.
   */
  public abstract List<List<SyntaxTreeNode>> tag(List<InputWord> words);

}