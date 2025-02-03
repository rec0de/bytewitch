import bitmage.fromHex
import bitmage.hex
import decoders.*
import kotlinx.browser.window
//import java.util.Base64


object ByteWitch {
    private val decoders = listOf<ByteWitchDecoder>(
        BPList17,BPList15, BPListParser, OpackParser, Utf8Decoder, Utf16Decoder,
        ProtobufParser, ASN1BER, GenericTLV, TLV8,
        EntropyDetector, HeuristicSignatureDetector
    )

    fun analyzeInput(data: String, tryhard: Boolean = false): List<Pair<String, ByteWitchResult>> {
        val cleanedData = data.trim()
        val isHex = cleanedData.all { it in "0123456789abcdefABCDEF" }
        val isBase64 = cleanedData.matches(Regex("^[A-Za-z0-9+/=]+$"))
        val isHexdump = cleanedData.contains("\n") && cleanedData.any { it in "0123456789abcdefABCDEF" }

        return when {
            isHex -> analyzeHex(cleanedData, tryhard)
            isBase64 -> {
                try {
                    val decodedBytes = decodeBase64(cleanedData)
                    val hexData = decodedBytes.toHex()
                    analyzeHex(hexData, tryhard)
                } catch (e: Exception) {
                    Logger.log("Failed to decode Base64: ${e.message}")
                    e.printStackTrace()
                    emptyList()
                }
            }
            isHexdump -> {
                try {
                    val decodedBytes = decodeHexdump(cleanedData)
                    val hexData = decodedBytes.toHex()
                    analyzeHex(hexData, tryhard)
                } catch (e: Exception) {
                    Logger.log("Failed to decode hexdump: ${e.message}")
                    e.printStackTrace()
                    emptyList()
                }
            }
            else -> {
                Logger.log("Unsupported format: Data must be in Hex, Base64, or Hexdump format")
                emptyList()
            }
        }
    }


    fun analyzeHex(data: String, tryhard: Boolean = false): List<Pair<String, ByteWitchResult>> {
        val filtered = data.filter { it in "0123456789abcdefABCDEF" }

        if (filtered.length % 2 != 0)
            return emptyList()

        try {
            val bytes = filtered.fromHex()
            return if (tryhard) analyzeTryhard(bytes) else analyze(bytes)
        } catch (e: Exception) {
            Logger.log("Failed to analyze hex: ${e.message}")
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
            if (decode != null) Pair(it.name, decode) else null
        }
    }

    fun quickDecode(data: ByteArray, sourceOffset: Int): ByteWitchResult? = decoders.firstOrNull {
        val confidence = it.confidence(data)
        confidence > 0.75
    }?.decode(data, sourceOffset, inlineDisplay = true)

    private fun decodeHexdump(hexdumpData: String): ByteArray {
        // Extract hex data from the hexdump lines
        val hexData = hexdumpData.lines()
            .mapNotNull { line ->
                // Extract the hex data section (ignore offset and ASCII parts)
                val parts = line.split("\\s{2,}".toRegex()) // Split into offset, hex, and ASCII
                if (parts.size < 2) {
                    println("Skipping invalid line: $line")
                    return@mapNotNull null
                }
                val hexPart = parts[1] // Second part should be the hex data
                hexPart.replace("\\s".toRegex(), "") // Remove spaces
            }
            .joinToString("") // Combine all hex data into a single string

        println("Cleaned hex data: $hexData")

        // Convert the cleaned hex string to ByteArray
        return try {
            hexData.fromHex()
        } catch (e: Exception) {
            println("Error during hex conversion: ${e.message}")
            ByteArray(0) // Return empty byte array on failure
        }
    }

    fun String.fromHex(): ByteArray {
        require(length % 2 == 0) { "Hex string must have an even number of characters" }
        return chunked(2).map { it.toUByte(16).toByte() }.toByteArray()
    }


    private fun decodeBase64(base64Data: String): ByteArray {
        // Decode Base64 directly into raw bytes
        return js("atob(base64Data)").unsafeCast<String>()
            .toCharArray()
            .map { it.code.toByte() }
            .toByteArray()
    }


    private fun ByteArray.toHex(): String {
        // Convert ByteArray to a clean hexadecimal string
        return joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
    }
}
