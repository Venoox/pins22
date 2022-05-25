package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.stmt.*;
import pins.data.imc.code.expr.*;
import pins.data.mem.*;

public class StmtGenerator implements AstVisitor<ImcStmt, Stack<MemFrame>> {

	@Override
	public ImcStmt visit(AstAssignStmt assignStmt, Stack<MemFrame> frames) {
		if (assignStmt.fstSubExpr.accept(new ExprGenerator(), frames) == null) {
			assignStmt.fstSubExpr.accept(new ExprGenerator(), frames);
		}
		ImcStmt code = new ImcMOVE(
				assignStmt.fstSubExpr.accept(new ExprGenerator(), frames),
				assignStmt.sndSubExpr.accept(new ExprGenerator(), frames)
		);
		ImcGen.stmtImc.put(assignStmt, code);
		return code;
	}

	@Override
	public ImcStmt visit(AstExprStmt exprStmt, Stack<MemFrame> frames) {
		ImcExpr expr = exprStmt.expr.accept(new ExprGenerator(), frames);
		ImcStmt stmt = new ImcESTMT(expr);
		ImcGen.stmtImc.put(exprStmt, stmt);
		return stmt;
	}

	@Override
	public ImcStmt visit(AstIfStmt ifStmt, Stack<MemFrame> frames) {
		ImcExpr condExpr = ifStmt.condExpr.accept(new ExprGenerator(), frames);
		ImcStmt thenStmt = ifStmt.thenBodyStmt.accept(this, frames);
		ImcStmt elseStmt = null;
		if (ifStmt.elseBodyStmt != null)
			elseStmt = ifStmt.elseBodyStmt.accept(this, frames);

		ImcLABEL thenLabel = new ImcLABEL(new MemLabel());
		ImcLABEL elseLabel = null;
		if (elseStmt != null)
			elseLabel = new ImcLABEL(new MemLabel());
		ImcLABEL endLabel = new ImcLABEL(new MemLabel());

		Vector<ImcStmt> stmts = new Vector<>();
		if (elseStmt != null)
			stmts.add(new ImcCJUMP(condExpr, thenLabel.label, elseLabel.label));
		else
			stmts.add(new ImcCJUMP(condExpr, thenLabel.label, endLabel.label));
		stmts.add(thenLabel);
		stmts.add(thenStmt);
		if (elseStmt != null) {
			stmts.add(new ImcJUMP(endLabel.label));
			stmts.add(elseLabel);
			stmts.add(elseStmt);
		}
		stmts.add(endLabel);

		ImcStmt code = new ImcSTMTS(stmts);
		ImcGen.stmtImc.put(ifStmt, code);
		return code;
	}

	@Override
	public ImcStmt visit(AstWhileStmt whileStmt, Stack<MemFrame> frames) {
		ImcExpr condExpr = whileStmt.condExpr.accept(new ExprGenerator(), frames);
		ImcStmt bodyStmt = whileStmt.bodyStmt.accept(this, frames);
		ImcLABEL startLabel = new ImcLABEL(new MemLabel());
		ImcLABEL bodyLabel = new ImcLABEL(new MemLabel());
		ImcLABEL endLabel = new ImcLABEL(new MemLabel());
		ImcCJUMP condJump = new ImcCJUMP(condExpr, bodyLabel.label, endLabel.label);
		ImcJUMP jumpToStartLabel = new ImcJUMP(startLabel.label);

		Vector<ImcStmt> stmts = new Vector<>();
		stmts.add(startLabel);
		stmts.add(condJump);
		stmts.add(bodyLabel);
		stmts.add(bodyStmt);
		stmts.add(jumpToStartLabel);
		stmts.add(endLabel);

		ImcStmt code = new ImcSTMTS(stmts);
		ImcGen.stmtImc.put(whileStmt, code);
		return code;
	}


}
