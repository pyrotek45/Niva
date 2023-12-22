@file:Suppress("unused")

package frontend.resolver

import frontend.meta.TokenType
import frontend.meta.compileError
import frontend.parser.parsing.CodeAttribute
import frontend.parser.parsing.MessageDeclarationType
import frontend.parser.types.ast.*
import frontend.resolver.Type.RecursiveType.copy
import frontend.resolver.Type.RecursiveType.isNullable
import frontend.resolver.Type.RecursiveType.isPrivate
import frontend.resolver.Type.RecursiveType.name
import frontend.resolver.Type.RecursiveType.pkg
import frontend.resolver.Type.RecursiveType.protocols
import main.CYAN
import main.RED
import main.WHITE
import main.YEL
import main.utils.isGeneric

data class MsgSend(
    val pkg: String,
    val selector: String,
    val project: String,
    val type: MessageDeclarationType
)

sealed class MessageMetadata(
    val name: String,
    var returnType: Type, // need to change in single expression case
    val pkg: String,
    val pragmas: MutableList<CodeAttribute> = mutableListOf(),
    @Suppress("unused")
    val msgSends: List<MsgSend> = listOf()
) {
    override fun toString(): String {
        return when (this) {
            is BinaryMsgMetaData -> this.toString()
            is KeywordMsgMetaData -> this.toString()
            is UnaryMsgMetaData -> this.toString()
        }
    }
}

class UnaryMsgMetaData(
    name: String,
    returnType: Type,
    pkg: String,
    codeAttributes: MutableList<CodeAttribute> = mutableListOf(),
    msgSends: List<MsgSend> = listOf(),
    val isGetter: Boolean = false
) : MessageMetadata(name, returnType, pkg, codeAttributes, msgSends) {
    override fun toString(): String {
        return "$name -> $returnType"
    }
}

class BinaryMsgMetaData(
    name: String,
    val argType: Type,
    returnType: Type,
    pkg: String,
    codeAttributes: MutableList<CodeAttribute> = mutableListOf(),
    msgSends: List<MsgSend> = listOf()
) : MessageMetadata(name, returnType, pkg, codeAttributes, msgSends) {
    override fun toString(): String {
        return "$name $argType -> $returnType"
    }
}


class KeywordMsgMetaData(
    name: String,
    val argTypes: List<KeywordArg>,
    returnType: Type,
    pkg: String,
    codeAttributes: MutableList<CodeAttribute> = mutableListOf(),
    msgSends: List<MsgSend> = listOf()
) : MessageMetadata(name, returnType, pkg, codeAttributes, msgSends) {
    override fun toString(): String {
        val args = argTypes.joinToString(" ") { it.toString() }
        return "$args -> $returnType"
    }
}

//class ConstructorMsgMetaData(
//    name: String,
//    returnType: Type,
//    msgSends: List<MsgSend> = listOf()
//) : MessageMetadata(name, returnType, msgSends)

sealed class FieldWithType(
    val name: String,
    var type: Type,
) {
    override fun toString(): String {
        return "$name: $type"
    }
}

class KeywordArg(
    name: String,
    type: Type,
) : FieldWithType(name, type)

class TypeField(
    name: String,
    type: Type //when generic, we need to reassign it to real type
) : FieldWithType(name, type)


class FieldWithValue(
    val name: String,
    var value: Expression
) {
    override fun toString(): String {
        return "$name: $value"
    }
}


fun Type.isDescendantOf(type: Type): Boolean {
    if (this !is Type.UserLike || type !is Type.UserLike) {
        return false
    }
    var parent: Type? = this.parent
    while (parent != null) {
        if (compare2Types(type, parent)) {
            return true
        }
        parent = parent.parent
    }
    return false
}


fun MutableList<TypeField>.copy(): MutableList<TypeField> =
    this.map {
        val type = it.type
        TypeField(
            name = it.name,
            type = if (type is Type.UserLike) type.copy() else type
        )
    }.toMutableList()


sealed class Type(
    val name: String, // when generic, we need to reassign it to AST's Type field, instead of type's typeField
    val pkg: String,
    val isPrivate: Boolean,
    var isNullable: Boolean,
    val protocols: MutableMap<String, Protocol> = mutableMapOf(),
    var parent: Type? = null, // = Resolver.defaultBasicTypes[InternalTypes.Any] ?:
    var beforeGenericResolvedName: String? = null,
//    var bind: Boolean = false
) {
    override fun toString(): String =
        if (this is InternalLike)
            name
        else
            "$pkg.$name"


    class NullableType(
        val realType: Type?,
    ) : Type(
        realType?.name ?: name,
        realType?.pkg ?: pkg,
        realType?.isPrivate ?: isPrivate,
        realType?.isNullable ?: isNullable,
        realType?.protocols ?: protocols
    ) {
        fun getTypeOrNullType(): Type {
            if (realType != null)
                return realType

            return Resolver.defaultTypes[InternalTypes.Null]!!
        }
    }


    class Lambda(
        val args: MutableList<TypeField>,
        val returnType: Type,
        pkg: String = "common",
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
    ) : Type("[${args.joinToString(", ") { it.type.name }} -> ${returnType.name}]", pkg, isPrivate, isNullable)

    sealed class InternalLike(
        typeName: InternalTypes,
        pkg: String,
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        protocols: MutableMap<String, Protocol>
    ) : Type(typeName.name, pkg, isPrivate, isNullable, protocols)

    class InternalType(
        typeName: InternalTypes,
        pkg: String,
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        protocols: MutableMap<String, Protocol> = mutableMapOf()
    ) : InternalLike(typeName, pkg, isPrivate, isNullable, protocols)


    sealed class UserLike(
        name: String,
        var typeArgumentList: List<Type>,
        var fields: MutableList<TypeField>,
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        pkg: String,
        protocols: MutableMap<String, Protocol>,
        var isBinding: Boolean = false
    ) : Type(name, pkg, isPrivate, isNullable, protocols)

    fun UserLike.copy(): UserLike =
        when (this) {
            is UserType -> UserType(
                name = this.name,
                typeArgumentList = this.typeArgumentList.map { if (it is UserLike) it.copy() else it },
                fields = this.fields.copy(),
                isPrivate = this.isPrivate,
                pkg = this.pkg,
                protocols = this.protocols.toMutableMap(),
            ).also { it.isBinding = this.isBinding }

            is UserEnumRootType -> UserEnumRootType(
                name = this.name,
                typeArgumentList = this.typeArgumentList.toList(),
                fields = this.fields.toMutableList(),
                isPrivate = this.isPrivate,
                pkg = this.pkg,
                branches = this.branches.toList(),
                protocols = this.protocols.toMutableMap(),
            ).also { it.isBinding = this.isBinding }

            is UserUnionRootType -> UserUnionRootType(
                name = this.name,
                typeArgumentList = this.typeArgumentList.toList(),
                fields = this.fields.toMutableList(),
                isPrivate = this.isPrivate,
                pkg = this.pkg,
                branches = this.branches.toList(),
                protocols = this.protocols.toMutableMap(),
            ).also { it.isBinding = this.isBinding }

            is UserEnumBranchType -> TODO()
            is UserUnionBranchType -> TODO()
            is KnownGenericType -> TODO()
            is UnknownGenericType -> this
            RecursiveType -> TODO()
        }


    class UserType(
        name: String,
        typeArgumentList: List<Type>, // for <T, G>
        fields: MutableList<TypeField>,
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        pkg: String,
        protocols: MutableMap<String, Protocol> = mutableMapOf()
    ) : UserLike(name, typeArgumentList, fields, isPrivate, isNullable, pkg, protocols)

    class UserUnionRootType(
        var branches: List<UserUnionBranchType>,
        name: String,
        typeArgumentList: List<Type>, // for <T, G>
        fields: MutableList<TypeField>,
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        pkg: String,
        protocols: MutableMap<String, Protocol> = mutableMapOf()
    ) : UserLike(name, typeArgumentList, fields, isPrivate, isNullable, pkg, protocols)

    class UserUnionBranchType(
        val root: UserUnionRootType,
        name: String,
        typeArgumentList: List<Type>, // for <T, G>
        fields: MutableList<TypeField>,
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        pkg: String,
        protocols: MutableMap<String, Protocol> = mutableMapOf()
    ) : UserLike(name, typeArgumentList, fields, isPrivate, isNullable, pkg, protocols)


    class UserEnumRootType(
        var branches: List<UserEnumBranchType>,
        name: String,
        typeArgumentList: List<Type>, // for <T, G>
        fields: MutableList<TypeField>,
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        pkg: String,
        protocols: MutableMap<String, Protocol> = mutableMapOf()
    ) : UserLike(name, typeArgumentList, fields, isPrivate, isNullable, pkg, protocols)

    class UserEnumBranchType(
        val root: UserEnumRootType,
        name: String,
        typeArgumentList: List<Type>, // for <T, G>
        fields: MutableList<TypeField>,
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        pkg: String,
        protocols: MutableMap<String, Protocol> = mutableMapOf()
    ) : UserLike(name, typeArgumentList, fields, isPrivate, isNullable, pkg, protocols)


    class KnownGenericType(
        name: String,
        typeArgumentList: List<Type>,
        pkg: String,
        fields: MutableList<TypeField> = mutableListOf(),
        isNullable: Boolean = false,
        isPrivate: Boolean = false,
        protocols: MutableMap<String, Protocol> = mutableMapOf()
    ) : UserLike(name, typeArgumentList, fields, isPrivate, isNullable, pkg, protocols)

    class UnknownGenericType(
        name: String,
        typeArgumentList: List<Type> = listOf(),
        fields: MutableList<TypeField> = mutableListOf(),
        isNullable: Boolean = false,
        isPrivate: Boolean = true,
        pkg: String = "common",
        protocols: MutableMap<String, Protocol> = mutableMapOf()
    ) : UserLike(name, typeArgumentList, fields, isPrivate, isNullable, pkg, protocols)

    object RecursiveType : UserLike("RecursiveType", listOf(), mutableListOf(), false, false, "common", mutableMapOf())


}

data class Protocol(
    val name: String,
    val unaryMsgs: MutableMap<String, UnaryMsgMetaData> = mutableMapOf(),
    val binaryMsgs: MutableMap<String, BinaryMsgMetaData> = mutableMapOf(),
    val keywordMsgs: MutableMap<String, KeywordMsgMetaData> = mutableMapOf(),
    val staticMsgs: MutableMap<String, MessageMetadata> = mutableMapOf(),
)

class Package(
    val packageName: String,
    val declarations: MutableList<Declaration> = mutableListOf(),
    val types: MutableMap<String, Type> = mutableMapOf(),
//    val usingPackages: MutableList<Package> = mutableListOf(),
    // import x.y.*
    val imports: MutableSet<String> = mutableSetOf(),
    // import x.y
    val concreteImports: MutableSet<String> = mutableSetOf(),
    val isBinding: Boolean = false,
    val comment: String = ""
) {
    override fun toString(): String {
        return packageName
    }
}

class Project(
    val name: String,
    val packages: MutableMap<String, Package> = mutableMapOf(),
    val usingProjects: MutableList<Project> = mutableListOf()
)

fun TypeAST.toType(typeDB: TypeDB, typeTable: Map<TypeName, Type>, selfType: Type.UserLike? = null): Type {

    val replaceToNullableIfNeeded = { type: Type ->
        val isNullable = token.kind == TokenType.NullableIdentifier || token.kind == TokenType.Null
        type.isNullable = isNullable

        if (isNullable) {
            Type.NullableType(realType = type)
        } else {
            type
        }
    }

    when (this) {
        is TypeAST.InternalType -> {
            val type = Resolver.defaultTypes.getOrElse(InternalTypes.valueOf(name)) {
                this.token.compileError("Can't find default type: ${YEL}$name")
            }

            return replaceToNullableIfNeeded(type)
        }


        is TypeAST.UserType -> {
            if (name.isGeneric()) {
                return Type.UnknownGenericType(name)
            }
            if (selfType != null && name == selfType.name) return selfType

            if (this.typeArgumentList.isNotEmpty()) {
                // need to know, what Generic name(like T), become what real type(like Int) to replace fields types from T to Int


                val type = typeTable[name] ?: this.token.compileError("Can't find user type: ${YEL}$name")
                //TODO DB
                if (type is Type.UserLike) {
                    val letterToTypeMap = mutableMapOf<String, Type>()

                    if (this.typeArgumentList.count() != type.typeArgumentList.count()) {
                        throw Exception("Count ${this.name}'s type arguments not the same it's AST version ")
                    }
                    val typeArgs = this.typeArgumentList.mapIndexed { i, it ->
                        val rer = it.toType(typeDB, typeTable, selfType)
                        letterToTypeMap[type.typeArgumentList[i].name] = rer
                        rer
                    }


                    type.typeArgumentList = typeArgs
                    // replace fields types from T to real
                    type.fields.forEachIndexed { i, field ->
                        val fieldType = letterToTypeMap[field.type.name]
                        if (fieldType != null) {
                            field.type = fieldType
                        }
                    }
                    return type
                } else {
                    this.token.compileError("Panic: type: ${YEL}${this.name}${RED} with typeArgumentList cannot but be Type.UserType")
                }
            }
            val type = typeTable[name]
                ?: this.token.compileError("Can't find user type: ${YEL}$name")

            return replaceToNullableIfNeeded(type)
        }

        is TypeAST.Lambda -> {
            val lambdaType = Type.Lambda(
                args = inputTypesList.map {
                    TypeField(
                        type = it.toType(typeDB, typeTable, selfType),
                        name = it.name
                    )
                }.toMutableList(),
                returnType = this.returnType.toType(typeDB, typeTable, selfType),
                isNullable = token.kind == TokenType.NullableIdentifier || token.kind == TokenType.Null
            )

            return replaceToNullableIfNeeded(lambdaType)
        }


    }

}

fun TypeFieldAST.toTypeField(typeDB: TypeDB, typeTable: Map<TypeName, Type>, selfType: Type.UserLike): TypeField {
    val result = TypeField(
        name = name,
        type = type!!.toType(typeDB, typeTable, selfType)
    )
    return result
}

fun SomeTypeDeclaration.toType(
    pkg: String,
    typeTable: Map<TypeName, Type>,
    typeDB: TypeDB,
    isUnion: Boolean = false,
    isEnum: Boolean = false,
    unionRootType: Type.UserUnionRootType? = null, // if not null, then this is branch
    enumRootType: Type.UserEnumRootType? = null,
): Type.UserLike {

    val result = if (isUnion)
        Type.UserUnionRootType(
            branches = listOf(),
            name = typeName,
            typeArgumentList = listOf(),
            fields = mutableListOf(),
            isPrivate = isPrivate,
            pkg = pkg,
            protocols = mutableMapOf()
        )
    else if (isEnum)
        Type.UserEnumRootType(
            branches = listOf(),
            name = typeName,
            typeArgumentList = listOf(),
            fields = mutableListOf(),
            isPrivate = isPrivate,
            pkg = pkg,
            protocols = mutableMapOf()
        )
    else if (enumRootType != null) {
        Type.UserEnumBranchType(
            root = enumRootType,
            name = typeName,
            typeArgumentList = listOf(),
            fields = mutableListOf(),
            isPrivate = isPrivate,
            pkg = pkg,
            protocols = mutableMapOf()
        )
    } else if (unionRootType != null)
        Type.UserUnionBranchType(
            root = unionRootType,
            name = typeName,
            typeArgumentList = listOf(),
            fields = mutableListOf(),
            isPrivate = isPrivate,
            pkg = pkg,
            protocols = mutableMapOf()
        )
    else
        Type.UserType(
            name = typeName,
            typeArgumentList = listOf(),
            fields = mutableListOf(),
            isPrivate = isPrivate,
            pkg = pkg,
            protocols = mutableMapOf()
        )


    val fieldsTyped = mutableListOf<TypeField>()
    val unresolvedSelfTypeFields = mutableListOf<TypeField>()

//    val createTypeAlreadyWithNoFields // than fill it with them

    fields.forEach {
        val astType = it.type
        if (astType != null && astType.name == typeName) {
            // this is recursive type
            val fieldType = TypeField(
                name = it.name,
                type = Type.RecursiveType
            )
            fieldsTyped.add(fieldType)
            unresolvedSelfTypeFields.add(fieldType)

        } else fieldsTyped.add(it.toTypeField(typeDB, typeTable, selfType = result))
    }

    fun getAllGenericTypesFromFields(fields2: List<TypeField>, fields: List<TypeFieldAST>): MutableList<Type.UserLike> {
        val result2 = mutableListOf<Type.UserLike>()
        fields2.forEachIndexed { i, it ->
            val type = it.type

            if (type is Type.UserLike) {
                val qwe = List(type.typeArgumentList.size) { i2 ->
                    val field = fields[i].type
                    val typeName =
                        if (field is TypeAST.UserType) {
                            field.typeArgumentList[i2].name
                        } else {
                            throw Exception("field is not user type")
                        }
                    Type.UnknownGenericType(
                        name = typeName
                    )
                }

                result2.addAll(qwe)

                if (type.fields.isNotEmpty()) {
                    result2.addAll(getAllGenericTypesFromFields(type.fields, fields))
                }
            }
        }
        return result2
    }

    val typeFields1 = fieldsTyped.filter { it.type is Type.UnknownGenericType }.map { it.type }
    val typeFieldsGeneric = getAllGenericTypesFromFields(fieldsTyped, fields)


    val typeFields = (typeFields1 + typeFieldsGeneric).toMutableList()


    unresolvedSelfTypeFields.forEach {
        it.type = result
    }




    this.genericFields.addAll(typeFields.map { it.name })

    // add already declared generic fields(via `type Sas::T` syntax)
    this.genericFields.forEach {
        if (it.isGeneric() && typeFields.find { x -> x.name == it } == null) {
            typeFields.add(Type.UnknownGenericType(it))
        }
    }

    result.typeArgumentList = typeFields
    result.fields = fieldsTyped

    return result
}


fun MessageDeclarationUnary.toMessageData(
    typeDB: TypeDB,
    typeTable: MutableMap<TypeName, Type>,
    pkg: Package,
    isGetter: Boolean = false
): UnaryMsgMetaData {
    val returnType = this.returnType ?: this.returnTypeAST?.toType(typeDB, typeTable)
    ?: Resolver.defaultTypes[InternalTypes.Unit]!!
    this.returnType = returnType

    val result = UnaryMsgMetaData(
        name = this.name,
        returnType = returnType,
        pkg = pkg.packageName,
        codeAttributes = pragmas,
        isGetter = isGetter
    )
    return result
}

fun MessageDeclarationBinary.toMessageData(
    typeDB: TypeDB,
    typeTable: MutableMap<TypeName, Type>,
    pkg: Package
): BinaryMsgMetaData {
    val returnType = this.returnType ?: this.returnTypeAST?.toType(typeDB, typeTable)
    ?: Resolver.defaultTypes[InternalTypes.Unit]!!
    this.returnType = returnType


    val argType = this.forTypeAst.toType(typeDB, typeTable)

    val result = BinaryMsgMetaData(
        name = this.name,
        argType = argType,
        returnType = returnType,
        pkg = pkg.packageName,
        codeAttributes = pragmas
    )
    return result
}

fun MessageDeclarationKeyword.toMessageData(
    typeDB: TypeDB,
    typeTable: MutableMap<TypeName, Type>,
    pkg: Package
): KeywordMsgMetaData {
    val returnType = this.returnType ?: this.returnTypeAST?.toType(typeDB, typeTable)
    ?: Resolver.defaultTypes[InternalTypes.Unit]!!

    this.returnType = returnType


    val keywordArgs = this.args.map {
        KeywordArg(
            name = it.name,
            type = it.type?.toType(typeDB, typeTable)
                ?: token.compileError("Type of keyword message ${CYAN}${this.name}${RED}'s arg ${WHITE}${it.name}${RED} not registered")
        )
    }
    val result = KeywordMsgMetaData(
        name = this.name,
        argTypes = keywordArgs,
        returnType = returnType,
        codeAttributes = pragmas,
        pkg = pkg.packageName
    )
    return result
}
