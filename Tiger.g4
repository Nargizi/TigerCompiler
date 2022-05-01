grammar Tiger;

//Grammar
tiger_program: PROGRAM ID LET declaration_segment BEGIN funct_list END;
declaration_segment: type_declaration_list var_declaration_list;
type_declaration_list: type_declaration type_declaration_list | /* epsilon */;
var_declaration_list: var_declaration var_declaration_list | /* epsilon */;
funct_list: funct funct_list | /* epsilon */;
type_declaration returns [String id, Type varType]
            : TYPE ID TASSIGN type SEMICOLON {$id = $ID.text;
                                              $varType = $type.varType;}
            ;
type returns [Type varType]
            : base_type {$varType = new Type($base_type.varTypeString);}
            | ARRAY OPENBRACK INTLIT CLOSEBRACK OF base_type {$varType = new Type($base_type.varTypeString, $INTLIT.int);}
            | ID {$varType = new Type($ID.text);}
            ;
base_type returns [String varTypeString]
            : INT {$varTypeString = $INT.text;}
            | FLOAT {$varTypeString = $FLOAT.text;}
            ;
var_declaration returns [String storageClass, Type varType, List<String> idList]
            : storage_class id_list COLON type optional_init SEMICOLON {$storageClass = $storage_class.storageClass;
                                                                        $varType = $type.varType;
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
funct returns [String id, Type retType, List<Type> params, boolean hasReturn, boolean outsideBreak, List<Integer> breakLines, boolean semError = false]
            : FUNCTION ID OPENPAREN param_list CLOSEPAREN ret_type BEGIN stat_seq END {$id = $ID.text;
                                                                                       $retType = $ret_type.varType;
                                                                                       $params = $param_list.params;
                                                                                       $hasReturn = $stat_seq.hasReturn;
                                                                                       $outsideBreak = $stat_seq.outsideBreak;
                                                                                       $breakLines = $stat_seq.breakLines;}
            ;
param_list returns [List<Type> params = new ArrayList<>()]
            : param param_list_tail {$params.add($param.varType);
                                     $params.addAll($param_list_tail.params);}
            | /* epsilon */
            ;
param_list_tail returns [List<Type> params = new ArrayList<>()]
            : COMMA param param_list_tail{$params.add($param.varType);
                                          $params.addAll($param_list_tail.params);}
            | /* epsilon */
            ;
ret_type returns [Type varType]
            : COLON type {$varType = $type.varType;}
            | /* epsilon */ {$varType = Type.ERROR;}
            ;
param returns [Type varType, String id]
            : ID COLON type {$varType = $type.varType;
                             $id = $ID.text;}
            ;
stat_seq returns [boolean hasReturn, boolean outsideBreak, List<Integer> breakLines]
            : stat {$hasReturn = $stat.hasReturn;
                    $outsideBreak = $stat.outsideBreak;
                    $breakLines= $stat.breakLines;}
            | stat stat_seq {$hasReturn = $stat.hasReturn || $stat_seq.hasReturn;
                            $outsideBreak = $stat.outsideBreak || $stat_seq.outsideBreak;
                            $breakLines = $stat.breakLines;
                            $breakLines.addAll($stat_seq.breakLines);}
            ;
stat returns [boolean hasReturn, boolean outsideBreak = false, List<Integer> breakLines = new ArrayList<>();]:
      value_stat |
      if_stat  {$hasReturn = $if_stat.hasReturn;
                $outsideBreak = $if_stat.outsideBreak;
                $breakLines = $if_stat.breakLines;}|
      if_else_stat {$hasReturn = $if_else_stat.hasReturn;
                    $outsideBreak = $if_else_stat.outsideBreak;
                    $breakLines = $if_else_stat.breakLines;}|
      while_stat {$hasReturn = $while_stat.hasReturn;} |
      for_stat {$hasReturn = $for_stat.hasReturn;} |
      func_call_stat |
      BREAK SEMICOLON {$outsideBreak = true;
                       $breakLines = List.of($BREAK.getLine());} |
      ret_stat {$hasReturn = true;} |
      let_stat {$hasReturn = $let_stat.hasReturn;
                $outsideBreak = $let_stat.outsideBreak;
                $breakLines = $let_stat.breakLines;};

value_stat: value ASSIGN expr SEMICOLON;
if_stat returns [boolean hasReturn, boolean outsideBreak, List<Integer> breakLines]
                : IF expr THEN stat_seq ENDIF SEMICOLON {$hasReturn = $stat_seq.hasReturn;
                                                         $outsideBreak = $stat_seq.outsideBreak;
                                                         $breakLines = $stat_seq.breakLines;}
                ;
if_else_stat returns [boolean hasReturn, boolean outsideBreak, List<Integer> breakLines]
                : IF expr THEN a=stat_seq ELSE b=stat_seq ENDIF SEMICOLON {$hasReturn = $a.hasReturn || $b.hasReturn;
                                                                           $outsideBreak = $a.outsideBreak || $b.outsideBreak;
                                                                           $breakLines = $a.breakLines;
                                                                           $breakLines.addAll($b.breakLines);}
                ;
while_stat returns [boolean hasReturn]: WHILE expr DO stat_seq ENDDO SEMICOLON {$hasReturn = $stat_seq.hasReturn;};
for_stat returns [boolean hasReturn]: FOR ID ASSIGN expr TO expr DO stat_seq ENDDO SEMICOLON {$hasReturn = $stat_seq.hasReturn;};
func_call_stat: optprefix ID OPENPAREN expr_list CLOSEPAREN SEMICOLON;
ret_stat
            : RETURN optreturn SEMICOLON
            ;
let_stat returns [boolean hasReturn, boolean outsideBreak, List<Integer> breakLines]
            : LET declaration_segment BEGIN stat_seq END {$hasReturn = $stat_seq.hasReturn;
                                                          $outsideBreak = $stat_seq.outsideBreak;
                                                          $breakLines = $stat_seq.breakLines;}
            ;
optreturn returns [Type varType]
            : expr
            | /* epsilon */
            ;
optprefix returns [Type varType, String id]
            : value ASSIGN {$id = $value.id;}
            | /* epsilon */
            ;
expr returns [Type varType]
            : precedence_or
            ;
precedence_or returns [Type varType]
            : precedence_or OR precedence_and
            | precedence_and
            ;
precedence_and returns [Type varType]
            : precedence_and AND precedence_compare
            | precedence_compare
            ;
precedence_compare returns [Type varType]
            : precedence_plus_minus ((EQUAL | NEQUAL | LESS |
                    GREAT | GREATEQ | LESSEQ) precedence_plus_minus)*
            ;
precedence_plus_minus returns [Type varType]
            : precedence_plus_minus (PLUS | MINUS) precedence_mult_div
            | precedence_mult_div
            ;
precedence_mult_div returns [Type varType]
            : precedence_mult_div (MULT | DIV) precedence_pow
            | precedence_pow
            ;
precedence_pow returns [Type varType]
            : precedence_paren POW precedence_pow
            | precedence_paren
            ;
precedence_paren returns [Type varType]
            : OPENPAREN expr CLOSEPAREN
            | precedence_trail
            ;
precedence_trail returns [Type varType]
            : const_
            | value
            ;
value returns [Type varType, String id, boolean isSubscript]
            : ID value_tail {$id = $ID.text;
                             $isSubscript = $value_tail.isSubscript;}
            ;
const_ returns [Type varType]
            : INTLIT {$varType = Type.INT;}
            | FLOATLIT {$varType = Type.FLOAT;}
            ;
expr_list returns [List<Type> params = new ArrayList<>()]
            : expr expr_list_tail
            | /* epsilon */
            ;
expr_list_tail returns [List<Type> params = new ArrayList<>()]
            : COMMA expr expr_list_tail
            | /* epsilon */
            ;
value_tail returns [boolean isSubscript = false]
            : OPENBRACK expr CLOSEBRACK {$isSubscript = true;}
            | /* epsilon */
            ;

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
