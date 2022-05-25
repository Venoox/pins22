package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.*;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;
import pins.phase.memory.*;

public class CodeGenerator extends AstFullVisitor<Object, Stack<MemFrame>> {

    @Override
    public Object visit(AstFunDecl funDecl, Stack<MemFrame> frames) {
        MemFrame frame = Memory.frames.get(funDecl);
        if (frames == null) {
            frames = new Stack<>();
        }
        frames.push(frame);
        ImcExpr funCode = funDecl.expr.accept(new ExprGenerator(), frames);
        ImcGen.exprImc.put(funDecl.expr, funCode);
        frames.pop();
        return null;
    }
}
