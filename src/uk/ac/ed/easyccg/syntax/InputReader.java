package uk.ac.ed.easyccg.syntax;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;

import uk.ac.ed.easyccg.main.EasyCCG.InputFormat;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeFactory;

public abstract class InputReader
{

  static class InputWord {

    private InputWord(String word, String pos, String ner)
    {
      this.word = word;
      this.pos = pos;
      this.ner = ner;
    }
    public InputWord(String word)
    {
      this (word, null, null);
    }
    public final String word;
    public final String pos;
    public final String ner;
  }
  
  public Iterable<InputToParser> readFile(File input) throws IOException {
    final Iterator<String> inputIt = Util.readFileLineByLine(input);
    
    
    return new Iterable<InputToParser>() {

      @Override
      public Iterator<InputToParser> iterator()
      {
        return new Iterator<InputToParser>() {

          private InputToParser next = getNext();
          @Override
          public boolean hasNext()
          {
            return next != null;
          }

          private InputToParser getNext()
          {         
            while (inputIt.hasNext()) {
              String nextLine = inputIt.next();
              if (!nextLine.startsWith("#") && !nextLine.isEmpty()) {
                // Skip commented or empty lines;                
                return readInput(nextLine);
              }
            }
            
            return null;
          }
 
          @Override
          public InputToParser next()
          {
            InputToParser result = next;
            next = getNext();
            return result;
          }

          @Override
          public void remove()
          {
            throw new UnsupportedOperationException();
          }
          
        };
      }
      
    };
  }
  
  abstract InputToParser readInput(String line);
  
  static class InputToParser {
    private final List<InputWord> words;
    InputToParser(List<InputWord> words, List<Category> goldCategories, List<List<SyntaxTreeNode>> inputSupertags)
    {
      this.words = words;
      this.goldCategories = goldCategories;
      this.inputSupertags = inputSupertags;
    }
    private final List<Category> goldCategories;
    private final List<List<SyntaxTreeNode>> inputSupertags;
    public int length()
    {
      return words.size();
    }
    public boolean isAlreadyTagged()
    {
      return getInputSupertags() != null;
    }
    public List<List<SyntaxTreeNode>> getInputSupertags()
    {
      return inputSupertags;
    }
    public boolean haveGoldCategories()
    {
      return getGoldCategories() != null;
    }
    
    public List<Category> getGoldCategories()
    {
      return goldCategories;
    }

    public List<InputWord> getInputWords() {
      return words;
     
    }
    
    public String getWordsAsString()
    {
      StringBuilder result = new StringBuilder();
      for (InputWord word : words) {
        result.append(word.word + " ");
      }
      
      return result.toString().trim();
    }

    public static InputToParser fromTokens(List<String> tokens) {
      List<InputWord> inputWords = new ArrayList<InputWord>(tokens.size());
      for (String word : tokens) {
        inputWords.add(new InputWord(word, null, null));
      }
      return new InputToParser(inputWords, null, null);
    }
    
  }
  

  private static class RawInputReader extends InputReader {

    @Override
    InputToParser readInput(String line)
    {
      //TODO quotes
      return InputToParser.fromTokens(Arrays.asList(line.replaceAll("\"", "").replaceAll("  +", " ").trim().split(" ")));
    }
  }

  /**
   *  Reads input tagged with a distribution of supertags. The format can be produced running the C&C supertagger with the output format: %w\t%p\t%S|\n
   *  
   *  Example:
   *  Pierre  NNP     0       N/N     0.99525070603732        N       0.0026450007306822|Vinken       NNP     0       N       0.70743834018551        S/S...
   */
  private static class SuperTaggedInputReader extends InputReader {

    private SuperTaggedInputReader(SyntaxTreeNodeFactory nodeFactory)
    {
      this.nodeFactory = nodeFactory;
    }

    private final SyntaxTreeNodeFactory nodeFactory;
    
    @Override
    InputToParser readInput(String line) {
    // Pierre  NNP     0       N/N     0.99525070603732        N       0.0026450007306822|Vinken       NNP     0       N       0.70743834018551        S/S     0.14043752457392
      List<List<SyntaxTreeNode>> supertags = new ArrayList<List<SyntaxTreeNode>>();

      String[] split = line.split("|");
      List<InputWord> words = new ArrayList<InputWord>(split.length);
      for (String wordTags :  split) {
        
        List<SyntaxTreeNode> entries = new ArrayList<SyntaxTreeNode>();
        String[] fields = wordTags.split("(\t| )+");
        
        String word = fields[0];
        if (fields.length < 5) throw new InputMismatchException("Expected input using the C&C with output format --ofmt \"%w\\t%p\\t%S|\\n\" but was: " + line);

        words.add(new InputWord(word));
        
        for (int i=3; i < fields.length; i=i+2) {
          Category cat = Category.valueOf(fields[i]);
          entries.add(nodeFactory.makeTerminal(word, cat, fields[1], null, Math.log(Double.valueOf(fields[i + 1])), entries.size()));
        }
        supertags.add(entries);
      }
      
      return new InputToParser(words, null, supertags);
    }
    
  }
  
  private static class GoldInputReader extends InputReader {

    @Override
    InputToParser readInput(String line)
    {
      List<Category> result = new ArrayList<Category>();
      String[] goldEntries = line.split(" ");
      List<InputWord> words = new ArrayList<InputWord>(goldEntries.length);
      for (String entry : goldEntries) {
        String[] goldFields = entry.split("\\|");
        
        if (goldFields[0].equals("\"")) continue ; //TODO quotes
        if (goldFields.length < 3) throw new InputMismatchException("Invalid input: expected \"word|POS|SUPERTAG\" but was: " + entry);

        words.add(new InputWord(goldFields[0]));
        result.add(Category.valueOf(goldFields[2]));
      }
      return new InputToParser(words, result, null);
    }    
  }
  
  private static class POSTaggedInputReader extends InputReader
  {

    @Override
    InputToParser readInput(String line)
    {
      String[] taggedEntries = line.split(" ");
      List<InputWord> inputWords = new ArrayList<InputWord>(taggedEntries.length);
      for (String entry : taggedEntries) {
        String[] taggedFields = entry.split("\\|");
        
        if (taggedFields.length < 2) throw new InputMismatchException("Invalid input: expected \"word|POS\" but was: " + entry);
        if (taggedFields[0].equals("\"")) continue ; //TODO quotes
        inputWords.add(new InputWord(taggedFields[0], taggedFields[1], null));
      }
      return new InputToParser(inputWords, null, null);
    }    
  }

  private static class POSandNERTaggedInputReader extends InputReader
  {
    @Override
    InputToParser readInput(String line)
    {
      String[] taggedEntries = line.split(" ");
      List<InputWord> inputWords = new ArrayList<InputWord>(taggedEntries.length);
      for (String entry : taggedEntries) {
        String[] taggedFields = entry.split("\\|");
        
        if (taggedFields[0].equals("\"")) continue ; //TODO quotes
        if (taggedFields.length < 3) throw new InputMismatchException("Invalid input: expected \"word|POS|NER\" but was: " + entry + "\n" +
        		"The C&C can produce this format using: \"bin/pos -model models/pos | bin/ner -model models/ner -ofmt \"%w|%p|%n \\n\"\"" );
        inputWords.add(new InputWord(taggedFields[0], taggedFields[1], taggedFields[2]));
      }
      return new InputToParser(inputWords, null, null);
    }    
  }

  public static InputReader make(InputFormat inputFormat, SyntaxTreeNodeFactory nodeFactory)
  {
    switch (inputFormat) {
    case SUPERTAGGED : return new SuperTaggedInputReader(nodeFactory);  
    case TOKENIZED : return new RawInputReader();  
    case GOLD : return new GoldInputReader();  
    case POSTAGGED : return new POSTaggedInputReader();  
    case POSANDNERTAGGED : return new POSandNERTaggedInputReader();  
    default : throw new Error("Unknown input format: " + inputFormat);
    }
  }
}
