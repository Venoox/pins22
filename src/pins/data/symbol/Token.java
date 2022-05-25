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

	EOF;

	@Override
	public String toString() {
		String s;
		switch (this) {
			case IDENTIFIER:
				s = "IDENTIFIER";
				break;
			// SYMBOLS
			case LPAREN:
				s = "(";
				break;// (
			case RPAREN:
				s = ")";
				break;// )
			case LBRACES:
				s = "{";
				break;// {
			case RBRACES:
				s = "}";
				break;// }
			case LBRACKET:
				s = "[";
				break;// [
			case RBRACKET:
				s = "]";
				break;// ]
			case COMMA:
				s = ",";
				break;
			case COLON:
				s = ":";
				break;// :
			case SEMIC:
				s = ";";
				break;// ;
			case AND:
				s = "&";
				break;// &
			case OR:
				s = "|";
				break;// |
			case NOT:
				s = "!";
				break;// !
			case NOT_EQUAL:
				s = "!=";
				break;// !=
			case EQUAL_EQUAL:
				s = "==";
				break;// ==
			case EQUAL:
				s = "=";
				break;// =
			case LESS:
				s = "<";
				break;// <
			case GREATER:
				s = ">";
				break;// >
			case LESS_EQUAL:
				s = "<=";
				break;// <=
			case GREATER_EQUAL:
				s = ">=";
				break;// >=
			case MUL:
				s = "*";
				break;// *
			case DIV:
				s = "/";
				break;// /
			case MOD:
				s = "%";
				break;// %
			case PLUS:
				s = "+";
				break;// +
			case MINUS:
				s = "-";
				break;// -
			case CARAT:
				s = "^";
				break;// ^

			// CONSTANTS
			case CONST_NONE:
				s = "none";
				break;// none
			case CONST_NIL:
				s = "nil";
				break;// nil
			case CONST_INT:
				s = "integer";
				break;
			case CONST_CHAR:
				s = "character";
				break;

			// KEYWORDS
			case CHAR:
			case WHILE:
			case WHERE:
			case VOID:
			case VAR:
			case TYP:
			case THEN:
			case NEW:
			case INT:
			case IF:
			case FUN:
			case END:
			case ELSE:
			case DO:
			case DEL:
				s = this.name();
				break;

			case EOF:
				s = "EOF (End of File)";
				break;
			default:
				s = "";
		}
		return s;
	}
}
