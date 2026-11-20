import bitmage.ByteOrder
import bitmage.hex
import bitmage.untilIndex
import decoders.QuickDirtyDecoder

interface ByteWitchResult {

    enum class Colour {
        BPLIST, PROTOBUF, OPACK, NSARCHIVE, ASN1, GENERIC, NEUTRAL, PLAIN
    }

    val colour: Colour

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

    fun wrapIfSameColour(subresult: ByteWitchResult?, data: ByteArray, rangeTags: String): String {
        val fallback = when {
            data.isEmpty() -> "∅"
            data.size < veryLargeDataFieldThreshold -> "0x${data.hex()}"
            else -> "long data (${humanReadableByteCount(data.size)}), first 32B: 0x${data.untilIndex(32).hex()}"
        }
        return wrapIfSameColour(subresult, fallback, rangeTags)
    }

    fun rangeTagsFor(start: Int, end: Int) = "data-start=\"$start\" data-end=\"$end\""
    fun relativeRangeTags(start: Int, length: Int) : String {
        check(sourceByteRange != null) { "attempting use of relativeRangeTagsFor with null sourceByteRange" }
        return rangeTagsFor(sourceByteRange!!.first+start, sourceByteRange!!.first+start+length)
    }

    fun humanReadableByteCount(len: ULong): String {
        val kb = 1024UL
        val mb = kb * 1024UL

        return when(len) {
            in 0UL..(5U*kb) -> "$len B"
            in 5U*kb..5U*mb -> "<span title=\"$len B\">${len/kb} KiB</span>"
            else -> "<span title=\"$len B\">${len/mb} MiB</span>"
        }
    }
    fun humanReadableByteCount(len: Int) = humanReadableByteCount(len.toULong())

    val sourceByteRange: Pair<Int,Int>?

    val byteRangeDataTags: String
        get() = if(sourceByteRange == null || sourceByteRange!!.first < 0) "" else rangeTagsFor(sourceByteRange!!.first, sourceByteRange!!.second)
}

abstract class AnnotatedResult : ByteWitchResult {
    enum class Type {
        STRUCTURE,
        LENGTH_FLAG, LENGTH_FLAG_LE, LENGTH_FLAG_BE, TYPE_FLAG, TYPE_LENGTH_FLAG,
        STRING, UTF8, UTF16, ASCII,
        INT, INT_8, INT_16_LE, INT_16_BE, INT_24_LE, INT_24_BE, INT_32_LE, INT_32_BE, INT_64_BE, INT_64_LE,
        UINT, UINT_8, UINT_16_LE, UINT_16_BE, UINT_24_LE, UINT_24_BE, UINT_32_LE, UINT_32_BE, UINT_64_BE, UINT_64_LE,
        REAL, DATE, MAGIC_BYTES, BYTES, BYTES_HIGH_ENTROPY, BOOL, NULL, FILL, OID, UID, FLAGS, POINTER_BE, POINTER_LE,
        UNKNOWN;

        companion object {
            fun getIntType(signed: Boolean, length: Int, endianness: ByteOrder): Type {
                return when {
                    signed && length == 1 -> INT_8
                    signed && length == 2 && endianness == ByteOrder.BIG -> INT_16_BE
                    signed && length == 2 && endianness == ByteOrder.LITTLE -> INT_16_LE
                    signed && length == 3 && endianness == ByteOrder.BIG -> INT_24_BE
                    signed && length == 3 && endianness == ByteOrder.LITTLE -> INT_24_LE
                    signed && length == 4 && endianness == ByteOrder.BIG -> INT_32_BE
                    signed && length == 4 && endianness == ByteOrder.LITTLE -> INT_32_LE
                    signed && length == 8 && endianness == ByteOrder.BIG -> INT_64_BE
                    signed && length == 8 && endianness == ByteOrder.LITTLE -> INT_64_LE
                    !signed && length == 1 -> INT_8
                    !signed && length == 2 && endianness == ByteOrder.BIG -> UINT_16_BE
                    !signed && length == 2 && endianness == ByteOrder.LITTLE -> UINT_16_LE
                    !signed && length == 3 && endianness == ByteOrder.BIG -> UINT_24_BE
                    !signed && length == 3 && endianness == ByteOrder.LITTLE -> UINT_24_LE
                    !signed && length == 4 && endianness == ByteOrder.BIG -> UINT_32_BE
                    !signed && length == 4 && endianness == ByteOrder.LITTLE -> UINT_32_LE
                    !signed && length == 8 && endianness == ByteOrder.BIG -> UINT_64_BE
                    !signed && length == 8 && endianness == ByteOrder.LITTLE -> UINT_64_LE
                    else -> throw Exception("unsupported int type: signed? $signed size $length endianness $endianness")
                }
            }
        }
    }

    var source: String? = null
    var interpretation: Type? = null

    fun annotate(source: String, interpretation: Type) {
        this.source = source
        this.interpretation = interpretation
    }
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

open class BWGenericSequence(val elements: List<ByteWitchResult>, override val sourceByteRange: Pair<Int, Int>, val vertical: Boolean = false) : AnnotatedResult() {
    override val colour = ByteWitchResult.Colour.GENERIC
    override fun renderHTML(): String {
        val large = if(vertical) " largecollection" else ""
        return if(elements.size == 1)
            elements.first().renderHTML()
        else
            "<div class=\"generic roundbox$large\" $byteRangeDataTags>${elements.joinToString("") { wrapIfSameColour(it, "", rangeTagsFor(it.sourceByteRange!!.first, it.sourceByteRange!!.second)) }}</div>"
    }
}

class BWGenericData(val data: ByteArray, override val sourceByteRange: Pair<Int, Int>): AnnotatedResult() {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML(): String {
        val quickDecode = ByteWitch.quickDecode(data, sourceByteRange.first)
        return wrapIfSameColour(quickDecode, data, rangeTagsFor(sourceByteRange.first, sourceByteRange.second))
    }
}

class BWStringCollection(val elements: List<BWString>, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.NEUTRAL
    override fun renderHTML() = "<div class=\"roundbox neutral\">" + elements.joinToString(" ") { it.renderHTML() } + "</div>"
}

open class BWString(val string: String, override val sourceByteRange: Pair<Int, Int>) : AnnotatedResult() {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = "<div class=\"bwvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)}</div>"
}

class BWLinkedString(string: String, private val url: String, sourceByteRange: Pair<Int, Int>): BWString(string, sourceByteRange) {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML() = "<div class=\"bwvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)} <a href=\"${url}\" target=\"_blank\">(info)</a></div>"
}

class BWAnnotatedData(val annotationHTML: String, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>) : AnnotatedResult() {
    override val colour = ByteWitchResult.Colour.PLAIN
    override fun renderHTML(): String {
        val dataRendering = if(data.size > veryLargeDataFieldThreshold)
            "long data (${humanReadableByteCount(data.size)}), first 32B: 0x${data.untilIndex(32).hex()}"
        else
            "0x${data.hex()}"
        return "<div class=\"bwvalue data\" $byteRangeDataTags>$annotationHTML $dataRendering</div>"
    }
}

class BWRangeTaggedData(val data: ByteArray, val start: Int)
class BWRangeTaggedInt(val value: Int, val start: Int, val length: Int)

fun bwvalue(content: String, rangeTags: String, data: Boolean = false) = "<div class=\"bwvalue${if(data) " data" else ""}\" $rangeTags>$content</div>"