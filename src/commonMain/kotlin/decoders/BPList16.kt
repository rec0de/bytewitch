package decoders

import ParseCompanion
import bitmage.fromIndex
import bitmage.hex

class BPList16 : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "bp16"

        override fun decodesAsValid(data: ByteArray): Boolean {
            return data.sliceArray(0 until 8).decodeToString() == "bplist16"
        }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return BPList16().decode(data, sourceOffset)
        }
    }

    fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {

        parseOffset = 0
        val magic = readBytes(data, 6)

        if(magic.decodeToString() != "bplist")
            throw Exception("not a bplist")

        val version = readBytes(data, 2)

        if(version.decodeToString() != "16")
            throw Exception("not version 16")

        val rest = data.fromIndex(8)

        val result = BP16Result(rest)
        return result
    }
}

class BP16Result(val data: ByteArray) : ByteWitchResult {
    override val sourceByteRange: Pair<Int, Int>? = null
    override fun renderHTML(): String {
        return data.hex()
    }
}