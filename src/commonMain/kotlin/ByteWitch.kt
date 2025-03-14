import bitmage.fromHex
import decoders.*


object ByteWitch {

    private val decoders = listOf<ByteWitchDecoder>(
        BPList17, BPList15, BPList16, BPListParser, OpackParser, Utf8Decoder, Utf16Decoder,
        ProtobufParser, ASN1BER, GenericTLV, TLV8,
        EntropyDetector, HeuristicSignatureDetector
    )

    fun analyzeInput(data: String, tryhard: Boolean = false): List<Pair<String, ByteWitchResult>> {
        val cleanedData = data.trim()
        val isHex = cleanedData.all { it in "0123456789abcdefABCDEF" }
        val isBase64 = cleanedData.matches(Regex("^[A-Za-z0-9+/=]+$"))
        val isHexdump = cleanedData.contains("\n") && cleanedData.any { it in "0123456789abcdefABCDEF" }

        val outputs = when {
            isHex -> analyzeHex(cleanedData, tryhard)
            isBase64 -> analyze(decodeBase64(cleanedData), tryhard)
            isHexdump -> analyze(decodeHexdump(cleanedData), tryhard)
            else -> {
                Logger.log("Unsupported format: Data must be in Hex, Base64, or Hexdump format")
                emptyList()
            }
        }

        return outputs
    }

    fun analyzeHex(data: String, tryhard: Boolean = false): List<Pair<String, ByteWitchResult>> {
        val filtered = data.filter { it in "0123456789abcdefABCDEF" }

        if (filtered.length % 2 != 0)
            return emptyList()

        try {
            val bytes = filtered.fromHex()
            return analyze(bytes, tryhard)
        } catch (e: Exception) {
            Logger.log("Failed to analyze hex: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun analyze(data: ByteArray, tryhard: Boolean): List<Pair<String, ByteWitchResult>> {
        if(tryhard) {
            Logger.log("tryhard decode attempt...")
            return decoders.mapNotNull {
                val decode = it.tryhardDecode(data)
                Logger.log("decode with ${it.name} yielded $decode")
                if (decode != null) Pair(it.name, decode) else null
            }
        }
        else {
            val possibleDecoders = decoders.filter { it.decodesAsValid(data) }
            return possibleDecoders.map { Pair(it.name, it.decode(data, 0)) }
        }
    }

    fun quickDecode(data: ByteArray, sourceOffset: Int): ByteWitchResult? = decoders.firstOrNull {
        val confidence = it.confidence(data)
        confidence > 0.75
    }?.decode(data, sourceOffset, inlineDisplay = true)

    private fun decodeHexdump(hexdumpData: String): ByteArray {
        var collectedBytes = byteArrayOf()

        // Process the hexdump line by line
        hexdumpData.lines().forEach { line ->
            // Skip blank lines
            if (line.isBlank()) return@forEach

            // Remove the first column: everything until the first space
            val firstSpaceIndex = line.indexOf(' ')
            if (firstSpaceIndex == -1) return@forEach

            // Get the rest of the line after the address, and trim leading spaces
            var hexPart = line.substring(firstSpaceIndex).trimStart()

            // If the line contains a '|' character, only take the part before it
            val pipeIndex = hexPart.indexOf('|')
            if (pipeIndex != -1) {
                hexPart = hexPart.substring(0, pipeIndex).trim()
            }

            collectedBytes += hexPart.filter { it in "0123456789abcdefABCDEF" }.fromHex()
        }

        return collectedBytes
    }

    private fun decodeBase64(base64Data: String): ByteArray {
        // Decode Base64 directly into raw bytes
        return js("atob(base64Data)").unsafeCast<String>()
            .toCharArray()
            .map { it.code.toByte() }
            .toByteArray()
    }
}
