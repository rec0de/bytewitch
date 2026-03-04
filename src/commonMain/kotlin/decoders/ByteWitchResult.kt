package decoders

import bitmage.hex
import htmlEscape

interface ByteWitchResult {

    enum class Colour {
        BPLIST, PROTOBUF, OPACK, NSARCHIVE, ASN1, GENERIC, NEUTRAL, PLAIN
    }

    val colour: ByteWitchResult.Colour

    fun renderHTML(): String

    fun wrapIfSameColour(subresult: ByteWitchResult): String {
        return if(subresult.colour == Colour.PLAIN || subresult.colour != colour)
            subresult.renderHTML()
        else {
            "<div class=\"bwvalue data\">${subresult.renderHTML()}</div>"
        }
    }

    fun wrapIfSameColour(subresult: ByteWitchResult?, fallback: String, rangeTags: String): String {
        return if(subresult == null)
            bwvalue(fallback, rangeTags, data = true)
        else
            wrapIfSameColour(subresult)
    }

    fun wrapIfSameColour(subresult: ByteWitchResult?, data: ByteArray, rangeTags: String) = wrapIfSameColour(subresult, "0x${data.hex()}", rangeTags)

    fun rangeTagsFor(start: Int, end: Int) = "data-start=\"$start\" data-end=\"$end\""
    fun relativeRangeTags(start: Int, length: Int) : String {
        check(sourceByteRange != null) { "attempting use of relativeRangeTagsFor with null sourceByteRange" }
        return rangeTagsFor(sourceByteRange!!.first+start, sourceByteRange!!.first+start+length)
    }

    val sourceByteRange: Pair<Int,Int>?

    val byteRangeDataTags: String
        get() = if(sourceByteRange == null || sourceByteRange!!.first < 0) "" else rangeTagsFor(sourceByteRange!!.first, sourceByteRange!!.second)
}

class MultiPartialDecode(val parts: List<Pair<ByteWitchResult?, BWRangeTaggedData?>>, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.NEUTRAL

    override fun renderHTML(): String {
        val items = parts.mapNotNull {
            when {
                it.first != null -> wrapIfSameColour(it.first!!)
                it.second != null && it.second!!.data.isNotEmpty() -> bwvalue("0x${it.second!!.data.hex()}",  rangeTagsFor(it.second!!.start, it.second!!.start + it.second!!.data.size), data = true)
                else -> null
            }
        }

        return "<div class=\"roundbox neutral largecollection\" $byteRangeDataTags>${items.joinToString(" ")}</div>"
    }
}

class BWStringCollection(val elements: List<BWString>, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.NEUTRAL
    override fun renderHTML() = "<div class=\"roundbox neutral\">" + elements.joinToString(" ") { it.renderHTML() } + "</div>"
}

open class BWString(val string: String, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = "<div class=\"bwvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)}</div>"
}

class BWLinkedString(string: String, private val url: String, sourceByteRange: Pair<Int, Int>): BWString(string, sourceByteRange) {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = "<div class=\"bwvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)} <a href=\"${url}\" target=\"_blank\">(info)</a></div>"
}

class BWAnnotatedData(val annotationHTML: String, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = "<div class=\"bwvalue data\" $byteRangeDataTags>$annotationHTML 0x${data.hex()}</div>"
}

class BWRangeTaggedData(val data: ByteArray, val start: Int)
class BWRangeTaggedInt(val value: Int, val start: Int, val length: Int)

fun bwvalue(content: String, rangeTags: String, data: Boolean = false) = "<div class=\"bwvalue${if(data) " data" else ""}\" $rangeTags>$content</div>"