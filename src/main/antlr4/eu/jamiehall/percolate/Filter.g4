/**
 *  Grammar for filtering collections using SQL-like where expressions
 *  Uses ANTLR v4's left-recursive expression notation.
 */
grammar Filter;

start :                     expression EOF;

expression :                attributeName operator=(LT | LT_EQ | GT | GT_EQ | EQ | NOT_EQ1 | NOT_EQ2) queryParameter        #comparatorExpression
                            | expression operator=(AND | OR) expression                                                     #binaryExpression
                            | attributeName BETWEEN queryParameter AND queryParameter                                       #betweenQuery
                            | attributeName NOT BETWEEN queryParameter AND queryParameter                                   #notBetweenQuery
                            | attributeName IN OPEN_PAR queryParameter (COMMA queryParameter)* CLOSE_PAR                    #inQuery
                            | attributeName NOT IN OPEN_PAR queryParameter (COMMA queryParameter)* CLOSE_PAR                #notInQuery
                            | attributeName LIKE stringQueryParameter                                                       #matchesRegexQuery
                            | attributeName IS NOT NULL                                                                     #hasQuery
                            | attributeName IS NULL                                                                         #notHasQuery
                            | OPEN_PAR expression CLOSE_PAR                                                                 #basicQuery
                            ;

attributeName :             IDENTIFIER | STRING_LITERAL ;
queryParameter :            NUMERIC_LITERAL |  STRING_LITERAL | BOOLEAN_LITERAL ;
stringQueryParameter :      STRING_LITERAL ;

SCOL :                      ';';
DOT :                       '.';
OPEN_PAR :                  '(';
CLOSE_PAR :                 ')';
COMMA :                     ',';
LT :                        '<';
LT_EQ :                     '<=';
GT :                        '>';
GT_EQ :                     '>=';
EQ :                        '=';
NOT_EQ1 :                   '!=';
NOT_EQ2 :                   '<>';
BETWEEN :                   B E T W E E N;
AND :                       A N D  | '&&';
OR :                        O R | '||';
NOT :                       N O T;
IN :                        I N;
LIKE :                      L I K E;
IS :                        I S;
NULL :                      N U L L;
BOOLEAN_LITERAL :           'true' | 'false';
IDENTIFIER :                [a-zA-Z_] [a-zA-Z_0-9.]*;
STRING_LITERAL :            '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';
NUMERIC_LITERAL :           DIGIT+ ( '.' DIGIT* )? ( E [-+]? DIGIT+ )?
                            | '.' DIGIT+ ( E [-+]? DIGIT+ )?;
SINGLE_LINE_COMMENT :       '--' ~[\r\n]* -> channel(HIDDEN);
MULTILINE_COMMENT :         '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN);
SPACES:                     [ \u000B\t\r\n] -> channel(HIDDEN);
UNEXPECTED_CHAR:            . ;

fragment DIGIT :            [0-9];
fragment A :                [aA];
fragment B :                [bB];
fragment C :                [cC];
fragment D :                [dD];
fragment E :                [eE];
fragment F :                [fF];
fragment G :                [gG];
fragment H :                [hH];
fragment I :                [iI];
fragment J :                [jJ];
fragment K :                [kK];
fragment L :                [lL];
fragment M :                [mM];
fragment N :                [nN];
fragment O :                [oO];
fragment P :                [pP];
fragment Q :                [qQ];
fragment R :                [rR];
fragment S :                [sS];
fragment T :                [tT];
fragment U :                [uU];
fragment V :                [vV];
fragment W :                [wW];
fragment X :                [xX];
fragment Y :                [yY];
fragment Z :                [zZ];
