package pins.phase.lexan;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import pins.common.report.*;
import pins.data.symbol.*;

/**
 * Lexical analyzer.
 */
public class LexAn implements AutoCloseable {

	private String srcFileName;

	private FileReader srcFile;

	private int currRow = 1;
	private int currCol = 1;

	private String source;
	private int currChar = 0;

	public LexAn(String srcFileName) {
		this.srcFileName = srcFileName;
		try {
			srcFile = new FileReader(new File(srcFileName));
			source = new String(Files.readAllBytes(Paths.get(srcFileName)), StandardCharsets.US_ASCII);
		} catch (FileNotFoundException __) {
			throw new Report.Error("Cannot open source file '" + srcFileName + "'.");
		} catch (IOException e) {
			throw new Report.Error("Cannot read source file '" + srcFileName + "'.");
		}
	}

	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file '" + srcFileName + "'.");
		}
	}

	private int beg = 0;
	private int end = 0;
	private int begCol = 0;
	private int endCol = 0;
	private Token token = null;
	private Location loc = null;

	public Symbol lexer()  {
		token = null;
		while (currChar < source.length()) {
			beg = end = currChar;
			begCol = endCol = currCol;
			char in = current();
			if (in == ' ' || in == '\r') {
				currChar++;
				currCol++;
				continue;
			}
			if (in == '\t') {
				currChar++;
				currCol = currCol / 8 * 8 + 8; // next multiple of 8
				continue;
			}
			if (in == '\n') {
				currChar++;
				currRow++;
				currCol = 1;
				continue;
			}
			if (in == '(') {
				token = Token.LPAREN;
			}
			else if (in == ')') {
				token = Token.RPAREN;
			}
			else if (in == '{') {
				token = Token.LBRACES;
			}
			else if (in == '}') {
				token = Token.RBRACES;
			}
			else if (in == '[') {
				token = Token.LBRACKET;
			}
			else if (in == ']') {
				token = Token.RBRACKET;
			}
			else if (in == ',') {
				token = Token.COMMA;
			}
			else if (in == ':') {
				token = Token.COLON;
			}
			else if (in == ';') {
				token = Token.SEMIC;
			}
			else if (in == '&') {
				token = Token.AND;
			}
			else if (in == '|') {
				token = Token.OR;
			}
			else if (in == '!') {
				if (peek() == '=') {
					advance();
					token = Token.NOT_EQUAL;
				} else {
					token = Token.NOT;
				}
				end = currChar + 1;
				endCol = currCol;
			}
			else if (in == '=') {
				if (peek() == '=') {
					advance();
					token = Token.EQUAL_EQUAL;
				} else {
					token = Token.EQUAL;
				}
				end = currChar + 1;
				endCol = currCol;
			}
			else if (in == '<') {
				if (peek() == '=') {
					advance();
					token = Token.LESS_EQUAL;
				} else {
					token = Token.LESS;
				}
				end = currChar + 1;
				endCol = currCol;
			}
			else if (in == '>') {
				if (peek() == '=') {
					advance();
					token = Token.GREATER_EQUAL;
				} else {
					token = Token.GREATER;
				}
				end = currChar + 1;
				endCol = currCol;
			}
			else if (in == '*') {
				token = Token.MUL;
			}
			else if (in == '/') {
				token = Token.DIV;
			}
			else if (in == '%') {
				token = Token.MOD;
			}
			else if (in == '^') {
				token = Token.CARAT;
			}
			else if (in == '+') {
				token = Token.PLUS;
			}
			else if (in == '-') {
				token = Token.MINUS;
			}
			else if (isNumeric(in)) {
				token = Token.CONST_INT;
				getNumber();
			}
			else if (isAlphabetic(in) || in == '_') {
				// identifier, keyword or const
				getString();
			}
			else if (in == '#' && peek() == '{') {
				skipComment();
				continue;
			}
			else if (in == '\'') {
				getChar();
			} else {
				throw new Report.Error("Unknown token '" + source.substring(beg, currChar+1) + "' at " + currRow + ":" + currCol);
			}
			advance();
			break;
		}
		if (beg == end && begCol == endCol) {
			end = beg + 1;
		}
		if (begCol != 0 && endCol != 0) {
			loc = new Location(currRow, begCol, currRow, endCol);
		}
		if (token == null) {
			token = Token.EOF;
			beg = end = begCol = endCol = 0;
			loc = null;
		}
		return new Symbol(token, source.substring(beg, end), loc);
	}

	private void advance() {
		currCol++;
		currChar++;
	}

	private char current() {
		if (currChar >= source.length()) return 0;
		return source.charAt(currChar);
	}

	private char peek() {
		if (currChar + 1 >= source.length()) return 0;
		return source.charAt(currChar + 1);
	}

	private char peek(int n) {
		if (currChar + n >= source.length()) return 0;
		return source.charAt(currChar + n);
	}

	private void getChar() {
		advance();
		beg = currChar;
		if (current() == '\\') {
			advance();
		} else if (current() == '\\' || current() == '\'') {
			throw new Report.Error("Character " + current() + " not escaped!");
		}
		if ((int)current() >= 32 && (int)current() <= 126 && peek() == '\'') {
			token = Token.CONST_CHAR;
			advance();
			end = currChar;
			endCol = currCol;
		} else {
			throw new Report.Error("Unclosed char");
		}
	}

	private void getString() {
		while (isValidIdentifierCharacter(peek())) {
			advance();
		}
		end = currChar + 1;
		endCol = currCol;
		String identifier = source.substring(beg, end);
		switch (identifier) {
			case "none": token = Token.CONST_NONE; break;
			case "nil": token = Token.CONST_NIL; break;
			case "char": token = Token.CHAR; break;
			case "del": token = Token.DEL; break;
			case "do": token = Token.DO; break;
			case "else": token = Token.ELSE; break;
			case "end": token = Token.END; break;
			case "fun": token = Token.FUN; break;
			case "if": token = Token.IF; break;
			case "int": token = Token.INT; break;
			case "new": token = Token.NEW; break;
			case "then": token = Token.THEN; break;
			case "typ": token = Token.TYP; break;
			case "var": token = Token.VAR; break;
			case "void": token = Token.VOID; break;
			case "where": token = Token.WHERE; break;
			case "while": token = Token.WHILE; break;
			default: token = Token.IDENTIFIER;
		}
	}

	private void getNumber() {
		while (isNumeric(peek())) {
			advance();
		}
		end = currChar + 1;
		endCol = currCol;
	}

	private void skipComment() {
		while (!(current() == '}' && peek() == '#')) {
			if (currChar >= source.length()) {
				Report.warning("Comment not closed!");
				return;
			}
			if (current() == '\n') {
				currRow++;
				currCol = 1;
			} else {
				currCol++;
			}
			currChar++;
			if (current() == '#' && peek() == '{') {
				skipComment();
			}
		}
		advance(); advance();
	}

	private boolean isAlphabetic(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
	}

	private boolean isAlphanumeric(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
	}

	private boolean isValidIdentifierCharacter(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
	}

	private boolean isNumeric(char c) {
		return c >= '0' && c <= '9';
	}
}
