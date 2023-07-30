package frontend.parser.types.ast

import frontend.meta.Token


sealed class TypeAST(
    val name: String,
    val isNullable: Boolean,
    token: Token,
    isPrivate: Boolean,
    pragmas: List<Pragma>
) : Statement(token, isPrivate, pragmas) {
//    val name: String
//        get() = this.type.name

    // [anyType, anyType -> anyType]?


    class UserType(
        name: String,
        val typeArgumentList: List<TypeAST>,
        isNullable: Boolean,
        token: Token,
        isPrivate: Boolean = false,
        pragmas: List<Pragma> = listOf()
    ) : TypeAST(name, isNullable, token, isPrivate, pragmas)

    class InternalType(
        name: InternalTypes,
        isNullable: Boolean,
        token: Token,
        isPrivate: Boolean = false,
        pragmas: List<Pragma> = listOf()
    ) : TypeAST(name.name, isNullable, token, isPrivate, pragmas)

    class Lambda(
        name: String,
        val inputTypesList: List<TypeAST>,
        val returnType: TypeAST,
        isNullable: Boolean,
        token: Token,
        isPrivate: Boolean = false,
        pragmas: List<Pragma> = listOf()
    ) : TypeAST(name, isNullable, token, isPrivate, pragmas)
}


class TypeFieldAST(
    val name: String,
    val type: TypeAST?,
    val token: Token
)

interface ITypeDeclaration {
    val typeName: String
    val fields: List<TypeFieldAST>
}

class TypeDeclaration(
    override val typeName: String,
    override val fields: List<TypeFieldAST>,
    token: Token,
    pragmas: List<Pragma> = listOf(),
    isPrivate: Boolean = false,
) : Declaration(token, isPrivate, pragmas), ITypeDeclaration

class UnionBranch(
    override val typeName: String,
    override val fields: List<TypeFieldAST>,
    val token: Token,
) : ITypeDeclaration

class UnionDeclaration(
    override val typeName: String,
    val branches: List<UnionBranch>,
    override val fields: List<TypeFieldAST>,
    token: Token,
    pragmas: List<Pragma> = listOf(),
    isPrivate: Boolean = false,
) : Declaration(token, isPrivate, pragmas), ITypeDeclaration

class AliasDeclaration(
    val typeName: String,
    val matchedTypeName: String,
    token: Token,
    pragmas: List<Pragma> = listOf(),
    isPrivate: Boolean = false,
) : Declaration(token, isPrivate, pragmas)


enum class InternalTypes {
    Int, String, Float, Boolean, Unit, Project, Char
}
