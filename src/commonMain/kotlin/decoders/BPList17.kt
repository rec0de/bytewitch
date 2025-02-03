package decoders

import ParseCompanion
import bitmage.fromIndex
import bitmage.hex

class BPList17 : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "bp17"

        override fun decodesAsValid(data: ByteArray): Boolean {
            return data.sliceArray(0 until 8).decodeToString() == "bplist17"
        }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return BPList17().decode(data, sourceOffset)
        }
    }

    fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {

        parseOffset = 0
        val magic = readBytes(data, 6)

        if(magic.decodeToString() != "bplist")
            throw Exception("not a bplist")

        val version = readBytes(data, 2)

        if(version.decodeToString() != "17")
            throw Exception("not version 17")

        val rest = data.fromIndex(8)

        val result = BP17Result(rest)
        return result
    }
}

class BP17Result(val data: ByteArray) : ByteWitchResult {
    override val sourceByteRange: Pair<Int, Int>? = null
    override fun renderHTML(): String {
        return data.hex()
    }
}