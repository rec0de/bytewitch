import bitmage.fromHex
import decoders.*

object ByteWitch {
    private val decoders = listOf<ByteWitchDecoder>(BPListParser, OpackParser, Utf8Decoder, Utf16Decoder, ProtobufParser, TLV8, GenericTLV)

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
        return decoders.mapNotNull {
            val decode = it.tryhardDecode(data)
            if(decode != null) Pair(it.name, decode) else null
        }
    }

    fun quickDecode(data: ByteArray, sourceOffset: Int): ByteWitchResult? = decoders.firstOrNull { it.confidence(data) > 0.75 }?.decode(data, sourceOffset)
}