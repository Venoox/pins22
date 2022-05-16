package pins.phase.memory;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.mem.*;
import pins.data.typ.*;
import pins.phase.seman.*;

import java.util.Vector;

/**
 * Computing memory layout: frames and accesses.
 */
public class MemEvaluator extends AstFullVisitor<Object, MemEvaluator.FunContext> {


	/**
	 * Functional context, i.e., used when traversing function and building a new
	 * frame, parameter acceses and variable acceses.
	 */
	protected class FunContext {
		public int depth = 0;
		public long locsSize = 0;
		public long argsSize = 0;
		public long parsSize = new SemPtr(new SemVoid()).size();
	}

	@Override
	public Object visit(AstFunDecl funDecl, FunContext funContext) {
		FunContext newContext = new FunContext();
		if (funContext == null)
			newContext.depth = 1;
		else
			newContext.depth = funContext.depth + 1;
		super.visit(funDecl, newContext);
		MemLabel label;
		if (funContext == null) {
			// global func
			label = new MemLabel(funDecl.name);
		} else {
			// local func
			label = new MemLabel();
		}
		Memory.frames.put(funDecl, new MemFrame(label, funContext == null ? 0 : funContext.depth, newContext.locsSize, newContext.argsSize + 8));
		return null;
	}

	@Override
	public Object visit(AstParDecl parDecl, FunContext funContext) {
		super.visit(parDecl, funContext);

		long size = SemAn.describesType.get(parDecl.type).size();
		MemRelAccess relAccess = new MemRelAccess(size, funContext.parsSize, funContext.depth);
		funContext.parsSize += size;
		Memory.parAccesses.put(parDecl, relAccess);
		return null;
	}

	@Override
	public Object visit(AstVarDecl varDecl, FunContext funContext) {
		super.visit(varDecl, funContext);
		MemAccess access;
		long size = SemAn.describesType.get(varDecl.type).size();
		if (funContext == null) {
			// global var
			access = new MemAbsAccess(size, new MemLabel(varDecl.name));
		} else {
			// local var
			funContext.locsSize += size;
			access = new MemRelAccess(size, -funContext.locsSize, funContext.depth);
		}
		Memory.varAccesses.put(varDecl, access);
		return null;
	}

	@Override
	public Object visit(AstCallExpr callExpr, FunContext funContext) {
		super.visit(callExpr, funContext);

		long size = 0;
		Vector<AstExpr> args = callExpr.args.asts();
		for (AstExpr arg : args) {
			size += SemAn.exprOfType.get(arg).size();
		}
		if (size > funContext.argsSize) {
			funContext.argsSize = size;
		}
		return null;
	}


}
