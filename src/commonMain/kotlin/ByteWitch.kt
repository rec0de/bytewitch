import bitmage.fromHex
import bitmage.hex
import decoders.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object ByteWitch {
    private val decoders = listOf<ByteWitchDecoder>(
        BPList17, BPList15, BPListParser, Utf8Decoder, Utf16Decoder, OpackParser, MsgPackParser, CborParser, BsonParser, UbjsonParser,
        ProtobufParser, ASN1BER, Sec1Ec, PGP, GenericTLV, TLV8, IEEE754, EdDSA, ECCurves, MSZIP,
        Randomness, HeuristicSignatureDetector
    )

    private var plainHex = false
    fun isPlainHex() = plainHex

    fun stripCommentsAndFilterHex(data: String): String {
        // allow use of # as line comment
        val stripped = data.split("\n").map { line ->
            val commentMarker = line.indexOfFirst{ it == '#' }
            val lineEnd = if(commentMarker == -1) line.length else commentMarker
            line.substring(0, lineEnd)
        }.joinToString("")

        return stripped.filter { it in "0123456789abcdefABCDEF" }
    }

    fun getBytesFromInputEncoding(data: String): ByteArray {
        val cleanedData = data.trim()
        val isBase64 = cleanedData.matches(Regex("^[A-Z0-9+/=]+[G-Z+/=][A-Z0-9+/=]*$", RegexOption.IGNORE_CASE)) // matches b64 charset and at least one char distinguishing from raw hex
        val isHexdump = cleanedData.contains(Regex("^[0-9a-f]+\\s+([0-9a-f]{2}\\s+)+\\s+\\|.*\\|\\s*$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)))
        plainHex = false

        return when {
            isBase64 -> decodeBase64(cleanedData)
            isHexdump -> decodeHexdump(cleanedData)
            else -> {
                plainHex = true
                val filtered = stripCommentsAndFilterHex(cleanedData)
                if (filtered.length % 2 != 0)
                    byteArrayOf()
                else
                    filtered.fromHex()
            }
        }

    }


    fun analyze(data: ByteArray, tryhard: Boolean): List<Pair<String, ByteWitchResult>> {
        val allDecoders = decoders

        if(tryhard) {
            Logger.log("tryhard decode attempt...")
            return allDecoders.mapNotNull {
                val decode = it.tryhardDecode(data)
                Logger.log("decode with ${it.name} yielded $decode")
                if (decode != null) Pair(it.name, decode) else null
            }
        }
        else {
            // decodes as valid gives a quick estimate of which decoders could decode a payload
            // this is not necessarily true, so we catch failed parses later on also and remove them from the results
            val possibleDecoders = decoders.map { Pair(it, it.confidence(data, 0)) }.filter { it.second.first > 0.3 }

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

    fun quickDecode(data: ByteArray, sourceOffset: Int): ByteWitchResult? {

        decoders.forEach { decoder ->
            val confidence = decoder.confidence(data, sourceOffset)
            if (confidence.first > 0.75) {
                try {
                    return confidence.second ?: decoder.decode(data, sourceOffset, inlineDisplay = true)
                } catch (e: Exception) {
                    Logger.log("Quick decode failed with exception: '${e.message}' when assuming ${decoder.name} format with confidence ${confidence.first}")
                    e.printStackTrace()
                }
            }
        }

        return null
    }

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

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64(base64Data: String): ByteArray {
        return try {
            Base64.Default.decode(base64Data)
        }
        catch (e: Exception) {
            byteArrayOf()
        }
    }
}