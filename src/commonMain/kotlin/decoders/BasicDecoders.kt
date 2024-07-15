package decoders

import bitmage.decodeAsUTF16BE
import bitmage.fromHex
import bitmage.hex
import bitmage.indexOfSubsequence
import htmlEscape
import looksLikeUtf8String
import kotlin.math.*

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

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
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

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        return BWString(data.decodeAsUTF16BE(), Pair(sourceOffset, sourceOffset+data.size))
    }

    override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        return if(confidence(data) > 0.25)
            decode(data, 0)
        else
            null
    }
}

object EntropyDetector : ByteWitchDecoder {
    override val name = "entropy"

    override fun tryhardDecode(data: ByteArray) = null

    override fun decodesAsValid(data: ByteArray) = true

    // we'll display entropy indicators in quick decode results (if no other decode is available) given sufficient length
    // (for small payloads entropy doesn't really say anything)
    override fun confidence(data: ByteArray) = if(data.size > 20) 0.76 else 0.00


    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val byteCounts = IntArray(256){ 0 }
        data.forEach { byte -> byteCounts[byte.toUByte().toInt()] += 1 }

        // we'll assume uniform distribution of bytes, which means byte counts should be approximately normally distributed
        val expected = data.size.toDouble() / 256
        val sd = sqrt(0.003890991*data.size) // constant is p(1-p) = (1/256)*(1-(1/256))
        val entropy = byteCounts.map { it.toDouble() / data.size }.filter { it > 0 }.sumOf { - it * log2(it) }

        Logger.log("expecting $expected counts per byte, 2sd above: ${expected+2*sd}")

        val emoji = when {
            entropy > 7.0 -> "\uD83C\uDFB2"
            entropy > 5.5 -> "\uD83D\uDCDA"
            entropy > 3 -> "📖"
            else -> "\uD83D\uDDD2\uFE0F"
        }

        val rounded = round(entropy*10)/10
        val iconHTML = "<span title=\"entropy: $rounded\">$emoji</span>"

        // alright, here's how we'll do this:
        // if a 'hot byte' (<5% chance of occurring this often randomly in the sample) occurs more than 2 times
        // we color that byte according to its relative frequency, where full opacity of the highlight is applied to
        // the most frequent byte
        val hotByteRendering = if(expected+2*sd > 2) {
            val mostFrequent = byteCounts.maxOrNull()!!
            data.asUByteArray().joinToString("") {
                val hexByte = it.toString(16).padStart(2, '0')
                if(byteCounts[it.toInt()] > expected+2*sd) {
                    val alpha = ((byteCounts[it.toInt()].toDouble() / mostFrequent)*255).roundToInt()
                    "<span style=\"background: #c9748f${alpha.toString(16)}\">$hexByte</span>"
                }
                else
                    hexByte
            }
        }
        else
            data.hex()

        return if(inlineDisplay) {
                BWAnnotatedData(iconHTML+hotByteRendering, "".fromHex(), Pair(sourceOffset, sourceOffset + data.size))
            }
            else
                BWAnnotatedData("$iconHTML Shannon Entropy: $rounded (0-8) $hotByteRendering", "".fromHex(), Pair(sourceOffset, sourceOffset+data.size))
    }
}

object HeuristicSignatureDetector : ByteWitchDecoder {
    override val name = "heuristic"

    private val patterns: Map<String, String> = mapOf(
        "3082" to "ASN.1 sequence",
        "3081" to "ASN.1 sequence",
        "62706c697374" to "binary plist header",
        "53514C697465" to "sqlite header",
        "425A68" to "bzip2 compression",
        "4C5A4950" to "lzip compression",
        "504B0304" to "zip archive",
        "47494638" to "GIF magic bytes",
        "FFD8FF" to "JPG magic bytes",
        "52617221" to "RAR magic bytes",
        "7F454C46" to "ELF header",
        "89504E470D0A1A0A" to "PNG header",
        "255044462D" to "PDF header",
        "7573746172" to "tar archive",
        "377ABCAF271C" to "7zip compression",
        "1F8B" to "gzip compresssion",
        "FD377A585A00" to "xz compression",
        "04224D18" to "LZ4 frame",
        "0061736D" to "web assembly binary",
        "62767832" to "lzfse compression",
        "0000000C4A584C200D0A870A" to "JPEG XL header",
    )

    override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        val containedSignatures = patterns.map {
            Triple(data.indexOfSubsequence(it.key.fromHex()), it.value, it.key.length/2)
        }.filter { it.first > -1 }

        if(containedSignatures.isEmpty())
            return null

        return BWStringCollection(containedSignatures.map { BWString(it.second, Pair(it.first, it.first+it.third)) }, Pair(0, 0))
    }

    // we only want this to kick in as a last-ditch effort on a tryhard decode
    override fun decodesAsValid(data: ByteArray) = false

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean) = BWString("you should not see this", Pair(sourceOffset, sourceOffset))
}

class BWStringCollection(val elements: List<BWString>, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML() = elements.joinToString(" ") { it.renderHTML() }
}

class BWString(val string: String, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML() = "<div class=\"bpvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)}</div>"
}

class BWAnnotatedData(val annotationHTML: String, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML() = "<div class=\"bpvalue data\" $byteRangeDataTags>$annotationHTML ${data.hex()}</div>"
}