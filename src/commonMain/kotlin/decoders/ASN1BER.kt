package decoders

import ByteWitch
import Logger
import ParseCompanion
import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.fromIndex
import bitmage.hex
import dateFromUTCString
import htmlEscape

class ASN1BER : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "ASN.1"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            val decoder = ASN1BER()
            val result = decoder.decode(data, sourceOffset)
            check(decoder.parseOffset >= data.size-1){ "input data not fully consumed" }
            return result
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            val decoder = ASN1BER()
            try {
                val result = decoder.decode(data, 0)
                val remainder = data.fromIndex(decoder.parseOffset)

                // parsed ASN.1 should represent at least 30% of input data
                if(remainder.size > data.size * 0.7)
                    return null

                return if(remainder.isEmpty())
                    result
                else
                    PartialDecode(byteArrayOf(), result, remainder, Pair(0, data.size))
            } catch (e: Exception) {
                Logger.log(e.toString())
                return null
            }
        }
    }

    private fun decode(bytes: ByteArray, sourceOffset: Int) : ASN1Result {
        val start = parseOffset + sourceOffset
        val tag = readIdentifier(bytes)

        checkPlausibleTag(tag)
        
        val len = readLength(bytes)
        if(len > bytes.size - parseOffset)
            throw Exception("excessive length: $len")

        val payloadStartOffset = parseOffset
        val payload = readBytes(bytes, len)
        val byteRange = Pair(start, sourceOffset+parseOffset)


        //Logger.log("Payload size: $len")
        //Logger.log("Payload: ${payload.hex()}")

        val supportedTypes = setOf(0, 1, 2, 5, 6, 12, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30)

        return if(tag.tagClass == ASN1Class.Universal && tag.type in supportedTypes) {
            when(tag.type) {
                0 -> {
                    check(len == 0){"end-of-contents with unexpected payload length $len"}
                    ASN1Null(tag, len, byteRange) // naja, todo
                }
                // Boolean
                1 -> {
                    check(len == 1){"boolean with unexpected payload length $len"}
                    ASN1Boolean(tag, len, payload[0].toInt() != 0, byteRange)
                }
                2 -> ASN1Integer(tag, len, payload, byteRange)
                // Null
                5 -> {
                    check(len == 0){"null with unexpected payload length $len"}
                    ASN1Null(tag, len, byteRange)
                }
                // ObjectIdentifier
                6 -> ASN1ObjectIdentifier(tag, len, payload, byteRange)
                // Sequence, Set
                16, 17 -> {
                    // aight, hacky, but we want to reset parsing progress here
                    parseOffset -= payload.size
                    val elements = mutableListOf<ASN1Result>()
                    while(parseOffset < payloadStartOffset + payload.size) {
                        elements.add(decode(bytes, sourceOffset))
                    }
                    ASN1Sequence(tag, len, elements, byteRange)
                }
                // Strings
                12, in 18..22, in 25..30 -> ASN1String(tag, len, payload.decodeToString(), byteRange)
                // Time
                23 -> {
                    val string = payload.decodeToString()
                    ASN1Time(tag, len, string, byteRange, dateFromUTCString(string, false).toString())
                }
                24 -> {
                    val string = payload.decodeToString()
                    ASN1Time(tag, len, string, byteRange, dateFromUTCString(string, true).toString())
                }
                else -> throw Exception("unreachable (update supported types?)")
            }
        }
        else if(tag.primitive) {
            GenericASN1Result(tag, len, payload, Pair(sourceOffset, sourceOffset+parseOffset))
        }
        else {
            parseOffset -= payload.size
            val elements = mutableListOf<ASN1Result>()
            while(parseOffset < payloadStartOffset + payload.size) {
                elements.add(decode(bytes, sourceOffset))
            }
            ASN1Constructed(tag, len, elements, byteRange)
        }
    }

    private fun readIdentifier(bytes: ByteArray) : ASN1Tag {
        val start = parseOffset
        val firstByte = readBytes(bytes, 1)[0].toUByte().toInt()

        val tagClass = (firstByte shr 6) and 0x03
        val primitive = (firstByte and 0x20) == 0
        var tagType = firstByte and 0x1F

        if(tagType == 31) {
            var moreBytesFollow = true
            tagType = 0
            while(moreBytesFollow) {
                val continuationByte = readBytes(bytes, 1)[0].toUByte().toInt()
                moreBytesFollow = (continuationByte and 0x80) != 0
                val tagBits = continuationByte and 0x7F
                tagType = (tagType shl 7) or tagBits
            }
        }

        val encodedLength = parseOffset - start
        val tag = ASN1Tag(ASN1Class.fromInt(tagClass), primitive, tagType, encodedLength)
        //Logger.log("read ASN tag: $tag")
        return tag
    }

    private fun readLength(bytes: ByteArray): Int {
        val firstByte = readBytes(bytes, 1)[0].toUByte().toInt()
        return when(firstByte) {
            in 0..127 -> firstByte
            128 -> throw Exception("indefinite length not supported")
            in 129..254 -> readInt(bytes, firstByte - 128)
            255 -> throw Exception("reserved length tag value 255")
            else -> throw Exception("unreachable")
        }
    }

    private fun checkPlausibleTag(tag: ASN1Tag) {
        if(tag.tagClass == ASN1Class.Universal && tag.type <= 36) {
            if(tag.primitive && tag.type in listOf(8, 11, 16, 17, 29))
                throw Exception("Expected constructed for type ${tag.type} but is marked primitive")
            else if(!tag.primitive && tag.type in listOf(0, 1, 2, 5, 6, 9, 10, 13, 14, 31, 32, 33, 34, 35, 36))
                throw Exception("Expected primitive for type ${tag.type} but is marked constructed")
        }
        if(tag.tagClass == ASN1Class.Universal && tag.type == 15)
            throw Exception("reserved ASN.1 type 15")
    }

    data class ASN1Tag(val tagClass: ASN1Class, val primitive: Boolean, val type: Int, val encodedLength: Int) {
        override fun toString(): String {
            return if(tagClass == ASN1Class.Universal && type <= 36) {
                universalTypeNames[type]
            }
            else {
                val primitiveConstructed = if(primitive) "primitive" else "constructed"
                "$tagClass $primitiveConstructed type: $type"
            }
        }

        companion object {
            val universalTypeNames = listOf("End-of-Content", "BOOLEAN", "INTEGER", "BIT STRING", "OCTET STRING", "NULL", "OBJECT IDENTIFIER", "Object Descriptor", "EXTERNAL", "REAL", "ENUMERATED", "EMBEDDED PDV", "UTF8String", "RELATIVE-OID", "TIME", "Reserved", "SEQUENCE", "SET", "NumericString", "PrintableString", "T61String", "VideotexString", "IA5String", "UTCTime", "GeneralizedTime", "GraphicString", "VisibleString", "GeneralString", "UniversalString", "CHARACTER STRING", "BMPString", "DATE", "TIME-OF-DAY", "DATE-TIME", "DURATION", "OID-IRI", "RELATIVE-OID-IRI")
        }
    }

    enum class ASN1Class(val value: Int) {
        Universal(0), Application(1), ContextSpecific(2), Private(3);

        companion object {
            fun fromInt(value: Int): ASN1Class {
                return when (value) {
                    0 -> Universal
                    1 -> Application
                    2 -> ContextSpecific
                    else -> Private
                }
            }
        }
    }
}

abstract class ASN1Result(val tag: ASN1BER.ASN1Tag, val length: Int, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"roundbox asn1\" $byteRangeDataTags>${renderHtmlValue()}</div>"
    }

    abstract fun renderHtmlValue(): String

    val asnTagByteRangeDataTags: String
        get() = rangeTagsFor(sourceByteRange.first, sourceByteRange.first+tag.encodedLength)
    val asnLengthByteRangeDataTags: String
        get() = rangeTagsFor(sourceByteRange.first+tag.encodedLength, sourceByteRange.second-length)
    val asnPayloadByteRangeDataTags: String
        get() = rangeTagsFor(sourceByteRange.second-length, sourceByteRange.second)

    val tagLengthDivs: String
        get() = "<div class=\"bpvalue\" $asnTagByteRangeDataTags>$tag</div> <div class=\"bpvalue\" $asnLengthByteRangeDataTags>length $length</div>"
}

class GenericASN1Result(tag: ASN1BER.ASN1Tag, length: Int, val payload: ByteArray, sourceByteRange: Pair<Int, Int>) : ASN1Result(tag, length, sourceByteRange) {
    override fun renderHtmlValue(): String {
        // try to decode nested stuff
        val decode = ByteWitch.quickDecode(payload, sourceByteRange.second - payload.size)
        val payloadHtml = decode?.renderHTML() ?: ("0x" + payload.hex())

        // we have to wrap in a bpvalue if we have a nested decode of the same type to distinguish them visually
        // for nested decodes of different types we can omit it for cleaner display
        val requiresWrapping = decode == null || decode is ASN1Result

        val prePayload = if(requiresWrapping) "<div class=\"bpvalue data\" $asnPayloadByteRangeDataTags>" else ""
        val postPayload = if(requiresWrapping) "</div>" else ""

        return "$tagLengthDivs $prePayload$payloadHtml$postPayload"
    }
}

class ASN1Constructed(tag: ASN1BER.ASN1Tag, length: Int, val elements: List<ASN1Result>, sourceByteRange: Pair<Int, Int>) : ASN1Result(tag, length, sourceByteRange) {
    override fun renderHtmlValue(): String {
        return if(elements.size == 1)
            "$tagLengthDivs <div class=\"bpvalue data\" $asnPayloadByteRangeDataTags>${elements[0].renderHTML()}</div>"
            else
                "$tagLengthDivs <div class=\"bpvalue asn1sequence\" $asnPayloadByteRangeDataTags>${elements.joinToString("") { it.renderHTML() }}</div>"
    }
}

class ASN1String(tag: ASN1BER.ASN1Tag, length: Int, val value: String, sourceByteRange: Pair<Int, Int>) : ASN1Result(tag, length, sourceByteRange) {
    override fun renderHtmlValue(): String {
        val veryLong = if(value.length > 300) " data" else ""
        return "$tagLengthDivs <div class=\"bpvalue stringlit$veryLong\" $asnPayloadByteRangeDataTags>\"${htmlEscape(value)}\"</div>"
    }
}

class ASN1Integer(tag: ASN1BER.ASN1Tag, length: Int, val value: ByteArray, sourceByteRange: Pair<Int, Int>) : ASN1Result(tag, length, sourceByteRange) {
    override fun renderHtmlValue(): String {
        val valueRendering = when {
            value.size <= 4 -> Int.fromBytes(value, ByteOrder.BIG).toString()
            value.size <= 8 -> Long.fromBytes(value, ByteOrder.BIG).toString()
            else -> "0x${value.hex()}"
        }
        return "$tagLengthDivs <div class=\"bpvalue data\" $asnPayloadByteRangeDataTags>$valueRendering</div>"
    }
}

class ASN1Time(tag: ASN1BER.ASN1Tag, length: Int, val value: String, sourceByteRange: Pair<Int, Int>, private val rendering: String) : ASN1Result(tag, length, sourceByteRange) {
    override fun renderHtmlValue(): String {
        return "$tagLengthDivs <div class=\"bpvalue data\" $asnPayloadByteRangeDataTags>$rendering</div>"
    }
}

class ASN1Boolean(tag: ASN1BER.ASN1Tag, length: Int, val value: Boolean, sourceByteRange: Pair<Int, Int>) : ASN1Result(tag, length, sourceByteRange) {
    override fun renderHtmlValue(): String {
        return "$tagLengthDivs <div class=\"bpvalue data\" $asnPayloadByteRangeDataTags>$value</div>"
    }
}

class ASN1Null(tag: ASN1BER.ASN1Tag, length: Int, sourceByteRange: Pair<Int, Int>) : ASN1Result(tag, length, sourceByteRange) {
    override fun renderHtmlValue() = tagLengthDivs
}

class ASN1ObjectIdentifier(tag: ASN1BER.ASN1Tag, length: Int, val value: ByteArray, sourceByteRange: Pair<Int, Int>) : ASN1Result(tag, length, sourceByteRange) {

    private fun readVarInt(bytes: ByteArray): Pair<Int, ByteArray> {
        var moreBytesFollow = true
        var value = 0
        var i = 0
        while(moreBytesFollow) {
            val continuationByte = bytes[i].toUByte().toInt()
            moreBytesFollow = (continuationByte and 0x80) != 0
            val valueBits = continuationByte and 0x7F
            value = (value shl 7) or valueBits
            i += 1
        }

        return Pair(value, bytes.fromIndex(i))
    }

    override fun renderHtmlValue(): String {

        val components = readVarInt(value)
        val first = components.first
        var rest = components.second

        val a = when(first) {
            in 0..39 -> 0
            in 40..79 -> 1
            else -> 2
        }

        val b = when(first) {
            in 0..39 -> first
            in 40..79 -> first - 40
            else -> first - 80
        }

        val subidentifiers = mutableListOf(a, b)

        while(rest.isNotEmpty()) {
            val components = readVarInt(rest)
            subidentifiers.add(components.first)
            rest = components.second
        }

        val oid = subidentifiers.joinToString(".")

        return "$tagLengthDivs <div class=\"bpvalue data\" $asnPayloadByteRangeDataTags><a href=\"https://oid-rep.orange-labs.fr/get/$oid\" target=\"_blank\">$oid</a></div>"
    }
}

class ASN1Sequence(tag: ASN1BER.ASN1Tag, length: Int, val elements: List<ASN1Result>, sourceByteRange: Pair<Int, Int>) : ASN1Result(tag, length, sourceByteRange) {
    override fun renderHtmlValue(): String {
        val renderedElements = if(elements.isEmpty()) "∅" else elements.joinToString("") { it.renderHTML() }
        return "$tagLengthDivs <div class=\"bpvalue asn1sequence\" $asnPayloadByteRangeDataTags>$renderedElements</div>"
    }
}