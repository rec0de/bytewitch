package decoders

import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.hex
import bitmage.padLeft
import bitmage.toBooleanArray
import bitmage.toByteArray
import bitmage.toInt
import bitmage.toMinimalAmountOfBytes
import bitmage.toUInt
import bitmage.toUTF8String
import kaitai.KTRepeat
import kaitai.KTSeq
import kaitai.KTStruct
import kaitai.KTType
import kaitai.KTValid
import kaitai.StringOrInt
import kaitai.toByteOrder
import kaitai.KTEnum
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

enum class DisplayStyle {
    HEX, BINARY, SIGNED_INTEGER, UNSIGNED_INTEGER, FLOAT, STRING, ENUM
}

// acts just like a MutableList except it also has the added features specifically for Kaitai stuff
class MutableKaitaiTree (private val innerList: MutableList<KaitaiElement> = mutableListOf()) : MutableList<KaitaiElement> by innerList {
    var byteOrder = ByteOrder.BIG

    // getter and setters for integer ids already implemented, now we do them for strings aswell, as ids are unique
    operator fun get(id : String): KaitaiElement {
        val element = this.find { it.id == id }
        if (element == null) {
            throw Exception("Could not find element with id $id")
        }
        return element
    }

    operator fun set(id: String, element: KaitaiElement) {
        this[id] = element
    }

    var parent: MutableKaitaiTree? = null
        get() { return field }
        set(value) {field = value}

    var root: MutableKaitaiTree? = null
        get() {
            var rootCandidate: MutableKaitaiTree = this
            while (rootCandidate.parent != null) {
                rootCandidate = rootCandidate.parent!!
            }
            return rootCandidate
        }
        private set
}

class Type(val type: String?) {
    var byteOrder: ByteOrder = ByteOrder.BIG
    var sizeInBits: Int = 0
    var sizeIsKnown: Boolean = false
    var sizeIsUntilEOS: Boolean = false
    var terminator: Int? = null
    var usedDisplayStyle: DisplayStyle = DisplayStyle.HEX
    var customType: KTStruct? = null
}

class Kaitai(kaitaiName: String, val kaitaiStruct: KTStruct, val canonicalPath: String) : ByteWitchDecoder {
    override val name = "Kaitai-$kaitaiName"

    private val importedStructs = mutableMapOf<String, KTStruct>()

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val result = try {  // JS Exceptions don't get simply logged to console but instead trigger the big red overlay. We convert JS Errors to Kotlin Exceptions here
            val imports = kaitaiStruct.meta?.imports
            if (imports != null) {
                importedStructs.putAll(importTypes(imports))
            }

            val id = kaitaiStruct.meta?.id ?: name
            processSeq(id, null, parentBytesListTree = null, null, kaitaiStruct, data.toBooleanArray(), sourceOffset, _offsetInDatastreamInBits = 0)
        } catch (e: dynamic) {  // with dynamic, we catch all exceptions, however. But that's fine too
            console.error(e)
            throw Exception("Unexpected Exception has been thrown:\n$e")
        }
        return result
    }

    fun parseReference(reference: String, bytesListTree: MutableKaitaiTree): BooleanArray {
        // if it is not a valid reference, i.e. there exists no element in the seq with the id @param: reference, then an exception is thrown
        if (reference.startsWith("_root.")) //_root.magic
            return parseReference(reference.removePrefix("_root."), bytesListTree.root!!)
        else if (reference.startsWith("_parent."))
            return parseReference(reference.removePrefix("_parent."), bytesListTree.parent!!)
        else (
            if (reference.contains("."))
                return parseReference(reference.substringAfter("."), bytesListTree[reference.substringBefore(".")]!!.bytesListTree!!)
            else
                return bytesListTree[reference]!!.value
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

    fun parseValue(value: String, bytesListTree: MutableKaitaiTree) : BooleanArray {
        return parseValue(listOf(value), bytesListTree)
    }

    fun parseValue(value: List<String>, bytesListTree: MutableKaitaiTree) : BooleanArray {
        var fullyFlatArray = booleanArrayOf()
        for (element in value) {
            if (element.toIntOrNull() != null) {
                fullyFlatArray += element.toInt().toMinimalAmountOfBytes(ByteOrder.BIG).toBooleanArray()
            } else {
                try {
                    fullyFlatArray += parseReference(element, bytesListTree)
                } catch (e: Exception) {
                    fullyFlatArray += element.encodeToByteArray().toBooleanArray()
                }
            }
        }
        return fullyFlatArray
    }

    fun checkContentsKey(contents: List<String>, dataBytes: BooleanArray, bytesListTree: MutableKaitaiTree) : Boolean {
        return parseValue(contents, bytesListTree).contentEquals(dataBytes)
    }

    // TODO: refactor
    fun checkValidKey(valid: KTValid, dataBytes: BooleanArray, bytesListTree: MutableKaitaiTree) : Boolean {
        if (valid.min != null || valid.max != null) {
            val parsedValue = dataBytes.toByteArray().toUInt(ByteOrder.BIG)
            if (valid.min != null) {
                val parsedValidMin = parseValue(valid.min, bytesListTree).toByteArray().toUInt(ByteOrder.BIG)
                if (parsedValue < parsedValidMin) {
                    return false
                }
            }
            if (valid.max != null) {
                val parsedValidMax = parseValue(valid.max, bytesListTree).toByteArray().toUInt(ByteOrder.BIG)
                if (parsedValue > parsedValidMax) {
                    return false
                }
            }
            return true
        } else if (valid.anyOf != null) {
            for (entry in valid.anyOf) {
                if (parseValue(entry, bytesListTree).contentEquals(dataBytes)) {
                    return true
                }
            }
            return false
        } else if (valid.expr != null) {
            // TODO parse arbitrary expression
            return true
        } else if (valid.eq != null) {
            val valueToCheckAgainst = parseValue(valid.eq, bytesListTree)
            return valueToCheckAgainst.padLeft(dataBytes.size - valueToCheckAgainst.size).contentEquals(dataBytes)
        }
        return true
    }

    fun importTypes(imports: List<String>) : Map<String, KTStruct> {
        val result = mutableMapOf<String, KTStruct>()
        for (import in imports) {
            val currentPath = if (import.startsWith("/")) {
                emptyList<String>()
            } else {
                canonicalPath.split("/").dropLast(1)
            }
            val importPath = import.split("/").filter { it != "." && it != "" }
            for (part in importPath) {
                if (part == "..") {
                    currentPath.dropLast(1)
                } else {
                    currentPath + part
                }
            }
            val canonicalPath = currentPath.joinToString(separator = "/")

            val struct = ByteWitch.findKaitaiStructByPath(canonicalPath)
            checkNotNull(struct) { "Could not find imported file at path $canonicalPath for import $import" }
            val name = struct.meta?.id ?: import.substringAfterLast("/")
            result[name] = struct
        }
        return result
    }

    fun getImportedType(path: String) : KTStruct? {
        val elements = path.split("::", limit = 2)
        val type = importedStructs[elements[0]]
        if (type != null && elements.size == 2) {
            return getCustomType(type, elements[1]) // go one scope deeper
        }
        return type
    }

    /**
     * check if there is a fitting custom type defined in the current scope
     * @param path: might be just a type or prefixed by a path via "::"
     * @return null if type doesn't exist in any scope or appropriate scope if it does
     */
    fun getCustomType(currentScopeStruct: KTStruct?, path: String) : KTStruct? {
        if (currentScopeStruct == null) {
            return null
        } else {
            val elements = path.split("::", limit = 2)
            val type = currentScopeStruct.types[elements[0]]
            if (type != null && elements.size == 2) {
                return getCustomType(type, elements[1]) // go one scope deeper
            }
            return type
        }
    }

    fun parseBuiltinType(type: Type) : Type {
        if (type.type == null) {
            throw RuntimeException("Attempted to parse as builtin type null which is always invalid")
        }
        if (type.type == "strz") {
            type.usedDisplayStyle = DisplayStyle.STRING
            type.terminator = 0
        } else if (type.type == "str") {
            type.usedDisplayStyle = DisplayStyle.STRING
        } else {
            val match = Regex("^([sufb])(\\d+)(le|be)?$").find(type.type)
            if (match != null) {
                val typePrefix = match.groupValues[1]
                val size = match.groupValues[2].toInt()
                when (typePrefix) {
                    "s" -> {
                        type.usedDisplayStyle = DisplayStyle.SIGNED_INTEGER
                        type.sizeInBits = size * 8
                    }
                    "u" -> {
                        type.usedDisplayStyle = DisplayStyle.UNSIGNED_INTEGER
                        type.sizeInBits = size * 8
                    }
                    "f" -> {
                        type.usedDisplayStyle = DisplayStyle.FLOAT
                        type.sizeInBits = size * 8
                    }
                    "b" -> {
                        type.usedDisplayStyle = DisplayStyle.BINARY
                        type.sizeInBits = size
                    }
                }
                if (match.groupValues[3] == "le") {
                    type.byteOrder = ByteOrder.LITTLE
                } else if (match.groupValues[3] == "be") {
                    type.byteOrder = ByteOrder.BIG
                }
                type.sizeIsKnown = true
            } else {
                throw RuntimeException("Attempted to parse as builtin type ${type.type} but that doesn't seem to be a valid type")
            }
        }
        return type
    }

    fun parseType(parentScopeStruct: KTStruct?, currentScopeStruct: KTStruct, seqElement: KTSeq, bytesListTree: MutableKaitaiTree) : Type {
        if (seqElement.type is KTType.Switch) {
            throw RuntimeException("Switches are not supported yet")
        }
        var type = Type((seqElement.type as KTType.Primitive?)?.type)

        type.byteOrder = bytesListTree.byteOrder
        if (seqElement.contents != null) {
            type.sizeInBits = parseValue(seqElement.contents, bytesListTree).size
            type.sizeIsKnown = true
        }

        type.sizeIsUntilEOS = seqElement.sizeEos
        type.terminator = seqElement.terminator

        if (type.type != null) {
            // Check if we have a custom type defined in the current scope or in the global scope
            val customTypeCandidate =
                getCustomType(currentScopeStruct, type.type) ?:
                getCustomType(parentScopeStruct, type.type)?:
                getCustomType(kaitaiStruct, type.type) ?:
                getImportedType(type.type)
            if (customTypeCandidate != null) {  // parse custom type aka subtypes
                type.customType = customTypeCandidate
            } else {  // parse builtin type
                type = parseBuiltinType(type)
            }
        }
        if (seqElement.size != null) {  // we do this last, because in some cases other sizes can be overwritten
            if (seqElement.size is StringOrInt.IntValue) {
                type.sizeInBits = seqElement.size.value * 8
            } else {
                val parsedValue = parseValue(seqElement.size.toString(), bytesListTree)
                type.sizeInBits = parsedValue.toByteArray().toInt(ByteOrder.BIG) * 8
            }
            type.sizeIsKnown = true
        }
        return type
    }

    fun getEnum(currentScopeStruct: KTStruct?, path: String) : KTEnum? {
        if (currentScopeStruct == null) {
            return null
        } else {
            val elements = path.split("::")
            if (elements.size > 1) {
                val type = currentScopeStruct.types[elements[0]]
                if (type != null) return getEnum(type, path.substringAfter("::"))
            }

            return currentScopeStruct.enums[elements[0]]
        }
    }

    private fun processSingleSeqElement(
        elementId: String,
        seqElement: KTSeq,
        parentScopeStruct: KTStruct?,
        currentScopeStruct: KTStruct,
        bytesListTree: MutableKaitaiTree,
        ioStream: BooleanArray,
        _offsetInDatastreamInBits: Int,
        sourceOffsetInBits: Int,
        _dataSizeOfSequenceInBits: Int,
    ): Triple<KaitaiElement, Int, Int> {
        var offsetInDatastreamInBits = _offsetInDatastreamInBits
        var dataSizeOfSequenceInBits = _dataSizeOfSequenceInBits

        if (seqElement.pos != null) {
            offsetInDatastreamInBits = if (seqElement.pos is StringOrInt.IntValue) {
                seqElement.pos.value * 8
            } else {
                Int.fromBytes(parseValue(seqElement.pos.toString(), bytesListTree).toByteArray(), ByteOrder.BIG) * 8
            }
        }

        val type = parseType(parentScopeStruct, currentScopeStruct, seqElement, bytesListTree)
        if (type.sizeIsUntilEOS) {
            type.sizeInBits = (ioStream.size - offsetInDatastreamInBits)
            type.sizeIsKnown = true
        }
        if (type.terminator != null) {
            val dataAsByteArray =
                ioStream.toByteArray()  // technically not ideal as we don't check for byte-alignment, but right after we throw away all results if it's not byte aligned
            type.sizeInBits =
                (dataAsByteArray.sliceArray(offsetInDatastreamInBits / 8..dataAsByteArray.size - 1).indexOf(type.terminator!!.toByte()) + 1) * 8
            type.sizeIsKnown = true
        }

        // if we are operating on non byte aligned data, but we don't have a binary data type, something is very wrong
        if (((((offsetInDatastreamInBits + sourceOffsetInBits) % 8) != 0) && (type.usedDisplayStyle != DisplayStyle.BINARY))
            && type.customType == null
        ) {  // if it has subtypes we ignore the problem for now and we'll see inside the subtype again
            throw RuntimeException("Cannot have a non binary type that starts in the middle of a byte")
        }

        var kaitaiElement: KaitaiElement
        var ioSubStream = if (type.sizeIsKnown) {  // slice if we have a substream
            ioStream.sliceArray(offsetInDatastreamInBits..offsetInDatastreamInBits + type.sizeInBits - 1)
        } else {
            ioStream
        }

        // flip bytes in case byteOrder is little and therefore different order than kotlins BigEndian
        if (type.customType == null // no subtypes
            && (type.usedDisplayStyle == DisplayStyle.FLOAT || type.usedDisplayStyle == DisplayStyle.SIGNED_INTEGER || type.usedDisplayStyle == DisplayStyle.UNSIGNED_INTEGER) // makes sense to flip
            && type.byteOrder == ByteOrder.LITTLE
        ) { // little needs flipping, big doesn't need it anyways
            ioSubStream = ioSubStream.toByteArray().reversedArray().toBooleanArray()
        }

        // get the enum value
        var enum: Pair<KTEnum?, String> = Pair(null, "")
        if (seqElement.enum != null &&
            (type.usedDisplayStyle == DisplayStyle.SIGNED_INTEGER ||
                    type.usedDisplayStyle == DisplayStyle.UNSIGNED_INTEGER ||
                    (type.usedDisplayStyle == DisplayStyle.BINARY && type.sizeInBits == 1))
        ) {
            val path: KTEnum? = getEnum(currentScopeStruct, seqElement.enum) ?:
            getEnum(parentScopeStruct, seqElement.enum)?:
            getEnum(kaitaiStruct, seqElement.enum)
            if (path != null) {
                if (type.usedDisplayStyle == DisplayStyle.UNSIGNED_INTEGER) {
                    if (path[Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toUInt().toLong()] == null) {
                        throw RuntimeException(
                            "The enum ${seqElement.enum} has no key-value pair with the given key ${
                                Int.fromBytes(
                                    ioSubStream.toByteArray(),
                                    ByteOrder.BIG
                                )
                            }"
                        )
                    } else {
                        enum = Pair(
                            path,
                            path[Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toUInt().toLong()]!!.id.toString()
                        )
                    }
                } else if (type.usedDisplayStyle == DisplayStyle.SIGNED_INTEGER) {
                    if (path[Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toLong()] == null) {
                        throw RuntimeException(
                            "The enum ${seqElement.enum} has no key-value pair with the given key ${
                                Int.fromBytes(
                                    ioSubStream.toByteArray(),
                                    ByteOrder.BIG
                                )
                            }"
                        )
                    } else {
                        enum = Pair(
                            path,
                            path[Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toLong()]!!.id.toString()
                        )
                    }
                } else {
                    if (path[if (ioSubStream[0]) 1 else 0] == null) {
                        val temp = if (ioSubStream[0]) 1 else 0
                        throw RuntimeException(
                            "The enum ${seqElement.enum} has no key-value pair with the given key ${
                                Long.fromBytes(
                                    ioSubStream.toByteArray(),
                                    ByteOrder.BIG
                                )
                            }"
                        )
                    } else {
                        enum = Pair(
                            path,
                            path[if (ioSubStream[0]) 1 else 0]!!.id.toString()
                        )
                    }
                }
                type.usedDisplayStyle = DisplayStyle.ENUM
            } else {
                throw RuntimeException("The given enum ${seqElement.enum} does not exist.")
            }
        }

        if (type.customType != null) {
            kaitaiElement = processSeq(
                elementId,
                seqElement,
                bytesListTree,
                currentScopeStruct,
                type.customType!!,
                ioSubStream,
                sourceOffsetInBits + dataSizeOfSequenceInBits,
                if (type.sizeInBits != 0) 0 else offsetInDatastreamInBits  // reset offset in case of substream
            )

            if (seqElement.doc == null && seqElement.docRef == null) {
                // current sequence element has no doc, so we try to get the doc from the type
                kaitaiElement.doc = KaitaiDoc(type.customType?.doc, type.customType?.docRef)
            }
        } else {
            val sourceByteRange = Pair(
                (sourceOffsetInBits + dataSizeOfSequenceInBits) / 8,
                (sourceOffsetInBits + dataSizeOfSequenceInBits + type.sizeInBits) / 8
            )
            val sourceRangeBitOffset = Pair(
                (sourceOffsetInBits + dataSizeOfSequenceInBits) % 8,
                (sourceOffsetInBits + dataSizeOfSequenceInBits + type.sizeInBits) % 8
            )
            val elementDoc = KaitaiDoc(seqElement.doc, seqElement.docRef)

            kaitaiElement = when (type.usedDisplayStyle) {
                DisplayStyle.BINARY -> KaitaiBinary(elementId, type.byteOrder, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.STRING -> KaitaiString(elementId, type.byteOrder, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.SIGNED_INTEGER -> KaitaiSignedInteger(elementId, type.byteOrder, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.UNSIGNED_INTEGER -> KaitaiUnsignedInteger(elementId, type.byteOrder, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.FLOAT -> KaitaiFloat(elementId, type.byteOrder, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.ENUM -> KaitaiEnum(elementId, type.byteOrder, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc, enum)
                // displayStyle.HEX as the fallback (even if it's a known type or whatever)
                else -> KaitaiBytes(elementId, type.byteOrder, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
            }
        }

        if (seqElement.contents != null && !checkContentsKey(seqElement.contents, ioSubStream, bytesListTree)) {
            throw Exception("Value of bytes does not align with expected contents value.")
        }

        if (seqElement.valid != null && !checkValidKey(seqElement.valid, ioSubStream, bytesListTree)) {
            throw Exception("Value of bytes does not align with expected valid value.")
        }

        if (!type.sizeIsKnown) {  // only update value if it's still totally unknown. If we overwrite it always, then we would cut off seq that don't use the full datastream prematurely
            type.sizeInBits = kaitaiElement.value.size
            type.sizeIsKnown
        }

        offsetInDatastreamInBits += type.sizeInBits
        dataSizeOfSequenceInBits += type.sizeInBits
        return Triple(kaitaiElement, offsetInDatastreamInBits, dataSizeOfSequenceInBits)
    }

    fun processManySeqElements(
        elementId: String,
        seqElement: KTSeq,
        parentScopeStruct: KTStruct?,
        currentScopeStruct: KTStruct,
        bytesListTree: MutableKaitaiTree,
        ioStream: BooleanArray,
        _offsetInDatastreamInBits: Int,
        sourceOffsetInBits: Int,
        _dataSizeOfSequenceInBits: Int
    ) : Triple<KaitaiElement, Int, Int> {
        var offsetInDatastreamInBits = _offsetInDatastreamInBits
        var dataSizeOfSequenceInBits = _dataSizeOfSequenceInBits

        val bytesListTreeForInnerList = MutableKaitaiTree()
        bytesListTreeForInnerList.parent = bytesListTree
        bytesListTreeForInnerList.byteOrder = bytesListTreeForInnerList.parent!!.byteOrder
        var repeatAmount = 0
        while (true) {
            val triple = processSingleSeqElement(
                elementId,
                seqElement,
                parentScopeStruct,
                currentScopeStruct,
                bytesListTreeForInnerList,
                ioStream,
                offsetInDatastreamInBits,
                sourceOffsetInBits,
                dataSizeOfSequenceInBits
            )

            bytesListTreeForInnerList.add(triple.first)

            offsetInDatastreamInBits = triple.second
            dataSizeOfSequenceInBits = triple.third

            repeatAmount += 1
            if (seqElement.repeat == KTRepeat.EOS) {
                if (ioStream.size == dataSizeOfSequenceInBits) {
                    break
                }
            } else if (seqElement.repeat == KTRepeat.EXPR) {
                checkNotNull(seqElement.repeatExpr) { "With repeat type expr, a repeat-expr key is needed" }
                if (repeatAmount >= parseValue(seqElement.repeatExpr, bytesListTree).toByteArray().toInt(ByteOrder.BIG)) {
                    break
                }
            } else if (seqElement.repeat == KTRepeat.UNTIL) {
                checkNotNull(seqElement.repeatUntil) { "With repeat type until, a repeat-until key is needed" }
                throw Exception("repeat-until is not supported yet")
                // TODO: implement repeat until, needs expression parsing to work
            }
        }

        val resultSourceByteRange =
            Pair(bytesListTreeForInnerList.first().sourceByteRange!!.first, bytesListTreeForInnerList.last().sourceByteRange!!.second)
        val resultSourceRangeBitOffset =
            Pair(bytesListTreeForInnerList.first().sourceRangeBitOffset.first, bytesListTreeForInnerList.last().sourceRangeBitOffset.second)
        return Triple(
            KaitaiList(
                elementId, bytesListTree.byteOrder, bytesListTreeForInnerList, resultSourceByteRange, resultSourceRangeBitOffset,
                KaitaiDoc(undefined, undefined)
            ),
            offsetInDatastreamInBits,
            dataSizeOfSequenceInBits
        )
    }

    fun processOneOrManySeqElements(
        elementId: String,
        seqElement: KTSeq,
        parentScopeStruct: KTStruct?,
        currentScopeStruct: KTStruct,
        bytesListTree: MutableKaitaiTree,
        ioStream: BooleanArray,
        offsetInDatastreamInBits: Int,
        sourceOffsetInBits: Int,
        dataSizeOfSequenceInBits: Int
    ): Triple<KaitaiElement, Int, Int> {
        return if (seqElement.repeat != null) {
            processManySeqElements(
                elementId,
                seqElement,
                parentScopeStruct,
                currentScopeStruct,
                bytesListTree,
                ioStream,
                offsetInDatastreamInBits,
                sourceOffsetInBits,
                dataSizeOfSequenceInBits
            )
        } else {
            processSingleSeqElement(
                elementId,
                seqElement,
                parentScopeStruct,
                currentScopeStruct,
                bytesListTree,
                ioStream,
                offsetInDatastreamInBits,
                sourceOffsetInBits,
                dataSizeOfSequenceInBits
            )
        }
    }

    private fun processInstance(
        id: String,
        instance: KTSeq,
        parentScopeStruct: KTStruct?,
        currentScopeStruct: KTStruct,
        bytesListTree: MutableKaitaiTree,
        ioStream: BooleanArray,
        sourceOffsetInBits: Int,
        dataSizeOfSequenceInBits: Int,
    ): Triple<KaitaiElement, Int, Int> {
        throw Exception("Instances are not properly implemented yet. Please come back later.")
        // TODO what to do with value?
        val value = instance.value?.let {
            parseExpression(instance.value)
        }

        // TODO wait until I get iostreams from Justus
        val actualIoStream : BooleanArray = if (instance.io != null) {
            parseExpression(instance.io)
        } else {
            ioStream
        }

        val offsetInDatastreamInBits = 0

        return processOneOrManySeqElements(
            id,
            instance,
            parentScopeStruct,
            currentScopeStruct,
            bytesListTree,
            ioStream,
            offsetInDatastreamInBits,
            sourceOffsetInBits,
            dataSizeOfSequenceInBits
        )
    }

    fun processSeq(parentId: String, parentSeq: KTSeq?, parentBytesListTree: MutableKaitaiTree?, parentScopeStruct: KTStruct?, currentScopeStruct: KTStruct,
                   ioStream: BooleanArray, sourceOffsetInBits: Int, _offsetInDatastreamInBits: Int) : KaitaiElement {
        var offsetInDatastreamInBits: Int = _offsetInDatastreamInBits
        /*
        Entweder data als ByteArray und Bitshiften
        var test = 13  -> 1101
        test[6..8] = 1
        test and 0b00000111 >> 0 = -> 101
        test and 0b00111000 >> 3 = -> 001
        oder data als BooleanArray so wie aktuell. Vermutlich inperformant, aber wohl gut genug
        */
        val bytesListTree = MutableKaitaiTree()
        bytesListTree.parent = parentBytesListTree
        if (bytesListTree.parent != null) {
            bytesListTree.byteOrder = bytesListTree.parent!!.byteOrder
        } else {
            bytesListTree.byteOrder = ByteOrder.BIG
        }
        currentScopeStruct.meta?.endian?.let { endian ->
            bytesListTree.byteOrder = endian.toByteOrder()
        }

        var dataSizeOfSequenceInBits = 0
        if (currentScopeStruct.seq.isNotEmpty()) {
            for (seqElement in currentScopeStruct.seq) {
                val elementId = seqElement.id
                checkNotNull(seqElement.id) { "Sequence element id must not be null" }
                val triple = processOneOrManySeqElements(
                    elementId,
                    seqElement,
                    parentScopeStruct,
                    currentScopeStruct,
                    bytesListTree,
                    ioStream,
                    offsetInDatastreamInBits,
                    sourceOffsetInBits,
                    dataSizeOfSequenceInBits
                )

                bytesListTree.add(triple.first)
                offsetInDatastreamInBits = triple.second
                dataSizeOfSequenceInBits = triple.third
            }
        }
        val resultDoc = KaitaiDoc(parentSeq?.doc, parentSeq?.docRef)

        // TODO we should have way to see if we have a substream or not and highlight accordingly. currently only until end of data gets highlighted, not all of the actual substream
        val resultSourceByteRange: Pair<Int, Int>
        val resultSourceRangeBitOffset: Pair<Int, Int>
        if (currentScopeStruct.seq.isEmpty()) {
            resultSourceByteRange = Pair(0, 0) // is it possible to do the actual start and end value (which are the same) here instead of 0?
            resultSourceRangeBitOffset = Pair(0, 0)
        } else {
            resultSourceByteRange = Pair(bytesListTree.first().sourceByteRange!!.first, bytesListTree.last().sourceByteRange!!.second)
            resultSourceRangeBitOffset = Pair(bytesListTree.first().sourceRangeBitOffset.first, bytesListTree.last().sourceRangeBitOffset.second)
        }

        if (currentScopeStruct.instances.isNotEmpty()) {
            for ((id, instance) in currentScopeStruct.instances) {
                val triple = processInstance(id, instance, parentScopeStruct, currentScopeStruct,  bytesListTree, ioStream, sourceOffsetInBits, dataSizeOfSequenceInBits)

                bytesListTree.add(triple.first)
                dataSizeOfSequenceInBits = triple.third
            }
        }

        return KaitaiResult(parentId, bytesListTree.byteOrder, bytesListTree, resultSourceByteRange, resultSourceRangeBitOffset, resultDoc)
    }
}

interface KaitaiElement : ByteWitchResult {
    val id: String
    val bytesListTree: MutableKaitaiTree? get() = null
    val value: BooleanArray
    var endianness: ByteOrder
    var doc: KaitaiDoc
}

class KaitaiDoc(val docstring: String?, val docRef: List<String>?) {

    /*
    Learnings for 'doc' and 'doc-ref' keys

    'doc'
    - Can use YAML folded style strings for longer documentation that spans multiple lines.
        -> automatically converted with newlines by JsYaml parser
    - Can also be in markdown format
        -> converted to HTML by JetBrains Markdown library

    'doc-ref'
    - Can be a link to an external documentation page
        -> should be rendered as a link in the HTML output
    - After inspection of existing Kaitai structs, it can also be a list of strings apparently
        -> should be rendered as a list of links in the HTML output
     */

    private val urlRegex = Regex("""\bhttps?://[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]""")


    fun renderHTML(): String {
        if (docstring == null && docRef == null) {
            return ""
        }

        // doc
        val docstringHtml = docstring?.let { docstring ->
            val docstringMarkdown = docstring
            val flavour = CommonMarkFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(docstringMarkdown)
            HtmlGenerator(docstringMarkdown, parsedTree, flavour).generateHtml()
        } ?: ""

        // doc-ref
        val docRefList = docRef ?: emptyList()
        val docRefLinkified = docRefList.map { ref ->
            if (urlRegex.containsMatchIn(ref)) {
                // replace the URL with an HTML link
                ref.replace(urlRegex) { matchResult ->
                    "<a href=\"${matchResult.value}\" target=\"_blank\">${matchResult.value}</a>"
                }
            } else {
                ref // return the raw string as is
            }
        }
        val docRefHtml = when (docRefLinkified.size) {
            0 -> ""
            1 -> docRefLinkified[0]
            else -> docRefLinkified.joinToString(separator = "", prefix = "<ul>", postfix = "</ul>") { ref ->
                "<li>$ref</li>"
            }
        }

        if (docstringHtml.isEmpty() && docRefHtml.isEmpty()) {
            return ""
        }
        val renderDivider = docstringHtml.isNotEmpty() && docRefHtml.isNotEmpty()

        return  "<div class=\"tooltip-container\">" +
                    "<div class=\"tooltip-content roundbox\">" +
                        docstringHtml +
                        if (renderDivider) { "<hr/>" } else { "" } +
                        docRefHtml +
                    "</div>" +
                    "<div class=\"tooltip-arrow\"></div>" +
                "</div>"
    }
}

class KaitaiResult(override val id: String, override var endianness: ByteOrder,
                   override val bytesListTree: MutableKaitaiTree, override val sourceByteRange: Pair<Int, Int>,
                   override val sourceRangeBitOffset: Pair<Int, Int>, override var doc: KaitaiDoc): KaitaiElement {
    override fun renderHTML(): String {
        return  "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>" +
                    "${id}(${bytesListTree.joinToString("") { it.renderHTML() }})" +
                    doc.renderHTML() +
                "</div>"
    }

    // KaitaiResult does not really have a value itself, but if it's called we want to deliver a reasonable result
    override val value: BooleanArray
        get() {
            var result = booleanArrayOf()
            for (element in bytesListTree) {
                result += element.value
            }
            return result
        }
}

class KaitaiList(override val id: String, override var endianness: ByteOrder,
                 override val bytesListTree: MutableKaitaiTree, override val sourceByteRange: Pair<Int, Int>,
                 override val sourceRangeBitOffset: Pair<Int, Int>, override var doc: KaitaiDoc
): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>${bytesListTree.joinToString(", ", "${id}[", "]") { it.renderHTML() }}</div>"
    }

    // KaitaiList does not really have a value itself, but if it's called we want to deliver a reasonable result
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
                  override val sourceRangeBitOffset: Pair<Int, Int>, override var doc: KaitaiDoc): KaitaiElement {
    override fun renderHTML(): String {
        return  "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>" +
                    "${id}(${value.toByteArray().hex()})h" +
                    doc.renderHTML() +
                "</div>"
    }
}

// TODO: Currently the endianness is not used, because the value is already flipped in the Kaitai parser.
//  Therefor, do we even need it here? The conversion here can support both endianness, if the value is not flipped
abstract class KaitaiNumber(
    override val id: String, override var endianness: ByteOrder, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>,
    override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc
) : KaitaiElement {
    abstract val suffix: String

    override fun renderHTML(): String {
        return  "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>" +
                    "${id}(${parseValueAsString()})${suffix}" +
                    doc.renderHTML() +
                "</div>"
    }

    protected abstract fun parseValueAsString(): String
}

class KaitaiSignedInteger(
    override val id: String, override var endianness: ByteOrder, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>,
    override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc
) : KaitaiNumber(id, endianness, value, sourceByteRange, sourceRangeBitOffset, doc) {
    override val suffix: String = "s"

    override fun parseValueAsString(): String {
        val byteArray = value.toByteArray()
        return when (byteArray.size) {
            1 -> byteArray[0].toInt().toString()
            2 -> Short.fromBytes(byteArray, ByteOrder.BIG).toString()
            4 -> Int.fromBytes(byteArray, ByteOrder.BIG).toString()
            8 -> Long.fromBytes(byteArray, ByteOrder.BIG).toString()
            else -> throw IllegalArgumentException("Invalid byte array size for signed integer: ${byteArray.size}")
        }
    }
}

class KaitaiUnsignedInteger(
    override val id: String, override var endianness: ByteOrder, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>,
    override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc
) : KaitaiNumber(id, endianness, value, sourceByteRange, sourceRangeBitOffset, doc) {
    override val suffix: String = "u"

    override fun parseValueAsString(): String {
        val byteArray = value.toByteArray()
        return when (byteArray.size) {
            1 -> byteArray[0].toUByte().toString()
            2 -> Short.fromBytes(byteArray, ByteOrder.BIG).toUShort().toString()
            4 -> Int.fromBytes(byteArray, ByteOrder.BIG).toUInt().toString()
            8 -> Long.fromBytes(byteArray, ByteOrder.BIG).toULong().toString()
            else -> throw IllegalArgumentException("Invalid byte array size for unsigned integer: ${byteArray.size}")
        }
    }
}

class KaitaiFloat(
    override val id: String, override var endianness: ByteOrder, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>,
    override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc
) : KaitaiNumber(id, endianness, value, sourceByteRange, sourceRangeBitOffset, doc) {
    override val suffix: String = "f"

    override fun parseValueAsString(): String {
        val byteArray = value.toByteArray()
        return when (byteArray.size) {
            4 -> Float.fromBytes(byteArray, ByteOrder.BIG).toString()
            8 -> Double.fromBytes(byteArray, ByteOrder.BIG).toString()
            else -> throw IllegalArgumentException("Invalid byte array size for float or double: ${byteArray.size}")
        }
    }
}

class KaitaiBinary(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Int, Int>,
                   override val sourceRangeBitOffset: Pair<Int, Int>, override var doc: KaitaiDoc): KaitaiElement {
    override fun renderHTML(): String {
        return  "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>" +
                    "${id}(${value.joinToString("") { if (it) "1" else "0" }})b" +
                    doc.renderHTML() +
                "</div>"
    }
}

class KaitaiString(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Int, Int>,
                   override val sourceRangeBitOffset: Pair<Int, Int>, override var doc: KaitaiDoc): KaitaiElement {
    override fun renderHTML(): String {
        return  "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>" +
                    "${id}(${value.toByteArray().toUTF8String()})utf8" +
                    doc.renderHTML() +
                "</div>"
    }
}

class KaitaiEnum(
    override val id: String,
    override var endianness: ByteOrder,
    override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>,
    override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc,
    val enum: Pair<KTEnum?, String>,
) : KaitaiElement {
    override fun renderHTML(): String {
        return  "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>" +
                    "${id}(${enum.second})enum" +
                    doc.renderHTML() +
                "</div>"
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