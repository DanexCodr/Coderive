package cod;

/* *
     *
     * This class is for centralization of keywords ensuring that strings won't break anything in the language internal codes.
     *
     * */

public class Constants {

/* * * *
     * KEYWORD TERMINOLOGIES and their EQUIVALENTS
     * unit  -  "package" if in java language.
     * get  -  "import" or "include" in any other languages.
     * ship  -   "public" or "pub" in any other programming  langages.
     * local  -  "private" in any other programming languages.
     *
 * * * */

    // For lower case
    public static final String
            share = "share",
            local = "local",
            unit = "unit",
            get = "get",
            extend = "extend",
            this_ = "this",
            var = "var",
            input_ = "input",
            output = "output",

            // Newly added constants
            if_ = "if",
            else_ = "else",
            elif_ = "elif",
            for_ = "for",
            in_ = "in",
            to_ = "to",
            by_ = "by",
            int_ = "int",
            string = "string",
            float_ = "float",
            bool = "bool",
            true_ = "true",
            false_ = "false"
            ;
    
    // For upper case
    public static final String
            _share = "share", 
            _local = "LOCAL",
            _unit = "UNIT",
            _get = "GET",
            _extend = "EXTEND",
            _this = "THIS",
            _var = "VAR",
            _output = "OUTPUT",

            // Newly added token names
            _input = "INPUT",
            _if = "IF",
            _else = "ELSE",
            _elif = "ELIF",
            _for = "FOR",
            _in = "IN",
            _to = "TO",
            _by = "BY",
            _int = "INT",
            _string = "STRING",
            _float = "FLOAT",
            _bool = "BOOL",
            
            // Other Token Names
            _eof = "EOF",
            _int_lit = "INT_LIT",
            _float_lit = "FLOAT_LIT",
            _string_lit = "STRING_LIT",
            _bool_lit = "BOOL_LIT",
            _id = "ID",
            _assign = "ASSIGN",
            _plus = "PLUS",
            _minus = "MINUS",
            _mul = "MUL",
            _div = "DIV",
            _mod = "MOD",
            _colon = "COLON",
            _gt = "GT",
            _lt = "LT",
            _gte = "GTE",
            _lte = "LTE",
            _eq = "EQ",
            _neq = "NEQ",
            _dot = "DOT",
            _comma = "COMMA",
            _lparen = "LPAREN",
            _rparen = "RPAREN",
            _lbrace = "LBRACE",
            _rbrace = "RBRACE",
            _lbracket = "LBRACKET",
            _rbracket = "RBRACKET",
            _plus_assign = "PLUS_ASSIGN",
            _minus_assign = "MINUS_ASSIGN",
            _mul_assign = "MUL_ASSIGN",
            _div_assign = "DIV_ASSIGN",
            _invalid = "INVALID"
            ;

}
