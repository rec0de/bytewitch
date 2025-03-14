package decoders

import Logger
import ParseCompanion
import bitmage.*

class BPList16 : ParseCompanion() {

    private val objectMap = mutableMapOf<Int, BPListObject>()
    private var sourceOffset = 0

    companion object : ByteWitchDecoder {
        override val name = "bp16"

        override fun decodesAsValid(data: ByteArray): Boolean {
            if (data.size < 8) {
                return false
            }
            return data.sliceArray(0 until 8).decodeToString() == "bplist16"
        }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return BPList16().parse(data, sourceOffset)
        }
    }

    fun parse(bytes: ByteArray, sourceOffset: Int): BPListObject {
        val magic = readBytes(bytes, 8)

        if(magic.decodeToString() != "bplist16")
            throw Exception("not a bplist16")

        val rootObject = parseCodable(bytes, sourceOffset + 8)
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
        return readObjectFromOffset(bytes, sourceOffset)
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


        val parsed = when(objectByte) {
            0x00 -> BPNull
            0xC0 -> BPFalse
            0xB0 -> BPTrue
            0x0f -> BPFill
            // Int
            in 0x10 until 0x20 -> {
                val byteLen = lengthBits

                // some bplists contain crazy long integers for tiny numbers
                // we'll just hope they're never used beyond actual long range
                if(byteLen in 9..16) {

                    println("parsing int too damn long")

                    val upper = Long.fromBytes(bytes.sliceArray(offset+1 until (offset+1+byteLen-8)), ByteOrder.BIG)
                    val lower = Long.fromBytes(bytes.sliceArray((offset+1+byteLen-8) until (offset+1+byteLen)), ByteOrder.BIG)

                    if(upper != 0L) {
                        Logger.log("Encountered very long BPInt ($byteLen B) that cannot be equivalently represented as Long")
                        throw Exception("Overlong BPInt")
                    }
                    // Bug here that I'm too lazy to fix: We're potentially interpreting unsigned data as signed here
                    BPInt(lower, Pair(offset, offset+1+byteLen))
                }
                else {

                    println("parsing int result from " + (offset+1) + " to " + (offset+1+byteLen) + " : " + Long.fromBytes(bytes.sliceArray(offset+1 until offset+1+byteLen), ByteOrder.BIG))

                    // TODO: does this mess with signs? how does bigint do it?
                    BPInt(Long.fromBytes(bytes.sliceArray(offset+1 until offset+1+byteLen), ByteOrder.BIG), Pair(offset, offset+1+byteLen))
                }
            }
            // Real float
            in 0x22 until 0x23 -> {
                BPReal(bytes.sliceArray(offset+1 until offset+1+4).readFloat(ByteOrder.BIG).toDouble(), Pair(offset, offset+1+4))
            }
            // Real double
            in 0x23 until 0x24 -> {
                BPReal(bytes.sliceArray(offset+1 until offset+1+8).readDouble(ByteOrder.BIG), Pair(offset, offset+1+8))
            }
            // Date, always 8 bytes long
            0x33 -> {
                val timestamp = bytes.sliceArray(offset+1 until offset+1+8).readDouble(ByteOrder.BIG)
                BPDate(timestamp, Pair(offset, offset+1+8))
            }
            // Data
            in 0x40 until 0x50 -> {
                // length bits encode byte count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val byteLen = tmp.first
                val effectiveOffset = tmp.second

                println("parsing BPData of byteLen $byteLen with offset $effectiveOffset")

                val data = bytes.sliceArray(effectiveOffset until effectiveOffset+byteLen)
                BPData(data, Pair(offset, effectiveOffset+byteLen))
            }
            // Ascii string
            in 0x50 until 0x70 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second
                val stringBytes = bytes.sliceArray(effectiveOffset until effectiveOffset+charLen)
                val string = stringBytes.decodeToString()
                println("parsed String of type 0x${objectByte.toUByte().toString(16).uppercase().padStart(2, '0')} and len " + charLen + " at offset " + effectiveOffset + " value '" + string + "'")
                BPUnicodeString(string, Pair(offset, effectiveOffset+charLen))
            }
            // Unicode string
            in 0x70 until 0x80 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second
                val stringBytes = bytes.sliceArray(effectiveOffset until effectiveOffset+charLen)
                val string = stringBytes.decodeToString()
                println("parsed String of type 0x${objectByte.toUByte().toString(16).uppercase().padStart(2, '0')} and len " + charLen + " at offset " + effectiveOffset + " value '" + string + "'")
                BPUnicodeString(string, Pair(offset, effectiveOffset+charLen))
            }       
            in 0x80 until 0x90 -> BPUid(bytes.sliceArray(offset+1 until offset+2+lengthBits), Pair(offset, offset+2+lengthBits))
            // Array
            in 0xa0 until 0xb0 -> {
                println("parsing array")
                val endAddr = Long.fromBytes(bytes.sliceArray(offset + 1 until offset + 1 + 8), ByteOrder.LITTLE)

                println("parsing array with end addr $endAddr")

                val values = mutableListOf<BPListObject>()
                var currentOffset = offset + 1 + 8
                val arrayStartOffset = currentOffset

                while (currentOffset < endAddr) {
                    println("parsing next array entry at $currentOffset")
                    val obj = readObjectFromOffset(bytes, currentOffset)
                    values.add(obj)
                    // if no end range, we have a 1 byte object
                    currentOffset = obj.sourceByteRange?.second ?: (currentOffset + 1)
                    println("setting next array entry byte to index $currentOffset")
                }

                println("array parsed, we are at addr $currentOffset of $endAddr. obj starting at $arrayStartOffset, ends at $currentOffset")
                BPArray(values, Pair(offset, currentOffset))
            }
            // Dict
            in 0xd0 until 0xe0 -> {
                println("parsing dict")
                val endAddr = Long.fromBytes(bytes.sliceArray(offset + 1 until offset + 1 + 8), ByteOrder.LITTLE)

                println("parsing array with end addr $endAddr")

                val map = mutableMapOf<BPListObject, BPListObject>()
                var currentOffset = offset + 1 + 8

                while (currentOffset < endAddr) {
                    println("parsing next dict key at $currentOffset")
                    val key = readObjectFromOffset(bytes, currentOffset)
                    currentOffset = key.sourceByteRange?.second ?: (currentOffset + 1)
                    println("parsing next dict value at $currentOffset")
                    val value = readObjectFromOffset(bytes, currentOffset)
                    currentOffset = value.sourceByteRange?.second ?: (currentOffset + 1)
                    map[key] = value
                }

                println("dict parsed. obj starting at ${offset}, ends at $currentOffset")
                BPDict(map, Pair(offset, currentOffset))
            }
            in 0xe0 until 0xf0 -> {
                println("parsed NULL")
                BPNull
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

        val sizeFieldSize = bytes[offset+1].toInt() and 0x0f

        // Read the raw value from the byte array
        val rawSize = Int.fromBytes(
            bytes.sliceArray(offset + 2 until offset + 2 + sizeFieldSize),
            ByteOrder.LITTLE
        )

        return Pair(rawSize, offset+2+sizeFieldSize)
    }
}

