package decoders

import bitmage.hex
import htmlEscape

interface ByteWitchResult {

    enum class Colour {
        BPLIST, PROTOBUF, OPACK, NSARCHIVE, ASN1, GENERIC, NEUTRAL, PLAIN
    }

    val colour: ByteWitchResult.Colour

    fun renderHTML(): String

    fun wrapIfDifferentColour(subresult: ByteWitchResult?, data: ByteArray, rangeTags: String) = wrapIfDifferentColour(subresult, "0x${data.hex()}", rangeTags)

    fun wrapIfDifferentColour(subresult: ByteWitchResult?, fallback: String, rangeTags: String): String {
        return if(subresult != null && (subresult.colour == Colour.PLAIN || subresult.colour != colour))
            subresult.renderHTML()
        else {
            "<div class=\"bwvalue data\" $rangeTags>${subresult?.renderHTML() ?: fallback}</div>"
        }
    }

    fun rangeTagsFor(start: Int, end: Int) = "data-start=\"$start\" data-end=\"$end\""
    fun relativeRangeTags(start: Int, length: Int) : String {
        check(sourceByteRange != null) { "attempting use of relativeRangeTagsFor with null sourceByteRange" }
        return rangeTagsFor(sourceByteRange!!.first+start, sourceByteRange!!.first+start+length)
    }

    val sourceByteRange: Pair<Int,Int>?

    val byteRangeDataTags: String
        get() = if(sourceByteRange == null || sourceByteRange!!.first < 0) "" else rangeTagsFor(sourceByteRange!!.first, sourceByteRange!!.second)
}

class PartialDecode(val prefix: ByteArray, val result: ByteWitchResult, val suffix: ByteArray, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

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

class BWStringCollection(val elements: List<BWString>, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = elements.joinToString(" ") { it.renderHTML() }
}

open class BWString(val string: String, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = "<div class=\"bpvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)}</div>"
}

class BWLinkedString(string: String, private val url: String, sourceByteRange: Pair<Int, Int>): BWString(string, sourceByteRange) {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = "<div class=\"bpvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)} <a href=\"${url}\" target=\"_blank\">(info)</a></div>"
}

class BWAnnotatedData(val annotationHTML: String, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = "<div class=\"bpvalue data\" $byteRangeDataTags>$annotationHTML 0x${data.hex()}</div>"
}