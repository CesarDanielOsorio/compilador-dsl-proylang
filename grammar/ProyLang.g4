/*
 * ============================================================
 *  ProyLang  -  Lenguaje DSL para Gestion de Proyectos
 *  Compilador en ANTLR4 + Java
 *  Autor: Cesar Daniel Osorio Polanco  (Carne 0900-16-6607)
 * ============================================================
 *
 *  Esta gramatica define un lenguaje para describir proyectos:
 *  sus tareas, duraciones, costos, responsables y dependencias.
 *
 *  Un archivo .proy contiene uno o varios proyectos. Cada
 *  proyecto agrupa tareas. Cada tarea tiene atributos.
 */
grammar ProyLang;

/* ============================================================
 *  REGLAS DEL PARSER  (analisis sintactico - la "gramatica")
 *  Empiezan con minuscula.
 * ============================================================ */

// Un programa es uno o mas proyectos, seguido del fin de archivo (EOF).
programa
    : proyecto+ EOF
    ;

// proyecto "Nombre" { ...tareas... }
proyecto
    : PROYECTO STRING LLAVE_IZQ declTarea* LLAVE_DER
    ;

// tarea identificador { ...atributos... }
declTarea
    : TAREA ID LLAVE_IZQ atributo* LLAVE_DER
    ;

// Cada atributo termina en ';'. Usamos etiquetas (#nombre) para
// que el visitor de Java distinga facilmente cada caso.
atributo
    : KW_NOMBRE      ASIGNA STRING   PYC   # atrNombre
    | KW_DURACION    ASIGNA expr     PYC   # atrDuracion
    | KW_COSTO       ASIGNA expr     PYC   # atrCosto
    | KW_RESPONSABLE ASIGNA STRING   PYC   # atrResponsable
    | KW_PRIORIDAD   ASIGNA ID       PYC   # atrPrioridad
    | KW_DEPENDE     ASIGNA listaIds PYC   # atrDepende
    ;

// Lista de identificadores de tareas separados por coma: A, B, C
listaIds
    : ID (COMA ID)*
    ;

// Expresiones aritmeticas (para 'duracion' y 'costo').
// El orden de las reglas define la PRECEDENCIA:
//   primero * y /, luego + y -.
expr
    : MENOS expr                # negativo    // menos unario: -100
    | expr (MUL | DIV) expr     # mulDiv
    | expr (MAS | MENOS) expr   # sumaResta
    | PAR_IZQ expr PAR_DER      # parentesis
    | NUMERO                    # numero
    ;

/* ============================================================
 *  REGLAS DEL LEXER  (analisis lexico - los "tokens")
 *  Empiezan con MAYUSCULA.
 * ============================================================ */

// --- Palabras reservadas ---
PROYECTO       : 'proyecto' ;
TAREA          : 'tarea' ;
KW_NOMBRE      : 'nombre' ;
KW_DURACION    : 'duracion' ;
KW_COSTO       : 'costo' ;
KW_RESPONSABLE : 'responsable' ;
KW_PRIORIDAD   : 'prioridad' ;
KW_DEPENDE     : 'depende' ;

// --- Simbolos ---
ASIGNA    : '=' ;
PYC       : ';' ;
COMA      : ',' ;
LLAVE_IZQ : '{' ;
LLAVE_DER : '}' ;
PAR_IZQ   : '(' ;
PAR_DER   : ')' ;
MAS       : '+' ;
MENOS     : '-' ;
MUL       : '*' ;
DIV       : '/' ;

// --- Literales ---
NUMERO : [0-9]+ ('.' [0-9]+)? ;          // 5, 10, 3.5
STRING : '"' (~["\r\n])* '"' ;           // "texto entre comillas"
ID     : [a-zA-Z_] [a-zA-Z_0-9]* ;       // diseno, tareaA, _temp

// --- Cosas que se ignoran ---
COMENTARIO_LINEA  : '//' ~[\r\n]*      -> skip ;
COMENTARIO_BLOQUE : '/*' .*? '*/'      -> skip ;
WS                : [ \t\r\n]+         -> skip ;

// Cualquier caracter no reconocido cae aqui y produce ERROR LEXICO.
CARACTER_INVALIDO : . ;
