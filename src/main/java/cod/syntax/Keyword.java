package cod.syntax;

/** *
        * This enum contains all of the keywords
        *  for the Coderive language.
        *
        *  **/
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
  
  // Loop
  FOR,
  BREAK,
  SKIP,
  
  // Used in loop and natural arrays
  IN,
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
  
  // Keyword for converting between safe and unsafe states
  CONTROL,
  
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

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}