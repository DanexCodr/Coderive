package cod.syntax;

import java.util.HashMap;
import java.util.Map;

/** 
 * This enum contains all of the keywords for the Coderive language.
 */
public enum Keyword {
  // Visibility modifiers
  SHARE,
  LOCAL,
  
  // namespace
  UNIT,
  
  // import
  USE,
  
  // extends & instanceof
  IS,
  
  THIS,
  SUPER,
  
  IF,
  ELSE,
  ELIF,
  
  OF,
  
  // Loop
  FOR,
  BREAK,
  SKIP, /* Equivalent of CONTINUE */
  
  // Used in loop and natural arrays
  TO,
  BY,
  
  // Primitive types
  INT,
  TEXT,
  FLOAT,
  BOOL,
  
  // metaprimitive type
  TYPE,
  
  // for policy classes declaration.
  POLICY,
  WITH,

  BUILTIN,
    
  ALL,
  ANY,
  
  EXIT,
  
  NONE,
  TRUE,
  FALSE,
  
  // For more control and auto wrappings of variables
  GET,
  SET,
  
  // Toggle strictly after visibility modifier to be able to use unsafe primitives and do low level operations
  UNSAFE,
  
  // Unsafe primitives (for low-level operations)
  I8,
  I16,
  I32,
  I64,
  U8,
  U16,
  U32,
  U64,
  F32,
  F64;

  private static final Map<String, Keyword> STRING_TO_KEYWORD = new HashMap<>();
  
  static {
    for (Keyword keyword : values()) {
      STRING_TO_KEYWORD.put(keyword.toString(), keyword);
    }
  }
  
  public static Keyword fromString(String text) {
    return STRING_TO_KEYWORD.get(text.toLowerCase());
  }
  
  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
