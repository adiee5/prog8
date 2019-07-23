package prog8

import prog8.compiler.compileProgram
import prog8.vm.astvm.AstVm
import java.nio.file.Paths
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    printSoftwareHeader("compiler")

    if (args.isEmpty())
        usage()
    compileMain(args)
}

internal fun printSoftwareHeader(what: String) {
    val buildVersion = object {}.javaClass.getResource("/version.txt").readText().trim()
    println("\nProg8 $what v$buildVersion by Irmen de Jong (irmen@razorvine.net)")
    println("This software is licensed under the GNU GPL 3.0, see https://www.gnu.org/licenses/gpl.html\n")
}


private fun compileMain(args: Array<String>) {
    var emulatorToStart = ""
    var moduleFile = ""
    var writeAssembly = true
    var optimize = true
    var optimizeInlining = true
    var launchAstVm = false
    for (arg in args) {
        if(arg=="-emu")
            emulatorToStart = "x64"
        else if(arg=="-emu2")
            emulatorToStart = "x64sc"
        else if(arg=="-noasm")
            writeAssembly = false
        else if(arg=="-noopt")
            optimize = false
        else if(arg=="-nooptinline")
            optimizeInlining = false
        else if(arg=="-avm")
            launchAstVm = true
        else if(!arg.startsWith("-"))
            moduleFile = arg
        else
            usage()
    }
    if(moduleFile.isBlank())
        usage()

    val filepath = Paths.get(moduleFile).normalize()

    val (programAst, programName) = compileProgram(filepath, optimize, optimizeInlining, writeAssembly)

    if(launchAstVm) {
        println("\nLaunching AST-based vm...")
        val vm = AstVm(programAst)
        vm.run()
    }

    if(emulatorToStart.isNotEmpty()) {
        if(programName==null)
            println("\nCan't start emulator because no program was assembled.")
        else {
            println("\nStarting C-64 emulator $emulatorToStart...")
            val cmdline = listOf(emulatorToStart, "-silent", "-moncommands", "$programName.vice-mon-list",
                    "-autostartprgmode", "1", "-autostart-warp", "-autostart", programName + ".prg")
            val process = ProcessBuilder(cmdline).inheritIO().start()
            process.waitFor()
        }
    }
}


private fun usage() {
    System.err.println("Missing argument(s):")
    System.err.println("    [-emu]          auto-start the 'x64' C-64 emulator after successful compilation")
    System.err.println("    [-emu2]         auto-start the 'x64sc' C-64 emulator after successful compilation")
    System.err.println("    [-noasm]        don't create assembly code")
    System.err.println("    [-avm]          launch the prog8 ast-based virtual machine after compilation")
    System.err.println("    [-noopt]        don't perform any optimizations")
    System.err.println("    [-nooptinline]  don't perform subroutine inlining optimizations")
    System.err.println("    modulefile      main module file to compile")
    exitProcess(1)
}
