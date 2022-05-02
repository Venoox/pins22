package pins.phase.seman;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.*;

public class NameResolver extends AstFullVisitor<Object, Object> {
    private final SymbTable symbTable = new SymbTable();

    @Override
    public Object visit(ASTs<? extends AST> trees, Object arg) {
        for (AST t : trees.asts())
            if (t instanceof AstTypDecl) {
                try {
                    symbTable.ins(((AstTypDecl) t).name, (AstDecl) t);
                } catch (SymbTable.CannotInsNameException e) {
                    throw new Report.Error("Type '" + ((AstTypDecl) t).name + "' is already declared!");
                }
            }

        for (AST t : trees.asts())
            if (t instanceof AstTypDecl) {
                t.accept(this, arg);
            }

        for (AST t : trees.asts())
            if (t instanceof AstVarDecl) {
                try {
                    symbTable.ins(((AstVarDecl) t).name, (AstDecl) t);
                } catch (SymbTable.CannotInsNameException e) {
                    throw new Report.Error("Variable '" + ((AstVarDecl) t).name + "' is already declared!");
                }
            }

        for (AST t : trees.asts())
            if (t instanceof AstVarDecl) {
                t.accept(this, arg);
            }

        for (AST t : trees.asts())
            if (t instanceof AstFunDecl) {
                try {
                    symbTable.ins(((AstFunDecl) t).name, (AstDecl) t);
                } catch (SymbTable.CannotInsNameException e) {
                    throw new Report.Error("Function '" + ((AstFunDecl) t).name + "' is already declared!");
                }
            }

        for (AST t : trees.asts())
            if (t instanceof AstFunDecl) {
                t.accept(this, arg);
            }

        for (AST t : trees.asts())
            if (!(t instanceof AstFunDecl) && !(t instanceof AstVarDecl) && !(t instanceof AstTypDecl)) {
                t.accept(this, arg);
            }
        return null;
    }

    @Override
    public Object visit(AstTypeName typeName, Object arg) {
        try {
            AstDecl decl = symbTable.fnd(typeName.name);
            SemAn.declaredAt.put(typeName, decl);
        } catch (SymbTable.CannotFndNameException e) {
            throw new Report.Error("Type '" + typeName.name + "' is not declared!");
        }

        return null;
    }

    @Override
    public Object visit(AstNameExpr nameExpr, Object arg) {
        try {
            AstDecl decl = symbTable.fnd(nameExpr.name);
            SemAn.declaredAt.put(nameExpr, decl);
        } catch (SymbTable.CannotFndNameException e) {
            throw new Report.Error("Variable '" + nameExpr.name + "' is not declared!");
        }

        return null;
    }

    @Override
    public Object visit(AstCallExpr callExpr, Object arg) {
        try {
            AstDecl decl = symbTable.fnd(callExpr.name);
            SemAn.declaredAt.put(callExpr, decl);
        } catch (SymbTable.CannotFndNameException e) {
            throw new Report.Error("Function '" + callExpr.name + "' is not declared!");
        }

        if (callExpr.args != null)
            callExpr.args.accept(this, arg);
        return null;
    }

    @Override
    public Object visit(AstFunDecl funDecl, Object arg) {
        if (funDecl.type != null)
            funDecl.type.accept(this, arg);
        if (funDecl.pars != null)
            funDecl.pars.accept(this, arg);
        symbTable.newScope();
        assert funDecl.pars != null;
        for (AstParDecl par : funDecl.pars.asts()) {
            try {
                symbTable.ins(par.name, par);
            } catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error("Parameter '" + par.name + "' is already declared!");
            }
        }
        if (funDecl.expr != null)
            funDecl.expr.accept(this, arg);
        symbTable.oldScope();
        return null;
    }

    @Override
    public Object visit(AstWhereExpr whereExpr, Object arg) {
        symbTable.newScope();
        if (whereExpr.decls != null)
            whereExpr.decls.accept(this, arg);
        if (whereExpr.subExpr != null)
            whereExpr.subExpr.accept(this, arg);
        symbTable.oldScope();
        return null;
    }
}
