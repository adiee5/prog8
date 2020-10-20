package prog8.ast.antlr

import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import prog8.ast.Module
import prog8.ast.base.*
import prog8.ast.expressions.*
import prog8.ast.statements.*
import prog8.compiler.target.CompilationTarget
import prog8.parser.CustomLexer
import prog8.parser.prog8Parser
import java.io.CharConversionException
import java.io.File
import java.nio.file.Path


/***************** Antlr Extension methods to create AST ****************/

private data class NumericLiteral(val number: Number, val datatype: DataType)

internal fun prog8Parser.ModuleContext.toAst(name: String, isLibrary: Boolean, source: Path) : Module {
    val nameWithoutSuffix = if(name.endsWith(".p8")) name.substringBeforeLast('.') else name
    val directives = this.directive().map { it.toAst() }
    val blocks = this.block().map { it.toAst(isLibrary) }
    return Module(nameWithoutSuffix, (directives + blocks).toMutableList(), toPosition(), isLibrary, source)
}

private fun ParserRuleContext.toPosition() : Position {
    val customTokensource = this.start.tokenSource as? CustomLexer
    val filename =
            when {
                customTokensource!=null -> customTokensource.modulePath.toString()
                start.tokenSource.sourceName == IntStream.UNKNOWN_SOURCE_NAME -> "@internal@"
                else -> File(start.inputStream.sourceName).name
            }
    // note: be ware of TAB characters in the source text, they count as 1 column...
    return Position(filename, start.line, start.charPositionInLine, stop.charPositionInLine + stop.text.length)
}

private fun prog8Parser.BlockContext.toAst(isInLibrary: Boolean) : Statement {
    val blockstatements = block_statement().map {
        when {
            it.variabledeclaration()!=null -> it.variabledeclaration().toAst()
            it.subroutinedeclaration()!=null -> it.subroutinedeclaration().toAst()
            it.directive()!=null -> it.directive().toAst()
            it.inlineasm()!=null -> it.inlineasm().toAst()
            else -> throw FatalAstException("weird block statement $it")
        }
    }
    return Block(identifier().text, integerliteral()?.toAst()?.number?.toInt(), blockstatements.toMutableList(), isInLibrary, toPosition())
}

private fun prog8Parser.Statement_blockContext.toAst(): MutableList<Statement> =
        statement().asSequence().map { it.toAst() }.toMutableList()

private fun prog8Parser.VariabledeclarationContext.toAst() : Statement {
    vardecl()?.let { return it.toAst() }

    varinitializer()?.let {
        val vd = it.vardecl()
        return VarDecl(
                VarDeclType.VAR,
                vd.datatype()?.toAst() ?: DataType.STRUCT,
                if (vd.ZEROPAGE() != null) ZeropageWish.PREFER_ZEROPAGE else ZeropageWish.DONTCARE,
                vd.arrayindex()?.toAst(),
                vd.varname.text,
                null,
                it.expression().toAst(),
                vd.ARRAYSIG() != null || vd.arrayindex() != null,
                false,
                it.toPosition()
        )
    }

    structvarinitializer()?.let {
        val vd = it.structvardecl()
        return VarDecl(
                VarDeclType.VAR,
                DataType.STRUCT,
                ZeropageWish.NOT_IN_ZEROPAGE,
                null,
                vd.varname.text,
                vd.structname.text,
                it.expression().toAst(),
                isArray = false,
                autogeneratedDontRemove = false,
                position = it.toPosition()
        )
    }

    structvardecl()?.let {
        return VarDecl(
                VarDeclType.VAR,
                DataType.STRUCT,
                ZeropageWish.NOT_IN_ZEROPAGE,
                null,
                it.varname.text,
                it.structname.text,
                null,
                isArray = false,
                autogeneratedDontRemove = false,
                position = it.toPosition()
        )
    }

    constdecl()?.let {
        val cvarinit = it.varinitializer()
        val vd = cvarinit.vardecl()
        return VarDecl(
                VarDeclType.CONST,
                vd.datatype()?.toAst() ?: DataType.STRUCT,
                if (vd.ZEROPAGE() != null) ZeropageWish.PREFER_ZEROPAGE else ZeropageWish.DONTCARE,
                vd.arrayindex()?.toAst(),
                vd.varname.text,
                null,
                cvarinit.expression().toAst(),
                vd.ARRAYSIG() != null || vd.arrayindex() != null,
                false,
                cvarinit.toPosition()
        )
    }

    memoryvardecl()?.let {
        val mvarinit = it.varinitializer()
        val vd = mvarinit.vardecl()
        return VarDecl(
                VarDeclType.MEMORY,
                vd.datatype()?.toAst() ?: DataType.STRUCT,
                if (vd.ZEROPAGE() != null) ZeropageWish.PREFER_ZEROPAGE else ZeropageWish.DONTCARE,
                vd.arrayindex()?.toAst(),
                vd.varname.text,
                null,
                mvarinit.expression().toAst(),
                vd.ARRAYSIG() != null || vd.arrayindex() != null,
                false,
                mvarinit.toPosition()
        )
    }

    structdecl()?.let {
        return StructDecl(it.identifier().text,
                it.vardecl().map { vd->vd.toAst() }.toMutableList(),
                toPosition())
    }

    throw FatalAstException("weird variable decl $this")
}

private fun prog8Parser.SubroutinedeclarationContext.toAst() : Subroutine {
    return when {
        subroutine()!=null -> subroutine().toAst()
        asmsubroutine()!=null -> asmsubroutine().toAst()
        romsubroutine()!=null -> romsubroutine().toAst()
        else -> throw FatalAstException("weird subroutine decl $this")
    }
}

private fun prog8Parser.StatementContext.toAst() : Statement {
    val vardecl = variabledeclaration()?.toAst()
    if(vardecl!=null) return vardecl

    assignment()?.let {
        return Assignment(it.assign_target().toAst(), it.expression().toAst(), it.toPosition())
    }

    augassignment()?.let {
        // replace A += X  with  A = A + X
        val target = it.assign_target().toAst()
        val oper = it.operator.text.substringBefore('=')
        val expression = BinaryExpression(target.toExpression(), oper, it.expression().toAst(), it.expression().toPosition())
        return Assignment(it.assign_target().toAst(), expression, it.toPosition())
    }

    postincrdecr()?.let {
        return PostIncrDecr(it.assign_target().toAst(), it.operator.text, it.toPosition())
    }

    val directive = directive()?.toAst()
    if(directive!=null) return directive

    val label = labeldef()?.toAst()
    if(label!=null) return label

    val jump = unconditionaljump()?.toAst()
    if(jump!=null) return jump

    val fcall = functioncall_stmt()?.toAst()
    if(fcall!=null) return fcall

    val ifstmt = if_stmt()?.toAst()
    if(ifstmt!=null) return ifstmt

    val returnstmt = returnstmt()?.toAst()
    if(returnstmt!=null) return returnstmt

    val subroutine = subroutinedeclaration()?.toAst()
    if(subroutine!=null) return subroutine

    val asm = inlineasm()?.toAst()
    if(asm!=null) return asm

    val branchstmt = branch_stmt()?.toAst()
    if(branchstmt!=null) return branchstmt

    val forloop = forloop()?.toAst()
    if(forloop!=null) return forloop

    val untilloop = untilloop()?.toAst()
    if(untilloop!=null) return untilloop

    val whileloop = whileloop()?.toAst()
    if(whileloop!=null) return whileloop

    val repeatloop = repeatloop()?.toAst()
    if(repeatloop!=null) return repeatloop

    val breakstmt = breakstmt()?.toAst()
    if(breakstmt!=null) return breakstmt

    val whenstmt = whenstmt()?.toAst()
    if(whenstmt!=null) return whenstmt

    throw FatalAstException("unprocessed source text (are we missing ast conversion rules for parser elements?): $text")
}

private fun prog8Parser.AsmsubroutineContext.toAst(): Subroutine {
    val subdecl = asmsub_decl().toAst()
    val statements = statement_block()?.toAst() ?: mutableListOf()
    return Subroutine(subdecl.name, subdecl.parameters, subdecl.returntypes,
            subdecl.asmParameterRegisters, subdecl.asmReturnvaluesRegisters,
            subdecl.asmClobbers, null, true, statements, toPosition())
}

private fun prog8Parser.RomsubroutineContext.toAst(): Subroutine {
    val subdecl = asmsub_decl().toAst()
    val address = integerliteral().toAst().number.toInt()
    return Subroutine(subdecl.name, subdecl.parameters, subdecl.returntypes,
            subdecl.asmParameterRegisters, subdecl.asmReturnvaluesRegisters,
            subdecl.asmClobbers, address, true, mutableListOf(), toPosition())
}

private class AsmsubDecl(val name: String,
                         val parameters: List<SubroutineParameter>,
                         val returntypes: List<DataType>,
                         val asmParameterRegisters: List<RegisterOrStatusflag>,
                         val asmReturnvaluesRegisters: List<RegisterOrStatusflag>,
                         val asmClobbers: Set<CpuRegister>)

private fun prog8Parser.Asmsub_declContext.toAst(): AsmsubDecl {
    val name = identifier().text
    val params = asmsub_params()?.toAst() ?: emptyList()
    val returns = asmsub_returns()?.toAst() ?: emptyList()
    val clobbers = asmsub_clobbers()?.clobber()?.toAst() ?: emptySet()
    val normalParameters = params.map { SubroutineParameter(it.name, it.type, it.position) }
    val normalReturntypes = returns.map { it.type }
    val paramRegisters = params.map { RegisterOrStatusflag(it.registerOrPair, it.statusflag, false) }
    val returnRegisters = returns.map { RegisterOrStatusflag(it.registerOrPair, it.statusflag, it.stack) }
    return AsmsubDecl(name, normalParameters, normalReturntypes, paramRegisters, returnRegisters, clobbers)
}

private class AsmSubroutineParameter(name: String,
                                     type: DataType,
                                     val registerOrPair: RegisterOrPair?,
                                     val statusflag: Statusflag?,
                                     // TODO implement: val stack: Boolean,
                                     position: Position) : SubroutineParameter(name, type, position)

private class AsmSubroutineReturn(val type: DataType,
                                  val registerOrPair: RegisterOrPair?,
                                  val statusflag: Statusflag?,
                                  val stack: Boolean,
                                  val position: Position)

private fun prog8Parser.Asmsub_returnsContext.toAst(): List<AsmSubroutineReturn>
        = asmsub_return().map {
            val register = it.identifier()?.toAst()
            var registerorpair: RegisterOrPair? = null
            var statusregister: Statusflag? = null
            if(register!=null) {
                when (val name = register.nameInSource.single()) {
                    in RegisterOrPair.names -> registerorpair = RegisterOrPair.valueOf(name)
                    in Statusflag.names -> statusregister = Statusflag.valueOf(name)
                    else -> throw FatalAstException("invalid register or status flag in $it")
                }
            }
            AsmSubroutineReturn(
                    it.datatype().toAst(),
                    registerorpair,
                    statusregister,
                    !it.stack?.text.isNullOrEmpty(), toPosition())
        }

private fun prog8Parser.Asmsub_paramsContext.toAst(): List<AsmSubroutineParameter>
        = asmsub_param().map {
    val vardecl = it.vardecl()
    val datatype = vardecl.datatype()?.toAst() ?: DataType.STRUCT
    val register = it.identifier()?.toAst()
    var registerorpair: RegisterOrPair? = null
    var statusregister: Statusflag? = null
    if(register!=null) {
        when (val name = register.nameInSource.single()) {
            in RegisterOrPair.names -> registerorpair = RegisterOrPair.valueOf(name)
            in Statusflag.names -> statusregister = Statusflag.valueOf(name)
            else -> throw FatalAstException("invalid register or status flag '$name'")
        }
    }
    AsmSubroutineParameter(vardecl.varname.text, datatype, registerorpair, statusregister, toPosition())
}

private fun prog8Parser.Functioncall_stmtContext.toAst(): Statement {
    val void = this.VOID() != null
    val location = scoped_identifier().toAst()
    return if(expression_list() == null)
        FunctionCallStatement(location, mutableListOf(), void, toPosition())
    else
        FunctionCallStatement(location, expression_list().toAst().toMutableList(), void, toPosition())
}

private fun prog8Parser.FunctioncallContext.toAst(): FunctionCall {
    val location = scoped_identifier().toAst()
    return if(expression_list() == null)
        FunctionCall(location, mutableListOf(), toPosition())
    else
        FunctionCall(location, expression_list().toAst().toMutableList(), toPosition())
}

private fun prog8Parser.InlineasmContext.toAst() =
        InlineAssembly(INLINEASMBLOCK().text, toPosition())

private fun prog8Parser.ReturnstmtContext.toAst() : Return {
    return Return(expression()?.toAst(), toPosition())
}

private fun prog8Parser.UnconditionaljumpContext.toAst(): Jump {
    val address = integerliteral()?.toAst()?.number?.toInt()
    val identifier = scoped_identifier()?.toAst()
    return Jump(address, identifier, null, toPosition())
}

private fun prog8Parser.LabeldefContext.toAst(): Statement =
        Label(children[0].text, toPosition())

private fun prog8Parser.SubroutineContext.toAst() : Subroutine {
    return Subroutine(identifier().text,
            sub_params()?.toAst() ?: emptyList(),
            sub_return_part()?.toAst() ?: emptyList(),
            emptyList(),
            emptyList(),
            emptySet(),
            null,
            false,
            statement_block()?.toAst() ?: mutableListOf(),
            toPosition())
}

private fun prog8Parser.Sub_return_partContext.toAst(): List<DataType> {
    val returns = sub_returns() ?: return emptyList()
    return returns.datatype().map { it.toAst() }
}

private fun prog8Parser.Sub_paramsContext.toAst(): List<SubroutineParameter> =
        vardecl().map {
            val datatype = it.datatype()?.toAst() ?: DataType.STRUCT
            SubroutineParameter(it.varname.text, datatype, it.toPosition())
        }

private fun prog8Parser.Assign_targetContext.toAst() : AssignTarget {
    val identifier = scoped_identifier()
    return when {
        identifier!=null -> AssignTarget(identifier.toAst(), null, null, toPosition())
        arrayindexed()!=null -> AssignTarget(null, arrayindexed().toAst(), null, toPosition())
        directmemory()!=null -> AssignTarget(null, null, DirectMemoryWrite(directmemory().expression().toAst(), toPosition()), toPosition())
        else -> AssignTarget(scoped_identifier()?.toAst(), null, null, toPosition())
    }
}

private fun prog8Parser.ClobberContext.toAst() : Set<CpuRegister> {
    val names = this.identifier().map { it.toAst().nameInSource.single() }
    return names.map { CpuRegister.valueOf(it) }.toSet()
}

private fun prog8Parser.DatatypeContext.toAst() = DataType.valueOf(text.toUpperCase())

private fun prog8Parser.ArrayindexContext.toAst() : ArrayIndex =
        ArrayIndex(expression().toAst(), toPosition())

private fun prog8Parser.DirectiveContext.toAst() : Directive =
        Directive(directivename.text, directivearg().map { it.toAst() }, toPosition())

private fun prog8Parser.DirectiveargContext.toAst() : DirectiveArg {
    val str = stringliteral()
    if(str?.ALT_STRING_ENCODING() != null)
        throw AstException("${toPosition()} can't use alternate string encodings for directive arguments")

    return DirectiveArg(stringliteral()?.text, identifier()?.text, integerliteral()?.toAst()?.number?.toInt(), toPosition())
}

private fun prog8Parser.IntegerliteralContext.toAst(): NumericLiteral {
    fun makeLiteral(text: String, radix: Int): NumericLiteral {
        val integer: Int
        var datatype = DataType.UBYTE
        when (radix) {
            10 -> {
                integer = try {
                    text.toInt()
                } catch(x: NumberFormatException) {
                    throw AstException("${toPosition()} invalid decimal literal ${x.message}")
                }
                datatype = when(integer) {
                    in 0..255 -> DataType.UBYTE
                    in -128..127 -> DataType.BYTE
                    in 0..65535 -> DataType.UWORD
                    in -32768..32767 -> DataType.WORD
                    else -> DataType.FLOAT
                }
            }
            2 -> {
                if(text.length>8)
                    datatype = DataType.UWORD
                try {
                    integer = text.toInt(2)
                } catch(x: NumberFormatException) {
                    throw AstException("${toPosition()} invalid binary literal ${x.message}")
                }
            }
            16 -> {
                if(text.length>2)
                    datatype = DataType.UWORD
                try {
                    integer = text.toInt(16)
                } catch(x: NumberFormatException) {
                    throw AstException("${toPosition()} invalid hexadecimal literal ${x.message}")
                }
            }
            else -> throw FatalAstException("invalid radix")
        }
        return NumericLiteral(integer, datatype)
    }
    val terminal: TerminalNode = children[0] as TerminalNode
    val integerPart = this.intpart.text
    return when (terminal.symbol.type) {
        prog8Parser.DEC_INTEGER -> makeLiteral(integerPart, 10)
        prog8Parser.HEX_INTEGER -> makeLiteral(integerPart.substring(1), 16)
        prog8Parser.BIN_INTEGER -> makeLiteral(integerPart.substring(1), 2)
        else -> throw FatalAstException(terminal.text)
    }
}

private fun prog8Parser.ExpressionContext.toAst() : Expression {

    val litval = literalvalue()
    if(litval!=null) {
        val booleanlit = litval.booleanliteral()?.toAst()
        return if(booleanlit!=null) {
            NumericLiteralValue.fromBoolean(booleanlit, litval.toPosition())
        }
        else {
            val intLit = litval.integerliteral()?.toAst()
            when {
                intLit!=null -> when(intLit.datatype) {
                    DataType.UBYTE -> NumericLiteralValue(DataType.UBYTE, intLit.number.toShort(), litval.toPosition())
                    DataType.BYTE -> NumericLiteralValue(DataType.BYTE, intLit.number.toShort(), litval.toPosition())
                    DataType.UWORD -> NumericLiteralValue(DataType.UWORD, intLit.number.toInt(), litval.toPosition())
                    DataType.WORD -> NumericLiteralValue(DataType.WORD, intLit.number.toInt(), litval.toPosition())
                    DataType.FLOAT -> NumericLiteralValue(DataType.FLOAT, intLit.number.toDouble(), litval.toPosition())
                    else -> throw FatalAstException("invalid datatype for numeric literal")
                }
                litval.floatliteral()!=null -> NumericLiteralValue(DataType.FLOAT, litval.floatliteral().toAst(), litval.toPosition())
                litval.stringliteral()!=null -> litval.stringliteral().toAst()
                litval.charliteral()!=null -> {
                    try {
                        NumericLiteralValue(DataType.UBYTE, CompilationTarget.instance.encodeString(
                                unescape(litval.charliteral().SINGLECHAR().text, litval.toPosition()),
                                litval.charliteral().ALT_STRING_ENCODING()!=null)[0], litval.toPosition())
                    } catch (ce: CharConversionException) {
                        throw SyntaxError(ce.message ?: ce.toString(), litval.toPosition())
                    }
                }
                litval.arrayliteral()!=null -> {
                    val array = litval.arrayliteral().toAst()
                    // the actual type of the arraysize can not yet be determined here (missing namespace & heap)
                    // the ConstantFold takes care of that and converts the type if needed.
                    ArrayLiteralValue(InferredTypes.InferredType.unknown(), array, position = litval.toPosition())
                }
                else -> throw FatalAstException("invalid parsed literal")
            }
        }
    }

    if(scoped_identifier()!=null)
        return scoped_identifier().toAst()

    if(bop!=null)
        return BinaryExpression(left.toAst(), bop.text, right.toAst(), toPosition())

    if(prefix!=null)
        return PrefixExpression(prefix.text, expression(0).toAst(), toPosition())

    val funcall = functioncall()?.toAst()
    if(funcall!=null) return funcall

    if (rangefrom!=null && rangeto!=null) {
        val defaultstep = if(rto.text == "to") 1 else -1
        val step = rangestep?.toAst() ?: NumericLiteralValue(DataType.UBYTE, defaultstep, toPosition())
        return RangeExpr(rangefrom.toAst(), rangeto.toAst(), step, toPosition())
    }

    if(childCount==3 && children[0].text=="(" && children[2].text==")")
        return expression(0).toAst()        // expression within ( )

    if(arrayindexed()!=null)
        return arrayindexed().toAst()

    if(typecast()!=null)
        return TypecastExpression(expression(0).toAst(), typecast().datatype().toAst(), false, toPosition())

    if(directmemory()!=null)
        return DirectMemoryRead(directmemory().expression().toAst(), toPosition())

    if(addressof()!=null)
        return AddressOf(addressof().scoped_identifier().toAst(), toPosition())

    throw FatalAstException(text)
}

private fun prog8Parser.StringliteralContext.toAst(): StringLiteralValue =
    StringLiteralValue(unescape(this.STRING().text, toPosition()), ALT_STRING_ENCODING()!=null, toPosition())

private fun prog8Parser.ArrayindexedContext.toAst(): ArrayIndexedExpression {
    return ArrayIndexedExpression(scoped_identifier().toAst(),
            arrayindex().toAst(),
            toPosition())
}

private fun prog8Parser.Expression_listContext.toAst() = expression().map{ it.toAst() }

private fun prog8Parser.IdentifierContext.toAst() : IdentifierReference =
        IdentifierReference(listOf(text), toPosition())

private fun prog8Parser.Scoped_identifierContext.toAst() : IdentifierReference =
        IdentifierReference(NAME().map { it.text }, toPosition())

private fun prog8Parser.FloatliteralContext.toAst() = text.toDouble()

private fun prog8Parser.BooleanliteralContext.toAst() = when(text) {
    "true" -> true
    "false" -> false
    else -> throw FatalAstException(text)
}

private fun prog8Parser.ArrayliteralContext.toAst() : Array<Expression> =
        expression().map { it.toAst() }.toTypedArray()

private fun prog8Parser.If_stmtContext.toAst(): IfStatement {
    val condition = expression().toAst()
    val trueStatements = statement_block()?.toAst() ?: mutableListOf(statement().toAst())
    val elseStatements = else_part()?.toAst() ?: mutableListOf()
    val trueScope = AnonymousScope(trueStatements, statement_block()?.toPosition()
            ?: statement().toPosition())
    val elseScope = AnonymousScope(elseStatements, else_part()?.toPosition() ?: toPosition())
    return IfStatement(condition, trueScope, elseScope, toPosition())
}

private fun prog8Parser.Else_partContext.toAst(): MutableList<Statement> {
    return statement_block()?.toAst() ?: mutableListOf(statement().toAst())
}

private fun prog8Parser.Branch_stmtContext.toAst(): BranchStatement {
    val branchcondition = branchcondition().toAst()
    val trueStatements = statement_block()?.toAst() ?: mutableListOf(statement().toAst())
    val elseStatements = else_part()?.toAst() ?: mutableListOf()
    val trueScope = AnonymousScope(trueStatements, statement_block()?.toPosition()
            ?: statement().toPosition())
    val elseScope = AnonymousScope(elseStatements, else_part()?.toPosition() ?: toPosition())
    return BranchStatement(branchcondition, trueScope, elseScope, toPosition())
}

private fun prog8Parser.BranchconditionContext.toAst() = BranchCondition.valueOf(text.substringAfter('_').toUpperCase())

private fun prog8Parser.ForloopContext.toAst(): ForLoop {
    val loopvar = identifier().toAst()
    val iterable = expression()!!.toAst()
    val scope =
            if(statement()!=null)
                AnonymousScope(mutableListOf(statement().toAst()), statement().toPosition())
            else
                AnonymousScope(statement_block().toAst(), statement_block().toPosition())
    return ForLoop(loopvar, iterable, scope, toPosition())
}

private fun prog8Parser.BreakstmtContext.toAst() = Break(toPosition())

private fun prog8Parser.WhileloopContext.toAst(): WhileLoop {
    val condition = expression().toAst()
    val statements = statement_block()?.toAst() ?: mutableListOf(statement().toAst())
    val scope = AnonymousScope(statements, statement_block()?.toPosition()
            ?: statement().toPosition())
    return WhileLoop(condition, scope, toPosition())
}

private fun prog8Parser.RepeatloopContext.toAst(): RepeatLoop {
    val iterations = expression()?.toAst()
    val statements = statement_block()?.toAst() ?: mutableListOf(statement().toAst())
    val scope = AnonymousScope(statements, statement_block()?.toPosition()
            ?: statement().toPosition())
    return RepeatLoop(iterations, scope, toPosition())
}

private fun prog8Parser.UntilloopContext.toAst(): UntilLoop {
    val untilCondition = expression().toAst()
    val statements = statement_block()?.toAst() ?: mutableListOf(statement().toAst())
    val scope = AnonymousScope(statements, statement_block()?.toPosition()
            ?: statement().toPosition())
    return UntilLoop(scope, untilCondition, toPosition())
}

private fun prog8Parser.WhenstmtContext.toAst(): WhenStatement {
    val condition = expression().toAst()
    val choices = this.when_choice()?.map { it.toAst() }?.toMutableList() ?: mutableListOf()
    return WhenStatement(condition, choices, toPosition())
}

private fun prog8Parser.When_choiceContext.toAst(): WhenChoice {
    val values = expression_list()?.toAst()
    val stmt = statement()?.toAst()
    val stmtBlock = statement_block()?.toAst()?.toMutableList() ?: mutableListOf()
    if(stmt!=null)
        stmtBlock.add(stmt)
    val scope = AnonymousScope(stmtBlock, toPosition())
    return WhenChoice(values, scope, toPosition())
}

private fun prog8Parser.VardeclContext.toAst(): VarDecl {
    return VarDecl(
            VarDeclType.VAR,
            datatype()?.toAst() ?: DataType.STRUCT,
            if(ZEROPAGE() != null) ZeropageWish.PREFER_ZEROPAGE else ZeropageWish.DONTCARE,
            arrayindex()?.toAst(),
            varname.text,
            null,
            null,
            ARRAYSIG() != null || arrayindex() != null,
            false,
            toPosition()
    )
}

internal fun escape(str: String): String {
    val es = str.map {
        when(it) {
            '\t' -> "\\t"
            '\n' -> "\\n"
            '\r' -> "\\r"
            '"' -> "\\\""
            in '\u8000'..'\u80ff' -> "\\x" + (it.toInt() - 0x8000).toString(16).padStart(2, '0')
            in '\u0000'..'\u00ff' -> it.toString()
            else -> "\\u" + it.toInt().toString(16).padStart(4, '0')
        }
    }
    return es.joinToString("")
}

internal fun unescape(str: String, position: Position): String {
    val result = mutableListOf<Char>()
    val iter = str.iterator()
    while(iter.hasNext()) {
        val c = iter.nextChar()
        if(c=='\\') {
            val ec = iter.nextChar()
            result.add(when(ec) {
                '\\' -> '\\'
                'n' -> '\n'
                'r' -> '\r'
                '"' -> '"'
                '\'' -> '\''
                'u' -> {
                    "${iter.nextChar()}${iter.nextChar()}${iter.nextChar()}${iter.nextChar()}".toInt(16).toChar()
                }
                'x' -> {
                    // special hack 0x8000..0x80ff  will be outputted verbatim without encoding
                    val hex = ("" + iter.nextChar() + iter.nextChar()).toInt(16)
                    (0x8000 + hex).toChar()
                }
                else -> throw SyntaxError("invalid escape char in string: \\$ec", position)
            })
        } else {
            result.add(c)
        }
    }
    return result.joinToString("")
}
