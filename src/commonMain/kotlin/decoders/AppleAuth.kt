package decoders

import Date
import ParseCompanion
import bitmage.ByteOrder
import bitmage.fromIndex
import bitmage.hex

object AppleAuth : ByteWitchDecoder, ParseCompanion() {
    override val name = "Apple Auth"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        return when(data.size) {
            in 24..64 -> decodeAnisette(data, sourceOffset)
            in 100..256 -> decodeToken(data, sourceOffset)
            else -> throw Exception("invalid length for apple auth")
        }
    }

    private fun decodeAnisette(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        // 00000005 00000010 7b92ab616d3245b65cdc12726305ca7b 00000004 // SideStore / Apple Music Android
        // 00000005 00000010 8c37b82fd01b312bccba2e03ee48dd56 00000003 // iOS 15.7.3
        // 00000005 00000010 e1b1ab79f61ac1f56f26eb3fc720c4e2 00000003 // watchOS 7.3.3
        val start = sourceOffset + parseOffset

        // conservative length estimate, real data should be either 24 or 28 B
        check(data.size in 24..64)

        val version = readUInt(data, 4, ByteOrder.BIG).toInt()
        check(version in 4..16) // expecting 4 or 5 but let's be future-proof

        val dataLen = readUInt(data, 4, ByteOrder.BIG).toInt()
        check(dataLen >= 16)

        val authData = readBytes(data, dataLen)

        // value is apparently sign | value encoded
        val platform = if(version >= 5) {
            val raw = readUInt(data, 4, ByteOrder.BIG)
            val sign = (raw shr 31) == 0u
            val rest = raw and 0x7fffffffu
            if(sign) rest.toInt() else - rest.toInt()
        }
        else
            null

        check(parseOffset == data.size) { "anisette decode has data left over" }
        return Anisette(version, platform, authData, Pair(start, sourceOffset+parseOffset))
    }

    // this is mostly guesswork
    private fun decodeToken(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        val start = sourceOffset + parseOffset
        val firstLen = readInt(data, 1)
        check(firstLen == 0x10) { "unusual auth token first length: $firstLen" }
        val u1 = readInt(data, 2, explicitlySigned = false, ByteOrder.BIG)
        val u2 = readInt(data, 4, explicitlySigned = false, ByteOrder.BIG)
        val u3 = data[parseOffset].toUByte().toUInt().toInt()
        parseOffset += 1
        val timestampLen = readInt(data, 1)
        val timestamp = readLong(data, timestampLen, ByteOrder.BIG)
        check(timestamp in 1000000000..<5000000000L) // plausible timestamp
        val date = Date(timestamp*1000) // convert timestamp to ms

        val secondLen = readInt(data, 1)
        val secondData = readBytes(data, secondLen)

        val nameLen = secondData[0].toInt()
        check(nameLen < secondData.size)
        val name = secondData.sliceArray(1 until 1+nameLen).decodeToString()
        val u4 = secondData.fromIndex(1+nameLen)

        val rest = data.fromIndex(parseOffset)
        parseOffset += rest.size

        return GSToken(u1, u2, u3, date, name, u4, rest, Pair(start, sourceOffset+parseOffset))
    }

}

class Anisette(val version: Int, val platform: Int?, val authData: ByteArray, override val sourceByteRange: Pair<Int, Int>?) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC
    private val platformLookup = mapOf<Int, String>(3 to "iOS/watchOS?", 4 to "Android/AppleMusic?")

    override fun renderHTML(): String {
        val platformHtml = if(platform != null) "<div class=\"bwvalue\" ${relativeRangeTags(8+authData.size, 4)}>Platform: $platform (${platformLookup.getOrElse(platform){ "unknown" }})</div>" else ""
        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bwvalue\" ${relativeRangeTags(0, 4)}>Anisette Version $version</div><div class=\"bwvalue data\" ${relativeRangeTags(8, authData.size)}>Data: 0x${authData.hex()}</div>$platformHtml</div>"
    }
}

class GSToken(val u1: Int, val u2: Int, val u3: Int, val timestamp: Date, val name: String, val u4: ByteArray, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val firstUnknowns = "<div class=\"bwvalue\" ${relativeRangeTags(1, 2)}>$u1</div> <div class=\"bwvalue\" ${relativeRangeTags(3, 4)}>$u2</div> <div class=\"bwvalue\" ${relativeRangeTags(7, 1)}>0x${u3.toString(16)}</div>"
        val timestampHtml = "<div class=\"bwvalue\" ${relativeRangeTags(9, 8)}>Expires $timestamp</div>"
        val nameHtml = "<div class=\"bwvalue\" ${relativeRangeTags(19, name.length)}>Type $name</div>"
        val secondUnknown = "<div class=\"bwvalue data\" ${relativeRangeTags(19+name.length, u4.size)}>0x${u4.hex()}</div>"
        val data = "<div class=\"bwvalue data\" ${rangeTagsFor(sourceByteRange.second - data.size, sourceByteRange.second)}>0x${data.hex()}</div>"
        return "<div class=\"roundbox generic\" $byteRangeDataTags>$firstUnknowns $timestampHtml $nameHtml $secondUnknown $data</div>"
    }
}