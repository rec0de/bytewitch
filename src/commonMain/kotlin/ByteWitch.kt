import bitmage.ByteOrder
import bitmage.fromHex
import bitmage.stripLeadingZeros
import bitmage.toBytes
import decoders.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object ByteWitch {
    private val decoders = listOf<ByteWitchDecoder>(
        BPList17, BPList15, BPListParser, Utf8Decoder, Utf16Decoder, JWT,
        OpackParser, MsgPackParser, CborParser, BsonParser, UbjsonParser,
        ProtobufParser, ASN1BER, Sec1Ec, PGP, ModernPGP, GenericTLV, TLV8, IEEE754, EdDSA, ECCurves, MSZIP, Bech32, DMAP,
        Randomness, HeuristicSignatureDetector
    )

    enum class Encoding(val label: String) {
        NONE("none"), PLAIN("plain"), HEX("hex"), DECIMAL("decimal"), HEXDUMP("hexdump"), BASE64("base64")
    }

    fun stripComments(data: String): String {
        // allow use of # as line comment
        val stripped = data.split("\n").joinToString("") { line ->
            val commentMarker = line.indexOfFirst { it == '#' }
            val lineEnd = if (commentMarker == -1) line.length else commentMarker
            line.take(lineEnd)
        }

        return stripped
    }

    fun getBytesFromInputEncoding(data: String): Pair<ByteArray, Encoding> {
        val cleanedData = data.trim()

        if(cleanedData.isEmpty())
            return Pair(byteArrayOf(), Encoding.NONE)

        // allow some overrides
        if(cleanedData.startsWith("#plain"))
            return Pair(cleanedData.removePrefix("#plain").trim().encodeToByteArray(), Encoding.PLAIN)
        else if(cleanedData.startsWith("#decimal")) {
            val parsed = parseDecimals(stripComments(cleanedData.removePrefix("#decimal")))
            return if(parsed != null) Pair(parsed, Encoding.DECIMAL) else Pair(byteArrayOf(), Encoding.NONE)
        }

        // note: in a bit of a hack, we support both classical base64 and base64url encodings here (-_ being url-only chars)
        val isBase64 = cleanedData.removePrefix("0x").replace("\n", "").matches(Regex("^[A-Z0-9+/\\-_=]+[G-Z+/=\\-_][A-Z0-9+/=\\-_]*$", RegexOption.IGNORE_CASE)) // matches b64 charset and at least one char distinguishing from raw hex
        val isHexdump = cleanedData.contains(Regex("^[0-9a-f]+\\s+([0-9a-f]{2}\\s+)+\\s*\\|.*\\|\\s*$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)))
        val commentsStripped = stripComments(cleanedData).removePrefix("0x")
        val isHex = Regex("[0-9a-fA-F\\s]+").matches(commentsStripped)

        return when {
            isBase64 -> Pair(decodeBase64(cleanedData.replace("\n", "")), Encoding.BASE64)
            isHexdump -> Pair(decodeHexdump(cleanedData), Encoding.HEXDUMP)
            isHex -> {
                val filtered = commentsStripped.filter { it in "0123456789abcdefABCDEF" }
                if (filtered.length % 2 != 0)
                    Pair(byteArrayOf(), Encoding.NONE)
                else
                    Pair(filtered.fromHex(), Encoding.HEX)
            }
            else -> Pair(cleanedData.encodeToByteArray(), Encoding.PLAIN)
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
    fun decodeBase64(base64Data: String): ByteArray {
        // transform URL-safe base64 into regular base64
        val canonicalB64 = base64Data.replace('-', '+').replace('_', '/')

        // b64 string is already padded!
        val padded = if(canonicalB64.endsWith("="))
            canonicalB64
        else {
            when (canonicalB64.length % 4) {
                0 -> canonicalB64
                2 -> "$canonicalB64=="
                3 -> "$canonicalB64="
                else -> "" // illegal unpadded b64
            }
        }

        return try {
            Base64.Default.decode(padded)
        }
        catch (e: Exception) {
            byteArrayOf()
        }
    }

    private fun parseDecimals(input: String): ByteArray? {
        val numbers = input.split(" ", "\n").filter { !it.matches(Regex("\\s*")) }.map {
            when {
                it.startsWith("0x") -> {
                    val hex = it.removePrefix("0x")
                    // pad to full bytes
                    val padded = if(hex.length % 2 == 0) hex else "0$hex"
                    padded.fromHex()
                }
                it.startsWith("0b") -> {
                    val binary = it.removePrefix("0b")
                    val padding = binary.length % 8
                    val padded = "0".repeat(padding) + binary
                    padded.chunked(8).map { byte -> byte.toInt(2).toByte() }.toByteArray()
                }
                it.matches(Regex("\\d+")) -> {
                    it.toLong().toBytes(ByteOrder.BIG).stripLeadingZeros()
                }
                else -> return null
            }
        }

        return numbers.fold(byteArrayOf()) { buf, elem -> buf + elem }
    }
}