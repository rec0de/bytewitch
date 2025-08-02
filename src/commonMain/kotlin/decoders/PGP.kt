package decoders

import Date
import ParseCompanion
import bitmage.ByteOrder
import bitmage.hex
import htmlEscape
import kotlin.math.ceil
import kotlin.math.roundToInt

object PGP: ByteWitchDecoder, ParseCompanion() {
    override val name = "PGPv1.0"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        val blocks = mutableListOf<ByteWitchResult>()
        check(data.size > 10) { "unreasonably short PGP message" }

        while(parseOffset < data.size) {
            val start = sourceOffset + parseOffset
            val ctb = readInt(data, 1)
            check(ctb and 0x80 != 0) { "expecting CTB to have high bit set" }

            var lengthFieldSize: Int
            val type: Int
            val length: Int
            val newFormat: Boolean

            // old format header
            if(ctb and 0x40 == 0) {
                newFormat = false
                lengthFieldSize = 1 shl (ctb and 0x03)
                type = (ctb and 0x7c) shr 2

                length = if(lengthFieldSize == 8) {
                    lengthFieldSize = 0
                    data.size - parseOffset
                }
                else {
                    readUInt(data, lengthFieldSize, ByteOrder.LITTLE).toInt()
                }
            }
            // new format header
            else {
                newFormat = true
                type = ctb and 0x3F
                val firstLenByte = readUInt(data, 1, ByteOrder.LITTLE).toInt()
                when(firstLenByte) {
                    in 0..191 -> {
                        lengthFieldSize = 1
                        length = firstLenByte
                    }
                    in 192..223 -> {
                        lengthFieldSize = 2
                        val secondLenByte = readUInt(data, 1, ByteOrder.LITTLE).toInt()
                        length = ((firstLenByte - 192) shl 8) + secondLenByte + 192
                    }
                    255 -> {
                        lengthFieldSize = 5
                        length = readUInt(data, 4, ByteOrder.LITTLE).toInt()
                    }
                    else -> throw Exception("illegal first length byte value $firstLenByte")
                }
            }

            Logger.tag("PGP", "reading type $type lengthFieldSize $lengthFieldSize length $length new? $newFormat")

            check(type in 1..19) { "undefined packet type $type" }
            check(data.size - parseOffset >= length) { "incorrect data length in header" }

            val block = when(type) {
                1 -> {
                    val keyID = readBytes(data, 8)
                    val encapSymKey = readLengthPrefixedBits(data, sourceOffset)
                    PGPencryption(length, lengthFieldSize, keyID, encapSymKey, Pair(start, sourceOffset+parseOffset))
                }
                2 -> {
                    val keyID = readBytes(data, 8)
                    val signature = readLengthPrefixedBits(data, sourceOffset)
                    PGPsignature(length, lengthFieldSize, keyID, signature, Pair(start, sourceOffset+parseOffset))
                }
                5 -> {
                    val timestamp = readUInt(data, 4, ByteOrder.LITTLE).toLong() * 1000
                    val userId = readLengthPrefixedString(data, 1, ByteOrder.LITTLE) ?: ""

                    val n = readLengthPrefixedBits(data, sourceOffset)
                    val e = readLengthPrefixedBits(data, sourceOffset)
                    val d = readLengthPrefixedBits(data, sourceOffset)
                    val p = readLengthPrefixedBits(data, sourceOffset)
                    val q = readLengthPrefixedBits(data, sourceOffset)
                    val u = readLengthPrefixedBits(data, sourceOffset)

                    PGPprivkey(length, lengthFieldSize, Date(timestamp), userId, n, e, d, p, q, u, Pair(start, sourceOffset+parseOffset))
                }
                6 -> {
                    val timestamp = readUInt(data, 4, ByteOrder.LITTLE).toLong() * 1000
                    val userId = readLengthPrefixedString(data, 1, ByteOrder.LITTLE) ?: ""

                    val n = readLengthPrefixedBits(data, sourceOffset)
                    val e = readLengthPrefixedBits(data, sourceOffset)

                    PGPpubkey(length, lengthFieldSize, Date(timestamp), userId, n, e, Pair(start, sourceOffset+parseOffset))
                }
                else -> PGPunknown(type, length, lengthFieldSize, readBytes(data, length), Pair(start, sourceOffset+parseOffset))
            }

            blocks.add(block)
        }

        return PGPfile(blocks, Pair(sourceOffset, sourceOffset+parseOffset))
    }

    private fun readLengthPrefixedBits(data: ByteArray, sourceOffset: Int): PGPBigInt {
        val start = parseOffset
        val bitLength = readUInt(data, 2, ByteOrder.LITTLE).toInt()
        val byteLength = ceil((bitLength.toDouble() / 8)).roundToInt()
        val bytes = readBytes(data, byteLength)
        return PGPBigInt(bitLength, bytes, Pair(sourceOffset+start, sourceOffset+parseOffset))
    }
}

class PGPfile(val blocks: List<ByteWitchResult>, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override fun renderHTML(): String {
        return if(blocks.size == 1)
            blocks[0].renderHTML()
        else {
            "<div class=\"roundbox generic largecollection\" $byteRangeDataTags>${blocks.joinToString(" ") { "<div class=\"bpvalue\">${it.renderHTML()}</div>" }}</div>"
        }
    }
}

class PGPBigInt(private val bitSize: Int, val bytes: ByteArray, override val sourceByteRange: Pair<Int, Int>?): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"bpvalue\" ${relativeRangeTags(0, 2)}>$bitSize bits</div><div class=\"bpvalue data\" ${relativeRangeTags(2, bytes.size)}>0x${bytes.hex()}</div>"
    }

    fun renderNamed(name: String): String {
        return "<div class=\"flexy\"><span style=\"align-self: center;\">$name:</span> ${renderHTML()}</div>"
    }
}

class PGPpubkey(val length: Int, val lengthSize: Int, val timestamp: Date, val user: String, val n: PGPBigInt, val e: PGPBigInt,
                override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult{
    override fun renderHTML(): String {
        val typeHTML = "<div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>CERT_PUBKEY</div>"
        val lengthHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>$length B</div>"
        val timeHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize, 4)}>$timestamp</div>"
        val userHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize+4, 1+user.length)}>User: \"${htmlEscape(user)}\"</div>"
        return "<div class=\"roundbox generic largecollection\" $byteRangeDataTags><div class=\"flexy\">$typeHTML $lengthHTML $timeHTML $userHTML</div> ${n.renderNamed("n")} ${e.renderNamed("e")}</div>"
    }
}

class PGPprivkey(
    val length: Int,
    val lengthSize: Int,
    val timestamp: Date,
    val user: String,
    val n: PGPBigInt,
    val e: PGPBigInt,
    val d: PGPBigInt,
    val p: PGPBigInt,
    val q: PGPBigInt,
    val u: PGPBigInt,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult{
    override fun renderHTML(): String {
        val typeHTML = "<div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>CERT_SECKEY</div>"
        val lengthHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>$length B</div>"
        val timeHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize, 4)}>$timestamp</div>"
        val userHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize+4, 1+user.length)}>User: \"${htmlEscape(user)}\"</div>"
        return "<div class=\"roundbox generic largecollection\" $byteRangeDataTags><div class=\"flexy\">$typeHTML $lengthHTML $timeHTML $userHTML</div> ${n.renderNamed("n")} ${e.renderNamed("e")} ${d.renderNamed("d")} ${p.renderNamed("p")} ${q.renderNamed("q")} ${u.renderNamed("u")}</div>"
    }
}

class PGPsignature(
    val length: Int,
    val lengthSize: Int,
    val keyID: ByteArray,
    val sigBytes: PGPBigInt,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult{
    override fun renderHTML(): String {
        val typeHTML = "<div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>SKE</div>"
        val lengthHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>$length B</div>"
        val keyidHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize, 8)}>Key ID 0x${keyID.hex()}</div>"
        return "<div class=\"roundbox generic largecollection\" $byteRangeDataTags><div class=\"flexy\">$typeHTML $lengthHTML $keyidHTML</div> ${sigBytes.renderNamed("sig")} </div>"
    }
}

class PGPencryption(
    val length: Int,
    val lengthSize: Int,
    val keyID: ByteArray,
    val encBytes: PGPBigInt,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult{
    override fun renderHTML(): String {
        val typeHTML = "<div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>PKE</div>"
        val lengthHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>$length B</div>"
        val keyidHTML = "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize, 8)}>Key ID 0x${keyID.hex()}</div>"
        return "<div class=\"roundbox generic largecollection\" $byteRangeDataTags><div class=\"flexy\">$typeHTML $lengthHTML $keyidHTML</div> ${encBytes.renderNamed("symKey")} </div>"
    }
}

class PGPunknown(val type: Int, val length: Int, val lengthSize: Int, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    private val typeLookup = mapOf(
        1 to "PKE",
        2 to "SKE",
        3 to "MD",
        4 to "CONKEY",
        5 to "CERT_SECKEY",
        6 to "CERT_PUBKEY",
        8 to "COMPRESSED",
        9 to "CKE",
        12 to "LITERAL",
        13 to "RAW1",
        14 to "PATTERN",
        15 to "EXTENDED"
    )

    private fun typeToString(type: Int): String {
        return if(typeLookup.containsKey(type)) typeLookup[type]!! else "UNK($type)"
    }

    override fun renderHTML(): String {
        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>Type ${typeToString(type)}</div><div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>Length $length B</div><div class=\"bpvalue data\" ${relativeRangeTags(lengthSize+1, data.size)}>0x${data.hex()}</div></div>"

    }
}

