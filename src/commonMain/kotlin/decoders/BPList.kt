package decoders

import ByteWitch
import Date
import bitmage.*
import currentTimestamp
import dateFromAppleTimestamp
import htmlEscape
import kotlin.math.absoluteValue

// based on https://medium.com/@karaiskc/understanding-apples-binary-property-list-format-281e6da00dbd
class BPListParser(private val nestedDecode: Boolean = true) {

    private val objectMap = mutableMapOf<Int, BPListObject>()
    private var objectRefSize = 0
    private var offsetTableOffsetSize = 0
    private var offsetTable = byteArrayOf()
    private var sourceOffset = 0

    companion object : ByteWitchDecoder {
        override val name = "bplist"
        override fun decodesAsValid(data: ByteArray): Pair<Boolean,ByteWitchResult?> {
            return Pair(data.size > 8 && data.sliceArray(0 until 7).decodeToString() == "bplist0", null)
        }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return BPListParser().parse(data, sourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            // find offset of bplist header
            val headerPosition = data.indexOfSubsequence("bplist00".encodeToByteArray())
            if(headerPosition == -1)
                return null

            val prefix = data.sliceArray(0 until headerPosition)
            var remainder = data.fromIndex(headerPosition)

            // find plausible footer
            var footerEnd = -1
            var alreadySearchedOffset = 0
            while(footerEnd < remainder.size) {
                val footerCandidatePosition = remainder.indexOfSubsequence("0000000000".fromHex())
                if(footerCandidatePosition == -1 || remainder.size - footerCandidatePosition < 32)
                    return null
                val tableSize = remainder[footerCandidatePosition + 6]
                val refSize = remainder[footerCandidatePosition + 7]
                val objCount = remainder.fromIndex(footerCandidatePosition + 8).readLong(ByteOrder.BIG)
                val topOffset = remainder.fromIndex(footerCandidatePosition + 16).readLong(ByteOrder.BIG)
                val tablePosition = remainder.fromIndex(footerCandidatePosition + 24).readLong(ByteOrder.BIG)

                if(tableSize < 4 && refSize < 4 && objCount < 10000 && topOffset < 10000 && tablePosition < footerCandidatePosition) {
                    footerEnd = alreadySearchedOffset + footerCandidatePosition + 32
                    break
                }

                remainder = remainder.fromIndex(footerCandidatePosition+1)
                alreadySearchedOffset += footerCandidatePosition+1
            }

            if(footerEnd == -1)
                return null

            val content = data.sliceArray(headerPosition until headerPosition+footerEnd)

            try {
                val result = BPListParser().parse(content, prefix.size)
                return if(content.size == data.size)
                    result
                else
                    PartialDecode(prefix, result, data.fromIndex(headerPosition+footerEnd), Pair(0, data.size))
            } catch(e: Exception) {
                return null
            }
        }
    }


    fun parse(bytes: ByteArray, sourceOffset: Int): BPListObject {
        val rootObject = parseCodable(bytes, sourceOffset)
        rootObject.rootByteRange = Pair(sourceOffset, sourceOffset+bytes.size)

        return if(KeyedArchiveDecoder.isKeyedArchive(rootObject)) {
                val archive = KeyedArchiveDecoder.decode(rootObject as BPDict)
                archive.rootByteRange = Pair(sourceOffset, sourceOffset+bytes.size)
                archive
            }
            else
                rootObject
    }

    fun parseCodable(bytes: ByteArray, sourceOffset: Int): BPListObject {
        this.sourceOffset = sourceOffset
        objectMap.clear()

        val header = bytes.sliceArray(0 until 8)
        if (!header.decodeToString().startsWith("bplist0"))
            throw Exception("Expected bplist header 'bplist0*' in bytes ${bytes.hex()}")

        val trailer = bytes.fromIndex(bytes.size - 32)
        offsetTableOffsetSize = trailer[6].toInt()
        objectRefSize = trailer[7].toInt()
        val numObjects = ULong.fromBytes(trailer.sliceArray(8 until 16), ByteOrder.BIG).toInt()
        val topObjectOffset = ULong.fromBytes(trailer.sliceArray(16 until 24), ByteOrder.BIG).toInt()
        val offsetTableStart = ULong.fromBytes(trailer.sliceArray(24 until 32), ByteOrder.BIG).toInt()

        offsetTable =
            bytes.sliceArray(offsetTableStart until (offsetTableStart + numObjects * offsetTableOffsetSize))

        return readObjectFromOffsetTableEntry(bytes, topObjectOffset)
    }

    private fun readObjectFromOffsetTableEntry(bytes: ByteArray, index: Int): BPListObject {
        val offset = UInt.fromBytes(offsetTable.sliceArray(index*offsetTableOffsetSize until (index+1)*offsetTableOffsetSize), ByteOrder.BIG).toInt()
        return readObjectFromOffset(bytes, offset)
    }

    private fun readObjectFromOffset(bytes: ByteArray, offset: Int): BPListObject {
        // check cache
        if(objectMap.containsKey(offset))
            return objectMap[offset]!!

        // objects start with a one byte type descriptor
        val objectByte = bytes[offset].toUByte().toInt()
        // for some objects, the lower four bits carry length info
        val lengthBits = objectByte and 0x0f

        val parsed = when(objectByte) {
            0x00 -> BPNull
            0x08 -> BPFalse
            0x09 -> BPTrue
            0x0f -> BPFill
            // Int
            in 0x10 until 0x20 -> {
                // length bits encode int byte size as 2^n
                val byteLen = 1 shl lengthBits

                // some bplists contain crazy long integers for tiny numbers
                // we'll just hope they're never used beyond actual long range
                if(byteLen in 9..16) {
                    val upper = Long.fromBytes(bytes.sliceArray(offset+1 until (offset+1+byteLen-8)), ByteOrder.BIG)
                    val lower = Long.fromBytes(bytes.sliceArray((offset+1+byteLen-8) until (offset+1+byteLen)), ByteOrder.BIG)

                    if(upper != 0L) {
                        Logger.log("Encountered very long BPInt ($byteLen B) that cannot be equivalently represented as Long")
                        throw Exception("Overlong BPInt")
                    }
                    // Bug here that I'm too lazy to fix: We're potentially interpreting unsigned data as signed here
                    BPInt(lower, Pair(sourceOffset+offset, sourceOffset+offset+1+byteLen))
                }
                else
                    // TODO: does this mess with signs? how does bigint do it?
                    BPInt(Long.fromBytes(bytes.sliceArray(offset+1 until offset+1+byteLen), ByteOrder.BIG), Pair(sourceOffset+offset, sourceOffset+offset+1+byteLen))
            }
            // Real
            in 0x20 until 0x30 -> {
                // length bits encode real byte size as 2^n
                val byteLen = 1 shl lengthBits
                val value = when(byteLen) {
                    4 -> {
                        bytes.sliceArray(offset+1 until offset+1+4).readFloat(ByteOrder.BIG).toDouble()
                    }
                    8 -> {
                        bytes.sliceArray(offset+1 until offset+1+8).readDouble(ByteOrder.BIG)
                    }
                    else -> throw Exception("Got unexpected byte length for real: $byteLen in ${bytes.hex()}")
                }
                BPReal(value, Pair(sourceOffset+offset, sourceOffset+offset+1+byteLen))
            }
            // Date, always 8 bytes long
            0x33 -> {
                val timestamp = bytes.sliceArray(offset+1 until offset+1+8).readDouble(ByteOrder.BIG)
                BPDate(timestamp, Pair(sourceOffset+offset, sourceOffset+offset+1+8))
            }
            // Data
            in 0x40 until 0x50 -> {
                // length bits encode byte count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val byteLen = tmp.first
                val effectiveOffset = tmp.second

                val data = bytes.sliceArray(effectiveOffset until effectiveOffset+byteLen)

                // decode nested bplists
                return if(decodesAsValid(data).first && nestedDecode)
                    BPListParser().parseCodable(data, effectiveOffset)
                else
                    BPData(data, Pair(sourceOffset+offset, sourceOffset+effectiveOffset+byteLen))
            }
            // ASCII string
            in 0x50 until 0x60 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second
                // ascii encodes at one char per byte, we can use default UTF8 decoding as ascii is cross compatible with everything
                val string = bytes.decodeToString(effectiveOffset, effectiveOffset+charLen)
                BPAsciiString(string, Pair(sourceOffset+offset, sourceOffset+effectiveOffset+charLen))
            }
            // Unicode string
            in 0x60 until 0x70 -> {
                // length bits encode character count, if all ones additional length integer follows
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val charLen = tmp.first
                val effectiveOffset = tmp.second
                // this is UTF16, encodes at two bytes per char
                val stringBytes = bytes.sliceArray(effectiveOffset until effectiveOffset+charLen*2)
                val string = stringBytes.decodeAsUTF16BE()
                BPUnicodeString(string, Pair(sourceOffset+offset, sourceOffset+effectiveOffset+charLen*2))
            }
            // UID, byte length is lengthBits+1
            in 0x80 until 0x90 -> BPUid(bytes.sliceArray(offset+1 until offset+2+lengthBits), Pair(sourceOffset+offset, sourceOffset+offset+2+lengthBits))
            // Array
            in 0xa0 until 0xb0 -> {
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val entries = tmp.first
                val effectiveOffset = tmp.second

                val values = (0 until entries).map {i ->
                    val objectIndex = Int.fromBytes(bytes.sliceArray(effectiveOffset+i*objectRefSize until effectiveOffset+(i+1)*objectRefSize), ByteOrder.BIG)
                    readObjectFromOffsetTableEntry(bytes, objectIndex)
                }

                BPArray(values, Pair(sourceOffset+offset, sourceOffset+effectiveOffset+objectRefSize*entries))
            }
            // Set
            in 0xc0 until 0xd0 -> {
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val entries = tmp.first
                val effectiveOffset = tmp.second

                val values = (0 until entries).map {i ->
                    val objectIndex = Int.fromBytes(bytes.sliceArray(effectiveOffset+i*objectRefSize until effectiveOffset+(i+1)*objectRefSize), ByteOrder.BIG)
                    readObjectFromOffsetTableEntry(bytes, objectIndex)
                }

                BPSet(entries, values, Pair(sourceOffset+offset, sourceOffset+effectiveOffset+objectRefSize*entries))
            }
            // Dict
            in 0xd0 until 0xf0 -> {
                val tmp = getFillAwareLengthAndOffset(bytes, offset)
                val entries = tmp.first
                var effectiveOffset = tmp.second

                val keys = (0 until entries).map {i ->
                    val keyIndex = Int.fromBytes(bytes.sliceArray(effectiveOffset+i*objectRefSize until effectiveOffset+(i+1)*objectRefSize), ByteOrder.BIG)
                    readObjectFromOffsetTableEntry(bytes, keyIndex)
                }

                effectiveOffset += entries * objectRefSize

                val values = (0 until entries).map {i ->
                    val valueIndex = Int.fromBytes(bytes.sliceArray(effectiveOffset+i*objectRefSize until effectiveOffset+(i+1)*objectRefSize), ByteOrder.BIG)
                    readObjectFromOffsetTableEntry(bytes, valueIndex)

                }

                BPDict(keys.zip(values).toMap(), Pair(sourceOffset+offset, sourceOffset+effectiveOffset+objectRefSize*entries))
            }
            else -> throw Exception("Unknown object type byte 0b${objectByte.toString(2)}")
        }

        objectMap[offset] = parsed
        return parsed
    }


    private fun getFillAwareLengthAndOffset(bytes: ByteArray, offset: Int): Pair<Int, Int> {
        val lengthBits = bytes[offset].toInt() and 0x0f
        if(lengthBits < 0x0f)
            return Pair(lengthBits, offset+1)

        val sizeFieldSize = 1 shl (bytes[offset+1].toInt() and 0x0f) // size field is 2^n bytes
        val size = ULong.fromBytes(bytes.sliceArray(offset+2 until offset+2+sizeFieldSize), ByteOrder.BIG).toInt() // let's just hope they never get into long territory

        return Pair(size, offset+2+sizeFieldSize)
    }
}

abstract class BPListObject : ByteWitchResult {

    var rootByteRange: Pair<Int,Int>? = null

    override fun renderHTML(): String {
        val rootRangeTags = if(rootByteRange != null) "data-start=\"${rootByteRange!!.first}\" data-end=\"${rootByteRange!!.second}\"" else ""
        return "<div class=\"bplist roundbox\" $rootRangeTags>${renderHtmlValue()}</div>"
    }

    open fun renderHtmlValue(): String {
        return "<div class=\"bpvalue\" $byteRangeDataTags>${htmlEscape(toString())}</div>"
    }
}

object BPNull : BPListObject() {
    override val sourceByteRange: Pair<Int, Int>? = null
    override fun toString() = "null"
}
object BPTrue : BPListObject() {
    override val sourceByteRange: Pair<Int, Int>? = null
    override fun toString() = "true"
}
object BPFalse : BPListObject() {
    override val sourceByteRange: Pair<Int, Int>? = null
    override fun toString() = "false"
}
object BPFill : BPListObject() {
    override val sourceByteRange: Pair<Int, Int>? = null
    override fun toString() = "BPFill"
}
data class BPInt(val value: Long, override val sourceByteRange: Pair<Int, Int>): BPListObject() {
    override fun toString() = value.toString()
}
data class BPUInt(val value: ULong, override val sourceByteRange: Pair<Int, Int>): BPListObject() {
    override fun toString() = value.toString()
}
data class BPReal(val value: Double, override val sourceByteRange: Pair<Int, Int>): BPListObject() {
    override fun renderHtmlValue(): String {

        val currentTime = currentTimestamp()
        val diffUnixEpoch = ((value*1000).toLong() - currentTime).absoluteValue
        val diffAppleEpoch = ((value*1000).toLong() + 978307200000L - currentTime).absoluteValue

        return when {
            // timestamps in reasonable date ranges are probably dates
            diffUnixEpoch < 1000L*60*60*24*365*10 -> "<div class=\"bpvalue\">Real(unix time ${asDate(false)})</div>"
            diffAppleEpoch < 1000L*60*60*24*365*10 -> "<div class=\"bpvalue\">Real(apple time ${asDate(true)})</div>"
            else -> "<div class=\"bpvalue\" $byteRangeDataTags>$value</div>"
        }
    }

    /**
     * Assume this I64 value represents a double containing an NSDate timestamp (seconds since Jan 01 2001)
     * and turn it into a Date object
     */
    fun asDate(appleEpoch: Boolean = true): Date {
        val offset = if(appleEpoch) 978307200000L else 0L
        return Date((value*1000).toLong() + offset)
    }
}
data class BPDate(val timestamp: Double, override val sourceByteRange: Pair<Int, Int>) : BPListObject() {
    override fun toString() = "BPDate($timestamp)"

    override fun renderHtmlValue() = "<div class=\"bpvalue\" $byteRangeDataTags>${asDate()}</div>"

    fun asDate(): Date = dateFromAppleTimestamp(timestamp)
}
class BPData(val value: ByteArray, override val sourceByteRange: Pair<Int, Int>) : BPListObject() {
    override fun toString() = "BPData(${value.hex()})"

    override fun renderHtmlValue(): String {
        // try to decode string-based payloads despite this not being a string, same for opack
        val decodeAttempt = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)
        return decodeAttempt?.renderHTML() ?: "<div class=\"bpvalue data\" $byteRangeDataTags>0x${value.hex()}</div>"
    }
}

abstract class BPString : BPListObject() {
    abstract val value: String

    override fun renderHtmlValue(): String {
        val veryLong = if(value.length > 300) "data" else ""
        return "<div class=\"bpvalue stringlit $veryLong\" $byteRangeDataTags>\"$value\"</div>"
    }
}
data class BPAsciiString(override val value: String, override val sourceByteRange: Pair<Int, Int>? = null) : BPString() {
    override fun toString() = "\"$value\""

    override fun equals(other: Any?): Boolean {
        return other is BPString && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
data class BPUnicodeString(override val value: String, override val sourceByteRange: Pair<Int, Int>? = null) : BPString() {
    override fun toString() = "\"$value\""

    override fun equals(other: Any?): Boolean {
        return other is BPString && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
class BPUid(val value: ByteArray, override val sourceByteRange: Pair<Int, Int>) : BPListObject() {
    override fun toString() = "uid(${value.hex()})"
}
data class BPArray(val values: List<BPListObject>, override val sourceByteRange: Pair<Int, Int>?) : BPListObject() {
    override fun toString() = "[${values.joinToString(", ")}]"

    override fun renderHtmlValue(): String {
        if(values.isEmpty())
            return "<div class=\"bpvalue\" $byteRangeDataTags>[∅]</div>"
        val entries = values.joinToString(""){ it.renderHtmlValue() }
        val maybelarge = if(entries.length > 700) "largecollection" else ""
        return "<div class=\"bpvalue\" $byteRangeDataTags><div class=\"bplist bparray $maybelarge\">$entries</div></div>"
    }
}
data class BPSet(val entries: Int, val values: List<BPListObject>, override val sourceByteRange: Pair<Int, Int>?) : BPListObject() {
    override fun toString() = "<${values.joinToString(", ")}>"

    override fun renderHtmlValue(): String {
        if(values.isEmpty())
            return "<div class=\"bpvalue\" $byteRangeDataTags><∅></div>"
        val entries = values.joinToString(""){ it.renderHtmlValue() }
        val maybelarge = if(entries.length > 500) "largecollection" else ""
        return "<div class=\"bpvalue\" $byteRangeDataTags><div class=\"bplist bpset $maybelarge\">$entries</div></div>"
    }
}
data class BPDict(val values: Map<BPListObject, BPListObject>, override val sourceByteRange: Pair<Int, Int>?) : BPListObject() {
    override fun toString() = values.toString()

    override fun renderHtmlValue(): String {
        if(values.isEmpty())
            return "<div class=\"bpvalue\" $byteRangeDataTags>{∅}</div>"
        val entries = values.toList().joinToString(""){ "<div>${it.first.renderHtmlValue()}<span>→</span> ${it.second.renderHtmlValue()}</div>" }
        return "<div class=\"bpvalue\" $byteRangeDataTags><div class=\"bplist bpdict\">$entries</div></div>"
    }
}

abstract class NSObject : BPListObject() {
    override fun renderHTML(): String {
        val rootRangeTags = if(rootByteRange != null) "data-start=\"${rootByteRange!!.first}\" data-end=\"${rootByteRange!!.second}\"" else ""
        return "<div class=\"nsarchive roundbox\" $rootRangeTags>${renderHtmlValue()}</div>"
    }
}

data class NSArray(val values: List<BPListObject>, override val sourceByteRange: Pair<Int, Int>?): NSObject() {
    override fun toString() = values.toString()

    override fun renderHtmlValue(): String {
        if(values.isEmpty())
            return "<div class=\"bpvalue\">[∅]</div>"
        val entries = values.joinToString(""){ it.renderHtmlValue() }
        val maybelarge = if(entries.length > 700) "largecollection" else ""
        return "<div class=\"bpvalue\"><div class=\"nsarray $maybelarge\">$entries</div></div>"
    }
}

data class NSSet(val values: Set<BPListObject>, override val sourceByteRange: Pair<Int, Int>?): NSObject() {
    override fun toString() = values.toString()

    override fun renderHtmlValue(): String {
        if(values.isEmpty())
            return "<div class=\"bpvalue\"><∅></div>"
        val entries = values.joinToString(""){ it.renderHtmlValue() }
        val maybelarge = if(entries.length > 500) "largecollection" else ""
        return "<div class=\"bpvalue\"><div class=\"nsset $maybelarge\">$entries</div></div>"
    }
}

data class NSDict(val values: Map<BPListObject, BPListObject>) : NSObject() {
    override val sourceByteRange: Pair<Int, Int>? = null
    override fun toString() = values.toString()

    override fun renderHtmlValue(): String {
        if(values.isEmpty())
            return "<div class=\"bpvalue\">{∅}</div>"
        val entries = values.toList().joinToString(""){ "<div>${it.first.renderHtmlValue()}<span>→</span> ${it.second.renderHtmlValue()}</div>" }
        return "<div class=\"bpvalue\"><div class=\"nsdict\">$entries</div></div>"
    }
}

data class NSDate(val value: Date, override val sourceByteRange: Pair<Int, Int>) : NSObject() {
    override fun toString() = value.toString()
}

data class NSUUID(val value: ByteArray, override val sourceByteRange: Pair<Int, Int>) : NSObject() {
    override fun toString() = value.hex() // for now
}

data class NSData(val value: ByteArray, override val sourceByteRange: Pair<Int, Int>) : NSObject() {
    override fun toString() = "NSData(${value.hex()})"

    override fun renderHtmlValue(): String {
        // try to decode string-based payloads despite this not being a string, same for opack
        val decodeAttempt = ByteWitch.quickDecode(value, sourceByteRange.first)

        // we have to wrap in an nsvalue if we have a nested decode of the same type to distinguish them visually
        // for nested decodes of different types we can omit it for cleaner display
        val requiresWrapping = decodeAttempt == null || decodeAttempt is NSObject

        val prePayload = if(requiresWrapping) "<div class=\"nsvalue data\" $byteRangeDataTags>" else ""
        val postPayload = if(requiresWrapping) "</div>" else ""
        val payloadHTML = decodeAttempt?.renderHTML() ?: "0x${value.hex()}"

        return "$prePayload$payloadHTML$postPayload"
    }
}

data class RecursiveBacklink(val index: Int, var value: BPListObject?) : BPListObject() {
    override val sourceByteRange: Pair<Int, Int>? = null
    override fun toString() = "Backlink(not rendering, index $index)"
}