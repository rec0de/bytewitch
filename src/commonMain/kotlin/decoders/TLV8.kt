package decoders

import ByteWitch
import bitmage.fromIndex
import bitmage.hex

object TLV8 : ByteWitchDecoder {
    override val name = "tlv8"

    override fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        val tlvs = mutableListOf<Tlv8Entry>()
        var remainder = data
        var internalOffset = 0

        while(remainder.isNotEmpty()) {
            if(remainder.size < 2)
                throw Exception("Insufficient TLV8 header bytes remaining")

            val type = remainder[0].toUByte().toInt()
            val length = remainder[1].toUByte().toInt()

            if(length > remainder.size-2)
                throw Exception("Trying to read TLV8 of length $length with only ${remainder.size} bytes remaining")
            val value = remainder.sliceArray(2 until 2+length)
            remainder = remainder.fromIndex(2+length)
            tlvs.add(Tlv8Entry(type, length, value, Pair(sourceOffset+internalOffset, sourceOffset+internalOffset+2+length)))
            internalOffset += 2+length
        }

        return Tlv8Result(tlvs, Pair(sourceOffset, sourceOffset+data.size))
    }
}

class Tlv8Result(val tlvs: List<Tlv8Entry>, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${tlvs.joinToString("") { it.renderHTML() }}</div>"
    }
}

class Tlv8Entry(val type: Int, val length: Int, val value: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML(): String {
        Logger.log("TLV8 nested quick decode of ${value.hex()}")
        val parseAttempt = ByteWitch.quickDecode(value, sourceByteRange.first+2)
        val valueHTML = parseAttempt?.renderHTML() ?: "0x${value.hex()}"
        return "<div class=\"bpvalue\" $byteRangeDataTags>Type 0x${type.toString(16)} Length $length Value $valueHTML</div>"
    }
}