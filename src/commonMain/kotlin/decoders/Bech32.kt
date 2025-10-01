package decoders

import bitmage.decodeBase32
import bitmage.hex

object Bech32: ByteWitchDecoder {
    override val name = "bech32"
    private val validator = Regex("^.{1,83}1[02-9ac-hj-np-zAC-HJ-NP-Z]{6,}$")

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        val string = data.decodeToString()
        if(string matches validator) {
            return Pair(1.0, null)
        }
        return Pair(0.0, null)
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val string = data.decodeToString()
        val dataPart = string.split("1").last()
        val humanReadable = string.removeSuffix("1$dataPart")

        val alphabet = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
        val values = dataPart.lowercase().map { alphabet.indexOf(it) }

        Logger.log("$humanReadable $dataPart")
        check(verifyChecksum(humanReadable.lowercase(), values)) { "invalid bech32 checksum" }

        val decoded = decodeBase32(values.subList(0, values.size-6))
        return Bech32Result(humanReadable, decoded, Pair(sourceOffset, sourceOffset+data.size))
    }

    private fun polymod(values: List<Int>): Int {
        val gen = listOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        var chk = 1
        values.forEach { v ->
            val b = chk shr 25
            chk = (chk and 0x1ffffff) shl 5 xor v
            for (i in 0..4) {
                if((b shr i) and 1 == 1)
                    chk = chk xor (gen[i])
            }
        }

        return chk
    }

    private fun hrpExpand(hrp: String): List<Int> {
        return hrp.map { it.code shr 5 } + listOf(0) + hrp.map { it.code and 31 }
    }

    private fun verifyChecksum(hrp: String, data: List<Int>): Boolean {
        val expanded = hrpExpand(hrp)
        val chk = polymod(expanded + data)
        Logger.log(chk)
        return chk == 1
    }
}

data class Bech32Result(
    val hrp: String,
    val data: ByteArray,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC
    override fun renderHTML(): String {
        val decode = ByteWitch.quickDecode(data, Int.MIN_VALUE)
        val payloadHTML = wrapIfDifferentColour(decode, data, rangeTagsFor(sourceByteRange.first + hrp.length + 1, sourceByteRange.second-6))

        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\" ${relativeRangeTags(0, hrp.length)}>$hrp</div>$payloadHTML</div>"
    }

}