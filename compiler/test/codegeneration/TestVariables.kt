package prog8tests.codegeneration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import prog8.ast.statements.Assignment
import prog8.ast.statements.AssignmentOrigin
import prog8.code.ast.PtAssignment
import prog8.code.target.C64Target
import prog8tests.helpers.ErrorReporterForTests
import prog8tests.helpers.compileText


class TestVariables: FunSpec({

    test("shared variables without refs not removed for inlined asm") {
        val text = """
            main {
                sub start() {
                    ubyte[] @shared arrayvar = [1,2,3,4]
                    str @shared stringvar = "test"
                    ubyte @shared bytevar = 0
            
                    %asm {{
                        lda  p8v_arrayvar
                        lda  p8v_stringvar
                        lda  p8v_bytevar
                    }}
                }
            }
        """
        compileText(C64Target(), true, text, writeAssembly = true) shouldNotBe null
    }

    test("array initialization with array literal") {
        val text = """
            main {
                sub start() {
                    ubyte[] @shared arrayvar = [1,2,3,4]
                }
            }
        """
        compileText(C64Target(), true, text, writeAssembly = true) shouldNotBe null
    }

    test("pipe character in string literal") {
        val text = """
            main {
                sub start() {
                    str name = "first|second"
                    str name2 = "first | second"
                }
            }
        """
        compileText(C64Target(), false, text, writeAssembly = true) shouldNotBe null
    }
    
    test("negation of unsigned via casts") {
        val text = """
            main {
                sub start() {
                    cx16.r0L = -(cx16.r0L as byte) as ubyte
                    cx16.r0 = -(cx16.r0 as word) as uword
                    ubyte ub
                    uword uw
                    ub = -(ub as byte) as ubyte
                    uw = -(uw as word) as uword
                }
            }
        """
        compileText(C64Target(), false, text, writeAssembly = true) shouldNotBe null
    }

    test("initialization of boolean array with array") {
        val text = """
            main {
                sub start() {
                    bool[3] sieve0 = [true, false, true]
                }
            }
        """
        compileText(C64Target(), false, text, writeAssembly = true) shouldNotBe null
    }

    test("initialization of boolean array with wrong array type should fail") {
        val text = """
            main {
                sub start() {
                    bool[] sieve0 = [true, false, 1]
                    bool[] sieve1 = [true, false, 42]
                }
            }
        """
        val errors = ErrorReporterForTests()
        compileText(C64Target(), false, text, writeAssembly = true, errors=errors) shouldBe null
        errors.errors.size shouldBe 2
        errors.errors[0] shouldContain "value has incompatible type"
        errors.errors[1] shouldContain "value has incompatible type"
    }

    test("global var init with array lookup should sometimes be const") {
        val src="""
main {

    bool[] barray =  [true, false, true, false]
    uword[] warray = [&value1, &barray, &value5, 4242]

    bool @shared value1
    bool @shared value2 = barray[2]         ; should be const!
    bool @shared value3 = true
    bool @shared value4 = false
    bool @shared value5 = barray[cx16.r0L]      ; cannot be const
    uword @shared value6 = warray[3]        ; should be const!
    uword @shared value7 = warray[2]        ; cannot be const

    sub start() {
    }
}"""

        val result = compileText(C64Target(), true, src, writeAssembly = true)!!.compilerAst
        val main = result.allBlocks.first { it.name=="main" }
        main.statements.size shouldBe 17
        val assigns = main.statements.filterIsInstance<Assignment>()
        assigns[0].target.identifier?.nameInSource shouldBe listOf("value1")
        assigns[1].target.identifier?.nameInSource shouldBe listOf("value2")
        assigns[2].target.identifier?.nameInSource shouldBe listOf("value3")
        assigns[3].target.identifier?.nameInSource shouldBe listOf("value4")
        assigns[4].target.identifier?.nameInSource shouldBe listOf("value5")
        assigns[5].target.identifier?.nameInSource shouldBe listOf("value6")
        assigns[6].target.identifier?.nameInSource shouldBe listOf("value7")
        assigns[0].origin shouldBe AssignmentOrigin.VARINIT
        assigns[1].origin shouldBe AssignmentOrigin.VARINIT
        assigns[2].origin shouldBe AssignmentOrigin.VARINIT
        assigns[3].origin shouldBe AssignmentOrigin.VARINIT
        assigns[4].origin shouldBe AssignmentOrigin.VARINIT
        assigns[5].origin shouldBe AssignmentOrigin.VARINIT
        assigns[6].origin shouldBe AssignmentOrigin.VARINIT
        assigns[0].value.constValue(result)?.number shouldBe 0
        assigns[1].value.constValue(result)?.number shouldBe 1
        assigns[2].value.constValue(result)?.number shouldBe 1
        assigns[3].value.constValue(result)?.number shouldBe 0
        assigns[4].value.constValue(result) shouldBe null
        assigns[5].value.constValue(result)?.number shouldBe 4242
        assigns[6].value.constValue(result) shouldBe null
    }


    test("scoped var init with array lookup should never be const") {
        val src="""
main {
    sub start() {
        bool[] barray =  [true, false, true, false]
        uword[] warray = [&value1, &barray, &value5, 4242]
    
        bool @shared value1
        bool @shared value2 = barray[2]
        bool @shared value3 = true
        bool @shared value4 = false
        bool @shared value5 = barray[cx16.r0L] 
        uword @shared value6 = warray[3]
        uword @shared value7 = warray[2]
    }
}"""

        val result = compileText(C64Target(), true, src, writeAssembly = true)!!.compilerAst
        val st = result.entrypoint.statements
        st.size shouldBe 17
        val assigns = st.filterIsInstance<Assignment>()
        assigns[0].target.identifier?.nameInSource shouldBe listOf("value1")
        assigns[1].target.identifier?.nameInSource shouldBe listOf("value2")
        assigns[2].target.identifier?.nameInSource shouldBe listOf("value3")
        assigns[3].target.identifier?.nameInSource shouldBe listOf("value4")
        assigns[4].target.identifier?.nameInSource shouldBe listOf("value5")
        assigns[5].target.identifier?.nameInSource shouldBe listOf("value6")
        assigns[6].target.identifier?.nameInSource shouldBe listOf("value7")
        assigns[0].origin shouldBe AssignmentOrigin.VARINIT
        assigns[1].origin shouldBe AssignmentOrigin.VARINIT
        assigns[2].origin shouldBe AssignmentOrigin.VARINIT
        assigns[3].origin shouldBe AssignmentOrigin.VARINIT
        assigns[4].origin shouldBe AssignmentOrigin.VARINIT
        assigns[5].origin shouldBe AssignmentOrigin.VARINIT
        assigns[6].origin shouldBe AssignmentOrigin.VARINIT
        assigns[0].value.constValue(result)?.number shouldBe 0
        assigns[1].value.constValue(result) shouldBe null
        assigns[2].value.constValue(result)?.number shouldBe 1
        assigns[3].value.constValue(result)?.number shouldBe 0
        assigns[4].value.constValue(result) shouldBe null
        assigns[5].value.constValue(result) shouldBe null
        assigns[6].value.constValue(result) shouldBe null
    }
})
