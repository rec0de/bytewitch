package decoders

import Logger
import ParseCompanion
import bitmage.*
import looksLikeUtf8String


// CBOR is basically MsgPack is basically OPack, so we'll re-use classes
// see https://www.rfc-editor.org/rfc/rfc8949.html
class CborParser : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "CBOR"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return CborParser().parseTopLevel(data, sourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            try {
                val parser = CborParser()
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
            if(data.size < 4)
                return Pair(0.0, null)
            return super.confidence(data, sourceOffset)
        }
    }

    private var sourceOffset = 0

    private val lastConsumedBytePosition: Int
        get() = sourceOffset + parseOffset

    fun parseTopLevel(bytes: ByteArray, sourceOffsetParam: Int): OpackObject {
        parseOffset = 0
        sourceOffset = sourceOffsetParam
        val result = parse(bytes)

        check(parseOffset >= bytes.size){ "input data not fully consumed" }

        return result
    }

    private fun parse(bytes: ByteArray): OpackObject {
        check(parseOffset < bytes.size)
        val start = parseOffset + sourceOffset
        val typeByte = bytes[parseOffset].toUByte().toUInt()

        val wireType = (typeByte shr 5).toInt()
        val count = (typeByte and 0x1fu).toInt()
        parseOffset += 1

        return when(wireType) {
            0 -> {
                val value = readCount(bytes, count)
                OPInt(value.toLong(), Pair(start, lastConsumedBytePosition))
            }
            1 -> {
                val value = readCount(bytes, count)
                OPInt(-1-value.toLong(), Pair(start, lastConsumedBytePosition))
            }
            2 -> {
                val length = readCount(bytes, count)
                if(length == ULong.MAX_VALUE) {
                    var bytearray = byteArrayOf()
                    var nested = parse(bytes)
                    while(nested !is CborEndMarker) {
                        check(nested is OPData){ "indefinite data contains invalid nested data $nested" }
                        bytearray += nested.value
                        nested = parse(bytes)
                    }
                    OPData(bytearray, Pair(start, lastConsumedBytePosition))
                }
                else
                    OPData(readBytes(bytes, length.toInt()), Pair(start, lastConsumedBytePosition))
            }
            3 -> {
                val length = readCount(bytes, count)
                if(length == ULong.MAX_VALUE) {
                    var string = ""
                    var nested = parse(bytes)
                    while(nested !is CborEndMarker) {
                        check(nested is OPString){ "indefinite string contains invalid nested data $nested" }
                        string += nested.value
                        nested = parse(bytes)
                    }
                    OPString(string, Pair(start, lastConsumedBytePosition))
                }
                else {
                    val stringBytes = readBytes(bytes, length.toInt())
                    check(looksLikeUtf8String(stringBytes, false) > 0.5) { "cbor string with implausible content: ${stringBytes.hex()}" }
                    OPString(stringBytes.decodeToString(), Pair(start, lastConsumedBytePosition))
                }
            }
            4 -> {
                val length = readCount(bytes, count)
                val indefinite = length == ULong.MAX_VALUE
                var i = 0u
                val elements = mutableListOf<OpackObject>()
                while (i < length) {
                    val element = parse(bytes)
                    if(indefinite && element is CborEndMarker)
                        break
                    elements.add(element)
                    i += 1u
                }
                OPArray(elements, Pair(start, lastConsumedBytePosition))
            }
            5 -> {
                val length = readCount(bytes, count)
                val indefinite = length == ULong.MAX_VALUE
                var i = 0u
                val elements = mutableMapOf<OpackObject,OpackObject>()
                while (i < length) {
                    val key = parse(bytes)

                    if(indefinite && key is CborEndMarker)
                        break

                    val value = parse(bytes)
                    elements[key] = value
                    i += 1u
                }
                OPDict(elements, Pair(start, lastConsumedBytePosition))
            }
            6 -> {
                val tag = readCount(bytes, count).toUInt()
                val value = parse(bytes)

                if(tag == 1u) {
                    val timestamp = when(value) {
                        is OPInt -> value.value.toDouble()
                        is OPReal -> value.value
                        else -> throw Exception("timestamp with unsupported value $value")
                    }
                    OPDate(
                        timestamp,
                        Pair(start, lastConsumedBytePosition),
                        isAppleEpoch = false
                    )
                }
                else
                    OPTaggedParsedData(value, tag.toInt(), Pair(start, lastConsumedBytePosition))
            }
            7 -> {
                when(count) {
                    20 -> OPFalse(start)
                    21 -> OPTrue(start)
                    22,23 -> OPNull(start) // we conflate undefined and null here
                    25 -> {
                        val fp16bytes = readBytes(bytes, 2)
                        OPReal(Float.fromFP16Bytes(fp16bytes, ByteOrder.BIG).toDouble(), Pair(start, lastConsumedBytePosition))
                    }
                    26 -> OPReal(readFloat(bytes).toDouble(), Pair(start, lastConsumedBytePosition))
                    27 -> OPReal(readDouble(bytes), Pair(start, lastConsumedBytePosition))
                    31 -> CborEndMarker(start)
                    else -> throw Exception("unknown special type 7 count $count")
                }
            }
            else -> throw Exception("Unsupported type $wireType count $count")
        }
    }

    private fun readCount(bytes: ByteArray, shortCount: Int): ULong {
        check(shortCount !in 28..30) { "short count 28-30 illegal" }
        return when(shortCount) {
            in 0..23 -> shortCount.toULong()
            24 -> {
                parseOffset += 1
                bytes[parseOffset-1].toUByte().toULong()
            }
            25 -> readUInt(bytes, 2, ByteOrder.BIG).toULong()
            26 -> readUInt(bytes, 4, ByteOrder.BIG).toULong()
            27 -> readULong(bytes, 8, ByteOrder.BIG)
            31 -> ULong.MAX_VALUE
            else -> throw Exception("short count 28-30 illegal")
        }
    }
}