package decoders

import ParseCompanion
import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.hex

object DMAP : ByteWitchDecoder, ParseCompanion() {
    override val name = "DMAP"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        val tlvs = readTLVs(data, sourceOffset)
        return DmapResult(tlvs, Pair(sourceOffset, sourceOffset+data.size))
    }

    private fun readTLVs(data: ByteArray, sourceOffset: Int): List<DmapTlv> {
        val payloads = mutableListOf<DmapTlv>()
        var start: Int

        while(parseOffset < data.size) {
            start = parseOffset + sourceOffset
            val key = readBytes(data, 4)
            val length = readUInt(data, 4, ByteOrder.BIG)

            check(key.all { it.toInt().toChar().isLetter() }) { "DMAP key should be ASCII letters, was ${key.hex()}" }
            check(length <= (data.size - parseOffset).toUInt()) { "DMAP TLV length longer than remaining data: $length"}

            val value = readBytes(data, length.toInt())
            payloads.add(DmapTlv(key.decodeToString(), length.toInt(), value, Pair(start, parseOffset+sourceOffset)))
        }

        return payloads
    }
}


class DmapResult(val tlvs: List<DmapTlv>, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        return if(tlvs.size == 1)
            tlvs.first().renderHTML()
        else
            "<div class=\"generic roundbox\" $byteRangeDataTags>${tlvs.joinToString("") { "<div class=\"bwvalue\">${it.renderHTML()}</div>" }}</div>"
    }
}

data class DmapTlv(
    val key: String,
    val length: Int,
    val value: ByteArray,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val decode = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)

        // reduce unnecessary visual nesting
        val payloadHTML = if(decode is DmapResult)
                "<div class=\"bwvalue flexy\">${decode.tlvs.joinToString(" ") { it.renderHTML() }}</div>"
            else
                wrapIfDifferentColour(decode, value, relativeRangeTags(8, length))

        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\" ${relativeRangeTags(0, 4)}>$key</div><div class=\"bpvalue\" ${relativeRangeTags(4, 4)}>Len: $length B</div>$payloadHTML</div>"
    }

}