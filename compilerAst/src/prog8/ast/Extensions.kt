package prog8.ast

import prog8.ast.base.FatalAstException
import prog8.ast.expressions.BinaryExpression
import prog8.ast.expressions.Expression
import prog8.ast.statements.VarDecl
import prog8.ast.statements.VarDeclOrigin
import prog8.ast.statements.VarDeclType
import prog8.code.core.DataType
import prog8.code.core.Position
import prog8.code.core.ZeropageWish


fun Program.getTempVar(dt: DataType, altNames: Boolean=false): Pair<List<String>, VarDecl> {
    val tmpvarName = if(altNames) {
        when (dt) {
            DataType.UBYTE, DataType.BOOL -> listOf("prog8_lib", "tempvar_ub2")
            DataType.BYTE -> listOf("prog8_lib", "tempvar_b2")
            DataType.UWORD -> listOf("prog8_lib", "tempvar_uw2")
            DataType.WORD -> listOf("prog8_lib", "tempvar_w2")
            DataType.FLOAT -> listOf("floats", "tempvar_swap_float2")
            else -> throw FatalAstException("invalid dt")
        }
    } else {
        when (dt) {
            DataType.UBYTE, DataType.BOOL -> listOf("prog8_lib", "tempvar_ub")
            DataType.BYTE -> listOf("prog8_lib", "tempvar_b")
            DataType.UWORD -> listOf("prog8_lib", "tempvar_uw")
            DataType.WORD -> listOf("prog8_lib", "tempvar_w")
            DataType.FLOAT -> listOf("floats", "tempvar_swap_float")
            else -> throw FatalAstException("invalid dt")
        }
    }

    val block = this.allBlocks.first { it.name==tmpvarName[0] }
    val existingDecl = block.statements.firstOrNull { it is VarDecl && it.name == tmpvarName[1] }
    if(existingDecl!=null)
        return Pair(tmpvarName, existingDecl as VarDecl)

    // add new temp variable to the ast directly (we can do this here because we're not iterating inside those container blocks)
    val decl = VarDecl(
        VarDeclType.VAR, VarDeclOrigin.AUTOGENERATED, dt, ZeropageWish.DONTCARE,
        null, tmpvarName[1], null, isArray = false, sharedWithAsm = false, position = Position.DUMMY
    )
    block.statements.add(decl)
    decl.linkParents(block)
    return Pair(tmpvarName, decl)
}

fun maySwapOperandOrder(binexpr: BinaryExpression): Boolean {
    fun ok(expr: Expression): Boolean {
        return when(expr) {
            is BinaryExpression -> expr.left.isSimple
            is IFunctionCall -> false
            else -> expr.isSimple
        }
    }
    return ok(binexpr.left) || ok(binexpr.right)
}
