package decoders

import bitmage.ByteOrder
import bitmage.hex
import bitmage.indexOfFirstSubsequence
import bitmage.padLeft
import bitmage.toBooleanArray
import bitmage.toByteArray
import bitmage.toInt
import bitmage.toMinimalAmountOfBytes
import bitmage.toUInt
import bitmage.toUTF8String

@JsModule("js-yaml")
@JsNonModule
external object JsYaml {
    fun load(yaml: String): dynamic  // needs to be dynamic, otherwise kotlin thinks all the properties (i.e. completeStruct.meta or completeStruct.seq) don't exist
    fun dump(obj: dynamic): String
}

enum class DisplayStyle {
    HEX, BINARY, SIGNED_INTEGER, UNSIGNED_INTEGER, FLOAT, STRING
}

// acts just like a MutableList except it also has the added features
class MutableListTree<T>(private val innerList: MutableList<T> = mutableListOf()) : MutableList<T> by innerList {
    var byteOrder = ByteOrder.BIG

    var parent: MutableListTree<T>? = null
        get() { return field }
        set(value) {field = value}

    var root: MutableListTree<T>? = null
        get() {
            var rootCandidate: MutableListTree<T> = this
            while (rootCandidate.parent != null) {
                rootCandidate = rootCandidate.parent!!
            }
            return rootCandidate
        }
        private set
}

class Type(seqElement : dynamic) {
    var byteOrder: ByteOrder = ByteOrder.BIG
    var sizeInBits: UInt = 0u
    var sizeIsUntilEOS: Boolean = false
    var sizeIsUntilTerminator: Boolean = false
    var terminator: ByteArray? = null
    var type: String? = if (seqElement.type) {seqElement.type.toString()} else {null}
    var usedDisplayStyle: DisplayStyle = DisplayStyle.HEX
    var hasCustomType: Boolean = false
}

// TODO[IMPORTANT]: Move the ByteWitchDecoder into a companion object as the parse methods are static and do not need an instance. See the other decoders for examples
class Kaitai(val kaitaiName: String, val kaitaiStruct: String) : ByteWitchDecoder {
    override val name = "Kaitai-$kaitaiName"
    var completeStruct: dynamic = undefined

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        completeStruct = try {
            JsYaml.load(kaitaiStruct)
        } catch (e: dynamic) {
            throw Exception("Unexpected Error occurred during parsing of Kaitai Yaml File. Is it syntactically correct Yaml?\n$e")
        }
        val result = try {  // JS Exceptions don't get simply logged to console but instead trigger the big red overlay. We convert JS Errors to Kotlin Exceptions here
            val rootID = if (completeStruct.meta != undefined && completeStruct.meta.id != undefined) {
                completeStruct.meta.id
            } else {
                name
            }
            processSeq(rootID, parentBytesListTree = null, completeStruct, data.toBooleanArray(), sourceOffset, _offsetInDatastreamInBits = 0)
        } catch (e: dynamic) {  // with dynamic, we catch all exceptions, however. But that's fine too
            throw Exception("Unexpected Exception has been thrown:\n$e")
        }
        return result
    }

    fun parseReference(reference: String, bytesListTree: MutableListTree<KaitaiElement>): BooleanArray {
        // if it is not a valid reference, i.e. there exists no element in the seq with the id @param: reference, then an exception is thrown
        if (reference.startsWith("_root.")) //_root.magic
            return parseReference(reference.removePrefix("_root."), bytesListTree.root!!)
        else if (reference.startsWith("_parent.")) 
            return parseReference(reference.removePrefix("_parent."), bytesListTree.parent!!)
        else (
            if (reference.contains("."))
                return parseReference(reference.substringAfter("."), bytesListTree.find { it.id == reference.substringBefore(".") }!!.bytesListTree!!)
            else
                return bytesListTree.find { it.id == reference }!!.value
        )
    }


    enum class TokenType (val symbol: String) {
        INTEGER(""),
        FLOAT(""),
        STRING(""), // enclosed by '' or ""
        IDENTIFIER(""), // starts with a lower case literal
        BOOLEAN("true/false"),
        PLUS("+"), // addition and string concatenation
        MINUS("-"),
        MUL("*"),
        DIV("/"),
        MODULO("%"),
        LESS("<"),
        GREATER(">"),
        LESSEQUAL("<="),
        GREATEREQUAL(">="),
        EQUAL("=="),
        NOTEQUAL("!="),
        LSHIFT("<<"),
        RSHIFT(">>"),
        BITWISEAND("&"),
        BITWISEOR("|"),
        BITWISEXOR("^"),
        BOOLEANNOT("not"),
        BOOLEANAND("and"),
        BOOLEANOR("or"),
        QUESTIONMARK("?"),
        COLON(":"),
        PARENTHESES("()"),
        BRACKETS("[]"),
        FUNCTION("f(x)"),
        DOT("."),
        DOUBLECOLON("::"),
        EMPTY(""),
        CAST("as<>"),
        REFERENCE(""),
    }

    //******************************************************************************************************************
    //*                                                 operands                                                       *
    //******************************************************************************************************************

    val operandTokens = mapOf(
        TokenType.INTEGER to ::parseInteger,
        TokenType.FLOAT to ::parseFloat,
        TokenType.STRING to ::parseString,
        TokenType.BOOLEAN to ::parseBoolean,
        TokenType.PARENTHESES to ::parseParentheses,
        TokenType.REFERENCE to ::parseReference2 // TODO
    )

    fun parseInteger(token: Pair<TokenType, dynamic>) : Int = token.second

    fun parseFloat(token: Pair<TokenType, dynamic>) : Float = token.second

    fun parseString(token: Pair<TokenType, dynamic>) : String = token.second

    fun parseBoolean(token: Pair<TokenType, dynamic>) : Boolean = token.second

    fun parseParentheses(token: Pair<TokenType, dynamic>) : dynamic = parseExpression(token.second)

    fun parseReference2(token: Pair<TokenType, dynamic>) : dynamic = null //TODO

    //******************************************************************************************************************
    //*                                           unary operators                                                      *
    //******************************************************************************************************************

    val unaryPrecedence = mapOf( // unary tokens
        TokenType.PLUS to ::parseUnaryPlus,
        TokenType.MINUS to ::parseUnaryMinus,
        TokenType.BOOLEANNOT to ::parseBooleanNot
    )

    fun parseUnaryPlus(tokens: MutableList<Pair<TokenType, dynamic>>) : Number {
        val op: dynamic = parseTokens(tokens)
        return when (op) {
            is Float -> +op
            is Int -> +op
            else -> throw Exception("Cannot apply unary plus to non number")
        }
    }

    fun parseUnaryMinus(tokens: MutableList<Pair<TokenType, dynamic>>) : Number {
        val op: dynamic = parseTokens(tokens)
        return when (op) {
            is Float -> -op
            is Int -> -op
            else -> throw Exception("Cannot apply unary minus to non number")
        }
    }

    fun parseBooleanNot(tokens: MutableList<Pair<TokenType, dynamic>>) : Boolean {
        val op: dynamic = parseTokens(tokens)
        return if (op is Boolean)
                !op
            else
                throw Exception("Cannot apply logical or to non boolean")
    }

    //******************************************************************************************************************
    //*                                          binary operators                                                      *
    //******************************************************************************************************************

    val binaryPrecedence = listOf( // binary tokens
        mapOf(TokenType.MUL to ::parseMul, TokenType.DIV to ::parseDiv, TokenType.MODULO to ::parseModulo),
        mapOf(TokenType.PLUS to ::parsePlus, TokenType.MINUS to ::parseMinus),
        mapOf(TokenType.LSHIFT to ::parseLShift, TokenType.RSHIFT to ::parseRShift),
        mapOf(TokenType.LESS to ::parseLess, TokenType.LESSEQUAL to ::parseLessEqual, TokenType.GREATER to ::parseGreater, TokenType.GREATEREQUAL to ::parseGreaterEqual),
        mapOf(TokenType.EQUAL to ::parseEqual, TokenType.NOTEQUAL to ::parseNotEqual),
        mapOf(TokenType.BITWISEAND to ::parseBitwiseAnd),
        mapOf(TokenType.BITWISEXOR to ::parseBitwiseXor),
        mapOf(TokenType.BITWISEOR to ::parseBitwiseOr),
        mapOf(TokenType.BOOLEANAND to ::parseBooleanAnd),
        mapOf(TokenType.BOOLEANOR to ::parseBooleanOr),
    )

    fun parseMul(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Number{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return if (op1 is Float || op2 is Float)
            (op1 as Float) * (op2 as Float)
        else
            (op1 as Int) * (op2 as Int)
    }

    fun parseDiv(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Number{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return if (op1 is Float || op2 is Float)
            (op1 as Float )/ (op2 as Float)
        else
            (op1 as Int) / (op2 as Int)
    }

    fun parseModulo(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Number{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return if (op1 is Float || op2 is Float)
            (op1 as Float) % (op2 as Float)
        else
            (op1 as Int) % (op2 as Int)
    }

    fun parsePlus(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : dynamic{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return if (op1 is String || op2 is String)
            (op1 as String) + (op2 as String)
        else if (op1 is Float || op2 is Float)
            (op1 as Float) + (op2 as Float)
        else
            (op1 as Int) + (op2 as Int)

    }

    fun parseMinus(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Number{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return if (op1 is Float || op2 is Float)
            (op1 as Float) - (op2 as Float)
        else
            (op1 as Int) - (op2 as Int)
    }

    fun parseLShift(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Int{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) shl (op2 as Int)
    }

    fun parseRShift(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Int{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) shr (op2 as Int)
    }

    fun parseLess(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Boolean{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) < (op2 as Int)
    }

    fun parseLessEqual(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Boolean{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) <= (op2 as Int)
    }

    fun parseGreater(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Boolean{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) > (op2 as Int)
    }

    fun parseGreaterEqual(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Boolean{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) >= (op2 as Int)
    }

    fun parseEqual(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Boolean{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return op1 == op2
    }

    fun parseNotEqual(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Boolean{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return op1 != op2
    }

    fun parseBitwiseAnd(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Int{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) and (op2 as Int)
    }

    fun parseBitwiseXor(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Int{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) xor (op2 as Int)
    }

    fun parseBitwiseOr(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Int{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Int) or (op2 as Int)
    }

    fun parseBooleanAnd(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Boolean{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Boolean) && (op2 as Boolean)
    }

    fun parseBooleanOr(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>) : Boolean{
        val op1: dynamic = parseTokens(tokens1)
        val op2: dynamic = parseTokens(tokens2)

        return (op1 as Boolean) || (op2 as Boolean)
    }

    //******************************************************************************************************************
    //*                                         ternary operators                                                      *
    //******************************************************************************************************************

    fun parseIfElse(condition: MutableList<Pair<TokenType, dynamic>>, tokensTrue: MutableList<Pair<TokenType, dynamic>>, tokensFalse: MutableList<Pair<TokenType, dynamic>>) : dynamic {
        return if (parseTokens(condition) as Boolean) parseTokens(tokensTrue) else parseTokens(tokensFalse)
    }

    fun parseTokens (tokens: MutableList<Pair<TokenType, dynamic>>) : dynamic {
        if (tokens.size == 1 && tokens[0].first in operandTokens){
            val function = operandTokens.getValue(tokens[0].first)
            return function.invoke(tokens[0])
        }

        if (tokens.contains(Pair(TokenType.QUESTIONMARK, "?"))){
            var depth: Int = 0
            var posQuestionmark: Int = 0
            var posColon: Int
            for ((index,token) in tokens.withIndex()) {
                if (token == Pair(TokenType.QUESTIONMARK, "?")){
                    if (depth == 0) posQuestionmark = index
                    depth++
                } else if (token == Pair(TokenType.COLON, ":")){
                    posColon = index
                    depth--
                    if(depth == 0)
                        return parseIfElse(tokens.subList(0, posQuestionmark), tokens.subList(posQuestionmark+1, posColon), tokens.subList(posColon+1, tokens.size))
                }
            }
        }

        for (operatorMap in binaryPrecedence.reversed()) {
            for ((index, token) in tokens.reversed().withIndex()) {
                if (index != tokens.size-1 && tokens.reversed()[index+1].first in operandTokens && token.first in operatorMap) {
                    val op1: MutableList<Pair<TokenType, dynamic>> = tokens.subList(0, tokens.size-index-1)
                    val op2: MutableList<Pair<TokenType, dynamic>> = tokens.subList(tokens.size-index, tokens.size)
                    val function = operatorMap.getValue((token.first))
                    return function.invoke(op1, op2)
                }
            }
        }

        for ((index, token) in tokens.withIndex()){
            if (token.first in unaryPrecedence) {
                val op: MutableList<Pair<TokenType, dynamic>> = tokens.subList(index+1, tokens.size)
                val function = unaryPrecedence.getValue(token.first)
                return function.invoke(op)
            }
        }

        return null
    }

    val operators = setOf("+", "-", "*", "/", "%", "<", ">", "&", "|", "^", "?", ":", ".")
    val operators2 = setOf("<=", ">=", "==", "!=", "<<", ">>", "::")

    val operatorMap: Map<String, TokenType> = TokenType.entries.associateBy { it.symbol }
    val operator2Map: Map<String, TokenType> = TokenType.entries.associateBy { it.symbol }

    fun getOperator(char: Char): TokenType = operatorMap[char.toString()]!!
    fun getOperator2(string: String): TokenType = operator2Map[string]!!

    fun isKeyword(char: Char?) : Boolean {
        return (char == null
                || (!char.isLetterOrDigit()
                && char != '_'))
    }

    fun nextToken(expression: String) : Pair<Pair<TokenType, dynamic>, Int> {
        val trimmedExpression = expression.trimStart()
        if (trimmedExpression.isEmpty()) return Pair(Pair(TokenType.EMPTY, null), 0)
        val trimmed = expression.length - trimmedExpression.length

        if (trimmedExpression.length >= 5 && trimmedExpression.startsWith("false")) // token FALSE
            if (isKeyword(trimmedExpression.getOrNull(5)))
                return Pair(Pair(TokenType.BOOLEAN, false), trimmed+5)
        if (trimmedExpression.length >= 4 && trimmedExpression.startsWith("true")) // token TRUE
            if (isKeyword(trimmedExpression.getOrNull(4)))
                return Pair(Pair(TokenType.BOOLEAN, true), trimmed+4)
        if (trimmedExpression.length >= 3 && trimmedExpression.startsWith("not")) // token NOT
            if (isKeyword(trimmedExpression.getOrNull(3)))
                return Pair(Pair(TokenType.BOOLEANNOT, "not"), trimmed+3)
        if (trimmedExpression.length >= 3 && trimmedExpression.startsWith("and")) // token AND
            if (isKeyword(trimmedExpression.getOrNull(3)))
                return Pair(Pair(TokenType.BOOLEANAND, "and"), trimmed+3)
        if (trimmedExpression.length >= 2 && trimmedExpression.startsWith("or")) // token OR
            if (isKeyword(trimmedExpression.getOrNull(2)))
                return Pair(Pair(TokenType.BOOLEANOR, "or"), trimmed+2)
        if (trimmedExpression.length >= 2 && trimmedExpression.substring(0..1) in operators2) // tokens consisting of two symbols: "<=", ">=", "==", "!=", "<<", ">>", "::"
            return Pair(Pair(getOperator2(trimmedExpression.substring(0..1)), trimmedExpression.substring(0..1)) ,trimmed+2)
        if (trimmedExpression[0].toString() in operators) // one symbol tokens: "+", "-", "*", "/", "%", "<", ">", "&", "|", "^", "?", ":", "."
            return Pair(Pair(getOperator(trimmedExpression[0]), trimmedExpression[0].toString()), trimmed+1)

        var breakIndex: Int = 0

        if (trimmedExpression.startsWith("as<")) { // token CAST
            var depth: Int = 0
            var sqstring: Boolean = false
            var dqstring: Boolean = false
            for ((index, char) in trimmedExpression.substring(2).withIndex()) {
                when (char.toString()) {
                    "'" -> if (!dqstring) sqstring = !sqstring
                    "\"" -> if (!sqstring && (!dqstring || trimmedExpression[index-1] != '\\')) dqstring = !dqstring
                    "<" -> if (!(sqstring || dqstring)) depth++
                    ">" -> {
                        if (!(sqstring || dqstring)) {
                            depth--
                            if (depth == 0) {
                                breakIndex = index + 1
                                break
                            }
                        }
                    }
                }
            }
            return Pair(Pair(TokenType.CAST, trimmedExpression.substring(0..breakIndex+1)), trimmed+breakIndex+2)
        }

        when (trimmedExpression[0]) {
            '(' -> { // token PARENTHESES
                var depth: Int = 0
                var sqstring: Boolean = false
                var dqstring: Boolean = false
                for ((index, char) in trimmedExpression.withIndex()) {
                    when (char.toString()) {
                        "'" -> if (!dqstring) sqstring = !sqstring
                        "\"" -> if (!sqstring && (!dqstring || trimmedExpression[index-1] != '\\')) dqstring = !dqstring
                        "(" -> if (!(sqstring || dqstring)) depth++
                        ")" -> {
                            if (!(sqstring || dqstring)) {
                                depth--
                                if (depth == 0) {
                                    breakIndex = index + 1
                                    break
                                }
                            }
                        }
                    }
                }
                return Pair(Pair(TokenType.PARENTHESES, trimmedExpression.substring(1..breakIndex-2)), trimmed+breakIndex)
            }
            '[' -> { // token BRACKETS
                var depth: Int = 0
                var sqstring: Boolean = false
                var dqstring: Boolean = false
                for ((index, char) in trimmedExpression.withIndex()) {
                    when (char.toString()) {
                        "'" -> if (!dqstring) sqstring = !sqstring
                        "\"" -> if (!sqstring && (!dqstring || trimmedExpression[index-1] != '\\')) dqstring = !dqstring
                        "[" -> if (!(sqstring || dqstring)) depth++
                        "]" -> {
                            if (!(sqstring || dqstring)) {
                                depth--
                                if (depth == 0) {
                                    breakIndex = index + 1
                                    break
                                }
                            }
                        }
                    }
                }
                return Pair(Pair(TokenType.BRACKETS, trimmedExpression.substring(0..breakIndex-1)), trimmed+breakIndex)
            }
            '\'' -> { // token STRING with single quotation marks
                for ((index, char) in trimmedExpression.withIndex()) {
                    if (char == '\'' && index != 0) {
                        breakIndex = index+1
                        break
                    }
                }
                return Pair(Pair(TokenType.STRING, trimmedExpression.substring(1..breakIndex-2)), trimmed+breakIndex)
            }
            '"' -> { // token STRING with double quotation marks
                for ((index, char) in trimmedExpression.withIndex()) {
                    if (char == '"' && index != 0) {
                        breakIndex = index+1
                        break
                    }
                }
                return Pair(Pair(TokenType.STRING, trimmedExpression.substring(1..breakIndex-2)), trimmed+breakIndex)
            }
        }

        if (trimmedExpression[0].isLowerCase() || trimmedExpression[0] == '_') { // token IDENTIFIER
            for ((index, char) in trimmedExpression.withIndex()) {
                if (!char.isLetterOrDigit() && char != '_') {
                    breakIndex = index
                    break
                }
                breakIndex = index+1
            }
            return Pair(Pair(TokenType.IDENTIFIER, trimmedExpression.substring(0..breakIndex-1)), trimmed+breakIndex)
        } else if (trimmedExpression[0].isDigit()) { // tokens INTEGER and FLOAT
            val exceptions = setOf<Char>('x', 'X', 'o', 'O', 'b', 'B')
            if (trimmedExpression[0] == '0' && (trimmedExpression.getOrNull(1) == 'x' || trimmedExpression.getOrNull(1) == 'X')) { // token INTEGER in hexadecimal notation
                val hexadecimalRegex: Regex = Regex("^[a-fA-F0-9_]$")
                for ((index, char) in trimmedExpression.withIndex()) {
                    if (!hexadecimalRegex.matches(char.toString()) && !(index == 1 && char in exceptions)) {
                        breakIndex = index
                        break
                    }
                    breakIndex = index+1
                }
                return Pair(Pair(TokenType.INTEGER, trimmedExpression.substring(2..breakIndex-1).replace("_", "").toInt(16)), trimmed+breakIndex)
            }
            else if (trimmedExpression[0] == '0' && (trimmedExpression.getOrNull(1) == 'b' || trimmedExpression.getOrNull(1) == 'B')) { // token INTEGER in binary notation
                for ((index, char) in trimmedExpression.withIndex()) {
                    if (char != '0' && char != '1' && char != '_' && !(index == 1 && char in exceptions)) {
                        breakIndex = index
                        break
                    }
                    breakIndex = index+1
                }
                return Pair(Pair(TokenType.INTEGER, trimmedExpression.substring(2..breakIndex-1).replace("_", "").toInt(2)), trimmed+breakIndex)
            }
            else if (trimmedExpression[0] == '0' && (trimmedExpression.getOrNull(1) == 'o' || trimmedExpression.getOrNull(1) == 'O')) { // token INTEGER in octal notation
                val octalRegex: Regex = Regex("^[0-7_]$")
                for ((index, char) in trimmedExpression.withIndex()) {
                    if (!octalRegex.matches(char.toString()) && !(index == 1 && char in exceptions)) {
                        breakIndex = index
                        break
                    }
                    breakIndex = index+1
                }
                return Pair(Pair(TokenType.INTEGER, trimmedExpression.substring(2..breakIndex-1).replace("_", "").toInt(8)), trimmed+breakIndex)
            } else { // tokens INTEGER AND FLOAT in decimal notation
                var float_point: Boolean = false
                var exponential: Boolean = false
                for ((index, char) in trimmedExpression.withIndex()) {
                    if (!float_point && char == '.') { // FLOATS must contain exactly one '.'
                        float_point = true
                        continue
                    }
                    if (!exponential && (char == 'e' || char == 'E')) {
                        float_point = true // FLOAT can contain exponential notation, INTEGER cannot
                        exponential = true
                        continue
                    }
                    if ((char == '+' || char == '-') && (trimmedExpression[index-1] == 'e' || trimmedExpression[index-1] == 'E'))
                        continue
                    if (!char.isDigit() && char != '_') {
                        breakIndex = index
                        break
                    }
                    breakIndex = index+1
                }
                if (float_point)
                    return Pair(Pair(TokenType.FLOAT, trimmedExpression.substring(0..breakIndex-1).replace("_", "").toFloat()), trimmed+breakIndex)
                else
                    return Pair(Pair(TokenType.INTEGER, trimmedExpression.substring(0..breakIndex-1).replace("_", "").toInt()), trimmed+breakIndex)

            }
        }
        return Pair(Pair(TokenType.EMPTY, null), 0)
    }

    val tokensForReference = setOf<TokenType>(TokenType.IDENTIFIER, TokenType.DOT, TokenType.DOUBLECOLON, TokenType.FUNCTION,
        TokenType.CAST, TokenType.BRACKETS)

    fun tokenizeExpression(expression: String) : MutableList<Pair<TokenType, dynamic>> {
        var expression: String = expression
        var tokens: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()
        while (expression != "") {
            var token: Pair<Pair<TokenType, dynamic>, Int> = nextToken(expression)
            tokens.add(token.first)
            expression = expression.substring(token.second)
        }

        var tokensWithFunctions: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()

        for ((index, token: Pair<TokenType, dynamic>) in tokens.withIndex()) { // combining TokenType IDENTIFIER followed by PARENTHESES to TokenType FUNCTION
            if ((token.first == TokenType.IDENTIFIER && tokens[index+1].first == TokenType.PARENTHESES) || token.first == TokenType.EMPTY)
                continue
            else if (token.first == TokenType.PARENTHESES && index > 0 && tokens[index-1].first == TokenType.IDENTIFIER)
                tokensWithFunctions.add(Pair(TokenType.FUNCTION, Pair(tokens[index-1], token)))
            else
                tokensWithFunctions.add(token)
        }

        var tokensWithReferences: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()
        var reference: Boolean = false

        for (token: Pair<TokenType, dynamic> in tokensWithFunctions) { // combining tokens of types in val tokensForReference to TokenType REFERENCE
            if (token.first in tokensForReference) {
                if (!reference) {
                    tokensWithReferences.add(Pair(TokenType.REFERENCE, mutableListOf<Pair<TokenType, dynamic>>()))
                    reference = true
                }
                (tokensWithReferences.last().second as MutableList<Pair<TokenType, dynamic>>).add(token)
            } else {
                reference = false
                tokensWithReferences.add(token)
            }
        }

        return tokensWithReferences
    }

    fun parseExpression (expression: String) : dynamic {
        return parseTokens(tokenizeExpression(expression))
    }

    fun parseValue(value: dynamic, bytesListTree: MutableListTree<KaitaiElement>) : BooleanArray {
        val flattenedArray = js("[value].flat(2)")  // handle both cases of 0x0a as well as [0x0a]. // TODO Can someone find an elegant way without javascript?
        var fullyFlatArray = booleanArrayOf()
        for (element in flattenedArray) {
            if (element is Int) {
                fullyFlatArray += (element as Int).toMinimalAmountOfBytes(ByteOrder.BIG).toBooleanArray()
            } else {
                try {
                    fullyFlatArray += parseReference(element, bytesListTree)
                } catch (e: Exception) {
                    for (i in 0..element.length - 1) {
                        fullyFlatArray += (element.charCodeAt(i) as Int).toMinimalAmountOfBytes(ByteOrder.BIG).toBooleanArray()
                    }
                }
            }
        }
        return fullyFlatArray
    }

    fun checkContentsKey(seqElement: dynamic, dataBytes: BooleanArray, bytesListTree: MutableListTree<KaitaiElement>, kaitaiElement: KaitaiElement): Boolean {
        return parseValue(seqElement.contents, bytesListTree).contentEquals(dataBytes)
    }

    fun checkValidKey(seqElement: dynamic, dataBytes: BooleanArray, bytesListTree: MutableListTree<KaitaiElement>, kaitaiElement: KaitaiElement): Boolean {
        if (seqElement.valid.min != undefined || seqElement.valid.max != undefined) {
            val parsedValue = dataBytes.toByteArray().toUInt(ByteOrder.BIG)
            val parsedValidMin = parseValue(seqElement.valid.min, bytesListTree).toByteArray().toUInt(ByteOrder.BIG)
            if ((seqElement.valid.min != undefined) && (parsedValue < parsedValidMin)) {
                return false
            }
            val parsedValidMax = parseValue(seqElement.valid.max, bytesListTree).toByteArray().toUInt(ByteOrder.BIG)
            if ((seqElement.valid.max != undefined) && (parsedValue > parsedValidMax)) {
                return false
            }
            return true
        } else if (seqElement.valid["any-of"] != undefined) {
            for (i in 0..seqElement.valid["any-of"].length-1) {
                if (parseValue(seqElement.valid["any-of"][i], bytesListTree).contentEquals(dataBytes)) {
                    return true
                }
            }
            return false
        } else if (seqElement.valid.expr != undefined) {
            // TODO parse arbitrary expression
        } else {
            val valueToCheckAgainst = if (seqElement.valid.eq != undefined) {
                parseValue(seqElement.valid.eq, bytesListTree)
            } else {
                parseValue(seqElement.valid, bytesListTree)
            }
            return valueToCheckAgainst.padLeft(dataBytes.size - valueToCheckAgainst.size).contentEquals(dataBytes)
        }
        return true
    }

    /*
    parse a static path in a given scope recursively
     */
    fun parseStaticPath(currentScopeStruct: dynamic, path: String) : dynamic {
        if (path.split("::").size == 1)
            return currentScopeStruct.types[path]
        else {
            return parseStaticPath(currentScopeStruct.types[path.split("::")[0]], path.substringAfter("::")) // go one scope deeper
        }
    }

    // TODO: add support for imports
    /**
     * check if there is a fitting custom type defined in the current or global scope
     * @param path: might be just a type or prefixed by a path via "::"
     * @return null if type doesn't exist in any scope or appropriate scope if it does
     */
    fun getCustomType(currentScopeStruct: dynamic, path: String?) : dynamic {
        if (path == null) {
            return null
        }
        try {
            return parseStaticPath(currentScopeStruct, path)
        } catch (e: dynamic) { // catch js exceptions
            try {
                return parseStaticPath(completeStruct, path)
            } catch (e: dynamic) { // catch js exceptions
                return null
            }
        }
    }

    /*
    parse the byte order/endianness of a sequence element
     */
    fun parseByteOrder(currentScopeStruct: dynamic, seqElement: dynamic, bytesListTree: MutableListTree<KaitaiElement>): ByteOrder {
        var byteOrder = bytesListTree.byteOrder // use the byte order of the sequence

        if (seqElement.type != undefined) { // if the element has a type
            if (getCustomType(currentScopeStruct, seqElement.type) == null) { // if the type is not a custom type
                if (seqElement.type.endsWith("be")) {
                    byteOrder = ByteOrder.BIG
                } else if (seqElement.type.endsWith("le")) {
                    byteOrder = ByteOrder.LITTLE
                }
            }
        }
        return byteOrder
    }

    fun parseBuiltinType(seqElement : dynamic, bytesListTree: MutableListTree<KaitaiElement>, type: Type) : Type {
        if (type.type == null) {
            throw RuntimeException("Attempted to parse as builtin type null which is always invalid")
        }
        if (type.type == "strz") {
            type.usedDisplayStyle = DisplayStyle.STRING
            type.sizeIsUntilTerminator = true
            type.terminator = byteArrayOf(0x00)
        } else if (type.type == "str") {
            type.usedDisplayStyle = DisplayStyle.STRING
            if (type.terminator != undefined) {
                type.sizeIsUntilTerminator = true
                type.terminator = parseValue(seqElement.terminator, bytesListTree).toByteArray() // TODO: Make proper method out of this. Could probably be a value defined somewhere else aswell :(
            }
        } else {
            if (Regex("^s\\d+(le|be)?$").matches(type.type!!)) {  // signed int
                type.sizeInBits = type.type!!.filter { it.isDigit() }.toUInt() * 8u
                type.usedDisplayStyle = DisplayStyle.UNSIGNED_INTEGER
            } else if (Regex("^u\\d+(le|be)?$").matches(type.type!!)) {  // unsigned int
                type.sizeInBits = type.type!!.filter { it.isDigit() }.toUInt() * 8u
                type.usedDisplayStyle = DisplayStyle.SIGNED_INTEGER
            } else if (Regex("^f\\d+(le|be)?$").matches(type.type!!)) {  // float
                type.sizeInBits = type.type!!.filter { it.isDigit() }.toUInt() * 8u
                type.usedDisplayStyle = DisplayStyle.FLOAT
            } else if (Regex("^b\\d+(le|be)?$").matches(type.type!!)) {  // binary
                type.sizeInBits = type.type!!.filter { it.isDigit() }.toUInt()
                type.usedDisplayStyle = DisplayStyle.BINARY
            } else {
                throw RuntimeException("Attempted to parse as builtin type ${type.type} but that doesn't seem to be a valid type")
            }
        }
        return type
    }

    fun parseType(currentScopeStruct: dynamic, seqElement: dynamic, bytesListTree: MutableListTree<KaitaiElement>) : Type {
        var type = Type(seqElement)
        type.byteOrder = parseByteOrder(currentScopeStruct, seqElement, bytesListTree)
        if (seqElement.contents != undefined) {
            type.sizeInBits = parseValue(seqElement.contents, bytesListTree).size.toUInt()
        }
        if (seqElement["size-eos"] != undefined) {
            type.sizeIsUntilEOS = seqElement["size-eos"]
        }
        if (seqElement.terminator != undefined) {
            type.terminator = parseValue(seqElement.terminator, bytesListTree).toByteArray()
            type.sizeIsUntilTerminator = true
        }
        if (getCustomType(currentScopeStruct, type.type) != null) {  // parse custom type aka subtypes
            type.hasCustomType = true
        } else if (type.type != null) {  // parse builtin type
            type = parseBuiltinType(seqElement, bytesListTree, type)
        }
        if (seqElement.size != undefined) {
            var parsedValue = parseValue(seqElement.size, bytesListTree)
            type.sizeInBits = parsedValue.toByteArray().toUInt(ByteOrder.BIG) * 8u
        }
        return type
    }

    fun getRepetitionKind(seqElement: dynamic): String {
        val repetitionKind = if (seqElement.repeat != undefined) {
            seqElement.repeat
        } else {
            null
        }
        if (repetitionKind == "expr" && seqElement["repeat-expr"] == undefined) {
            throw Exception("With repeat type expr, a repeat-expr key is needed")
        } else if (repetitionKind == "until" && seqElement["repeat-expr"] == undefined) {
            throw Exception("With repeat type until, a repeat-until key is needed")
        }
        if (seqElement["repeat-expr"] != undefined && repetitionKind != "expr") {
            throw Exception("When repeat-expr key is defined, repetition needs to be set to expr")
        } else if (seqElement["repeat-until"] != undefined && repetitionKind != "until") {
            throw Exception("When repeat-until key is defined, repetition needs to be set to until")
        }
        return repetitionKind
    }

    fun processSeq(id: String, parentBytesListTree: MutableListTree<KaitaiElement>?, currentScopeStruct: dynamic, data: BooleanArray, sourceOffsetInBits: Int, _offsetInDatastreamInBits: Int) : KaitaiElement {
        var offsetInDatastreamInBits: Int = _offsetInDatastreamInBits
        /*
        Entweder data als ByteArray und Bitshiften
        var test = 13  -> 1101
        test[6..8] = 1
        test and 0b00000111 >> 0 = -> 101
        test and 0b00111000 >> 3 = -> 001
        oder data als BooleanArray so wie aktuell. Vermutlich inperformant, aber wohl gut genug
        */
        val bytesListTree = MutableListTree<KaitaiElement>()
        bytesListTree.parent = parentBytesListTree
        if (bytesListTree.parent != null) {
            bytesListTree.byteOrder = bytesListTree.parent!!.byteOrder
        } else {
            bytesListTree.byteOrder = ByteOrder.BIG
        }
        if (!(currentScopeStruct.meta == undefined || currentScopeStruct.meta.endian == undefined)) {
            bytesListTree.byteOrder = if (currentScopeStruct.meta.endian == "be") {
                ByteOrder.BIG
            } else {
                ByteOrder.LITTLE
            }
        }
        var dataSizeOfSequenceInBits: Int = 0

        for (seqElement in currentScopeStruct.seq) {
            val repetitionKind = getRepetitionKind(seqElement)
            var repeatAmount = if (repetitionKind == "expr") {
                parseValue(seqElement["repeat-expr"], bytesListTree).toByteArray().toUInt(ByteOrder.BIG)
            } else {
                null
            }
            while (true) {  // repeat key demands we create many elements possibly. We break at the very end of the loop
                val type = parseType(currentScopeStruct, seqElement, bytesListTree)
                if (type.sizeIsUntilEOS) {
                    type.sizeInBits = (data.size - offsetInDatastreamInBits).toUInt()
                }
                if (type.sizeIsUntilTerminator) {
                    val dataAsByteArray = data.toByteArray()  // technically not ideal as we don't check for byte-alignment, but right after we throw away all results if it's not byte aligned
                    type.sizeInBits = (dataAsByteArray.sliceArray(offsetInDatastreamInBits / 8 .. dataAsByteArray.size-1).indexOfFirstSubsequence(type.terminator).toUInt() + type.terminator!!.size.toUInt()) * 8u
                }

                // if we are operating on non byte aligned data, but we don't have a binary data type, something is very wrong
                if (((((offsetInDatastreamInBits + sourceOffsetInBits) % 8) != 0) && (type.usedDisplayStyle != DisplayStyle.BINARY))
                    && !type.hasCustomType) {  // if it has subtypes we ignore the problem for now and we'll see inside the subtype again
                    throw RuntimeException("Cannot have a non binary type that starts in the middle of a byte")
                }

                val elementId = seqElement.id

                var kaitaiElement : KaitaiElement
                var value = if (type.sizeInBits != 0u) {  // slice if we have a substream
                    data.sliceArray(offsetInDatastreamInBits .. offsetInDatastreamInBits + type.sizeInBits.toInt() -1)
                } else {
                    data
                }

                // flip bytes in case byteOrder is little and therefore different order than kotlins BigEndian
                if (!type.hasCustomType // no subtypes
                    && (type.usedDisplayStyle == DisplayStyle.FLOAT || type.usedDisplayStyle == DisplayStyle.SIGNED_INTEGER || type.usedDisplayStyle == DisplayStyle.UNSIGNED_INTEGER) // makes sense to flip
                    && type.byteOrder == ByteOrder.LITTLE) { // little needs flipping, big doesn't need it anyways
                    value = value.toByteArray().reversedArray().toBooleanArray()
                }

                if (type.hasCustomType) {
                    if (type.sizeInBits != 0u) {
                        kaitaiElement = processSeq(elementId, bytesListTree, getCustomType(currentScopeStruct, type.type), value, sourceOffsetInBits + dataSizeOfSequenceInBits, 0)
                    } else {
                        kaitaiElement = processSeq(elementId, bytesListTree, getCustomType(currentScopeStruct, type.type), value, sourceOffsetInBits + dataSizeOfSequenceInBits, offsetInDatastreamInBits)
                    }
                } else {
                    val sourceByteRange = Pair((sourceOffsetInBits + dataSizeOfSequenceInBits) / 8, (sourceOffsetInBits + dataSizeOfSequenceInBits + type.sizeInBits.toInt()) / 8)
                    val sourceRangeBitOffset = Pair((sourceOffsetInBits + dataSizeOfSequenceInBits) % 8, (sourceOffsetInBits + dataSizeOfSequenceInBits + type.sizeInBits.toInt()) % 8)

                    kaitaiElement = if (type.usedDisplayStyle == DisplayStyle.BINARY) {
                        KaitaiBinary(
                            elementId,
                            type.byteOrder,
                            value,
                            sourceByteRange,
                            sourceRangeBitOffset
                        )
                    } else if (type.usedDisplayStyle == DisplayStyle.STRING) {
                        KaitaiString(
                            elementId,
                            type.byteOrder,
                            value,
                            sourceByteRange,
                            sourceRangeBitOffset
                        )
                    } else { // displayStyle.HEX as the fallback (even if it's a known type like int or whatever
                        KaitaiBytes(
                            elementId,
                            type.byteOrder,
                            value,
                            sourceByteRange,
                            sourceRangeBitOffset
                        )
                    }
                }
                if ((seqElement.contents != undefined) && !checkContentsKey(seqElement, value, bytesListTree, kaitaiElement)) {
                    throw Exception("Value of bytes does not align with expected contents value.")
                }
                if ((seqElement.valid != undefined) && !checkValidKey(seqElement, value, bytesListTree, kaitaiElement)) {
                    throw Exception("Value of bytes does not align with expected valid value.")
                }

                if (type.sizeInBits == 0u) {  // only update value if it's still totally unknown. If we overwrite it always, then we would cut off seq that don't use the full datastream
                    type.sizeInBits = kaitaiElement.value.size.toUInt()
                }

                offsetInDatastreamInBits = offsetInDatastreamInBits + type.sizeInBits.toInt()
                dataSizeOfSequenceInBits += type.sizeInBits.toInt()

                bytesListTree.add(kaitaiElement)

                if (repetitionKind == "eos") {
                    if (data.size == dataSizeOfSequenceInBits) {
                        break
                    }
                } else if (repetitionKind == "expr") {
                    repeatAmount = repeatAmount!! - 1u
                    if (repeatAmount <= 0u) {
                        break
                    }
                } else if (repetitionKind == "until") {
                    throw Exception("repeat-until is not supported yet")
                    if (seqElement["repeat-until"]) {  // TODO Completely wrong, needs expression parsing to work
                        break
                    }
                } else {
                    break  // if we have no repeat at all we just do this inner loop once
                }
            }
        }

        val resultSourceByteRange = Pair((sourceOffsetInBits) / 8, (sourceOffsetInBits + data.size) / 8)
        val resultSourceRangeBitOffset = Pair((sourceOffsetInBits) % 8, (sourceOffsetInBits + data.size) % 8)
        return KaitaiResult(id, bytesListTree.byteOrder, bytesListTree, resultSourceByteRange, resultSourceRangeBitOffset)
    }
}

interface KaitaiElement : ByteWitchResult {
    val id: String
    val bytesListTree: MutableListTree<KaitaiElement>? get() = null
    val value: BooleanArray
    var endianness: ByteOrder
}

class KaitaiResult(override val id: String, override var endianness: ByteOrder,
                   override val bytesListTree: MutableListTree<KaitaiElement>, override val sourceByteRange: Pair<Int, Int>,
                   override val sourceRangeBitOffset: Pair<Int, Int>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${bytesListTree.joinToString("") { it.renderHTML() }})</div>"
    }

    // KaitaiResult result does not really have a value itself, but if it's called we want to deliver a reasonable result
    override val value: BooleanArray
        get() {
            var result = booleanArrayOf()
            for (element in bytesListTree) {
                result += element.value
            }
            return result
        }
}

class KaitaiBytes(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Int, Int>,
                  override val sourceRangeBitOffset: Pair<Int, Int>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().hex()})h</div>"
    }
}

class KaitaiInteger(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Int, Int>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().toInt(ByteOrder.BIG)})s</div>"
    }
}

class KaitaiBinary(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Int, Int>,
                   override val sourceRangeBitOffset: Pair<Int, Int>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.joinToString("") { if (it) "1" else "0" }})b</div>"
    }
}

class KaitaiString(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Int, Int>,
                   override val sourceRangeBitOffset: Pair<Int, Int>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().toUTF8String()})utf8</div>"
    }
}
/*
class KaitaiArray(val id: String, val endianness: ByteOrder, val value: ByteArray, override val sourceByteRange: Pair<Int, Int>): KaitaiElement {
    override fun renderHTML(): String {
        val formattedValues = ""
        for (i in 0 until (value.size / valuesSizeBytes)) {
            formattedValues.plus("<span>${value.slice(i*valuesSizeBytes ..i*valuesSizeBytes+valuesSizeBytes)}</span>")
        }
        return "<div class=\"generic roundbox\" $byteRangeDataTags>" +
                formattedValues +
                "</div>"
    }
}
*/