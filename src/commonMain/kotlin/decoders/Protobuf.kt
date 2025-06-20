package decoders

import ByteWitch
import Date
import bitmage.*
import currentTimestamp
import htmlEscape
import looksLikeUtf8String
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

class ProtobufParser {

    companion object : ByteWitchDecoder {
        override val name = "protobuf"
        override fun decode(data: ByteArray, sourceOffset: Int,  inlineDisplay: Boolean) = ProtobufParser().parse(data, sourceOffset)

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            var offsetSearchStart = 0

            while(offsetSearchStart < 3) {
                Logger.log("trying to decode as protobuf with offset search at $offsetSearchStart")
                try {
                    val startOffset = findPlausibleStartOffset(data.fromIndex(offsetSearchStart))
                    val effectiveStartOffset = offsetSearchStart + startOffset
                    //Logger.log("divined start offset $effectiveStartOffset")
                    val prefix = data.sliceArray(0 until effectiveStartOffset)

                    val parser = ProtobufParser()
                    val result = parser.parse(data.fromIndex(effectiveStartOffset), effectiveStartOffset)
                    Logger.log("Parsed protobuf accounts for ${((parser.offset.toDouble() / data.size)*100).roundToInt()}% of input bytes")

                    // parsed protobuf should at least represent 30% of the input bytes
                    if(parser.offset < data.size * 0.3) {
                        offsetSearchStart += 1
                        continue
                    }

                    return if(parser.fullyParsed && effectiveStartOffset == 0) {
                        result
                    }
                    else {
                        PartialDecode(prefix, result, data.fromIndex(parser.offset+effectiveStartOffset), Pair(0, data.size))
                    }
                } catch (e: Exception) {
                    Logger.log(e.toString())
                }

                offsetSearchStart += 1
            }

            return null
        }

        private fun findPlausibleStartOffset(data: ByteArray): Int {
            // we're looking for a start tag that encodes a plausibly low field number with a valid field type
            var offsetCandidate = -1
            var continueFlag: Boolean
            var varintOffset: Int
            val numberBytes = mutableListOf<Int>()

            // search 50 bytes deep at max
            outer@ while(offsetCandidate < 50) {
                offsetCandidate += 1
                varintOffset = offsetCandidate
                numberBytes.clear()
                continueFlag = true

                while(continueFlag) {
                    val byte = data[varintOffset].toInt()
                    continueFlag = (byte and 0x80) != 0
                    numberBytes.add(byte and 0x7f)
                    varintOffset += 1

                    // varint is implausibly long
                    if(numberBytes.size > 1 && continueFlag)
                        continue@outer
                }

                // little endian
                numberBytes.reverse()

                var assembled = 0
                numberBytes.forEach {
                    assembled = assembled shl 7
                    assembled = assembled or it
                }

                val field = assembled shr 3
                val type = when(assembled and 0x07) {
                    0 -> ProtobufField.VARINT
                    1 -> ProtobufField.I64
                    2 -> ProtobufField.LEN
                    5 -> ProtobufField.I32
                    else -> ProtobufField.INVALID
                }

                // plausible candidate found!
                if(field < 30 && type != ProtobufField.INVALID)
                    return offsetCandidate
            }

            return 0
        }

        override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
            try {
                val parser = ProtobufParser()
                val parsed = parser.parse(data, sourceOffset)

                if(!parser.fullyParsed)
                    return Pair(0.0, null)

                val fieldNumberPenalty = if(parsed.objs.size < 3) -0.4 else 0.0
                val highFieldPenalty = if(parsed.objs.keys.any { it > 200 }) -0.4 else 0.0

                // short valid protobuf sequences may well be false positives, the longer the sequence the more sure we are
                // (might factor in plausible field number ranges here in the future)
                return Pair(min(data.size.toDouble() / 10, 1.0) + fieldNumberPenalty + highFieldPenalty, parsed)
            } catch (e: Exception) {
                return Pair(0.0, null)
            }
        }
    }


    private lateinit var bytes: ByteArray
    private var offset = 0
    private var sourceOffset = 0

    val fullyParsed: Boolean
        get() = offset >= bytes.size-1


    fun parse(bytes: ByteArray, sourceOffset: Int): ProtoBuf {
        this.sourceOffset = sourceOffset
        this.bytes = bytes
        offset = 0
        val result = mutableMapOf<Int, MutableList<ProtoValue>>()

        while(offset < bytes.size) {
            val start = sourceOffset+offset
            val info = readTag()
            val type = info.second
            val fieldNo = info.first

            val value = when(type) {
                ProtobufField.I32 -> readI32(start)
                ProtobufField.I64 -> readI64(start)
                ProtobufField.VARINT -> ProtoVarInt(readVarInt(), Pair(start, sourceOffset+offset))
                ProtobufField.LEN -> guessVarLenValue(readLen(start))
                // abort with partial parse on invalid field tag
                else -> {
                    // we probably read a garbage varint, so we'll reset the offset to before that
                    offset = start-sourceOffset
                    return ProtoBuf(result, bytes, Pair(sourceOffset, start))
                }
            }

            if(!result.containsKey(fieldNo))
                result[fieldNo] = mutableListOf()
            result[fieldNo]!!.add(value)
        }

        return ProtoBuf(result, bytes, Pair(sourceOffset, sourceOffset+bytes.size))
    }

    private fun readTag(): Pair<Int, ProtobufField> {
        val tag = readVarInt(expectInt = true).toInt()
        val field = tag shr 3
        val type = when(tag and 0x07) {
            0 -> ProtobufField.VARINT
            1 -> ProtobufField.I64
            2 -> ProtobufField.LEN
            //3 -> net.rec0de.android.watchwitch.decoders.protobuf.ProtobufField.SGROUP
            //4 -> net.rec0de.android.watchwitch.decoders.protobuf.ProtobufField.EGROUP
            5 -> ProtobufField.I32
            else -> ProtobufField.INVALID
        }

        return Pair(field, type)
    }

    private fun readI32(bytePos: Int): ProtoI32 {
        val b = bytes.sliceArray(offset until offset+4)
        offset += 4
        return ProtoI32(Int.fromBytes(b, ByteOrder.LITTLE), Pair(bytePos, sourceOffset+offset))
    }

    private fun readI64(bytePos: Int): ProtoI64 {
        val b = bytes.sliceArray(offset until offset+8)
        offset += 8
        return ProtoI64(Long.fromBytes(b, ByteOrder.LITTLE), Pair(bytePos, sourceOffset+offset))
    }

    private fun readLen(bytePos: Int): ProtoLen {
        val length = readVarInt(expectInt = true)
        val data = bytes.sliceArray(offset until offset+length.toInt())
        offset += length.toInt()
        return ProtoLen(data, Pair(bytePos, sourceOffset+offset))
    }

    private fun guessVarLenValue(data: ProtoLen): ProtoValue {
        // short ascii strings are sometimes hard to distinguish from valid protobufs

        // try decoding as string
        try {
            // first character should be printable ascii
            if(looksLikeUtf8String(data.value, enableLengthBias = false) > 0.95 && data.value[0] in 33..122)
                return ProtoString(data.value.decodeToString(), data.sourceByteRange)
        } catch(_: Exception) {}

        // try decoding as nested protobuf
        try {
            val parser = ProtobufParser()
            val nested = parser.parse(data.value, (data.sourceByteRange.second) - data.value.size)
            // we sometimes get spurious UUIDs that are valid protobufs and get misclassified
            // checking that field ids are in sane ranges should help avoid that
            if(parser.fullyParsed && nested.objs.keys.all { it in 1..99 })
                return nested
        } catch (_: Exception) { }

        // try decoding as string with lower threshold
        try {
            if(looksLikeUtf8String(data.value) > 0.6)
                return ProtoString(data.value.decodeToString(), data.sourceByteRange)
        } catch(_: Exception) {}

        return data
    }

    private fun readVarInt(expectInt: Boolean = false): Long {
        var continueFlag = true
        val numberBytes = mutableListOf<Int>()

        while(continueFlag) {
            val byte = bytes[offset].toInt()
            continueFlag = (byte and 0x80) != 0
            val value = byte and 0x7f
            numberBytes.add(value)
            offset += 1
        }

        // little endian
        numberBytes.reverse()

        // the largest 64bit ints would result in 10 byte varints (32bit ints -> 5 byte varint)
        check(numberBytes.size <= 5 || (!expectInt && numberBytes.size <= 10)){ "overly long varint: ${numberBytes.size} bytes" }

        // we might clip the top bits of a >64bit int here
        var assembled = 0L
        numberBytes.forEach {
            assembled = assembled shl 7
            assembled = assembled or it.toLong()
        }

        if(expectInt && (assembled > Int.MAX_VALUE || assembled < Int.MIN_VALUE))
            throw Exception("overly long varint: ${numberBytes.size} bytes, $assembled (exceeds expected int32 value)")

        return assembled
    }
}

enum class ProtobufField {
    VARINT, I64, LEN, I32, INVALID
}

interface ProtoValue : ByteWitchResult {
    val wireType: Int

    fun renderWithFieldId(fieldId: Int): ByteArray {
        val tag = (fieldId.toLong() shl 3) or (wireType and 0x03).toLong()
        return renderAsVarInt(tag) + render()
    }

    fun render(): ByteArray

    fun renderAsVarInt(v: Long): ByteArray {
        var bytes = byteArrayOf()
        var remaining = v

        while(remaining > 0x7F) {
            bytes += ((remaining and 0x7F) or 0x80).toByte() // Take lowest 7 bit and encode with continuation flag
            remaining = remaining shr 7
        }

        bytes += (remaining and 0x7F).toByte()
        return bytes
    }
}

class ProtoBuf(val objs: Map<Int, List<ProtoValue>>, val bytes: ByteArray = byteArrayOf(), sourceByteRange: Pair<Int, Int>) : ProtoLen(bytes, sourceByteRange) {
    override fun toString() = "Protobuf($objs)"

    override fun asProtoBuf() = this

    fun renderStandalone(): ByteArray {
        val fieldRecords = objs.map { field ->
            val fieldId = field.key
            val renderedRecords = field.value.map { it.renderWithFieldId(fieldId) }
            renderedRecords.fold(byteArrayOf()){ acc, new -> acc + new }
        }

        return fieldRecords.fold(byteArrayOf()){ acc, new -> acc + new }
    }

    override fun renderHTML(): String {
        var renderedContent = objs.toList().map { fieldContents ->
            val renderedFieldContents = fieldContents.second.joinToString("") {
                when(it) {
                    is ProtoBuf, is ProtoBPList -> it.renderHTML()
                    is ProtoString -> {
                        val veryLong = if(it.stringValue.length > 300) " data" else ""
                        "<div class=\"protovalue stringlit$veryLong\" ${it.byteRangeDataTags}>${it.renderHTML()}</div>"
                    }
                    is ProtoLen -> {
                        val decode = ByteWitch.quickDecode(it.value, it.sourceByteRange.second - it.value.size)

                        // we have to wrap in a protovalue if we have a nested decode of the same type to distinguish them visually
                        // for nested decodes of different types we can omit it for cleaner display
                        val requiresWrapping = decode == null || decode is ProtoValue

                        val prePayload = if(requiresWrapping) "<div class=\"protovalue data\" ${it.byteRangeDataTags}>" else ""
                        val postPayload = if(requiresWrapping) "</div>" else ""
                        val payloadHTML = decode?.renderHTML() ?: it.renderHTML()

                        "$prePayload$payloadHTML$postPayload"
                    }
                    else -> "<div class=\"protovalue\" ${it.byteRangeDataTags}>${it.renderHTML()}</div>"
                }
            }
            "<div class=\"protofield roundbox\"><span>${fieldContents.first}</span><div>${renderedFieldContents}</div></div>"
        }

        if(renderedContent.isEmpty()) {
            renderedContent = listOf("∅")
        }

        return "<div class=\"protobuf roundbox\" $byteRangeDataTags>${renderedContent.joinToString("")}</div>"
    }

    override fun render(): ByteArray {
        val bytes = renderStandalone()
        return renderAsVarInt(bytes.size.toLong()) + bytes // protobuf as a substructure is length delimited
    }
}

data class ProtoI32(val value: Int, override val sourceByteRange: Pair<Int, Int>) : ProtoValue {
    override val wireType = 5
    override fun toString() = "I32($value)"

    override fun render() = value.toBytes(ByteOrder.LITTLE)

    override fun renderHTML() = "I32(int: $value float: ${asFloat()})"

    fun asFloat(): Float = value.toBytes(ByteOrder.BIG).readFloat(ByteOrder.BIG)
}

data class ProtoI64(val value: Long, override val sourceByteRange: Pair<Int, Int>) : ProtoValue {

    override val wireType = 1
    override fun toString() = "I64($value)"

    override fun renderHTML(): String {
        val intv = value
        val floatv = asDouble()

        val currentTime = currentTimestamp()
        val diffUnixEpoch = ((floatv*1000).toLong() - currentTime).absoluteValue
        val diffAppleEpoch = ((floatv*1000).toLong() + 978307200000L - currentTime).absoluteValue

        return when {
            // small floats close to integer numbers are probably intended to be floats
            (floatv - floatv.roundToInt() <= 0.00001) && floatv.absoluteValue < 1000 -> "I64($floatv)"
            // small integer values are probably not doubles in disguise
            intv.absoluteValue < 100 -> "I64($intv)"
            // timestamps in reasonable date ranges are probably dates
            diffUnixEpoch < 1000L*60*60*24*365*10 -> "I64(unix time ${asDate(false)})"
            diffAppleEpoch < 1000L*60*60*24*365*10 -> "I64(apple time ${asDate(true)})"
            else -> "I64(int: $intv double: $floatv date: ${asDate()} / ${asDate(false)})"
        }
    }

    override fun render() = value.toBytes(ByteOrder.LITTLE)

    fun asDouble(): Double = value.toBytes(ByteOrder.BIG).readDouble(ByteOrder.BIG)

    /**
     * Assume this I64 value represents a double containing an NSDate timestamp (seconds since Jan 01 2001)
     * and turn it into a Date object
     */
    fun asDate(appleEpoch: Boolean = true): Date {
        val timestamp = asDouble()
        val offset = if(appleEpoch) 978307200000L else 0L
        return Date((timestamp*1000).toLong() + offset)
    }
}

data class ProtoVarInt(val value: Long, override val sourceByteRange: Pair<Int, Int>) : ProtoValue {

    override val wireType = 0
    override fun toString() = "VarInt($value)"

    override fun renderHTML() = toString()

    override fun render() = renderAsVarInt(value)
}

open class ProtoLen(val value: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ProtoValue {
    override val wireType = 2 // LEN
    override fun toString() = "LEN(${value.hex()})"

    override fun render(): ByteArray {
        return renderAsVarInt(value.size.toLong()) + value
    }

    override fun renderHTML() = "0x${value.hex()}"

    open fun asString() = value.decodeToString()
    open fun asProtoBuf() = ProtobufParser().parse(value, sourceByteRange.first)
}

class ProtoString(val stringValue: String, sourceByteRange: Pair<Int, Int>) : ProtoLen(stringValue.encodeToByteArray(), sourceByteRange) {
    override fun toString() = "String($stringValue)"

    override fun render(): ByteArray {
        val bytes = stringValue.encodeToByteArray()
        return renderAsVarInt(bytes.size.toLong()) + bytes
    }

    override fun renderHTML() = "\"${htmlEscape(stringValue)}\""

    override fun asString() = stringValue
}

class ProtoBPList(value: ByteArray, sourceByteRange: Pair<Int, Int>) : ProtoLen(value, sourceByteRange) {
    val parsed = BPListParser().parse(value, (sourceByteRange.second) - value.size)
    override fun toString() = "bplist($parsed)"

    override fun renderHTML() = parsed.renderHTML()

    override fun render(): ByteArray {
        return renderAsVarInt(value.size.toLong()) + value
    }
}