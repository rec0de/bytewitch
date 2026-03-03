package decoders

import ByteWitch
import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.fromIndex
import bitmage.hex

object TLV8 : ByteWitchDecoder {
    override val name = "tlv8"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val tlvs = mutableListOf<TlvChainEntry>()
        var remainder = data
        var internalOffset = 0

        while(remainder.isNotEmpty()) {
            if(remainder.size < 2)
                throw Exception("Insufficient TLV8 header bytes remaining")

            val type = remainder[0].toUByte().toInt()
            val length = remainder[1].toUByte().toInt()

            if(length > remainder.size-2)
                throw Exception("Trying to read TLV8 of length $length with only ${remainder.size} bytes remaining")
            val value = remainder.sliceArray(2 until 2+length)
            remainder = remainder.fromIndex(2+length)
            tlvs.add(TlvChainEntry(type, length, value, Pair(sourceOffset+internalOffset, sourceOffset+internalOffset+2+length)))
            internalOffset += 2+length
        }

        return TlvChainResult(tlvs, Pair(sourceOffset, sourceOffset+data.size))
    }

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        try {
            val res = decode(data, sourceOffset) as TlvChainResult

            val zeroLengthTLVs = res.tlvs.count { it.length == 0 }
            val zeroLengthPenalty = (zeroLengthTLVs.toDouble() / res.tlvs.size) * 0.8

            return Pair(1.0 - zeroLengthPenalty, res)
        } catch (e: Exception) {
            return Pair(0.0, null)
        }
    }
}

object TLV16 : ByteWitchDecoder {
    override val name = "tlv16"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val tlvs = mutableListOf<TlvChainEntry>()
        var remainder = data
        var internalOffset = 0

        while(remainder.isNotEmpty()) {
            if(remainder.size < 3)
                throw Exception("Insufficient TLV16 header bytes remaining")

            val type = remainder[0].toUByte().toInt()
            val length = Int.fromBytes(remainder.sliceArray(1 ..2), ByteOrder.BIG)

            if(length > remainder.size-3)
                throw Exception("Trying to read TLV16 of length $length with only ${remainder.size} bytes remaining")
            val value = remainder.sliceArray(3 until 3+length)
            remainder = remainder.fromIndex(3+length)
            tlvs.add(TlvChainEntry(type, length, value, Pair(sourceOffset+internalOffset, sourceOffset+internalOffset+3+length), lengthLength = 2))
            internalOffset += 3+length
        }

        return TlvChainResult(tlvs, Pair(sourceOffset, sourceOffset+data.size))
    }

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        try {
            val res = decode(data, sourceOffset) as TlvChainResult

            val zeroLengthTLVs = res.tlvs.count { it.length == 0 }
            val zeroLengthPenalty = (zeroLengthTLVs.toDouble() / res.tlvs.size) * 0.8

            return Pair(1.0 - zeroLengthPenalty, res)
        } catch (e: Exception) {
            return Pair(0.0, null)
        }
    }
}

class TlvChainResult(val tlvs: List<TlvChainEntry>, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC
    override fun renderHTML(): String {
        return if(tlvs.size == 1)
            tlvs.first().renderHTML()
        else
            "<div class=\"generic roundbox\" $byteRangeDataTags>${tlvs.joinToString("") { wrapIfDifferentColour(it, "", rangeTagsFor(it.sourceByteRange.first, it.sourceByteRange.second)) }}</div>"
    }
}

class TlvChainEntry(val type: Int, val length: Int, val value: ByteArray, override val sourceByteRange: Pair<Int, Int>, private val typeLength: Int = 1, private val lengthLength: Int = 1) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC
    override fun renderHTML(): String {
        val parseAttempt = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)
        val valueHTML = wrapIfDifferentColour(parseAttempt, value, relativeRangeTags(typeLength+lengthLength, value.size))
        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bwvalue\" ${relativeRangeTags(0, typeLength)}>Type 0x${type.toString(16)}</div><div class=\"bpvalue\" ${relativeRangeTags(typeLength, lengthLength)}>Len: $length</div>$valueHTML</div>"
    }
}