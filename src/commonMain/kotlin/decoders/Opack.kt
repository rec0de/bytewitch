package decoders

import ByteWitch
import Date
import ParseCompanion
import bitmage.*
import currentTimestamp
import dateFromAppleTimestamp
import htmlEscape
import looksLikeUtf8String
import kotlin.math.absoluteValue


class OpackParser : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "opack"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
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

        // single bytes are often false-positive detected as booleans
        override fun decodesAsValid(data: ByteArray): Pair<Boolean, ByteWitchResult?> {
            if(data.size < 3)
                return Pair(false, null)
            return super.decodesAsValid(data)
        }
    }

    private var sourceOffset = 0

    private val lastConsumedBytePosition: Int
        get() = sourceOffset + parseOffset

    fun parseTopLevel(bytes: ByteArray, sourceOffsetParam: Int): OpackObject {
        parseOffset = 0
        sourceOffset = sourceOffsetParam
        val result = parse(bytes)

        check(parseOffset >= bytes.size){ "input data not fully consumed" }

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

        // NOTE: deviation from prior documentation ("Analyzing Apple’s private wireless communication protocols with a focus on security and privacy")
        // instead of linar byte length increments (1, 2, 3, 4) it seems apple uses exponential increments (1, 2, 4, 8)
        // is this an error in the documentation, or do both versions exist?
        // we also assume these are all signed - i don't actually know if that's the case, but we also don't have a counterexample
        return when(type) {
            in 0x08..0x2f -> OPInt((type - 8).toLong(), Pair(start, lastConsumedBytePosition))
            0x30 -> OPInt(readInt(bytes, 1, explicitlySigned = true), Pair(start, lastConsumedBytePosition))
            0x31 -> OPInt(readInt(bytes, 2, explicitlySigned = true, byteOrder = ByteOrder.LITTLE), Pair(start, lastConsumedBytePosition))
            0x32 -> OPInt(readInt(bytes, 4, explicitlySigned = true, byteOrder = ByteOrder.LITTLE), Pair(start, lastConsumedBytePosition))
            0x33 -> OPInt(readLong(bytes, 8, byteOrder = ByteOrder.LITTLE), Pair(start, lastConsumedBytePosition))
            else -> throw Exception("Unexpected OPACK int ${bytes.hex()}")
        }
    }

    private fun parseAsFloat(bytes: ByteArray): OPReal {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)
        when(type) {
            0x35 -> {
                return OPReal(readBytes(bytes, 4).readFloat(ByteOrder.LITTLE).toDouble(), Pair(start, lastConsumedBytePosition))
            }
            0x36 -> {
                return OPReal(readBytes(bytes, 8).readDouble(ByteOrder.LITTLE), Pair(start, lastConsumedBytePosition))
            }
            else -> throw Exception("Unexpected OPACK float ${bytes.hex()}")
        }
    }

    private fun parseAsString(bytes: ByteArray): OPString {
        val start = sourceOffset + parseOffset
        val type = readInt(bytes, 1)

        val length = when(type) {
            in 0x40..0x60 -> type - 0x40
            0x61 -> readInt(bytes, 1)
            0x62 -> readInt(bytes, 2, byteOrder = ByteOrder.LITTLE)
            0x63 -> readInt(bytes, 3, byteOrder = ByteOrder.LITTLE)
            0x64 -> readInt(bytes, 4, byteOrder = ByteOrder.LITTLE)
            else -> throw Exception("Unexpected OPACK string ${bytes.hex()}")
        }

        val stringBytes = readBytes(bytes, length)
        check(looksLikeUtf8String(stringBytes, false) > 0.5) { "OPString has implausible string content: ${stringBytes.hex()}" }
        return OPString(stringBytes.decodeToString(), Pair(start, lastConsumedBytePosition))
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
                val length = readInt(bytes, 2, byteOrder = ByteOrder.LITTLE)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }
            0x93 -> {
                val length = readInt(bytes, 3, byteOrder = ByteOrder.LITTLE)
                return OPData(readBytes(bytes, length), Pair(start, lastConsumedBytePosition))
            }
            0x94 -> {
                val length = readInt(bytes, 4, byteOrder = ByteOrder.LITTLE)
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
    override val sourceByteRange = Pair(bytePosition, bytePosition+1)
    override fun toString() = "true"
}

class OPFalse(bytePosition: Int) : OpackObject() {
    override val sourceByteRange = Pair(bytePosition, bytePosition+1)
    override fun toString() = "false"
}

class OPNull(bytePosition: Int) : OpackObject() {
    override val sourceByteRange = Pair(bytePosition, bytePosition+1)
    override fun toString() = "null"
}

class OPUndefined(bytePosition: Int) : OpackObject() {
    override val sourceByteRange = Pair(bytePosition, bytePosition+1)
    override fun toString() = "undefined"
}

class CborEndMarker(bytePosition: Int) : OpackObject() {
    override val sourceByteRange = Pair(bytePosition, bytePosition+1)
    override fun toString() = "end"
}

data class OPInt(val value: Long, override val sourceByteRange: Pair<Int, Int>): OpackObject() {
    constructor(value: Int, sourceByteRange: Pair<Int, Int>) : this(value.toLong(), sourceByteRange)
    override fun toString() = value.toString()
}

data class OPUInt(val value: ULong, override val sourceByteRange: Pair<Int, Int>): OpackObject() {
    constructor(value: UInt, sourceByteRange: Pair<Int, Int>) : this(value.toULong(), sourceByteRange)
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

data class OPDate(val timestamp: Double, override val sourceByteRange: Pair<Int, Int>, val isAppleEpoch: Boolean = true) : OpackObject() {
    override fun toString() = "BPDate($timestamp)"

    override fun renderHtmlValue() = "<div class=\"bpvalue\" $byteRangeDataTags>${asDate()}</div>"

    fun asDate(): Date {
        return if(isAppleEpoch) {
            dateFromAppleTimestamp(timestamp)
        }
        else {
            Date((timestamp*1000).toLong())
        }
    }
}

class OPData(val value: ByteArray, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "BPData(${value.hex()})"

    override fun renderHtmlValue(): String {
        // try to decode nested stuff
        val decode = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)

        // we have to wrap in a bpvalue if we have a nested decode of the same type to distinguish them visually
        // for nested decodes of different types we can omit it for cleaner display
        val requiresWrapping = decode == null || decode is OpackObject

        val prePayload = if(requiresWrapping) "<div class=\"bpvalue data\" $byteRangeDataTags>" else ""
        val postPayload = if(requiresWrapping) "</div>" else ""
        val payloadHTML = decode?.renderHTML() ?: "0x${value.hex()}"

        return "$prePayload$payloadHTML$postPayload"
    }
}

data class OPString(val value: String, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "\"${htmlEscape(value)}\""
}

data class OPArray(val values: List<OpackObject>, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "[${values.joinToString(", ")}]"

    override fun renderHTML(): String {
        if(values.isEmpty())
            return "<div class=\"roundbox opack\">[∅]</div>"
        val entries = values.joinToString(""){ it.renderHtmlValue() }
        val maybelarge = if(entries.length > 700) "largecollection" else ""
        return "<div class=\"roundbox opack oparray $maybelarge\" $byteRangeDataTags>$entries</div>"
    }

    override fun renderHtmlValue() = "<div class=\"bpvalue\">${renderHTML()}</div>"
}

data class OPDict(val values: Map<OpackObject, OpackObject>, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = values.toString()

    override fun renderHTML(): String {
        if(values.isEmpty())
            return "<div class=\"roundbox opack\">{∅}</div>"
        val entries = values.toList().joinToString(""){ "<div>${it.first.renderHtmlValue()}<span>→</span> ${it.second.renderHtmlValue()}</div>" }
        return "<div class=\"roundbox opack opdict\" $byteRangeDataTags>$entries</div>"
    }

    override fun renderHtmlValue() = "<div class=\"bpvalue\">${renderHTML()}</div>"
}

class OPTaggedData(val value: ByteArray, val type: Int, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "OPTaggedData($type: ${value.hex()})"

    override fun renderHtmlValue(): String {
        // try to decode nested stuff
        val decode = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)

        val payloadHTML = decode?.renderHTML() ?: "<div class=\"bpvalue data\" ${rangeTagsFor(sourceByteRange.second-value.size, sourceByteRange.second)}>0x${value.hex()}</div>"
        return "<div class=\"roundbox opack\" $byteRangeDataTags><div class=\"bpvalue\" ${rangeTagsFor(sourceByteRange.first, sourceByteRange.second-value.size)}>type $type</div>$payloadHTML</div>"
    }
}

class OPTaggedParsedData(val value: OpackObject, val type: Int, override val sourceByteRange: Pair<Int, Int>) : OpackObject() {
    override fun toString() = "OPData($type: $value)"

    override fun renderHtmlValue(): String {
        val payloadHTML = value.renderHTML()
        return "<div class=\"roundbox opack\" $byteRangeDataTags><div class=\"bpvalue\" ${rangeTagsFor(sourceByteRange.first, value.sourceByteRange!!.first)}>type $type</div>$payloadHTML</div>"
    }
}