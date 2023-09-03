package codogen

import frontend.parser.types.ast.*


val operators = hashMapOf(
    "+" to "plus",
    "-" to "minus",
    "*" to "times",
    "/" to "div",
    "%" to "rem",
    ".." to "rangeTo",

    "%" to "contains",

    "+=" to "plusAssign",
    "-=" to "minusAssign",
    "*=" to "timesAssign",
    "/=" to "divAssign",
    "%=" to "remAssign",

    "==" to "equals",
    "!=" to "equals",

    ">" to "compareTo",
    "<" to "compareTo",
    ">=" to "compareTo",
    "<=" to "compareTo",

    "<-=" to "getValue",
    "=->" to "setValue",

    "apply" to "invoke",
)

fun MessageDeclarationUnary.generateUnaryDeclaration(isStatic: Boolean = false) = buildString {
    // fun Int.sas(): unit {
    //   this.echo()
    // }
    append("fun ", forType.name)
    if (isStatic) {
        append(".Companion")
    }
    append(".", name, "()")
    bodyPart(this@generateUnaryDeclaration, this)
}

fun MessageDeclarationBinary.generateBinaryDeclaration(isStatic: Boolean = false) = buildString {
    fun operatorToString(x: String): String {
        return operators[x]!!
    }

    //            operator fun Int.plus(increment: Int): Counter {
    //              this.echo()
    //            }

    append("operator fun ", forType.name)
    if (isStatic) {
        append(".Companion")
    }
    append(".", operatorToString(name), "(", arg.name)

    if (arg.type != null) {
        append(": ", arg.type.name)
    }
    append(")")
    // operator fun int.sas(...)
    bodyPart(this@generateBinaryDeclaration, this)
}

fun MessageDeclarationKeyword.generateKeywordDeclaration(isStatic: Boolean = false) = buildString {
    //            fun Int.fromTo(x: Int, y: Int): Counter {
    //              this.echo()
    //            }
    append("fun ", forType.name)
    if (isStatic) {
        append(".Companion")
    }
    append(".", name, "(")

    val c = args.count() - 1
    args.forEachIndexed { i, arg ->
        append(arg.name())
        if (arg.type != null) {
            append(": ", arg.type.toKotlinStr())
            if (i != c) {
                append(", ")
            }
        }
    }

    append(")")
    // operator fun int.sas(...)
    bodyPart(this@generateKeywordDeclaration, this)
}


private fun bodyPart(
    messageDeclaration: MessageDeclaration,
    stringBuilder: StringBuilder
) {
    if (messageDeclaration.returnType != null) {
        stringBuilder.append(": ", messageDeclaration.returnType.name)
    }

    if (messageDeclaration.body.count() == 1 && messageDeclaration.body[0] !is ReturnStatement) {
        stringBuilder.append(" = ", codogenKt(messageDeclaration.body, 0))
    } else {
        stringBuilder.append(" {\n")
        stringBuilder.append(codogenKt(messageDeclaration.body, 1))
        stringBuilder.append("}\n")
    }
}
