package pins.data.ast;

import pins.common.report.*;

/**
 * A binary expression.
 */
public class AstBinExpr extends AstExpr {

	public enum Oper {
		AND, OR, EQU, NEQ, LTH, GTH, LEQ, GEQ, MUL, DIV, MOD, ADD, SUB, ARR
	};
	
	public final Oper oper;
	
	public final AstExpr fstSubExpr;
	
	public final AstExpr sndSubExpr;
	
	public AstBinExpr(Location location, Oper oper, AstExpr fstSubExpr, AstExpr sndSubExpr) {
		super(location);
		this.oper = oper;
		this.fstSubExpr = fstSubExpr;
		this.sndSubExpr = sndSubExpr;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstBinExpr(" + oper + ")\033[0m @(" + location + ")");
		fstSubExpr.log(pfx + "  ");
		sndSubExpr.log(pfx + "  ");
	}

}
