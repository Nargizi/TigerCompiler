grammar Tiger;

//Grammar
tiger_program: PROGRAM ID LET declaration_segment BEGIN funct_list END;
declaration_segment: type_declaration_list var_declaration_list;
type_declaration_list: type_declaration type_declaration_list | /* epsilon */;
var_declaration_list: var_declaration var_declaration_list | /* epsilon */;
funct_list: funct funct_list | /* epsilon */;
type_declaration returns [String id, String varType, int varSize, boolean isArray]
            : TYPE ID TASSIGN type SEMICOLON {$id = $ID.text;
                                              $varType = $type.varType;
                                              $varSize = $type.varSize;
                                              $isArray = $type.isArray;}
            ;
type returns [String varType, int varSize = 0, boolean isArray = false]
            : base_type {$varType = $base_type.varType;}
            | ARRAY OPENBRACK INTLIT CLOSEBRACK OF base_type {$varType = $base_type.varType;
                                                              $varSize = $INTLIT.int;
                                                              $isArray = true;}
            | ID {$varType = $ID.text;}
            ;
base_type returns [String varType]
            : INT {$varType = $INT.text;}
            | FLOAT {$varType = $FLOAT.text;}
            ;
var_declaration returns [String storageClass, String varType, int varSize, boolean isArray, List<String> idList]
            : storage_class id_list COLON type optional_init SEMICOLON {$storageClass = $storage_class.storageClass;
                                                                        $varType = $type.varType;
                                                                        $varSize = $type.varSize;
                                                                        $isArray = $type.isArray;
                                                                        $idList = $id_list.idList;}
            ;
storage_class returns [String storageClass]
            : VAR {$storageClass = $VAR.text;}
            | STATIC {$storageClass = $STATIC.text;}
            ;
id_list returns [ArrayList<String> idList = new ArrayList<String>()]
            : ID {$idList.add($ID.text);}
            | ID COMMA id_list {$idList.add($ID.text);
                                $idList.addAll($id_list.idList);}
            ;
optional_init: ASSIGN const_ | /* epsilon */;
funct returns [String id, String retType, List<String> params]
            : FUNCTION ID OPENPAREN param_list CLOSEPAREN ret_type BEGIN stat_seq END {$id = $ID.text;
                                                                                       $retType = $ret_type.varType;
                                                                                       $params = $param_list.params;}
            ;
param_list returns [List<String> params = new ArrayList<>()]
            : param param_list_tail {$params.add($param.varType);
                                     $params.addAll($param_list_tail.params);}
            | /* epsilon */
            ;
param_list_tail returns [List<String> params = new ArrayList<>()]
            : COMMA param param_list_tail{$params.add($param.varType);
                                          $params.addAll($param_list_tail.params);}
            | /* epsilon */
            ;
ret_type returns [String varType]
            : COLON type {$varType = $type.varType;}
            | /* epsilon */
            ;
param returns [String varType, String id]
            : ID COLON type {$varType = $type.varType;
                             $id = $ID.text;}
            ;
stat_seq: stat | stat stat_seq;
stat: value ASSIGN expr SEMICOLON |
      IF expr THEN stat_seq ENDIF SEMICOLON |
      IF expr THEN stat_seq ELSE stat_seq ENDIF SEMICOLON |
      WHILE expr DO stat_seq ENDDO SEMICOLON |
      FOR ID ASSIGN expr TO expr DO stat_seq ENDDO SEMICOLON |
      optprefix ID OPENPAREN expr_list CLOSEPAREN SEMICOLON |
      BREAK SEMICOLON |
      RETURN optreturn SEMICOLON |
      let_stat;
let_stat: LET declaration_segment BEGIN stat_seq END;
optreturn: expr | /* epsilon */;
optprefix: value ASSIGN | /* epsilon */;
expr returns [String varType]
            : precedence_or
            ;
precedence_or returns [String varType]
            : precedence_or OR precedence_and
            | precedence_and
            ;
precedence_and returns [String varType]
            : precedence_and AND precedence_compare
            | precedence_compare
            ;
precedence_compare returns [String varType]
            : precedence_plus_minus ((EQUAL | NEQUAL | LESS |
                    GREAT | GREATEQ | LESSEQ) precedence_plus_minus)?
            ;
precedence_plus_minus returns [String varType]
            : precedence_plus_minus (PLUS | MINUS) precedence_mult_div
            | precedence_mult_div
            ;
precedence_mult_div returns [String varType]
            : precedence_mult_div (MULT | DIV) precedence_pow
            | precedence_pow
            ;
precedence_pow returns [String varType]
            : precedence_paren POW precedence_pow
            | precedence_paren
            ;
precedence_paren returns [String varType]
            : OPENPAREN expr CLOSEPAREN
            | precedence_trail
            ;
precedence_trail returns [String varType]
            : const_
            | value
            ;
value returns [String varType]
            : ID value_tail
            ;
const_ returns [String varType]
            : INTLIT {$varType = "INT";}
            | FLOATLIT {$varType = "FLOAT";}
            ;
expr_list: expr expr_list_tail | /* epsilon */;
expr_list_tail: COMMA expr expr_list_tail | /* epsilon */;
value_tail: OPENBRACK expr CLOSEBRACK | /* epsilon */;

//MISC
WHITESPACE: [ \t\n] -> skip;
COMMENT: '/*' (.|'\n'|'\r')*? '*/' -> skip;
INTLIT: '0'|[1-9][0-9]*;
FLOATLIT: (INTLIT|'0')'.'[0-9]*;
//WhiteSpace : [ \t]+ -> skip;
//fragment NEWLINE: '\r' '\n' | '\n' | '\r';
//Keywords
ARRAY: 'array';
BEGIN: 'begin';
BREAK: 'break';
DO: 'do';
ELSE: 'else';
END: 'end';
ENDDO: 'enddo';
ENDIF: 'endif';
FLOAT: 'float';
FOR: 'for';
FUNCTION: 'function';
IF: 'if';
INT: 'int';
LET: 'let';
OF: 'of';
PROGRAM: 'program';
RETURN: 'return';
STATIC: 'static';
THEN: 'then';
TO: 'to';
TYPE: 'type';
VAR: 'var';
WHILE: 'while';
//Punctuation
COMMA: ',';
DOT: '.';
COLON: ':';
SEMICOLON: ';';
OPENPAREN: '(';
CLOSEPAREN: ')';
OPENBRACK: '[';
CLOSEBRACK: ']';
OPENCURLY: '{';
CLOSECURLY: '}';
//Binary Operators'
PLUS: '+';
MINUS: '-';
MULT: '*';
DIV: '/';
POW: '**';
EQUAL: '==';
NEQUAL: '!=';
LESS: '<';
GREAT: '>';
LESSEQ: '<=';
GREATEQ: '>=';
AND: '&';
OR: '|';
//Assignment Operators
ASSIGN: ':=';
TASSIGN: '=';
//MISC.
ID: [a-zA-Z][1-9a-zA-Z_]*;
