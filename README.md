## Program
PRG -> DECL PRG' .
PRG' -> PRG .
PRG' -> .

## Type declaration
DECL -> typ identifier = TYPE ; .
## Variable declaration
DECL -> var identifier : TYPE ; .
## Function declaration
DECL -> fun identifier ( PARAMS ) : TYPE = EXPR ; .
PARAMS -> identifier : TYPE PARAMS* .
PARAMS -> .
PARAMS* -> , identifier : TYPE PARAMS* .
PARAMS* -> .

## Atomic types
TYPE -> void .
TYPE -> char .
TYPE -> int .
## Named type
TYPE -> identifier .
## Array type
TYPE -> [ EXPR ] TYPE .
## Pointer type
TYPE -> ^ TYPE .
## Enclosed type
TYPE -> ( TYPE ) .

EXPR -> N EXPR' .
EXPR' -> .
EXPR' -> I N EXPR' .

N -> T N' .
N' -> .
N' -> & T N' .

T -> F T' .
T' -> .
T' -> == F T' .
T' -> != F T' .
T' -> < F T' .
T' -> > F T' .
T' -> <= F T' .
T' -> >= F T' .

F -> G F' .
F' -> .
F' -> + G F' .
F' -> - G F' .

G -> M G' .
G' -> .
G' -> * M G' .
G' -> / M G' .
G' -> % M G' .

M -> PRE' PO .
PRE' -> ! PRE' .
PRE' -> + PRE' .
PRE' -> - PRE' .
PRE' -> ^ PRE' .
PRE' -> .

PO -> E PO' .
PO' -> [ L ] PO' .
PO' ->  ^ PO' .
PO' -> .

E -> const_char .
E -> const_int .
E -> const_nil .
E -> const_none .
E -> identifier CALL .
CALL -> ( ARG ) .
CALL -> .

ARG -> L ARG' .
ARG -> .
ARG' -> , L ARG' .
ARG' -> .

E -> new E .
E -> del E .
E -> { STMT STMT* } .
E -> ( L E' ) .
E' -> : TYPE .
E' -> where PRG .
E' -> .

STMT -> EXPR STMT' ; .
STMT' -> = EXPR .
STMT' -> .

# conditional
STMT -> if EXPR then STMT STMT* ELSE end ; .
ELSE -> else STMT STMT* .
ELSE -> .

# loop
STMT -> while EXPR do STMT STMT* end ; .

STMT* -> STMT STMT* .
STMT* -> .