package prog8tests

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.instanceOf
import prog8.ast.base.DataType
import prog8.ast.expressions.*
import prog8.ast.statements.*
import prog8.compiler.target.C64Target
import prog8tests.helpers.ErrorReporterForTests
import prog8tests.helpers.assertFailure
import prog8tests.helpers.assertSuccess
import prog8tests.helpers.compileText


class TestSubroutines: FunSpec({

    test("stringParameter") {
        val text = """
            main {
                sub start() {
                    str text = "test"
                    
                    asmfunc("text")
                    asmfunc(text)
                    asmfunc($2000)
                    func("text")
                    func(text)
                    func($2000)
                }
                
                asmsub asmfunc(str thing @AY) {
                }

                sub func(str thing) {
                    uword t2 = thing as uword
                    asmfunc(thing)
                }
            }
        """
        val result = compileText(C64Target, false, text, writeAssembly = false).assertSuccess()
        val module = result.program.toplevelModule
        val mainBlock = module.statements.single() as Block
        val asmfunc = mainBlock.statements.filterIsInstance<Subroutine>().single { it.name=="asmfunc"}
        val func = mainBlock.statements.filterIsInstance<Subroutine>().single { it.name=="func"}
        asmfunc.isAsmSubroutine shouldBe true
        asmfunc.parameters.single().type shouldBe DataType.STR
        asmfunc.statements.isEmpty() shouldBe true
        func.isAsmSubroutine shouldBe false
        func.parameters.single().type shouldBe DataType.STR
        func.statements.size shouldBe 4
        val paramvar = func.statements[0] as VarDecl
        paramvar.name shouldBe "thing"
        paramvar.datatype shouldBe DataType.STR
        val assign = func.statements[2] as Assignment
        assign.target.identifier!!.nameInSource shouldBe listOf("t2")
        withClue("str param in function body should not be transformed by normal compiler steps") {
            assign.value shouldBe instanceOf<TypecastExpression>()
        }
        (assign.value as TypecastExpression).type shouldBe DataType.UWORD
        val call = func.statements[3] as FunctionCallStatement
        call.target.nameInSource.single() shouldBe "asmfunc"
        withClue("str param in function body should not be transformed by normal compiler steps") {
            call.args.single() shouldBe instanceOf<IdentifierReference>()
        }
        (call.args.single() as IdentifierReference).nameInSource.single() shouldBe "thing"
    }

    test("stringParameterAsmGen") {
        val text = """
            main {
                sub start() {
                    str text = "test"
                    
                    asmfunc("text")
                    asmfunc(text)
                    asmfunc($2000)
                    func("text")
                    func(text)
                    func($2000)
                }
                
                asmsub asmfunc(str thing @AY) {
                }

                sub func(str thing) {
                    uword t2 = thing as uword
                    asmfunc(thing)
                }
            }
        """
        val result = compileText(C64Target, false, text, writeAssembly = true).assertSuccess()
        val module = result.program.toplevelModule
        val mainBlock = module.statements.single() as Block
        val asmfunc = mainBlock.statements.filterIsInstance<Subroutine>().single { it.name=="asmfunc"}
        val func = mainBlock.statements.filterIsInstance<Subroutine>().single { it.name=="func"}
        asmfunc.isAsmSubroutine shouldBe true
        asmfunc.parameters.single().type shouldBe DataType.STR
        asmfunc.statements.single() shouldBe instanceOf<Return>()
        func.isAsmSubroutine shouldBe false
        withClue("asmgen should have changed str to uword type") {
            func.parameters.single().type shouldBe DataType.UWORD
        }
        asmfunc.statements.last() shouldBe instanceOf<Return>()

        func.statements.size shouldBe 5
        func.statements[4] shouldBe instanceOf<Return>()
        val paramvar = func.statements[0] as VarDecl
        paramvar.name shouldBe "thing"
        withClue("pre-asmgen should have changed str to uword type") {
            paramvar.datatype shouldBe DataType.UWORD
        }
        val assign = func.statements[2] as Assignment
        assign.target.identifier!!.nameInSource shouldBe listOf("t2")
        withClue("str param in function body should be treated as plain uword before asmgen") {
            assign.value shouldBe instanceOf<IdentifierReference>()
        }
        (assign.value as IdentifierReference).nameInSource.single() shouldBe "thing"
        val call = func.statements[3] as FunctionCallStatement
        call.target.nameInSource.single() shouldBe "asmfunc"
        withClue("str param in function body should be treated as plain uword and not been transformed") {
            call.args.single() shouldBe instanceOf<IdentifierReference>()
        }
        (call.args.single() as IdentifierReference).nameInSource.single() shouldBe "thing"
    }

    test("arrayParameterNotYetAllowed_ButShouldPerhapsBe") {
        // note: the *parser* accepts this as it is valid *syntax*,
        // however, it's not (yet) valid for the compiler
        val text = """
            main {
                sub start() {
                }
                
                asmsub asmfunc(ubyte[] thing @AY) {
                }

                sub func(ubyte[22] thing) {
                }
            }
        """

        val errors = ErrorReporterForTests()
        compileText(C64Target, false, text, errors, false).assertFailure("currently array dt in signature is invalid")     // TODO should not be invalid?
        errors.warnings.size shouldBe 0
        errors.errors.single() shouldContain ".p8:9:16: Non-string pass-by-reference types cannot occur as a parameter type directly"
    }

    // TODO allow this?
    xtest("arrayParameter") {
        val text = """
            main {
                sub start() {
                    ubyte[] array = [1,2,3]
                    
                    asmfunc(array)
                    asmfunc([4,5,6])
                    asmfunc($2000)
                    asmfunc(12.345)
                    func(array)
                    func([4,5,6])
                    func($2000)
                    func(12.345)
                }
                
                asmsub asmfunc(ubyte[] thing @AY) {
                }

                sub func(ubyte[22] thing) {
                }
            }
        """

        val result = compileText(C64Target, false, text, writeAssembly = false).assertSuccess()
        val module = result.program.toplevelModule
        val mainBlock = module.statements.single() as Block
        val asmfunc = mainBlock.statements.filterIsInstance<Subroutine>().single { it.name=="asmfunc"}
        val func = mainBlock.statements.filterIsInstance<Subroutine>().single { it.name=="func"}
        asmfunc.isAsmSubroutine shouldBe true
        asmfunc.parameters.single().type shouldBe DataType.ARRAY_UB
        asmfunc.statements.isEmpty() shouldBe true
        func.isAsmSubroutine shouldBe false
        func.parameters.single().type shouldBe DataType.ARRAY_UB
        func.statements.isEmpty() shouldBe true
    }

    test("testUwordParameterAndNormalVarIndexedAsArrayWorkAsDirectMemoryRead") {
        val text="""
            main {
              sub thing(uword rr) {
                ubyte xx = rr[1]    ; should still work as var initializer that will be rewritten
                ubyte yy
                yy = rr[2]
                uword other
                ubyte zz = other[3]
              }
            
              sub start() {
                ubyte[] array=[1,2,3]
                thing(array)
              }
            }
        """

        val result = compileText(C64Target, false, text, writeAssembly = true).assertSuccess()
        val module = result.program.toplevelModule
        val block = module.statements.single() as Block
        val thing = block.statements.filterIsInstance<Subroutine>().single {it.name=="thing"}
        block.name shouldBe "main"
        thing.statements.size shouldBe 10          // rr, xx, xx assign, yy, yy assign, other, other assign 0, zz, zz assign, return
        val xx = thing.statements[1] as VarDecl
        withClue("vardecl init values must have been moved to separate assignments") {
            xx.value shouldBe null
        }
        val assignXX = thing.statements[2] as Assignment
        val assignYY = thing.statements[4] as Assignment
        val assignZZ = thing.statements[8] as Assignment
        assignXX.target.identifier!!.nameInSource shouldBe listOf("xx")
        assignYY.target.identifier!!.nameInSource shouldBe listOf("yy")
        assignZZ.target.identifier!!.nameInSource shouldBe listOf("zz")
        val valueXXexpr = (assignXX.value as DirectMemoryRead).addressExpression as BinaryExpression
        val valueYYexpr = (assignYY.value as DirectMemoryRead).addressExpression as BinaryExpression
        val valueZZexpr = (assignZZ.value as DirectMemoryRead).addressExpression as BinaryExpression
        (valueXXexpr.left as IdentifierReference).nameInSource shouldBe listOf("rr")
        (valueYYexpr.left as IdentifierReference).nameInSource shouldBe listOf("rr")
        (valueZZexpr.left as IdentifierReference).nameInSource shouldBe listOf("other")
        (valueXXexpr.right as NumericLiteralValue).number.toInt() shouldBe 1
        (valueYYexpr.right as NumericLiteralValue).number.toInt() shouldBe 2
        (valueZZexpr.right as NumericLiteralValue).number.toInt() shouldBe 3
    }

    test("testUwordParameterAndNormalVarIndexedAsArrayWorkAsMemoryWrite") {
        val text="""
            main {
              sub thing(uword rr) {
                rr[10] = 42
              }
            
              sub start() {
                ubyte[] array=[1,2,3]
                thing(array)
              }
            }
        """

        val result = compileText(C64Target, false, text, writeAssembly = true).assertSuccess()
        val module = result.program.toplevelModule
        val block = module.statements.single() as Block
        val thing = block.statements.filterIsInstance<Subroutine>().single {it.name=="thing"}
        block.name shouldBe "main"
        thing.statements.size shouldBe 3   // "rr, rr assign, return void"
        val assignRR = thing.statements[1] as Assignment
        (assignRR.value as NumericLiteralValue).number.toInt() shouldBe 42
        val memwrite = assignRR.target.memoryAddress
        memwrite shouldNotBe null
        val addressExpr = memwrite!!.addressExpression as BinaryExpression
        (addressExpr.left as IdentifierReference).nameInSource shouldBe listOf("rr")
        addressExpr.operator shouldBe "+"
        (addressExpr.right as NumericLiteralValue).number.toInt() shouldBe 10
    }
})