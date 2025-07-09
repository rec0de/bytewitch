package decoders

import bitmage.hex

interface ByteWitchResult {

    fun renderHTML(): String

    fun rangeTagsFor(start: Int, end: Int) = "data-start=\"$start\" data-end=\"$end\""
    fun relativeRangeTags(start: Int, length: Int) : String {
        check(sourceByteRange != null) { "attempting use of relativeRangeTagsFor with null sourceByteRange" }
        return rangeTagsFor(sourceByteRange!!.first+start, sourceByteRange!!.first+start+length)
    }

    fun bitOffsetTagsFor(startOffset: Int, endOffset: Int) : String {
        return "data-start-bit-offset=\"${sourceRangeBitOffset.first}\" data-end-bit-offset=\"${sourceRangeBitOffset.second}\""
    }

    val sourceByteRange: Pair<Int,Int>?
    val sourceRangeBitOffset: Pair<Int,Int>
        get() = Pair(0, 0) // default implementation, can be overridden

    val byteRangeDataTags: String
        get() = if (sourceByteRange == null || sourceByteRange!!.first < 0) "" else (
                rangeTagsFor(sourceByteRange!!.first, sourceByteRange!!.second) + " "
                        + bitOffsetTagsFor(sourceRangeBitOffset.first, sourceRangeBitOffset.second))
}

class PartialDecode(val prefix: ByteArray, val result: ByteWitchResult, val suffix: ByteArray, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override fun renderHTML(): String {
        val pre = if(prefix.isNotEmpty())
            "<div class=\"bpvalue data\" data-start=\"${sourceByteRange.first}\" data-end=\"${sourceByteRange.first + prefix.size}\">0x${prefix.hex()}</div>"
        else ""

        val post = if(suffix.isNotEmpty())
            "<div class=\"bpvalue data\" data-start=\"${sourceByteRange.second - suffix.size}\" data-end=\"${sourceByteRange.second}\">0x${suffix.hex()}</div>"
        else ""

        return "<div class=\"roundbox generic largecollection\" $byteRangeDataTags>$pre ${result.renderHTML()} $post</div>"
    }
}