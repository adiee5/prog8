package prog8.codegen.cpu6502

import prog8.code.*
import prog8.code.ast.*
import prog8.code.core.*
import prog8.codegen.cpu6502.assignment.AsmAssignTarget
import prog8.codegen.cpu6502.assignment.TargetStorageKind
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.absoluteValue

/**
 * Generates the main parts of the program:
 *  - entry/exit code
 *  - initialization routines
 *  - blocks
 *  - subroutines
 *  - all variables (note: VarDecl ast nodes are *NOT* used anymore for this! now uses IVariablesAndConsts data tables!)
 */
internal class ProgramAndVarsGen(
    val program: PtProgram,
    val options: CompilationOptions,
    val errors: IErrorReporter,
    private val symboltable: SymbolTable,
    private val functioncallAsmGen: FunctionCallAsmGen,
    private val asmgen: AsmGen6502Internal,
    private val allocator: VariableAllocator,
    private val zeropage: Zeropage
) {
    private val compTarget = options.compTarget
    private val blockVariableInitializers = program.allBlocks().associateWith { it.children.filterIsInstance<PtAssignment>() }

    internal fun generate() {
        header()

        if(errors.noErrors())  {
            program.allBlocks().forEach { block2asm(it) }

            // the global list of all floating point constants for the whole program
            asmgen.out("; global float constants")
            for (flt in allocator.globalFloatConsts) {
                val floatFill = compTarget.machine.getFloatAsmBytes(flt.key)
                val floatvalue = flt.key
                asmgen.out("${flt.value}\t.byte  $floatFill  ; float $floatvalue")
            }

            memorySlabs()
            tempVars()
            footer()
        }
    }

    private fun header() {
        val ourName = this.javaClass.name
        val cpu = when(compTarget.machine.cpu) {
            CpuType.CPU6502 -> "6502"
            CpuType.CPU65c02 -> "w65c02"
            else -> "unsupported"
        }

        asmgen.out("; $cpu assembly code for '${program.name}'")
        asmgen.out("; generated by $ourName on ${LocalDateTime.now().withNano(0)}")
        asmgen.out("; assembler syntax is for the 64tasm cross-assembler")
        asmgen.out("; output options: output=${options.output} launcher=${options.launcher} zp=${options.zeropage}")
        asmgen.out("")
        asmgen.out(".cpu  '$cpu'\n.enc  'none'")

        // the global prog8 variables needed
        val zp = zeropage
        asmgen.out("P8ZP_SCRATCH_B1 = ${zp.SCRATCH_B1}")
        asmgen.out("P8ZP_SCRATCH_REG = ${zp.SCRATCH_REG}")
        asmgen.out("P8ZP_SCRATCH_W1 = ${zp.SCRATCH_W1}    ; word")
        asmgen.out("P8ZP_SCRATCH_W2 = ${zp.SCRATCH_W2}    ; word")
        if(compTarget.name=="c64") {
            if(options.floats)
                asmgen.out("PROG8_C64_BANK_CONFIG=31  ; basic+IO+kernal")
            else
                asmgen.out("PROG8_C64_BANK_CONFIG=30  ; IO+kernal, no basic")
        }
        asmgen.out(".weak")   // hack to allow user to override the following two with command line redefinition (however, just use '-esa' command line option instead!)
        asmgen.out(".endweak")

        if(options.symbolDefs.isNotEmpty()) {
            asmgen.out("; -- user supplied symbols on the command line")
            for((name, value) in options.symbolDefs) {
                asmgen.out("$name = $value")
            }
        }

        when(options.output) {
            OutputType.RAW -> {
                asmgen.out("; ---- raw assembler program ----")
                asmgen.out("* = ${options.loadAddress.toHex()}")
                asmgen.out("prog8_program_start\t; start of program label")
                asmgen.out("  cld")
                asmgen.out("  tsx  ; save stackpointer for sys.exit()")
                asmgen.out("  stx  prog8_lib.orig_stackpointer")
                if(!options.noSysInit)
                    asmgen.out("  jsr  p8_sys_startup.init_system")
                asmgen.out("  jsr  p8_sys_startup.init_system_phase2")
            }
            OutputType.PRG -> {
                when(options.launcher) {
                    CbmPrgLauncherType.BASIC -> {
                        if (options.loadAddress != options.compTarget.machine.PROGRAM_LOAD_ADDRESS) {
                            errors.err("BASIC output must have load address ${options.compTarget.machine.PROGRAM_LOAD_ADDRESS.toHex()}", program.position)
                        }
                        asmgen.out("; ---- basic program with sys call ----")
                        asmgen.out("* = ${options.loadAddress.toHex()}")
                        asmgen.out("prog8_program_start\t; start of program label")
                        val year = LocalDate.now().year
                        asmgen.out("  .word  (+), $year")
                        asmgen.out("  .null  $9e, format(' %d ', prog8_entrypoint), $3a, $8f, ' prog8'")
                        asmgen.out("+\t.word  0")
                        asmgen.out("prog8_entrypoint")
                        asmgen.out("  cld")
                        asmgen.out("  tsx  ; save stackpointer for sys.exit()")
                        asmgen.out("  stx  prog8_lib.orig_stackpointer")
                        if(!options.noSysInit)
                            asmgen.out("  jsr  p8_sys_startup.init_system")
                        asmgen.out("  jsr  p8_sys_startup.init_system_phase2")
                    }
                    CbmPrgLauncherType.NONE -> {
                        // this is the same as RAW
                        asmgen.out("; ---- program without basic sys call ----")
                        asmgen.out("* = ${options.loadAddress.toHex()}")
                        asmgen.out("prog8_program_start\t; start of program label")
                        asmgen.out("  cld")
                        asmgen.out("  tsx  ; save stackpointer for sys.exit()")
                        asmgen.out("  stx  prog8_lib.orig_stackpointer")
                        if(!options.noSysInit)
                            asmgen.out("  jsr  p8_sys_startup.init_system")
                        asmgen.out("  jsr  p8_sys_startup.init_system_phase2")
                    }
                }
            }
            OutputType.XEX -> {
                asmgen.out("; ---- atari xex program ----")
                asmgen.out("* = ${options.loadAddress.toHex()}")
                asmgen.out("prog8_program_start\t; start of program label")
                asmgen.out("  cld")
                asmgen.out("  tsx  ; save stackpointer for sys.exit()")
                asmgen.out("  stx  prog8_lib.orig_stackpointer")
                if(!options.noSysInit)
                    asmgen.out("  jsr  p8_sys_startup.init_system")
                asmgen.out("  jsr  p8_sys_startup.init_system_phase2")
            }
        }

        if(options.zeropage !in arrayOf(ZeropageType.BASICSAFE, ZeropageType.DONTUSE)) {
            asmgen.out("""
                ; zeropage is clobbered so we need to reset the machine at exit
                lda  #>sys.reset_system
                pha
                lda  #<sys.reset_system
                pha""")
        }

        when(compTarget.name) {
            "cx16" -> {
                if(options.floats)
                    asmgen.out("  lda  #4 |  sta  $01")    // to use floats, make sure Basic rom is banked in
                asmgen.out("  jsr  p8b_main.p8s_start")
                asmgen.out("  jmp  p8_sys_startup.cleanup_at_exit")
            }
            "c64" -> {
                asmgen.out("  jsr  p8b_main.p8s_start")
                asmgen.out("  jmp  p8_sys_startup.cleanup_at_exit")
            }
            "c128" -> {
                asmgen.out("  jsr  p8b_main.p8s_start")
                asmgen.out("  jmp  p8_sys_startup.cleanup_at_exit")
            }
            else -> {
                asmgen.out("  jsr  p8b_main.p8s_start")
                asmgen.out("  jmp  p8_sys_startup.cleanup_at_exit")
            }
        }
    }

    private fun memorySlabs() {
        if(symboltable.allMemorySlabs.isNotEmpty()) {
            asmgen.out("; memory slabs\n  .section slabs_BSS")
            asmgen.out("prog8_slabs\t.block")
            for (slab in symboltable.allMemorySlabs) {
                if (slab.align > 1u)
                    asmgen.out("\t.align  ${slab.align.toHex()}")
                asmgen.out("${slab.name}\t.fill  ${slab.size}")
            }
            asmgen.out("\t.bend\n  .send slabs_BSS")
        }
    }

    private fun tempVars() {
        asmgen.out("; expression temp vars\n  .section BSS")
        for((dt, count) in asmgen.tempVarsCounters) {
            if(count>0) {
                for(num in 1..count) {
                    val name = asmgen.buildTempVarName(dt, num)
                    when (dt) {
                        DataType.BOOL  -> asmgen.out("$name    .byte  ?")
                        DataType.BYTE  -> asmgen.out("$name    .char  ?")
                        DataType.UBYTE -> asmgen.out("$name    .byte  ?")
                        DataType.WORD  -> asmgen.out("$name    .sint  ?")
                        DataType.UWORD -> asmgen.out("$name    .word  ?")
                        DataType.FLOAT -> asmgen.out("$name    .fill  ${options.compTarget.machine.FLOAT_MEM_SIZE}")
                        else -> throw AssemblyError("weird dt for extravar $dt")
                    }
                }
            }
        }
        asmgen.out("  .send BSS")
    }

    private fun footer() {
        var relocateBssVars = false
        var relocateBssSlabs = false
        var relocatedBssStart = 0u
        var relocatedBssEnd = 0u

        if(options.varsGolden) {
            if(options.compTarget.machine.BSSGOLDENRAM_START == 0u ||
                options.compTarget.machine.BSSGOLDENRAM_END == 0u ||
                options.compTarget.machine.BSSGOLDENRAM_END <= options.compTarget.machine.BSSGOLDENRAM_START) {
                throw AssemblyError("current compilation target hasn't got the golden ram area properly defined or it is simply not available")
            }
            relocateBssVars = true
            relocatedBssStart = options.compTarget.machine.BSSGOLDENRAM_START
            relocatedBssEnd = options.compTarget.machine.BSSGOLDENRAM_END
        }
        else if(options.varsHighBank!=null) {
            if(options.compTarget.machine.BSSHIGHRAM_START == 0u ||
                options.compTarget.machine.BSSHIGHRAM_END == 0u ||
                options.compTarget.machine.BSSHIGHRAM_END <= options.compTarget.machine.BSSHIGHRAM_START) {
                throw AssemblyError("current compilation target hasn't got the high ram area properly defined or it is simply not available")
            }
            if(options.slabsHighBank!=null && options.varsHighBank!=options.slabsHighBank)
                throw AssemblyError("slabs and vars high bank must be the same")
            relocateBssVars = true
            relocatedBssStart = options.compTarget.machine.BSSHIGHRAM_START
            relocatedBssEnd = options.compTarget.machine.BSSHIGHRAM_END
        }

        if(options.slabsGolden) {
            if(options.compTarget.machine.BSSGOLDENRAM_START == 0u ||
                options.compTarget.machine.BSSGOLDENRAM_END == 0u ||
                options.compTarget.machine.BSSGOLDENRAM_END <= options.compTarget.machine.BSSGOLDENRAM_START) {
                throw AssemblyError("current compilation target hasn't got the golden ram area properly defined or it is simply not available")
            }
            relocateBssSlabs = true
            relocatedBssStart = options.compTarget.machine.BSSGOLDENRAM_START
            relocatedBssEnd = options.compTarget.machine.BSSGOLDENRAM_END
        }
        else if(options.slabsHighBank!=null) {
            if(options.compTarget.machine.BSSHIGHRAM_START == 0u ||
                options.compTarget.machine.BSSHIGHRAM_END == 0u ||
                options.compTarget.machine.BSSHIGHRAM_END <= options.compTarget.machine.BSSHIGHRAM_START) {
                throw AssemblyError("current compilation target hasn't got the high ram area properly defined or it is simply not available")
            }
            if(options.varsHighBank!=null && options.varsHighBank!=options.slabsHighBank)
                throw AssemblyError("slabs and vars high bank must be the same")
            relocateBssSlabs = true
            relocatedBssStart = options.compTarget.machine.BSSHIGHRAM_START
            relocatedBssEnd = options.compTarget.machine.BSSHIGHRAM_END
        }

        asmgen.out("; bss sections")
        asmgen.out("PROG8_VARSHIGH_RAMBANK = ${options.varsHighBank ?: 1}")
        if(relocateBssVars) {
            if(!relocateBssSlabs)
                asmgen.out("  .dsection slabs_BSS")
            asmgen.out("prog8_program_end\t; end of program label for progend()")
            asmgen.out("  * = ${relocatedBssStart.toHex()}")
            asmgen.out("prog8_bss_section_start")
            asmgen.out("  .dsection BSS")
            if(relocateBssSlabs)
                asmgen.out("  .dsection slabs_BSS")
            asmgen.out("  .cerror * > ${relocatedBssEnd.toHex()}, \"too many variables/data for BSS section\"")
            asmgen.out("prog8_bss_section_size = * - prog8_bss_section_start")
        } else {
            asmgen.out("prog8_bss_section_start")
            asmgen.out("  .dsection BSS")
            asmgen.out("prog8_bss_section_size = * - prog8_bss_section_start")
            if(!relocateBssSlabs)
                asmgen.out("  .dsection slabs_BSS")
            asmgen.out("prog8_program_end\t; end of program label for progend()")
            if(relocateBssSlabs) {
                asmgen.out("  * = ${relocatedBssStart.toHex()}")
                asmgen.out("  .dsection slabs_BSS")
                asmgen.out("  .cerror * > ${relocatedBssEnd.toHex()}, \"too many data for slabs_BSS section\"")
            }
        }
        asmgen.out("  ; memtop check")
        asmgen.out("  .cerror * >= ${options.memtopAddress.toHex()}, \"Program too long by \", * - ${(options.memtopAddress-1u).toHex()}, \" bytes, memtop=${options.memtopAddress.toHex()}\"")
    }

    private fun block2asm(block: PtBlock) {
        asmgen.out("")
        asmgen.out("; ---- block: '${block.name}' ----")
        if(block.options.address!=null)
            asmgen.out("* = ${block.options.address!!.toHex()}")

        asmgen.out("${block.name}\t" + (if(block.options.forceOutput) ".block" else ".proc"))
        asmgen.outputSourceLine(block)

        createBlockVariables(block)
        asmsubs2asm(block.children)

        asmgen.out("")

        val initializers = blockVariableInitializers.getValue(block)
        val notInitializers = block.children.filterNot { it in initializers }
        notInitializers.forEach { asmgen.translate(it) }

        // generate subroutine to initialize block-level (global) variables
        if (initializers.isNotEmpty()) {
            asmgen.out("prog8_init_vars\t.block")
            initializers.forEach { assign ->
                if((assign.value as? PtNumber)?.number != 0.0 || allocator.isZpVar(assign.target.identifier!!.name))
                    asmgen.translate(assign)
                // the other variables that should be set to zero are done so as part of the BSS section.
            }
            asmgen.out("  rts\n  .bend")
        }

        asmgen.out(if(block.options.forceOutput) "\n\t.bend" else "\n\t.pend")
    }

    private fun getVars(scope: StNode): Map<String, StNode> =
        scope.children.filter { it.value.type in arrayOf(StNodeType.STATICVAR, StNodeType.CONSTANT, StNodeType.MEMVAR) }

    private fun createBlockVariables(block: PtBlock) {
        val scope = symboltable.lookupUnscopedOrElse(block.name) { throw AssemblyError("lookup") }
        require(scope.type==StNodeType.BLOCK)
        val varsInBlock = getVars(scope)

        // Zeropage Variables
        val varnames = varsInBlock.filter { it.value.type==StNodeType.STATICVAR }.map { it.value.scopedName }.toSet()
        zeropagevars2asm(varnames)

        // MemDefs and Consts
        val mvs = varsInBlock
            .filter { it.value.type==StNodeType.MEMVAR }
            .map { it.value as StMemVar }
        val consts = varsInBlock
            .filter { it.value.type==StNodeType.CONSTANT }
            .map { it.value as StConstant }
        memdefsAndConsts2asm(mvs, consts)

        // normal statically allocated variables
        val variables = varsInBlock
            .filter { it.value.type==StNodeType.STATICVAR && !allocator.isZpVar(it.value.scopedName) }
            .map { it.value as StStaticVariable }
        nonZpVariables2asm(variables)
    }

    internal fun translateAsmSubroutine(sub: PtAsmSub) {
        if(sub.inline) {
            return      // subroutine gets inlined at call site.
        }

        asmgen.out("")

        val asmStartScope: String
        val asmEndScope: String
        if(sub.definingBlock()!!.options.forceOutput) {
            asmStartScope = ".block"
            asmEndScope = ".bend"
        } else {
            asmStartScope = ".proc"
            asmEndScope = ".pend"
        }

        if(sub.address!=null)
            return  // already done at the memvars section

        // asmsub with most likely just an inline asm in it
        asmgen.out("${sub.name}\t$asmStartScope")
        sub.children.forEach { asmgen.translate(it) }
        asmgen.out("  $asmEndScope")
    }


    internal fun translateSubroutine(sub: PtSub) {
        asmgen.out("")

        val asmStartScope: String
        val asmEndScope: String
        if(sub.definingBlock()!!.options.forceOutput) {
            asmStartScope = ".block"
            asmEndScope = ".bend"
        } else {
            asmStartScope = ".proc"
            asmEndScope = ".pend"
        }

        asmgen.out("${sub.name}\t$asmStartScope")

        val scope = symboltable.lookupOrElse(sub.scopedName) {
            throw AssemblyError("lookup ${sub.scopedName}")
        }
        require(scope.type==StNodeType.SUBROUTINE)
        val varsInSubroutine = getVars(scope)

        // Zeropage Variables
        val varnames = varsInSubroutine.filter { it.value.type==StNodeType.STATICVAR }.map { it.value.scopedName }.toSet()
        zeropagevars2asm(varnames)

        // MemDefs and Consts
        val mvs = varsInSubroutine
            .filter { it.value.type==StNodeType.MEMVAR }
            .map { it.value as StMemVar }
        val consts = varsInSubroutine
            .filter { it.value.type==StNodeType.CONSTANT }
            .map { it.value as StConstant }
        memdefsAndConsts2asm(mvs, consts)

        asmsubs2asm(sub.children)

        // the main.start subroutine is the program's entrypoint and should perform some initialization logic
        if((sub.name=="start" || sub.name=="p8s_start") && (sub.definingBlock()!!.name=="main" || sub.definingBlock()!!.name=="p8b_main"))
            entrypointInitialization()

        if(functioncallAsmGen.optimizeIntArgsViaRegisters(sub)) {
            asmgen.out("; simple int arg(s) passed via register(s)")
            if(sub.parameters.size==1) {
                val dt = sub.parameters[0].type
                val target = AsmAssignTarget(TargetStorageKind.VARIABLE, asmgen, dt, sub, sub.parameters[0].position, variableAsmName = sub.parameters[0].name)
                if(dt in ByteDatatypesWithBoolean)
                    asmgen.assignRegister(RegisterOrPair.A, target)
                else
                    asmgen.assignRegister(RegisterOrPair.AY, target)
            } else {
                require(sub.parameters.size==2)
                // 2 simple byte args, first in A, second in Y
                val target1 = AsmAssignTarget(TargetStorageKind.VARIABLE, asmgen, sub.parameters[0].type, sub, sub.parameters[0].position, variableAsmName = sub.parameters[0].name)
                val target2 = AsmAssignTarget(TargetStorageKind.VARIABLE, asmgen, sub.parameters[1].type, sub, sub.parameters[1].position, variableAsmName = sub.parameters[1].name)
                asmgen.assignRegister(RegisterOrPair.A, target1)
                asmgen.assignRegister(RegisterOrPair.Y, target2)
            }
        }

        asmgen.out("; statements")
        sub.children.forEach { asmgen.translate(it) }

        asmgen.out("; variables")
        asmgen.out("    .section BSS")
        val asmGenInfo = asmgen.subroutineExtra(sub)
        for((dt, name, addr) in asmGenInfo.extraVars) {
            if(addr!=null)
                asmgen.out("$name = $addr")
            else when(dt) {
                DataType.UBYTE -> asmgen.out("$name    .byte  ?")
                DataType.UWORD -> asmgen.out("$name    .word  ?")
                DataType.FLOAT -> asmgen.out("$name    .fill  ${options.compTarget.machine.FLOAT_MEM_SIZE}")
                else -> throw AssemblyError("weird dt for extravar $dt")
            }
        }
        if(asmGenInfo.usedFloatEvalResultVar1)
            asmgen.out("$subroutineFloatEvalResultVar1    .fill  ${options.compTarget.machine.FLOAT_MEM_SIZE}")
        if(asmGenInfo.usedFloatEvalResultVar2)
            asmgen.out("$subroutineFloatEvalResultVar2    .fill  ${options.compTarget.machine.FLOAT_MEM_SIZE}")
        asmgen.out("  .send BSS")

        // normal statically allocated variables
        val variables = varsInSubroutine
            .filter { it.value.type==StNodeType.STATICVAR && !allocator.isZpVar(it.value.scopedName) }
            .map { it.value as StStaticVariable }
        nonZpVariables2asm(variables)

        asmgen.out("  $asmEndScope")
    }

    private fun entrypointInitialization() {
        // zero out the BSS area first, before setting the variable init values
        // this is mainly to make sure the arrays are all zero'd out at program startup
        asmgen.out("  jsr  prog8_lib.program_startup_clear_bss")

        // initialize block-level (global) variables at program start
        blockVariableInitializers.forEach {
            if (it.value.isNotEmpty())
                asmgen.out("  jsr  ${it.key.name}.prog8_init_vars")
        }

        // string and array variables in zeropage that have initializer value, should be initialized
        val stringVarsWithInitInZp = getZpStringVarsWithInitvalue()
        val arrayVarsWithInitInZp = getZpArrayVarsWithInitvalue()
        if(stringVarsWithInitInZp.isNotEmpty() || arrayVarsWithInitInZp.isNotEmpty()) {
            asmgen.out("; zp str and array initializations")
            stringVarsWithInitInZp.forEach {
                val name = asmgen.asmVariableName(it.name)
                asmgen.out("""
                    lda  #<${name}
                    ldy  #>${name}
                    sta  P8ZP_SCRATCH_W1
                    sty  P8ZP_SCRATCH_W1+1
                    lda  #<${name}_init_value
                    ldy  #>${name}_init_value
                    jsr  prog8_lib.strcpy""")
            }
            arrayVarsWithInitInZp.forEach {
                val size = it.alloc.size
                val name = asmgen.asmVariableName(it.name)
                asmgen.out("""
                    lda  #<${name}_init_value
                    ldy  #>${name}_init_value
                    sta  cx16.r0
                    sty  cx16.r0+1
                    lda  #<${name}
                    ldy  #>${name}
                    sta  cx16.r1
                    sty  cx16.r1+1
                    lda  #<$size
                    ldy  #>$size
                    jsr  sys.memcopy""")
            }
            asmgen.out("  jmp  +")
        }

        stringVarsWithInitInZp.forEach {
            val varname = asmgen.asmVariableName(it.name)+"_init_value"
            outputStringvar(varname, 0, it.value.second, it.value.first)
        }

        arrayVarsWithInitInZp.forEach {
            val varname = asmgen.asmVariableName(it.name)+"_init_value"
            arrayVariable2asm(varname, it.alloc.dt, 0, it.value, null)
        }

        asmgen.out("+")
    }

    private class ZpStringWithInitial(
        val name: String,
        val alloc: MemoryAllocator.VarAllocation,
        val value: Pair<String, Encoding>
    )

    private class ZpArrayWithInitial(
        val name: String,
        val alloc: MemoryAllocator.VarAllocation,
        val value: StArray
    )

    private fun getZpStringVarsWithInitvalue(): Collection<ZpStringWithInitial> {
        val result = mutableListOf<ZpStringWithInitial>()
        val vars = allocator.zeropageVars.filter { it.value.dt==DataType.STR }
        for (variable in vars) {
            val scopedName = variable.key
            val svar = symboltable.lookup(scopedName) as? StStaticVariable
            if(svar?.initializationStringValue!=null)
                result.add(ZpStringWithInitial(scopedName, variable.value, svar.initializationStringValue!!))
        }
        return result
    }

    private fun getZpArrayVarsWithInitvalue(): Collection<ZpArrayWithInitial> {
        val result = mutableListOf<ZpArrayWithInitial>()
        val vars = allocator.zeropageVars.filter { it.value.dt in ArrayDatatypes }
        for (variable in vars) {
            val scopedName = variable.key
            val svar = symboltable.lookup(scopedName) as? StStaticVariable
            if(svar?.initializationArrayValue!=null)
                result.add(ZpArrayWithInitial(scopedName, variable.value, svar.initializationArrayValue!!))
        }
        return result
    }

    private fun zeropagevars2asm(varNames: Set<String>) {
        val zpVariables = allocator.zeropageVars.filter { it.key in varNames }.toList().sortedBy { it.second.address }
        for ((scopedName, zpvar) in zpVariables) {
            if (scopedName.startsWith("cx16.r"))
                continue        // The 16 virtual registers of the cx16 are not actual variables in zp, they're memory mapped
            val variable = symboltable.flat.getValue(scopedName) as StStaticVariable
            if(variable.dt in SplitWordArrayTypes) {
                val lsbAddr = zpvar.address
                val msbAddr = zpvar.address + (zpvar.size/2).toUInt()
                asmgen.out("${scopedName.substringAfterLast('.')}_lsb \t= $lsbAddr \t; zp ${zpvar.dt} (lsbs)")
                asmgen.out("${scopedName.substringAfterLast('.')}_msb \t= $msbAddr \t; zp ${zpvar.dt} (msbs)")
            } else {
                asmgen.out("${scopedName.substringAfterLast('.')} \t= ${zpvar.address} \t; zp ${zpvar.dt}")
            }
        }
    }

    private fun nonZpVariables2asm(variables: List<StStaticVariable>) {
        asmgen.out("")
        val (varsNoInit, varsWithInit) = variables.partition { it.uninitialized }
        if(varsNoInit.isNotEmpty()) {
            asmgen.out("; non-zeropage variables")
            asmgen.out("  .section BSS")
            val (notAligned, aligned) = varsNoInit.partition { it.align==0 }
            notAligned.sortedWith(compareBy<StStaticVariable> { it.name }.thenBy { it.dt }).forEach {
                uninitializedVariable2asm(it)
            }
            aligned.sortedWith(compareBy<StStaticVariable> { it.align }.thenBy { it.name }.thenBy { it.dt }).forEach {
                uninitializedVariable2asm(it)
            }
            asmgen.out("  .send BSS")
        }

        if(varsWithInit.isNotEmpty()) {
            asmgen.out("; non-zeropage variables with init value")
            val (stringvars, othervars) = varsWithInit.sortedBy { it.name }.partition { it.dt == DataType.STR }
            val (notAlignedStrings, alignedStrings) = stringvars.partition { it.align==0 }
            val (notAlignedOther, alignedOther) = othervars.partition { it.align==0 }
            notAlignedStrings.forEach {
                outputStringvar(
                    it.name,
                    it.align,
                    it.initializationStringValue!!.second,
                    it.initializationStringValue!!.first
                )
            }
            alignedStrings.sortedBy { it.align }.forEach {
                outputStringvar(
                    it.name,
                    it.align,
                    it.initializationStringValue!!.second,
                    it.initializationStringValue!!.first
                )
            }

            notAlignedOther.sortedBy { it.type }.forEach {
                staticVariable2asm(it)
            }
            alignedOther.sortedBy { it.align }.sortedBy { it.type }.forEach {
                staticVariable2asm(it)
            }
        }
    }

    private fun uninitializedVariable2asm(variable: StStaticVariable) {
        when (variable.dt) {
            DataType.BOOL, DataType.UBYTE -> asmgen.out("${variable.name}\t.byte  ?")
            DataType.BYTE -> asmgen.out("${variable.name}\t.char  ?")
            DataType.UWORD -> asmgen.out("${variable.name}\t.word  ?")
            DataType.WORD -> asmgen.out("${variable.name}\t.sint  ?")
            DataType.FLOAT -> asmgen.out("${variable.name}\t.fill  ${compTarget.machine.FLOAT_MEM_SIZE}")
            in SplitWordArrayTypes -> {
                alignVar(variable.align)
                val numbytesPerHalf = compTarget.memorySize(variable.dt, variable.length!!) / 2
                asmgen.out("${variable.name}_lsb\t.fill  $numbytesPerHalf")
                asmgen.out("${variable.name}_msb\t.fill  $numbytesPerHalf")
            }
            in ArrayDatatypes -> {
                alignVar(variable.align)
                val numbytes = compTarget.memorySize(variable.dt, variable.length!!)
                asmgen.out("${variable.name}\t.fill  $numbytes")
            }
            else -> {
                throw AssemblyError("weird dt")
            }
        }
    }

    private fun alignVar(align: Int) {
        if(align > 1)
            asmgen.out("  .align  ${align.toHex()}")
    }

    private fun staticVariable2asm(variable: StStaticVariable) {
        val initialValue: Number =
            if(variable.initializationNumericValue!=null) {
                if(variable.dt== DataType.FLOAT)
                    variable.initializationNumericValue!!
                else
                    variable.initializationNumericValue!!.toInt()
            } else 0

        when (variable.dt) {
            DataType.BOOL, DataType.UBYTE -> asmgen.out("${variable.name}\t.byte  ${initialValue.toHex()}")
            DataType.BYTE -> asmgen.out("${variable.name}\t.char  $initialValue")
            DataType.UWORD -> asmgen.out("${variable.name}\t.word  ${initialValue.toHex()}")
            DataType.WORD -> asmgen.out("${variable.name}\t.sint  $initialValue")
            DataType.FLOAT -> {
                if(initialValue==0) {
                    asmgen.out("${variable.name}\t.byte  0,0,0,0,0  ; float")
                } else {
                    val floatFill = compTarget.machine.getFloatAsmBytes(initialValue)
                    asmgen.out("${variable.name}\t.byte  $floatFill  ; float $initialValue")
                }
            }
            DataType.STR -> {
                throw AssemblyError("all string vars should have been interned into prog")
            }
            in ArrayDatatypes -> {
                arrayVariable2asm(variable.name, variable.dt, variable.align, variable.initializationArrayValue, variable.length)
            }
            else -> {
                throw AssemblyError("weird dt")
            }
        }
    }

    private fun arrayVariable2asm(varname: String, dt: DataType, align: Int, value: StArray?, orNumberOfZeros: Int?) {
        alignVar(align)
        when(dt) {
            DataType.ARRAY_UB, DataType.ARRAY_BOOL -> {
                val data = makeArrayFillDataUnsigned(dt, value, orNumberOfZeros)
                if (data.size <= 16)
                    asmgen.out("$varname\t.byte  ${data.joinToString()}")
                else {
                    asmgen.out(varname)
                    for (chunk in data.chunked(16))
                        asmgen.out("  .byte  " + chunk.joinToString())
                }
            }
            DataType.ARRAY_B -> {
                val data = makeArrayFillDataSigned(dt, value, orNumberOfZeros)
                if (data.size <= 16)
                    asmgen.out("$varname\t.char  ${data.joinToString()}")
                else {
                    asmgen.out(varname)
                    for (chunk in data.chunked(16))
                        asmgen.out("  .char  " + chunk.joinToString())
                }
            }
            DataType.ARRAY_UW -> {
                val data = makeArrayFillDataUnsigned(dt, value, orNumberOfZeros)
                if (data.size <= 16)
                    asmgen.out("$varname\t.word  ${data.joinToString()}")
                else {
                    asmgen.out(varname)
                    for (chunk in data.chunked(16))
                        asmgen.out("  .word  " + chunk.joinToString())
                }
            }
            DataType.ARRAY_W -> {
                val data = makeArrayFillDataSigned(dt, value, orNumberOfZeros)
                if (data.size <= 16)
                    asmgen.out("$varname\t.sint  ${data.joinToString()}")
                else {
                    asmgen.out(varname)
                    for (chunk in data.chunked(16))
                        asmgen.out("  .sint  " + chunk.joinToString())
                }
            }
            DataType.ARRAY_UW_SPLIT -> {
                val data = makeArrayFillDataUnsigned(dt, value, orNumberOfZeros)
                asmgen.out("_array_$varname := ${data.joinToString()}")
                asmgen.out("${varname}_lsb\t.byte <_array_$varname")
                asmgen.out("${varname}_msb\t.byte >_array_$varname")
            }
            DataType.ARRAY_W_SPLIT -> {
                val data = makeArrayFillDataSigned(dt, value, orNumberOfZeros)
                asmgen.out("_array_$varname := ${data.joinToString()}")
                asmgen.out("${varname}_lsb\t.byte <_array_$varname")
                asmgen.out("${varname}_msb\t.byte >_array_$varname")
            }
            DataType.ARRAY_F -> {
                val array = value ?: zeroFilledArray(orNumberOfZeros!!)
                val floatFills = array.map {
                    compTarget.machine.getFloatAsmBytes(it.number!!)
                }
                asmgen.out(varname)
                for (f in array.zip(floatFills))
                    asmgen.out("  .byte  ${f.second}  ; float ${f.first}")
            }
            else -> throw AssemblyError("require array dt")
        }
    }

    private fun zeroFilledArray(numElts: Int): StArray {
        val values = mutableListOf<StArrayElement>()
        repeat(numElts) {
            values.add(StArrayElement(0.0, null, null))
        }
        return values
    }

    private fun memdefsAndConsts2asm(memvars: Collection<StMemVar>, consts: Collection<StConstant>) {
        memvars.sortedBy { it.address }.forEach {
            asmgen.out("  ${it.name} = ${it.address.toHex()}")
        }
        consts.sortedBy { it.name }.forEach {
            if(it.dt==DataType.FLOAT)
                asmgen.out("  ${it.name} = ${it.value}")
            else
                asmgen.out("  ${it.name} = ${it.value.toHex()}")
        }
    }

    private fun asmsubs2asm(statements: List<PtNode>) {
        statements
            .filter { it is PtAsmSub && it.address!=null }
            .forEach { asmsub ->
                asmsub as PtAsmSub
                val address = asmsub.address!!
                val bank = if(address.constbank!=null) "; @bank ${address.constbank}"
                    else if(address.varbank!=null) "; @bank ${address.varbank?.name}"
                    else ""
                asmgen.out("  ${asmsub.name} = ${address.address.toHex()} $bank")
            }
    }

    private fun outputStringvar(varname: String, align: Int, encoding: Encoding, value: String) {
        alignVar(align)
        asmgen.out("$varname\t; $encoding:\"${value.escape().replace("\u0000", "<NULL>")}\"", false)
        val bytes = compTarget.encodeString(value, encoding).plus(0.toUByte())
        val outputBytes = bytes.map { "$" + it.toString(16).padStart(2, '0') }
        for (chunk in outputBytes.chunked(16))
            asmgen.out("  .byte  " + chunk.joinToString())
    }

    private fun makeArrayFillDataUnsigned(dt: DataType, value: StArray?, orNumberOfZeros: Int?): List<String> {
        val array = value ?: zeroFilledArray(orNumberOfZeros!!)
        return when (dt) {
            DataType.ARRAY_BOOL ->
                // byte array can never contain pointer-to types, so treat values as all integers
                array.map {
                    if(it.boolean!=null)
                        if(it.boolean==true) "1" else "0"
                    else {
                        val number = it.number!!
                        if(number==0.0) "0" else "1"
                    }
                }
            DataType.ARRAY_UB ->
                // byte array can never contain pointer-to types, so treat values as all integers
                array.map {
                    val number = it.number!!.toInt()
                    "$"+number.toString(16).padStart(2, '0')
                }
            DataType.ARRAY_UW, DataType.ARRAY_UW_SPLIT -> array.map {
                if(it.number!=null) {
                    "$" + it.number!!.toInt().toString(16).padStart(4, '0')
                }
                else if(it.addressOfSymbol!=null) {
                    asmgen.asmSymbolName(it.addressOfSymbol!!)
                }
                else
                    throw AssemblyError("weird array elt")
            }
            else -> throw AssemblyError("invalid dt")
        }
    }

    private fun makeArrayFillDataSigned(dt: DataType, value: StArray?, orNumberOfZeros: Int?): List<String> {
        val array = value ?: zeroFilledArray(orNumberOfZeros!!)
        return when (dt) {
            // byte array can never contain pointer-to types, so treat values as all integers
            DataType.ARRAY_UB ->
                array.map {
                    val number = it.number!!.toInt()
                    "$"+number.toString(16).padStart(2, '0')
                }
            DataType.ARRAY_B ->
                array.map {
                    val number = it.number!!.toInt()
                    val hexnum = number.absoluteValue.toString(16).padStart(2, '0')
                    if(number>=0)
                        "$$hexnum"
                    else
                        "-$$hexnum"
                }
            DataType.ARRAY_UW, DataType.ARRAY_UW_SPLIT -> array.map {
                val number = it.number!!.toInt()
                "$" + number.toString(16).padStart(4, '0')
            }
            DataType.ARRAY_W, DataType.ARRAY_W_SPLIT -> array.map {
                val number = it.number!!.toInt()
                val hexnum = number.absoluteValue.toString(16).padStart(4, '0')
                if(number>=0)
                    "$$hexnum"
                else
                    "-$$hexnum"
            }
            else -> throw AssemblyError("invalid dt")
        }
    }

}
