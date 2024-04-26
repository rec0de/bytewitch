package decoders

import bitmage.hex

interface ByteWitchResult {

    fun renderHTML(): String

    val sourceByteRange: Pair<Int,Int>?
        get() = null

    val byteRangeDataTags: String
        get() = if(sourceByteRange == null || sourceByteRange!!.first < 0) "" else "data-start=\"${sourceByteRange!!.first}\" data-end=\"${sourceByteRange!!.second}\""
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