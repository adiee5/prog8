package prog8.compiler.astprocessing

import prog8.ast.IFunctionCall
import prog8.ast.Node
import prog8.ast.Program
import prog8.ast.expressions.*
import prog8.ast.statements.FunctionCallStatement
import prog8.ast.walk.AstWalker
import prog8.ast.walk.IAstModification
import prog8.code.core.BaseDataType
import prog8.code.core.IErrorReporter
import prog8.code.core.isByte
import prog8.code.core.isWord


internal class BeforeAsmTypecastCleaner(val program: Program,
                                        private val errors: IErrorReporter
) : AstWalker() {

    override fun after(typecast: TypecastExpression, parent: Node): Iterable<IAstModification> {
        // see if we can remove redundant typecasts (outside of expressions)
        // such as casting byte<->ubyte,  word<->uword  or even redundant casts (sourcetype = target type).
        // the special typecast of a reference type (str, array) to an UWORD will be changed into address-of,
        //   UNLESS it's a str parameter in the containing subroutine - then we remove the typecast altogether
        val sourceDt = typecast.expression.inferType(program).getOrUndef()
        if (typecast.type.isByte && sourceDt.isByte || typecast.type.isWord && sourceDt.isWord) {
            if(typecast.parent !is Expression) {
                return listOf(IAstModification.ReplaceNode(typecast, typecast.expression, parent))
            }
        }

        if(typecast.type==sourceDt.base)
            return listOf(IAstModification.ReplaceNode(typecast, typecast.expression, parent))

        if(sourceDt.isPassByRef) {
            if(typecast.type == BaseDataType.UWORD) {
                val identifier = typecast.expression as? IdentifierReference
                if(identifier!=null) {
                    return if(identifier.isSubroutineParameter()) {
                        listOf(
                            IAstModification.ReplaceNode(
                                typecast,
                                typecast.expression,
                                parent
                            )
                        )
                    } else {
                        listOf(
                            IAstModification.ReplaceNode(
                                typecast,
                                AddressOf(identifier, null, false, typecast.position),
                                parent
                            )
                        )
                    }
                } else if(typecast.expression is IFunctionCall) {
                    return listOf(
                        IAstModification.ReplaceNode(
                            typecast,
                            typecast.expression,
                            parent
                        )
                    )
                }
            } else {
                errors.err("cannot cast pass-by-reference value to type ${typecast.type} (only to UWORD)", typecast.position)
            }
        }

        return noModifications
    }

    override fun after(functionCallStatement: FunctionCallStatement, parent: Node): Iterable<IAstModification> {
        if(functionCallStatement.target.nameInSource==listOf("cmp")) {
            // if the datatype of the arguments of cmp() are different, cast the byte one to word.
            val arg1 = functionCallStatement.args[0]
            val arg2 = functionCallStatement.args[1]
            val dt1 = arg1.inferType(program).getOrUndef()
            val dt2 = arg2.inferType(program).getOrUndef()
            if(dt1.isBool && dt2.isBool)
                return noModifications
            else if(dt1.isByte) {
                if(dt2.isByte)
                    return noModifications
                val (replaced, cast) = arg1.typecastTo(if(dt1.isUnsignedByte) BaseDataType.UWORD else BaseDataType.WORD, dt1, true)
                if(replaced)
                    return listOf(IAstModification.ReplaceNode(arg1, cast, functionCallStatement))
            } else {
                if(dt2.isWord)
                    return noModifications
                val (replaced, cast) = arg2.typecastTo(if(dt2.isUnsignedByte) BaseDataType.UWORD else BaseDataType.WORD, dt2, true)
                if(replaced)
                    return listOf(IAstModification.ReplaceNode(arg2, cast, functionCallStatement))
            }
        }
        return noModifications
    }

    override fun after(expr: BinaryExpression, parent: Node): Iterable<IAstModification> {
        if(expr.operator=="<<" || expr.operator==">>") {
            val shifts = expr.right.constValue(program)
            if(shifts!=null) {
                val dt = expr.left.inferType(program)
                if(dt issimpletype BaseDataType.UBYTE && shifts.number>=8.0)
                    errors.info("shift always results in 0", expr.position)
                if(dt issimpletype BaseDataType.UWORD && shifts.number>=16.0)
                    errors.info("shift always results in 0", expr.position)
                if(shifts.number<=255.0 && shifts.type.isWord) {
                    val byteVal = NumericLiteral(BaseDataType.UBYTE, shifts.number, shifts.position)
                    return listOf(IAstModification.ReplaceNode(expr.right, byteVal, expr))
                }
            }
        }
        return noModifications
    }
}