package pins.phase.synan;

import pins.common.report.Report;
import pins.data.symbol.Symbol;
import pins.data.symbol.Token;
import pins.phase.lexan.*;

public class SynAn implements AutoCloseable {
	private final LexAn lexan;
	private Symbol curr;

	public SynAn(LexAn lexan) {
		this.lexan = lexan;
		curr = this.lexan.lexer();
	}
	
	public void close() {
		lexan.close();
	}
	
	public void parser() {
		parsePRG();
	}

	private void remove() {
		curr = lexan.lexer();
	}

	private void parsePRG() {
		switch (curr.token) {
			case TYP:
			case FUN:
			case VAR:
				System.out.println("PRG -> DECL PRG'");
				parseDECL();
				parseOptionalPRG();
				break;
			default:
				error();
		}
	}

	private void parseOptionalPRG() {
		switch (curr.token) {
			case TYP:
			case FUN:
			case VAR:
				System.out.println("PRG' -> PRG");
				parsePRG();
				break;
			case RPAREN:
			case EOF:
				System.out.println("PRG' -> ");
				break;
			default:
				error();
		}
	}

	private void parseDECL() {
		switch (curr.token) {
			case TYP:
				System.out.println("DECL -> typ identifier = TYPE ;");
				remove();
				checkAndRemove(Token.IDENTIFIER);
				checkAndRemove(Token.EQUAL);
				parseTYPE();
				checkAndRemove(Token.SEMIC);
				break;
			case VAR:
				System.out.println("DECL -> var identifier : TYPE ;");
				remove();
				checkAndRemove(Token.IDENTIFIER);
				checkAndRemove(Token.COLON);
				parseTYPE();
				checkAndRemove(Token.SEMIC);
				break;
			case FUN:
				System.out.println("DECL -> fun identifier ( PARAMS ) : TYPE = EXPR ;");
				remove();
				checkAndRemove(Token.IDENTIFIER);
				checkAndRemove(Token.LPAREN);
				parsePARAMS();
				checkAndRemove(Token.RPAREN);
				checkAndRemove(Token.COLON);
				parseTYPE();
				checkAndRemove(Token.EQUAL);
				parseEXPR();
				checkAndRemove(Token.SEMIC);
				break;
			default:
				error();
		}
	}

	private void parsePARAMS() {
		switch (curr.token) {
			case IDENTIFIER:
				System.out.println("PARAMS -> identifier : TYPE PARAMS*");
				remove();
				checkAndRemove(Token.COLON);
				parseTYPE();
				parseOptionalPARAMS();
				break;
			case RPAREN:
				System.out.println("PARAMS -> ");
				break;
			default:
				error();
		}
	}

	private void parseOptionalPARAMS() {
		switch (curr.token) {
			case COMMA:
				System.out.println("PARAMS* -> , identifier : TYPE PARAMS*");
				remove();
				checkAndRemove(Token.IDENTIFIER);
				checkAndRemove(Token.COLON);
				parseTYPE();
				parseOptionalPARAMS();
				break;
			case RPAREN:
				System.out.println("PARAMS* -> ");
				break;
			default:
				error();
		}
	}

	private void parseTYPE() {
		switch (curr.token) {
			case IDENTIFIER:
				System.out.println("TYPE -> identifier");
				remove();
				break;
			case VOID:
				System.out.println("TYPE -> void");
				remove();
				break;
			case CHAR:
				System.out.println("TYPE -> char");
				remove();
				break;
			case INT:
				System.out.println("TYPE -> int");
				remove();
				break;
			case LPAREN:
				System.out.println("TYPE -> ( TYPE )");
				remove();
				parseTYPE();
				checkAndRemove(Token.RPAREN);
				break;
			case CARAT:
				System.out.println("TYPE -> ^ TYPE");
				remove();
				parseTYPE();
				break;
			case LBRACKET:
				System.out.println("TYPE -> [ EXPR ] TYPE");
				remove();
				parseEXPR();
				checkAndRemove(Token.RBRACKET);
				parseTYPE();
				break;
		}
	}

	private void parseEXPR() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("EXPR -> N EXPR'");
				parseN();
				parseOptionalEXPR();
				break;
			default:
				error();
		}
	}

	private void parseOptionalEXPR() {
		switch (curr.token) {
			case EQUAL:
			case SEMIC:
			case COLON:
			case RPAREN:
			case COMMA:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
				System.out.println("EXPR' -> ");
				break;
			case OR:
				System.out.println("EXPR' -> | N EXPR'");
				remove();
				parseN();
				parseOptionalEXPR();
				break;
			default:
				error();
		}
	}

	private void parseN() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("N -> T N'");
				parseT();
				parseOptionalN();
				break;
			default:
				error();
		}
	}

	private void parseOptionalN() {
		switch (curr.token) {
			case EQUAL:
			case SEMIC:
			case COLON:
			case RPAREN:
			case COMMA:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
			case OR:
				System.out.println("N' -> ");
				break;
			case AND:
				System.out.println("N' -> & T N'");
				remove();
				parseT();
				parseOptionalN();
				break;
			default:
				error();
		}
	}

	private void parseT() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("T -> F T'");
				parseF();
				parseOptionalT();
				break;
			default:
				error();
		}
	}

	private void parseOptionalT() {
		switch (curr.token) {
			case EQUAL:
			case SEMIC:
			case COLON:
			case RPAREN:
			case COMMA:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
			case OR:
			case AND:
				System.out.println("T' -> ");
				break;
			case EQUAL_EQUAL:
				System.out.println("T' -> == F T'");
				remove();
				parseF();
				parseOptionalT();
				break;
			case NOT_EQUAL:
				System.out.println("T' -> != F T'");
				remove();
				parseF();
				parseOptionalT();
				break;
			case LESS:
				System.out.println("T' -> < F T'");
				remove();
				parseF();
				parseOptionalT();
				break;
			case GREATER:
				System.out.println("T' -> > F T'");
				remove();
				parseF();
				parseOptionalT();
				break;
			case LESS_EQUAL:
				System.out.println("T' -> <= F T'");
				remove();
				parseF();
				parseOptionalT();
				break;
			case GREATER_EQUAL:
				System.out.println("T' -> >= F T'");
				remove();
				parseF();
				parseOptionalT();
				break;
			default:
				error();
		}
	}

	private void parseF() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("F -> G F'");
				parseG();
				parseOptionalF();
				break;
			default:
				error();
		}
	}

	private void parseOptionalF() {
		switch (curr.token) {
			case EQUAL:
			case SEMIC:
			case COLON:
			case RPAREN:
			case COMMA:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
			case OR:
			case AND:
			case EQUAL_EQUAL:
			case NOT_EQUAL:
			case LESS:
			case GREATER:
			case LESS_EQUAL:
			case GREATER_EQUAL:
				System.out.println("F' -> ");
				break;
			case PLUS:
				System.out.println("F' -> + G F'");
				remove();
				parseG();
				parseOptionalF();
				break;
			case MINUS:
				System.out.println("F' -> - G F'");
				remove();
				parseG();
				parseOptionalF();
				break;
			default:
				error();
		}
	}

	private void parseG() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("G -> M G'");
				parseM();
				parseOptionalG();
				break;
			default:
				error();
		}
	}

	private void parseOptionalG() {
		switch (curr.token) {
			case EQUAL:
			case SEMIC:
			case COLON:
			case RPAREN:
			case COMMA:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
			case OR:
			case AND:
			case EQUAL_EQUAL:
			case NOT_EQUAL:
			case LESS:
			case GREATER:
			case LESS_EQUAL:
			case GREATER_EQUAL:
			case PLUS:
			case MINUS:
				System.out.println("F' -> ");
				break;
			case MUL:
				System.out.println("G' -> * M G'");
				remove();
				parseM();
				parseOptionalG();
				break;
			case DIV:
				System.out.println("G' -> / M G'");
				remove();
				parseM();
				parseOptionalG();
				break;
			case MOD:
				System.out.println("G' -> % M G'");
				remove();
				parseM();
				parseOptionalG();
				break;
			default:
				error();
		}
	}

	private void parseM() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("M -> PRE' PO");
				parseOptionalPrefix();
				parsePostfix();
				break;
			default:
				error();
		}
	}

	private void parseOptionalPrefix() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("PRE' -> ");
				break;
			case NOT:
				System.out.println("PRE' -> ! PRE'");
				remove();
				parseOptionalPrefix();
				break;
			case PLUS:
				System.out.println("PRE' -> + PRE'");
				remove();
				parseOptionalPrefix();
				break;
			case MINUS:
				System.out.println("PRE' -> - PRE'");
				remove();
				parseOptionalPrefix();
				break;
			case CARAT:
				System.out.println("PRE' -> ^ PRE'");
				remove();
				parseOptionalPrefix();
				break;
			default:
				error();
		}
	}

	private void parsePostfix() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("PO -> E PO'");
				parseE();
				parseOptionalPostfix();
				break;
			default:
				error();
		}
	}

	private void parseOptionalPostfix() {
		switch (curr.token) {
			case EQUAL:
			case SEMIC:
			case COLON:
			case RPAREN:
			case COMMA:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
			case OR:
			case AND:
			case EQUAL_EQUAL:
			case NOT_EQUAL:
			case LESS:
			case GREATER:
			case LESS_EQUAL:
			case GREATER_EQUAL:
			case PLUS:
			case MINUS:
			case MUL:
			case DIV:
			case MOD:
				System.out.println("PO' -> ");
				break;
			case LBRACKET:
				System.out.println("PO' -> [ EXPR ] PO'");
				remove();
				parseEXPR();
				checkAndRemove(Token.RBRACKET);
				parseOptionalPostfix();
				break;
			case CARAT:
				System.out.println("PO' -> ^ PO'");
				remove();
				parseOptionalPostfix();
				break;
			default:
				error();
		}
	}

	private void parseE() {
		switch (curr.token) {
			case IDENTIFIER:
				System.out.println("E -> identifier CALL");
				remove();
				parseCALL();
				break;
			case LPAREN:
				System.out.println("E -> ( EXPR E' )");
				remove();
				parseEXPR();
				parseOptionalE();
				checkAndRemove(Token.RPAREN);
				break;
			case CONST_CHAR:
				System.out.println("E -> const_char");
				remove();
				break;
			case CONST_INT:
				System.out.println("E -> const_int");
				remove();
				break;
			case CONST_NIL:
				System.out.println("E -> const_nil");
				remove();
				break;
			case CONST_NONE:
				System.out.println("E -> const_none");
				remove();
				break;
			case NEW:
				System.out.println("E -> new E");
				remove();
				parseE();
				break;
			case DEL:
				System.out.println("E -> del E");
				remove();
				parseE();
				break;
			case LBRACES:
				System.out.println("E -> { STMT STMT* }");
				remove();
				parseSTMT();
				parseOptionalSTMT();
				checkAndRemove(Token.RBRACES);
				break;
			default:
				error();
		}
	}

	private void parseCALL() {
		switch (curr.token) {
			case EQUAL:
			case SEMIC:
			case COLON:
			case RPAREN:
			case COMMA:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
			case OR:
			case AND:
			case EQUAL_EQUAL:
			case NOT_EQUAL:
			case LESS:
			case GREATER:
			case LESS_EQUAL:
			case GREATER_EQUAL:
			case PLUS:
			case MINUS:
			case MUL:
			case DIV:
			case MOD:
			case LBRACKET:
			case CARAT:
				System.out.println("CALL -> ");
				break;
			case LPAREN:
				System.out.println("CALL -> ( ARG )");
				remove();
				parseARG();
				checkAndRemove(Token.RPAREN);
				break;
			default:
				error();
		}
	}

	private void parseARG() {
		switch (curr.token) {
			case RPAREN:
				System.out.println("ARG -> .");
				break;
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("ARG -> EXPR ARG'");
				parseEXPR();
				parseOptionalARG();
				break;
			default:
				error();
		}
	}

	private void parseOptionalARG() {
		switch (curr.token) {
			case RPAREN:
				System.out.println("ARG' -> ");
				break;
			case COMMA:
				System.out.println("ARG' -> , EXPR ARG'");
				remove();
				parseEXPR();
				parseOptionalARG();
				break;
			default:
				error();
		}
	}

	private void parseOptionalE() {
		switch (curr.token) {
			case COLON:
				System.out.println("E' -> : TYPE");
				remove();
				parseTYPE();
				break;
			case RPAREN:
				System.out.println("E' -> ");
				break;
			case WHERE:
				System.out.println("E' -> where PRG");
				remove();
				parsePRG();
				break;
			default:
				error();
		}
	}

	private void parseSTMT() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
				System.out.println("STMT -> EXPR STMT' ;");
				parseEXPR();
				parseOptionalAssigment();
				checkAndRemove(Token.SEMIC);
				break;
			case IF:
				System.out.println("STMT -> if EXPR then STMT STMT* ELSE end ;");
				remove();
				parseEXPR();
				checkAndRemove(Token.THEN);
				parseSTMT();
				parseOptionalSTMT();
				parseELSE();
				checkAndRemove(Token.END);
				checkAndRemove(Token.SEMIC);
				break;
			case WHILE:
				System.out.println("STMT -> while EXPR do STMT STMT* end ;");
				remove();
				parseEXPR();
				checkAndRemove(Token.DO);
				parseSTMT();
				parseOptionalSTMT();
				checkAndRemove(Token.END);
				checkAndRemove(Token.SEMIC);
				break;
			default:
				error();
		}
	}

	private void parseOptionalAssigment() {
		switch (curr.token) {
			case EQUAL:
				System.out.println("STMT' -> = EXPR");
				remove();
				parseEXPR();
				break;
			case SEMIC:
				System.out.println("STMT' -> ");
				break;
			default:
				error();
		}
	}

	private void parseOptionalSTMT() {
		switch (curr.token) {
			case IDENTIFIER:
			case LPAREN:
			case CARAT:
			case PLUS:
			case MINUS:
			case NOT:
			case CONST_CHAR:
			case CONST_INT:
			case CONST_NIL:
			case CONST_NONE:
			case NEW:
			case DEL:
			case LBRACES:
			case IF:
			case WHILE:
				System.out.println("STMT* -> STMT STMT*");
				parseSTMT();
				parseOptionalSTMT();
				break;
			case RBRACES:
			case END:
			case ELSE:
				System.out.println("STMT* -> ");
				break;
			default:
				error();
		}
	}

	private void parseELSE() {
		switch (curr.token) {
			case END:
				System.out.println("ELSE -> ");
				break;
			case ELSE:
				System.out.println("ELSE -> else STMT STMT*");
				remove();
				parseSTMT();
				parseOptionalSTMT();
				break;
			default:
				error();
		}
	}

	private void checkAndRemove(Token token) {
		if (curr.token == token)
			remove();
		else
			error();
	}

	private void error() {
		throw new Report.Error(curr, "Unexpected token " + curr);
	}
}
