package frontend.parser.parsing

import frontend.meta.Position
import frontend.meta.Token
import frontend.meta.TokenType
import frontend.meta.isIdentifier
import frontend.parser.types.ast.Statement

class Parser(
    val file: String,
    val tokens: MutableList<Token>,
    val source: String,
    val currentFunction: Statement? = null,
    val scopeDepth: Int = 0,
//    val operators: OperatorTable,
    val tree: MutableList<Statement> = mutableListOf(),
    var current: Int = 0,
    val modules: MutableList<Module> = mutableListOf(),
)

fun Parser.getCurrent() = current
fun Parser.getCurrentToken() =
    if (getCurrent() >= tokens.size - 1 || getCurrent() - 1 < 0)
        tokens.elementAt(tokens.size - 1)
    else
        tokens.elementAt(current - 1)

fun Parser.getCurrentFunction() = currentFunction
fun endOfFile() = Token(
    kind = TokenType.EndOfFile,
    lexeme = "",
    line = -1,
    pos = Position(-1, -1),
    relPos = Position(-1, -1)
)

fun Parser.peek(distance: Int = 0): Token =
    // check
    if (tokens.size == 0 || current + distance > tokens.size - 1 || current + distance < 0)
        endOfFile()
    else
        tokens[current + distance]

fun Parser.done(): Boolean =
    check(TokenType.EndOfFile)

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

fun Parser.matchAssertAnyIdent(errorMessage: String): Token {
    val tok = peek()

    return if (tok.isIdentifier() || tok.kind == TokenType.NullableIdentifier) {
        step()
        tok
    } else {
        error(errorMessage)
    }
}

fun Parser.matchAssert(kind: TokenType, errorMessage: String): Token {
    val tok = peek()

    return if (tok.kind == kind) {
        step()
        tok
    } else {
        error(errorMessage)
    }
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