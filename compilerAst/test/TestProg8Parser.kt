package prog8tests

import prog8tests.helpers.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import kotlin.test.*
import kotlin.io.path.*

import prog8.parser.ParseError
import prog8.parser.Prog8Parser.parseModule
import prog8.parser.SourceCode
import prog8.ast.*
import prog8.ast.statements.*
import prog8.ast.base.Position
import prog8.ast.expressions.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestProg8Parser {

    @Test
    fun testModuleSourceNeedNotEndWithNewline() {
        val nl = "\n" // say, Unix-style (different flavours tested elsewhere)
        val src = SourceCode.of("foo {" + nl + "}")   // source ends with '}' (= NO newline, issue #40)

        // #45: Prog8ANTLRParser would report (throw) "missing <EOL> at '<EOF>'"
        val module = parseModule(src)
        assertEquals(1, module.statements.size)
    }

    @Test
    fun testModuleSourceMayEndWithNewline() {
        val nl = "\n" // say, Unix-style (different flavours tested elsewhere)
        val srcText = "foo {" + nl + "}" + nl  // source does end with a newline (issue #40)
        val module = parseModule(SourceCode.of(srcText))
        assertEquals(1, module.statements.size)
    }

    @Test
    fun testAllBlocksButLastMustEndWithNewline() {
        val nl = "\n" // say, Unix-style (different flavours tested elsewhere)

        // BAD: 2nd block `bar` does NOT start on new line; however, there's is a nl at the very end
        val srcBad = "foo {" + nl + "}" + " bar {" + nl + "}" + nl

        // GOOD: 2nd block `bar` does start on a new line; however, a nl at the very end ain't needed
        val srcGood = "foo {" + nl + "}" + nl + "bar {" + nl + "}"

        assertFailsWith<ParseError> { parseModule(SourceCode.of(srcBad)) }
        val module = parseModule(SourceCode.of(srcGood))
        assertEquals(2, module.statements.size)
    }

    @Test
    fun testWindowsAndMacNewlinesAreAlsoFine() {
        val nlWin = "\r\n"
        val nlUnix = "\n"
        val nlMac = "\r"

        //parseModule(Paths.get("test", "fixtures", "mac_newlines.p8").toAbsolutePath())

        // a good mix of all kinds of newlines:
        val srcText =
            "foo {" +
            nlMac +
            nlWin +
            "}" +
            nlMac +     // <-- do test a single \r (!) where an EOL is expected
            "bar {" +
            nlUnix +
            "}" +
            nlUnix + nlMac   // both should be "eaten up" by just one EOL token
            "combi {" +
            nlMac + nlWin + nlUnix   // all three should be "eaten up" by just one EOL token
            "}" +
            nlUnix      // end with newline (see testModuleSourceNeedNotEndWithNewline)

        val module = parseModule(SourceCode.of(srcText))
        assertEquals(2, module.statements.size)
    }

    @Test
    fun testInterleavedEolAndCommentBeforeFirstBlock() {
        // issue: #47
        val srcText = """
            ; comment
            
            ; comment
            
            blockA {            
            }
"""
        val module = parseModule(SourceCode.of(srcText))
        assertEquals(1, module.statements.size)
    }

    @Test
    fun testInterleavedEolAndCommentBetweenBlocks() {
        // issue: #47
        val srcText = """
            blockA {
            }
            ; comment
            
            ; comment
            
            blockB {            
            }
"""
        val module = parseModule(SourceCode.of(srcText))
        assertEquals(2, module.statements.size)
    }

    @Test
    fun testInterleavedEolAndCommentAfterLastBlock() {
        // issue: #47
        val srcText = """
            blockA {            
            }
            ; comment
            
            ; comment
            
"""
        val module = parseModule(SourceCode.of(srcText))
        assertEquals(1, module.statements.size)
    }

    @Test
    fun testNewlineBetweenTwoBlocksOrDirectivesStillRequired() {
        // issue: #47

        // block and block
        assertFailsWith<ParseError>{ parseModule(SourceCode.of("""
            blockA {
            } blockB {            
            }            
        """)) }

        // block and directive
        assertFailsWith<ParseError>{ parseModule(SourceCode.of("""
            blockB {            
            } %import textio            
        """)) }

        // The following two are bogus due to directive *args* expected to follow the directive name.
        // Leaving them in anyways.

        // dir and block
        assertFailsWith<ParseError>{ parseModule(SourceCode.of("""
            %import textio blockB {            
            }            
        """)) }

        assertFailsWith<ParseError>{ parseModule(SourceCode.of("""
            %import textio %import syslib            
        """)) }
    }

    @Test
    fun parseModuleShouldNotLookAtImports() {
        val importedNoExt = assumeNotExists(fixturesDir, "i_do_not_exist")
        assumeNotExists(fixturesDir, "i_do_not_exist.p8")
        val text = "%import ${importedNoExt.name}"
        val module = parseModule(SourceCode.of(text))

        assertEquals(1, module.statements.size)
    }


    @Test
    fun testParseModuleWithEmptyString() {
        val module = parseModule(SourceCode.of(""))
        assertEquals(0, module.statements.size)
    }

    @Test
    fun testParseModuleWithEmptyFile() {
        val path = assumeReadableFile(fixturesDir,"empty.p8")
        val module = parseModule(SourceCode.fromPath(path))
        assertEquals(0, module.statements.size)
    }

    @Test
    fun testModuleNameForSourceFromString() {
        val srcText = """
            main {
            }
        """.trimIndent()
        val module = parseModule(SourceCode.of(srcText))

        // Note: assertContains has *actual* as first param
        assertContains(module.name, Regex("^anonymous_[0-9a-f]+$"))
    }

    @Test
    fun testModuleNameForSourceFromPath() {
        val path = assumeReadableFile(fixturesDir,"simple_main.p8")
        val module = parseModule(SourceCode.fromPath(path))
        assertEquals(path.nameWithoutExtension, module.name)
    }


    fun assertPosition(actual: Position, expFile: String? = null, expLine: Int? = null, expStartCol: Int? = null, expEndCol: Int? = null) {
        require(!listOf(expLine, expStartCol, expEndCol).all { it == null })
        if (expLine != null) assertEquals(expLine, actual.line, ".position.line (1-based)")
        if (expStartCol != null) assertEquals(expStartCol, actual.startCol, ".position.startCol (0-based)" )
        if (expEndCol != null) assertEquals(expEndCol, actual.endCol, ".position.endCol (0-based)")
        if (expFile != null) assertEquals(expFile, actual.file, ".position.file")
    }

    fun assertPosition(actual: Position, expFile: Regex? = null, expLine: Int? = null, expStartCol: Int? = null, expEndCol: Int? = null) {
        require(!listOf(expLine, expStartCol, expEndCol).all { it == null })
        if (expLine != null) assertEquals(expLine, actual.line, ".position.line (1-based)")
        if (expStartCol != null) assertEquals(expStartCol, actual.startCol, ".position.startCol (0-based)" )
        if (expEndCol != null) assertEquals(expEndCol, actual.endCol, ".position.endCol (0-based)")
        // Note: assertContains expects *actual* value first
        if (expFile != null) assertContains(actual.file, expFile, ".position.file")
    }

    fun assertPositionOf(actual: Node, expFile: String? = null, expLine: Int? = null, expStartCol: Int? = null, expEndCol: Int? = null) =
        assertPosition(actual.position, expFile, expLine, expStartCol, expEndCol)

    fun assertPositionOf(actual: Node, expFile: Regex? = null, expLine: Int? = null, expStartCol: Int? = null, expEndCol: Int? = null) =
        assertPosition(actual.position, expFile, expLine, expStartCol, expEndCol)


    @Test
    fun testErrorLocationForSourceFromString() {
        val srcText = "bad * { }\n"

        assertFailsWith<ParseError> { parseModule(SourceCode.of(srcText)) }
        try {
            parseModule(SourceCode.of(srcText))
        } catch (e: ParseError) {
            assertPosition(e.position, Regex("^<String@[0-9a-f]+>$"), 1, 4, 4)
        }
    }

    @Test
    fun testErrorLocationForSourceFromPath() {
        val path = assumeReadableFile(fixturesDir, "file_with_syntax_error.p8")

        assertFailsWith<ParseError> { parseModule(SourceCode.fromPath(path)) }
        try {
            parseModule(SourceCode.fromPath(path))
        } catch (e: ParseError) {
            assertPosition(e.position, path.absolutePathString(), 2, 6) // TODO: endCol wrong
        }
    }

    @Test
    fun testModulePositionForSourceFromString() {
        val srcText = """
            main {
            }
        """.trimIndent()
        val module = parseModule(SourceCode.of(srcText))
        assertPositionOf(module, Regex("^<String@[0-9a-f]+>$"), 1, 0) // TODO: endCol wrong
    }

    @Test
    fun testModulePositionForSourceFromPath() {
        val path = assumeReadableFile(fixturesDir,"simple_main.p8")

        val module = parseModule(SourceCode.fromPath(path))
        assertPositionOf(module, path.absolutePathString(), 1, 0) // TODO: endCol wrong
    }

    @Test
    fun testInnerNodePositionsForSourceFromPath() {
        val path = assumeReadableFile(fixturesDir,"simple_main.p8")

        val module = parseModule(SourceCode.fromPath(path))
        val mpf = module.position.file

        assertPositionOf(module, path.absolutePathString(), 1, 0) // TODO: endCol wrong
        val mainBlock = module.statements.filterIsInstance<Block>()[0]
        assertPositionOf(mainBlock, mpf, 1, 0)  // TODO: endCol wrong!
        val startSub = mainBlock.statements.filterIsInstance<Subroutine>()[0]
        assertPositionOf(startSub, mpf, 2, 4)  // TODO: endCol wrong!
    }

    /**
     * TODO: this test is testing way too much at once
     */
    @Test
    @Disabled("TODO: fix .position of nodes below Module - step 8, 'refactor AST gen'")
    fun testInnerNodePositionsForSourceFromString() {
        val srcText = """
            %target 16, "abc" ; DirectiveArg directly inherits from Node - neither an Expression nor a Statement..?
            main {
                sub start() {
                    ubyte foo = 42
                    ubyte bar
                    when (foo) {
                        23 -> bar = 'x' ; WhenChoice, also directly inheriting Node
                        42 -> bar = 'y'
                        else -> bar = 'z'
                    }
                }
            }
        """.trimIndent()
        val module = parseModule(SourceCode.of(srcText))
        val mpf = module.position.file

        val targetDirective = module.statements.filterIsInstance<Directive>()[0]
        assertPositionOf(targetDirective, mpf, 1, 0)  // TODO: endCol wrong!
        val mainBlock = module.statements.filterIsInstance<Block>()[0]
        assertPositionOf(mainBlock, mpf, 2, 0)  // TODO: endCol wrong!
        val startSub = mainBlock.statements.filterIsInstance<Subroutine>()[0]
        assertPositionOf(startSub, mpf, 3, 4)  // TODO: endCol wrong!
        val declFoo = startSub.statements.filterIsInstance<VarDecl>()[0]
        assertPositionOf(declFoo, mpf, 4, 8)  // TODO: endCol wrong!
        val rhsFoo = declFoo.value!!
        assertPositionOf(rhsFoo, mpf, 4, 20)  // TODO: endCol wrong!
        val declBar = startSub.statements.filterIsInstance<VarDecl>()[1]
        assertPositionOf(declBar, mpf, 5, 8)  // TODO: endCol wrong!
        val whenStmt = startSub.statements.filterIsInstance<WhenStatement>()[0]
        assertPositionOf(whenStmt, mpf, 6, 8)  // TODO: endCol wrong!
        assertPositionOf(whenStmt.choices[0], mpf, 7, 12)  // TODO: endCol wrong!
        assertPositionOf(whenStmt.choices[1], mpf, 8, 12)  // TODO: endCol wrong!
        assertPositionOf(whenStmt.choices[2], mpf, 9, 12)  // TODO: endCol wrong!
    }

    @Test
    fun testCharLitAsArg() {
        val src = SourceCode.of("""
             main {
                sub start() {
                    chrout('\n')
                }
            }
        """)
        val module = parseModule(src)

        val startSub = module
            .statements.filterIsInstance<Block>()[0]
            .statements.filterIsInstance<Subroutine>()[0]
        val funCall = startSub.statements.filterIsInstance<IFunctionCall>().first()

        assertIs<CharLiteral>(funCall.args[0])
        val char = funCall.args[0] as CharLiteral
        assertEquals('\n', char.value)
    }

    @Test
    fun testBlockLevelVarDeclWithCharLiteral_noAltEnc() {
        val src = SourceCode.of("""
            main {
                ubyte c = 'x'
            }
        """)
        val module = parseModule(src)
        val decl = module
            .statements.filterIsInstance<Block>()[0]
            .statements.filterIsInstance<VarDecl>()[0]

        val rhs = decl.value as CharLiteral
        assertEquals('x', rhs.value, "char literal's .value")
        assertEquals(false, rhs.altEncoding, "char literal's .altEncoding")
    }

    @Test
    fun testBlockLevelConstDeclWithCharLiteral_withAltEnc() {
        val src = SourceCode.of("""
            main {
                const ubyte c = @'x'
            }
        """)
        val module = parseModule(src)
        val decl = module
            .statements.filterIsInstance<Block>()[0]
            .statements.filterIsInstance<VarDecl>()[0]

        val rhs = decl.value as CharLiteral
        assertEquals('x', rhs.value, "char literal's .value")
        assertEquals(true, rhs.altEncoding, "char literal's .altEncoding")
    }

    @Test
    fun testSubRoutineLevelVarDeclWithCharLiteral_noAltEnc() {
        val src = SourceCode.of("""
            main {
                sub start() {
                    ubyte c = 'x'
                }
            }
        """)
        val module = parseModule(src)
        val decl = module
            .statements.filterIsInstance<Block>()[0]
            .statements.filterIsInstance<Subroutine>()[0]
            .statements.filterIsInstance<VarDecl>()[0]

        val rhs = decl.value as CharLiteral
        assertEquals('x', rhs.value, "char literal's .value")
        assertEquals(false, rhs.altEncoding, "char literal's .altEncoding")
    }

    @Test
    fun testSubRoutineLevelConstDeclWithCharLiteral_withAltEnc() {
        val src = SourceCode.of("""
            main {
                sub start() {
                    const ubyte c = @'x'
                }
            }
        """)
        val module = parseModule(src)
        val decl = module
            .statements.filterIsInstance<Block>()[0]
            .statements.filterIsInstance<Subroutine>()[0]
            .statements.filterIsInstance<VarDecl>()[0]

        val rhs = decl.value as CharLiteral
        assertEquals('x', rhs.value, "char literal's .value")
        assertEquals(true, rhs.altEncoding, "char literal's .altEncoding")
    }


    @Test
    fun testForloop() {
        val module = parseModule(SourceCode.of("""
            main {
                sub start() {
                    ubyte ub
                    for ub in "start" downto "end" {    ; #0
                    }
                    for ub in "something" {             ; #1
                    }
                    for ub in @'a' to 'f' {             ; #2
                    }
                    for ub in false to true {           ; #3
                    }
                    for ub in 9 to 1 {                  ; #4 - yes, *parser* should NOT check!
                    }
                }
            }
        """))
        val iterables = module
            .statements.filterIsInstance<Block>()[0]
            .statements.filterIsInstance<Subroutine>()[0]
            .statements.filterIsInstance<ForLoop>()
            .map { it.iterable }

        assertEquals(5, iterables.size)

        val it0 = iterables[0] as RangeExpr
        assertIs<StringLiteralValue>(it0.from, "parser should leave it as is")
        assertIs<StringLiteralValue>(it0.to, "parser should leave it as is")

        val it1 = iterables[1] as StringLiteralValue
        assertEquals("something", it1.value, "parser should leave it as is")

        val it2 = iterables[2] as RangeExpr
        assertIs<CharLiteral>(it2.from, "parser should leave it as is")
        assertIs<CharLiteral>(it2.to, "parser should leave it as is")

        val it3 = iterables[3] as RangeExpr
        // TODO: intro BoolLiteral
        assertIs<NumericLiteralValue>(it3.from, "parser should leave it as is")
        assertIs<NumericLiteralValue>(it3.to, "parser should leave it as is")

        val it4 = iterables[4] as RangeExpr
        assertIs<NumericLiteralValue>(it4.from, "parser should leave it as is")
        assertIs<NumericLiteralValue>(it4.to, "parser should leave it as is")
    }
}
