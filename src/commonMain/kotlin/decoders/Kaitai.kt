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
class MutableKaitaiTree (private val innerList: MutableList<KaitaiElement> = mutableListOf(), val ioStream: BooleanArray) : MutableList<KaitaiElement> by innerList {
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

class Kaitai(kaitaiName: String, val kaitaiStruct: KTStruct) : ByteWitchDecoder {
    override val name = "Kaitai-$kaitaiName"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val result = try {  // JS Exceptions don't get simply logged to console but instead trigger the big red overlay. We convert JS Errors to Kotlin Exceptions here
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
                    return parseReference(
                        reference.substringAfter("."),
                        bytesListTree[reference.substringBefore(".")]!!.bytesListTree!!
                    )
                else
                    return bytesListTree[reference]!!.value
                )
    }

    // specifically for ExpressionParser
    enum class TokenType(val symbol: String) {
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
        ARRAY("[,]"),
        BYTEARRAY(""),
        INDEX("[]"),
        FUNCTION("f(x)"),
        DOT("."),
        DOUBLECOLON("::"),
        EMPTY(""),
        CAST("as<>"),
        ENUMCALL(""),
        ENUM(""),
        STREAM(""),
        KAITAITREE(""),
        KAITAIELEMENT(""),
    }

    inner class ExpressionParser(
        val bytesListTree: MutableKaitaiTree,
        val currentScopeStruct: KTStruct,
        val parentScopeStruct: KTStruct?,
        val ioStream: BooleanArray,
        val offsetInCurrentIoStream: Int,
        val repeatIndex: Int?,
        val currentRepeatElement: KaitaiElement?,
    ) {
    //******************************************************************************************************************
    //*                                                 methods                                                        *
    //******************************************************************************************************************
    fun expressionToString(input: Pair<TokenType, dynamic>, encoding: String? = null): Pair<TokenType, String> {
        when (input.first) {
            TokenType.INTEGER -> {
                return Pair(TokenType.STRING, input.second.toString())
            }

                TokenType.BYTEARRAY, TokenType.ARRAY -> {
                    if (encoding != "UTF-8" && encoding != "ASCII") {
                        throw RuntimeException("Encodings other than UTF-8 and ASCII are not supported.") // limited by kotlin js. the js part more specifically
                    }

                    return Pair(TokenType.STRING, (input.second as List<Pair<TokenType, Long>>).map { element -> element.second.toByte() }.toByteArray().toUTF8String())
                }

                else -> {
                    throw RuntimeException("Unexpected token type: " + input.first + "for method: to_s.")
                }
            }
        }

        fun expressionToInt(input: Pair<TokenType, dynamic>, radix: Int = 10): Pair<TokenType, Long> {
            when (input.first) {
                TokenType.FLOAT -> {
                    return Pair(TokenType.INTEGER, (input.second as Double).toLong())
                }

                TokenType.STRING -> {
                    val regex = Regex("0[bBoOxX]")
                    return if (regex.matches((input.second as String).substring(0, 2))) {
                        Pair(TokenType.INTEGER, (input.second as String).substring(2, input.second.length).toLong(radix))
                    } else {
                        Pair(TokenType.INTEGER, (input.second as String).toLong(radix))
                    }
                }

                TokenType.ENUM -> {
                    return Pair(TokenType.INTEGER, getEnumKeyFromValue((input.second as Pair<KTEnum, String>).first, (input.second as Pair<KTEnum, String>).second))
                }

                TokenType.BOOLEAN -> {
                    return Pair(TokenType.INTEGER, if (input.second as Boolean) 1L else 0L)
                }

                else -> {
                    throw RuntimeException("Unexpected token type: " + input.first + "for method: to_i.")
                }
            }
        }

        fun getEnumKeyFromValue(enum: KTEnum, value: String): Long {
            try {
                return enum.values.entries.find { it.value.id.toString() == value }!!.key
            } catch (e: Exception) {
                throw RuntimeException("Invalid enum value:$value")
            }

        }

        fun expressionLength(input: Pair<TokenType, dynamic>): Pair<TokenType, Long> {
            return when (input.first) {
                TokenType.BYTEARRAY, TokenType.ARRAY -> {
                    Pair(TokenType.INTEGER, (input.second as List<Pair<TokenType, Long>>).size.toLong())
                }

                TokenType.STRING -> {
                    Pair(TokenType.INTEGER, (input.second as String).length.toLong())
                }

                else -> {
                    throw RuntimeException("Unexpected token type: " + input.first + "for method: length.")
                }
            }
        }

        fun expressionReverse(input: Pair<TokenType, String>): Pair<TokenType, String> {
            return Pair(TokenType.STRING, input.second.reversed())
        }

        fun expressionSubstring(input: Pair<TokenType, dynamic>, fromIndex: Long, toIndex: Long): Pair<TokenType, String> {
            return Pair(TokenType.STRING, input.second.substring(fromIndex.toInt(), toIndex.toInt()))
        }

        fun expressionFirst(input: Pair<TokenType, List<Pair<TokenType, dynamic>>>): Pair<TokenType, dynamic> {
            if (input.second.isEmpty()) {
                throw RuntimeException("The array is empty and therefore has no first element.")
            } else {
                return input.second.first()
            }
        }

        fun expressionLast(input: Pair<TokenType, List<Pair<TokenType, dynamic>>>): Pair<TokenType, dynamic> {
            if (input.second.isEmpty()) {
                throw RuntimeException("The array is empty and therefore has no last element.")
            } else {
                return input.second.last()
            }
        }

        fun expressionSize(input: Pair<TokenType, dynamic>): Pair<TokenType, Long> {
            return if (input.first == TokenType.ARRAY) {
                Pair(TokenType.INTEGER, (input.second as List<Pair<TokenType, dynamic>>).size.toLong())
            } else if (input.first == TokenType.STREAM) {
                return Pair(TokenType.INTEGER, (input.second as BooleanArray).size / 8L)
            } else {
                throw RuntimeException("Unexpected token type: " + input.first + "for method: size.")
            }
        }

        fun expressionMax(input: Pair<TokenType, List<Pair<TokenType, dynamic>>>): Pair<TokenType, dynamic> {
            return if (input.second.isEmpty()) {
                throw RuntimeException("The array is empty and therefore has no max element.")
            } else {
                when (input.second[0].first) {
                    in listOf(TokenType.ARRAY, TokenType.BYTEARRAY) -> {
                        throw RuntimeException("Cannot determine the max element of an array containing arrays or bytearray.")
                    }

                    TokenType.KAITAIELEMENT -> {
                        Pair(
                            TokenType.KAITAIELEMENT,
                            input.second.maxBy { it -> Long.fromBytes((it.second as KaitaiElement).value.toByteArray(), ByteOrder.BIG) })
                    }

                    else -> {
                        input.second.maxBy { it -> it.second }
                    }
                }
            }
        }

        fun expressionMin(input: Pair<TokenType, List<Pair<TokenType, dynamic>>>): Pair<TokenType, dynamic> {
            return if (input.second.isEmpty()) {
                throw RuntimeException("The array is empty and therefore has no min element.")
            } else {
                when (input.second[0].first) {
                    in listOf(TokenType.ARRAY, TokenType.BYTEARRAY) -> {
                        throw RuntimeException("Cannot determine the min element of an array containing arrays or bytearray.")
                    }

                    TokenType.KAITAIELEMENT -> {
                        Pair(
                            TokenType.KAITAIELEMENT,
                            input.second.minBy { it -> Long.fromBytes((it.second as KaitaiElement).value.toByteArray(), ByteOrder.BIG) })
                    }

                    else -> {
                        input.second.minBy { it -> it.second }
                    }
                }
            }
        }

        fun expressionEof(input: Pair<TokenType, dynamic>, currentOffset: Int): Pair<TokenType, Boolean> {
            if (input.first == TokenType.STREAM) {
                return Pair(TokenType.BOOLEAN, currentOffset / 8 > (input.second as BooleanArray).size)
            } else {
                throw RuntimeException("Unexpected token type: " + input.first + "for method: eof.")
            }
        }

        fun expressionPos(input: Pair<TokenType, dynamic>, currentOffset: Int): Pair<TokenType, Long> {
            if (input.first == TokenType.STREAM) {
                return Pair(
                    TokenType.INTEGER,
                    if (currentOffset > (input.second as BooleanArray).size) {
                        throw RuntimeException("The current position ${currentOffset} is not in the stream.")
                    } else {
                        currentOffset / 8.toLong()
                    }
                )
            } else {
                throw RuntimeException("Unexpected token type: " + input.first + "for method: eof.")
            }
        }

        fun expressionCast(input: Pair<TokenType, dynamic>, castType: Pair<TokenType, String>): Pair<TokenType, dynamic> {
            val castAsInt: List<String> = listOf("u1", "u2", "u4", "u8", "s1", "s2", "s4", "s8")
            val castAsFloat: List<String> = listOf("f1", "f2", "f4", "f8")  // TODO f1 and f2 do not exist, while f8 currently doesn't work
            val castAsString: List<String> = listOf("str", "strz")
            when (input.first) {
                TokenType.INTEGER -> {
                    if (castType.second in castAsInt ||
                        (castType.second[0] == 'b' &&
                                castType.second.substring(1).all { it.isDigit() } &&
                                castType.second != "b1")
                    ) {
                        return input
                    } else if (castType.second == "b1" || castType.second == "bool") {
                        return Pair(TokenType.BOOLEAN, input.second == 1) // TODO idk
                    } else if (castType.second in castAsFloat) {
                        return Pair(TokenType.FLOAT, input.second.toFloat())
                    } else if (castType.second in castAsString) {
                        return Pair(TokenType.STRING, input.second.toString())
                    } else if (castType.second == "bytes") {
                        return Pair(TokenType.BYTEARRAY, input.second.toBytes(ByteOrder.BIG))
                    } else { // this case only happens if anyone tries to cast a primitive type to a custom type which should not happen. In case it happens anyway I just return the
                        return input
                    }
                }

                TokenType.FLOAT -> {
                    if (castType.second in castAsInt ||
                        (castType.second[0] == 'b' &&
                                castType.second.substring(1).all { it.isDigit() } &&
                                castType.second != "b1")
                    ) {
                        return Pair(TokenType.INTEGER, input.second.toLong())
                    } else if (castType.second == "b1" || castType.second == "bool") {
                        return Pair(TokenType.BOOLEAN, input.second == 1) // TODO idk
                    } else if (castType.second in castAsFloat) {
                        return input
                    } else if (castType.second in castAsString) {
                        return Pair(TokenType.STRING, input.second.toString())
                    } else if (castType.second == "bytes") {
                        return Pair(TokenType.BYTEARRAY, input.second.toLong().toBytes(ByteOrder.BIG))
                    } else { // this case only happens if anyone tries to cast a primitive type to a custom type which should not happen. In case it happens anyway I just return the
                        return input
                    }
                }

                TokenType.STRING -> {
                    if (castType.second in castAsInt ||
                        (castType.second[0] == 'b' &&
                                castType.second.substring(1).all { it.isDigit() } &&
                                castType.second != "b1")
                    ) {
                        return Pair(TokenType.INTEGER, input.second.toLong())
                    } else if (castType.second == "b1" || castType.second == "bool") {
                        return Pair(TokenType.BOOLEAN, input.second.toBoolean()) // maybe toBooleanStrict()
                    } else if (castType.second in castAsFloat) {
                        return Pair(TokenType.FLOAT, input.second.toFloat())
                    } else if (castType.second in castAsString) {
                        return input
                    } else if (castType.second == "bytes") {
                        return Pair(TokenType.BYTEARRAY, input.second.encodeToByteArray())
                    } else { // this case only happens if anyone tries to cast a primitive type to a custom type which should not happen. In case it happens anyway I just return the
                        return input
                    }
                }

                TokenType.BOOLEAN -> {
                    if (castType.second in castAsInt ||
                        (castType.second[0] == 'b' &&
                                castType.second.substring(1).all { it.isDigit() } &&
                                castType.second != "b1")
                    ) {
                        return Pair(TokenType.INTEGER, if (input.second) 1L else 0L)
                    } else if (castType.second == "b1" || castType.second == "bool") {
                        return input
                    } else if (castType.second in castAsFloat) {
                        return Pair(TokenType.FLOAT, if (input.second) 1F else 0F)
                    } else if (castType.second in castAsString) {
                        return Pair(TokenType.STRING, input.second.toString())
                    } else if (castType.second == "bytes") {
                        return Pair(TokenType.BYTEARRAY, byteArrayOf(if (input.second) 0x01 else 0x00))
                    } else { // this case only happens if anyone tries to cast a primitive type to a custom type which should not happen. In case it happens anyway I just return the
                        return input
                    }
                }

                TokenType.BYTEARRAY, TokenType.ARRAY -> {
                    if (castType.second in castAsInt ||
                        (castType.second[0] == 'b' &&
                                castType.second.substring(1).all { it.isDigit() } &&
                                castType.second != "b1")
                    ) {
                        return Pair(TokenType.INTEGER, Long.fromBytes(input.second, ByteOrder.BIG))
                    } else if (castType.second == "b1" || castType.second == "bool") {
                        return expressionCast(
                            Pair(TokenType.INTEGER, Long.fromBytes(input.second, ByteOrder.BIG)),
                            Pair(TokenType.CAST, "bool")
                        )
                    } else if (castType.second in castAsFloat) {
                        return Pair(TokenType.FLOAT, Float.fromBytes(input.second, ByteOrder.BIG))
                    } else if (castType.second in castAsString) {
                        return Pair(TokenType.STRING, input.second.decodeToString())
                    } else if (castType.second == "bytes") {
                        return input
                    } else { // this case only happens if anyone tries to cast a primitive type to a custom type which should not happen. In case it happens anyway I just return the
                        return input
                    }
                }

                TokenType.ARRAY -> {
                    if (castType.second == "bytes" && input.second[0].first == TokenType.INTEGER) {
                        return Pair(
                            TokenType.BYTEARRAY,
                            (input.second as MutableList<Pair<TokenType, Long>>).map { it.second.toByte() }.toByteArray()
                        )
                    } else {
                        throw RuntimeException("Cannon cast an array of type ${input.second[0].first} to a byte array. Only an array of type ${TokenType.INTEGER} is allowed.")
                    }
                }

                TokenType.KAITAIELEMENT -> {
                    return expressionCast( // TODO this is only a temporary solution
                        Pair(TokenType.BYTEARRAY, input.second.toByteArray()),
                        castType
                    )
                }

                else -> { // other source types and cast types are not supported. For other source types append here.
                    return input
                }
            }
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
            TokenType.ENUMCALL to ::parseEnum,
            TokenType.ARRAY to ::parseArray,
            TokenType.IDENTIFIER to ::parseIdentifier
        )

        fun parseInteger(token: Pair<TokenType, Long>): Pair<TokenType, Long> = token

        fun parseFloat(token: Pair<TokenType, Double>): Pair<TokenType, Double> = token

        fun parseString(token: Pair<TokenType, String>): Pair<TokenType, String> = token

        fun parseBoolean(token: Pair<TokenType, Boolean>): Pair<TokenType, Boolean> = token

        fun parseParentheses(
            token: Pair<TokenType, dynamic>
        ): Pair<TokenType, dynamic> = parseExpressionInner(
            token.second
        )

        fun parseEnum(token: Pair<TokenType, String>): Pair<TokenType, Pair<KTEnum, String>> {
            val path: String = token.second.substringBeforeLast("::")
            val enum: KTEnum = getEnum(currentScopeStruct, path) ?: getEnum(parentScopeStruct, path) ?: getEnum(kaitaiStruct, path)
            ?: throw RuntimeException("The enum $path does not exist.")

            return Pair(TokenType.ENUM, Pair(enum, token.second.substringAfterLast("::")))
        }

    fun tokenizeArray(fullArray: String): List<String> {
        var array: String = fullArray
        val result: MutableList<String> = mutableListOf()

            while (array.isNotEmpty()) {
                var slice: Int = array.length

                var sqString: Boolean = false
                var dqString: Boolean = false
                // add logic here to support more complex arrays: nested arrays, entries with complex expression containing more commas
                for ((index, char) in array.withIndex()) {
                    if (char == ',' && !sqString && !dqString) {
                        slice = index
                        break
                    } else if (char == '\'' && !dqString) {
                        sqString = !sqString
                    } else if (char == '"' && !sqString) {
                        dqString = !dqString
                    }
                }

                result.add(array.slice(IntRange(0, slice - 1)).trim())

                if (slice == array.length) {
                    if (result.last().trim() == "") {
                        result.removeLast()
                    }
                    break
                } else {
                    array = array.slice(IntRange(slice + 1, array.length))
                }
            }
            return result.toList()
        }

        fun parseArray(token: Pair<TokenType, String>): Pair<TokenType, List<Pair<TokenType, dynamic>>> {

            var expressions: List<String> = tokenizeArray(token.second)
            var array: MutableList<Pair<TokenType, dynamic>> = mutableListOf()
            for (expression: String in expressions) {
                array.add(
                    parseExpressionInner(
                        expression
                    )
                )
            }

            return Pair(TokenType.ARRAY, array.toList())
        }

        fun parseReferenceHelper(targetElement: KaitaiElement): Pair<TokenType, dynamic> {
            return when (targetElement) {
                is KaitaiResult -> Pair(TokenType.KAITAITREE, targetElement.bytesListTree)
                is KaitaiBinary -> Pair(TokenType.INTEGER, Long.fromBytes(targetElement.value.toByteArray(), ByteOrder.BIG))
                is KaitaiString -> Pair(TokenType.STRING, targetElement.value.toByteArray().toUTF8String())
                is KaitaiSignedInteger -> {
                    val byteArray = targetElement.value.toByteArray()
                    val result = when (byteArray.size) {
                        1 -> byteArray[0].toLong()
                        2 -> Short.fromBytes(byteArray, ByteOrder.BIG).toLong()
                        4 -> Int.fromBytes(byteArray, ByteOrder.BIG).toLong()
                        8 -> Long.fromBytes(byteArray, ByteOrder.BIG)
                        else -> throw IllegalArgumentException("Invalid byte array size for signed integer: ${byteArray.size}")
                    }
                    Pair(TokenType.INTEGER, result)
                }
                is KaitaiUnsignedInteger -> Pair(TokenType.INTEGER, Long.fromBytes(targetElement.value.toByteArray(), ByteOrder.BIG))
                is KaitaiFloat -> {
                    val byteArray = targetElement.value.toByteArray()
                    val result = when (byteArray.size) {
                        4 -> Float.fromBytes(byteArray, ByteOrder.BIG).toDouble()
                        8 -> Double.fromBytes(byteArray, ByteOrder.BIG)
                        else -> throw IllegalArgumentException("Invalid byte array size for float: ${byteArray.size}")
                    }
                    Pair(TokenType.FLOAT, result)
                }
                is KaitaiEnum -> Pair(TokenType.ENUM, targetElement.enum)
                is KaitaiBytes -> {
                    val byteArray = targetElement.value.toByteArray()
                    val array: MutableList<Pair<TokenType, Long>> = mutableListOf()
                    for (byte in byteArray) {
                        array.add(Pair(TokenType.INTEGER, byte.toLong()))
                    }
                    Pair(TokenType.BYTEARRAY, array.toList())
                }
                is KaitaiList -> {
                    val kaitaiTree: MutableKaitaiTree = targetElement.bytesListTree
                    val array: MutableList<Pair<TokenType, dynamic>> = mutableListOf()
                    for (element in kaitaiTree) {
                        array.add(parseReferenceHelper(element))
                    }
                    Pair(TokenType.ARRAY, array.toList())
                }
                else -> { // TODO instances with values
                    throw RuntimeException("Unexpected KaitaiElement type ${targetElement::class}")
                }
            }
        }

        fun parseIdentifier(token: Pair<TokenType, String>): Pair<TokenType, dynamic> {
            return when (token.second) {
                "_" -> {
                    val targetElement = currentRepeatElement
                    if (targetElement != null) {
                        parseReferenceHelper(targetElement)
                    } else {
                        throw RuntimeException("The identifier _ cannot be used outside of repeat.")
                    }
                }

                "_io" -> {
                    Pair(TokenType.STREAM, ioStream)
                }

                "_index" -> {
                    if (repeatIndex == null) {
                        throw RuntimeException("Cannot use \"_index\" in non repeated element.")
                    } else {
                        Pair(TokenType.INTEGER, repeatIndex.toLong())
                    }
                }

                "_parent" -> {
                    Pair(TokenType.KAITAITREE, bytesListTree.parent)
                }

                "_root" -> {
                    Pair(TokenType.KAITAITREE, bytesListTree.root)
                }

                else -> {
                    val targetElement = bytesListTree[token.second]
                    parseReferenceHelper(targetElement)
                }
            }
        }

        //******************************************************************************************************************
        //*                                     indexing, references and functions                                         *
        //******************************************************************************************************************

        fun parseIndex(tokens: MutableList<Pair<TokenType, dynamic>>, index: Pair<TokenType, String>): Pair<TokenType, dynamic> {
            val array: Pair<TokenType, List<Pair<TokenType, dynamic>>> = parseTokens(tokens)
            val index: Pair<TokenType, Long> = parseExpressionInner(index.second)

            return array.second[index.second.toInt()]
        }

        fun parseDot(leftTokens: MutableList<Pair<TokenType, dynamic>>, rightToken: Pair<TokenType, dynamic>): Pair<TokenType, dynamic> {
            val op1: Pair<TokenType, dynamic> = parseTokens(leftTokens)
            val op2: Pair<TokenType, dynamic> = rightToken

            if (op1.first == TokenType.STREAM) {
                when (op2.second) {
                    "eof" -> {
                        return expressionEof(op1, offsetInCurrentIoStream)
                    }

                    "size" -> {
                        return expressionSize(op1)
                    }

                    "pos" -> {
                        return expressionPos(op1, offsetInCurrentIoStream)
                    }
                }
            } else if (op1.first == TokenType.INTEGER) {
                if (op2.second == "to_s") {
                    return expressionToString(op1)
                } else if (op2.first == TokenType.CAST) {
                    return expressionCast(op1, op2)
                }
            } else if (op1.first == TokenType.FLOAT) {
                if (op2.second == "to_i") {
                    return expressionToInt(op1)
                } else if (op2.first == TokenType.CAST) {
                    return expressionCast(op1, op2)
                }
            } else if (op1.first == TokenType.BYTEARRAY) {
                if (op2.second == "length") {
                    return expressionLength(op1)
                } else if (op2.first == TokenType.FUNCTION && (op2.second as Pair<Pair<TokenType, String>, Pair<TokenType, String>>).first.second == "to_s") { // in this case op2 is a function token containing a pair of an identifier token which contains the function name and a parenthesis token with the function params. Therefore op2.second.first is the identifier token
                    return expressionToString(
                        op1,
                        (op2.second as Pair<Pair<TokenType, String>, Pair<TokenType, String>>).second.second
                    ) // and op2.second.second is the parenthesis token. .second on top of that in both cases just returns the values of these tokens. See the last for loop in the function tokenizeExpression for more information.
                } else if (op2.first == TokenType.CAST) {
                    return expressionCast(op1, op2)
                }
            } else if (op1.first == TokenType.STRING) {
                if (op2.second == "length") {
                    return expressionLength(op1)
                } else if (op2.second == "reverse") {
                    return expressionReverse(op1)
                } else if (op2.second == "to_i") {
                    return expressionToInt(op1)
                } else if (op2.first == TokenType.FUNCTION && (op2.second as Pair<Pair<TokenType, String>, Pair<TokenType, dynamic>>).first.second == "to_i") {
                    val radix: Long = parseExpression((op2.second as Pair<Pair<TokenType, String>, Pair<TokenType, String>>).second.second)
                    return expressionToInt(op1, radix.toInt())
                } else if (op2.first == TokenType.FUNCTION && (op2.second as Pair<Pair<TokenType, String>, Pair<TokenType, dynamic>>).first.second == "substring") {
                    val range =
                        parseArray( // the params from and to for substring can also be any expression. since at this point the content of the parentheses is just a string and the two params are seperated via a comma
                            Pair(TokenType.ARRAY, (op2.second as Pair<Pair<TokenType, dynamic>, Pair<TokenType, dynamic>>).second.second) // I just use the parseArray function to give me an integer array of length 2 which I later index accordingly.
                        )
                    return expressionSubstring(op1, range.second[0].second, range.second[1].second)
                } else if (op2.first == TokenType.CAST) {
                    return expressionCast(op1, op2)
                }
            } else if (op1.first == TokenType.ENUM) {
                if (op2.second == "to_i") {
                    return expressionToInt(op1)
                }
            } else if (op1.first == TokenType.BOOLEAN) {
                if (op2.second == "to_i") {
                    return expressionToInt(op1)
                } else if (op2.first == TokenType.CAST) {
                    return expressionCast(op1, op2)
                }
            } else if (op1.first == TokenType.ARRAY) {
                when (op2.second) {
                    "first" -> {
                        return expressionFirst(op1)
                    }

                    "last" -> {
                        return expressionLast(op1)
                    }

                    "size" -> {
                        return expressionSize(op1)
                    }

                    "min" -> {
                        return expressionMin(op1)
                    }

                    "max" -> {
                        return expressionMax(op1)
                    }
                }
                if ((op1.second as List<Pair<TokenType, dynamic>>)[0].first == TokenType.INTEGER && (op1.second as List<Pair<TokenType, Long>>).all { it.second in 0L..255L }) {
                    // the array might have been intended as a bytearray but was parsed as an array of integers. Therefore, the functions length and to_s are allowed.
                    if (op2.second == "length") {
                        return expressionLength(op1)
                    } else if (op2.first == TokenType.FUNCTION && (op2.second as Pair<Pair<TokenType, String>, Pair<TokenType, String>>).first.second == "to_s") { // in this case op2 is a function token containing a pair of an identifier token which contains the function name and a parenthesis token with the function params. Therefore op2.second.first is the identifier token
                        return expressionToString(
                            op1,
                            (op2.second as Pair<Pair<TokenType, String>, Pair<TokenType, String>>).second.second
                        ) // and op2.second.second is the parenthesis token. .second on top of that in both cases just returns the values of these tokens. See the last for loop in the function tokenizeExpression for more information.
                    } else if (op2.first == TokenType.CAST) {
                        return expressionCast(op1, op2)
                    }
                }
            } else if (op1.first == TokenType.KAITAITREE) {
                return when (op2.second) {
                    "_parent" -> {
                        Pair(TokenType.KAITAITREE, (op1.second as MutableKaitaiTree).parent)
                    }

                    "_root" -> {
                        Pair(TokenType.KAITAITREE, (op1.second as MutableKaitaiTree).root)
                    }

                    "_io" -> {
                        Pair(TokenType.STREAM, (op1.second as MutableKaitaiTree).ioStream)
                    }

                    else -> {
                        val targetElement: KaitaiElement = (op1.second as MutableKaitaiTree)[op2.second as String]
                        parseReferenceHelper(targetElement)
                    }
                }
            } else if (op1.first == TokenType.KAITAIELEMENT) {
                return if (op2.second == "_parent") {
                    Pair(TokenType.KAITAITREE, op1.second.bytesListTree.parent)
                } else if (op2.second == "_root") {
                    Pair(TokenType.KAITAITREE, op1.second.bytesListTree.root)
                } else if (op2.second == "_io") {
                    Pair(TokenType.STREAM, (op1.second as KaitaiElement).ioStream)
                } else if (op2.first == TokenType.CAST) {
                    expressionCast(
                        op1,
                        op2
                    ) // the source type KAITAIELEMENT is not supported in expressionCast. The function just returns the token op1
                } else {
                    val targetElement: KaitaiElement = (op1.second as KaitaiResult).bytesListTree[op2.second as String]
                    parseReferenceHelper(targetElement)
                }
            }

            throw RuntimeException("The token dot could not be passed because of either the wrong token on the left $op1 or on the right $op2") // return Pair(TokenType.EMPTY, "")
        }

        //******************************************************************************************************************
        //*                                              unary operators                                                   *
        //******************************************************************************************************************

        val unaryPrecedence = mapOf( // unary tokens
            TokenType.PLUS to ::parseUnaryPlus,
            TokenType.MINUS to ::parseUnaryMinus,
            TokenType.BOOLEANNOT to ::parseBooleanNot
        )

        fun parseUnaryPlus(
            tokens: MutableList<Pair<TokenType, dynamic>>
        ): Pair<TokenType, Number> {
            return parseTokens(
                tokens
            )
        }

        fun parseUnaryMinus(tokens: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Number> {
            val op: Pair<TokenType, Number> = parseTokens(tokens)

            return if (op.first == TokenType.INTEGER)
                Pair(
                    TokenType.INTEGER,
                    -(op.second).toLong()
                )
            else
                Pair(
                    TokenType.FLOAT,
                    -(op.second).toDouble()
                )
        }

        fun parseBooleanNot(tokens: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val op: Pair<TokenType, dynamic> = parseTokens(tokens)
            return if (op.first == TokenType.BOOLEAN)
                Pair(
                    TokenType.BOOLEAN,
                    !op.second
                )
            else
                throw Exception("Cannot apply logical or to non boolean")
        }

        //******************************************************************************************************************
        //*                                          binary operators                                                      *
        //******************************************************************************************************************

        val binaryPrecedence = listOf(
            // binary tokens
            mapOf(TokenType.MUL to ::parseMul, TokenType.DIV to ::parseDiv, TokenType.MODULO to ::parseModulo),
            mapOf(TokenType.PLUS to ::parsePlus, TokenType.MINUS to ::parseMinus),
            mapOf(TokenType.LSHIFT to ::parseLShift, TokenType.RSHIFT to ::parseRShift),
            mapOf(
                TokenType.LESS to ::parseLess,
                TokenType.LESSEQUAL to ::parseLessEqual,
                TokenType.GREATER to ::parseGreater,
                TokenType.GREATEREQUAL to ::parseGreaterEqual
            ),
            mapOf(TokenType.EQUAL to ::parseEqual, TokenType.NOTEQUAL to ::parseNotEqual),
            mapOf(TokenType.BITWISEAND to ::parseBitwiseAnd),
            mapOf(TokenType.BITWISEXOR to ::parseBitwiseXor),
            mapOf(TokenType.BITWISEOR to ::parseBitwiseOr),
            mapOf(TokenType.BOOLEANAND to ::parseBooleanAnd),
            mapOf(TokenType.BOOLEANOR to ::parseBooleanOr),
        )

        fun parseMul(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Number> {
            val op1: Pair<TokenType, Number> = parseTokens(tokens1)
            val op2: Pair<TokenType, Number> = parseTokens(tokens2)

        val result: Pair<TokenType, Number> = if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT) {
            val left: Double = if (op1.first == TokenType.INTEGER) (op1.second as Long).toDouble() else op1.second as Double
            val right: Double = if (op2.first == TokenType.INTEGER) (op2.second as Long).toDouble() else op2.second as Double
            Pair(
                TokenType.FLOAT,
                left * right
            )
        }
        else
            Pair(
                TokenType.INTEGER,
                op1.second as Long * op2.second as Long
            )

            return result
        }

        fun parseDiv(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Number> {
            val op1: Pair<TokenType, Number> = parseTokens(tokens1)
            val op2: Pair<TokenType, Number> = parseTokens(tokens2)

        val result: Pair<TokenType, Number> = if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT) {
            val left: Double = if (op1.first == TokenType.INTEGER) (op1.second as Long).toDouble() else op1.second as Double
            val right: Double = if (op2.first == TokenType.INTEGER) (op2.second as Long).toDouble() else op2.second as Double
            Pair(
                TokenType.FLOAT,
                left / right
            )
        }
        else
            Pair(
                TokenType.INTEGER,
                op1.second as Long / op2.second as Long
            )

            return result
        }

        fun parseModulo(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Number> {
            val op1: Pair<TokenType, Number> = parseTokens(tokens1)
            val op2: Pair<TokenType, Number> = parseTokens(tokens2)

        val result: Pair<TokenType, Number> = if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT) {
            val result: Double = (op1.second).toDouble() % (op2.second).toDouble()
            Pair(
                TokenType.FLOAT,
                if (result < 0) result + (op2.second).toDouble() else result
            )
        }
        else {
            val result: Long = (op1.second).toLong() % (op2.second).toLong()
            Pair(
                TokenType.INTEGER,
                if (result < 0) result + (op2.second).toLong() else result
            )
        }

            return result
        }

        fun parsePlus(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, dynamic> {
            val op1: Pair<TokenType, dynamic> = parseTokens(tokens1)
            val op2: Pair<TokenType, dynamic> = parseTokens(tokens2)

        val result: Pair<TokenType, dynamic> = if (op1.first == TokenType.STRING && op2.first == TokenType.STRING)
            Pair(
                TokenType.STRING,
                (op1.second as String) + (op2.second as String)
            )
        else if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT) {
            val left: Double = if (op1.first == TokenType.INTEGER) (op1.second as Long).toDouble() else op1.second as Double
            val right: Double = if (op2.first == TokenType.INTEGER) (op2.second as Long).toDouble() else op2.second as Double
            Pair(
                TokenType.FLOAT,
                left + right
            )
        }
        else
            Pair(
                TokenType.INTEGER,
                op1.second as Long + op2.second as Long
            )

            return result
        }

        fun parseMinus(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Number> {
            val op1: Pair<TokenType, Number> = parseTokens(tokens1)
            val op2: Pair<TokenType, Number> = parseTokens(tokens2)

        val result: Pair<TokenType, Number> = if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT) {
            val left: Double = if (op1.first == TokenType.INTEGER) (op1.second as Long).toDouble() else op1.second as Double
            val right: Double = if (op2.first == TokenType.INTEGER) (op2.second as Long).toDouble() else op2.second as Double
            Pair(
                TokenType.FLOAT,
                left - right
            )
        }
        else
            Pair(
                TokenType.INTEGER,
                op1.second as Long - op2.second as Long
            )

            return result
        }

        fun parseLShift(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Long> {
            val op1: Pair<TokenType, Long> = parseTokens(tokens1)
            val op2: Pair<TokenType, Long> = parseTokens(tokens2)

            val result: Pair<TokenType, Long> = Pair(
                TokenType.INTEGER,
                op1.second shl (op2.second).toInt()
            )

            return result
        }

        fun parseRShift(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Long> {
            val op1: Pair<TokenType, Long> = parseTokens(tokens1)
            val op2: Pair<TokenType, Long> = parseTokens(tokens2)

            val result: Pair<TokenType, Long> = Pair(
                TokenType.INTEGER,
                op1.second shr (op2.second).toInt()
            )

            return result
        }

        fun parseLess(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val op1: Pair<TokenType, dynamic> = parseTokens(tokens1)
            val op2: Pair<TokenType, dynamic> = parseTokens(tokens2)

            val result: Pair<TokenType, Boolean> = Pair(
                TokenType.BOOLEAN,
                op1.second < op2.second
            )

            return result
        }

        fun parseLessEqual(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val op1: Pair<TokenType, dynamic> = parseTokens(tokens1)
            val op2: Pair<TokenType, dynamic> = parseTokens(tokens2)

            val result: Pair<TokenType, Boolean> = Pair(
                TokenType.BOOLEAN,
                op1.second <= op2.second
            )

            return result
        }

        fun parseGreater(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val op1: Pair<TokenType, dynamic> = parseTokens(tokens1)
            val op2: Pair<TokenType, dynamic> = parseTokens(tokens2)

            val result: Pair<TokenType, Boolean> = Pair(
                TokenType.BOOLEAN,
                op1.second > op2.second
            )

            return result
        }

        fun parseGreaterEqual(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val op1: Pair<TokenType, dynamic> = parseTokens(tokens1)
            val op2: Pair<TokenType, dynamic> = parseTokens(tokens2)

            val result: Pair<TokenType, Boolean> = Pair(
                TokenType.BOOLEAN,
                op1.second >= op2.second
            )

            return result
        }

        fun parseEqual(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val op1: Pair<TokenType, dynamic> = parseTokens(tokens1)
            val op2: Pair<TokenType, dynamic> = parseTokens(tokens2)

            if ((op1.first == TokenType.BYTEARRAY || op1.first == TokenType.ARRAY) && (op2.first == TokenType.BYTEARRAY || op2.first == TokenType.ARRAY)) {
                if ((op1.second as List<Pair<TokenType, dynamic>>).size != (op2.second as List<Pair<TokenType, dynamic>>).size) {
                    return Pair(TokenType.BOOLEAN, false)
                } else {
                    for (i in 0..op1.second.size) {
                        val elementResult: Pair<TokenType, Boolean> = parseEqual(mutableListOf(op1.second[i]), mutableListOf(op2.second[i]))
                        if (!elementResult.second) {
                            return elementResult
                        }
                    }
                    return Pair(TokenType.BOOLEAN, true)
                }
            } else if (op1.first == TokenType.ENUM && op2.first == TokenType.ENUM) {
                val result: Pair<TokenType, Boolean> = Pair(
                    TokenType.BOOLEAN,
                    // This condition is not necessary according to the kaitai user guide, but can be used to enforce a stricter enum comparison
                    // (op1.second as Pair<KTEnum, String>).first === (op2.second as Pair<KTEnum, String>).first &&
                    (op1.second as Pair<KTEnum, String>).second == (op2.second as Pair<KTEnum, String>).second
                )

                return result
            } else if (op1.first == TokenType.STRING && op2.first == TokenType.STRING){
                val result: Pair<TokenType, Boolean> = Pair(
                    TokenType.BOOLEAN,
                    op1.second as String == op2.second as String
                )

                return result
            } else if (op1.first == TokenType.INTEGER && op2.first == TokenType.INTEGER){
                val result: Pair<TokenType, Boolean> = Pair(
                    TokenType.BOOLEAN,
                    (op1.second as Long).toString() == (op2.second as Long).toString()
                ) // The cast to String is necessary as Kotlin JS just forgets that the longs are Kotlin longs because they are dynamics for a short time
                // even though the Kotlin class is still Long in both cases.
                // unsafeCast<Long>() also does not work.

                return result
            } else if (op1.first == TokenType.FLOAT && op2.first == TokenType.FLOAT){
                val result: Pair<TokenType, Boolean> = Pair(
                    TokenType.BOOLEAN,
                    op1.second as Double == op2.second as Double
                )

                return result
            } else if (op1.first == TokenType.BOOLEAN && op2.first == TokenType.BOOLEAN){
                val result: Pair<TokenType, Boolean> = Pair(
                    TokenType.BOOLEAN,
                    op1.second as Boolean == op2.second as Boolean
                )

                return result
            }else {
                // this approach does not work as kotlin does not support comparing different values
                /*val left = when (op1.first) {
                    TokenType.INTEGER -> op1.second as Long
                    TokenType.FLOAT -> op1.second as Double
                    TokenType.STRING -> op1.second as String
                    TokenType.BOOLEAN -> op1.second as Boolean
                    else -> throw RuntimeException("Cannot compare ${op1.first}")
                }
                val right = when (op2.first) {
                    TokenType.INTEGER -> op2.second as Long
                    TokenType.FLOAT -> op2.second as Double
                    TokenType.STRING -> op2.second as String
                    TokenType.BOOLEAN -> op2.second as Boolean
                    else -> throw RuntimeException("Cannot compare ${op2.first}")
                }

                return Pair(TokenType.BOOLEAN, left == right)*/

                throw RuntimeException("Cannot compare ${op1.first} wih ${op2.first}")
                // return Pair(TokenType.BOOLEAN, false)  would also be possible
            }

        }

        fun parseNotEqual(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val result = parseEqual(tokens1, tokens2)
            return Pair(result.first, !result.second)
        }

        fun parseBitwiseAnd(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Long> {
            val op1: Pair<TokenType, Long> = parseTokens(tokens1)
            val op2: Pair<TokenType, Long> = parseTokens(tokens2)

            val result: Pair<TokenType, Long> = Pair(
                TokenType.INTEGER,
                op1.second and op2.second
            )

            return result
        }

        fun parseBitwiseXor(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Long> {
            val op1: Pair<TokenType, Long> = parseTokens(tokens1)
            val op2: Pair<TokenType, Long> = parseTokens(tokens2)

            val result: Pair<TokenType, Long> = Pair(
                TokenType.INTEGER,
                op1.second xor op2.second
            )

            return result
        }

        fun parseBitwiseOr(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Long> {
            val op1: Pair<TokenType, Long> = parseTokens(tokens1)
            val op2: Pair<TokenType, Long> = parseTokens(tokens2)

            val result: Pair<TokenType, Long> = Pair(
                TokenType.INTEGER,
                op1.second or op2.second
            )

            return result
        }

        fun parseBooleanAnd(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val op1: Pair<TokenType, dynamic> = parseTokens(tokens1)

            if (!op1.second) {
                return Pair(TokenType.BOOLEAN, false)
            }

            val op2: Pair<TokenType, dynamic> = parseTokens(tokens2)

            val result: Pair<TokenType, Boolean> = Pair(
                TokenType.BOOLEAN,
                (op1.second as Boolean) && (op2.second as Boolean)
            )

            return result
        }

        fun parseBooleanOr(tokens1: MutableList<Pair<TokenType, dynamic>>, tokens2: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, Boolean> {
            val op1: Pair<TokenType, dynamic> = parseTokens(tokens1)

            if (op1.second) {
                return Pair(TokenType.BOOLEAN, true)
            }

            val op2: Pair<TokenType, dynamic> = parseTokens(tokens2)

            val result: Pair<TokenType, Boolean> = Pair(
                TokenType.BOOLEAN,
                (op1.second as Boolean) || (op2.second as Boolean)
            )

            return result
        }

        //******************************************************************************************************************
        //*                                         ternary operators                                                      *
        //******************************************************************************************************************

        fun parseIfElse(
            condition: MutableList<Pair<TokenType, dynamic>>,
            tokensTrue: MutableList<Pair<TokenType, dynamic>>,
            tokensFalse: MutableList<Pair<TokenType, dynamic>>
        ): Pair<TokenType, dynamic> {
            val evaluation = parseTokens(condition)
            return if (evaluation.second as Boolean) {
                parseTokens(tokensTrue)
            } else {
                parseTokens(tokensFalse)
            }
        }

    val validTokensForBinaryPlusAndMinus: List<TokenType> = listOf(TokenType.INTEGER, TokenType.FLOAT, TokenType.STRING, TokenType.IDENTIFIER, TokenType.BOOLEAN, TokenType.PARENTHESES, TokenType.ARRAY, TokenType.INDEX, TokenType.FUNCTION, TokenType.CAST, TokenType.ENUMCALL)
        // TokenType.BYTEARRAY, TokenType.ENUM, TokenType.STREAM, TokenType.KAITAITREE, TokenType.KAITAIELEMENT
        // These tokens are not in the list as they are only returned by parse functions and never created while tokenizing an expression. If they are for some reason added to expressions naturally later on, they also have to be added to the list for parseTokens to work properly.

        fun parseTokens(tokens: MutableList<Pair<TokenType, dynamic>>): Pair<TokenType, dynamic> {
            if (tokens.size == 1 && tokens[0].first in operandTokens) { // while they have the highest precedence of any token they are the only option in case there is only one token left and can therefore be the first check
                val function = operandTokens.getValue(tokens[0].first)
                return function(tokens[0])
            }

            if (tokens.contains(Pair(TokenType.QUESTIONMARK, "?"))) { // ternary if else has the lowest precedence
                var depth: Int = 0
                var posQuestionmark: Int = 0
                var posColon: Int
                for ((index, token) in tokens.withIndex()) {
                    if (token == Pair(TokenType.QUESTIONMARK, "?")) {
                        if (depth == 0) posQuestionmark = index
                        depth++
                    } else if (token == Pair(TokenType.COLON, ":")) {
                        posColon = index
                        depth--
                        if (depth == 0)
                            return parseIfElse(
                                tokens.subList(0, posQuestionmark),
                                tokens.subList(posQuestionmark + 1, posColon),
                                tokens.subList(posColon + 1, tokens.size)
                            )
                    }
                }
            }

            for (operatorMap in binaryPrecedence.reversed()) { // the lower precedence operators are checked first
                for ((index, token) in tokens.reversed().withIndex()) {
                    if (index != tokens.size - 1 && tokens.reversed()[index + 1].first in validTokensForBinaryPlusAndMinus && token.first in operatorMap) {
                        val op1: MutableList<Pair<TokenType, dynamic>> = tokens.subList(0, tokens.size - index - 1)
                        val op2: MutableList<Pair<TokenType, dynamic>> = tokens.subList(tokens.size - index, tokens.size)
                        val function = operatorMap.getValue(token.first)
                        return function.invoke(op1, op2)
                    }
                }
            }

            for ((index, token) in tokens.withIndex()) { // unary operands all have the same precedence
                if (token.first in unaryPrecedence) {
                    val op: MutableList<Pair<TokenType, dynamic>> = tokens.subList(index + 1, tokens.size)
                    val function = unaryPrecedence.getValue(token.first)
                    return function(op)
                }
            }

            // Array indexing and dot basically have the same precedence or more specifically are mutually exclusive anyway.
            // The token before an index token cannot be a dot token. In Kaitai dot can be used for references, functions and casts,
            // but this is handled by parseDot.
            if (tokens.last().first == TokenType.INDEX) {
                return parseIndex(
                    tokens.subList(0, tokens.size - 1),
                    tokens.last()
                )
            } else if (tokens[tokens.size - 2].first == TokenType.DOT) {
                return parseDot(
                    tokens.subList(0, tokens.size - 2),
                    tokens.last()
                )
            }
            return Pair(TokenType.EMPTY, null)
        }

        val operators = setOf("+", "-", "*", "/", "%", "<", ">", "&", "|", "^", "?", ":", ".")
        val operators2 = setOf("<=", ">=", "==", "!=", "<<", ">>", "::")

        val operatorMap: Map<String, TokenType> = TokenType.entries.associateBy { it.symbol }
        val operator2Map: Map<String, TokenType> = TokenType.entries.associateBy { it.symbol }

        fun getOperator(char: Char): TokenType = operatorMap[char.toString()]!!
        fun getOperator2(string: String): TokenType = operator2Map[string]!!

        fun isKeyword(char: Char?): Boolean {
            return (char == null
                    || (!char.isLetterOrDigit()
                    && char != '_'))
        }

        fun nextToken(expression: String): Pair<Pair<TokenType, dynamic>, Int> {
            val trimmedExpression = expression.trimStart()
            if (trimmedExpression.isEmpty()) return Pair(Pair(TokenType.EMPTY, null), 0)
            val trimmed = expression.length - trimmedExpression.length

            if (trimmedExpression.length >= 5 && trimmedExpression.startsWith("false")) // token FALSE
                if (isKeyword(trimmedExpression.getOrNull(5)))
                    return Pair(Pair(TokenType.BOOLEAN, false), trimmed + 5)
            if (trimmedExpression.length >= 4 && trimmedExpression.startsWith("true")) // token TRUE
                if (isKeyword(trimmedExpression.getOrNull(4)))
                    return Pair(Pair(TokenType.BOOLEAN, true), trimmed + 4)
            if (trimmedExpression.length >= 3 && trimmedExpression.startsWith("not")) // token NOT
                if (isKeyword(trimmedExpression.getOrNull(3)))
                    return Pair(Pair(TokenType.BOOLEANNOT, "not"), trimmed + 3)
            if (trimmedExpression.length >= 3 && trimmedExpression.startsWith("and")) // token AND
                if (isKeyword(trimmedExpression.getOrNull(3)))
                    return Pair(Pair(TokenType.BOOLEANAND, "and"), trimmed + 3)
            if (trimmedExpression.length >= 2 && trimmedExpression.startsWith("or")) // token OR
                if (isKeyword(trimmedExpression.getOrNull(2)))
                    return Pair(Pair(TokenType.BOOLEANOR, "or"), trimmed + 2)
            if (trimmedExpression.length >= 2 && trimmedExpression.substring(0..1) in operators2) // tokens consisting of two symbols: "<=", ">=", "==", "!=", "<<", ">>", "::"
                return Pair(
                    Pair(getOperator2(trimmedExpression.substring(0..1)), trimmedExpression.substring(0..1)),
                    trimmed + 2
                )
            if (trimmedExpression[0].toString() in operators) // one symbol tokens: "+", "-", "*", "/", "%", "<", ">", "&", "|", "^", "?", ":", "."
                return Pair(Pair(getOperator(trimmedExpression[0]), trimmedExpression[0].toString()), trimmed + 1)

            var breakIndex: Int = 0

            if (trimmedExpression.startsWith("as<")) { // token CAST
                for ((index, char) in trimmedExpression.substring(2).withIndex()) {
                    if (char == '>') {
                        breakIndex = index + 1
                        break
                    }
                }
                return Pair(Pair(TokenType.CAST, trimmedExpression.substring(3..breakIndex).trim()), trimmed + breakIndex + 2)
            }

            when (trimmedExpression[0]) {
                '(' -> { // token PARENTHESES
                    var depth: Int = 0
                    var sqstring: Boolean = false
                    var dqstring: Boolean = false
                    for ((index, char) in trimmedExpression.withIndex()) {
                        when (char.toString()) {
                            "'" -> if (!dqstring) sqstring = !sqstring
                            "\"" -> if (!sqstring && (!dqstring || trimmedExpression[index - 1] != '\\')) dqstring =
                                !dqstring

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
                    return Pair(
                        Pair(TokenType.PARENTHESES, trimmedExpression.substring(1..breakIndex - 2)),
                        trimmed + breakIndex
                    )
                }

            '[' -> { // token ARRAY
                var depth: Int = 0
                var sqstring: Boolean = false
                var dqstring: Boolean = false
                for ((index, char) in trimmedExpression.withIndex()) {
                    when (char.toString()) {
                        "'" -> if (!dqstring) sqstring = !sqstring
                        "\"" -> if (!sqstring && (!dqstring || trimmedExpression[index - 1] != '\\')) dqstring =
                            !dqstring

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
                return Pair(
                    Pair(TokenType.ARRAY, trimmedExpression.substring(1..breakIndex - 2)),
                    trimmed + breakIndex
                )
            }

                '\'' -> { // token STRING with single quotation marks
                    for ((index, char) in trimmedExpression.withIndex()) {
                        if (char == '\'' && index != 0) {
                            breakIndex = index + 1
                            break
                        }
                    }
                    return Pair(
                        Pair(TokenType.STRING, trimmedExpression.substring(1..breakIndex - 2)),
                        trimmed + breakIndex
                    )
                }

                '"' -> { // token STRING with double quotation marks
                    for ((index, char) in trimmedExpression.withIndex()) {
                        if (char == '"' && index != 0) {
                            breakIndex = index + 1
                            break
                        }
                    }
                    return Pair(
                        Pair(TokenType.STRING, trimmedExpression.substring(1..breakIndex - 2)),
                        trimmed + breakIndex
                    )
                }
            }

            if (trimmedExpression[0].isLowerCase() || trimmedExpression[0] == '_') { // token IDENTIFIER
                for ((index, char) in trimmedExpression.withIndex()) {
                    if (!char.isLetterOrDigit() && char != '_') {
                        breakIndex = index
                        break
                    }
                    breakIndex = index + 1
                }
                return Pair(
                    Pair(TokenType.IDENTIFIER, trimmedExpression.substring(0..breakIndex - 1)),
                    trimmed + breakIndex
                )
            } else if (trimmedExpression[0].isDigit()) { // tokens INTEGER and FLOAT
                val exceptions = setOf<Char>('x', 'X', 'o', 'O', 'b', 'B')
                if (trimmedExpression[0] == '0' && (trimmedExpression.getOrNull(1) == 'x' || trimmedExpression.getOrNull(1) == 'X')) { // token INTEGER in hexadecimal notation
                    val hexadecimalRegex: Regex = Regex("^[a-fA-F0-9_]$")
                    for ((index, char) in trimmedExpression.withIndex()) {
                        if (!hexadecimalRegex.matches(char.toString()) && !(index == 1 && char in exceptions)) {
                            breakIndex = index
                            break
                        }
                        breakIndex = index + 1
                    }
                    return Pair(
                        Pair(
                            TokenType.INTEGER,
                            trimmedExpression.substring(2..breakIndex - 1).replace("_", "").toLong(16)
                        ), trimmed + breakIndex
                    )
                } else if (trimmedExpression[0] == '0' && (trimmedExpression.getOrNull(1) == 'b' || trimmedExpression.getOrNull(
                        1
                    ) == 'B')
                ) { // token INTEGER in binary notation
                    for ((index, char) in trimmedExpression.withIndex()) {
                        if (char != '0' && char != '1' && char != '_' && !(index == 1 && char in exceptions)) {
                            breakIndex = index
                            break
                        }
                        breakIndex = index + 1
                    }
                    return Pair(
                        Pair(
                            TokenType.INTEGER,
                            trimmedExpression.substring(2..breakIndex - 1).replace("_", "").toLong(2)
                        ), trimmed + breakIndex
                    )
                } else if (trimmedExpression[0] == '0' && (trimmedExpression.getOrNull(1) == 'o' || trimmedExpression.getOrNull(
                        1
                    ) == 'O')
                ) { // token INTEGER in octal notation
                    val octalRegex: Regex = Regex("^[0-7_]$")
                    for ((index, char) in trimmedExpression.withIndex()) {
                        if (!octalRegex.matches(char.toString()) && !(index == 1 && char in exceptions)) {
                            breakIndex = index
                            break
                        }
                        breakIndex = index + 1
                    }
                    return Pair(
                        Pair(
                            TokenType.INTEGER,
                            trimmedExpression.substring(2..breakIndex - 1).replace("_", "").toLong(8)
                        ), trimmed + breakIndex
                    )
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
                        if ((char == '+' || char == '-') && (trimmedExpression[index - 1] == 'e' || trimmedExpression[index - 1] == 'E'))
                            continue
                        if (!char.isDigit() && char != '_') {
                            breakIndex = index
                            break
                        }
                        breakIndex = index + 1
                    }
                    if (float_point)
                        return Pair(
                            Pair(
                                TokenType.FLOAT,
                                trimmedExpression.substring(0..breakIndex - 1).replace("_", "").toDouble()
                            ), trimmed + breakIndex
                        )
                    else
                        return Pair(
                            Pair(
                                TokenType.INTEGER,
                                trimmedExpression.substring(0..breakIndex - 1).replace("_", "").toLong()
                            ), trimmed + breakIndex
                        )

                }
            }
            return Pair(Pair(TokenType.EMPTY, null), 0)
        }

    fun tokenizeExpression(expression: String): MutableList<Pair<TokenType, dynamic>> {
        var expression: String = expression
        var tokens: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()
        while (expression != "") {
            var token: Pair<Pair<TokenType, dynamic>, Int> = nextToken(expression)
            tokens.add(token.first)
            expression = expression.substring(token.second)
            expression = expression.trimStart()
        }

            var tokensWithEnums: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()
            var enum: Boolean = false

        for ((index: Int, token: Pair<TokenType, dynamic>) in tokens.withIndex()) {
            if (!enum) {
                if (token.first == TokenType.IDENTIFIER && tokens.getOrNull(index + 1) != null && tokens[index + 1].first == TokenType.DOUBLECOLON) {
                    tokensWithEnums.add(Pair(TokenType.ENUMCALL, token.second))
                    enum = true
                    continue
                }
                tokensWithEnums.add(token)
            } else {
                if (token.first != TokenType.IDENTIFIER && token.first != TokenType.DOUBLECOLON) {
                    enum = false
                    tokensWithEnums.add(token)
                } else {
                    val tempEnum: String = tokensWithEnums.last().second
                    tokensWithEnums.removeLast()
                    tokensWithEnums.add(Pair(TokenType.ENUMCALL, tempEnum + token.second))
                }
            }
        }

            var tokensWithIndex: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()

        for ((index, token: Pair<TokenType, dynamic>) in tokensWithEnums.withIndex()) {
            if (token.first == TokenType.ARRAY) {
                if (index > 0 &&
                    (tokensWithEnums[index - 1].first == TokenType.IDENTIFIER ||
                            tokensWithEnums[index - 1].first == TokenType.ARRAY ||
                            tokensWithEnums[index - 1].first == TokenType.INDEX)
                ) {
                    tokensWithIndex.add(Pair(TokenType.INDEX, token.second))
                    continue
                }
            }
            tokensWithIndex.add(token)
        }


            var tokensWithFunctions: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()
            var function: Boolean = false

        for ((index, token: Pair<TokenType, dynamic>) in tokensWithIndex.withIndex()) { // combining TokenType IDENTIFIER with value: to_s, substring or to_s followed by TokenType PARENTHESES to TokenType FUNCTION
            if (token.first == TokenType.IDENTIFIER &&
                (token.second == "to_s" ||
                        token.second == "substring" ||
                        token.second == "to_i") &&
                index + 1 < tokensWithIndex.size &&
                tokensWithIndex[index + 1].first == TokenType.PARENTHESES
            ) {
                function = true
            } else {
                if (function) {
                    val functionToken = Pair(tokensWithIndex[index - 1], token)
                    tokensWithFunctions.add(
                        Pair(
                            TokenType.FUNCTION,
                            functionToken
                        )
                    )
                    function = false
                } else {
                    tokensWithFunctions.add(token)
                }
            }
        }
        return tokensWithFunctions
    }

        fun parseExpressionInner(expression: String): Pair<TokenType, dynamic> {
            return parseTokens(tokenizeExpression(expression))
        }

        fun decapsulateArrayTokens(array: MutableList<Pair<TokenType, dynamic>>): MutableList<dynamic> {
            val result: MutableList<dynamic> = mutableListOf()
            for (token: Pair<TokenType, dynamic> in array) {
                if (token.first == TokenType.ARRAY) {
                    result.add(decapsulateArrayTokens(token.second))
                } else {
                    result.add(token.second)
                }
            }
            return result
        }

        fun parseExpression(expression: String): dynamic {
            val result: Pair<TokenType, dynamic> = parseExpressionInner(expression)
            return if (result.first == TokenType.ARRAY) {
                decapsulateArrayTokens(result.second)
            } else {
                result.second
            }
        }
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

    // TODO: add support for imports
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

    fun parseType(parentScopeStruct: KTStruct?, currentScopeStruct: KTStruct, seqElement: KTSeq, bytesListTree: MutableKaitaiTree, expressionParser: ExpressionParser) : Type {
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
                getCustomType(kaitaiStruct, type.type)
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
                val parsedValue = expressionParser.parseExpression(seqElement.size.toString())
                type.sizeInBits = parsedValue * 8
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
        repeatIndex: Int?,
    ): Triple<KaitaiElement, Int, Int> {
        var offsetInDatastreamInBits = _offsetInDatastreamInBits
        var dataSizeOfSequenceInBits = _dataSizeOfSequenceInBits

        val expressionParser = ExpressionParser(bytesListTree, currentScopeStruct, parentScopeStruct, ioStream, offsetInDatastreamInBits, repeatIndex, null)

        if (seqElement.pos != null) {
            offsetInDatastreamInBits = if (seqElement.pos is StringOrInt.IntValue) {
                seqElement.pos.value * 8
            } else {
                Int.fromBytes(parseValue(seqElement.pos.toString(), bytesListTree).toByteArray(), ByteOrder.BIG) * 8
            }
        }

        val type = parseType(parentScopeStruct, currentScopeStruct, seqElement, bytesListTree, expressionParser)
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
                DisplayStyle.BINARY -> KaitaiBinary(elementId, type.byteOrder, ioStream, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.STRING -> KaitaiString(elementId, type.byteOrder, ioStream, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.SIGNED_INTEGER -> KaitaiSignedInteger(elementId, type.byteOrder, ioStream, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.UNSIGNED_INTEGER -> KaitaiUnsignedInteger(elementId, type.byteOrder, ioStream, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.FLOAT -> KaitaiFloat(elementId, type.byteOrder, ioStream, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
                DisplayStyle.ENUM -> KaitaiEnum(elementId, type.byteOrder, ioStream, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc, enum)
                // displayStyle.HEX as the fallback (even if it's a known type or whatever)
                else -> KaitaiBytes(elementId, type.byteOrder, ioStream, ioSubStream, sourceByteRange, sourceRangeBitOffset, elementDoc)
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

        val bytesListTreeForInnerList = MutableKaitaiTree(ioStream = ioStream)
        bytesListTreeForInnerList.parent = bytesListTree.parent
        if (bytesListTree.parent != null) {
            bytesListTreeForInnerList.byteOrder = bytesListTreeForInnerList.parent!!.byteOrder
        } else {
            bytesListTreeForInnerList.byteOrder = ByteOrder.BIG
        }
        var repeatIndex = 0
        while (true) {
            val triple = processSingleSeqElement(
                elementId,
                seqElement,
                parentScopeStruct,
                currentScopeStruct,
                bytesListTree,
                ioStream,
                offsetInDatastreamInBits,
                sourceOffsetInBits,
                dataSizeOfSequenceInBits,
                repeatIndex,
            )

            bytesListTreeForInnerList.add(triple.first)

            offsetInDatastreamInBits = triple.second
            dataSizeOfSequenceInBits = triple.third

            repeatIndex += 1
            if (seqElement.repeat == KTRepeat.EOS) {
                if (ioStream.size == dataSizeOfSequenceInBits) {
                    break
                }
            } else if (seqElement.repeat == KTRepeat.EXPR) {
                checkNotNull(seqElement.repeatExpr) { "With repeat type expr, a repeat-expr key is needed" }
                if (repeatIndex >= parseValue(seqElement.repeatExpr, bytesListTree).toByteArray().toInt(ByteOrder.BIG)) {
                    break
                }
            } else if (seqElement.repeat == KTRepeat.UNTIL) {
                checkNotNull(seqElement.repeatUntil) { "With repeat type until, a repeat-until key is needed" }
                val expressionParser = ExpressionParser(bytesListTree, currentScopeStruct, parentScopeStruct, ioStream, offsetInDatastreamInBits, repeatIndex, triple.first)
                if (expressionParser.parseExpression(seqElement.repeatUntil)) {
                    break
                }
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
    ): Triple<KaitaiElement?, Int, Int> {
        val expressionParser = ExpressionParser(bytesListTree, currentScopeStruct, parentScopeStruct, ioStream, offsetInDatastreamInBits, null, null)
        if (!expressionParser.parseExpression(seqElement.ifCondition.toString())) {
            return Triple(null, offsetInDatastreamInBits, dataSizeOfSequenceInBits)
        }

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
                dataSizeOfSequenceInBits,
                null,
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
    ): Triple<KaitaiElement?, Int, Int> {
        throw Exception("Instances are not properly implemented yet. Please come back later.")
        // TODO what to do with value?
        /*val value = instance.value?.let {
            //parseExpression(instance.value)
        }

        // TODO wait until I get iostreams from Justus
        val actualIoStream : BooleanArray = if (instance.io != null) {
            parseExpression(instance.io)
        } else {
            ioStream
        }*/

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
            dataSizeOfSequenceInBits,
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
        val bytesListTree = MutableKaitaiTree(ioStream = ioStream)
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

                if (triple.first != null) {
                    bytesListTree.add(triple.first!!)
                }

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
                val triple = processInstance(id, instance, parentScopeStruct, currentScopeStruct, bytesListTree, ioStream, sourceOffsetInBits, dataSizeOfSequenceInBits)

                if (triple.first != null) {
                    bytesListTree.add(triple.first!!)
                }

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
    val ioStream: BooleanArray
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

class KaitaiResult(
    override val id: String, override var endianness: ByteOrder,
    override val bytesListTree: MutableKaitaiTree,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc,
) : KaitaiElement {
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

    override val ioStream: BooleanArray = bytesListTree.ioStream
}

class KaitaiList(
    override val id: String, override var endianness: ByteOrder,
    override val bytesListTree: MutableKaitaiTree,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc,
) : KaitaiElement {
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

    override val ioStream: BooleanArray = bytesListTree.ioStream
}

class KaitaiBytes(
    override val id: String, override var endianness: ByteOrder,
    override val ioStream: BooleanArray, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc,
) : KaitaiElement {
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
    override val id: String, override var endianness: ByteOrder,
    override val ioStream: BooleanArray, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
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
    override val id: String, override var endianness: ByteOrder,
    override val ioStream: BooleanArray, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc
) : KaitaiNumber(id, endianness, ioStream, value, sourceByteRange, sourceRangeBitOffset, doc) {
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
    override val id: String, override var endianness: ByteOrder,
    override val ioStream: BooleanArray, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc
) : KaitaiNumber(id, endianness, ioStream, value, sourceByteRange, sourceRangeBitOffset, doc) {
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
    override val id: String, override var endianness: ByteOrder,
    override val ioStream: BooleanArray, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc
) : KaitaiNumber(id, endianness, ioStream, value, sourceByteRange, sourceRangeBitOffset, doc) {
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

class KaitaiBinary(
    override val id: String, override var endianness: ByteOrder,
    override val ioStream: BooleanArray, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc,
) : KaitaiElement {
    override fun renderHTML(): String {
        return  "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>" +
                    "${id}(${value.joinToString("") { if (it) "1" else "0" }})b" +
                    doc.renderHTML() +
                "</div>"
    }
}

class KaitaiString(
    override val id: String, override var endianness: ByteOrder,
    override val ioStream: BooleanArray, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
    override var doc: KaitaiDoc
) : KaitaiElement {
    override fun renderHTML(): String {
        return  "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>" +
                    "${id}(${value.toByteArray().toUTF8String()})utf8" +
                    doc.renderHTML() +
                "</div>"
    }
}

class KaitaiEnum(
    override val id: String, override var endianness: ByteOrder,
    override val ioStream: BooleanArray, override val value: BooleanArray,
    override val sourceByteRange: Pair<Int, Int>, override val sourceRangeBitOffset: Pair<Int, Int>,
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
