package decoders

import ByteWitch
import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.fromIndex
import bitmage.hex

object GenericTLV : ByteWitchDecoder {
    override val name = "generic-tlv"

    override fun decodesAsValid(data: ByteArray): Boolean {
        if(data.size < 3)
            return false
        return testLengthPrefixAtOffset(data, 0) != null || testLengthPrefixAtOffset(data, 1) != null || testLengthPrefixAtOffset(data, 2) != null
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

    override fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {
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

data class GenericTLVResult(
    val type: ByteArray,
    val length: ByteArray,
    val value: ByteArray,
    val byteOrder: ByteOrder,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {

    private val encodedLength = Int.fromBytes(length, byteOrder)
    private val endian = if(byteOrder == ByteOrder.BIG) "big" else "little"
    private val valueHTML = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)?.renderHTML() ?: "<div class=\"bpvalue data\">${value.hex()}</div>"
    override fun renderHTML() = "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\">Type 0x${type.hex()}</div><div class=\"bpvalue\">Len: ${encodedLength} (${length.size} B, $endian endian)</div>$valueHTML</div>"

}