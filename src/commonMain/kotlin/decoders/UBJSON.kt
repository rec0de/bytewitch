package decoders

import Logger
import ParseCompanion
import bitmage.*
import looksLikeUtf8String


// Once again reusing OPack classes
// there seems to be deprecated incompatible UBJSON versions
// (see: https://github.com/ubjson/universal-binary-json-java/blob/master/src/main/java/org/ubjson/io/UBJInputStreamParser.java)
// we try to somewhat support them but it probably doesn't fully work
class UbjsonParser : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "UBJSON"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return UbjsonParser().parseTopLevel(data, sourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            try {
                val parser = UbjsonParser()
                val result = parser.parse(data)
                val remainder = data.fromIndex(parser.parseOffset)

                // parsed CBOR should represent at least 30% of input data
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

        // single bytes are often false-positive detected as booleans
        override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
            if(data.size < 3)
                return Pair(0.0, null)
            return super.confidence(data, sourceOffset)
        }
    }

    private var sourceOffset = 0
    private var looksDeprecated = false

    private val lastConsumedBytePosition: Int
        get() = sourceOffset + parseOffset

    fun parseTopLevel(bytes: ByteArray, sourceOffsetParam: Int): OpackObject {
        looksDeprecated = false
        parseOffset = 0
        sourceOffset = sourceOffsetParam
        val result = parse(bytes)

        check(parseOffset >= bytes.size){ "input data not fully consumed" }

        return result
    }

    private fun parse(bytes: ByteArray, typeOverride: Char? = null): OpackObject {
        check(parseOffset < bytes.size)
        val start = parseOffset + sourceOffset

        val typeByte = if(typeOverride != null)
            typeOverride
        else {
            parseOffset += 1
            bytes[parseOffset-1].toUByte().toInt().toChar()
        }

        Logger.log("reading UBJSON type $typeByte / ${bytes[parseOffset-1]}")

        return when(typeByte) {
            'Z' -> OPNull(start)
            'N' -> parse(bytes) // nop, parse next
            'T' -> OPTrue(start)
            'F' -> OPFalse(start)
            'i' -> OPInt(if(looksDeprecated) readInt(bytes, 2, explicitlySigned = true) else readInt(bytes, 1, explicitlySigned = true), Pair(start, lastConsumedBytePosition))
            'U', 'B' -> OPUInt(readUInt(bytes, 1), Pair(start, lastConsumedBytePosition))
            'I' -> OPInt(if(looksDeprecated) readInt(bytes, 4) else readInt(bytes, 2, explicitlySigned = true), Pair(start, lastConsumedBytePosition))
            'l' -> OPInt(readInt(bytes, 4, explicitlySigned = true), Pair(start, lastConsumedBytePosition))
            'L' -> OPInt(readLong(bytes, 8), Pair(start, lastConsumedBytePosition))
            'd' -> OPReal(readFloat(bytes).toDouble(), Pair(start, lastConsumedBytePosition))
            'D' -> OPReal(readDouble(bytes), Pair(start, lastConsumedBytePosition))
            'C' -> OPString(readInt(bytes, 1, explicitlySigned = false).toChar().toString(), Pair(start, lastConsumedBytePosition))
            'S' -> {
                val byteLength = if(looksDeprecated) {
                    val length = parse(bytes)
                    if(length is OPUInt) length.value.toLong() else (length as OPInt).value
                }
                else
                    readInt(bytes, 4)

                val stringBytes = readBytes(bytes, byteLength.toInt())
                check(looksLikeUtf8String(stringBytes, enableLengthBias = false) > 0.5) { "UBJSON string has implausible content: ${stringBytes.hex()}" }
                OPString(stringBytes.decodeToString(), Pair(start, lastConsumedBytePosition))
            }
            'H' -> {
                // high precision numbers, handle as raw data
                val length = parse(bytes)
                val byteLength = if(length is OPUInt) length.value.toLong() else (length as OPInt).value
                val numberBytes = readBytes(bytes, byteLength.toInt())
                OPData(numberBytes, Pair(start, lastConsumedBytePosition))
            }
            '[' -> {
                val values = mutableListOf<OpackObject>()
                var compactType: Char? = null

                if(bytes[parseOffset].toInt().toChar() == '$') {
                    parseOffset += 1
                    compactType = bytes[parseOffset].toInt().toChar()
                    parseOffset += 1
                }

                if(bytes[parseOffset].toInt().toChar() == '#') {
                    parseOffset += 1
                    val length = parse(bytes)
                    val itemCount = if(length is OPUInt) length.value.toLong() else (length as OPInt).value
                    var i = 0
                    while(i < itemCount) {
                        if(compactType != null)
                            values.add(parse(bytes, compactType))
                        else
                            values.add(parse(bytes))
                        i += 1
                    }
                }
                else {
                    check(compactType == null) { "compact type requires explicit length" }
                    while(bytes[parseOffset].toInt().toChar() != ']') {
                        values.add(parse(bytes))
                    }
                    parseOffset += 1
                }

                OPArray(values, Pair(start, lastConsumedBytePosition))
            }
            '{' -> {
                val values = mutableMapOf<OpackObject,OpackObject>()
                var compactType: Char? = null

                if(bytes[parseOffset].toInt().toChar() == '$') {
                    parseOffset += 1
                    compactType = bytes[parseOffset].toInt().toChar()
                    parseOffset += 1
                }

                if(bytes[parseOffset].toInt().toChar() == '#') {
                    parseOffset += 1
                    val length = parse(bytes)
                    val itemCount = if(length is OPUInt) length.value.toLong() else (length as OPInt).value
                    var i = 0
                    while(i < itemCount) {
                        val key = parse(bytes, 'S')
                        if(compactType != null)
                            values[key] = parse(bytes, compactType)
                        else
                            values[key] = parse(bytes)
                        i += 1
                    }
                }
                else {
                    check(compactType == null) { "compact type requires explicit length" }
                    while(bytes[parseOffset].toInt().toChar() != '}') {
                        val key = parse(bytes, 'S')
                        val value = parse(bytes)
                        values[key] = value
                    }
                    parseOffset += 1
                }

                OPDict(values, Pair(start, lastConsumedBytePosition))
            }
            // deprecated string type
            's' -> OPString( readLengthPrefixedString(bytes, 1) ?: "", Pair(start, lastConsumedBytePosition))
            // deprecated container types
            'A', 'a' -> {
                looksDeprecated = true
                val values = mutableListOf<OpackObject>()
                val length = if(typeByte == 'a') readInt(bytes, 1) else readInt(bytes, 4)
                var i = 0
                while(i < length) {
                    values.add(parse(bytes))
                    i += 1
                }
                OPArray(values, Pair(start, lastConsumedBytePosition))
            }
            'O', 'o' -> {
                looksDeprecated = true
                val values = mutableMapOf<OpackObject, OpackObject>()
                val length = if(typeByte == 'o') readInt(bytes, 1) else readInt(bytes, 4)
                var i = 0
                while(i < length) {
                    val key = parse(bytes)
                    val value = parse(bytes)
                    values[key] = value
                    i += 1
                }
                OPDict(values, Pair(start, lastConsumedBytePosition))
            }
            // deprecated end type
            else -> throw Exception("Unsupported type $typeByte at $parseOffset")
        }
    }
}