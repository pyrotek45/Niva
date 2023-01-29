import frontend.Lexer
import frontend.lex
import frontend.meta.TokenType
import frontend.util.fillSymbolTable

fun emptySource() {
    checkOnKinds("", mutableListOf(TokenType.EndOfFile))
}

fun punctuation() {
    checkOnKinds("{}", mutableListOf(TokenType.BinarySymbol, TokenType.BinarySymbol, TokenType.EndOfFile))
}

fun checkOnKinds(source: String, tokens: MutableList<TokenType>) {
    val lexer = Lexer(source, "sas")
    lexer.fillSymbolTable()
    val result = lexer.lex().map { it.kind }
    if (tokens != result) {
        throw Throwable("\n\ttokens: $tokens\n\tresult: $result")
    }
}

fun main() {
    emptySource()
    punctuation()
}