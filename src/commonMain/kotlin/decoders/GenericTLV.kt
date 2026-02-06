package decoders

import ByteWitch
import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.fromIndex
import bitmage.hex
import bitmage.untilIndex
import kotlin.math.min

object GenericTLV : ByteWitchDecoder {
    override val name = "generic-tlv"

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        if(data.size < 3)
            return Pair(0.0, null)

        val lengthPrefixesOneByteType = testLengthPrefixAtOffset(data, 1)
        val lengthPrefixesTwoByteType = testLengthPrefixAtOffset(data, 2)

        if(lengthPrefixesOneByteType == null && lengthPrefixesTwoByteType == null)
            return Pair(0.0, null)

        val typeLen: Int
        val validPrefix = if(lengthPrefixesOneByteType != null)  {
            typeLen = 1
            lengthPrefixesOneByteType
        }
        else {
            typeLen = 2
            lengthPrefixesTwoByteType!!
        }

        val payloadLen = validPrefix.first
        val actualData = data.size - typeLen - validPrefix.second
        val zeroPayloadLengthPenalty = if(actualData == 0) 0.5 else 0.0
        val zeroTypePenalty = if(Int.fromBytes(data.untilIndex(typeLen), ByteOrder.BIG) == 0) 0.2 else 0.0
        val oddLengthPenalty = if(validPrefix.second == 3) 0.15 else 0.0

        val lengthBias = min((payloadLen+1).toDouble()/6, 1.0)
        val score = lengthBias - zeroPayloadLengthPenalty - zeroTypePenalty - oddLengthPenalty

        // TLVs with a payload length of less than 5 bytes are considered uncertain decodings
        // (add 1 to distinguish zero-length TLVs from completely invalid decodes)
        return Pair(score, null)
    }

    private fun testLengthPrefixAtOffset(bytes: ByteArray, offset: Int): Triple<Int, Int, ByteOrder>? {
        // length values may encode: total length, length without type tag, length without type tag and size field
        val expectedLengths = setOf(bytes.size, bytes.size - offset)
        val postOffset = bytes.fromIndex(offset)

        val parsedLengths = mutableListOf<Triple<Int, Int, ByteOrder>>()
        parsedLengths.add(Triple(Int.fromBytes(postOffset.sliceArray(0 until 1), ByteOrder.BIG), 1, ByteOrder.BIG))

        if(postOffset.size > 1) {
            parsedLengths.add(Triple(Int.fromBytes(postOffset.sliceArray(0 until 2), ByteOrder.BIG), 2, ByteOrder.BIG))
            parsedLengths.add(Triple(Int.fromBytes(postOffset.sliceArray(0 until 2), ByteOrder.LITTLE), 2, ByteOrder.LITTLE))
        }

        if(postOffset.size > 2) {
            parsedLengths.add(Triple(Int.fromBytes(postOffset.sliceArray(0 until 3), ByteOrder.BIG), 3, ByteOrder.BIG))
            parsedLengths.add(Triple(Int.fromBytes(postOffset.sliceArray(0 until 3), ByteOrder.LITTLE), 3, ByteOrder.LITTLE))
        }

        if(postOffset.size > 3) {
            parsedLengths.add(Triple(Int.fromBytes(postOffset.sliceArray(0 until 4), ByteOrder.BIG), 4, ByteOrder.BIG))
            parsedLengths.add(Triple(Int.fromBytes(postOffset.sliceArray(0 until 4), ByteOrder.LITTLE), 4, ByteOrder.LITTLE))
        }

        // allow length field to not be included in encoded length
        return parsedLengths.firstOrNull { (it.first in expectedLengths) || (it.first + it.second in expectedLengths)}
    }

    override fun decode(data: ByteArray, sourceOffset: Int,  inlineDisplay: Boolean): ByteWitchResult {
        val params = listOf(1, 2, 0).map { Pair(it, testLengthPrefixAtOffset(data, it)) }.first { it.second != null }

        val typeLength = params.first
        val lengthLength = params.second!!.second
        val lengthByteOrder = params.second!!.third

        val remaining = data.fromIndex(typeLength + lengthLength)

        return GenericTLVResult(
            data.sliceArray(0 until typeLength),
            data.sliceArray(typeLength until typeLength + lengthLength),
            remaining,
            lengthByteOrder,
            Pair(sourceOffset, sourceOffset+data.size)
        )

    }
}

class GenericTLVResult(
    val type: ByteArray,
    val length: ByteArray,
    val value: ByteArray,
    val byteOrder: ByteOrder,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val encodedLength = Int.fromBytes(length, byteOrder)
        val endian = if(byteOrder == ByteOrder.BIG) "big" else "little"
        val decode = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)

        val payloadHTML = wrapIfDifferentColour(decode, value, relativeRangeTags(type.size+length.size, value.size))
        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\">Type 0x${type.hex()}</div><div class=\"bpvalue\">Len: $encodedLength (${length.size} B, $endian endian)</div>$payloadHTML</div>"
    }

}