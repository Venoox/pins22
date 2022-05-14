package pins.phase.seman;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.AstFullVisitor;
import pins.data.typ.*;

import java.util.Vector;

public class TypeChecker extends AstFullVisitor<Object, Object> {

    private boolean compareTypes(SemType type1, SemType type2) {
        if (type1 instanceof SemInt && type2 instanceof SemInt) {
            return true;
        }
        else if (type1 instanceof SemChar && type2 instanceof SemChar) {
            return true;
        }
        else if (type1 instanceof SemVoid && type2 instanceof SemVoid) {
            return true;
        }
        else if (type1 instanceof SemPtr && type2 instanceof SemPtr) {
            return compareTypes(((SemPtr) type1).baseType, ((SemPtr) type2).baseType);
        }
        else if (type1 instanceof SemArr && type2 instanceof SemArr) {
            if (((SemArr) type1).numElems != ((SemArr) type2).numElems)
                return false;
            return compareTypes(((SemArr) type1).elemType, ((SemArr) type2).elemType);
        }
        else {
            return false;
        }
    }

    @Override
    public Object visit(AstConstExpr constExpr, Object arg) {
        SemType type = null;
        switch (constExpr.kind) {
            case VOID:
                type = new SemVoid();
                break;
            case INT:
                type = new SemInt();
                break;
            case PTR:
                type = new SemPtr(new SemVoid());
                break;
            case CHAR:
                type = new SemChar();
                break;
        }
        SemAn.exprOfType.put(constExpr, type);
        return null;
    }

    @Override
    public Object visit(AstPreExpr preExpr, Object arg) {
        if (preExpr.subExpr != null)
            preExpr.subExpr.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType type = null;
        switch (preExpr.oper) {
            case PTR:
                SemType subType = SemAn.exprOfType.get(preExpr.subExpr).actualType();
                type = new SemPtr(subType);
                break;
            case ADD:
            case SUB:
                type = new SemInt();
                break;
            case DEL:
                subType = SemAn.exprOfType.get(preExpr.subExpr).actualType();
                if (!(subType instanceof SemPtr)) {
                    throw new Report.Error(preExpr.subExpr.location, "Expression should be a pointer");
                }
                type = new SemVoid();
                break;
            case NEW:
                subType = SemAn.exprOfType.get(preExpr.subExpr).actualType();
                if (!(subType instanceof SemInt)) {
                    throw new Report.Error(preExpr.subExpr.location, "Expression should be a CONST_INT");
                }
                type = new SemPtr(new SemVoid());
                break;
            case NOT:
                subType = SemAn.exprOfType.get(preExpr.subExpr).actualType();
                if (!(subType instanceof SemInt)) {
                    throw new Report.Error(preExpr.subExpr.location, "Expression should be a CONST_INT");
                }
                type = new SemInt();
                break;
        }
        SemAn.exprOfType.put(preExpr, type);
        return null;
    }

    @Override
    public Object visit(AstPstExpr pstExpr, Object arg) {
        if (pstExpr.subExpr != null)
            pstExpr.subExpr.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType type = null;
        switch (pstExpr.oper) {
            case PTR:
                SemType subType = SemAn.exprOfType.get(pstExpr.subExpr).actualType();
                if (!(subType instanceof SemPtr)) {
                    throw new Report.Error(pstExpr.location, "Only a pointer type can be dereferenced");
                }
                type = ((SemPtr) subType).baseType;
                break;
        }
        SemAn.exprOfType.put(pstExpr, type);
        return null;
    }

    @Override
    public Object visit(AstBinExpr binExpr, Object arg) {
        if (binExpr.fstSubExpr != null)
            binExpr.fstSubExpr.accept(this, arg);
        else
            throw new Report.InternalError();
        if (binExpr.sndSubExpr != null)
            binExpr.sndSubExpr.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType fstSubType = SemAn.exprOfType.get(binExpr.fstSubExpr).actualType();
        SemType sndSubType = SemAn.exprOfType.get(binExpr.sndSubExpr).actualType();
        if (binExpr.oper == AstBinExpr.Oper.ARR) {
            if (!(fstSubType instanceof SemArr)) {
                throw new Report.Error(binExpr.fstSubExpr.location, "Expression should be a variable of type array");
            }
            if (!(sndSubType instanceof SemInt)) {
                throw new Report.Error(binExpr.fstSubExpr.location, "Expression should be a CONST_INT");
            }
            SemAn.exprOfType.put(binExpr, ((SemArr) fstSubType).elemType);
        } else {
            // TODO: improve check
            if (!(fstSubType instanceof SemInt && sndSubType instanceof SemInt)
                    && !(fstSubType instanceof SemChar && sndSubType instanceof SemChar)
                    && !(fstSubType instanceof SemPtr && sndSubType instanceof SemPtr)) {
                throw new Report.Error(binExpr.location, "Cannot do binary operation between different types");
            }
            SemAn.exprOfType.put(binExpr, new SemInt());
        }
        return null;
    }

    @Override
    public Object visit(AstNameExpr nameExpr, Object arg) {
        AstDecl decl = SemAn.declaredAt.get(nameExpr);
        SemType type = SemAn.describesType.get(decl.type);
        if (type == null) {
            decl.accept(this, arg);
            type = SemAn.describesType.get(decl.type);
        }
        SemAn.exprOfType.put(nameExpr, type.actualType());
        return null;
    }

    @Override
    public Object visit(AstCallExpr callExpr, Object arg) {
        if (callExpr.args != null)
            callExpr.args.accept(this, arg);
        else
            throw new Report.InternalError();

        AstFunDecl funDecl = (AstFunDecl) SemAn.declaredAt.get(callExpr);
        if (SemAn.describesType.get(funDecl.type) == null) { // if function hasn't been visited
            funDecl.type.accept(this, arg);
            funDecl.pars.accept(this, arg);
        }

        Vector<AstExpr> exprs = callExpr.args.asts();
        Vector<AstParDecl> pars = funDecl.pars.asts();
        if (exprs.size() != pars.size()) {
            throw new Report.Error(callExpr.args.location, "Number of arguments doesn't match the number of parameters");
        }
        for (int i = 0; i < exprs.size(); i++) {
            SemType exprType = SemAn.exprOfType.get(exprs.get(i)).actualType();
            SemType parType = SemAn.describesType.get(pars.get(i).type).actualType();

            if (!compareTypes(exprType, parType)) {
                throw new Report.Error(exprs.get(i).location, "Argument " + i + " doesnt match the parameter type");
            }
        }

        SemType type = SemAn.describesType.get(funDecl.type).actualType();
        SemAn.exprOfType.put(callExpr, type);
        return null;
    }

    @Override
    public Object visit(AstCastExpr castExpr, Object arg) {
        if (castExpr.subExpr != null)
            castExpr.subExpr.accept(this, arg);
        else
            throw new Report.InternalError();
        if (castExpr.type != null)
            castExpr.type.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType exprType = SemAn.exprOfType.get(castExpr.subExpr).actualType();
        SemType castType = SemAn.describesType.get(castExpr.type).actualType();
        if ((exprType instanceof SemChar || exprType instanceof SemInt || exprType instanceof SemPtr) &&
                (castType instanceof SemChar || castType instanceof SemInt || castType instanceof SemPtr)) {
            SemAn.exprOfType.put(castExpr, castType);
        } else {
            throw new Report.Error(castExpr.type.location, "Wrong type to cast");
        }
        return null;
    }

    @Override
    public Object visit(AstStmtExpr stmtExpr, Object arg) {
        if (stmtExpr.stmts != null)
            stmtExpr.stmts.accept(this, arg);
        else
            throw new Report.InternalError();

        Vector<AstStmt> stmts = stmtExpr.stmts.asts();
        SemType type = SemAn.stmtOfType.get(stmts.lastElement()).actualType();
        SemAn.exprOfType.put(stmtExpr, type);
        return null;
    }

    @Override
    public Object visit(AstWhereExpr whereExpr, Object arg) {
        if (whereExpr.subExpr != null)
            whereExpr.subExpr.accept(this, arg);
        else
            throw new Report.InternalError();
        if (whereExpr.decls != null)
            whereExpr.decls.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType subType = SemAn.exprOfType.get(whereExpr.subExpr).actualType();
        SemAn.exprOfType.put(whereExpr, subType);
        return null;
    }

    @Override
    public Object visit(AstAssignStmt assignStmt, Object arg) {
        if (assignStmt.fstSubExpr != null)
            assignStmt.fstSubExpr.accept(this, arg);
        else
            throw new Report.InternalError();
        if (assignStmt.sndSubExpr != null)
            assignStmt.sndSubExpr.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType fstSubType = SemAn.exprOfType.get(assignStmt.fstSubExpr).actualType();
        SemType sndSubType = SemAn.exprOfType.get(assignStmt.sndSubExpr).actualType();

        if (!(fstSubType instanceof SemInt && sndSubType instanceof SemInt) &&
                !(fstSubType instanceof SemChar && sndSubType instanceof SemChar) &&
                !(fstSubType instanceof SemPtr && sndSubType instanceof SemPtr)) {
            throw new Report.Error(assignStmt.location, "Left-hand side expression is of different type than right-hand side");
        }

        if (!compareTypes(fstSubType, sndSubType)) {
            throw new Report.Error(assignStmt.location, "Left-hand side expression is of different type than right-hand side");
        }

        SemAn.stmtOfType.put(assignStmt, new SemVoid());
        return null;
    }

    @Override
    public Object visit(AstExprStmt exprStmt, Object arg) {
        if (exprStmt.expr != null)
            exprStmt.expr.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType type = SemAn.exprOfType.get(exprStmt.expr).actualType();
        SemAn.stmtOfType.put(exprStmt, type);
        return null;
    }

    @Override
    public Object visit(AstIfStmt ifStmt, Object arg) {
        if (ifStmt.condExpr != null)
            ifStmt.condExpr.accept(this, arg);
        else
            throw new Report.InternalError();
        if (ifStmt.thenBodyStmt != null)
            ifStmt.thenBodyStmt.accept(this, arg);
        else
            throw new Report.InternalError();
        if (ifStmt.elseBodyStmt != null)
            ifStmt.elseBodyStmt.accept(this, arg);

        SemType condType = SemAn.exprOfType.get(ifStmt.condExpr).actualType();
        if (!(condType instanceof SemInt)) {
            throw new Report.Error(ifStmt.condExpr.location, "Expression should be of type CONST_INT");
        }
        SemType ifType = SemAn.stmtOfType.get(ifStmt.thenBodyStmt).actualType();
        if (!(ifType instanceof SemVoid)) {
            throw new Report.Error(ifStmt.thenBodyStmt.location, "Statement type should be VOID");
        }
        if (ifStmt.elseBodyStmt != null) {
            SemType elseType = SemAn.stmtOfType.get(ifStmt.elseBodyStmt).actualType();
            if (!(elseType instanceof SemVoid)) {
                throw new Report.Error(ifStmt.elseBodyStmt.location, "Statement type should be VOID");
            }
        }

        SemAn.stmtOfType.put(ifStmt, new SemVoid());
        return null;
    }

    @Override
    public Object visit(AstWhileStmt whileStmt, Object arg) {
        if (whileStmt.condExpr != null)
            whileStmt.condExpr.accept(this, arg);
        else
            throw new Report.InternalError();
        if (whileStmt.bodyStmt != null)
            whileStmt.bodyStmt.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType condType = SemAn.exprOfType.get(whileStmt.condExpr).actualType();
        if (!(condType instanceof SemInt)) {
            throw new Report.Error(whileStmt.condExpr.location, "Expression should be of type CONST_INT");
        }
        SemType bodyType = SemAn.stmtOfType.get(whileStmt.bodyStmt).actualType();
        if (!(bodyType instanceof SemVoid)) {
            throw new Report.Error(whileStmt.bodyStmt.location, "Statement type should be VOID");
        }

        SemAn.stmtOfType.put(whileStmt, new SemVoid());
        return null;
    }

    @Override
    public Object visit(AstFunDecl funDecl, Object arg) {
        if (funDecl.pars != null)
            funDecl.pars.accept(this, arg);
        else
            throw new Report.InternalError();
        if (funDecl.type != null)
            funDecl.type.accept(this, arg);
        else
            throw new Report.InternalError();
        if (funDecl.expr != null)
            funDecl.expr.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType returnType = SemAn.describesType.get(funDecl.type).actualType();
        SemType exprType = SemAn.exprOfType.get(funDecl.expr).actualType();

        if (!(returnType instanceof SemInt) &&
                !(returnType instanceof SemChar) &&
                !(returnType instanceof SemPtr) &&
                !(returnType instanceof SemVoid)) {
            throw new Report.Error(funDecl.type.location, "Return type can be only int, char, pointer or void");
        }

        if (!compareTypes(returnType, exprType)) {
            throw new Report.Error(funDecl.type.location, "Return type is not the same as declared");
        }
        return null;
    }

    @Override
    public Object visit(AstTypDecl typDecl, Object arg) {
        // check if already declared
        SemName self = SemAn.declaresType.get(typDecl);
        if (self != null) return null;

        if (typDecl.type != null)
            typDecl.type.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType type = SemAn.describesType.get(typDecl.type).actualType();
        SemName name = new SemName(typDecl.name);
        name.define(type);
        SemAn.declaresType.put(typDecl, name);
        return null;
    }

    @Override
    public Object visit(AstParDecl parDecl, Object arg) {
        if (parDecl.type != null)
            parDecl.type.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType type = SemAn.describesType.get(parDecl.type).actualType();
        if (!(type instanceof SemInt) &&
                !(type instanceof SemChar) &&
                !(type instanceof SemPtr)) {
            throw new Report.Error(parDecl.type.location, "Parameter type can be only int, char or pointer");
        }

        return null;
    }

    @Override
    public Object visit(AstVarDecl varDecl, Object arg) {
        if (varDecl.type != null)
            varDecl.type.accept(this, arg);
        else
            throw new Report.InternalError();

        return null;
    }

    @Override
    public Object visit(AstArrType arrType, Object arg) {
        if (arrType.elemType != null)
            arrType.elemType.accept(this, arg);
        else
            throw new Report.InternalError();
        if (arrType.size != null)
            arrType.size.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType type = SemAn.describesType.get(arrType.elemType);
        if (type instanceof SemVoid) {
            throw new Report.Error(arrType.elemType.location, "Array type should not be VOID");
        }
        if (!(arrType.size instanceof AstConstExpr)) {
            throw new Report.Error(arrType.size.location,"Array size should be CONST_INT");
        }
        if (((AstConstExpr) arrType.size).kind != AstConstExpr.Kind.INT) {
            throw new Report.Error(arrType.size.location, "Array size should be CONST_INT not CONST_" + ((AstConstExpr) arrType.size).kind.name());
        }
        int size = Integer.parseInt(((AstConstExpr) arrType.size).name);
        if (size <= 0) {
            throw new Report.Error(arrType.size.location,"Array size should be higher than 0.");
        }
        SemAn.describesType.put(arrType, new SemArr(type.actualType(), size));
        return null;
    }

    @Override
    public Object visit(AstAtomType atomType, Object arg) {
        SemType type = null;
        switch (atomType.kind) {
            case INT:
                type = new SemInt();
                break;
            case CHAR:
                type = new SemChar();
                break;
            case VOID:
                type = new SemVoid();
                break;
        }
        SemAn.describesType.put(atomType, type);
        return null;
    }

    @Override
    public Object visit(AstPtrType ptrType, Object arg) {
        if (ptrType.subType != null)
            ptrType.subType.accept(this, arg);
        else
            throw new Report.InternalError();

        SemType type = SemAn.describesType.get(ptrType.subType);
        SemAn.describesType.put(ptrType, new SemPtr(type.actualType()));
        return null;
    }

    @Override
    public Object visit(AstTypeName typeName, Object arg) {
        AstDecl decl = SemAn.declaredAt.get(typeName);
        SemName name = SemAn.declaresType.get(decl);
        if (name == null) {
            decl.accept(this, arg);
            name = SemAn.declaresType.get(decl);
        }
        SemAn.describesType.put(typeName, name.actualType());
        return null;
    }
}
