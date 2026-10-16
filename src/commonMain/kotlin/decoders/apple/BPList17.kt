package decoders.apple


import ParseCompanion
import bitmage.*
import decoders.BWRangeTaggedData
import decoders.ByteWitchDecoder
import decoders.ByteWitchResult
import decoders.MultiPartialDecode

class BPList17: ParseCompanion() {
    private val objectMap = mutableMapOf<Int, BPListObject>()

    private val decodeKeyedArchives = true


    companion object : ByteWitchDecoder {
        override val name = "bplist17"

        override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
            if (data.size < 9) {
                return Pair(0.0, null)
            }
            val headerString = data.sliceArray(0 until 8).decodeToString()
            return if(headerString == "bplist17" || headerString == "bplist16") Pair(1.0, null) else Pair(0.0, null)
        }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return BPList17().parse(data, sourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            try {
                val headerPos = data.indexOfSubsequence("bplist17".encodeToByteArray())

                return if (headerPos >= 0) {
                    val parser = BPList17()
                    val prefix = data.untilIndex(headerPos)
                    val parse = parser.parse(data.fromIndex(headerPos), headerPos)
                    val endPos = parse.rootByteRange!!.second
                    val parts = listOf(
                        Pair(null, BWRangeTaggedData(prefix, 0)),
                        Pair(parse, null),
                        Pair(null, BWRangeTaggedData(data.fromIndex(endPos), endPos))
                    )
                    MultiPartialDecode(parts, Pair(0, data.size))
                } else
                    null
            }
            catch (e: Exception) {
                Logger.log(e.toString())
                return null
            }

        }
    }

    fun parse(bytes: ByteArray, sourceOffset: Int): BPListObject {
        parseOffset = 0
        objectMap.clear()
        val magic = readString(bytes, 8)

        if(magic !in listOf("bplist16", "bplist17"))
            throw Exception("not a bplist 16/17")

        val rootObject = readObject(bytes, sourceOffset)

        rootObject.rootByteRange = Pair(sourceOffset, sourceOffset+parseOffset)

        return if(decodeKeyedArchives && KeyedArchiveDecoder.isKeyedArchive(rootObject)) {
            val archive = KeyedArchiveDecoder.decode(rootObject as BPDict)
            archive.rootByteRange = Pair(sourceOffset, sourceOffset+parseOffset)
            archive
        }
        else
            rootObject
    }



    private fun readObject(bytes: ByteArray, sourceOffset: Int): BPListObject {
        val start = parseOffset + sourceOffset

        // objects start with a one byte type descriptor
        val objectByte = readInt(bytes, 1)

        // for some objects, the lower four bits carry length info
        val lengthBits = objectByte and 0x0f

        //println("parsing object of type: 0x${objectByte.toUByte().toString(16).uppercase().padStart(2, '0')} at $start ($sourceOffset)")

        val parsed = when(objectByte) {
            0x00 -> BPNull
            0xe0 -> BPNull
            0xc0 -> BPFalse
            0xb0 -> BPTrue
            0x0f -> BPFill
            // Int
            in 0x10 until 0x20 -> {
                val byteLen = lengthBits

                // some bplists contain crazy long integers for tiny numbers
                // we'll just hope they're never used beyond actual long range
                if(byteLen in 9..16) {
                    throw Exception("overlong int")
                }
                else {
                    val value = readLong(bytes, byteLen, ByteOrder.LITTLE)
                    BPInt(value, Pair(start, sourceOffset + parseOffset))
                }
            }
            // Real float
            0x22 -> {
                val value = readFloat(bytes, ByteOrder.LITTLE).toDouble()
                BPReal(value, Pair(start, sourceOffset + parseOffset))
            }
            // Real double
            0x23 -> {
                val value = readDouble(bytes, ByteOrder.LITTLE)
                BPReal(value, Pair(start, sourceOffset + parseOffset))
            }
            // Date, always 8 bytes long
            0x33 -> {
                val timestamp = readDouble(bytes, ByteOrder.LITTLE) // don't have evidence of this being LE but guessing it should match the others
                BPDate(timestamp, Pair(start, sourceOffset + parseOffset))
            }
            // Data
            in 0x40 until 0x50 -> {
                // length bits encode byte count, if all ones additional length integer follows
                val (byteLen, sizeFieldLen) = readOptionalExtendedLengthField(bytes, lengthBits)
                val data = readBytes(bytes, byteLen)
                BPData(data, Pair(start, sourceOffset + parseOffset))
            }
            // Unicode string
            in 0x60 until 0x70 -> {
                // length bits encode character count, if all ones additional length integer follows
                val (charLen, sizeFieldLen) = readOptionalExtendedLengthField(bytes, lengthBits)

                // this is UTF16, encodes at two bytes per char
                val string = readBytes(bytes, charLen*2).decodeAsUTF16LE()
                BPUnicodeString(string, Pair(start, sourceOffset + parseOffset))
            }
            // Ascii string
            in 0x70 until 0x80 -> {
                // length bits encode character count, if all ones additional length integer follows
                val (charLen, sizeFieldLen) = readOptionalExtendedLengthField(bytes, lengthBits)
                val string = readString(bytes, charLen)
                BPUnicodeString(string, Pair(start, sourceOffset + parseOffset))
            }
            // reference object
            in 0x80 until 0x90 -> {
                val (sizeFieldLength, ohGodSizeFieldSizeFieldLength) = readOptionalExtendedLengthField(bytes, lengthBits)
                val objAddr = readInt(bytes, sizeFieldLength, explicitlySigned = false, ByteOrder.LITTLE)

                if(objectMap.containsKey(objAddr))
                    return objectMap[objAddr]!! // return object directly, don't store new reference to reference
                else
                    throw Exception("undefined reference pointer to offset $objAddr")
            }
            // Array
            in 0xa0 until 0xb0 -> {
                val endAddr = readLong(bytes, 8, ByteOrder.LITTLE)

                val values = mutableListOf<BPListObject>()

                while (parseOffset <= endAddr) {
                    val obj = readObject(bytes, sourceOffset)
                    values.add(obj)
                }

                BPArray(values, Pair(start, sourceOffset + parseOffset))
            }
            // Dict
            in 0xd0 until 0xe0 -> {
                val endAddr = readLong(bytes, 8, ByteOrder.LITTLE)
                val map = mutableMapOf<BPListObject, BPListObject>()

                while (parseOffset <= endAddr) {
                    val key = readObject(bytes, sourceOffset)
                    val value = readObject(bytes, sourceOffset)
                    map[key] = value
                }

                BPDict(map, Pair(start, sourceOffset + parseOffset))
            }
            // UInt
            in 0xf0 until 0x100 -> {
                val byteLen = lengthBits

                // some bplists contain crazy long integers for tiny numbers
                // we'll just hope they're never used beyond actual long range
                if(byteLen in 9..16) {
                    throw Exception("overly long UInt")
                }
                else {
                    val value = readULong(bytes, byteLen, ByteOrder.BIG)
                    BPUInt(value,
                        Pair(start, sourceOffset + parseOffset)
                    )
                }
            }

            else -> throw Exception("Unknown object type byte 0b${objectByte.toString(2)}")
        }

        objectMap[start-sourceOffset] = parsed
        return parsed
    }


    // returns Pair(length, lengthFieldLength)
    private fun readOptionalExtendedLengthField(bytes: ByteArray, lengthBits: Int): Pair<Int, Int> {
        if(lengthBits < 0x0f)
            return Pair(lengthBits, 0)

        val sizeFieldSize = readInt(bytes, 1) and 0x0f // size field is n bytes

        // Read the raw value from the byte array
        val rawSize = readInt(bytes, sizeFieldSize, explicitlySigned = false, ByteOrder.LITTLE)
        return Pair(rawSize, sizeFieldSize)
    }
}