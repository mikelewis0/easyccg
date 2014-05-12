package uk.ac.ed.easyccg.syntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public abstract class Category {
  private final String cat;
  private final int id;
  private final static String WILDCARD_FEATURE = "X"; 
  private final static Set<String> punctuationCategories = ImmutableSet.of("LRB", "RRB", "LQU", "RQU");

  
  private Category(String cat) {
    this.cat = cat;
    this.id = numCats;
    numCats++;
  }
  
  enum Slash {
    FWD, BWD, EITHER;
    public String toString()
    {
      String result = "";
      
      switch (this)
      {
      case FWD: result = "/"; break;
      case BWD: result = "\\"; break;
      case EITHER: result = "|"; break;

      }
      
      return result;
    }
    
    public static Slash fromString(String text) {
      if (text != null) {
        for (Slash slash : values()) {
          if (text.equalsIgnoreCase(slash.toString())) 
          {
            return slash;
          }
        }
      }
      throw new IllegalArgumentException("Invalid slash: " + text);
    }

    public boolean matches(Slash other)
    {
      return this == EITHER || this == other;
    }
  }
  
  static int numCats = 0;
  private final static Map<String, Category> cache = new HashMap<String, Category>();
  public static final Category COMMA = Category.valueOf(",");
  public static final Category SEMICOLON = Category.valueOf(";");
  public static final Category CONJ = Category.valueOf("conj");
  public final static Category N = valueOf("N"); 
  public static final Category LQU = Category.valueOf("LQU");
  public static final Category LRB = Category.valueOf("LRB");

  public static Category valueOf(String cat) {
    
    Category result = cache.get(cat);
    if (result == null) {
      
      String name = Util.dropBrackets(cat);
      result = cache.get(name);

      if (result == null) {
        result = Category.fromStringUncached(name);
        if (name != cat) {
          cache.put(name, result);
        }
      }
      
      cache.put(cat, result);
    }
    
    return result;
  }
  
  private static Category fromStringUncached(String source)
  {
    String newSource = source;
      if (newSource.startsWith("("))
      {
        int closeIndex = Util.findClosingBracket(newSource);

        if (Util.indexOfAny(newSource.substring(closeIndex), "/\\|") == -1)
        {
          // Simplify (X) to X
          newSource = newSource.substring(1, closeIndex);
          Category result = fromStringUncached(newSource);

          return result;
        }
      }

      int endIndex = newSource.length();
      
      int opIndex =  Util.findNonNestedChar(newSource, "/\\|");
      
      
      if (opIndex == -1)
      {
        // Atomic Category
        int featureIndex = newSource.indexOf("[");
        List<String> features = new ArrayList<String>();

        String base = (featureIndex == -1 ? newSource : newSource.substring(0, featureIndex));

        while (featureIndex > -1)
        {
          features.add(newSource.substring(featureIndex + 1, newSource.indexOf("]", featureIndex)));
          featureIndex = newSource.indexOf("[", featureIndex + 1);
        }

        if (features.size() > 1) {
          throw new RuntimeException("Can only handle single features: " + source);
        }
        
        Category c = new AtomicCategory(base, features.size() == 0 ? null : features.get(0));
        return c;
      }
      else
      {
        // Functor Category

        Category left = valueOf(newSource.substring(0, opIndex));
        Category right = valueOf(newSource.substring(opIndex + 1, endIndex));
       return new FunctorCategory(left, 
            Slash.fromString(newSource.substring(opIndex, opIndex + 1)), 
            right
            );
      }
  }  
  
  public String toString() {
    return cat;
  }
  
  @Override
  public boolean equals(Object other) {
    return this == other;
  }
  
  @Override 
  public int hashCode() {
    return id;
  }
  
  abstract boolean isTypeRaised();
  
  abstract boolean isModifier();
  abstract boolean matches(Category other);
  abstract Category getLeft();
  abstract Category getRight();
  abstract Slash getSlash();
  abstract String getFeature();

  abstract String toStringWithBrackets();
  
  static class FunctorCategory extends Category {
    private final Category left;
    private final Category right;
    private final Slash slash;
    private final boolean isMod;
    
    private FunctorCategory(Category left, Slash slash, Category right) {
      super(left.toStringWithBrackets() + slash + right.toStringWithBrackets());
      this.left = left;
      this.right = right;
      this.slash = slash;
      this.isMod = left.equals(right);
    }

    @Override
    boolean isModifier()
    {
      return isMod;
    }

    @Override
    boolean matches(Category other)
    {
      return other.isFunctor() && left.matches(other.getLeft()) && right.matches(other.getRight()) && slash.matches(other.getSlash());
    }

    @Override
    Category getLeft()
    {
      return left;
    }

    @Override
    Category getRight()
    {
      return right;
    }

    @Override
    Slash getSlash()
    {
      return slash;
    }

    @Override
    String getFeature()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    String toStringWithBrackets()
    {
      return "(" + toString() + ")";
    }

    @Override
    public boolean isFunctor()
    {
      return true;
    }

    @Override
    public boolean isPunctuation()
    {
      return false;
    }

    @Override
    String getType()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    String getSubstitution(Category other)
    {
      String result = getRight().getSubstitution(other.getRight());
      if (result == null) {
        // Bit of a hack, but seems to reproduce CCGBank in cases of clashing features.
        result = getLeft().getSubstitution(other.getLeft());
      }
      return result;
    }

    @Override
    public boolean isTypeRaised()
    {
      // X|(X|Y)
      return getRight().isFunctor() && getRight().getLeft().equals(getLeft());
    }

    @Override
    boolean isNounOrNP()
    {
      return false;
    }
  }

  abstract String getSubstitution(Category other);

  
  static class AtomicCategory extends Category {

    private AtomicCategory(String type, String feature)
    {
      super(type + (feature == null ? "" : "[" + feature + "]"));
      this.type = type;
      this.feature = feature;
      isPunctuation = !type.matches("[A-Za-z]+") || punctuationCategories.contains(type);
    }

    private final String type;
    private final String feature;
    @Override
    boolean isModifier()
    {
      return false;
    }

    @Override
    boolean matches(Category other)
    {
      return !other.isFunctor() && type.equals(other.getType()) && 
      (feature == null || feature.equals(other.getFeature()) || WILDCARD_FEATURE.equals(getFeature()) || WILDCARD_FEATURE.equals(other.getFeature())
         || type.equals("N") || type.equals("NP")
         || "num".equals(getFeature()) // Ignoring the "num" feature, which is inconsistently used.
      );
    }

    @Override
    Category getLeft()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    Category getRight()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    Slash getSlash()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    String getFeature()
    {
      return feature;
    }

    @Override
    String toStringWithBrackets()
    {
      return toString();
    }

    @Override
    public boolean isFunctor()
    {
      return false;
    }

    private final boolean isPunctuation;
    @Override
    public boolean isPunctuation()
    {
      return isPunctuation ;
    }

    @Override
    String getType()
    {
      return type;
    }

    @Override
    String getSubstitution(Category other)
    {
      if (WILDCARD_FEATURE.equals(getFeature())) {
        return other.getFeature();
      } else if (WILDCARD_FEATURE.equals(other.getFeature())) {
        return feature;
      }
      return null;
    }

    @Override
    public boolean isTypeRaised()
    {
      return false;
    }

    @Override
    boolean isNounOrNP()
    {
      return type.equals("N") || type.equals("NP");
    }    
  }

  public static Category make(Category left, Slash op, Category right)
  {
    return valueOf(left.toStringWithBrackets() + op + right.toStringWithBrackets());
  }

  abstract String getType();

  abstract boolean isFunctor();

  abstract boolean isPunctuation();
  
  /**
   * Returns the Category created by substituting all [X] wildcard features with the supplied argument.
   */
  Category doSubstitution(String substitution)
  {
    if (substitution == null) return this;
    return valueOf(toString().replaceAll(WILDCARD_FEATURE, substitution));
  }

  /**
   * A unique numeric identifier for this category.
   */
  int getID()
  {
    return id;
  }

  abstract boolean isNounOrNP();
}