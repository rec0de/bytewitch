import bitmage.fromHex
import bitmage.hex
import decoders.*

object ByteWitch {
    private val decoders = listOf<ByteWitchDecoder>(BPListParser, OpackParser, Utf8Decoder, Utf16Decoder, Sec1Ec, AppleProtobuf, ProtobufParser, ASN1BER, GenericTLV, TLV8, EntropyDetector, HeuristicSignatureDetector)

    fun analyzeHex(data: String, tryhard: Boolean = false): List<Pair<String, ByteWitchResult>> {
        val filtered = data.filter { it in "0123456789abcdefABCDEF" }

        if(filtered.length % 2 != 0)
            return emptyList()

        try {
            val bytes = filtered.fromHex()
            return if(tryhard) analyzeTryhard(bytes) else analyze(bytes)
        }
        catch (e: Exception) {
            Logger.log(e.message)
            e.printStackTrace()
            return emptyList()
        }

    }

    fun analyze(data: ByteArray): List<Pair<String, ByteWitchResult>> {
        val possibleDecoders = decoders.filter { it.decodesAsValid(data) }
        return possibleDecoders.map { Pair(it.name, it.decode(data, 0)) }
    }

    fun analyzeTryhard(data: ByteArray): List<Pair<String, ByteWitchResult>> {
        Logger.log("tryhard decode attempt...")
        return decoders.mapNotNull {
            val decode = it.tryhardDecode(data)
            Logger.log("decode with ${it.name} yielded $decode")
            if(decode != null) Pair(it.name, decode) else null
        }
    }

    fun quickDecode(data: ByteArray, sourceOffset: Int): ByteWitchResult? = decoders.firstOrNull {
        val confidence = it.confidence(data)
        //Logger.log("quick decode of ${data.hex()} as ${it.name}: $confidence")
        confidence > 0.75
    }?.decode(data, sourceOffset, inlineDisplay = true)
}