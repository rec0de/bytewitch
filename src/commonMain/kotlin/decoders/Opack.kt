package decoders

import ByteWitch
import Date
import bitmage.*
import currentTimestamp
import dateFromAppleTimestamp
import htmlEscape
import kotlin.math.absoluteValue


// OPACK encodes a subset of data that can be encoded in BPLists - we'll just use the existing BPList wrapper classes for now
class OpackParser {

    companion object : ByteWitchDecoder {
        override val name = "opack"

        override fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {
            return OpackParser().parseTopLevel(data, sourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            try {
                val parser = OpackParser()
                val result = parser.parse(data)
                val remainder = data.fromIndex(parser.parseOffset)

                // parsed OPACK should represent at least 30% of input data
                if(remainder.size > data.size * 0.7)
                    return null

                return if(remainder.isEmpty())
                    result
                else
                    PartialDecode(byteArrayOf(), result, remainder, Pair(0, data.size))

            } catch (e: Exception) {
                Logger.log(e.toString())
                return null
            }
        }

        // single 0x01 / 0x02 bytes are often false-positive detected as booleans, return low confidence for those
        override fun confidence(data: ByteArray): Double {
            return if(data.size == 1 && data[0].toInt() in 1..2)
                    0.2
                else
                    super.confidence(data)
        }
    }

    private var parseOffset = 0
    private var sourceOffset = 0

    private val lastConsumedBytePosition: Int
        get() = sourceOffset + parseOffset

    fun parseTopLevel(bytes: ByteArray, sourceOffsetParam: Int): OpackObject {
        parseOffset = 0
        sourceOffset = sourceOffsetParam
        val result = parse(bytes)

        check(parseOffset >= bytes.size-1){ "input data not fully consumed" }

        return result
    }

    private fun parse(bytes: ByteArray): OpackObject {
        val typeByte = bytes[parseOffset].toUByte().toUInt()
        //Logger.log("parsing type byte: 0x${typeByte.toString(16)}")
        return when(typeByte) {
            0x01u, 0x02u -> parseAsBool(bytes)
            0x05u -> parseAsUUID(bytes)
            0x06u -> parseAsDate(bytes)
            in 0x08u..0x2fu -> parseAsInt(bytes)
            0x30u, 0x31u, 0x32u, 0x33u -> parseAsInt(bytes)
            0x35u, 0x36u -> parseAsFloat(bytes)
            in 0x40u..0x60u -> parseAsString(bytes)
            in 0x61u..0x64u -> parseAsString(bytes)
            in 0x70u..0x90u -> parseAsData(bytes)
            in 0x91u..0x94u -> parseAsData(bytes)
            in 0xd0u..0xdfu -> parseAsArray(bytes)
            in 0xe0u..0xefu -> parseAsDict(bytes)
            else -> throw Exception("Unsupported type 0x${typeByte.toString(16)}")
        }
    }

    private fun parseAsBool(bytes: ByteArray): OpackObject {
        val byte = readInt(bytes, 1)
        return when (byte) {
            0x01 -> OPTrue(sourceOffset+parseOffset-1) // we already incremented parse offset here
            0x02 -> OPFalse(sourceOffset+parseOffset-1)
            else -> throw Exception("Unexpected OPACK boolean ${bytes.hex()}")
        }
    }

    private fun parseAsUUID(bytes: ByteArray): OPData {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        if(type != 0x05)
            throw Exception("Unexpected OPACK UUID ${bytes.hex()}")
        val uuid = readBytes(bytes, 16)
        return OPData(uuid, Pair(start, lastConsumedBytePosition))
    }

    private fun parseAsDate(bytes: ByteArray): OPDate {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        if(type != 0x06)
            throw Exception("Unexpected OPACK date ${bytes.hex()}")
        val timestamp = readBytes(bytes, 8).readDouble(ByteOrder.BIG)
        return OPDate(timestamp, Pair(start, lastConsumedBytePosition))
    }

    private fun parseAsInt(bytes: ByteArray): OPInt {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)

        return when(type) {
            in 0x08..0x2f -> OPInt((type - 8).toLong(), Pair(start, lastConsumedBytePosition))
            0x30 -> OPInt(readInt(bytes, 1).toLong(), Pair(start, lastConsumedBytePosition))
            0x31 -> OPInt(readInt(bytes, 2).toLong(), Pair(start, lastConsumedBytePosition))
            0x32 -> OPInt(readInt(bytes, 3).toLong(), Pair(start, lastConsumedBytePosition))
            0x33 -> OPInt(readInt(bytes, 4).toLong(), Pair(start, lastConsumedBytePosition))
            else -> throw Exception("Unexpected OPACK int ${bytes.hex()}")
        }
    }

    private fun parseAsFloat(bytes: ByteArray): OPReal {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        when(type) {
            0x35 -> {
                return OPReal(readBytes(bytes, 4).readFloat(ByteOrder.BIG).toDouble(), Pair(start, lastConsumedBytePosition))
            }
            0x36 -> {
                return OPReal(readBytes(bytes, 8).readDouble(ByteOrder.BIG), Pair(start, lastConsumedBytePosition))
            }
            else -> throw Exception("Unexpected OPACK float ${bytes.hex()}")
        }
    }

    private fun parseAsString(bytes: ByteArray): OPString {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)

        when(type) {
            in 0x40..0x60 -> return OPString(readBytes(bytes, type - 0x40).decodeToString(), Pair(start, lastConsumedBytePosition))
            0x61 -> {
                val length = readInt(bytes, 1)
                return OPString(readBytes(bytes, length).decodeToString(), Pair(start, lastConsumedBytePosition))
            }
            0x62 -> {
                val length = readInt(bytes, 2)
                return OPString(readBytes(bytes, length).decodeToString(), Pair(start, lastConsumedBytePosition))
            }
            0x63 -> {
                val length = readInt(bytes, 3)
                return OPString(readBytes(bytes, length).decodeToString(), Pair(start, lastConsumedBytePosition))
            }
            0x64 -> {
                val length = readInt(bytes, 4)
                return OPString(readBytes(bytes, length).decodeToString(), Pair(start, lastConsumedBytePosition))
            }
            else -> throw Exception("Unexpected OPACK string ${bytes.hex()}")
        }
    }

    private fun parseAsData(bytes: ByteArray): OPData {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)

        when(type) {
            in 0x70..0x90 -> return OPData(readBytes(bytes, type - 0x70), Pair(start, lastConsumedBytePosition))
            0x91 -> {
                val length = readInt(bytes, 1)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }
            0x92 -> {
                val length = readInt(bytes, 2)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }
            0x93 -> {
                val length = readInt(bytes, 3)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }
            0x94 -> {
                val length = readInt(bytes, 4)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }
            else -> throw Exception("Unexpected OPACK data ${bytes.hex()}")
        }
    }

    private fun parseAsArray(bytes: ByteArray): OPArray {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        val entries = mutableListOf<OpackObject>()

        when(type) {
            in 0xd0..0xde -> {
                val length = type - 0xd0
                var i = 0
                while(i < length) {
                    entries.add(parse(bytes))
                    i += 1
                }
            }
            0xdf -> {
                while(bytes[parseOffset].toInt() != 0x03)
                    entries.add(parse(bytes))
            }
            else -> throw Exception("Unexpected OPACK array ${bytes.hex()}")
        }

        return OPArray(entries, Pair(start, lastConsumedBytePosition))
    }

    private fun parseAsDict(bytes: ByteArray): OPDict {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        val entries = mutableMapOf<OpackObject, OpackObject>()

        when(type) {
            in 0xe0..0xee -> {
                val length = type - 0xe0
                var i = 0
                while(i < length) {
                    entries[parse(bytes)] = parse(bytes)
                    i += 1
                }
            }
            0xef -> {
                while(bytes[parseOffset].toInt() != 0x03)
                    entries[parse(bytes)] = parse(bytes)
            }
            else -> throw Exception("Unexpected OPACK dict ${bytes.hex()}")
        }

        return OPDict(entries, Pair(start, lastConsumedBytePosition))
    }

    private fun readBytes(bytes: ByteArray, length: Int): ByteArray {
        val sliced = bytes.sliceArray(parseOffset until parseOffset + length)
        parseOffset += length
        return sliced
    }

    private fun readInt(bytes: ByteArray, size: Int): Int {
        val int = Int.fromBytes(bytes.sliceArray(parseOffset until parseOffset +size), ByteOrder.BIG)
        parseOffset += size
        return int
    }
}

abstract class OpackObject : ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"opack roundbox\">${renderHtmlValue()}</div>"
    }

    open fun renderHtmlValue(): String {
        return "<div class=\"bpvalue\" $byteRangeDataTags>${htmlEscape(toString())}</div>"
    }
}


class OPTrue(bytePosition: Int) : OpackObject() {
    override val sourceByteRange = Pair(bytePosition, bytePosition)
    override fun toString() = "true"
}
class OPFalse(bytePosition: Int) : OpackObject() {
    override val sourceByteRange = Pair(bytePosition, bytePosition)
    override fun toString() = "false"
}

data class OPInt(val value: Long, override val sourceByteRange: Pair<Int, Int>): OpackObject() {
    override fun toString() = value.toString()
}
data class OPReal(val value: Double, override val sourceByteRange: Pair<Int, Int>): OpackObject() {
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
data class OPDate(val timestamp: Double, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "BPDate($timestamp)"

    override fun renderHtmlValue() = "<div class=\"bpvalue\" $byteRangeDataTags>${asDate()}</div>"

    fun asDate(): Date = dateFromAppleTimestamp(timestamp)
}
class OPData(val value: ByteArray, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "BPData(${value.hex()})"

    override fun renderHtmlValue(): String {
        // try to decode nested stuff
        val decode = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)
        return decode?.renderHTML() ?: "<div class=\"bpvalue data\" $byteRangeDataTags>0x${value.hex()}</div>"
    }
}

data class OPString(val value: String, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "\"$value\""
}

data class OPArray(val values: List<OpackObject>, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "[${values.joinToString(", ")}]"

    override fun renderHtmlValue(): String {
        if(values.isEmpty())
            return "<div class=\"bpvalue\">[∅]</div>"
        val entries = values.joinToString(""){ it.renderHtmlValue() }
        val maybelarge = if(entries.length > 700) "largecollection" else ""
        return "<div class=\"bpvalue\" $byteRangeDataTags><div class=\"oparray $maybelarge\">$entries</div></div>"
    }
}

data class OPDict(val values: Map<OpackObject, OpackObject>, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = values.toString()

    override fun renderHtmlValue(): String {
        if(values.isEmpty())
            return "<div class=\"bpvalue\">{∅}</div>"
        val entries = values.toList().joinToString(""){ "<div>${it.first.renderHtmlValue()}<span>→</span> ${it.second.renderHtmlValue()}</div>" }
        return "<div class=\"bpvalue\" $byteRangeDataTags><div class=\"opdict\">$entries</div></div>"
    }
}