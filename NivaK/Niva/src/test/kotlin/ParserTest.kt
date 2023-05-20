import frontend.parser.*
import org.testng.annotations.Test

class ParserTest {
    @Test
    fun varDeclaration() {
        val source = "x::int = 1"
        val ast = getAst(source)
        println("ast.count = ${ast.count()}")
        assert(ast.count() == 1)
        println("ast = $ast")
        println("ast[0] = ${ast[0]}")

        val declaration: VarDeclaration = ast[0] as VarDeclaration
        assert(declaration.name.str == "x")
        assert(declaration.value.type == "int")
        assert(declaration.value.str == "1")
    }

    @Test
    fun varDeclWithTypeInfer() {
        val source = "x = 1"
        val ast = getAst(source)
        assert(ast.count() == 1)

        val declaration: VarDeclaration = ast[0] as VarDeclaration
        assert(declaration.name.str == "x")
        assert(declaration.value.type == "int")
        assert(declaration.value.str == "1")
    }

    @Test
    fun string() {
        val source = "\"sas\""
        val ast = getAst(source)
        assert(ast.count() == 1)

        val declaration = ast[0] as UnaryMsg

        assert(declaration.str == "\"sas\"")
    }


    @Test
    fun helloWorld() {
        val source = "\"sas\" echo"
        val ast = getAst(source)
        assert(ast.count() == 1)

        val unaryMsg = ast[0] as UnaryMsg

        assert(unaryMsg.selectorName == "echo")
        assert(unaryMsg.receiver.type == "string")
        assert(unaryMsg.receiver.str == "\"sas\"")
    }


    @Test
    fun varDeclAndUnaryMsg() {
        val source = """
        x = 1
        x echo
    """.trimIndent()
        val ast = getAst(source)
        assert(ast.count() == 2)

        val declaration = ast[0] as VarDeclaration
        val unaryMsg = ast[1] as UnaryMsg
        assert(declaration.name.str == "x")
        assert(declaration.value.type == "int")
        assert(declaration.value.str == "1")

        assert(unaryMsg.selectorName == "echo")
//        assert(unaryMsg.receiver.type == "int")
        assert(unaryMsg.receiver.str == "x")
    }

    @Test
    fun twoUnary() {
        val source = "3 inc inc"
        val ast = getAst(source)
        assert(ast.count() == 1)

        val unary: MessageCall = ast[0] as MessageCall
        assert(unary.messages.count() == 2)
        assert(unary.messages[0].selectorName == "inc")
        assert(unary.messages[1].selectorName == "inc")
        assert(unary.messages[1].receiver.type == "int")
    }

    @Test
    fun binaryWithUnary() {
        // inc(inc(3)) + dec(dec(2))
        val source = "3 inc inc + 2 dec dec"
        val ast = getAst(source)
        assert(ast.count() == 1)

        val unary: MessageCall = ast[0] as MessageCall
        val messages = unary.messages
        assert(messages.count() == 5)

        assert(messages[0].selectorName == "inc")
        assert(messages[1].selectorName == "inc")
        assert(messages[2].selectorName == "dec")
        assert(messages[3].selectorName == "dec")
        assert(messages[4].selectorName == "+")

        assert(messages[0].receiver.str == "3")
        assert(messages[1].receiver.str == "3")
        assert(messages[2].receiver.str == "2")
        assert(messages[3].receiver.str == "2")
        assert(messages[4].receiver.str == "2")
    }

//    @Test
//    fun binaryMsg() {
//        val source = "2 + 1 - 3"
//        val ast = declarationList(source)
//        assert(ast.count() == 2)
//
//        val binaryMsg = ast[0] as BinaryMsg
//
//        assert(binaryMsg.selectorName == "+")
//        assert(binaryMsg.receiver.type == "int")
//        assert(binaryMsg.receiver.str == "2")
//
//        val binaryMsg2 = ast[0] as BinaryMsg
//    }


}

fun getAst(source: String): List<Declaration> {
    val tokens = lex(source)
    val parser = Parser(file = "", tokens = tokens, source = "sas.niva")
    val ast = parser.declarations()
    return ast
}
