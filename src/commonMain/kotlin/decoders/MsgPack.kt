package decoders

import Logger
import ParseCompanion
import bitmage.*


// MsgPack is basically a flavor of OPACK, so we'll reuse OPACK classes
// based on https://github.com/msgpack/msgpack/blob/master/spec.md
class MsgPackParser : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "MsgPack"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            // remove prefix 4d500305 (apple MsgPack header)
            val effectiveData: ByteArray
            val effectiveSourceOffset: Int
            if(data.untilIndex(4).contentEquals("4d500305".fromHex())) {
                effectiveData = data.fromIndex(4)
                effectiveSourceOffset = sourceOffset + 4
            }
            else {
                effectiveData = data
                effectiveSourceOffset = sourceOffset
            }

            return MsgPackParser().parseTopLevel(effectiveData, effectiveSourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            try {
                val parser = MsgPackParser()
                val result = parser.parse(data)
                val remainder = data.fromIndex(parser.parseOffset)

                // parsed MsgPack should represent at least 30% of input data
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
        override fun decodesAsValid(data: ByteArray): Pair<Boolean, ByteWitchResult?> {
            if(data.size < 3)
                return Pair(false, null)
            return super.decodesAsValid(data)
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
        val typeByte = bytes[parseOffset].toUByte().toUInt()
        //Logger.log("parsing type byte: 0x${typeByte.toString(16)}")
        return when(typeByte) {
            in 0x00u..0x7fu -> parseAsInt(bytes)
            in 0x80u..0x8fu -> parseAsDict(bytes)
            in 0x90u..0x9fu -> parseAsArray(bytes)
            in 0xa0u..0xbfu -> parseAsString(bytes)
            0xc0u -> parseAsNull(bytes)
            0xc2u, 0xc3u -> parseAsBool(bytes)
            in 0xc4u..0xc6u -> parseAsData(bytes)
            in 0xc7u..0xc9u -> parseAsExtension(bytes)
            0xcau, 0xcbu -> parseAsFloat(bytes)
            in 0xccu..0xd3u -> parseAsInt(bytes)
            in 0xd4u..0xd8u -> parseAsExtension(bytes)
            in 0xd9u..0xdbu -> parseAsString(bytes)
            0xdcu, 0xddu -> parseAsArray(bytes)
            0xdeu, 0xdfu -> parseAsDict(bytes)
            in 0xe0u..0xffu -> parseAsInt(bytes)
            else -> throw Exception("Unsupported type 0x${typeByte.toString(16)}")
        }
    }

    private fun parseAsBool(bytes: ByteArray): OpackObject {
        val byte = readInt(bytes, 1)
        return when (byte) {
            0xc3 -> OPTrue(sourceOffset+parseOffset-1) // we already incremented parse offset here
            0xc2 -> OPFalse(sourceOffset+parseOffset-1)
            else -> throw Exception("Unexpected OPACK boolean ${bytes.hex()}")
        }
    }

    private fun parseAsNull(bytes: ByteArray): OpackObject {
        parseOffset += 1
        return OPNull(sourceOffset+parseOffset-1) // we already incremented parse offset here
    }

    private fun parseAsInt(bytes: ByteArray): OpackObject {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        
        return when(type) {
            in 0x00..0x7f -> OPInt(type, Pair(start, lastConsumedBytePosition))
            in 0xe0..0xff -> OPInt(Int.fromBytes(byteArrayOf(bytes[parseOffset-1]), ByteOrder.BIG, explicitlySigned = true), Pair(start, lastConsumedBytePosition))
            // uints
            0xcc -> OPUInt(readUInt(bytes, 1), Pair(start, lastConsumedBytePosition))
            0xcd -> OPUInt(readUInt(bytes, 2), Pair(start, lastConsumedBytePosition))
            0xce -> OPUInt(readUInt(bytes, 4), Pair(start, lastConsumedBytePosition))
            0xcf -> OPUInt(readULong(bytes, 8), Pair(start, lastConsumedBytePosition))
            // ints
            0xd0 -> OPInt(readInt(bytes, 1, explicitlySigned = true), Pair(start, lastConsumedBytePosition))
            0xd1 -> OPInt(readInt(bytes, 2, explicitlySigned = true), Pair(start, lastConsumedBytePosition))
            0xd2 -> OPInt(readInt(bytes, 4, explicitlySigned = true), Pair(start, lastConsumedBytePosition))
            0xd3 -> OPInt(readLong(bytes, 8), Pair(start, lastConsumedBytePosition))
            else -> throw Exception("Unexpected MsgPack int type $type in ${bytes.hex()}")
        }
    }

    private fun parseAsFloat(bytes: ByteArray): OPReal {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        return when(type) {
            0xca -> OPReal(readBytes(bytes, 4).readFloat(ByteOrder.BIG).toDouble(), Pair(start, lastConsumedBytePosition))
            0xcb -> OPReal(readBytes(bytes, 8).readDouble(ByteOrder.BIG), Pair(start, lastConsumedBytePosition))
            else -> throw Exception("Unexpected MsgPack float type $type in ${bytes.hex()}")
        }
    }

    private fun parseAsString(bytes: ByteArray): OPString {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)

        when(type) {
            in 0xa0..0xbf -> return OPString(readBytes(bytes, type - 0xa0).decodeToString(), Pair(start, lastConsumedBytePosition))
            0xd9 -> {
                val length = readInt(bytes, 1)
                return OPString(readBytes(bytes, length).decodeToString(), Pair(start, lastConsumedBytePosition))
            }
            0xda -> {
                val length = readInt(bytes, 2)
                return OPString(readBytes(bytes, length).decodeToString(), Pair(start, lastConsumedBytePosition))
            }
            0xdb -> {
                val length = readInt(bytes, 4)
                return OPString(readBytes(bytes, length).decodeToString(), Pair(start, lastConsumedBytePosition))
            }
            else -> throw Exception("Unexpected MsgPack string type $type in ${bytes.hex()}")
        }
    }

    private fun parseAsData(bytes: ByteArray): OPData {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)

        when(type) {
            0xc4 -> {
                val length = readInt(bytes, 1)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }
            0xc5 -> {
                val length = readInt(bytes, 2)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }
            0xc6 -> {
                val length = readInt(bytes, 4)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }

            else -> throw Exception("Unexpected MsgPack data type $type in ${bytes.hex()}")
        }
    }

    private fun parseAsExtension(bytes: ByteArray): OpackObject {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)

        val extType: Int
        val length: Int
        when(type) {
            0xd4 -> {
                extType = readInt(bytes, 1)
                length = 1
            }
            0xd5 -> {
                extType = readInt(bytes, 1)
                length = 2
            }
            0xd6 -> {
                extType = readInt(bytes, 1)
                length = 4
            }
            0xd7 -> {
                extType = readInt(bytes, 1)
                length = 8
            }
            0xd8 -> {
                extType = readInt(bytes, 1)
                length = 16
            }
            0xc7 -> {
                length = readInt(bytes, 1)
                extType = readInt(bytes, 1)
            }
            0xc8 -> {
                length = readInt(bytes, 2)
                extType = readInt(bytes, 1)
            }
            0xc9 -> {
                length = readInt(bytes, 4)
                extType = readInt(bytes, 1)
            }
            else -> throw Exception("Unexpected MsgPack data type $type in ${bytes.hex()}")
        }

        if(extType == 0xff) {
            // date conversion will be lossy due to double precision, but oh well
            when(length) {
                4 -> {
                    val seconds = readInt(bytes, 4)
                    return OPDate(seconds.toDouble(), isAppleEpoch = false, sourceByteRange = Pair(start, lastConsumedBytePosition))
                }
                8 -> {
                    val nano = readInt(bytes, 4)
                    val seconds = readInt(bytes, 4)
                    return OPDate(seconds.toDouble() + nano.toDouble()/1000000, isAppleEpoch = false, sourceByteRange = Pair(start, lastConsumedBytePosition))
                }
                12 -> {
                    val nano = readInt(bytes, 4)
                    val seconds = readLong(bytes, 8)
                    return OPDate(seconds.toDouble() + nano.toDouble()/1000000, isAppleEpoch = false, sourceByteRange = Pair(start, lastConsumedBytePosition))
                }
                else -> throw Exception("Unexpected data length for date extension type: $length in ${bytes.hex()}")
            }
        }
        else
            return OPTaggedData(readBytes(bytes, length), extType, Pair(start, lastConsumedBytePosition))
    }

    private fun parseAsArray(bytes: ByteArray): OPArray {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        val entries = mutableListOf<OpackObject>()

        val length = when(type) {
            in 0x90..0x9f -> type - 0x90
            0xdc -> readInt(bytes, 2)
            0xdd -> readInt(bytes, 4)
            else -> throw Exception("Unexpected MsgPack array type $type in ${bytes.hex()}")
        }

        var i = 0
        while(i < length) {
            entries.add(parse(bytes))
            i += 1
        }

        return OPArray(entries, Pair(start, lastConsumedBytePosition))
    }

    private fun parseAsDict(bytes: ByteArray): OPDict {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        val entries = mutableMapOf<OpackObject, OpackObject>()

        val length = when(type) {
            in 0x80..0x8f -> type - 0x80
            0xde -> readInt(bytes, 2)
            0xdf -> readInt(bytes, 4)
            else -> throw Exception("Unexpected MsgPack dict type $type in ${bytes.hex()}")
        }

        var i = 0
        while(i < length) {
            entries[parse(bytes)] = parse(bytes)
            i += 1
        }

        return OPDict(entries, Pair(start, lastConsumedBytePosition))
    }
}