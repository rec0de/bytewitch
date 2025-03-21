import bitmage.fromHex
import bitmage.hex
import decoders.*

object ByteWitch {

    private val decoders = listOf<ByteWitchDecoder>(
        BPList17, BPList15, BPListParser, Utf8Decoder, Utf16Decoder, OpackParser,
        ProtobufParser, ASN1BER, Sec1Ec, GenericTLV, TLV8, EdDSA, ECCurves,
        EntropyDetector, HeuristicSignatureDetector, Nemesys
    )

    fun getBytesFromInputEncoding(data: String): ByteArray {
        val cleanedData = data.trim()
        val isBase64 = cleanedData.matches(Regex("^[A-Z0-9+/=]+[G-Z+/=][A-Z0-9+/=]*$", RegexOption.IGNORE_CASE)) // matches b64 charset and at least one char distinguishing from raw hex
        val isHexdump = cleanedData.contains(Regex("^[0-9a-f]+\\s+([0-9a-f]{2}\\s+)+\\s+\\|.*\\|\\s*$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)))

        return when {
            isBase64 -> decodeBase64(cleanedData)
            isHexdump -> decodeHexdump(cleanedData)
            else -> {
                val filtered = data.filter { it in "0123456789abcdefABCDEF" }
                Logger.log(filtered)
                if (filtered.length % 2 != 0)
                    byteArrayOf()
                else
                    filtered.fromHex()
            }
        }

    }

    fun analyze(data: ByteArray, tryhard: Boolean): List<Pair<String, ByteWitchResult>> {
        if(tryhard) {
            Logger.log("tryhard decode attempt...")
            return decoders.mapNotNull {
                val decode = it.tryhardDecode(data)
                Logger.log("decode with ${it.name} yielded $decode")
                if (decode != null) Pair(it.name, decode) else null
            }
        }
        else {
            // decodes as valid gives a quick estimate of which decoders could decode a payload
            // this is not necessarily true, so we catch failed parses later on also and remove them from the results
            val possibleDecoders = decoders.map { Pair(it, it.decodesAsValid(data)) }.filter { it.second.first }

            return possibleDecoders.mapNotNull {
                try {
                    Pair(it.first.name, it.second.second ?: it.first.decode(data, 0))
                } catch (e: Exception) {
                    Logger.log(e.toString())
                    null
                }
            }
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
        return try {
            js("atob(base64Data)").unsafeCast<String>()
                .toCharArray()
                .map { it.code.toByte() }
                .toByteArray()
        }
        catch (e: Exception) {
            byteArrayOf()
        }
    }
}