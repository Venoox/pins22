package pins.phase.synan;

import pins.common.report.Location;
import pins.common.report.Report;
import pins.data.symbol.Symbol;
import pins.data.symbol.Token;
import pins.phase.lexan.*;
import pins.data.ast.*;

import java.util.Vector;

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
	
	public AST parser() {
		Vector<AstDecl> decls = parsePRG();
		assert decls != null;
		return new ASTs<AstDecl>(new Location(decls.firstElement().location, decls.lastElement().location), decls);
	}

	private void remove() {
		curr = lexan.lexer();
	}

	private Vector<AstDecl> parsePRG() {
		switch (curr.token) {
			case TYP:
			case FUN:
			case VAR:
				// PRG -> DECL PRG'
				AstDecl decl = parseDECL();
				Vector<AstDecl> decls = parseOptionalPRG();
				assert decls != null;
				decls.insertElementAt(decl, 0);
				return decls;
			default:
				error();
		}
		return null;
	}

	private Vector<AstDecl> parseOptionalPRG() {
		switch (curr.token) {
			case TYP:
			case FUN:
			case VAR:
				// PRG' -> PRG
				return parsePRG();
			case RPAREN:
			case EOF:
				// PRG' ->
				return new Vector<>();
			default:
				error();
		}
		return null;
	}

	private AstDecl parseDECL() {
		Location startLoc = curr.location;
		Location endLoc;
		switch (curr.token) {
			case TYP:
				// DECL -> typ identifier = TYPE ;
				remove();
				String name = curr.lexeme;
				checkAndRemove(Token.IDENTIFIER);
				checkAndRemove(Token.EQUAL);
				AstType type = parseTYPE();
				endLoc = curr.location;
				checkAndRemove(Token.SEMIC);
				return new AstTypDecl(new Location(startLoc, endLoc), name, type);
			case VAR:
				// DECL -> var identifier : TYPE ;
				remove();
				name = curr.lexeme;
				checkAndRemove(Token.IDENTIFIER);
				checkAndRemove(Token.COLON);
				type = parseTYPE();
				endLoc = curr.location;
				checkAndRemove(Token.SEMIC);
				return new AstVarDecl(new Location(startLoc, endLoc), name, type);
			case FUN:
				// DECL -> fun identifier ( PARAMS ) : TYPE = EXPR ;
				remove();
				name = curr.lexeme;
				checkAndRemove(Token.IDENTIFIER);
				Location startParamsLoc = curr.location;
				checkAndRemove(Token.LPAREN);
				Vector<AstParDecl> params = parsePARAMS();
				Location endParamsLoc = curr.location;
				checkAndRemove(Token.RPAREN);
				checkAndRemove(Token.COLON);
				type = parseTYPE();
				checkAndRemove(Token.EQUAL);
				AstExpr expr = parseEXPR();
				endLoc = curr.location;
				checkAndRemove(Token.SEMIC);
				assert params != null;
				return new AstFunDecl(new Location(startLoc, endLoc), name, new ASTs<>(new Location(startParamsLoc, endParamsLoc), params), type, expr);
			default:
				error();
		}
		return null;
	}

	private Vector<AstParDecl> parsePARAMS() {
		switch (curr.token) {
			case IDENTIFIER:
				// PARAMS -> identifier : TYPE PARAMS*
				Location startLoc = curr.location;
				String name = curr.lexeme;
				remove();
				checkAndRemove(Token.COLON);
				AstType type = parseTYPE();
				Vector<AstParDecl> params = parseOptionalPARAMS();
				assert params != null;
				assert type != null;
				params.insertElementAt(new AstParDecl(new Location(startLoc, type.location), name, type), 0);
				return params;
			case RPAREN:
				// PARAMS ->
				return new Vector<>();
			default:
				error();
		}
		return null;
	}

	private Vector<AstParDecl> parseOptionalPARAMS() {
		switch (curr.token) {
			case COMMA:
				// PARAMS* -> , identifier : TYPE PARAMS*
				remove();
				Location startLoc = curr.location;
				String name = curr.lexeme;
				checkAndRemove(Token.IDENTIFIER);
				checkAndRemove(Token.COLON);
				AstType type = parseTYPE();
				Vector<AstParDecl> params = parseOptionalPARAMS();
				assert params != null;
				assert type != null;
				params.insertElementAt(new AstParDecl(new Location(startLoc, type.location), name, type), 0);
				return params;
			case RPAREN:
				// PARAMS* ->
				return new Vector<>();
			default:
				error();
		}
		return null;
	}

	private AstType parseTYPE() {
		Location startLoc = curr.location;
		switch (curr.token) {
			case IDENTIFIER:
				// TYPE -> identifier
				String name = curr.lexeme;
				remove();
				return new AstTypeName(startLoc, name);
			case VOID:
				// TYPE -> void
				remove();
				return new AstAtomType(startLoc, AstAtomType.Kind.VOID);
			case CHAR:
				// TYPE -> char
				remove();
				return new AstAtomType(startLoc, AstAtomType.Kind.CHAR);
			case INT:
				// TYPE -> int
				remove();
				return new AstAtomType(startLoc, AstAtomType.Kind.INT);
			case LPAREN:
				// TYPE -> ( TYPE )
				remove();
				AstType type = parseTYPE();
				checkAndRemove(Token.RPAREN);
				return type;
			case CARAT:
				// TYPE -> ^ TYPE
				remove();
				type = parseTYPE();
				assert type != null;
				return new AstPtrType(new Location(startLoc, type.location), type);
			case LBRACKET:
				// TYPE -> [ EXPR ] TYPE
				remove();
				AstExpr expr = parseEXPR();
				checkAndRemove(Token.RBRACKET);
				type = parseTYPE();
				assert type != null;
				return new AstArrType(new Location(startLoc, type.location), type, expr);
		}
		return null;
	}

	private AstExpr parseEXPR() {
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
				// EXPR -> N EXPR'
				AstExpr expr1 = parseN();
				return parseOptionalEXPR(expr1);
			default:
				error();
		}
		return null;
	}

	private AstExpr parseOptionalEXPR(AstExpr expr1) {
		AstBinExpr.Oper binOp;
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
				// EXPR' ->
				return expr1;
			case OR:
				// EXPR' -> | N EXPR'
				binOp = AstBinExpr.Oper.OR;
				break;
			default:
				error();
				return null;
		}
		remove();
		AstExpr expr2 = parseN();
		assert expr2 != null;
		return parseOptionalEXPR(new AstBinExpr(new Location(expr1.location, expr2.location), binOp, expr1, expr2));
	}

	private AstExpr parseN() {
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
				// N -> T N'
				AstExpr expr1 = parseT();
				return parseOptionalN(expr1);
			default:
				error();
		}
		return null;
	}

	private AstExpr parseOptionalN(AstExpr expr1) {
		AstBinExpr.Oper binOp;
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
				// N' ->
				return expr1;
			case AND:
				// N' -> & T N'
				binOp = AstBinExpr.Oper.AND;
				break;
			default:
				error();
				return null;
		}
		remove();
		AstExpr expr2 = parseT();
		assert expr2 != null;
		return parseOptionalN(new AstBinExpr(new Location(expr1.location, expr2.location), binOp, expr1, expr2));
	}

	private AstExpr parseT() {
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
				// T -> F T'
				AstExpr expr1 = parseF();
				return parseOptionalT(expr1);
			default:
				error();
		}
		return null;
	}

	private AstExpr parseOptionalT(AstExpr expr1) {
		AstBinExpr.Oper binOp;
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
				// T' ->
				return expr1;
			case EQUAL_EQUAL:
				// T' -> == F T'
				binOp = AstBinExpr.Oper.EQU;
				break;
			case NOT_EQUAL:
				// T' -> != F T'
				binOp = AstBinExpr.Oper.NEQ;
				break;
			case LESS:
				// T' -> < F T'
				binOp = AstBinExpr.Oper.LTH;
				break;
			case GREATER:
				// T' -> > F T'
				binOp = AstBinExpr.Oper.GTH;
				break;
			case LESS_EQUAL:
				// T' -> <= F T'
				binOp = AstBinExpr.Oper.LEQ;
				break;
			case GREATER_EQUAL:
				// T' -> >= F T'
				binOp = AstBinExpr.Oper.GEQ;
				break;
			default:
				error();
				return null;
		}
		remove();
		AstExpr expr2 = parseF();
		assert expr2 != null;
		return parseOptionalT(new AstBinExpr(new Location(expr1.location, expr2.location), binOp, expr1, expr2));
	}

	private AstExpr parseF() {
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
				// F -> G F'
				AstExpr expr1 = parseG();
				return parseOptionalF(expr1);
			default:
				error();
		}
		return null;
	}

	private AstExpr parseOptionalF(AstExpr expr1) {
		AstBinExpr.Oper binOp;
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
				// F' ->
				return expr1;
			case PLUS:
				// F' -> + G F'
				binOp = AstBinExpr.Oper.ADD;
				break;
			case MINUS:
				// F' -> - G F'
				binOp = AstBinExpr.Oper.SUB;
				break;
			default:
				error();
				return null;
		}
		remove();
		AstExpr expr2 = parseG();
		assert expr2 != null;
		return parseOptionalF(new AstBinExpr(new Location(expr1.location, expr2.location), binOp, expr1, expr2));
	}

	private AstExpr parseG() {
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
				// G -> M G'
				AstExpr expr1 = parseM();
				return parseOptionalG(expr1);
			default:
				error();
		}
		return null;
	}

	private AstExpr parseOptionalG(AstExpr expr1) {
		AstBinExpr.Oper binOp;
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
				// F' ->
				return expr1;
			case MUL:
				// G' -> * M G'
				binOp = AstBinExpr.Oper.MUL;
				break;
			case DIV:
				// G' -> / M G'
				binOp = AstBinExpr.Oper.DIV;
				break;
			case MOD:
				// G' -> % M G'
				binOp = AstBinExpr.Oper.MOD;
				break;
			default:
				error();
				return null;
		}
		remove();
		AstExpr expr2 = parseM();
		assert expr2 != null;
		return parseOptionalG(new AstBinExpr(new Location(expr1.location, expr2.location), binOp, expr1, expr2));
	}

	private AstExpr parseM() {
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
				// M -> PRE' PO
				Vector<AstPreExpr.Oper> preOpers = parseOptionalPrefix();
				assert preOpers != null;
				AstExpr expr = parsePostfix();
				assert expr != null;
				for (int i = preOpers.size()-1; i >= 0; i--) {
					expr = new AstPreExpr(expr.location, preOpers.get(i), expr); // TODO: fix location
				}
				return expr;
			default:
				error();
		}
		return null;
	}

	private Vector<AstPreExpr.Oper> parseOptionalPrefix() {
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
				// PRE' ->
				return new Vector<>();
			case NOT:
				// PRE' -> ! PRE'
				remove();
				Vector<AstPreExpr.Oper> preOpers = parseOptionalPrefix();
				assert preOpers != null;
				preOpers.insertElementAt(AstPreExpr.Oper.NOT, 0);
				return preOpers;
			case PLUS:
				// PRE' -> + PRE'
				remove();
				preOpers = parseOptionalPrefix();
				assert preOpers != null;
				preOpers.insertElementAt(AstPreExpr.Oper.ADD, 0);
				return preOpers;
			case MINUS:
				// PRE' -> - PRE'
				remove();
				preOpers = parseOptionalPrefix();
				assert preOpers != null;
				preOpers.insertElementAt(AstPreExpr.Oper.SUB, 0);
				return preOpers;
			case CARAT:
				// PRE' -> ^ PRE'
				remove();
				preOpers = parseOptionalPrefix();
				assert preOpers != null;
				preOpers.insertElementAt(AstPreExpr.Oper.PTR, 0);
				return preOpers;
			default:
				error();
		}
		return null;
	}

	private AstExpr parsePostfix() {
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
				// PO -> E PO'
				AstExpr expr = parseE();
				return parseOptionalPostfix(expr);
			default:
				error();
		}
		return null;
	}

	private AstExpr parseOptionalPostfix(AstExpr expr) {
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
				// PO' ->
				return expr;
			case LBRACKET:
				// PO' -> [ EXPR ] PO'
				remove();
				AstExpr arrExpr = parseEXPR();
				assert arrExpr != null;
				Location endLoc = curr.location;
				checkAndRemove(Token.RBRACKET);
				expr = new AstBinExpr(new Location(expr.location, endLoc), AstBinExpr.Oper.ARR, expr, arrExpr);
				return parseOptionalPostfix(expr);
			case CARAT:
				// PO' -> ^ PO'
				endLoc = curr.location;
				remove();
				expr = new AstPstExpr(new Location(expr.location, endLoc), AstPstExpr.Oper.PTR, expr);
				return parseOptionalPostfix(expr);
			default:
				error();
		}
		return null;
	}

	private AstExpr parseE() {
		Token token = curr.token;
		Location startLoc = curr.location;
		String name = curr.lexeme;
		switch (curr.token) {
			case IDENTIFIER:
				// E -> identifier CALL
				remove();
				return parseOptionalCall(new Symbol(token, name, startLoc));
			case LPAREN:
				// E -> ( EXPR E' )
				remove();
				AstExpr expr = parseEXPR();
				expr = parseOptionalE(expr);
				checkAndRemove(Token.RPAREN);
				return expr;
			case CONST_CHAR:
				// E -> const_char
				remove();
				return new AstConstExpr(startLoc, AstConstExpr.Kind.CHAR, name);
			case CONST_INT:
				// E -> const_int
				remove();
				return new AstConstExpr(startLoc, AstConstExpr.Kind.INT, name);
			case CONST_NIL:
				// E -> const_nil
				remove();
				return new AstConstExpr(startLoc, AstConstExpr.Kind.PTR, name);
			case CONST_NONE:
				// E -> const_none
				remove();
				return new AstConstExpr(startLoc, AstConstExpr.Kind.VOID, name);
			case NEW:
				// E -> new E
				remove();
				expr = parseE();
				assert expr != null;
				return new AstPreExpr(new Location(startLoc, expr.location), AstPreExpr.Oper.NEW, expr);
			case DEL:
				// E -> del E
				remove();
				expr = parseE();
				assert expr != null;
				return new AstPreExpr(new Location(startLoc, expr.location), AstPreExpr.Oper.DEL, expr);
			case LBRACES:
				// E -> { STMT STMT* }
				remove();
				AstStmt stmt = parseSTMT();
				Vector<AstStmt> stmts = parseOptionalSTMT();
				assert stmts != null;
				stmts.insertElementAt(stmt, 0);
				Location endLoc = curr.location;
				checkAndRemove(Token.RBRACES);
				return new AstStmtExpr(new Location(startLoc, endLoc), new ASTs<>(stmts.size() > 0 ? new Location(stmts.firstElement().location, stmts.lastElement().location) : new Location(startLoc, endLoc), stmts));
			default:
				error();
		}
		return null;
	}

	private AstExpr parseOptionalCall(Symbol symbol) {
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
				// CALL ->
				return new AstNameExpr(symbol.location, symbol.lexeme);
			case LPAREN:
				// CALL -> ( ARG )
				Location startLoc = curr.location;
				remove();
				Vector<AstExpr> args = parseARG();
				Location endLoc = curr.location;
				checkAndRemove(Token.RPAREN);
				assert args != null;
				return new AstCallExpr(new Location(symbol.location, endLoc), symbol.lexeme, new ASTs<>(new Location(startLoc, endLoc), args));
			default:
				error();
		}
		return null;
	}

	private Vector<AstExpr> parseARG() {
		switch (curr.token) {
			case RPAREN:
				// ARG -> .
				return new Vector<>();
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
				// ARG -> EXPR ARG'
				AstExpr expr = parseEXPR();
				Vector<AstExpr> args = parseOptionalARG();
				assert args != null;
				args.insertElementAt(expr, 0);
				return args;
			default:
				error();
		}
		return null;
	}

	private Vector<AstExpr> parseOptionalARG() {
		switch (curr.token) {
			case RPAREN:
				// ARG' ->
				return new Vector<>();
			case COMMA:
				// ARG' -> , EXPR ARG'
				remove();
				AstExpr expr = parseEXPR();
				Vector<AstExpr> args = parseOptionalARG();
				assert args != null;
				args.insertElementAt(expr, 0);
				return args;
			default:
				error();
		}
		return null;
	}

	private AstExpr parseOptionalE(AstExpr expr) {
		switch (curr.token) {
			case COLON:
				// E' -> : TYPE
				remove();
				AstType type = parseTYPE();
				assert type != null;
				return new AstCastExpr(new Location(expr.location, type.location), expr, type);
			case RPAREN:
				// E' ->
				return expr;
			case WHERE:
				// E' -> where PRG
				remove();
				Vector<AstDecl> decls = parsePRG();
				Location endLoc = curr.location;
				assert decls != null;
				assert decls.size() > 0;
				return new AstWhereExpr(new Location(expr.location, endLoc), new ASTs<>(new Location(decls.firstElement().location, decls.lastElement().location), decls), expr);
			default:
				error();
		}
		return null;
	}

	private AstStmt parseSTMT() {
		Location startLoc = curr.location;
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
				// STMT -> EXPR STMT' ;
				AstExpr expr = parseEXPR();
				AstStmt stmt = parseOptionalAssigment(expr);
				checkAndRemove(Token.SEMIC);
				return stmt;
			case IF:
				// STMT -> if EXPR then STMT STMT* ELSE end ;
				remove();
				expr = parseEXPR();
				assert expr != null;
				checkAndRemove(Token.THEN);
				stmt = parseSTMT();
				Vector<AstStmt> stmts = parseOptionalSTMT();
				assert stmts != null;
				stmts.insertElementAt(stmt, 0);
				AstStmtExpr ifStmtExpr = new AstStmtExpr(new Location(stmts.firstElement().location, stmts.lastElement().location), new ASTs<>(new Location(stmts.firstElement().location, stmts.lastElement().location), stmts));
				AstStmtExpr elseStmtExpr = parseELSE();
				checkAndRemove(Token.END);
				Location endLoc = curr.location;
				checkAndRemove(Token.SEMIC);
				return new AstIfStmt(new Location(startLoc, endLoc), expr, new AstExprStmt(ifStmtExpr.location, ifStmtExpr), elseStmtExpr != null ? new AstExprStmt(elseStmtExpr.location, elseStmtExpr) : null);
			case WHILE:
				// STMT -> while EXPR do STMT STMT* end ;
				remove();
				expr = parseEXPR();
				assert expr != null;
				checkAndRemove(Token.DO);
				stmt = parseSTMT();
				stmts = parseOptionalSTMT();
				assert stmts != null;
				stmts.insertElementAt(stmt, 0);
				AstStmtExpr stmtExpr = new AstStmtExpr(new Location(stmts.firstElement().location, stmts.lastElement().location), new ASTs<>(new Location(stmts.firstElement().location, stmts.lastElement().location), stmts));
				checkAndRemove(Token.END);
				endLoc = curr.location;
				checkAndRemove(Token.SEMIC);
				return new AstWhileStmt(new Location(startLoc, endLoc), expr, new AstExprStmt(stmtExpr.location, stmtExpr));
			default:
				error();
		}
		return null;
	}

	private AstStmt parseOptionalAssigment(AstExpr expr1) {
		switch (curr.token) {
			case EQUAL:
				// STMT' -> = EXPR
				remove();
				AstExpr expr2 = parseEXPR();
				assert expr2 != null;
				return new AstAssignStmt(new Location(expr1.location, expr2.location), expr1, expr2);
			case SEMIC:
				// STMT' ->
				return new AstExprStmt(expr1.location, expr1);
			default:
				error();
		}
		return null;
	}

	private Vector<AstStmt> parseOptionalSTMT() {
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
				// STMT* -> STMT STMT*
				AstStmt stmt = parseSTMT();
				Vector<AstStmt> stmts = parseOptionalSTMT();
				assert stmts != null;
				stmts.insertElementAt(stmt, 0);
				return stmts;
			case RBRACES:
			case END:
			case ELSE:
				// STMT* ->
				return new Vector<>();
			default:
				error();
		}
		return null;
	}

	private AstStmtExpr parseELSE() {
		switch (curr.token) {
			case END:
				// ELSE ->
				return null;
			case ELSE:
				// ELSE -> else STMT STMT*
				remove();
				AstStmt stmt = parseSTMT();
				Vector<AstStmt> stmts = parseOptionalSTMT();
				assert stmts != null;
				stmts.insertElementAt(stmt, 0);
				return new AstStmtExpr(new Location(stmts.firstElement().location, stmts.lastElement().location), new ASTs<>(new Location(stmts.firstElement().location, stmts.lastElement().location), stmts));
			default:
				error();
		}
		return null;
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
