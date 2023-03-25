package prog8.codegen.experimental

import prog8.code.SymbolTable
import prog8.code.ast.PtProgram
import prog8.code.core.*
import prog8.codegen.intermediate.IRCodeGen
import prog8.intermediate.IRFileWriter

class ExperiCodeGen: ICodeGeneratorBackend {
    override fun generate(
        program: PtProgram,
        symbolTable: SymbolTable,
        options: CompilationOptions,
        errors: IErrorReporter
    ): IAssemblyProgram? {

        if(options.useNewExprCode) {
            // TODO("transform BinExprs?")
            // errors.warn("EXPERIMENTAL NEW EXPRESSION CODEGEN IS USED. CODE SIZE+SPEED POSSIBLY SUFFERS.", Position.DUMMY)
        }

        // you could write a code generator directly on the PtProgram AST,
        // but you can also use the Intermediate Representation to build a codegen on:
        val irCodeGen = IRCodeGen(program, symbolTable, options, errors)
        val irProgram = irCodeGen.generate()

        // this stub only writes the IR program to disk but doesn't generate anything else.
        IRFileWriter(irProgram, null).write()

        println("** experimental codegen stub: no assembly generated **")
        return null
    }
}