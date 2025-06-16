package decoders

import bitmage.hex

interface ByteWitchResult {

    fun renderHTML(): String

    fun rangeTagsFor(start: Number, end: Number) = "data-start=\"$start\" data-end=\"$end\""

    val sourceByteRange: Pair<Number,Number>?

    val byteRangeDataTags: String
        get() = if(sourceByteRange == null || sourceByteRange!!.first.toFloat() < 0) "" else rangeTagsFor(sourceByteRange!!.first, sourceByteRange!!.second)
}

class PartialDecode(val prefix: ByteArray, val result: ByteWitchResult, val suffix: ByteArray, override val sourceByteRange: Pair<Number, Number>): ByteWitchResult {
    override fun renderHTML(): String {
        val pre = if(prefix.isNotEmpty())
            "<div class=\"bpvalue data\" data-start=\"${sourceByteRange.first}\" data-end=\"${sourceByteRange.first.toFloat() + prefix.size}\">0x${prefix.hex()}</div>"
        else ""

        val post = if(suffix.isNotEmpty())
            "<div class=\"bpvalue data\" data-start=\"${sourceByteRange.second.toFloat() - suffix.size}\" data-end=\"${sourceByteRange.second}\">0x${suffix.hex()}</div>"
        else ""

        return "<div class=\"roundbox generic largecollection\" $byteRangeDataTags>$pre ${result.renderHTML()} $post</div>"
    }
}