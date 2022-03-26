package pins.data.symbol;

public enum Token {
	IDENTIFIER,

	// SYMBOLS
	LPAREN, // (
	RPAREN, // )
	LBRACES, // {
	RBRACES, // }
	LBRACKET, // [
	RBRACKET, // ]
	COMMA, // ,
	COLON, // :
	SEMIC, // ;
	AND, // &
	OR, // |
	NOT, // !
	NOT_EQUAL, // !=
	EQUAL_EQUAL, // ==
	EQUAL, // =
	LESS, // <
	GREATER, // >
	LESS_EQUAL, // <=
	GREATER_EQUAL, // >=
	MUL, // *
	DIV, // /
	MOD, // %
	PLUS, // +
	MINUS, // -
	CARAT, // ^

	// CONSTANTS
	CONST_NONE, // none
	CONST_NIL, // nil
	CONST_INT,
	CONST_CHAR,

	// KEYWORDS
	CHAR,
	DEL,
	DO,
	ELSE,
	END,
	FUN,
	IF,
	INT,
	NEW,
	THEN,
	TYP,
	VAR,
	VOID,
	WHERE,
	WHILE,

	EOF,
}
