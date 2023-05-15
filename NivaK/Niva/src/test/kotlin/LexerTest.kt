import frontend.Lexer
import frontend.lex
import frontend.meta.TokenType
import frontend.meta.TokenType.*
import org.testng.Assert.assertEquals
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
import org.testng.annotations.Test


val helloWorldProgram = """
"Hello w" echo
""".trimIndent()



val functionDeclarationWithType = """
int to: x(int) = [
  code
]
""".trimIndent()

val rawString = """
x = r"string"
""".trimIndent()

class LexerTest {

    @Test
    fun identifierColon() {
        val manyExpr = "sas:"
        check(manyExpr, listOf(Identifier, Colon, EndOfFile))
    }


    @Test
    fun manyExpr() {
        val manyExpr = """
            x sas
            y sus
        """.trimIndent()
        check(manyExpr, listOf(Identifier, Identifier, EndOfLine, Identifier, Identifier, EndOfFile))
    }

    @Test
    fun oneManyLinesExpr() {
        val oneExpr = """x sas: 1
  .ses: 2
x sas
"""
        // there no end of line after "sas" because there end of file
        check(
            oneExpr,
            listOf(
                Identifier,
                Identifier,
                Colon,
                Integer,
                Dot,
                Identifier,
                Colon,
                Integer,
                EndOfLine,
                Identifier,
                Identifier,
                EndOfFile
            )
        )
    }

    @Test
    fun emptySource() {
        check("", listOf(EndOfFile))
    }

    @Test
    fun string() {
        check("\"sas\"", listOf(StringToken, EndOfFile))
    }

    @Test
    fun helloWorld() {
        check(helloWorldProgram, listOf(StringToken, Identifier, EndOfFile))
    }

    @Test
    fun createVariable() {
        check("x = 42", listOf(Identifier, Equal, Integer, EndOfFile))
    }

    @Test
    fun typedVar() {
        check("x::int", listOf(Identifier, DoubleColon, Identifier, EndOfFile))
    }

    @Test
    fun sass() {
        check("|=>", listOf(Else, EndOfFile))
    }

    @Test
    fun singleIdentifier() {
        check("sas", listOf(Identifier, EndOfFile))
    }

    @Test
    fun rawString() {
        check(rawString, listOf(Identifier, Equal, StringToken, EndOfFile))
    }

    @Test
    fun functionDeclarationWithBody() {

        val functionDeclaration = """
int to: x = [
  x echo
]
""".trimIndent()
        check(
            functionDeclaration,
            listOf(
                Identifier, Identifier, Colon, Identifier, Equal, LeftBracket, EndOfLine,
                Identifier, Identifier, EndOfLine,
                RightBracket,
                EndOfFile
            )
        )
    }

    @Test
    fun brackets() {
        check("{} () []", listOf(LeftParen, RightParen, LeftBrace, RightBrace, LeftBracket, RightBracket, EndOfFile))
    }

    @Test
    fun keywords() {
        check("true false type use union ", listOf(True, False, Type, Use, Union, EndOfFile))
    }

    @Test
    fun hardcodedBinarySymbols() {
        check(
            "^ |> | |=> = ::",
            listOf(Return, Pipe, BinarySymbol, Pipe, Else, Equal, DoubleColon, EndOfFile)
        )
    }

    @Test
    fun punctuation() {
        check(". ; , : ", listOf(Dot, Semicolon, Comma, Colon, EndOfFile))
    }

    private fun check(source: String, tokens: List<TokenType>, showTokens: Boolean = true) {
        val lexer = Lexer(source, "sas")
//        lexer.fillSymbolTable()
        val result = lexer.lex().map { it.kind }
        assertEquals(tokens, result)
        if (showTokens) {
            println("$result")
        }
//        if (tokens != result) {
//            throw Throwable("\n\ttokens: $tokens\n\tresult: $result")
//        }
    }
}