package pins.phase.imcgen;

import java.util.*;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.*;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;
import pins.phase.memory.Memory;
import pins.phase.seman.SemAn;
import pins.data.typ.*;

public class ExprGenerator implements AstVisitor<ImcExpr, Stack<MemFrame>> {

	@Override
	public ImcExpr visit(AstWhereExpr whereExpr, Stack<MemFrame> frames) {
		whereExpr.decls.accept(new CodeGenerator(), frames);
		ImcExpr code = whereExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(whereExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstBinExpr binExpr, Stack<MemFrame> frames) {
		ImcExpr fstExpr = binExpr.fstSubExpr.accept(this, frames);
		ImcExpr sndExpr = binExpr.sndSubExpr.accept(this, frames);
		ImcBINOP.Oper oper = null;
		switch (binExpr.oper) {
			case OR:
				oper = ImcBINOP.Oper.OR;
				break;
			case AND:
				oper = ImcBINOP.Oper.AND;
				break;
			case EQU:
				oper = ImcBINOP.Oper.EQU;
				break;
			case NEQ:
				oper = ImcBINOP.Oper.NEQ;
				break;
			case LTH:
				oper = ImcBINOP.Oper.LTH;
				break;
			case GTH:
				oper = ImcBINOP.Oper.GTH;
				break;
			case LEQ:
				oper = ImcBINOP.Oper.LEQ;
				break;
			case GEQ:
				oper = ImcBINOP.Oper.GEQ;
				break;
			case ADD:
				oper = ImcBINOP.Oper.ADD;
				break;
			case SUB:
				oper = ImcBINOP.Oper.SUB;
				break;
			case MUL:
				oper = ImcBINOP.Oper.MUL;
				break;
			case DIV:
				oper = ImcBINOP.Oper.DIV;
				break;
			case MOD:
				oper = ImcBINOP.Oper.MOD;
				break;
		}
		ImcExpr code = null;
		if (binExpr.oper == AstBinExpr.Oper.ARR) {
			SemType arrayType = ((SemArr) SemAn.exprOfType.get(binExpr.fstSubExpr)).elemType;
			code = new ImcMEM(
					new ImcBINOP(
							ImcBINOP.Oper.ADD,
							((ImcMEM) fstExpr).addr,
							new ImcBINOP(
									ImcBINOP.Oper.MUL,
									sndExpr,
									new ImcCONST(arrayType.size())
							)
					)
			);
		} else {
			code = new ImcBINOP(oper, fstExpr, sndExpr);
		}
		ImcGen.exprImc.put(binExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstCallExpr callExpr, Stack<MemFrame> frames) {

		MemFrame currFrame = frames.peek();
		MemFrame callFrame = Memory.frames.get(SemAn.declaredAt.get(callExpr));
		Vector<Long> offs = new Vector<Long>();
		long size = 0L;
		offs.add(size);
		for (AstExpr arg : callExpr.args.asts()) {
			size += SemAn.exprOfType.get(arg).size();
			offs.add(size);
		}
		Vector<ImcExpr> args = new Vector<>();
		ImcExpr SL;
		if (callFrame.depth == 0) {
			SL = new ImcCONST(0);
		} else {
			SL = new ImcTEMP(currFrame.FP);
			int levels = (currFrame.depth + 1) - callFrame.depth;
			for (int j = 0; j < levels; j++) {
				SL = new ImcMEM(SL);
			}
		}
		args.add(SL);
		for (AstExpr arg : callExpr.args.asts()) {
			args.add(arg.accept(new ExprGenerator(), frames));
		}
		ImcExpr code = new ImcCALL(callFrame.label, offs, args);
		ImcGen.exprImc.put(callExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstCastExpr castExpr, Stack<MemFrame> frames) {
		ImcExpr code = castExpr.subExpr.accept(this, frames);
		SemType exprType = SemAn.exprOfType.get(castExpr.subExpr).actualType();
		SemType castType = SemAn.describesType.get(castExpr.type).actualType();
		if ((exprType instanceof SemInt || exprType instanceof SemPtr) && castType instanceof SemChar) {
			code = new ImcBINOP(ImcBINOP.Oper.MOD, code, new ImcCONST(256));
		}
		ImcGen.exprImc.put(castExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstConstExpr constExpr, Stack<MemFrame> frames) {
		long value = 0;
		switch (constExpr.kind) {
			case INT:
				value = Integer.parseInt(constExpr.name);
				break;
			case CHAR:
				value = constExpr.name.charAt(0);
				break;
			case PTR:
			case VOID:
				value = 0;
				break;
		}
		ImcExpr code = new ImcCONST(value);
		ImcGen.exprImc.put(constExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstNameExpr nameExpr, Stack<MemFrame> frames) {
		AstDecl decl = SemAn.declaredAt.get(nameExpr);
		MemFrame currFrame = frames.peek();
		ImcExpr code = null;
		if (decl instanceof AstVarDecl) {
			MemAccess access = Memory.varAccesses.get(decl);
			if (access instanceof MemAbsAccess) {
				code = new ImcMEM(new ImcNAME(((MemAbsAccess) access).label));
			} else if (access instanceof MemRelAccess) {
				MemRelAccess relAccess = (MemRelAccess) access;
				int levels = (currFrame.depth + 1) - relAccess.depth;
				ImcExpr FP = new ImcTEMP(currFrame.FP);
				for (int i = 0; i < levels; i++) {
					FP = new ImcMEM(FP);
				}
				code = new ImcMEM(new ImcBINOP(
						ImcBINOP.Oper.ADD,
						FP,
						new ImcCONST(relAccess.offset)
				));
			}
		} else if (decl instanceof AstParDecl) {
			MemRelAccess relAccess = Memory.parAccesses.get(decl);
			int levels = (currFrame.depth + 1) - relAccess.depth;
			ImcExpr FP = new ImcTEMP(currFrame.FP);
			for (int i = 0; i < levels; i++) {
				FP = new ImcMEM(FP);
			}
			code = new ImcMEM(new ImcBINOP(
					ImcBINOP.Oper.ADD,
					FP,
					new ImcCONST(relAccess.offset)
			));
		} else if (decl instanceof AstTypDecl) {
			throw new Report.Error(decl.location, "Cannot assign value to a type '" + decl.name + "'");
		}
		ImcGen.exprImc.put(nameExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstPreExpr preExpr, Stack<MemFrame> frames) {
		ImcExpr subExpr = preExpr.subExpr.accept(this, frames);
		ImcExpr code = null;
		switch (preExpr.oper) {
			case ADD:
				// skip as it doesn't affect
				code = subExpr;
				break;
			case SUB:
				code = new ImcUNOP(ImcUNOP.Oper.NEG, subExpr);
				break;
			case NOT:
				code = new ImcUNOP(ImcUNOP.Oper.NOT, subExpr);
				break;
			case PTR:
				// get address of the variable
				code = ((ImcMEM) subExpr).addr;
				break;
			case NEW:
				// change into function new
				Vector<Long> offs = new Vector<>();
				offs.add(0L); offs.add(8L);
				Vector<ImcExpr> args = new Vector<>();
				args.add(new ImcCONST(0));
				args.add(subExpr);
				code = new ImcCALL(new MemLabel("new"), offs, args);
				break;
			case DEL:
				// change into function del
				offs = new Vector<>();
				offs.add(0L); offs.add(8L);
				args = new Vector<>();
				args.add(new ImcCONST(0));
				args.add(subExpr);
				code = new ImcCALL(new MemLabel("del"), offs, args);
				break;
		}
		ImcGen.exprImc.put(preExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstPstExpr pstExpr, Stack<MemFrame> frames) {
		ImcExpr subExpr = pstExpr.subExpr.accept(this, frames);
		ImcExpr code = new ImcMEM(subExpr);
		ImcGen.exprImc.put(pstExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstStmtExpr stmtExpr, Stack<MemFrame> frames) {
		Vector<ImcStmt> stmts = new Vector<>();
		for (AstStmt stmt : stmtExpr.stmts.asts()) {
			stmts.add(stmt.accept(new StmtGenerator(), frames));
		}
		ImcExpr returnExpr = null;
		if (stmts.lastElement() instanceof ImcESTMT) {
			returnExpr = ((ImcESTMT) stmts.lastElement()).expr;
			stmts.remove(stmts.size()-1);
		} else {
			returnExpr = new ImcCONST(0);
		}
		ImcExpr code = new ImcSEXPR(new ImcSTMTS(stmts), returnExpr);
		ImcGen.exprImc.put(stmtExpr, code);
		return code;
	}

}
