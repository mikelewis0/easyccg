package uk.ac.ed.easyccg.syntax;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import uk.ac.ed.easyccg.syntax.InputReader.InputWord;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeFactory;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.io.PatternFilenameFilter;
import com.google.common.primitives.Doubles;

public class TaggerEmbeddings implements Tagger
{
  private final Matrix weightMatrix;
  private final Vector bias;

  private final Map<String, double[]> discreteFeatures;
  private final Map<String, double[]> embeddingsFeatures;

  private final List<Category> lexicalCategories;

  private final int totalFeatures;
  
  /**
   * Number of words forward/backward to use as context (so a value of 3 means the tagger looks at 3+3+1=7 words).
   */
  private final int contextWindow = 3;

  private final static String leftPad="*left_pad*";
  private final static String rightPad="*right_pad*";
  private final static String unknownLower="*unknown_lower*";
  private final static String unknownUpper="*unknown_upper*";
  private final static String unknownSpecial="*unknown_special*";

  private static final String capsLower = "*lower_case*";
  private static final String capsUpper = "*upper_case*";
  private final static String capitalizedPad="*caps_pad*";
  private final static String suffixPad="*suffix_pad*";
  private final static String unknownSuffix="*unknown_suffix*";

  /**
   * Number of supertags to consider for each word. Choosing 50 means it's effectively unpruned,
   * but saves us having to sort the complete list of categories.
   */
  private final static int beam = 50;
  private final SyntaxTreeNodeFactory terminalFactory;


  public TaggerEmbeddings(File file, int maxSentenceLength) {
    try {
      FilenameFilter embeddingsFileFilter = new PatternFilenameFilter("embeddings.*");

      embeddingsFeatures = loadEmbeddings(true, file.listFiles(embeddingsFileFilter));
      discreteFeatures = new HashMap<String, double[]>();
      discreteFeatures.putAll(loadEmbeddings(false, new File(file, "capitals")));
      discreteFeatures.putAll(loadEmbeddings(false, new File(file, "suffix")));
      totalFeatures = 
        (embeddingsFeatures.get(unknownLower).length + 
            discreteFeatures.get(unknownSuffix).length + 
            discreteFeatures.get(capsLower).length)*(2 * contextWindow + 1);
      lexicalCategories = loadCategories(new File(file, "categories"));
      weightMatrix = new DenseMatrix(lexicalCategories.size(), totalFeatures);
      loadMatrix(weightMatrix, new File(file, "classifier"));
      bias = new DenseVector(lexicalCategories.size());
      
      int maxCategoryID = 0;
      for (Category c : lexicalCategories) {
        maxCategoryID = Math.max(maxCategoryID, c.getID());
      }
      terminalFactory = new SyntaxTreeNodeFactory(maxSentenceLength, maxCategoryID);
      loadVector(bias, new File(file, "bias"));
    }
    catch (Exception e) {
      throw new Error(e);
    }
  }

  /**
   * Loads the neural network weight matrix.
   */
  private void loadMatrix(Matrix matrix, File file) throws IOException
  {
    Iterator<String> lines = Util.readFileLineByLine(file);
    int row=0;
    while (lines.hasNext()) {
      String line = lines.next();
      String[] fields = line.split(" ");
      for (int i = 0 ; i < fields.length; i++) {
        matrix.set(row, i, Double.valueOf(fields[i]));
      }

      row++;
    }
  }

  private void loadVector(Vector vector, File file) throws IOException
  {
    Iterator<String> lines = Util.readFileLineByLine(file);
    int row=0;
    while (lines.hasNext()) {

      String data = lines.next();
      vector.set(row, Double.valueOf(data));
      row++;
    }
  }

  private List<Category> loadCategories(File catFile) throws IOException
  {
    List<Category> categories = new ArrayList<Category>();
    Iterator<String> lines = Util.readFileLineByLine(catFile);
    while (lines.hasNext()) {
      categories.add(Category.valueOf(lines.next()));
    }

    return categories;
  }

  /* (non-Javadoc)
   * @see uk.ac.ed.easyccg.syntax.Tagger#tag(java.util.List)
   */
  @Override
  public List<List<SyntaxTreeNode>> tag(List<InputWord> words) {
    final List<List<SyntaxTreeNode>> result = new ArrayList<List<SyntaxTreeNode>>(words.size());
    final double[] vector = new double[totalFeatures];
    
    for (int wordIndex = 0; wordIndex < words.size(); wordIndex++) {
      int vectorIndex = 0;
      for (int sentencePosition = wordIndex - contextWindow; sentencePosition<= wordIndex+contextWindow; sentencePosition++) {
        vectorIndex = addToFeatureVector(vectorIndex, vector, sentencePosition, words);
      }

      result.add(getTagsForWord(new DenseVector(vector), words.get(wordIndex), wordIndex));
    }

    return result;
  }
  
  /**
   * Adds the features for the word in the specified position to the vector, and returns the next empty index in the vector.
   */
  private int addToFeatureVector(int vectorIndex, double[] vector, int sentencePosition, List<InputWord> words)
  {
    double[] embedding = getEmbedding(words, sentencePosition);
    vectorIndex = addToVector(vectorIndex, vector, embedding);
    double[] suffix = getSuffix(words, sentencePosition);
    vectorIndex = addToVector(vectorIndex, vector, suffix);
    double[] caps = getCapitalization(words, sentencePosition);
    vectorIndex = addToVector(vectorIndex, vector, caps);
    return vectorIndex;
  }

  private int addToVector(int index, double[] vector, double[] embedding)
  {
    System.arraycopy(embedding, 0, vector, index, embedding.length);
    index = index + embedding.length;
    return index;
  }

  private Map<String, double[]> loadEmbeddings(boolean normalize, File... embeddingsFiles) throws IOException {
    Map<String, double[]> embeddingsMap = new HashMap<String, double[]>();
    // Allow sharded input.
    for (File embeddingsFile : embeddingsFiles) {
      Iterator<String> lines = Util.readFileLineByLine(embeddingsFile);
      while (lines.hasNext()) {
        String line = lines.next();
        String word = line.substring(0, line.indexOf(" "));
        if (normalize) {
          word = normalize(word);
        }

        if (!embeddingsMap.containsKey(word)) {
          String[] fields = line.split(" ");
          double[] embeddings = new double[fields.length - 1];
          for (int i = 1 ; i< fields.length; i++) {
            embeddings[i - 1] = Double.valueOf(fields[i]);
          }
          embeddingsMap.put(word, embeddings);
        }
      }
    }

    return embeddingsMap;
  }

  /**
   * Normalizes words by lower-casing and replacing numbers with '#'/
   */
  private String normalize(String word) {
    word = word.toLowerCase().replaceAll("[0-9]", "#");
    return word;
  }

  /**
   * Loads the embedding for the word at the specified index in the sentence.
   * The index is allowed to be outside the sentence range, in which case
   * the appropriate 'padding' embedding is returned. 
   */
  private double[] getEmbedding(List<InputWord> words, int index)
  {
    if (index < 0) return embeddingsFeatures.get(leftPad);
    if (index >= words.size()) return embeddingsFeatures.get(rightPad);
    String word = words.get(index).word;

    double[] result = embeddingsFeatures.get(normalize(word));
    if (result == null) {
      char c = word.charAt(0);
      boolean isLower = 'a' <= c && c <= 'z';
      boolean isUpper = 'A' <= c && c <= 'Z';
      if (isLower) {
        return embeddingsFeatures.get(unknownLower);
      } else if (isUpper) {
        return embeddingsFeatures.get(unknownUpper);
      } else {
        return embeddingsFeatures.get(unknownSpecial);
      }
    }
    return result;
  }

  /**
   * Loads the embedding for a word's 2-character suffix.
   * The index is allowed to be outside the sentence range, in which case
   * the appropriate 'padding' embedding is returned. 
   */
  private double[] getSuffix(List<InputWord> words, int index)
  {
    String suffix = null;
    if (index < 0 || index >= words.size()) {
      suffix = suffixPad;
    } else {
      String word = words.get(index).word.toLowerCase();
      if (word.length() > 1) { 
        suffix = (word.substring(word.length() - 2, word.length()));
      } else {
        // Padding for words of length 1.
        suffix = ("_" + word.substring(0, 1));
      }
    }

    double[] result = discreteFeatures.get(suffix);
    if (result == null) {
      result = discreteFeatures.get(unknownSuffix);
    }
    return result;
  }

  /**
   * Loads the embedding for a word's capitalization.
   * The index is allowed to be outside the sentence range, in which case
   * the appropriate 'padding' embedding is returned. 
   */
  private double[] getCapitalization(List<InputWord> words, int index)
  {
    String key;
    if (index < 0 || index >= words.size()) {
      key = capitalizedPad;
    } else {
      String word = words.get(index).word;

      char c = word.charAt(0);
      if ('A' <= c && c <= 'Z') {
        key = capsUpper;
      } else {
        key = capsLower;
      }   
    }

    return discreteFeatures.get(key);
  }

  private static class ScoredCategory implements Comparable<ScoredCategory> {
    private final int id;
    private final double score;
    private ScoredCategory(int id, double score)
    {
      this.id = id;
      this.score = score;
    }
    @Override
    public int compareTo(ScoredCategory o)
    {
      return Doubles.compare(o.score, score);
    }
  }

  /**
   * Returns a list of @SyntaxTreeNode for this word, sorted by their probability. 
   * @param vector A vector
   * @param word The word itself.
   * @param wordIndex The position of the word in the sentence.
   * @return
   */
  private List<SyntaxTreeNode> getTagsForWord(final Vector vector, final InputWord word, final int wordIndex)
  {
    // Fixed length priority queue, used to sort candidate tags.
    MinMaxPriorityQueue<ScoredCategory> queue = MinMaxPriorityQueue.expectedSize(beam).maximumSize(beam).create();

    double total = 0.0;

    Vector tmpVector = bias.copy();
    weightMatrix.multAdd(vector, tmpVector);
    for (int i=0; i < weightMatrix.numRows(); i++) {
      double score = Math.exp(tmpVector.get(i));
      
      if (i < beam || score > queue.peekLast().score) {
        // Surprisingly, this 'if' condition makes things faster.
        queue.add(new ScoredCategory(i, score));
      }
      total += score;
    }

    // Convert the queue into a sorted list of SyntaxTreeNode terminals.
    List<SyntaxTreeNode> result = new ArrayList<SyntaxTreeNode>(beam);
    while (queue.size() > 0) {
      ScoredCategory cat = queue.poll();
      double probability = cat.score / total;
      result.add(terminalFactory.makeTerminal(
          word.word,           
          lexicalCategories.get(cat.id),
          word.pos,
          word.ner,
          Math.log(probability), 
          wordIndex)); 
    }
    
    return result;
  }
}
