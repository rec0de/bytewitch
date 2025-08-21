package decoders

import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.hex
import bitmage.indexOfFirstSubsequence
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
import kaitai.KTEnumValue
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

enum class DisplayStyle {
    HEX, BINARY, SIGNED_INTEGER, UNSIGNED_INTEGER, FLOAT, STRING, ENUM
}

/*
kaitaiElement.bytesListTree.["repetition"][2].bytesListTree["first"].bytesListTree[.....]   <- Notation with mutable MapTree

kaitaiElement["repetition"][2]["subelement"][1]

kaitaiElement.bytesListTree.findAll { it.id == "repetition" }[2]   <- notation with mutableListTree
kaitaiElement.bytesListTree.find { it.id == "header" }    /     kaitaiElement.bytesListTree.findAll { it.id == "header" }[0]

element.repetition[2].subelement[1]   <-- notation in kaiatai
*/

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
    var sizeInBits: UInt = 0u
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
        BRACKETS("[]"),
        ARRAY("[,]"),
        INDEX("[]"),
        FUNCTION("f(x)"),
        DOT("."),
        DOUBLECOLON("::"),
        EMPTY(""),
        CAST("as<>"),
        REFERENCE(""),
        ENUMCALL(""),
        ENUM(""),
        STRUCT(""),
        STREAM(""),
        KAITAITREE(""),
        KAITAIELEMENT(""),
    }

    //******************************************************************************************************************
    //*                                                 methods                                                        *
    //******************************************************************************************************************

    val methods = listOf(
        "to_s",
        "to_i",
        "length",
        "reverse",
        "substring",
        "first",
        "last",
        "size",
        "min",
        "max",
        "eof",
        "size",
        "pos",
    )

    val encodings = listOf(
        "ASCII",
        "UTF-8",
        "UTF-16BE",
        "UTF-16LE",
        "UTF-32BE",
        "UTF-32LE",
        "ISO-8859-1",
        "ISO-8859-2",
        "ISO-8859-3",
        "ISO-8859-4",
        "ISO-8859-5",
        "ISO-8859-6",
        "ISO-8859-7",
        "ISO-8859-8",
        "ISO-8859-9",
        "ISO-8859-10",
        "ISO-8859-11",
        "ISO-8859-13",
        "ISO-8859-14",
        "ISO-8859-15",
        "ISO-8859-16",
        "windows-1250",
        "windows-1251",
        "windows-1252",
        "windows-1253",
        "windows-1254",
        "windows-1255",
        "windows-1256",
        "windows-1257",
        "windows-1258",
        "IBM437",
        "IBM866",
        "Shift_JIS",
        "Big5",
        "EUC-KR"
    )

    fun expressionToString(
        input: Pair<TokenType, dynamic>,
        encoding: String? = null,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, String> {
        when (input.first) {
            TokenType.INTEGER -> {
                return Pair(TokenType.STRING, input.second.toString())
            }

            TokenType.ARRAY -> {
                if (encoding != "UTF-16BE") {
                    throw RuntimeException("Encodings other than UTF-16BE are not supported.")
                }
                return Pair(TokenType.STRING, input.second.contentToString())
            }

            else -> {
                throw RuntimeException("Unexpected token type:" + input.first + "for method: to_s.")
            }
        }
    }

    fun expressionToInt(input: Pair<TokenType, dynamic>, radix: Int = 10): Pair<TokenType, Int> {
        when (input.first) {
            TokenType.FLOAT -> {
                return Pair(TokenType.INTEGER, input.second.toInt())
            }

            TokenType.STRING -> {
                return Pair(TokenType.INTEGER, input.second.toInt(radix))
            }

            TokenType.ENUM -> {
                return Pair(TokenType.INTEGER, getEnumKeyFromValue(input.second.first, input.second.second))
            }

            TokenType.BOOLEAN -> {
                return Pair(TokenType.INTEGER, if (input.second) 1 else 0)
            }

            else -> {
                throw RuntimeException("Unexpected token type:" + input.first + "for method: to_i.")
            }
        }
    }

    fun getEnumKeyFromValue(enum: KTEnum, value: String): Int {
        try {
            return enum.values.entries.find { it.value.id.toString() == value }!!.key
        } catch (e: Exception) {
            throw RuntimeException("Invalid enum value:$value")
        }

    }

    fun expressionLength(input: Pair<TokenType, dynamic>): Pair<TokenType, Int> {
        when (input.first) {
            TokenType.ARRAY -> {
                return Pair(TokenType.INTEGER, input.second.size)
            }

            TokenType.STRING -> {
                return Pair(TokenType.INTEGER, input.second.length)
            }

            else -> {
                throw RuntimeException("Unexpected token type:" + input.first + "for method: length.")
            }
        }
    }

    fun expressionStringReverse(input: Pair<TokenType, dynamic>): Pair<TokenType, String> {
        if (input.first == TokenType.STRING) {
            return Pair(TokenType.STRING, input.second.reversed())
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: reverse.")
        }
    }

    fun expressionSubstring(input: Pair<TokenType, dynamic>, fromIndex: Int, toIndex: Int): Pair<TokenType, String> {
        if (input.first == TokenType.STRING) {
            return Pair(TokenType.STRING, input.second.substring(fromIndex, toIndex))
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: substring.")
        }
    }

    val validArrayTypes: List<TokenType> =
        listOf(TokenType.INTEGER, TokenType.FLOAT, TokenType.BOOLEAN, TokenType.STRING, TokenType.STRUCT)

    fun expressionFirst(input: Pair<TokenType, dynamic>): Pair<TokenType, dynamic> {
        if (input.first == TokenType.ARRAY) {
            if (input.second.isEmpty()) {
                throw RuntimeException("The array is empty and therefore has no first element.")
            } else {
                if (input.second[0].first in validArrayTypes) {
                    return Pair(input.second[0].first, input.second.first())
                } else {
                    throw RuntimeException("Unexpected array type:" + input.second + "for method: first.")
                }
            }
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: first.")
        }
    }

    fun expressionLast(input: Pair<TokenType, dynamic>): Pair<TokenType, dynamic> {
        if (input.first == TokenType.ARRAY) {
            if (input.second.isEmpty()) {
                throw RuntimeException("The array is empty and therefore has no first element.")
            } else {
                if (input.second[0].first in validArrayTypes) {
                    return Pair(input.second[0].first, input.second.first())
                } else {
                    throw RuntimeException("Unexpected array type:" + input.second + "for method: last.")
                }
            }
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: last.")
        }
    }

    fun expressionSize(input: Pair<TokenType, dynamic>): Pair<TokenType, Int> {
        return if (input.first == TokenType.ARRAY) {
            val validTypes =
                listOf(TokenType.INTEGER, TokenType.FLOAT, TokenType.BOOLEAN, TokenType.STRING, TokenType.STRUCT)
            if (input.second.isEmpty()) {
                Pair(TokenType.INTEGER, 0)
            } else {
                if (input.second[0].first in validTypes) {
                    Pair(TokenType.INTEGER, input.second.size)
                } else {
                    throw RuntimeException("Unexpected array type:" + input.first + "for method: size.")
                }
            }
        } else if (input.first == TokenType.STREAM) {
            return Pair(TokenType.INTEGER, input.second.size / 8)
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: size.")
        }
    }

    fun expressionMax(input: Pair<TokenType, dynamic>): Pair<TokenType, dynamic> {
        if (input.first == TokenType.ARRAY) {
            return if (input.second.isEmpty()) {
                throw RuntimeException("The array is empty and therefore has no max element.")
            } else {
                if (input.second[0].first in validArrayTypes) {
                    when (input.second[0].first) {
                        TokenType.BOOLEAN -> {
                            Pair(TokenType.BOOLEAN, input.second.contains(true))
                        }

                        TokenType.STRUCT -> {
                            Pair(TokenType.STRUCT, input.second.maxBy { it -> it.value.size })
                        }

                        else -> {
                            Pair(input.second[0].first, input.second.maxOrNull())
                        }
                    }
                } else {
                    throw RuntimeException("Unexpected array type:" + input.second + "for method: max.")
                }
            }
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: max.")
        }
    }

    fun expressionMin(input: Pair<TokenType, dynamic>): Pair<TokenType, dynamic> {
        if (input.first == TokenType.ARRAY) {
            return if (input.second.isEmpty()) {
                throw RuntimeException("The array is empty and therefore has no min element.")
            } else {
                if (input.second[0].first in validArrayTypes) {
                    when (input.second[0].first) {
                        TokenType.BOOLEAN -> {
                            Pair(TokenType.BOOLEAN, !(input.second.contains(false)))
                        }

                        TokenType.STRUCT -> {
                            Pair(TokenType.STRUCT, input.second.minBy { it -> it.value.size })
                        }

                        else -> {
                            Pair(input.second[0].first, input.second.minOrNull())
                        }
                    }
                } else {
                    throw RuntimeException("Unexpected array type:" + input.second + "for method: min.")
                }
            }
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: min.")
        }
    }

    fun expressionEof(input: Pair<TokenType, dynamic>, currentOffset: Int): Pair<TokenType, Boolean> {

        if (input.first == TokenType.STREAM) {
            return Pair(TokenType.BOOLEAN, currentOffset > (input.second as BooleanArray).size) // TODO
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: eof.")
        }
    }

    fun expressionPos(input: Pair<TokenType, dynamic>, currentOffset: Int): Pair<TokenType, Int> {
        if (input.first == TokenType.STREAM) {
            return Pair(
                TokenType.INTEGER,
                if (currentOffset > (input.second as BooleanArray).size) throw RuntimeException("The current position ${currentOffset} is not in the stream.") else currentOffset
            ) // TODO
        } else {
            throw RuntimeException("Unexpected token type:" + input.first + "for method: eof.")
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

    fun parseInteger(
        token: Pair<TokenType, Int>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Int> = token

    fun parseFloat(
        token: Pair<TokenType, Float>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Float> = token

    fun parseString(
        token: Pair<TokenType, String>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, String> = token

    fun parseBoolean(
        token: Pair<TokenType, Boolean>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> = token

    fun parseParentheses(
        token: Pair<TokenType, dynamic>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, dynamic> = parseExpressionInner(
        token.second,
        bytesListTree,
        currentScopeStruct,
        parentScopeStruct,
        ioStream,
        offsetInCurrentIoStream
    )

    fun parseEnum(
        token: Pair<TokenType, String>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ) : Pair<TokenType, Pair<KTEnum, String>> {
        val path: String = token.second.substringBeforeLast("::")
        val enum: KTEnum = getEnum(currentScopeStruct, path)?:
                            getEnum(parentScopeStruct, path)?:
                            getEnum(kaitaiStruct, path)?:
                            throw RuntimeException("The enum $path does not exist.")

        return Pair(TokenType.ENUM, Pair(enum, token.second.substringAfterLast("::")))
    }

    fun tokenizeArray(_array: String): List<String> {
        var array: String = _array
        var result: MutableList<String> = mutableListOf()

        while (array.isNotEmpty()) {
            var slice: Int = array.length

            var sqString: Boolean = false
            var dqString: Boolean = false

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

    fun parseArray(
        token: Pair<TokenType, String>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, List<Pair<TokenType, dynamic>>> {
        var expressions: List<String> = tokenizeArray(token.second)
        var array: MutableList<Pair<TokenType, dynamic>> = mutableListOf()

        for (expression: String in expressions) {
            array.add(
                parseExpressionInner(
                    expression,
                    bytesListTree,
                    currentScopeStruct,
                    parentScopeStruct,
                    ioStream,
                    offsetInCurrentIoStream
                )
            )
        }

        return Pair(TokenType.ARRAY, array.toList())
    }

    fun parseIdentifier(
        token: Pair<TokenType, String>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, dynamic> {
        return when (token.second) {
            "_io" -> {
                Pair(TokenType.STREAM, ioStream)
            }
            "_parent" -> {
                Pair(TokenType.KAITAITREE, bytesListTree.parent)
            }
            "_root" -> {
                Pair(TokenType.KAITAITREE, bytesListTree.root)
            }
            else -> {
                val targetElement = bytesListTree[token.second]
                if (targetElement != null) {
                    when (targetElement) {
                        is KaitaiResult -> Pair(TokenType.KAITAIELEMENT, bytesListTree[token.second])
                        is KaitaiBinary -> Pair(TokenType.INTEGER, Int.fromBytes(targetElement.value.toByteArray(), ByteOrder.BIG))
                        is KaitaiSignedInteger -> Pair(TokenType.INTEGER, Int.fromBytes(targetElement.value.toByteArray(), ByteOrder.BIG))
                    }
                } else {
                    throw RuntimeException("The identifier ${token.second} does not exist.")
                }
                Pair(TokenType.KAITAIELEMENT, bytesListTree[token.second])
            }
        }
    }

    //******************************************************************************************************************
    //*                                     indexing, references and functions                                         *
    //******************************************************************************************************************

    fun parseIndex(
        tokens: MutableList<Pair<TokenType, dynamic>>,
        index: Pair<TokenType, String>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Int> {
        val array: Pair<TokenType, List<Pair<TokenType, dynamic>>> = parseTokens(tokens,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream)
        val index: Int = parseExpressionInner(
            index.second,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        ).second

        return array.second[index]
    }

    fun parseDot(leftTokens: MutableList<Pair<TokenType, dynamic>>,
                 rightToken: Pair<TokenType, dynamic>,
                 bytesListTree: MutableKaitaiTree,
                 currentScopeStruct: KTStruct,
                 parentScopeStruct: KTStruct?,
                 ioStream: BooleanArray,
                 offsetInCurrentIoStream: Int
    ): Pair<TokenType, dynamic> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            leftTokens,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, dynamic> = rightToken

        if (op2.first == TokenType.STREAM) {
            if (op2.second == "eof") {
                return expressionEof(op1, offsetInCurrentIoStream)
            } else if (op2.second == "size") {
                return expressionSize(op2)
            } else if (op2.second == "pos") {
                return expressionPos(op2, offsetInCurrentIoStream)
            }
        }
        return Pair(TokenType.EMPTY, "")
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
        tokens: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Number> {
        return parseTokens(
            tokens,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
    }

    fun parseUnaryMinus(
        tokens: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Number> {
        val op: Pair<TokenType, Number> = parseTokens(
            tokens,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        return if (op.first == TokenType.INTEGER)
            Pair(
                TokenType.INTEGER,
                -(op.second as Int)
            )
        else
            Pair(
                TokenType.FLOAT,
                -(op.second as Float)
            )
    }

    fun parseBooleanNot(
        tokens: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op: Pair<TokenType, dynamic> = parseTokens(
            tokens,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
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

    fun parseMul(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Number> {
        val op1: Pair<TokenType, Number> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Number> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Number> = if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT)
            Pair(
                TokenType.FLOAT,
                (op1.second as Float) * (op2.second as Float)
            )
        else
            Pair(
                TokenType.FLOAT,
                (op1.second as Int) * (op2.second as Int)
            )

        return result
    }

    fun parseDiv(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Number> {
        val op1: Pair<TokenType, Number> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Number> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Number> = if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT)
            Pair(
                TokenType.FLOAT,
                (op1.second as Float) / (op2.second as Float)
            )
        else
            Pair(
                TokenType.FLOAT,
                (op1.second as Int) / (op2.second as Int)
            )

        return result
    }

    fun parseModulo(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Number> {
        val op1: Pair<TokenType, Number> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Number> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Number> = if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT)
            Pair(
                TokenType.FLOAT,
                (op1.second as Float) % (op2.second as Float)
            )
        else
            Pair(
                TokenType.FLOAT,
                (op1.second as Int) % (op2.second as Int)
            )

        return result
    }

    fun parsePlus(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, dynamic> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, dynamic> = if (op1.first == TokenType.STRING && op2.first == TokenType.STRING)
            Pair(
                TokenType.STRING,
                (op1.second as String) + (op2.second as String)
            )
        else if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT)
            Pair(
                TokenType.FLOAT,
                (op1.second as Float) + (op2.second as Float)
            )
        else
            Pair(
                TokenType.FLOAT,
                (op1.second as Int) + (op2.second as Int)
            )

        return result
    }

    fun parseMinus(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Number> {
        val op1: Pair<TokenType, Number> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Number> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Number> = if (op1.first == TokenType.FLOAT || op2.first == TokenType.FLOAT)
            Pair(
                TokenType.FLOAT,
                (op1.second as Float) - (op2.second as Float)
            )
        else
            Pair(
                TokenType.FLOAT,
                (op1.second as Int) - (op2.second as Int)
            )

        return result
    }

    fun parseLShift(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Int> {
        val op1: Pair<TokenType, Int> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Int> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Int> = Pair(
            TokenType.INTEGER,
            op1.second shl op2.second
        )

        return result
    }

    fun parseRShift(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Int> {
        val op1: Pair<TokenType, Int> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Int> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Int> = Pair(
            TokenType.INTEGER,
            op1.second shr op2.second
        )

        return result
    }

    fun parseLess(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Boolean> = Pair(
            TokenType.BOOLEAN,
            op1.second < op2.second
        )

        return result
    }

    fun parseLessEqual(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Boolean> = Pair(
            TokenType.BOOLEAN,
            op1.second <= op2.second
        )

        return result
    }

    fun parseGreater(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Boolean> = Pair(
            TokenType.BOOLEAN,
            op1.second > op2.second
        )

        return result
    }

    fun parseGreaterEqual(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Boolean> = Pair(
            TokenType.BOOLEAN,
            op1.second >= op2.second
        )

        return result
    }

    fun parseEqual(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Boolean> = Pair(
            TokenType.BOOLEAN,
            op1.second == op2.second
        )

        return result
    }

    fun parseNotEqual(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Boolean> = Pair(
            TokenType.BOOLEAN,
            op1.second != op2.second
        )

        return result
    }

    fun parseBitwiseAnd(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Int> {
        val op1: Pair<TokenType, Int> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Int> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Int> = Pair(
            TokenType.INTEGER,
            op1.second and op2.second
        )

        return result
    }

    fun parseBitwiseXor(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Int> {
        val op1: Pair<TokenType, Int> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Int> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Int> = Pair(
            TokenType.INTEGER,
            op1.second xor op2.second
        )

        return result
    }

    fun parseBitwiseOr(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Int> {
        val op1: Pair<TokenType, Int> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        val op2: Pair<TokenType, Int> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Int> = Pair(
            TokenType.INTEGER,
            op1.second or op2.second
        )

        return result
    }

    fun parseBooleanAnd(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        if (!op1.second) {
            return Pair(TokenType.BOOLEAN, false)
        }

        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        val result: Pair<TokenType, Boolean> = Pair(
            TokenType.BOOLEAN,
            (op1.second as Boolean) && (op2.second as Boolean)
        )

        return result
    }

    fun parseBooleanOr(
        tokens1: MutableList<Pair<TokenType, dynamic>>,
        tokens2: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, Boolean> {
        val op1: Pair<TokenType, dynamic> = parseTokens(
            tokens1,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

        if (op1.second) {
            return Pair(TokenType.BOOLEAN, true)
        }

        val op2: Pair<TokenType, dynamic> = parseTokens(
            tokens2,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )

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
        tokensFalse: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, dynamic> {
        val evaluation = parseTokens(
            condition,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
        return if (evaluation.second as Boolean) {
            parseTokens(
                tokensTrue,
                bytesListTree,
                currentScopeStruct,
                parentScopeStruct,
                ioStream,
                offsetInCurrentIoStream
            )
        } else {
            parseTokens(
                tokensFalse,
                bytesListTree,
                currentScopeStruct,
                parentScopeStruct,
                ioStream,
                offsetInCurrentIoStream
            )
        }
    }


    fun parseTokens(
        tokens: MutableList<Pair<TokenType, dynamic>>,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, dynamic> {
        if (tokens.size == 1 && tokens[0].first in operandTokens) { // while they have the highest precedence of any token they are the only option in case there is only one token left and can therefore be the first check
            val function = operandTokens.getValue(tokens[0].first)
            return function.invoke(
                tokens[0],
                bytesListTree,
                currentScopeStruct,
                parentScopeStruct,
                ioStream,
                offsetInCurrentIoStream
            )
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
                            tokens.subList(posColon + 1, tokens.size),
                            bytesListTree,
                            currentScopeStruct,
                            parentScopeStruct,
                            ioStream,
                            offsetInCurrentIoStream
                        )
                }
            }
        }

        for (operatorMap in binaryPrecedence.reversed()) { // the lower precedence operators are checked first
            for ((index, token) in tokens.reversed().withIndex()) {
                if (index != tokens.size - 1 && tokens.reversed()[index + 1].first in operandTokens && token.first in operatorMap) {
                    val op1: MutableList<Pair<TokenType, dynamic>> = tokens.subList(0, tokens.size - index - 1)
                    val op2: MutableList<Pair<TokenType, dynamic>> = tokens.subList(tokens.size - index, tokens.size)
                    val function = operatorMap.getValue((token.first))
                    return function.invoke(
                        op1, op2,
                        bytesListTree,
                        currentScopeStruct,
                        parentScopeStruct,
                        ioStream,
                        offsetInCurrentIoStream
                    )
                }
            }
        }

        for ((index, token) in tokens.withIndex()) { // unary operands all have the same precedence
            if (token.first in unaryPrecedence) {
                val op: MutableList<Pair<TokenType, dynamic>> = tokens.subList(index + 1, tokens.size)
                val function = unaryPrecedence.getValue(token.first)
                return function.invoke(
                    op,
                    bytesListTree,
                    currentScopeStruct,
                    parentScopeStruct,
                    ioStream,
                    offsetInCurrentIoStream
                )
            }
        }

        for ((index, token) in tokens.withIndex()) { //

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
            var depth: Int = 0
            var sqstring: Boolean = false
            var dqstring: Boolean = false
            for ((index, char) in trimmedExpression.substring(2).withIndex()) {
                when (char.toString()) {
                    "'" -> if (!dqstring) sqstring = !sqstring
                    "\"" -> if (!sqstring && (!dqstring || trimmedExpression[index - 1] != '\\')) dqstring = !dqstring
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
            return Pair(Pair(TokenType.CAST, trimmedExpression.substring(0..breakIndex + 1)), trimmed + breakIndex + 2)
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

            '[' -> { // token BRACKETS
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
                    Pair(TokenType.BRACKETS, trimmedExpression.substring(1..breakIndex - 2)),
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
                        trimmedExpression.substring(2..breakIndex - 1).replace("_", "").toInt(16)
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
                        trimmedExpression.substring(2..breakIndex - 1).replace("_", "").toInt(2)
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
                        trimmedExpression.substring(2..breakIndex - 1).replace("_", "").toInt(8)
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
                            trimmedExpression.substring(0..breakIndex - 1).replace("_", "").toFloat()
                        ), trimmed + breakIndex
                    )
                else
                    return Pair(
                        Pair(
                            TokenType.INTEGER,
                            trimmedExpression.substring(0..breakIndex - 1).replace("_", "").toInt()
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
        }

        var tokensWithEnums: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()
        var enum: Boolean = false

        for ((index: Int, token: Pair<TokenType, dynamic>) in tokens.withIndex()) {
            if (!enum) {
                if (token.first == TokenType.IDENTIFIER && tokens.getOrNull(index + 1) != null && tokens.getOrNull(index + 1)!!.first == TokenType.DOUBLECOLON) {
                    tokensWithEnums.add(Pair(TokenType.ENUMCALL, String))
                    tokensWithEnums.last().second.plus(token.second)
                }
            } else {
                if (token.first != TokenType.IDENTIFIER && token.first != TokenType.DOUBLECOLON) {
                    enum = false
                } else {
                    tokensWithEnums.last().second.plus(token.second)
                }
            }
        }

        var tokensWithIndex: MutableList<Pair<TokenType, dynamic>> = mutableListOf<Pair<TokenType, dynamic>>()

        for ((index, token: Pair<TokenType, dynamic>) in tokensWithEnums.withIndex()) {
            if (token.first == TokenType.ARRAY) {
                if (index > 0 &&
                    (tokensWithEnums[index - 1].second == TokenType.IDENTIFIER ||
                            tokensWithEnums[index - 1].second == TokenType.ARRAY ||
                            tokensWithEnums[index - 1].second == TokenType.INDEX)
                ) {
                    tokensWithIndex.add(Pair(TokenType.INDEX, token.second))
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
                    tokensWithFunctions.add(
                        Pair(
                            TokenType.FUNCTION,
                            Pair(tokensWithIndex[index - 1], token)
                        )
                    )
                } else {
                    tokensWithFunctions.add(token)
                }
            }
        }

        return tokensWithFunctions
    }

    fun parseExpressionInner(
        expression: String,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): Pair<TokenType, dynamic> {
        return parseTokens(
            tokenizeExpression(expression),
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        )
    }

    fun parseExpression(
        expression: String,
        bytesListTree: MutableKaitaiTree,
        currentScopeStruct: KTStruct,
        parentScopeStruct: KTStruct?,
        ioStream: BooleanArray,
        offsetInCurrentIoStream: Int
    ): dynamic {
        return parseExpressionInner(
            expression,
            bytesListTree,
            currentScopeStruct,
            parentScopeStruct,
            ioStream,
            offsetInCurrentIoStream
        ).second
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
                val size = match.groupValues[2].toUInt()
                when (typePrefix) {
                    "s" -> {
                        type.usedDisplayStyle = DisplayStyle.SIGNED_INTEGER
                        type.sizeInBits = size * 8u
                    }
                    "u" -> {
                        type.usedDisplayStyle = DisplayStyle.UNSIGNED_INTEGER
                        type.sizeInBits = size * 8u
                    }
                    "f" -> {
                        type.usedDisplayStyle = DisplayStyle.FLOAT
                        type.sizeInBits = size * 8u
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
            type.sizeInBits = parseValue(seqElement.contents, bytesListTree).size.toUInt()
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
                type.sizeInBits = seqElement.size.value.toUInt() * 8u
            } else {
                val parsedValue = parseValue(seqElement.size.toString(), bytesListTree)
                type.sizeInBits = parsedValue.toByteArray().toUInt(ByteOrder.BIG) * 8u
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

        var dataSizeOfSequenceInBits: Int = 0
        if (currentScopeStruct.seq.isNotEmpty()) {
            for (seqElement in currentScopeStruct.seq) {
                checkNotNull(seqElement.id) { "Sequence element id must not be null" }
                val elementId = seqElement.id

                val bytesListTreeForInnerList = if (seqElement.repeat != null) {
                    val bytesListTreeForInnerList = MutableKaitaiTree()
                    bytesListTreeForInnerList.parent = bytesListTree
                    bytesListTreeForInnerList.byteOrder = bytesListTreeForInnerList.parent!!.byteOrder
                    bytesListTreeForInnerList
                } else {
                    null
                }

                var repeatAmount = 0
                while (true) { // repeat key demands we create many elements possibly. We break at the very end of the loop
                    val type = parseType(parentScopeStruct, currentScopeStruct, seqElement, bytesListTree)
                    if (type.sizeIsUntilEOS) {
                        type.sizeInBits = (ioStream.size - offsetInDatastreamInBits).toUInt()
                        type.sizeIsKnown = true
                    }
                    if (type.terminator != null) {
                        val dataAsByteArray =
                            ioStream.toByteArray()  // technically not ideal as we don't check for byte-alignment, but right after we throw away all results if it's not byte aligned
                        type.sizeInBits =
                            (dataAsByteArray.sliceArray(offsetInDatastreamInBits / 8..dataAsByteArray.size - 1).indexOf(type.terminator!!.toByte())
                                .toUInt() + 1u) * 8u
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
                        ioStream.sliceArray(offsetInDatastreamInBits..offsetInDatastreamInBits + type.sizeInBits.toInt() - 1)
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
                                (type.usedDisplayStyle == DisplayStyle.BINARY && type.sizeInBits == 1u))) {
                        val path: KTEnum? = getEnum(currentScopeStruct, seqElement.enum) ?:
                                            getEnum(parentScopeStruct, seqElement.enum)?:
                                            getEnum(kaitaiStruct, seqElement.enum)
                        if (path != null) {
                            if (type.usedDisplayStyle == DisplayStyle.UNSIGNED_INTEGER) {
                                if (path[Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toUInt().toLong()] == null) {
                                    throw RuntimeException("The enum ${seqElement.enum} has no key-value pair with the given key ${Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toUInt().toLong()}")
                                }else {
                                    enum = Pair(
                                        path,
                                        path[Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toUInt().toLong()]!!.id.toString()
                                    )
                                }
                            } else if (type.usedDisplayStyle == DisplayStyle.SIGNED_INTEGER) {
                                if (path[Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toLong()] == null) {
                                    throw RuntimeException("The enum ${seqElement.enum} has no key-value pair with the given key ${Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toLong()}")
                                }else {
                                    enum = Pair(
                                        path,
                                        path[Int.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG).toLong()]!!.id.toString()
                                    )
                                }
                            }
                            else {
                                if (path[if (ioSubStream[0]) 1 else 0] == null) {
                                    val temp = if (ioSubStream[0]) 1 else 0
                                    throw RuntimeException("The enum ${seqElement.enum} has no key-value pair with the given key ${Long.fromBytes(ioSubStream.toByteArray(), ByteOrder.BIG)}")
                                }else {
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
                            seqElement.id,
                            seqElement,
                            bytesListTree,
                            currentScopeStruct,
                            type.customType!!,
                            ioSubStream,
                            sourceOffsetInBits + dataSizeOfSequenceInBits,
                            if (type.sizeInBits != 0u) 0 else offsetInDatastreamInBits
                        )

                        if (seqElement.doc == null && seqElement.docRef == null) {
                            // current sequence element has no doc, so we try to get the doc from the type
                            kaitaiElement.doc = KaitaiDoc(type.customType?.doc, type.customType?.docRef)
                        }
                    } else {
                        val sourceByteRange = Pair(
                            (sourceOffsetInBits + dataSizeOfSequenceInBits) / 8,
                            (sourceOffsetInBits + dataSizeOfSequenceInBits + type.sizeInBits.toInt()) / 8
                        )
                        val sourceRangeBitOffset = Pair(
                            (sourceOffsetInBits + dataSizeOfSequenceInBits) % 8,
                            (sourceOffsetInBits + dataSizeOfSequenceInBits + type.sizeInBits.toInt()) % 8
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
                        type.sizeInBits = kaitaiElement.value.size.toUInt()
                        type.sizeIsKnown
                    }

                    offsetInDatastreamInBits += type.sizeInBits.toInt()
                    dataSizeOfSequenceInBits += type.sizeInBits.toInt()

                    if (bytesListTreeForInnerList != null) {
                        bytesListTreeForInnerList.add(kaitaiElement)
                    } else {
                        bytesListTree.add(kaitaiElement)
                    }

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
                    } else {
                        break  // if we have no repeat at all we just do this inner loop once
                    }
                }

                if (bytesListTreeForInnerList != null) {
                    val resultSourceByteRange =
                        Pair(bytesListTreeForInnerList.first().sourceByteRange!!.first, bytesListTreeForInnerList.last().sourceByteRange!!.second)
                    val resultSourceRangeBitOffset =
                        Pair(bytesListTreeForInnerList.first().sourceRangeBitOffset.first, bytesListTreeForInnerList.last().sourceRangeBitOffset.second)
                    bytesListTree.add(
                        KaitaiList(
                            elementId, bytesListTree.byteOrder, bytesListTreeForInnerList, resultSourceByteRange, resultSourceRangeBitOffset,
                            KaitaiDoc(undefined, undefined)
                        )
                    )
                }
            }
        }
        val resultDoc = KaitaiDoc(parentSeq?.doc, parentSeq?.docRef)

        // TODO we should have way to see if we have a substream or not and highlight accordingly. currently only until end of data gets highlighted, not all of the actual substream
        val resultSourceByteRange: Pair<Int, Int>
        val resultSourceRangeBitOffset: Pair<Int, Int>
        if (currentScopeStruct.seq.isEmpty()) {
            resultSourceByteRange = Pair(0,0) // is it possible to do the actual start and end value (which are the same) here instead of 0?
            resultSourceRangeBitOffset = Pair(0,0)
        } else {
            resultSourceByteRange = Pair(bytesListTree.first().sourceByteRange!!.first, bytesListTree.last().sourceByteRange!!.second)
            resultSourceRangeBitOffset = Pair(bytesListTree.first().sourceRangeBitOffset.first, bytesListTree.last().sourceRangeBitOffset.second)
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
        return "<div class=\"generic roundbox tooltip\" $byteRangeDataTags>${bytesListTree.joinToString(", ", "[", "]") { it.renderHTML() }}</div>"
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