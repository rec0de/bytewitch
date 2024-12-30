package decoders

import bitmage.decodeAsUTF16BE
import bitmage.fromHex
import bitmage.hex
import bitmage.indexOfSubsequence
import htmlEscape
import looksLikeUtf16String
import looksLikeUtf8String
import kotlin.math.*

object Utf8Decoder : ByteWitchDecoder {
    override val name = "utf8"

    override fun decodesAsValid(data: ByteArray) = confidence(data) > 0.6

    override fun confidence(data: ByteArray): Double {
        val effectiveData = stripNullTerminator(data)

        try {
            val score = looksLikeUtf8String(effectiveData)
            //Logger.log(data.decodeToString())
            //Logger.log(score)
            return score
        } catch (e: Exception) {
            return 0.0
        }
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        return BWString(stripNullTerminator(data).decodeToString(), Pair(sourceOffset, sourceOffset+data.size))
    }

    override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        return if(confidence(data) > 0.25)
            decode(data, 0)
        else
            null
    }

    fun stripNullTerminator(data: ByteArray): ByteArray {
        // to hell with it, we'll support arbitrarily long null terminators
        var end = data.size
        while (end > 0 && data[end-1] == 0.toByte()) {
            end -= 1
        }
        return data.sliceArray(0 until end)
    }
}

object Utf16Decoder : ByteWitchDecoder {
    override val name = "utf16"

    override fun confidence(data: ByteArray): Double {
        try {
            val string = Utf8Decoder.stripNullTerminator(data).decodeAsUTF16BE()
            return looksLikeUtf16String(string)
        } catch (e: Exception) {
            return 0.0
        }
    }

    override fun decodesAsValid(data: ByteArray) = confidence(data) > 0.6

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        return BWString(Utf8Decoder.stripNullTerminator(data).decodeAsUTF16BE(), Pair(sourceOffset, sourceOffset+data.size))
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
        val nibbleCounts = IntArray(16){ 0 }

        data.forEach { byte ->
            byteCounts[byte.toUByte().toInt()] += 1
            val lowerNibble = (byte.toUByte().toInt() and 0x0F)
            val upperNibble = (byte.toUByte().toInt() and 0xF0) ushr 4
            nibbleCounts[lowerNibble] += 1
            nibbleCounts[upperNibble] += 1
        }

        // we'll assume uniform distribution of bytes, which means byte counts should be approximately normally distributed
        val expected = data.size.toDouble() / 256
        val expectedNibbles = (data.size.toDouble() * 2) / 16
        val entropyNibbles = nibbleCounts.map { it.toDouble() / (data.size*2) }.filter { it > 0 }.sumOf { - it * log2(it) }


        val sd = sqrt(0.003890991*data.size) // constant is p(1-p) = (1/256)*(1-(1/256))
        val entropy = byteCounts.map { it.toDouble() / data.size }.filter { it > 0 }.sumOf { - it * log2(it) }

        //Logger.log("expecting $expected counts per byte, 2sd above: ${expected+2*sd}")

        val emoji = when {
            entropy > 7.0 -> "\uD83C\uDFB2"
            entropy > 5.5 -> "\uD83D\uDCDA"
            entropy > 3 -> "📖"
            else -> "\uD83D\uDDD2\uFE0F"
        }

        val emojiNib = when {
            entropyNibbles > 3.7 -> "\uD83C\uDFB2"
            entropyNibbles > 3.3 -> "\uD83D\uDCDA"
            entropyNibbles > 2.5 -> "📖"
            else -> "\uD83D\uDDD2\uFE0F"
        }

        val rounded = round(entropy*10)/10
        val roundedNib = round(entropyNibbles*10)/10
        val iconHTML = "<span title=\"entropy: $rounded/$roundedNib\">$emoji$emojiNib</span>"

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

    private val patterns: Map<String, Pair<String, String?>> = mapOf(
        "3082" to Pair("ASN.1 sequence", null),
        "3081" to Pair("ASN.1 sequence", null),
        "62706c697374" to Pair("binary plist header", "https://newosxbook.com/bonus/bplist.pdf"),
        "7062696c7473" to Pair("binary plist header - wrong endianness", "https://newosxbook.com/bonus/bplist.pdf"),
        "53514C697465" to Pair("sqlite header", null),
        "425A68" to Pair("bzip2 compression", null),
        "4C5A4950" to Pair("lzip compression", null),
        "504B0304" to Pair("zip archive", null),
        "47494638" to Pair("GIF magic bytes", null),
        "FFD8FF" to Pair("JPG magic bytes", null),
        "52617221" to Pair("RAR magic bytes", null),
        "7F454C46" to Pair("ELF header", null),
        "89504E470D0A1A0A" to Pair("PNG header", null),
        "255044462D" to Pair("PDF header", null),
        "7573746172" to Pair("tar archive", null),
        "377ABCAF271C" to Pair("7zip compression", null),
        "1F8B" to Pair("gzip compresssion", null),
        "FD377A58" to Pair("xz compression", null),
        "04224D18" to Pair("LZ4 frame", null),
        "0061736D" to Pair("web assembly binary", null),
        "62767832" to Pair("lzfse compression", null),
        "0000000C4A584C200D0A870A" to Pair("JPEG XL header", null),
        "424f4d53746f7265" to Pair("BOMStore (Apple OTA)", "https://newosxbook.com/articles/OTA.html"),
        "70627a78" to Pair("pbzx (Apple OTA)", "https://newosxbook.com/articles/OTA.html"),
        "59414131" to Pair("YAA (Apple OTA archive)", "https://newosxbook.com/articles/OTA9.html"),
        "dec07eab" to Pair("Apple Remote Invocation (ARI) magic bytes", "https://github.com/seemoo-lab/aristoteles"),
        "4157444d" to Pair("Apple Wireless Debug Metadata", "https://github.com/hack-different/apple-diagnostics-format"),
        "070707" to Pair("cpio archive", null),
        "4367424950" to Pair("iOS optimized PNG", "https://github.com/erlendfh/pngdefry"),
        "160301" to Pair("TLS 1.0 record header", null),
        "160302" to Pair("TLS 1.1 record header", null),
        "160303" to Pair("TLS 1.2 record header", null),
        "160304" to Pair("TLS 1.3 record header", null),
        "16fefd" to Pair("DTLS 1.2 record header", null)
    )

    override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        val containedSignatures = patterns.map {
            Triple(data.indexOfSubsequence(it.key.fromHex()), it.value, it.key.length/2)
        }.filter { it.first > -1 }

        if(containedSignatures.isEmpty())
            return null

        return BWStringCollection(
            containedSignatures.map {
                if(it.second.second == null)
                    BWString(it.second.first, Pair(it.first, it.first + it.third))
                else
                    BWLinkedString(it.second.first, it.second.second!!, Pair(it.first, it.first + it.third))
            },
            Pair(0, 0)
        )
    }

    // we only want this to kick in as a last-ditch effort on a tryhard decode
    override fun decodesAsValid(data: ByteArray) = false

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean) = BWString("you should not see this", Pair(sourceOffset, sourceOffset))
}

class BWStringCollection(val elements: List<BWString>, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML() = elements.joinToString(" ") { it.renderHTML() }
}

open class BWString(val string: String, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML() = "<div class=\"bpvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)}</div>"
}

class BWLinkedString(string: String, private val url: String, sourceByteRange: Pair<Int, Int>): BWString(string, sourceByteRange) {
    override fun renderHTML() = "<div class=\"bpvalue stringlit\" $byteRangeDataTags>${htmlEscape(string)} <a href=\"${url}\" target=\"_blank\">(info)</a></div>"
}

class BWAnnotatedData(val annotationHTML: String, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML() = "<div class=\"bpvalue data\" $byteRangeDataTags>$annotationHTML ${data.hex()}</div>"
}