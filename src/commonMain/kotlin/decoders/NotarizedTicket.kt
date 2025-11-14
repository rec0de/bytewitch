package decoders

import Date
import ParseCompanion
import bitmage.ByteOrder
import bitmage.hex

object NotarizedTicket: ByteWitchDecoder, ParseCompanion() {
    override val name = "NotarizedTicket"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        return decodeHeader(data, sourceOffset)
    }

    private fun decodeHeader(bytes: ByteArray, sourceOffset: Int): NotarizedTicketResult {
        check(readBytes(bytes, 4).decodeToString() == "s8ch") { "expected s8ch magic but got something else at ${parseOffset-4}"}
        val version = readInt(bytes, 4, explicitlySigned = false, ByteOrder.LITTLE)
        val derLen = readUInt(bytes, 4, ByteOrder.LITTLE)
        val contentLen = readUInt(bytes, 4, ByteOrder.LITTLE)

        val derPayload = readBytes(bytes, derLen.toInt())

        val preContent = parseOffset
        val contentPayload = decodeContent(bytes, sourceOffset)
        check(parseOffset == preContent + contentLen.toInt()) { "expected content block to be $contentLen B but got ${parseOffset-preContent} B"}

        check(bytes.size - parseOffset >= 72) { "insufficient bytes for signature, expected 72 got only ${bytes.size-parseOffset}"}
        val signature = readBytes(bytes, 72)

        val derResult = ASN1BER.decode(derPayload, sourceOffset + 16, inlineDisplay = true)
        return NotarizedTicketResult(version, derLen.toInt(), contentLen.toInt(), derResult, contentPayload, signature, sourceByteRange = Pair(sourceOffset, sourceOffset+parseOffset))
    }

    private fun decodeContent(bytes: ByteArray, sourceOffset: Int): NTContentBlock {
        val start = parseOffset+sourceOffset
        check(readBytes(bytes, 4).decodeToString() == "g8tk") { "expected g8tk magic but got something else at ${parseOffset-4}"}
        val type = readInt(bytes, 2, explicitlySigned = false, ByteOrder.LITTLE)
        val hashLen = readUInt(bytes, 2, ByteOrder.LITTLE)
        val hashCount = readUInt(bytes, 4, ByteOrder.LITTLE)
        val flags = readUInt(bytes, 4, ByteOrder.LITTLE)
        val timestamp = readULong(bytes, 8, ByteOrder.LITTLE)

        val date = Date(timestamp.toLong() * 1000) // timestamp is in seconds

        val hashes = mutableListOf<NTCDHash>()
        for (i in 0 until hashCount.toInt()) {
            val start = sourceOffset+parseOffset
            val type = readInt(bytes, 1)
            val hash = readBytes(bytes, hashLen.toInt())
            hashes.add(NTCDHash(type, hash, Pair(start, sourceOffset+parseOffset)))
        }

        return NTContentBlock(type, hashLen.toInt(), hashCount.toInt(), flags.toInt(), date, hashes, Pair(start, parseOffset+sourceOffset))
    }
}

class NotarizedTicketResult(
    val version: Int,
    val derSize: Int,
    val contentSize: Int,
    val derPayload: ByteWitchResult,
    val content: NTContentBlock,
    val signature: ByteArray,
    override val sourceByteRange: Pair<Int, Int>?
): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val versionTag = bwvalue("Version $version", relativeRangeTags(4, 4))
        val derLenTag = bwvalue("DER Size $derSize B", relativeRangeTags(8, 4))
        val contentLenTag = bwvalue("Content Size $contentSize B", relativeRangeTags(12, 4))
        val contentTag = "<div class=\"bwvalue\">${content.renderHTML()}</div>"
        val signatureTag = bwvalue("Signature 0x${signature.hex()}", relativeRangeTags(content.sourceByteRange.second, signature.size), data = true)
        return "<div class=\"roundbox generic\" $byteRangeDataTags>$versionTag $derLenTag $contentLenTag ${derPayload.renderHTML()} $contentTag $signatureTag</div>"
    }
}

class NTContentBlock(val type: Int, val hashLen: Int, val hashCount: Int, val flags: Int, val timestamp: Date, val hashes: List<NTCDHash>,
                     override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val typeTag = "<div class=\"bwvalue\" ${relativeRangeTags(4, 2)}>Type $type</div>"
        val lenTag = "<div class=\"bwvalue\" ${relativeRangeTags(6, 2)}>Hash Length $hashLen B</div>"
        val countTag = "<div class=\"bwvalue\" ${relativeRangeTags(8, 4)}>Hash Count $hashCount</div>"
        val flagTag = "<div class=\"bwvalue\" ${relativeRangeTags(12, 4)}>Flags 0x${flags.toString(16).padStart(8, '0')}</div>"
        val dateTag = "<div class=\"bwvalue\" ${relativeRangeTags(16, 8)}>Timestamp $timestamp</div>"

        val hashesTag = if(hashes.isEmpty())
                ""
            else
                bwvalue(hashes.joinToString(" ") { it.renderHTML() }, rangeTagsFor(sourceByteRange.first+24, sourceByteRange.second))

        return "<div class=\"roundbox generic flexy\" $byteRangeDataTags>$typeTag $lenTag $countTag $flagTag $dateTag $hashesTag</div>"
    }
}

class NTCDHash(val type: Int, val hash: ByteArray, override val sourceByteRange: Pair<Int, Int>?): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val typeStr = if(type == 2) "SHA256" else "unknown($type)"
        val typeTag = bwvalue("CDHash type $typeStr", relativeRangeTags(0, 1))
        val hashTag = bwvalue("0x${hash.hex()}", relativeRangeTags(1, hash.size), data = true)
        return "<div class=\"roundbox generic\" $byteRangeDataTags>$typeTag $hashTag</div>"
    }
}

