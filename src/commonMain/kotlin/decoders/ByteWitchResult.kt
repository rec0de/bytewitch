package decoders

import bitmage.hex

interface ByteWitchResult {

    fun renderHTML(): String

    fun rangeTagsFor(start: Int, end: Int) = "data-start=\"$start\" data-end=\"$end\""

    val sourceBitRange: Pair<Int,Int>?

    val bitRangeDataTags: String
        get() = if(sourceBitRange == null || sourceBitRange!!.first < 0) "" else rangeTagsFor(sourceBitRange!!.first, sourceBitRange!!.second)
}

class PartialDecode(val prefix: ByteArray, val result: ByteWitchResult, val suffix: ByteArray, override val sourceBitRange: Pair<Int, Int>): ByteWitchResult {
    override fun renderHTML(): String {
        val pre = if(prefix.isNotEmpty())
            "<div class=\"bpvalue data\" data-start=\"${sourceBitRange.first}\" data-end=\"${sourceBitRange.first + prefix.size}\">0x${prefix.hex()}</div>"
        else ""

        val post = if(suffix.isNotEmpty())
            "<div class=\"bpvalue data\" data-start=\"${sourceBitRange.second - suffix.size}\" data-end=\"${sourceBitRange.second}\">0x${suffix.hex()}</div>"
        else ""

        return "<div class=\"roundbox generic largecollection\" $bitRangeDataTags>$pre ${result.renderHTML()} $post</div>"
    }
}