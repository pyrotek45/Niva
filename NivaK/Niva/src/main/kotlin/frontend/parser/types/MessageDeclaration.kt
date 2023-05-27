package frontend.parser.types

import frontend.meta.Token


sealed class MessageDeclaration(
    val name: String,
    token: Token,
    isPrivate: Boolean = false,
    pragmas: List<Pragma> = listOf()
) : Declaration(token, isPrivate, pragmas)

class MessageDeclarationUnary(
    name: String,
    token: Token,
    isPrivate: Boolean = false,
    pragmas: List<Pragma> = listOf()
) : MessageDeclaration(name, token, isPrivate, pragmas)

class MessageDeclarationBinary(
    name: String,
    token: Token,
    isPrivate: Boolean = false,
    pragmas: List<Pragma> = listOf()
) : MessageDeclaration(name, token, isPrivate, pragmas)


// key: localName::type
class KeywordDeclarationArg(
    val name: String,
    val localName: String? = null,
    val type: String? = null,
)

class MessageDeclarationKeyword(
    name: String,
    token: Token,
    val args: List<KeywordDeclarationArg>,
    val body: List<Declaration>,
    val isSingleExpression: Boolean,
    isPrivate: Boolean = false,
    pragmas: List<Pragma> = listOf(),
) : MessageDeclaration(name, token, isPrivate, pragmas)
