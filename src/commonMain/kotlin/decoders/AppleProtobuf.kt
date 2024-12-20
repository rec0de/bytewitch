package decoders

import ParseCompanion
import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.fromIndex
import looksLikeUtf8String

class AppleProtobuf : ParseCompanion() {

    companion object : ByteWitchDecoder {
        override val name = "Apple ProtocolBuffer"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            val decoder = AppleProtobuf()
            val result = decoder.decode(data, sourceOffset)
            check(decoder.parseOffset >= data.size-1){ "input data not fully consumed" }
            return result
        }
    }

    private fun decode(bytes: ByteArray, sourceOffset: Int) : ByteWitchResult {
        parseOffset = 0
        val startOffset = parseOffset
        val version = readInt(bytes, 2)
        check(version == 1){ "expecting version 1 in first bytes, got $version" }

        // try to see if we have metadata strings or not
        val localeLen = UInt.fromBytes(bytes.sliceArray(parseOffset until parseOffset+2), ByteOrder.BIG)
        val hasMetadata = localeLen >= 2u && localeLen < 64u && looksLikeUtf8String(bytes.sliceArray(3 until 3+localeLen.toInt())) > 0.4

        val metadata = if(hasMetadata) {
            var stringStart = parseOffset
            val locale = BWString(
                readLengthPrefixedString(bytes, 2)!!,
                Pair(sourceOffset + stringStart, sourceOffset + parseOffset)
            )
            stringStart = parseOffset
            val application = BWString(
                readLengthPrefixedString(bytes, 2)!!,
                Pair(sourceOffset + stringStart, sourceOffset + parseOffset)
            )
            stringStart = parseOffset
            val osVersion = BWString(
                readLengthPrefixedString(bytes, 2)!!,
                Pair(sourceOffset + stringStart, sourceOffset + parseOffset)
            )
            Triple(locale, application, osVersion)
        }
        else
            null

        val tlvs = mutableListOf<AppleProtobufTLV>()

        while(parseOffset < bytes.size - 1) {
            tlvs.add(readTLV(bytes, sourceOffset))
        }

        return AppleProtocolBuffer(metadata, tlvs, Pair(sourceOffset+startOffset, sourceOffset+parseOffset))
    }


    private fun readTLV(bytes: ByteArray, sourceOffset: Int) : AppleProtobufTLV {
        check(bytes.size - parseOffset >= 8) { "not enough bytes to read TLV" }

        val startOffset = parseOffset
        val type = readInt(bytes, 4)
        val length = readUInt(bytes, 4)

        check(length <= bytes.size.toUInt() - parseOffset.toUInt()) { "excessive TLV length $length with ${bytes.size - parseOffset} bytes remaining" }

        val protobufStart = parseOffset
        val protobuf = readBytes(bytes, length.toInt())
        val decoded = ProtobufParser().parse(protobuf, sourceOffset+protobufStart)

        return AppleProtobufTLV(type, length.toInt(), decoded, Pair(sourceOffset+startOffset, sourceOffset+parseOffset))
    }
}

data class AppleProtocolBuffer(
    val strings: Triple<BWString, BWString, BWString>?,
    val tlvs: List<AppleProtobufTLV>,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {
    override fun renderHTML(): String {
        val metadataHTML = strings?.toList()?.joinToString("") { it.renderHTML() } ?: ""
        return "<div class=\"generic roundbox\" $byteRangeDataTags>$metadataHTML${tlvs.joinToString("") { "<div class=\"bpvalue data\">${it.renderHTML()}</div>" }}</div>"
    }
}

data class AppleProtobufTLV(
    val type: Int,
    val length: Int,
    val value: ProtoBuf,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {
    override fun renderHTML(): String {
        val payloadHTML = value.renderHTML()
        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\">Type ${type}</div><div class=\"bpvalue\">Len: $length</div>$payloadHTML</div>"
    }
}