package prog8.codegen.target

import prog8.ast.base.ByteDatatypes
import prog8.ast.base.DataType
import prog8.ast.base.PassByReferenceDatatypes
import prog8.ast.base.WordDatatypes
import prog8.ast.expressions.Expression
import prog8.ast.statements.RegisterOrStatusflag
import prog8.ast.statements.Subroutine
import prog8.codegen.target.atari.AtariMachineDefinition
import prog8.codegen.target.cbm.asmsub6502ArgsEvalOrder
import prog8.codegen.target.cbm.asmsub6502ArgsHaveRegisterClobberRisk
import prog8.compilerinterface.Encoding
import prog8.compilerinterface.ICompilationTarget
import prog8.compilerinterface.IMemSizer
import prog8.compilerinterface.IStringEncoding


class AtariTarget: ICompilationTarget, IStringEncoding by Encoder, IMemSizer {
    override val name = NAME
    override val machine = AtariMachineDefinition()
    override val supportedEncodings = setOf(Encoding.ATASCII)
    override val defaultEncoding = Encoding.ATASCII

    companion object {
        const val NAME = "atari"
    }

    override fun asmsubArgsEvalOrder(sub: Subroutine): List<Int> =
        asmsub6502ArgsEvalOrder(sub)
    override fun asmsubArgsHaveRegisterClobberRisk(args: List<Expression>, paramRegisters: List<RegisterOrStatusflag>) =
        asmsub6502ArgsHaveRegisterClobberRisk(args, paramRegisters)

    override fun memorySize(dt: DataType): Int {
        return when(dt) {
            in ByteDatatypes -> 1
            in WordDatatypes, in PassByReferenceDatatypes -> 2
            DataType.FLOAT -> 6
            else -> Int.MIN_VALUE
        }
    }
}