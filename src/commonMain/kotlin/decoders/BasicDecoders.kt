package decoders

import bitmage.decodeAsUTF16BE
import htmlEscape
import looksLikeUtf8String

object Utf8Decoder : ByteWitchDecoder {
    override val name = "utf8"

    override fun decodesAsValid(data: ByteArray) = confidence(data) > 0.6

    override fun confidence(data: ByteArray): Double {
        try {
            val score = looksLikeUtf8String(data)
            //Logger.log(data.decodeToString())
            //Logger.log(score)
            return score
        } catch (e: Exception) {
            return 0.0
        }
    }

    override fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        return BWString(data.decodeToString(), Pair(sourceOffset, sourceOffset+data.size))
    }

    override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        return if(confidence(data) > 0.25)
            decode(data, 0)
        else
            null
    }
}

object Utf16Decoder : ByteWitchDecoder {
    override val name = "utf16"

    override fun confidence(data: ByteArray): Double {
        try {
            val string = data.decodeAsUTF16BE()
            return looksLikeUtf8String(string.encodeToByteArray())
        } catch (e: Exception) {
            return 0.0
        }
    }

    override fun decodesAsValid(data: ByteArray) = confidence(data) > 0.6

    override fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        return BWString(data.decodeAsUTF16BE(), Pair(sourceOffset, sourceOffset+data.size))
    }

    override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        return if(confidence(data) > 0.25)
            decode(data, 0)
        else
            null
    }
}

class BWString(val string: String, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML() = "<div class=\"bpvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)}</div>"
}