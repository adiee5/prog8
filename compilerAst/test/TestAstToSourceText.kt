package prog8tests.ast

import io.kotest.assertions.fail
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.string.shouldContain
import prog8.ast.AstToSourceTextConverter
import prog8.ast.Module
import prog8.ast.Program
import prog8.ast.internedStringsModuleName
import prog8.parser.ParseError
import prog8.parser.Prog8Parser.parseModule
import prog8.parser.SourceCode
import prog8tests.ast.helpers.DummyFunctions
import prog8tests.ast.helpers.DummyMemsizer
import prog8tests.ast.helpers.DummyStringEncoder


class TestAstToSourceText: AnnotationSpec() {

    private fun generateP8(module: Module) : String {
        val program = Program("test", DummyFunctions, DummyMemsizer, DummyStringEncoder)
            .addModule(module)

        var generatedText = ""
        val it = AstToSourceTextConverter({ str -> generatedText += str }, program)
        it.visit(program)

        return generatedText
    }

    private fun roundTrip(module: Module): Pair<String, Module> {
        val generatedText = generateP8(module)
        try {
            val parsedAgain = parseModule(SourceCode.Text(generatedText))
            return Pair(generatedText, parsedAgain)
        } catch (e: ParseError) {
            fail("should produce valid Prog8 but threw $e")
        }
    }

    @Test
    fun testMentionsInternedStringsModule() {
        val orig = SourceCode.Text("\n")
        val (txt, _) = roundTrip(parseModule(orig))
        txt shouldContain Regex(";.*$internedStringsModuleName")
    }

    @Test
    fun testImportDirectiveWithLib() {
        val orig = SourceCode.Text("%import textio\n")
        val (txt, _) = roundTrip(parseModule(orig))
        txt shouldContain Regex("%import +textio")
    }

    @Test
    fun testImportDirectiveWithUserModule() {
        val orig = SourceCode.Text("%import my_own_stuff\n")
        val (txt, _) = roundTrip(parseModule(orig))
        txt shouldContain Regex("%import +my_own_stuff")
    }


    @Test
    fun testStringLiteral_noAlt() {
        val orig = SourceCode.Text("""
            main {
                str s = "fooBar\n"
            }
        """)
        val (txt, _) = roundTrip(parseModule(orig))
        txt shouldContain Regex("str +s += +\"fooBar\\\\n\"")
    }

    @Test
    fun testStringLiteral_withAlt() {
        val orig = SourceCode.Text("""
            main {
                str sAlt = @"fooBar\n"
            }
        """)
        val (txt, _) = roundTrip(parseModule(orig))
        txt shouldContain Regex("str +sAlt += +@\"fooBar\\\\n\"")
    }

    @Test
    fun testCharLiteral_noAlt() {
        val orig = SourceCode.Text("""
            main {
                ubyte c = 'x'
            }
        """)
        val (txt, _) = roundTrip(parseModule(orig))
        txt shouldContain Regex("ubyte +c += +'x'")
    }

    @Test
    fun testCharLiteral_withAlt() {
        val orig = SourceCode.Text("""
            main {
                ubyte cAlt = @'x'
            }
        """)
        val (txt, _) = roundTrip(parseModule(orig))
        txt shouldContain Regex("ubyte +cAlt += +@'x'")
    }

}