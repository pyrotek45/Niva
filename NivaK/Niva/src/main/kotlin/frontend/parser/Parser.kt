package frontend.parser

import frontend.meta.Position
import frontend.meta.Token
import frontend.meta.TokenType

// Unari messages
//class OperatorTable (
//    val tokens: List<String> =
//)

data class Module(val name: String, var loaded: Boolean)

class Parser(
    val file: String,
    val tokens: MutableList<Token>,
    val source: String,
//    val lines: MutableList<Position>,

//    val binaryMessages: MutableSet<String> = hashSetOf(),
//    val unaryMessages: MutableSet<String> = hashSetOf(),
//    val keywordMessages: MutableSet<String> = hashSetOf(),
    val currentFunction: Declaration? = null,
    val scopeDepth: Int = 0,
//    val operators: OperatorTable,
    val tree: MutableList<Declaration> = mutableListOf(),
    var current: Int = 0,
    val modules: MutableList<Module> = mutableListOf(),
)


fun Parser.getCurrent() = current
fun Parser.getCurrentToken() =
    if (getCurrent() >= tokens.size - 1 || getCurrent() - 1 < 0)
        tokens.elementAt(tokens.size - 1)
    else
        tokens.elementAt(current - 1)

fun Parser.getSource() = source
fun Parser.getCurrentFunction() = currentFunction
fun endOfFile() = Token(
    kind = TokenType.EndOfFile,
    lexeme = "",
    line = -1,
    pos = Position(-1, -1),
    relPos = Position(-1, -1)
)
//fun endOfLine(msg: String, tok: Token? = null) = expect()


fun Parser.peek(distance: Int = 0): Token =
    // check
    if (tokens.size == 0 || current + distance > tokens.size - 1 || current + distance < 0)
        endOfFile()
    else
        tokens[current + distance]

fun Parser.done(): Boolean =
    peek().kind == TokenType.EndOfFile

fun Parser.step(n: Int = 1): Token {
    val result =
        if (done())
            peek()
        else
            tokens[current]
    current += n
    return result
}

fun Parser.error(message: String, token: Token? = null): Nothing {
    var realToken = token ?: getCurrentToken()
    if (realToken.kind == TokenType.EndOfFile) {
        realToken = peek(-1)
    }
    throw Error("$message\ntoken: $token\nline: ${realToken.line}\nfile: $file\nparser: $this")
}

fun Parser.check(kind: TokenType, distance: Int = 0) =
    peek(distance).kind == kind

fun Parser.check(kind: String, distance: Int = 0) =
    peek(distance).lexeme == kind

fun Parser.check(kind: Iterable<TokenType>): Boolean {
    kind.forEach {
        if (check(it)) {
            return true
        }
    }
    return false
}

fun Parser.checkString(kind: Iterable<String>): Boolean {
    kind.forEach {
        if (check(it)) {
            step()
            return true
        }
    }
    return false
}

fun Parser.match(kind: TokenType) =
    if (check(kind)) {
        step()
        true
    } else {
        false
    }

fun Parser.match(kind: String) =
    if (check(kind)) {
        step() // TODO тут наверн надо делать степ на kind.length
        true
    } else {
        false
    }

fun Parser.match(kind: Iterable<TokenType>): Boolean {
    kind.forEach {
        if (match(it)) {
            return true
        }
    }
    return false
}

fun Parser.matchString(kind: Iterable<String>): Boolean {
    kind.forEach {
        if (match(it)) {
            return true
        }
    }
    return false
}

fun Parser.expect(kind: TokenType, message: String = "", token: Token? = null) {
    if (!match(kind)) {
        if (message.isEmpty()) {
            error("expecting token of kind $kind, found ${peek().kind}", token)
        } else {
            error(message)
        }
    }
}

fun Parser.expect(kind: String, message: String = "", token: Token? = null) {
    if (!match(kind)) {
        if (message.isEmpty()) {
            error("expecting token of kind $kind, found ${peek().kind}", token)
        } else {
            error(message)
        }
    }
}
//fun Parser.expect(kind: Iterable<String>, message: String = "", token: Token? = null) {
//
//}

fun Parser.primary(): Primary? =
    when (peek().kind) {
        TokenType.True -> LiteralExpression.TrueExpr(step())
        TokenType.False -> LiteralExpression.FalseExpr(step())
        TokenType.Integer -> LiteralExpression.IntExpr(step())
        TokenType.Float -> LiteralExpression.FloatExpr(step())
        TokenType.StringToken -> LiteralExpression.StringExpr(step())
        TokenType.Identifier -> {
            val x = step()
            val isTyped = check(TokenType.DoubleColon)
            if (isTyped) {
                step() // skip double colon
                val type = step().lexeme
                IdentifierExpr(x.lexeme, type, x)
            } else {
                IdentifierExpr(x.lexeme, null, x)
            }
        }

//        TokenType.Identifier2Colon -> {
//            val x = step()
//            val q = x.lexeme.split("::")
//            assert(q.count() == 2)
//            val name = q[0]
//            val type = step().lexeme
//            IdentifierExpr(name, type, x)
//        }

        TokenType.LeftParen -> TODO()
        else -> this.error("expected primary, but got ${peek().kind}")
    }


// messageCall | switchExpression
//fun Parser.expression(): Expression {
//    // пока токо инты
//    if (peek().kind == TokenType.Integer) {
//        return primary()
//    } else {
//        TODO()
//    }
//}

// messageCall | switchExpression
fun Parser.expression(): Expression {
    // сначала чекаем это messageCall или switch
    val tok = peek()
    if (tok.kind == TokenType.Pipe) {
        // Switch expr
        TODO()
    }
    // this is message call
    val receiver = receiver()

    return receiver
}

// for now only primary is recievers, no indentifiers or expressions
fun Parser.receiver(): Receiver {
    fun blockConstructor() = null
    fun collectionLiteral() = null

    val tryPrimary = primary() ?: blockConstructor() ?: collectionLiteral() ?: throw Error("bruh")

    return tryPrimary
}

fun Parser.assign(): VarDeclaration {

    val tok = this.step()
    val typeOrEqual = step()

    val value: Expression
    val valueType: String?
    when (typeOrEqual.kind) {
        TokenType.Equal -> {
            value = this.expression()
            valueType = value.type
        }
        // ::^int
        TokenType.DoubleColon -> {
            valueType = step().lexeme
            // x::int^ =
            match(TokenType.Equal)
            value = this.expression()
        }

        else -> error("after ${peek(-1)} needed type or expression")
    }

    val identifierExpr = IdentifierExpr(tok.lexeme, valueType, tok)
    val result = VarDeclaration(tok, identifierExpr, value, valueType)
    return result
}


// тк кк у кейвордов у аргументов могут быть бинарные или унарные, а у бинарных могут быть унарные
// то нужно сначала попробовать распарсить кейвордное
// если не выйдет то бинарное
// если не выйдет то унарное

fun Parser.keywordCall(receiver: Receiver) {
    // key1: expr
    check(TokenType.Colon, 1)

//    val keywordMsg: KeywordMsg = KeywordMsg(receiver,)
}

fun Parser.messageCall(receiver: Receiver): MessageCall {
    // tryParse keyword
    keywordCall(receiver)
    TODO()
}

fun Parser.message(): Message {
    // x echo // identifier
    // 1 echo // primary
    // (1 + 1) echo // parens
    // [1 2 3] // data structure


    val tok = peek()

    val receiver: Receiver = receiver()

    val tok2 = step()

    if (tok2.kind == TokenType.BinarySymbol) {
        return BinaryMsg(receiver, tok2.lexeme, null, tok)
    }

    if (check(TokenType.Colon)) {
        // Keyword
        // parse keyword
        TODO()
    } else {
        // unary
        return UnaryMsg(receiver, tok2.lexeme, null, tok) // TODO inference type from function table here
    }

//    error("cant parse message selector ${tok2.lexeme}")

}

fun TokenType.isPrimeToken() =
    when (this) {
        TokenType.Identifier,
        TokenType.Float,
        TokenType.StringToken,
        TokenType.Integer,
        TokenType.True,
        TokenType.False -> true

        else -> false
    }


// all top level declarations
// type
// messageDecl
// message x echo
// assign x = 1
fun Parser.declaration(): Declaration {
    val tok = peek()
    val kind = tok.kind

    // it could be assignment like x = 1
    // or just message like x echo
    // x = 1
    if (tok.kind == TokenType.Identifier) {
        if (check(TokenType.DoubleColon, 1) || (check(TokenType.Equal, 1))) {
            return assign()
        }
    }
//    if (tok.kind == TokenType.Identifier) {
//        if (check(TokenType.DoubleColon, 1) || (check(TokenType.Equal, 1))) {
//            return assign()
//        }
//    }

    if (kind == TokenType.Type) TODO()

//    if (kind == end)
    return message()
}

fun Parser.parse(): List<Declaration> {

    while (!this.done()) {
        this.tree.add(this.declaration())
        if (check(TokenType.EndOfLine))
            step()
    }

    return this.tree
}

//fun Parser.addBinaryMessage(lexeme: String) {
//    binaryMessages.add(lexeme)
//}
//
//fun Parser.addUnaryMessage(lexeme: String) {
//    unaryMessages.add(lexeme)
//}
//
//fun Parser.addKeywordMessage(lexeme: String) {
//    keywordMessages.add(lexeme)
//}
