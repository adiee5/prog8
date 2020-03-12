package prog8.compiler.target

import prog8.compiler.CompilationOptions

internal interface IAssemblyGenerator {
    fun compileToAssembly(optimize: Boolean): IAssemblyProgram
}

internal const val generatedLabelPrefix = "_prog8_label_"

internal interface IAssemblyProgram {
    val name: String
    fun assemble(options: CompilationOptions)
}
