package decoders

import Logger
import ParseCompanion
import bitmage.*


// once again reusing Opack classes
// see https://bsonspec.org/spec.html
class BsonParser : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "BSON"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return BsonParser().parse(data, sourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            try {
                val parser = BsonParser()
                val result = parser.parse(data, 0)
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

        override fun decodesAsValid(data: ByteArray): Pair<Boolean, ByteWitchResult?> {
            if(data.size < 7 || data.readInt(ByteOrder.LITTLE) != data.size)
                return Pair(false, null)

            try {
                val parse = BsonParser().parse(data, 0)
                return Pair(true, parse)
            } catch (e: Exception) {
                return Pair(false, null)
            }
        }
    }

    private var sourceOffset = 0

    private val lastConsumedBytePosition: Int
        get() = sourceOffset + parseOffset

    fun parse(bytes: ByteArray, sourceOffsetParam: Int): OpackObject {
        parseOffset = 0
        sourceOffset = sourceOffsetParam

        val result = parseDocument(bytes)

        check(parseOffset >= bytes.size){ "input data not fully consumed" }

        return result
    }

    private fun parseDocument(bytes: ByteArray): OpackObject {
        check(parseOffset < bytes.size)
        val start = parseOffset + sourceOffset
        val length = readUInt(bytes, 4, ByteOrder.LITTLE)
        val map = mutableMapOf<OpackObject, OpackObject>()

        while(bytes[parseOffset] != (0).toByte()) {
            val typeByte = bytes[parseOffset].toUByte().toUInt()
            parseOffset += 1

            val key = readCString(bytes)
            val valueStart = sourceOffset+parseOffset

            val value = when(typeByte.toInt()) {
                1 -> OPReal(readDouble(bytes, ByteOrder.LITTLE), Pair(valueStart, lastConsumedBytePosition))
                2, 13, 14 -> OPString(readLengthPrefixedString(bytes, 4, ByteOrder.LITTLE) ?: "", Pair(valueStart, lastConsumedBytePosition))
                3, 4 -> parseDocument(bytes)
                5 -> {
                    val size = readInt(bytes, 4, explicitlySigned = false, ByteOrder.LITTLE)
                    val type = readInt(bytes, 1, explicitlySigned = false, byteOrder = ByteOrder.LITTLE)
                    val payload = readBytes(bytes, size)
                    OPTaggedData(payload, type, Pair(valueStart, lastConsumedBytePosition))
                }
                6 -> OPUndefined(parseOffset)
                7 -> OPData(readBytes(bytes, 12), Pair(valueStart, lastConsumedBytePosition))
                8 -> {
                    val boolean = bytes[parseOffset]
                    parseOffset += 1
                    if(boolean == (0).toByte()) OPFalse(parseOffset-1) else OPTrue(parseOffset-1)
                }
                10 -> OPNull(parseOffset)
                11 -> OPArray(listOf(readCString(bytes), readCString(bytes)), Pair(valueStart, lastConsumedBytePosition))
                16 -> OPInt(readInt(bytes, 4, explicitlySigned = true, byteOrder = ByteOrder.LITTLE), Pair(valueStart, lastConsumedBytePosition))
                17, 9 -> OPInt(readLong(bytes, 8, byteOrder = ByteOrder.LITTLE), Pair(valueStart, lastConsumedBytePosition))
                18 -> OPInt(readULong(bytes, 8, byteOrder = ByteOrder.LITTLE).toLong(), Pair(valueStart, lastConsumedBytePosition))
                19 -> OPData(readBytes(bytes, 16), Pair(valueStart, lastConsumedBytePosition)) // 128bit floating point
                else -> throw Exception("Unsupported type $typeByte")
            }

            map[key] = value
        }


        val trailer = readUInt(bytes, 1, ByteOrder.LITTLE)
        check(trailer == 0u) { "invalid trailer $trailer" }
        check(length.toInt() == lastConsumedBytePosition - start) { "incorrect length field: $length, should be ${lastConsumedBytePosition - start}" }

        return OPDict(map, Pair(start, lastConsumedBytePosition))
    }

    private fun readCString(bytes: ByteArray): OPString {
        val start = parseOffset
        val end = bytes.fromIndex(start).indexOf(0)
        val string = bytes.sliceArray(parseOffset..<parseOffset+end).decodeToString()
        parseOffset += end + 1
        return OPString(string, Pair(start+sourceOffset, lastConsumedBytePosition))
    }
}