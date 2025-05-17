package decoders


import bitmage.*

class BPList17 {
    private val objectMap = mutableMapOf<Int, BPListObject>()
    private var sourceOffset = 0
    private var lastObjectEndOffset = 0

    companion object : ByteWitchDecoder {
        override val name = "bplist17"

        override fun decodesAsValid(data: ByteArray): Pair<Boolean, ByteWitchResult?> {
            if (data.size < 8) {
                return Pair(false, null)
            }
            val headerString = data.sliceArray(0 until 8).decodeToString()
            return Pair(headerString == "bplist17" || headerString == "bplist16", null)
        }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return BPList17().parse(data, sourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            val headerPos = data.indexOfSubsequence("bplist17".encodeToByteArray())

            return if(headerPos >= 0) {
                val parser = BPList17()
                val prefix = data.untilIndex(headerPos)
                val parse = parser.parse(data.fromIndex(headerPos), headerPos)
                val endPos = parse.rootByteRange!!.second
                PartialDecode(prefix, parse, data.fromIndex(endPos), Pair(0, data.size))
            }
            else
                null

        }
    }

    fun parse(bytes: ByteArray, sourceOffset: Int): BPListObject {
        val magic = bytes.untilIndex(8)

        if(magic.decodeToString() !in listOf("bplist16", "bplist17"))
            throw Exception("not a bplist 16/17")

        val rootObject = parseCodable(bytes, sourceOffset)
        rootObject.rootByteRange = Pair(sourceOffset, sourceOffset+lastObjectEndOffset)

        return if(KeyedArchiveDecoder.isKeyedArchive(rootObject)) {
            val archive = KeyedArchiveDecoder.decode(rootObject as BPDict)
            archive.rootByteRange = Pair(sourceOffset, sourceOffset+lastObjectEndOffset)
            archive
        }
        else
            rootObject
    }

    private fun parseCodable(bytes: ByteArray, sourceOffset: Int): BPListObject {
        this.sourceOffset = sourceOffset
        objectMap.clear()
        return readObjectFromOffset(bytes, 8) // first object starts after header
    }

    private fun readObjectFromOffset(bytes: ByteArray, offset: Int): BPListObject {
        // check cache
        if(objectMap.containsKey(offset))
            return objectMap[offset]!!

        // objects start with a one byte type descriptor
        val objectByte = bytes[offset].toUByte().toInt()
        // for some objects, the lower four bits carry length info
        val lengthBits = objectByte and 0x0f

        println("parsing object of type: 0x${objectByte.toUByte().toString(16).uppercase().padStart(2, '0')} and length: ${lengthBits} at $offset")

        lastObjectEndOffset = offset+1

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
                    val upper = Long.fromBytes(bytes.sliceArray(offset+1 until (offset+1+byteLen-8)), ByteOrder.LITTLE)
                    val lower = Long.fromBytes(bytes.sliceArray((offset+1+byteLen-8) until (offset+1+byteLen)), ByteOrder.LITTLE)

                    if(upper != 0L) {
                        Logger.log("Encountered very long BPInt ($byteLen B) that cannot be equivalently represented as Long")
                        throw Exception("Overlong BPInt")
                    }
                    // Bug here that I'm too lazy to fix: We're potentially interpreting unsigned data as signed here
                    lastObjectEndOffset = offset+1+byteLen
                    BPInt(lower, Pair(sourceOffset+offset, lastObjectEndOffset))
                }
                else {
                    // TODO: does this mess with signs? how does bigint do it?
                    lastObjectEndOffset = offset+1+byteLen
                    BPInt(Long.fromBytes(bytes.sliceArray(offset+1 until lastObjectEndOffset), ByteOrder.LITTLE), Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
                }
            }
            // Real float
            0x22 -> {
                lastObjectEndOffset = offset+1+4
                BPReal(bytes.sliceArray(offset+1 until lastObjectEndOffset).readFloat(ByteOrder.LITTLE).toDouble(), Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // Real double
            0x23 -> {
                lastObjectEndOffset = offset+1+8
                BPReal(bytes.sliceArray(offset+1 until lastObjectEndOffset).readDouble(ByteOrder.LITTLE), Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // Date, always 8 bytes long
            0x33 -> {
                val timestamp = bytes.sliceArray(offset+1 until offset+1+8).readDouble(ByteOrder.LITTLE) // don't have evidence of this being LE but guessing it should match the others
                lastObjectEndOffset = offset+1+8
                BPDate(timestamp, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // Data
            in 0x40 until 0x50 -> {
                // length bits encode byte count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val byteLen = tmp.first
                val effectiveOffset = tmp.second

                lastObjectEndOffset = effectiveOffset+byteLen
                val data = bytes.sliceArray(effectiveOffset until lastObjectEndOffset)
                return BPData(data, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // Unicode string
            in 0x60 until 0x70 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second
                // this is UTF16, encodes at two bytes per char
                lastObjectEndOffset = effectiveOffset+charLen*2
                val stringBytes = bytes.sliceArray(effectiveOffset until lastObjectEndOffset)
                val string = stringBytes.decodeAsUTF16LE()
                BPUnicodeString(string, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // Ascii string
            in 0x70 until 0x80 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second
                lastObjectEndOffset = effectiveOffset+charLen
                val stringBytes = bytes.sliceArray(effectiveOffset until effectiveOffset+charLen)
                val string = stringBytes.decodeToString()
                BPUnicodeString(string, Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
            }
            // reference object
            in 0x80 until 0x90 -> {
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val sizeFieldLength = tmp.first
                val effectiveOffset = tmp.second
                val objAddr = Int.fromBytes(bytes.sliceArray(effectiveOffset until effectiveOffset + sizeFieldLength), ByteOrder.LITTLE)

                lastObjectEndOffset = effectiveOffset+sizeFieldLength
                readObjectFromOffset(bytes, objAddr)
            }
            // Array
            in 0xa0 until 0xb0 -> {
                val endAddr = Long.fromBytes(bytes.sliceArray(offset+1 until offset+1+8), ByteOrder.LITTLE)

                val values = mutableListOf<BPListObject>()
                var currentOffset = offset + 1 + 8

                while (currentOffset < endAddr) {
                    val obj = readObjectFromOffset(bytes, currentOffset)
                    values.add(obj)
                    // if no end range, we have a 1 byte object
                    currentOffset = lastObjectEndOffset
                }

                lastObjectEndOffset = currentOffset
                BPArray(values, Pair(sourceOffset+offset, sourceOffset+currentOffset))
            }
            // Set (this is not based on any documentation, but we assume it to work like an array, if it exists)
            in 0xc0 until 0xd0 -> {
                val endAddr = Long.fromBytes(bytes.sliceArray(offset+1 until offset+1+8), ByteOrder.LITTLE)

                val values = mutableListOf<BPListObject>()
                var currentOffset = offset + 1 + 8

                while (currentOffset < endAddr) {
                    val obj = readObjectFromOffset(bytes, currentOffset)
                    values.add(obj)
                    // if no end range, we have a 1 byte object
                    currentOffset = lastObjectEndOffset
                }

                lastObjectEndOffset = currentOffset

                BPSet(values.size, values, Pair(sourceOffset+offset, sourceOffset+currentOffset))
            }
            // Dict
            in 0xd0 until 0xe0 -> {
                val endAddr = Long.fromBytes(bytes.sliceArray(offset + 1 until offset + 1 + 8), ByteOrder.LITTLE)

                val map = mutableMapOf<BPListObject, BPListObject>()
                var currentOffset = offset + 1 + 8

                while (currentOffset < endAddr) {
                    val key = readObjectFromOffset(bytes, currentOffset)
                    currentOffset = lastObjectEndOffset
                    val value = readObjectFromOffset(bytes, currentOffset)
                    currentOffset = lastObjectEndOffset
                    map[key] = value
                }

                lastObjectEndOffset = currentOffset
                BPDict(map, Pair(sourceOffset+offset, sourceOffset+currentOffset))
            }
            // UInt
            in 0xf0 until 0x100 -> {
                val byteLen = lengthBits

                // some bplists contain crazy long integers for tiny numbers
                // we'll just hope they're never used beyond actual long range
                if(byteLen in 9..16) {
                    val upper = ULong.fromBytes(bytes.sliceArray(offset+1 until (offset+1+byteLen-8)), ByteOrder.BIG)
                    val lower = ULong.fromBytes(bytes.sliceArray((offset+1+byteLen-8) until (offset+1+byteLen)), ByteOrder.BIG)

                    if(upper != 0uL) {
                        Logger.log("Encountered very long BPUInt ($byteLen B) that cannot be equivalently represented as ULong")
                        throw Exception("Overlong BPUInt")
                    }
                    // Bug here that I'm too lazy to fix: We're potentially interpreting unsigned data as signed here
                    lastObjectEndOffset = offset+1+byteLen
                    BPUInt(lower, Pair(sourceOffset+offset, lastObjectEndOffset))
                }
                else {
                    // TODO: does this mess with signs? how does bigint do it?
                    lastObjectEndOffset = offset+1+byteLen
                    BPUInt(ULong.fromBytes(bytes.sliceArray(offset+1 until lastObjectEndOffset), ByteOrder.BIG), Pair(sourceOffset+offset, sourceOffset+lastObjectEndOffset))
                }
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

        val sizeFieldSize = bytes[offset+1].toInt() and 0x0f // size field is n bytes

        // Read the raw value from the byte array
        val rawSize = Int.fromBytes(
            bytes.sliceArray(offset + 2 until offset + 2 + sizeFieldSize),
            ByteOrder.LITTLE
        )
        return Pair(rawSize, offset+2+sizeFieldSize)
    }
}