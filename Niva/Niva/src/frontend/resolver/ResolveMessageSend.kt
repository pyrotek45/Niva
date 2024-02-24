@file:Suppress("UnusedReceiverParameter")

package frontend.resolver

import frontend.meta.Token
import frontend.meta.compileError
import frontend.parser.types.ast.*
import frontend.resolver.Type.RecursiveType.copy
import main.*
import main.frontend.resolver.messageResolving.resolveBinaryMsg
import main.frontend.resolver.messageResolving.resolveKeywordMsg
import main.frontend.resolver.messageResolving.resolveUnaryMsg
import main.utils.isGeneric

fun fillGenericsWithLettersByOrder(type: Type.UserLike) {
    if (type.typeArgumentList.count() > 2) {
        throw Exception("Generics with more than 2 params are not supported yet")
    }
    val genericLetters = listOf("T", "G")

    type.typeArgumentList.forEachIndexed { i, it ->
        val k = genericLetters[i]
        it.beforeGenericResolvedName = k
    }
}


fun getTableOfLettersFrom_TypeArgumentListOfType(type: Type.UserLike): MutableMap<String, Type> {
    if (type.typeArgumentList.count() > 2) {
        throw Exception("Generics with more than 2 params are not supported yet")
    }
    val genericLetters = listOf("T", "G")

    val result = mutableMapOf<String, Type>()
    type.typeArgumentList.forEachIndexed { i, it ->
        val k = genericLetters[i]
        if (!it.name.isGeneric())
            result[k] = it
    }
    return result
}


fun resolveReceiverGenericsFromArgs(receiverType: Type, args: List<KeywordArgAst>, tok: Token): Type {
    if (receiverType !is Type.UserLike) return receiverType
    // replace every Generic type with real
    if (receiverType.typeArgumentList.isNotEmpty()) {
        val replacerTypeIfItGeneric = receiverType.copy()

        // match every type argument with fields
        val map = mutableMapOf<String, Type>()
        replacerTypeIfItGeneric.typeArgumentList.forEach { typeArg ->
            val fieldsOfThisType =
                replacerTypeIfItGeneric.fields.filter { it.type.name == typeArg.name }
            fieldsOfThisType.forEach { genericField ->
                // find real type from arguments
                val real = args.find { it.name == genericField.name }
                    ?: tok.compileError("Can't find real type for field: $YEL${genericField.name}${RESET} of generic type: $YEL${genericField.type.name}${RESET}")
                val realType = real.keywordArg.type
                    ?: real.keywordArg.token.compileError("Compiler bug: $YEL${real.name}${RESET} doesn't have type")
                map[typeArg.name] = realType
            }
        }
        // replace typeFields to real ones
        val realTypes = replacerTypeIfItGeneric.typeArgumentList.toMutableList()
        map.forEach { (fieldName, fieldRealType) ->
            val fieldIndex = realTypes.indexOfFirst { it.name == fieldName }
            realTypes[fieldIndex] = fieldRealType
            // replace all fields of generic type
            replacerTypeIfItGeneric.fields.forEach {
                if (it.type.name == fieldName) {
                    it.type = fieldRealType
                }
            }
        }
        replacerTypeIfItGeneric.typeArgumentList = realTypes
        return replacerTypeIfItGeneric
    }
    return receiverType
}


fun findThisInScopes(
    token: Token,
    currentScope: MutableMap<String, Type>,
    previousScope: MutableMap<String, Type>,
) = previousScope["this"] ?: currentScope["this"]
?: token.compileError("Cant resolve type of receiver for dot expression")


fun Resolver.resolveMessage(
    statement: Message,
    previousScope: MutableMap<String, Type>,
    currentScope: MutableMap<String, Type>
) {

    val previousAndCurrentScope = (previousScope + currentScope).toMutableMap()

    when (statement) {
        is KeywordMsg -> resolveKeywordMsg(statement, previousScope, currentScope)
        is BinaryMsg -> resolveBinaryMsg(statement, previousAndCurrentScope)
        is UnaryMsg -> resolveUnaryMsg(statement, previousAndCurrentScope)
    }
}


fun replaceAllGenericsToRealTypeRecursive(
    type: Type.UserLike,
    letterToRealType: MutableMap<String, Type>,
    receiverGenericsTable: MutableMap<String, Type>
): Type.UserType {
    val newResolvedTypeArgs2 = mutableListOf<Type>()

    val copyType = type.copy()

    copyType.typeArgumentList.forEach { typeArg ->
        val isSingleGeneric = typeArg.name.isGeneric()

        if (isSingleGeneric) {
            val resolvedLetterType =
                letterToRealType[typeArg.name] ?: receiverGenericsTable[typeArg.name]
                ?: throw Exception("Can't find generic type: $YEL${typeArg.name}${RESET} in letter table")
            newResolvedTypeArgs2.add(resolvedLetterType)
            resolvedLetterType.beforeGenericResolvedName = typeArg.name
        } else if (typeArg is Type.UserLike && type.typeArgumentList.isNotEmpty()) {
            newResolvedTypeArgs2.add(
                replaceAllGenericsToRealTypeRecursive(
                    typeArg,
                    letterToRealType,
                    receiverGenericsTable
                )
            )
        } else {
            newResolvedTypeArgs2.add(typeArg)
        }
    }

    return Type.UserType(
        name = copyType.name,
        typeArgumentList = newResolvedTypeArgs2,
        fields = copyType.fields,
        isPrivate = copyType.isPrivate,
        pkg = copyType.pkg,
        protocols = copyType.protocols
    )
}
