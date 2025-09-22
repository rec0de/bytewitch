package decoders

import bitmage.hex

object JWT: ByteWitchDecoder {
    override val name = "JWT"
    private val validator = Regex("^[A-Z0-9\\-_=]+\\.[A-Z0-9\\-_=]+\\.[A-Z0-9\\-_=]+$", RegexOption.IGNORE_CASE)

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        val string = data.decodeToString()
        if(string matches validator) {
            return Pair(1.0, null)
        }
        return Pair(0.0, null)
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val string = data.decodeToString()
        val parts = string.split(".")

        check(parts.size == 3){ "unexpected number of JWT parts: ${parts.size}" }

        val decoded = parts.map { ByteWitch.decodeBase64(it) }

        val headerEnd = sourceOffset + parts[0].encodeToByteArray().size
        val payloadEnd = headerEnd + 1 + parts[1].encodeToByteArray().size
        val header = JWTString(decoded[0].decodeToString(), Pair(sourceOffset, headerEnd))
        val payload = JWTString(decoded[1].decodeToString(), Pair(headerEnd+1, payloadEnd))
        val signature = JWTData(decoded[2], Pair(payloadEnd+1, sourceOffset+data.size))

        return JWTResult(header, payload, signature, Pair(sourceOffset, sourceOffset+data.size))
    }


}

data class JWTResult(
    val header: JWTString,
    val payload: JWTString,
    val signature: JWTData,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {

    override fun renderHTML(): String {
        return "<div class=\"roundbox generic\" $byteRangeDataTags>${header.renderHTML()} ${payload.renderHTML()} ${signature.renderHTML()}</div>"
    }
}

data class JWTString(val content: String, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"bpvalue\" $byteRangeDataTags>$content</div>"
    }
}

data class JWTData(val content: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"bpvalue data\" $byteRangeDataTags>0x${content.hex()}</div>"
    }
}