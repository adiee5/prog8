package prog8.codegen.cpu6502

import prog8.code.ast.*
import prog8.code.core.*
import prog8.codegen.cpu6502.assignment.*


internal class BuiltinFunctionsAsmGen(private val program: PtProgram,
                                      private val asmgen: AsmGen6502Internal,
                                      private val assignAsmGen: AssignmentAsmGen) {

    internal fun translateFunctioncallExpression(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?): DataType? {
        return translateFunctioncall(fcall, discardResult = false, resultToStack = resultToStack, resultRegister = resultRegister)
    }

    internal fun translateFunctioncallStatement(fcall: PtBuiltinFunctionCall) {
        translateFunctioncall(fcall, discardResult = true, resultToStack = false, resultRegister = null)
    }

    private fun translateFunctioncall(fcall: PtBuiltinFunctionCall, discardResult: Boolean, resultToStack: Boolean, resultRegister: RegisterOrPair?): DataType? {
        if (discardResult && fcall.hasNoSideEffects)
            return null  // can just ignore the whole function call altogether

        if(discardResult && resultToStack)
            throw AssemblyError("cannot both discard the result AND put it onto stack")

        val sscope = fcall.definingISub()

        when (fcall.name) {
            "msb" -> funcMsb(fcall, resultToStack, resultRegister)
            "lsb" -> funcLsb(fcall, resultToStack, resultRegister)
            "mkword" -> funcMkword(fcall, resultToStack, resultRegister)
            "abs" -> funcAbs(fcall, resultToStack, resultRegister, sscope)
            "any", "all" -> funcAnyAll(fcall, resultToStack, resultRegister, sscope)
            "sgn" -> funcSgn(fcall, resultToStack, resultRegister, sscope)
            "sqrt16" -> funcSqrt16(fcall, resultToStack, resultRegister, sscope)
            "rol" -> funcRol(fcall)
            "rol2" -> funcRol2(fcall)
            "ror" -> funcRor(fcall)
            "ror2" -> funcRor2(fcall)
            "sort" -> funcSort(fcall)
            "reverse" -> funcReverse(fcall)
            "memory" -> funcMemory(fcall, discardResult, resultToStack, resultRegister)
            "peekw" -> funcPeekW(fcall, resultToStack, resultRegister)
            "peek" -> throw AssemblyError("peek() should have been replaced by @()")
            "pokew" -> funcPokeW(fcall)
            "pokemon" -> { /* meme function */ }
            "poke" -> throw AssemblyError("poke() should have been replaced by @()")
            "push" -> asmgen.pushCpuStack(DataType.UBYTE, fcall.args[0])
            "pushw" -> asmgen.pushCpuStack(DataType.UWORD, fcall.args[0])
            "pop" -> {
                require(fcall.args[0] is PtIdentifier) {
                    "attempt to pop a value into a differently typed variable, or in something else that isn't supported ${fcall.position}"
                }
                val symbol = asmgen.symbolTable.lookup((fcall.args[0] as PtIdentifier).name)
                val target = symbol!!.astNode as IPtVariable
                asmgen.popCpuStack(DataType.UBYTE, target, fcall.definingISub())
            }
            "popw" -> {
                require(fcall.args[0] is PtIdentifier) {
                    "attempt to pop a value into a differently typed variable, or in something else that isn't supported ${fcall.position}"
                }
                val symbol = asmgen.symbolTable.lookup((fcall.args[0] as PtIdentifier).name)
                val target = symbol!!.astNode as IPtVariable
                asmgen.popCpuStack(DataType.UWORD, target, fcall.definingISub())
            }
            "rsave" -> funcRsave()
            "rsavex" -> funcRsaveX()
            "rrestore" -> funcRrestore()
            "rrestorex" -> funcRrestoreX()
            "cmp" -> funcCmp(fcall)
            "callfar" -> funcCallFar(fcall)
            "callrom" -> funcCallRom(fcall)
            else -> throw AssemblyError("missing asmgen for builtin func ${fcall.name}")
        }

        return BuiltinFunctions.getValue(fcall.name).returnType
    }

    private fun funcRsave() {
        if (asmgen.isTargetCpu(CpuType.CPU65c02))
            asmgen.out("""
                php
                pha
                phy
                phx""")
        else
            // see http://6502.org/tutorials/register_preservation.html
            asmgen.out("""
                php
                sta  P8ZP_SCRATCH_REG
                pha
                txa
                pha
                tya
                pha
                lda  P8ZP_SCRATCH_REG""")
    }

    private fun funcRsaveX() {
        if (asmgen.isTargetCpu(CpuType.CPU65c02))
            asmgen.out("  phx")
        else
            asmgen.out("  txa |  pha")
    }

    private fun funcRrestore() {
        if (asmgen.isTargetCpu(CpuType.CPU65c02))
            asmgen.out("""
                plx
                ply
                pla
                plp""")
        else
            asmgen.out("""
                pla
                tay
                pla
                tax
                pla
                plp""")
    }

    private fun funcRrestoreX() {
        if (asmgen.isTargetCpu(CpuType.CPU65c02))
            asmgen.out("  plx")
        else
            asmgen.out("  sta  P8ZP_SCRATCH_B1 |  pla |  tax |  lda  P8ZP_SCRATCH_B1")
    }

    private fun funcCallFar(fcall: PtBuiltinFunctionCall) {
        if(asmgen.options.compTarget.name != "cx16")
            throw AssemblyError("callfar only works on cx16 target at this time")

        val bank = fcall.args[0].asConstInteger()
        val address = fcall.args[1].asConstInteger() ?: 0
        val argAddrArg = fcall.args[2]
        if(bank==null)
            throw AssemblyError("callfar (jsrfar) bank has to be a constant")
        if(fcall.args[1] !is PtNumber) {
            assignAsmGen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.AY, false)
            asmgen.out("  sta  (+)+0 |  sty  (+)+1  ; store jsrfar address word")
        }

        if(argAddrArg.asConstInteger() == 0) {
            asmgen.out("""
                jsr  cx16.jsrfar
+               .word  ${address.toHex()}
                .byte  ${bank.toHex()}""")
        } else {
            when(argAddrArg) {
                is PtAddressOf -> {
                    if(argAddrArg.identifier.type != DataType.UBYTE)
                        throw AssemblyError("callfar done with 'arg' pointer to variable that's not UBYTE")
                    asmgen.out("""
                        lda  ${asmgen.asmVariableName(argAddrArg.identifier)}
                        jsr  cx16.jsrfar
+                       .word  ${address.toHex()}
                        .byte  ${bank.toHex()}
                        sta  ${asmgen.asmVariableName(argAddrArg.identifier)}""")
                }
                is PtNumber -> {
                    asmgen.out("""
                        lda  ${argAddrArg.number.toHex()}
                        jsr  cx16.jsrfar
+                       .word  ${address.toHex()}
                        .byte  ${bank.toHex()}
                        sta  ${argAddrArg.number.toHex()}""")
                }
                else -> throw AssemblyError("callfar only accepts pointer-of a (ubyte) variable or constant memory address for the 'arg' parameter")
            }
        }
    }

    private fun funcCallRom(fcall: PtBuiltinFunctionCall) {
        if(asmgen.options.compTarget.name != "cx16")
            throw AssemblyError("callrom only works on cx16 target at this time")

        val bank = fcall.args[0].asConstInteger()
        val address = fcall.args[1].asConstInteger()
        if(bank==null || address==null)
            throw AssemblyError("callrom requires constant arguments")

        if(address !in 0xc000..0xffff)
            throw AssemblyError("callrom done on address outside of cx16 banked rom")
        if(bank>=32)
            throw AssemblyError("callrom bank must be <32")

        val argAddrArg = fcall.args[2]
        if(argAddrArg.asConstInteger() == 0) {
            asmgen.out("""
                lda  $01
                pha
                lda  #${bank}
                sta  $01
                jsr  ${address.toHex()}
                pla
                sta  $01""")
        } else {
            when(argAddrArg) {
                is PtAddressOf -> {
                    if(argAddrArg.identifier.type != DataType.UBYTE)
                        throw AssemblyError("callrom done with 'arg' pointer to variable that's not UBYTE")
                    asmgen.out("""
                        lda  $01
                        pha
                        lda  #${bank}
                        sta  $01
                        lda  ${asmgen.asmVariableName(argAddrArg.identifier)}                
                        jsr  ${address.toHex()}
                        sta  ${asmgen.asmVariableName(argAddrArg.identifier)}
                        pla
                        sta  $01""")
                }
                is PtNumber -> {
                    asmgen.out("""
                        lda  $01
                        pha
                        lda  #${bank}
                        sta  $01
                        lda  ${argAddrArg.number.toHex()}
                        jsr  ${address.toHex()}
                        sta  ${argAddrArg.number.toHex()}
                        pla
                        sta  $01""")
                }
                else -> throw AssemblyError("callrom only accepts pointer-of a (ubyte) variable or constant memory address for the 'arg' parameter")
            }
        }
    }

    private fun funcCmp(fcall: PtBuiltinFunctionCall) {
        val arg1 = fcall.args[0]
        val arg2 = fcall.args[1]
        if(arg1.type in ByteDatatypes) {
            if(arg2.type in ByteDatatypes) {
                when (arg2) {
                    is PtIdentifier -> {
                        asmgen.assignExpressionToRegister(arg1, RegisterOrPair.A)
                        asmgen.out("  cmp  ${asmgen.asmVariableName(arg2)}")
                    }
                    is PtNumber -> {
                        asmgen.assignExpressionToRegister(arg1, RegisterOrPair.A)
                        asmgen.out("  cmp  #${arg2.number.toInt()}")
                    }
                    is PtMemoryByte -> {
                        if(arg2.address is PtNumber) {
                            asmgen.assignExpressionToRegister(arg1, RegisterOrPair.A)
                            asmgen.out("  cmp  ${arg2.address.asConstInteger()!!.toHex()}")
                        } else {
                            if(arg1.isSimple()) {
                                asmgen.assignExpressionToVariable(arg2, "P8ZP_SCRATCH_B1", DataType.UBYTE, fcall.definingISub())
                                asmgen.assignExpressionToRegister(arg1, RegisterOrPair.A)
                                asmgen.out("  cmp  P8ZP_SCRATCH_B1")
                            } else {
                                asmgen.pushCpuStack(DataType.UBYTE, arg1)
                                asmgen.assignExpressionToVariable(arg2, "P8ZP_SCRATCH_B1", DataType.UBYTE, fcall.definingISub())
                                asmgen.out("  pla |  cmp  P8ZP_SCRATCH_B1")
                            }
                        }
                    }
                    else -> {
                        if(arg1.isSimple()) {
                            asmgen.assignExpressionToVariable(arg2, "P8ZP_SCRATCH_B1", DataType.UBYTE, fcall.definingISub())
                            asmgen.assignExpressionToRegister(arg1, RegisterOrPair.A)
                            asmgen.out("  cmp  P8ZP_SCRATCH_B1")
                        } else {
                            asmgen.pushCpuStack(DataType.UBYTE, arg1)
                            asmgen.assignExpressionToVariable(arg2, "P8ZP_SCRATCH_B1", DataType.UBYTE, fcall.definingISub())
                            asmgen.out("  pla |  cmp  P8ZP_SCRATCH_B1")
                        }
                    }
                }
            } else
                throw AssemblyError("args for cmp() should have same dt")
        } else {
            // arg1 is a word
            if(arg2.type in WordDatatypes) {
                when (arg2) {
                    is PtIdentifier -> {
                        asmgen.assignExpressionToRegister(arg1, RegisterOrPair.AY)
                        asmgen.out("""
                            cpy  ${asmgen.asmVariableName(arg2)}+1
                            bne  +
                            cmp  ${asmgen.asmVariableName(arg2)}
+""")
                    }
                    is PtNumber -> {
                        asmgen.assignExpressionToRegister(arg1, RegisterOrPair.AY)
                        asmgen.out("""
                            cpy  #>${arg2.number.toInt()}
                            bne  +
                            cmp  #<${arg2.number.toInt()}
+""")
                    }
                    else -> {
                        if(arg1.isSimple()) {
                            asmgen.assignExpressionToVariable(arg2, "P8ZP_SCRATCH_W1", DataType.UWORD, fcall.definingISub())
                            asmgen.assignExpressionToRegister(arg1, RegisterOrPair.AY)
                            asmgen.out("""
                                cpy  P8ZP_SCRATCH_W1+1
                                bne  +
                                cmp  P8ZP_SCRATCH_W1
    +""")
                        } else {
                            asmgen.pushCpuStack(DataType.UWORD, arg1)
                            asmgen.assignExpressionToVariable(arg2, "P8ZP_SCRATCH_W1", DataType.UWORD, fcall.definingISub())
                            asmgen.restoreRegisterStack(CpuRegister.Y, false)
                            asmgen.restoreRegisterStack(CpuRegister.A, false)
                            asmgen.out("""
                                cpy  P8ZP_SCRATCH_W1+1
                                bne  +
                                cmp  P8ZP_SCRATCH_W1
    +""")
                        }
                    }
                }
            } else
                throw AssemblyError("args for cmp() should have same dt")
        }
    }

    private fun funcMemory(fcall: PtBuiltinFunctionCall, discardResult: Boolean, resultToStack: Boolean, resultRegister: RegisterOrPair?) {
        if(discardResult)
            throw AssemblyError("should not discard result of memory allocation at $fcall")
        val name = (fcall.args[0] as PtString).value
        require(name.all { it.isLetterOrDigit() || it=='_' }) {"memory name should be a valid symbol name ${fcall.position}"}
        val slabname = PtIdentifier("prog8_slabs.prog8_memoryslab_$name", DataType.UWORD, fcall.position)
        val addressOf = PtAddressOf(fcall.position)
        addressOf.add(slabname)
        addressOf.parent = fcall
        val src = AsmAssignSource(SourceStorageKind.EXPRESSION, program, asmgen, DataType.UWORD, expression = addressOf)
        val target =
            if(resultToStack)
                AsmAssignTarget(TargetStorageKind.STACK,  asmgen, DataType.UWORD, null)
            else
                AsmAssignTarget.fromRegisters(resultRegister ?: RegisterOrPair.AY, false, null, asmgen)
        val assign = AsmAssignment(src, target, false, program.memsizer, fcall.position)
        asmgen.translateNormalAssignment(assign)
    }

    private fun funcSqrt16(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?, scope: IPtSubroutine?) {
        translateArguments(fcall, scope)
        if(resultToStack)
            asmgen.out("  jsr  prog8_lib.func_sqrt16_stack")
        else {
            asmgen.out("  jsr  prog8_lib.func_sqrt16_into_A")
            assignAsmGen.assignRegisterByte(AsmAssignTarget.fromRegisters(resultRegister ?: RegisterOrPair.A, false, scope, asmgen), CpuRegister.A)
        }
    }

    private fun funcReverse(fcall: PtBuiltinFunctionCall) {
        val variable = fcall.args.single()
        if (variable is PtIdentifier) {
            val symbol = asmgen.symbolTable.lookup(variable.name)
            val decl = symbol!!.astNode as IPtVariable
            val numElements = when(decl) {
                is PtConstant -> throw AssemblyError("cannot reverse a constant")
                is PtMemMapped -> decl.arraySize
                is PtVariable -> decl.arraySize
            }
            val varName = asmgen.asmVariableName(variable)
            when (decl.type) {
                DataType.ARRAY_UB, DataType.ARRAY_B -> {
                    asmgen.out("""
                        lda  #<$varName
                        ldy  #>$varName
                        sta  P8ZP_SCRATCH_W1
                        sty  P8ZP_SCRATCH_W1+1
                        lda  #$numElements
                        jsr  prog8_lib.func_reverse_b""")
                }
                DataType.ARRAY_UW, DataType.ARRAY_W -> {
                    asmgen.out("""
                        lda  #<$varName
                        ldy  #>$varName
                        sta  P8ZP_SCRATCH_W1
                        sty  P8ZP_SCRATCH_W1+1
                        lda  #$numElements
                        jsr  prog8_lib.func_reverse_w""")
                }
                DataType.ARRAY_F -> {
                    asmgen.out("""
                        lda  #<$varName
                        ldy  #>$varName
                        sta  P8ZP_SCRATCH_W1
                        sty  P8ZP_SCRATCH_W1+1
                        lda  #$numElements
                        jsr  floats.func_reverse_f""")
                }
                else -> throw AssemblyError("weird type")
            }
        }
    }

    private fun funcSort(fcall: PtBuiltinFunctionCall) {
        val variable = fcall.args.single()
        if (variable is PtIdentifier) {
            val symbol = asmgen.symbolTable.lookup(variable.name)
            val decl = symbol!!.astNode as IPtVariable
            val varName = asmgen.asmVariableName(variable)
            val numElements = when(decl) {
                is PtConstant -> throw AssemblyError("cannot sort a constant")
                is PtMemMapped -> decl.arraySize
                is PtVariable -> decl.arraySize
            }
            when (decl.type) {
                DataType.ARRAY_UB, DataType.ARRAY_B -> {
                    asmgen.out("""
                        lda  #<$varName
                        ldy  #>$varName
                        sta  P8ZP_SCRATCH_W1
                        sty  P8ZP_SCRATCH_W1+1
                        lda  #$numElements""")
                    asmgen.out(if (decl.type == DataType.ARRAY_UB) "  jsr  prog8_lib.func_sort_ub" else "  jsr  prog8_lib.func_sort_b")
                }
                DataType.ARRAY_UW, DataType.ARRAY_W -> {
                    asmgen.out("""
                        lda  #<$varName
                        ldy  #>$varName
                        sta  P8ZP_SCRATCH_W1
                        sty  P8ZP_SCRATCH_W1+1
                        lda  #$numElements""")
                    asmgen.out(if (decl.type == DataType.ARRAY_UW) "  jsr  prog8_lib.func_sort_uw" else "  jsr  prog8_lib.func_sort_w")
                }
                DataType.ARRAY_F -> throw AssemblyError("sorting of floating point array is not supported")
                else -> throw AssemblyError("weird type")
            }
        } else
            throw AssemblyError("weird type")
    }

    private fun funcRor2(fcall: PtBuiltinFunctionCall) {
        val what = fcall.args.single()
        when (what.type) {
            DataType.UBYTE -> {
                when (what) {
                    is PtArrayIndexer -> {
                        translateRolRorArrayArgs(what.variable, what, "ror2", 'b')
                        asmgen.out("  jsr  prog8_lib.ror2_array_ub")
                    }
                    is PtMemoryByte -> {
                        if (what.address is PtNumber) {
                            val number = (what.address as PtNumber).number
                            asmgen.out("  lda  ${number.toHex()} |  lsr  a |  bcc  + |  ora  #\$80 |+  |  sta  ${number.toHex()}")
                        } else {
                            asmgen.assignExpressionToRegister(what.address, RegisterOrPair.AY)
                            asmgen.out("  jsr  prog8_lib.ror2_mem_ub")
                        }
                    }
                    is PtIdentifier -> {
                        val variable = asmgen.asmVariableName(what)
                        asmgen.out("  lda  $variable |  lsr  a |  bcc  + |  ora  #\$80 |+  |  sta  $variable")
                    }
                    else -> throw AssemblyError("weird type")
                }
            }
            DataType.UWORD -> {
                when (what) {
                    is PtArrayIndexer -> {
                        translateRolRorArrayArgs(what.variable, what, "ror2", 'w')
                        asmgen.out("  jsr  prog8_lib.ror2_array_uw")
                    }
                    is PtIdentifier -> {
                        val variable = asmgen.asmVariableName(what)
                        asmgen.out("  lsr  $variable+1 |  ror  $variable |  bcc  + |  lda  $variable+1 |  ora  #\$80 |  sta  $variable+1 |+  ")
                    }
                    else -> throw AssemblyError("weird type")
                }
            }
            else -> throw AssemblyError("weird type")
        }
    }

    private fun funcRor(fcall: PtBuiltinFunctionCall) {
        val what = fcall.args.single()
        when (what.type) {
            DataType.UBYTE -> {
                when (what) {
                    is PtArrayIndexer -> {
                        translateRolRorArrayArgs(what.variable, what, "ror", 'b')
                        asmgen.out("  jsr  prog8_lib.ror_array_ub")
                    }
                    is PtMemoryByte -> {
                        if (what.address is PtNumber) {
                            val number = (what.address as PtNumber).number
                            asmgen.out("  ror  ${number.toHex()}")
                        } else {
                            val ptrAndIndex = asmgen.pointerViaIndexRegisterPossible(what.address)
                            if(ptrAndIndex!=null) {
                                asmgen.saveRegisterLocal(CpuRegister.X, fcall.definingISub()!!)
                                asmgen.assignExpressionToRegister(ptrAndIndex.second, RegisterOrPair.X)
                                asmgen.saveRegisterLocal(CpuRegister.X, fcall.definingISub()!!)
                                asmgen.assignExpressionToRegister(ptrAndIndex.first, RegisterOrPair.AY)
                                asmgen.restoreRegisterLocal(CpuRegister.X)
                                asmgen.out("""
                                    sta  (+) + 1
                                    sty  (+) + 2
+                                   ror  ${'$'}ffff,x           ; modified""")
                                asmgen.restoreRegisterLocal(CpuRegister.X)
                            } else {
                                asmgen.assignExpressionToRegister(what.address, RegisterOrPair.AY)
                                asmgen.out("""
                                    sta  (+) + 1
                                    sty  (+) + 2
+                                   ror  ${'$'}ffff            ; modified""")
                            }
                        }
                    }
                    is PtIdentifier -> {
                        val variable = asmgen.asmVariableName(what)
                        asmgen.out("  ror  $variable")
                    }
                    else -> throw AssemblyError("weird type")
                }
            }
            DataType.UWORD -> {
                when (what) {
                    is PtArrayIndexer -> {
                        translateRolRorArrayArgs(what.variable, what, "ror", 'w')
                        asmgen.out("  jsr  prog8_lib.ror_array_uw")
                    }
                    is PtIdentifier -> {
                        val variable = asmgen.asmVariableName(what)
                        asmgen.out("  ror  $variable+1 |  ror  $variable")
                    }
                    else -> throw AssemblyError("weird type")
                }
            }
            else -> throw AssemblyError("weird type")
        }
    }

    private fun funcRol2(fcall: PtBuiltinFunctionCall) {
        val what = fcall.args.single()
        when (what.type) {
            DataType.UBYTE -> {
                when (what) {
                    is PtArrayIndexer -> {
                        translateRolRorArrayArgs(what.variable, what, "rol2", 'b')
                        asmgen.out("  jsr  prog8_lib.rol2_array_ub")
                    }
                    is PtMemoryByte -> {
                        if (what.address is PtNumber) {
                            val number = (what.address as PtNumber).number
                            asmgen.out("  lda  ${number.toHex()} |  cmp  #\$80 |  rol  a |  sta  ${number.toHex()}")
                        } else {
                            asmgen.assignExpressionToRegister(what.address, RegisterOrPair.AY)
                            asmgen.out("  jsr  prog8_lib.rol2_mem_ub")
                        }
                    }
                    is PtIdentifier -> {
                        val variable = asmgen.asmVariableName(what)
                        asmgen.out("  lda  $variable |  cmp  #\$80 |  rol  a |  sta  $variable")
                    }
                    else -> throw AssemblyError("weird type")
                }
            }
            DataType.UWORD -> {
                when (what) {
                    is PtArrayIndexer -> {
                        translateRolRorArrayArgs(what.variable, what, "rol2", 'w')
                        asmgen.out("  jsr  prog8_lib.rol2_array_uw")
                    }
                    is PtIdentifier -> {
                        val variable = asmgen.asmVariableName(what)
                        asmgen.out("  asl  $variable |  rol  $variable+1 |  bcc  + |  inc  $variable |+  ")
                    }
                    else -> throw AssemblyError("weird type")
                }
            }
            else -> throw AssemblyError("weird type")
        }
    }

    private fun funcRol(fcall: PtBuiltinFunctionCall) {
        val what = fcall.args.single()
        when (what.type) {
            DataType.UBYTE -> {
                when (what) {
                    is PtArrayIndexer -> {
                        translateRolRorArrayArgs(what.variable, what, "rol", 'b')
                        asmgen.out("  jsr  prog8_lib.rol_array_ub")
                    }
                    is PtMemoryByte -> {
                        if (what.address is PtNumber) {
                            val number = (what.address as PtNumber).number
                            asmgen.out("  rol  ${number.toHex()}")
                        } else {
                            val ptrAndIndex = asmgen.pointerViaIndexRegisterPossible(what.address)
                            if(ptrAndIndex!=null) {
                                asmgen.saveRegisterLocal(CpuRegister.X, fcall.definingISub()!!)
                                asmgen.assignExpressionToRegister(ptrAndIndex.second, RegisterOrPair.X)
                                asmgen.saveRegisterLocal(CpuRegister.X, fcall.definingISub()!!)
                                asmgen.assignExpressionToRegister(ptrAndIndex.first, RegisterOrPair.AY)
                                asmgen.restoreRegisterLocal(CpuRegister.X)
                                asmgen.out("""
                                    sta  (+) + 1
                                    sty  (+) + 2
+                                   rol  ${'$'}ffff,x           ; modified""")
                                asmgen.restoreRegisterLocal(CpuRegister.X)
                            } else {
                                asmgen.assignExpressionToRegister(what.address, RegisterOrPair.AY)
                                asmgen.out("""
                                    sta  (+) + 1
                                    sty  (+) + 2
+                                   rol  ${'$'}ffff            ; modified""")
                            }
                        }
                    }
                    is PtIdentifier -> {
                        val variable = asmgen.asmVariableName(what)
                        asmgen.out("  rol  $variable")
                    }
                    else -> throw AssemblyError("weird type")
                }
            }
            DataType.UWORD -> {
                when (what) {
                    is PtArrayIndexer -> {
                        translateRolRorArrayArgs(what.variable, what, "rol", 'w')
                        asmgen.out("  jsr  prog8_lib.rol_array_uw")
                    }
                    is PtIdentifier -> {
                        val variable = asmgen.asmVariableName(what)
                        asmgen.out("  rol  $variable |  rol  $variable+1")
                    }
                    else -> throw AssemblyError("weird type")
                }
            }
            else -> throw AssemblyError("weird type")
        }
    }

    private fun translateRolRorArrayArgs(arrayvar: PtIdentifier, indexer: PtArrayIndexer, operation: String, dt: Char) {
        if(arrayvar.type==DataType.UWORD) {
            if(dt!='b')
                throw AssemblyError("non-array var indexing requires bytes dt")
            asmgen.assignExpressionToVariable(arrayvar, "prog8_lib.${operation}_array_u${dt}._arg_target", DataType.UWORD, null)
        } else {
            val addressOf = PtAddressOf(arrayvar.position)
            addressOf.add(arrayvar)
            addressOf.parent = arrayvar.parent.parent
            asmgen.assignExpressionToVariable(addressOf, "prog8_lib.${operation}_array_u${dt}._arg_target", DataType.UWORD, null)
        }
        asmgen.assignExpressionToVariable(indexer.index, "prog8_lib.${operation}_array_u${dt}._arg_index", DataType.UBYTE, null)
    }

    private fun funcSgn(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?, scope: IPtSubroutine?) {
        translateArguments(fcall, scope)
        val dt = fcall.args.single().type
        if(resultToStack) {
            when (dt) {
                DataType.UBYTE -> asmgen.out("  jsr  prog8_lib.func_sign_ub_stack")
                DataType.BYTE -> asmgen.out("  jsr  prog8_lib.func_sign_b_stack")
                DataType.UWORD -> asmgen.out("  jsr  prog8_lib.func_sign_uw_stack")
                DataType.WORD -> asmgen.out("  jsr  prog8_lib.func_sign_w_stack")
                DataType.FLOAT -> asmgen.out("  jsr  floats.func_sign_f_stack")
                else -> throw AssemblyError("weird type $dt")
            }
        } else {
            when (dt) {
                DataType.UBYTE -> asmgen.out("  jsr  prog8_lib.func_sign_ub_into_A")
                DataType.BYTE -> asmgen.out("  jsr  prog8_lib.func_sign_b_into_A")
                DataType.UWORD -> asmgen.out("  jsr  prog8_lib.func_sign_uw_into_A")
                DataType.WORD -> asmgen.out("  jsr  prog8_lib.func_sign_w_into_A")
                DataType.FLOAT -> asmgen.out("  jsr  floats.func_sign_f_into_A")
                else -> throw AssemblyError("weird type $dt")
            }
            assignAsmGen.assignRegisterByte(AsmAssignTarget.fromRegisters(resultRegister ?: RegisterOrPair.A, false, scope, asmgen), CpuRegister.A)
        }
    }

    private fun funcAnyAll(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?, scope: IPtSubroutine?) {
        outputAddressAndLenghtOfArray(fcall.args[0])
        val dt = fcall.args.single().type
        if(resultToStack) {
            when (dt) {
                DataType.ARRAY_B, DataType.ARRAY_UB, DataType.STR -> asmgen.out("  jsr  prog8_lib.func_${fcall.name}_b_stack")
                DataType.ARRAY_UW, DataType.ARRAY_W -> asmgen.out("  jsr  prog8_lib.func_${fcall.name}_w_stack")
                DataType.ARRAY_F -> asmgen.out("  jsr  floats.func_${fcall.name}_f_stack")
                else -> throw AssemblyError("weird type $dt")
            }
        } else {
            when (dt) {
                DataType.ARRAY_B, DataType.ARRAY_UB, DataType.STR -> asmgen.out("  jsr  prog8_lib.func_${fcall.name}_b_into_A |  ldy  #0")
                DataType.ARRAY_UW, DataType.ARRAY_W -> asmgen.out("  jsr  prog8_lib.func_${fcall.name}_w_into_A |  ldy  #0")
                DataType.ARRAY_F -> asmgen.out("  jsr  floats.func_${fcall.name}_f_into_A |  ldy  #0")
                else -> throw AssemblyError("weird type $dt")
            }
            assignAsmGen.assignRegisterByte(AsmAssignTarget.fromRegisters(resultRegister ?: RegisterOrPair.A, false, scope, asmgen), CpuRegister.A)
        }
    }

    private fun funcAbs(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?, scope: IPtSubroutine?) {
        translateArguments(fcall, scope)
        val dt = fcall.args.single().type
        if(resultToStack) {
            when (dt) {
                DataType.UBYTE -> asmgen.out("  ldy  #0")
                DataType.BYTE -> asmgen.out("  jsr  prog8_lib.abs_b_stack")
                DataType.UWORD -> {}
                DataType.WORD -> asmgen.out("  jsr  prog8_lib.abs_w_stack")
                else -> throw AssemblyError("weird type")
            }
        } else {
            when (dt) {
                DataType.UBYTE -> asmgen.out("  ldy  #0")
                DataType.BYTE -> asmgen.out("  jsr  prog8_lib.abs_b_into_AY")
                DataType.UWORD -> {}
                DataType.WORD -> asmgen.out("  jsr  prog8_lib.abs_w_into_AY")
                else -> throw AssemblyError("weird type")
            }
            assignAsmGen.assignRegisterpairWord(AsmAssignTarget.fromRegisters(resultRegister ?: RegisterOrPair.AY, false, scope, asmgen), RegisterOrPair.AY)
        }
    }

    private fun funcPokeW(fcall: PtBuiltinFunctionCall) {
        when(val addrExpr = fcall.args[0]) {
            is PtNumber -> {
                asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.AY)
                val addr = addrExpr.number.toHex()
                asmgen.out("  sta  $addr |  sty  ${addr}+1")
                return
            }
            is PtIdentifier -> {
                val varname = asmgen.asmVariableName(addrExpr)
                if(asmgen.isZpVar(addrExpr)) {
                    // pointervar is already in the zero page, no need to copy
                    asmgen.saveRegisterLocal(CpuRegister.X, fcall.definingISub()!!)
                    asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.AX)
                    if (asmgen.isTargetCpu(CpuType.CPU65c02)) {
                        asmgen.out("""
                            sta  ($varname)
                            txa
                            ldy  #1
                            sta  ($varname),y""")
                    } else {
                        asmgen.out("""
                            ldy  #0
                            sta  ($varname),y
                            txa
                            iny
                            sta  ($varname),y""")
                    }
                    asmgen.restoreRegisterLocal(CpuRegister.X)
                    return
                }
            }
            is PtBinaryExpression -> {
                if(addrExpr.operator=="+" && addrExpr.left is PtIdentifier && addrExpr.right is PtNumber) {
                    val varname = asmgen.asmVariableName(addrExpr.left as PtIdentifier)
                    if(asmgen.isZpVar(addrExpr.left as PtIdentifier)) {
                        // pointervar is already in the zero page, no need to copy
                        asmgen.saveRegisterLocal(CpuRegister.X, fcall.definingISub()!!)
                        asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.AX)
                        val index = (addrExpr.right as PtNumber).number.toHex()
                        asmgen.out("""
                            ldy  #$index
                            sta  ($varname),y
                            txa
                            iny
                            sta  ($varname),y""")
                        asmgen.restoreRegisterLocal(CpuRegister.X)
                        return
                    }
                }
            }
            else -> throw AssemblyError("wrong pokew arg type")
        }

        asmgen.assignExpressionToVariable(fcall.args[0], "P8ZP_SCRATCH_W1", DataType.UWORD, null)
        asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.AY)
        asmgen.out("  jsr  prog8_lib.func_pokew")
    }

    private fun funcPeekW(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?) {
        when(val addrExpr = fcall.args[0]) {
            is PtNumber -> {
                val addr = addrExpr.number.toHex()
                asmgen.out("  lda  $addr |  ldy  ${addr}+1")
            }
            is PtIdentifier -> {
                val varname = asmgen.asmVariableName(addrExpr)
                if(asmgen.isZpVar(addrExpr)) {
                    // pointervar is already in the zero page, no need to copy
                    if (asmgen.isTargetCpu(CpuType.CPU65c02)) {
                        asmgen.out("""
                            ldy  #1
                            lda  ($varname),y
                            tay
                            lda  ($varname)""")
                    } else {
                        asmgen.out("""
                            ldy  #0
                            lda  ($varname),y
                            pha
                            iny
                            lda  ($varname),y
                            tay
                            pla""")
                    }
                } else {
                    asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.AY)
                    asmgen.out("  jsr  prog8_lib.func_peekw")
                }
            }
            is PtBinaryExpression -> {
                if(addrExpr.operator=="+" && addrExpr.left is PtIdentifier && addrExpr.right is PtNumber) {
                    val varname = asmgen.asmVariableName(addrExpr.left as PtIdentifier)
                    if(asmgen.isZpVar(addrExpr.left as PtIdentifier)) {
                        // pointervar is already in the zero page, no need to copy
                        val index = (addrExpr.right as PtNumber).number.toHex()
                        asmgen.out("""
                            ldy  #$index
                            lda  ($varname),y
                            pha
                            iny
                            lda  ($varname),y
                            tay
                            pla""")
                    }  else {
                        asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.AY)
                        asmgen.out("  jsr  prog8_lib.func_peekw")
                    }
                } else {
                    asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.AY)
                    asmgen.out("  jsr  prog8_lib.func_peekw")
                }
            }
            else -> {
                asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.AY)
                asmgen.out("  jsr  prog8_lib.func_peekw")
            }
        }

        if(resultToStack){
            asmgen.out("  sta  P8ESTACK_LO,x |  tya |  sta  P8ESTACK_HI,x |  dex")
        } else {
            when(resultRegister ?: RegisterOrPair.AY) {
                RegisterOrPair.AY -> {}
                RegisterOrPair.AX -> asmgen.out("  sty  P8ZP_SCRATCH_REG |  ldx  P8ZP_SCRATCH_REG")
                RegisterOrPair.XY -> asmgen.out("  tax")
                in Cx16VirtualRegisters -> asmgen.out(
                    "  sta  cx16.${
                        resultRegister.toString().lowercase()
                    } |  sty  cx16.${resultRegister.toString().lowercase()}+1")
                else -> throw AssemblyError("invalid reg")
            }
        }
    }

    private fun funcMkword(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?) {
        if(resultToStack) {
            asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.Y)      // msb
            asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.A)      // lsb
            asmgen.out("  sta  P8ESTACK_LO,x |  tya |  sta  P8ESTACK_HI,x |  dex")
        } else {
            val reg = resultRegister ?: RegisterOrPair.AY
            var needAsave = asmgen.needAsaveForExpr(fcall.args[0])
            if(!needAsave) {
                val mr0 = fcall.args[0] as? PtMemoryByte
                val mr1 = fcall.args[1] as? PtMemoryByte
                if (mr0 != null)
                    needAsave =  mr0.address !is PtNumber
                if (mr1 != null)
                    needAsave = needAsave or (mr1.address !is PtNumber)
            }
            when(reg) {
                RegisterOrPair.AX -> {
                    asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.A)      // lsb
                    if(needAsave)
                        asmgen.out("  pha")
                    asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.X)      // msb
                    if(needAsave)
                        asmgen.out("  pla")
                }
                RegisterOrPair.AY -> {
                    asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.A)      // lsb
                    if(needAsave)
                        asmgen.out("  pha")
                    asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.Y)      // msb
                    if(needAsave)
                        asmgen.out("  pla")
                }
                RegisterOrPair.XY -> {
                    asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.A)      // lsb
                    if(needAsave)
                        asmgen.out("  pha")
                    asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.Y)      // msb
                    if(needAsave)
                        asmgen.out("  pla")
                    asmgen.out("  tax")
                }
                in Cx16VirtualRegisters -> {
                    asmgen.assignExpressionToRegister(fcall.args[1], RegisterOrPair.A)      // lsb
                    asmgen.out("  sta  cx16.${reg.toString().lowercase()}")
                    asmgen.assignExpressionToRegister(fcall.args[0], RegisterOrPair.A)      // msb
                    asmgen.out("  sta  cx16.${reg.toString().lowercase()}+1")
                }
                else -> throw AssemblyError("invalid mkword target reg")
            }
        }
    }

    private fun funcMsb(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?) {
        val arg = fcall.args.single()
        if (arg.type !in WordDatatypes)
            throw AssemblyError("msb required word argument")
        if (arg is PtNumber)
            throw AssemblyError("msb(const) should have been const-folded away")
        if (arg is PtIdentifier) {
            val sourceName = asmgen.asmVariableName(arg)
            if(resultToStack) {
                asmgen.out("  lda  $sourceName+1 |  sta  P8ESTACK_LO,x |  dex")
            } else {
                when(resultRegister) {
                    null, RegisterOrPair.A -> asmgen.out("  lda  $sourceName+1")
                    RegisterOrPair.X -> asmgen.out("  ldx  $sourceName+1")
                    RegisterOrPair.Y -> asmgen.out("  ldy  $sourceName+1")
                    RegisterOrPair.AX -> asmgen.out("  lda  $sourceName+1 |  ldx  #0")
                    RegisterOrPair.AY -> asmgen.out("  lda  $sourceName+1 |  ldy  #0")
                    RegisterOrPair.XY -> asmgen.out("  ldx  $sourceName+1 |  ldy  #0")
                    in Cx16VirtualRegisters -> {
                        val regname = resultRegister.name.lowercase()
                        if(asmgen.isTargetCpu(CpuType.CPU65c02))
                            asmgen.out("  lda  $sourceName+1 |  sta  cx16.$regname |  stz  cx16.$regname+1")
                        else
                            asmgen.out("  lda  $sourceName+1 |  sta  cx16.$regname |  lda  #0 |  sta  cx16.$regname+1")
                    }
                    else -> throw AssemblyError("invalid reg")
                }
            }
        } else {
            if(resultToStack) {
                asmgen.assignExpressionToRegister(fcall.args.single(), RegisterOrPair.AY)
                asmgen.out("  tya |  sta  P8ESTACK_LO,x |  dex")
            } else {
                when(resultRegister) {
                    null, RegisterOrPair.A -> {
                        asmgen.assignExpressionToRegister(fcall.args.single(), RegisterOrPair.AY)
                        asmgen.out("  tya")
                    }
                    RegisterOrPair.X -> {
                        asmgen.out("  pha")
                        asmgen.assignExpressionToRegister(fcall.args.single(), RegisterOrPair.AX)
                        asmgen.out("  pla")
                    }
                    RegisterOrPair.Y -> {
                        asmgen.out("  pha")
                        asmgen.assignExpressionToRegister(fcall.args.single(), RegisterOrPair.AY)
                        asmgen.out("  pla")
                    }
                    else -> throw AssemblyError("invalid reg")
                }
            }
        }
    }

    private fun funcLsb(fcall: PtBuiltinFunctionCall, resultToStack: Boolean, resultRegister: RegisterOrPair?) {
        val arg = fcall.args.single()
        if (arg.type !in WordDatatypes)
            throw AssemblyError("lsb required word argument")
        if (arg is PtNumber)
            throw AssemblyError("lsb(const) should have been const-folded away")

        if (arg is PtIdentifier) {
            val sourceName = asmgen.asmVariableName(arg)
            if(resultToStack) {
                asmgen.out("  lda  $sourceName |  sta  P8ESTACK_LO,x |  dex")
            } else {
                when(resultRegister) {
                    null, RegisterOrPair.A -> asmgen.out("  lda  $sourceName")
                    RegisterOrPair.X -> asmgen.out("  ldx  $sourceName")
                    RegisterOrPair.Y -> asmgen.out("  ldy  $sourceName")
                    RegisterOrPair.AX -> asmgen.out("  lda  $sourceName |  ldx  #0")
                    RegisterOrPair.AY -> asmgen.out("  lda  $sourceName |  ldy  #0")
                    RegisterOrPair.XY -> asmgen.out("  ldx  $sourceName |  ldy  #0")
                    in Cx16VirtualRegisters -> {
                        val regname = resultRegister.name.lowercase()
                        if(asmgen.isTargetCpu(CpuType.CPU65c02))
                            asmgen.out("  lda  $sourceName |  sta  cx16.$regname |  stz  cx16.$regname+1")
                        else
                            asmgen.out("  lda  $sourceName |  sta  cx16.$regname |  lda  #0 |  sta  cx16.$regname+1")
                    }
                    else -> throw AssemblyError("invalid reg")
                }
            }
        } else {
            if(resultToStack) {
                asmgen.assignExpressionToRegister(fcall.args.single(), RegisterOrPair.AY)
                // NOTE: we rely on the fact that the above assignment to AY, assigns the Lsb to A as the last instruction.
                //       this is required because the compiler assumes the status bits are set according to what A is (lsb)
                //       and will not generate another cmp when lsb() is directly used inside a comparison expression.
                asmgen.out("  sta  P8ESTACK_LO,x |  dex")
            } else {
                when(resultRegister) {
                    null, RegisterOrPair.A -> {
                        asmgen.assignExpressionToRegister(fcall.args.single(), RegisterOrPair.AY)
                        // NOTE: we rely on the fact that the above assignment to AY, assigns the Lsb to A as the last instruction.
                        //       this is required because the compiler assumes the status bits are set according to what A is (lsb)
                        //       and will not generate another cmp when lsb() is directly used inside a comparison expression.
                    }
                    RegisterOrPair.X -> {
                        asmgen.assignExpressionToRegister(fcall.args.single(), RegisterOrPair.XY)
                        // NOTE: we rely on the fact that the above assignment to XY, assigns the Lsb to X as the last instruction.
                        //       this is required because the compiler assumes the status bits are set according to what X is (lsb)
                        //       and will not generate another cmp when lsb() is directly used inside a comparison expression.
                    }
                    RegisterOrPair.Y -> {
                        asmgen.out("  pha")
                        asmgen.assignExpressionToRegister(fcall.args.single(), RegisterOrPair.AY)
                        asmgen.out("  tay |  pla |  cpy  #0")
                    }
                    else -> throw AssemblyError("invalid reg")
                }
            }
        }
    }

    private fun outputAddressAndLenghtOfArray(arg: PtExpression) {
        // address in P8ZP_SCRATCH_W1,  number of elements in A
        arg as PtIdentifier
        val symbol = asmgen.symbolTable.lookup(arg.name)
        val arrayVar = symbol!!.astNode as IPtVariable
        val numElements = when(arrayVar) {
            is PtConstant -> null
            is PtMemMapped -> arrayVar.arraySize
            is PtVariable -> arrayVar.arraySize
        } ?: throw AssemblyError("length of non-array requested")
        val identifierName = asmgen.asmVariableName(arg)
        asmgen.out("""
                    lda  #<$identifierName
                    ldy  #>$identifierName
                    sta  P8ZP_SCRATCH_W1
                    sty  P8ZP_SCRATCH_W1+1
                    lda  #$numElements
                    """)
    }

    private fun translateArguments(call: PtBuiltinFunctionCall, scope: IPtSubroutine?) {
        val signature = BuiltinFunctions.getValue(call.name)
        val callConv = signature.callConvention(call.args.map { it.type})

        fun getSourceForFloat(value: PtExpression): AsmAssignSource {
            return when (value) {
                is PtIdentifier -> {
                    val addr = PtAddressOf(value.position)
                    addr.add(value)
                    addr.parent = call
                    AsmAssignSource.fromAstSource(addr, program, asmgen)
                }
                is PtNumber -> {
                    throw AssemblyError("float literals should have been converted into autovar")
                }
                else -> {
                    if(scope==null)
                        throw AssemblyError("cannot use float arguments outside of a subroutine scope")

                    asmgen.subroutineExtra(scope).usedFloatEvalResultVar2 = true
                    val variable = PtIdentifier(subroutineFloatEvalResultVar2, DataType.FLOAT, value.position)
                    val addr = PtAddressOf(value.position)
                    addr.add(variable)
                    addr.parent = call
                    asmgen.assignExpressionToVariable(value, asmgen.asmVariableName(variable), DataType.FLOAT, scope)
                    AsmAssignSource.fromAstSource(addr, program, asmgen)
                }
            }
        }

        call.args.zip(callConv.params).zip(signature.parameters).forEach {
            val paramName = it.second.name
            val conv = it.first.second
            val value = it.first.first
            when {
                conv.variable -> {
                    val varname = "prog8_lib.func_${call.name}._arg_${paramName}"
                    val src = when (conv.dt) {
                        DataType.FLOAT -> getSourceForFloat(value)
                        in PassByReferenceDatatypes -> {
                            // put the address of the argument in AY
                            val addr = PtAddressOf(value.position)
                            addr.add(value)
                            addr.parent = call
                            AsmAssignSource.fromAstSource(addr, program, asmgen)
                        }
                        else -> {
                            AsmAssignSource.fromAstSource(value, program, asmgen)
                        }
                    }
                    val tgt = AsmAssignTarget(TargetStorageKind.VARIABLE, asmgen, conv.dt, null, variableAsmName = varname)
                    val assign = AsmAssignment(src, tgt, false, program.memsizer, value.position)
                    asmgen.translateNormalAssignment(assign)
                }
                conv.reg != null -> {
                    val src = when (conv.dt) {
                        DataType.FLOAT -> getSourceForFloat(value)
                        in PassByReferenceDatatypes -> {
                            // put the address of the argument in AY
                            val addr = PtAddressOf(value.position)
                            addr.add(value)
                            addr.parent = call
                            AsmAssignSource.fromAstSource(addr, program, asmgen)
                        }
                        else -> {
                            AsmAssignSource.fromAstSource(value, program, asmgen)
                        }
                    }
                    val tgt = AsmAssignTarget.fromRegisters(conv.reg!!, false, null, asmgen)
                    val assign = AsmAssignment(src, tgt, false, program.memsizer, value.position)
                    asmgen.translateNormalAssignment(assign)
                }
                else -> throw AssemblyError("callconv")
            }
        }
    }

}
