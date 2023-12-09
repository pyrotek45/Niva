package frontend.parser.types.ast

import frontend.meta.Token
import frontend.parser.parsing.CodeAttribute
import frontend.typer.Type.RecursiveType.fields
import frontend.typer.Type.RecursiveType.isPrivate
import frontend.typer.Type.RecursiveType.name


sealed class TypeAST(
    val name: String,
    val isNullable: Boolean,
    token: Token,
    isPrivate: Boolean,
    pragmas: MutableList<CodeAttribute>
) : Statement(token, isPrivate, pragmas) {
//    val name: String
//        get() = this.type.name

    // [anyType, anyType -> anyType]?


    class UserType(
        name: String,

        val typeArgumentList: List<TypeAST>,
        isNullable: Boolean,
        token: Token,
        val names: List<String> = listOf(name),
        isPrivate: Boolean = false,
        pragmas: MutableList<CodeAttribute> = mutableListOf()
    ) : TypeAST(name, isNullable, token, isPrivate, pragmas) {
        override fun toString(): String {
            return names.joinToString(".")
        }
    }

    class InternalType(
        name: InternalTypes,
        isNullable: Boolean,
        token: Token,
        isPrivate: Boolean = false,
        pragmas: MutableList<CodeAttribute> = mutableListOf()
    ) : TypeAST(name.name, isNullable, token, isPrivate, pragmas)

//    object ResursiveType : TypeAST(
//        "ResursiveType", false, createFakeToken(), false, mutableListOf()
//    )


    class Lambda(
        name: String,
        val inputTypesList: List<TypeAST>,
        val returnType: TypeAST,
        isNullable: Boolean,
        token: Token,
        isPrivate: Boolean = false,
        pragmas: MutableList<CodeAttribute> = mutableListOf()
    ) : TypeAST(name, isNullable, token, isPrivate, pragmas)
}


fun TypeAST.generateType(generateGeneric: Boolean = true): String {
//    val x: (String, Int) -> Int = {}
    return when (this) {
        is TypeAST.InternalType -> name
        is TypeAST.UserType -> buildString {
            append(names.joinToString("."))
            if (generateGeneric && typeArgumentList.isNotEmpty()) {
                append("<${typeArgumentList.joinToString(", ") { it.name }}>")
            }
        }

        is TypeAST.Lambda -> {
            buildString {
                append("(")
                inputTypesList.forEach {
                    append(it.generateType(), ",")
                }
                append(") -> ")

                append(returnType.generateType())

            }
        }

    }
}
class EnumFieldAST(
    val name: String,
    val value: Expression,
    val token: Token,
) {
    override fun toString(): String {
        return "$name: $value"
    }
}

class TypeFieldAST(
    val name: String,
    val type: TypeAST?,
    val token: Token,
) {
    override fun toString(): String {
        return name + ":" + (type?.toString() ?: "")
    }
}

sealed class SomeTypeDeclaration(
    val typeName: String,
    val fields: List<TypeFieldAST>,
    token: Token,
    val genericFields: MutableList<String> = mutableListOf(),
    isPrivate: Boolean = false,
    pragmas: MutableList<CodeAttribute> = mutableListOf(),
) : Declaration(token, isPrivate, pragmas)

class TypeDeclaration(
    typeName: String,
    fields: List<TypeFieldAST>,
    token: Token,
    genericFields: MutableList<String> = mutableListOf(),
    codeAttributes: MutableList<CodeAttribute> = mutableListOf(),
    isPrivate: Boolean = false,
) : SomeTypeDeclaration(typeName, fields, token, genericFields, isPrivate, codeAttributes) {
    override fun toString(): String {
        return "TypeDeclaration($typeName)"
    }
}

class EnumBranch(
    name: String,
    val fieldsValues: List<EnumFieldAST>,
    token: Token,
    val root: EnumDeclarationRoot,
    codeAttributes: MutableList<CodeAttribute> = mutableListOf(),
) : SomeTypeDeclaration(name, listOf(), token, mutableListOf(), isPrivate, codeAttributes) {
    override fun toString(): String {
        return name + " " + fields.joinToString(", ") { it.name + ": " + it.toString() }
    }
}

class EnumDeclarationRoot(
    typeName: String,
    var branches: List<EnumBranch>,
    fields: List<TypeFieldAST>,
    token: Token,
    codeAttributes: MutableList<CodeAttribute> = mutableListOf(),
    isPrivate: Boolean = false,
) : SomeTypeDeclaration(typeName, fields, token, mutableListOf(), isPrivate, codeAttributes)

class UnionBranch(
    typeName: String,
    fields: List<TypeFieldAST>,
    token: Token,
    genericFields: MutableList<String> = mutableListOf(),
    val root: UnionDeclaration,
    var isRoot: Boolean = false,
    codeAttributes: MutableList<CodeAttribute> = mutableListOf(),
) : SomeTypeDeclaration(typeName, fields, token, genericFields, pragmas = codeAttributes) {
    override fun toString(): String {
        return typeName + " " + fields.joinToString(", ") { it.name + ": " + it.type }
    }
}

class UnionDeclaration(
    typeName: String,
    var branches: List<UnionBranch>,
    fields: List<TypeFieldAST>,
    token: Token,
    genericFields: MutableList<String> = mutableListOf(),
    codeAttributes: MutableList<CodeAttribute> = mutableListOf(),
    isPrivate: Boolean = false,
) : SomeTypeDeclaration(typeName, fields, token, genericFields, isPrivate, codeAttributes)

class AliasDeclaration(
    val typeName: String,
    val matchedTypeName: String,
    token: Token,
    pragmas: MutableList<CodeAttribute> = mutableListOf(),
    isPrivate: Boolean = false,
) : Declaration(token, isPrivate, pragmas)


enum class InternalTypes {
    Int, String, Float, Double, Boolean, Unit, Project, Char, IntRange, Any, Bind, Nothing, Exception
}
