package decoders


import bitmage.*

class BPList15 {
    private val objectMap = mutableMapOf<Int, BPListObject>()
    private var sourceOffset = 0
    private var lastObjectEndOffset = 0

    companion object : ByteWitchDecoder {
        override val name = "bplist15"

        override fun decodesAsValid(data: ByteArray): Pair<Boolean, ByteWitchResult?> {
            if (data.size < 8) {
                return Pair(false, null)
            }
            return Pair(data.sliceArray(0 until 8).decodeToString() == "bplist15", null)
        }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return BPList15().parse(data, sourceOffset)
        }
    }


    fun parse(bytes: ByteArray, sourceOffset: Int): BPListObject {
        val magic = bytes.untilIndex(8)

        if(magic.decodeToString() != "bplist15")
            throw Exception("not a bplist15")

        val rest = bytes.fromIndex(8)
        val rootObject = parseCodable(rest, sourceOffset+8)
        rootObject.rootByteRange = Pair(sourceOffset, sourceOffset+bytes.size)

        return if(KeyedArchiveDecoder.isKeyedArchive(rootObject)) {
            val archive = KeyedArchiveDecoder.decode(rootObject as BPDict)
            archive.rootByteRange = Pair(sourceOffset, sourceOffset+bytes.size)
            archive
        }
        else
            rootObject
    }

    fun parseCodable(bytes: ByteArray, sourceOffset: Int): BPListObject {
        this.sourceOffset = sourceOffset
        objectMap.clear()

        // bplist after bplist15:
        // 1 byte of 0x13
        // 4 bytes (?) of little endian length field
        // unidentified bytes f.e. 0x00000080
        // 5 bytes CRC f.e. 1200000000

        // -> skip 14 bytes
        return readObjectFromOffset(bytes, 14)
    }

    private fun readObjectFromOffset(bytes: ByteArray, offset: Int): BPListObject {
        // check cache
        if(objectMap.containsKey(offset))
            return objectMap[offset]!!

        // objects start with a one byte type descriptor
        val objectByte = bytes[offset].toUByte().toInt()
        // for some objects, the lower four bits carry length info
        val lengthBits = objectByte and 0x0f

        lastObjectEndOffset = offset+1

        val parsed = when(objectByte) {
            0x00 -> BPNull
            0x08 -> BPFalse
            0x09 -> BPTrue
            0x0f -> BPFill
            // Int
            in 0x10 until 0x20 -> {
                // length bits encode int byte size as 2^n
                val byteLen = 1 shl lengthBits
                lastObjectEndOffset = offset+1+byteLen

                // some bplists contain crazy long integers for tiny numbers
                // we'll just hope they're never used beyond actual long range
                if(byteLen in 9..16) {
                    val upper = Long.fromBytes(bytes.sliceArray(offset+1 until (offset+1+byteLen-8)), ByteOrder.BIG)
                    val lower = Long.fromBytes(bytes.sliceArray((offset+1+byteLen-8) until (offset+1+byteLen)), ByteOrder.BIG)

                    if(upper != 0L) {
                        Logger.log("Encountered very long BPInt ($byteLen B) that cannot be equivalently represented as Long")
                        throw Exception("Overlong BPInt")
                    }
                    // Bug here that I'm too lazy to fix: We're potentially interpreting unsigned data as signed here
                    BPInt(lower, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
                }
                else {
                    // TODO: does this mess with signs? how does bigint do it?
                    BPInt(Long.fromBytes(bytes.sliceArray(offset+1 until offset+1+byteLen), ByteOrder.BIG), Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
                }
            }
            // Real
            in 0x20 until 0x30 -> {
                // length bits encode real byte size as 2^n
                val byteLen = 1 shl lengthBits
                val value = when(byteLen) {
                    4 -> {
                        lastObjectEndOffset = offset+1+4
                        bytes.sliceArray(offset+1 until offset+1+4).readFloat(ByteOrder.BIG).toDouble()
                    }
                    8 -> {
                        lastObjectEndOffset = offset+1+8
                        bytes.sliceArray(offset+1 until offset+1+8).readDouble(ByteOrder.BIG)
                    }
                    else -> throw Exception("Got unexpected byte length for real: $byteLen in ${bytes.hex()}")
                }
                BPReal(value, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // Date, always 8 bytes long
            0x33 -> {
                lastObjectEndOffset = offset+1+8
                val timestamp = bytes.sliceArray(offset+1 until offset+1+8).readDouble(ByteOrder.BIG)
                BPDate(timestamp, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // Data
            in 0x40 until 0x50 -> {
                // length bits encode byte count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val byteLen = tmp.first
                val effectiveOffset = tmp.second

                val data = bytes.sliceArray(effectiveOffset until effectiveOffset+byteLen)
                lastObjectEndOffset = effectiveOffset+byteLen
                BPData(data, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // ASCII string
            in 0x50 until 0x60 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second
                // ascii encodes at one char per byte, we can use default UTF8 decoding as ascii is cross compatible with everything
                val string = bytes.decodeToString(effectiveOffset, effectiveOffset+charLen)
                lastObjectEndOffset = effectiveOffset+charLen
                BPAsciiString(string, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // Unicode string
            in 0x60 until 0x70 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second
                // this is UTF16, encodes at two bytes per char
                val stringBytes = bytes.sliceArray(effectiveOffset until effectiveOffset+charLen*2)
                val string = stringBytes.decodeAsUTF16LE()
                lastObjectEndOffset = effectiveOffset+charLen*2
                BPUnicodeString(string, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // UTF8 string
            in 0x70 until 0x80 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second

                val stringBytes = bytes.sliceArray(effectiveOffset until effectiveOffset+charLen)
                val string = stringBytes.decodeToString()
                lastObjectEndOffset = effectiveOffset+charLen
                BPUnicodeString(string, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // UID, byte length is lengthBits+1
            in 0x80 until 0x90 -> BPUid(bytes.sliceArray(offset+1 until offset+2+lengthBits), Pair(sourceOffset+offset, sourceOffset+offset+2+lengthBits))
            // Array
            in 0xa0 until 0xb0 -> {
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val entries = tmp.first
                val effectiveOffset = tmp.second

                val values = mutableListOf<BPListObject>()
                var currentOffset = effectiveOffset

                for (i in 0 until entries) {
                    val obj = readObjectFromOffset(bytes, currentOffset)
                    values.add(obj)
                    currentOffset = lastObjectEndOffset
                }

                lastObjectEndOffset = currentOffset
                BPArray(values, Pair(sourceOffset + offset, sourceOffset+currentOffset))
            }
            // Set
            in 0xc0 until 0xd0 -> {
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val entries = tmp.first
                val effectiveOffset = tmp.second

                val values = mutableListOf<BPListObject>()
                var currentOffset = effectiveOffset

                for (i in 0 until entries) {
                    val obj = readObjectFromOffset(bytes, currentOffset)
                    values.add(obj)
                    currentOffset = lastObjectEndOffset
                }

                lastObjectEndOffset = currentOffset
                BPSet(values.size, values, Pair(sourceOffset+offset, sourceOffset+currentOffset))
            }
            // Dict
            in 0xd0 until 0xf0 -> {
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val entries = tmp.first
                val effectiveOffset = tmp.second

                val map = mutableMapOf<BPListObject, BPListObject>()
                var currentOffset = effectiveOffset

                for (i in 0 until entries) {
                    val key = readObjectFromOffset(bytes, currentOffset)
                    currentOffset = lastObjectEndOffset

                    val value = readObjectFromOffset(bytes, currentOffset)
                    currentOffset = lastObjectEndOffset

                    map[key] = value
                }

                lastObjectEndOffset = currentOffset
                BPDict(map, Pair(sourceOffset + offset, sourceOffset+currentOffset))
            }
            else -> throw Exception("Unknown object type byte 0b${objectByte.toString(2)}")
        }

        objectMap[offset] = parsed
        return parsed
    }


    private fun getFillAwareLengthAndOffset(bytes: ByteArray, offset: Int): Pair<Int, Int> {
        val lengthBits = bytes[offset].toInt() and 0x0f
        if(lengthBits < 0x0f)
            return Pair(lengthBits, offset+1)

        val sizeFieldSize = 1 shl (bytes[offset+1].toInt() and 0x0f) // size field is 2^n bytes

        val rawSize = ULong.fromBytes(
            bytes.sliceArray(offset + 2 until offset + 2 + sizeFieldSize),
            ByteOrder.LITTLE
        )

        // Create a mask that clears the first bit.
        // For example, if sizeFieldSize == 1, then (1UL shl (8 - 1)) - 1 == (1UL shl 7) - 1 == 127 (0b0111_1111)
        val mask = (1UL shl (sizeFieldSize * 8 - 1)) - 1UL

        // Apply the mask to force the first bit to 0, then convert to Int.
        val size = (rawSize and mask).toInt()


        return Pair(size, offset+2+sizeFieldSize)
    }
}

