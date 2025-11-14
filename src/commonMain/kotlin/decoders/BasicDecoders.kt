package decoders

import bitmage.*
import htmlEscape
import looksLikeUtf16String
import looksLikeUtf8String
import kotlin.math.*

object Utf8Decoder : ByteWitchDecoder {
    override val name = "utf8"

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double,ByteWitchResult?> {
        val effectiveData = stripNullTerminator(data)

        val nullTerminatorBonus = if(effectiveData.size == data.size-1) 0.2 else 0.0

        try {
            val score = looksLikeUtf8String(effectiveData)
            return Pair(min(score+nullTerminatorBonus, 1.0), null)
        } catch (e: Exception) {
            return Pair(0.0, null)
        }
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        return BWString(stripNullTerminator(data).decodeToString(), Pair(sourceOffset, sourceOffset+data.size))
    }

    override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        return if(confidence(data, 0).first > 0.25)
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

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        // utf16 should be even byte length
        if(data.size % 2 == 1)
            return Pair(0.0, null)

        try {
            val string = Utf8Decoder.stripNullTerminator(data).decodeAsUTF16BE()
            return Pair(looksLikeUtf16String(string), null)
        } catch (e: Exception) {
            return Pair(0.0, null)
        }
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        return BWString(Utf8Decoder.stripNullTerminator(data).decodeAsUTF16BE(), Pair(sourceOffset, sourceOffset+data.size))
    }

    override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        return if(confidence(data, 0).first > 0.25)
            decode(data, 0)
        else
            null
    }

    // look for printable ASCII ranges encoded in UTF16
    override fun findDecodableSegments(data: ByteArray): List<Pair<Int, Int>> {
        val minLength = 4
        var currentStringStart = 0
        var inString = false
        var expectingZero = false

        val strings = mutableListOf<Pair<Int, Int>>()

        for (i in data.indices) {
            if(inString) {
                when {
                    expectingZero && data[i] != 0.toByte() -> {
                        inString = false
                        if(i - currentStringStart >= minLength*2)
                            strings.add(Pair(currentStringStart, i))
                    }
                    !expectingZero && data[i] !in 0x20..0x7e -> {
                        inString = false
                        if(i - currentStringStart >= minLength*2)
                            strings.add(Pair(currentStringStart, i-1))
                        if(data[i] == 0.toByte()) {
                            inString = true
                            currentStringStart = i
                            expectingZero = false
                        }
                    }
                    else -> {
                        expectingZero = !expectingZero
                    }
                }
            }
            else if (data[i] == 0.toByte()) {
                inString = true
                expectingZero = false
                currentStringStart = i
            }
        }

        if(inString && data.size - 1 - currentStringStart >= minLength * 2) {
            val endIndex = if(data.last() == 0.toByte()) data.size - 1 else data.size
            strings.add(Pair(currentStringStart, endIndex))
        }

        return strings
    }
}

object IEEE754 : ByteWitchDecoder {
    override val name = "IEEE754"

    private fun looksReasonable(positive: Boolean, exponent: Int, mantissa: Long, value: Double, isDouble: Boolean): Double {
        val positiveBonus = if(positive) 0.1 else 0.0
        // 12 and 41 are the bits that are in the mantissa Long (64b) but not in the actual binary data for float and double respectively
        val mantissaComplexity = if(isDouble) (mantissa.countTrailingZeroBits().toDouble() - 12) / 52 else (mantissa.countTrailingZeroBits().toDouble() - 41) / 23
        val magnitudeScore = (12 - abs(exponent)).toDouble() / 18
        val stringLength = (7.0 - value.toString().length) / 7

        val score = positiveBonus + (mantissaComplexity + magnitudeScore + stringLength) / 2
        //Logger.log("number plausibility: positive $positiveBonus exponent $exponent / $magnitudeScore roundness $mantissaComplexity stringLength $stringLength total $score")
        return score
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        when(data.size) {
            4 -> {
                val zeroBytePenalty = - (data.count { it.toInt() == 0 }.toDouble() / data.size) * 0.5
                val partsBE = dissectFloat(data.readInt(ByteOrder.BIG))
                val valueBE = data.readFloat(ByteOrder.BIG).toDouble()
                val scoreBE = looksReasonable(partsBE.first, partsBE.second, partsBE.third.toLong(), valueBE, isDouble = false) + zeroBytePenalty
                if(scoreBE > 0.75)
                    return BWString("Float BE: $valueBE", Pair(sourceOffset, sourceOffset+4))

                val partsLE = dissectFloat(data.readInt(ByteOrder.LITTLE))
                val valueLE = data.readFloat(ByteOrder.LITTLE).toDouble()
                val scoreLE = looksReasonable(partsLE.first, partsLE.second, partsLE.third.toLong(), valueLE, isDouble = false) + zeroBytePenalty
                if(scoreLE > 0.75)
                    return BWString("Float LE: $valueLE", Pair(sourceOffset, sourceOffset+4))
                throw Exception("not a reasonable float / double")
            }
            8 -> {
                val zeroBytePenalty = - (data.count { it.toInt() == 0 }.toDouble() / data.size) * 0.5
                val partsBE = dissectDouble(data.readLong(ByteOrder.BIG))
                val valueBE = data.readDouble(ByteOrder.BIG)
                val scoreBE = looksReasonable(partsBE.first, partsBE.second, partsBE.third, valueBE, isDouble = true) + zeroBytePenalty
                if(scoreBE > 0.75)
                    return BWString("Double BE: $valueBE", Pair(sourceOffset, sourceOffset+8))

                val partsLE = dissectDouble(data.readLong(ByteOrder.LITTLE))
                val valueLE = data.readDouble(ByteOrder.LITTLE)
                val scoreLE = looksReasonable(partsLE.first, partsLE.second, partsLE.third, valueLE, isDouble = true) + zeroBytePenalty
                if(scoreLE > 0.75)
                    return BWString("Double LE: $valueLE", Pair(sourceOffset, sourceOffset+8))
                throw Exception("not a reasonable float / double")
            }
            else -> throw Exception("not a reasonable float / double size")
        }
    }

    private fun dissectFloat(data: Int): Triple<Boolean, Int, Int> {
        val signPositive = data >= 0 // sign bit actually matches Int sign bit
        val exponent = ((data and 0x7f800000) shr 23) - 127
        val mantissa = data and 0x007fffff // clear top bit, which is part of exponent
        return Triple(signPositive, exponent, mantissa)
    }

    private fun dissectDouble(data: Long): Triple<Boolean, Int, Long> {
        val signPositive = data >= 0 // sign bit actually matches Long sign bit
        val exponent = (((data and 0x7ff0000000000000) shr 52) - 1023).toInt()
        val mantissa = (data and 0x000fffffffffffff)
        return Triple(signPositive, exponent, mantissa)
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
        "16fefd" to Pair("DTLS 1.2 record header", null),
        "212022" to Pair("IKEv2 SA_INIT header", "https://www.rfc-editor.org/rfc/rfc7296.html#section-3.1"),
        "4d500305" to Pair("Apple MsgPack header", null),
        "7b22" to Pair("JSON dict", null),
        "3a290a" to Pair("Smile Data Format", "https://github.com/FasterXML/smile-format-specification"),
        "d9d9f7" to Pair("CBOR magic", "https://en.wikipedia.org/wiki/CBOR"),
        "c301" to Pair("AVRO single object encoding marker", "https://avro.apache.org/docs/1.12.0/specification/"),
        "0a51e5c01800" to Pair("Microsoft Compression Header", "https://github.com/frereit/pymszip"),
        "c0a801" to Pair("Local IP address (192.168.1.x)", null),
        "46617364554153" to Pair("osascript", "https://github.com/Jinmo/applescript-disassembler"),
        "53616c7465645f5f" to Pair("openssl encrypted", "https://github.com/openssl/openssl/blob/ca95d136d238e5ead679df8a7573ecccef37cc0e/apps/enc.c#L121"),
        "6738746b" to Pair("Apple notarized ticket content magic", "https://www.mothersruin.com/software/Archaeology/reverse/tickets.html"),
        "73386368" to Pair("Apple notarized ticket magic", "https://www.mothersruin.com/software/Archaeology/reverse/tickets.html"),
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
    override fun confidence(data: ByteArray, sourceOffset: Int) = Pair(0.0, null)

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean) = BWString("you should not see this", Pair(sourceOffset, sourceOffset))
}

