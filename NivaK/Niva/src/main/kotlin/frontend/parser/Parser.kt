package frontend.parser

import frontend.meta.Position
import frontend.meta.Token
import frontend.meta.TokenType
import frontend.parser.types.*

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
) {
    val lookahead: TokenType
        get() = peekKind()

}


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

inline fun Parser.peekKind(distance: Int = 0) =
    peek(distance).kind

fun Parser.done(): Boolean =
    peekKind() == TokenType.EndOfFile

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
            error("expecting token of kind $kind, found ${peekKind()}", token)
        } else {
            error(message)
        }
    }
}

fun Parser.expect(kind: String, message: String = "", token: Token? = null) {
    if (!match(kind)) {
        if (message.isEmpty()) {
            error("expecting token of kind $kind, found ${peekKind()}", token)
        } else {
            error(message)
        }
    }
}
//fun Parser.expect(kind: Iterable<String>, message: String = "", token: Token? = null) {
//
//}

fun Parser.primary(): Primary =
    when (peekKind()) {
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
                IdentifierExpr(x.lexeme, null, x) // look for type in table
            }
        }

        TokenType.LeftParen -> TODO()
        else -> this.error("expected primary, but got ${peekKind()}")
    }


// messageCall | switchExpression
//fun Parser.expression(): Expression {
//    // пока токо инты
//    if (peekKind() == TokenType.Integer) {
//        return primary()
//    } else {
//        TODO()
//    }
//}


// for now only primary is recievers, no indentifiers or expressions
fun Parser.receiver(): Receiver {
    fun blockConstructor() = null
    fun collectionLiteral() = null

    val tryPrimary = primary() ?: blockConstructor() ?: collectionLiteral() ?: throw Error("bruh")

    return tryPrimary
}

fun Parser.varDeclaration(): VarDeclaration {

    val tok = this.step()
    val typeOrEqual = step()

    val value: Expression
    val valueType: String?
    when (typeOrEqual.kind) {
        TokenType.Equal -> {
            value = this.receiver()
            valueType = value.type
        }
        // ::^int
        TokenType.DoubleColon -> {
            valueType = step().lexeme
            // x::int^ =
            match(TokenType.Equal)
            value = this.receiver()
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


fun Parser.message(): MessageCall {
    // x echo // identifier
    // 1 echo // primary
    // (1 + 1) echo // parens
    // [1 2 3] // data structure
    val receiver: Receiver = receiver()

    val x: MessageCall = anyMessageCall(receiver)

    return x

}


fun Parser.getAllUnaries(receiver: Receiver): MutableList<UnaryMsg> {
    val unaryMessages = mutableListOf<UnaryMsg>()
    while (check(TokenType.Identifier) && !check(TokenType.Colon, 1)) {
        val tok = step()
        val unaryFirstMsg = UnaryMsg(receiver, tok.lexeme, null, tok)
        unaryMessages.add(unaryFirstMsg)
    }

    return unaryMessages
}


fun Parser.unaryOrBinary(receiver: Receiver): MutableList<out Message> {
    // 3 ^inc inc + 2 dec dec + ...
    val unaryMessagesForReceiver = getAllUnaries(receiver) // inc inc
    val binaryMessages = mutableListOf<BinaryMsg>()
    while (check(TokenType.BinarySymbol)) {
        val binarySymbol = step()
        val binaryArgument = receiver() // 2
        val unaryForArg = getAllUnaries(binaryArgument)
        val binaryMsg = BinaryMsg(
            receiver,
            unaryMessagesForReceiver,
            binarySymbol.lexeme,
            null,
            binarySymbol,
            binaryArgument,
            unaryForArg
        )
        binaryMessages.add(binaryMsg)
    }

    // if there is no binary message, that's mean there is only unary
    if (binaryMessages.isEmpty()) {
        return unaryMessagesForReceiver //as MutableList<Message>
    }
    // its binary msg
    return binaryMessages
}


// возможно стоит хранить сообщения предназначенные для аргументов кейводр сообщения вместе
fun Parser.anyMessageCall(receiver: Receiver): MessageCall {


    // keyword
    if (check(TokenType.Identifier) && check(TokenType.Colon, 1)) {
        val stringBuilder = StringBuilder()
        val unaryAndBinaryMessages = mutableListOf<Message>()
        val keyWordArguments = mutableListOf<KeywordArgAndItsMessages>()

        do {
            val keywordPart = step()
            step()// skip colon
            // x from: ^3 inc to: 2 inc
            val keyArg = receiver()
            // x from: 3 ^inc to: 2 inc
            val unaryOrBinary = unaryOrBinary(receiver)
            stringBuilder.append(keywordPart.lexeme, ":")

            val x = KeywordArgAndItsMessages(
                selectorName = keywordPart.lexeme,
                keywordArg = keyArg,
                unaryOrBinaryMsgsForArg = unaryOrBinary
            )
            keyWordArguments.add(x)

            // if keyword was split to 2 lines
            if (check(TokenType.EndOfLine)) {
                if (check(TokenType.Identifier, 1) && check(TokenType.Colon, 2))
                    step()
            }

            if (check(TokenType.Equal)) {
                println()
//                return keywordMessageDeclaration
                TODO()
            }

        } while (check(TokenType.Identifier) && check(TokenType.Colon, 1))

        val keywordMsg = KeywordMsg(receiver, stringBuilder.toString(), null, receiver.token, keyWordArguments)
        unaryAndBinaryMessages.add(keywordMsg)
        return MessageCall(receiver, unaryAndBinaryMessages, null, receiver.token)
    }
    // unary/binary
    val unaryAndBinaryMessage = unaryOrBinary(receiver)
    return MessageCall(receiver, unaryAndBinaryMessage, null, receiver.token)
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


fun Parser.tryToParseKeywordDeclaration(): Boolean {
    var isThereIdentColon = false
    var isThereEqualAfterThat = false
    var peekCounter = 0
//    var keywordArguments = listOf<KeywordDeclarationArg>()
//    val listOfKeywords = mutableListOf<String>()
//    val listOfKeywords2 = mutableListOf<String>()
    // is there ident: and = after that before end of line?
    while (!(check(TokenType.EndOfLine, peekCounter) || check(TokenType.EndOfFile, peekCounter))) {
        val q = peek(peekCounter)
        if (q.kind == TokenType.Identifier && check(TokenType.Colon, peekCounter + 1)) {
            isThereIdentColon = true
            // Person ^from: x::int
//            listOfKeywords.add(q.lexeme)
//            val argument = peek(peekCounter + 2)
//            val thereIsArg = argument.kind == TokenType.Identifier
//            val colon = peek(peekCounter + 3)
//            val thereIsDoubleColon = colon.kind == TokenType.DoubleColon
//            val type = peek(peekCounter + 4)
//            val thereIsType = type.kind == TokenType.Identifier
//
//            if (thereIsArg && thereIsDoubleColon) {
//
//            }
        }

        if (isThereIdentColon && check(TokenType.Equal, peekCounter)) {
            isThereEqualAfterThat = true
            break
        }
        peekCounter++

    }
    return isThereIdentColon && isThereEqualAfterThat
}

// all top level declarations
// type
// messageDecl
// message x echo
// assign x = 1
fun Parser.declaration(): Declaration {
    val tok = peek()
    val kind = tok.kind

    // Checks for declarations that starts from keyword like type/fn

    if (tok.kind == TokenType.Identifier) {
        // can be with "x::int =" or "x ="
        if (check(TokenType.DoubleColon, 1) || (check(TokenType.Equal, 1))) {
            return varDeclaration()
        }
    }
    if (kind == TokenType.Type) TODO()

    val methodDeclaration = tryToParseKeywordDeclaration()
    if (tryToParseKeywordDeclaration()) {
        return keywordDeclaration()
    }
    println(methodDeclaration)

    // replace with expression which is switch or message
    return message()
}

fun Parser.keywordDeclaration(): MessageDeclarationKeyword {
    val receiverTypeNameToken = match(TokenType.Identifier)
    val args = mutableListOf<KeywordDeclarationArg>()
    do {
        // it can be no type no local name :key
        // type, no local name key::int      key2::string
        // type and local name: to: x::int   from: y::int

        val noLocalNameNoType = check(TokenType.Colon)
        val noLocalName = check(TokenType.Identifier) && check(TokenType.DoubleColon, 1)
        // :foo
        if (noLocalNameNoType) {
            step() //skip colon
            val argName = step()
            if (argName.kind != TokenType.Identifier) {
                error("You tried to declare keyword message with arg without type and local name, identifier expected after colon :foobar")
            }
            args.add(KeywordDeclarationArg(name = argName.lexeme))
        }
        // arg::int
        else if (noLocalName) {
            val argName = step()
            if (argName.kind != TokenType.Identifier) {
                error("You tried to declare keyword message with arg without local name, identifier expected before double colon foobar::type")
            }
            match(TokenType.DoubleColon)
            val typeName = step()
            if (typeName.kind != TokenType.Identifier) {
                error("You tried to declare keyword message with arg without local name, type expected after colon double foobar::type")
            }
            args.add(KeywordDeclarationArg(name = argName.lexeme, type = typeName.lexeme))
        }
        // key: localName(::int)?
        else {
            val key = step()
            match(TokenType.Colon)
            val local = step()
            val typename: String? = if (check(TokenType.DoubleColon)) {
                step()// skip doubleColon
                step().lexeme
            } else {
                null
            }

            args.add(KeywordDeclarationArg(name = key.lexeme, localName = local.lexeme, type = typename))

        }


    } while (!check(TokenType.Equal))

    TODO()
}

fun Parser.declarations(): List<Declaration> {

    while (!this.done()) {
        this.tree.add(this.declaration())
        if (check(TokenType.EndOfLine)) {
            step()
        }
    }

    return this.tree
}
