package decoders.apple

import ParseCompanion
import bitmage.ByteOrder
import bitmage.untilIndex
import decoders.BWAnnotatedData
import decoders.BWGenericSequence
import decoders.ByteWitchDecoder
import decoders.ByteWitchResult
import decoders.bwvalue


object AppleCompression : ByteWitchDecoder, ParseCompanion() {
    override val name = "Apple Compression"

    private val magics = setOf("pbxz", "pbzm")

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        return if(data.size > 28 && data.untilIndex(4).decodeToString() in magics)
            Pair(1.0, null)
        else
            Pair(0.0, null)
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        check(data.size > 4 + 8 + 8 + 8) { "AppleCompression: too little data" }

        val magic = readBytes(data, 4).decodeToString()
        val flags = readULong(data, 8, ByteOrder.BIG)

        check(magic in magics){ "AppleCompression: unknown magic $magic" }

        val entries = mutableListOf<ByteWitchResult>()

        entries.add(ACHeader(magic, flags, Pair(sourceOffset, sourceOffset+parseOffset)))

        while(data.size > parseOffset) {
            entries.add(decodeSingle(data, sourceOffset))
        }

        check(entries.isNotEmpty()) { "AppleCompression: no block" }
        return BWGenericSequence(entries, Pair(sourceOffset, sourceOffset + parseOffset))
    }

    fun decodeSingle(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        val start = parseOffset + sourceOffset
        val uncompressedSize = readULong(data, 8, ByteOrder.BIG)
        val compressedSize = readULong(data, 8, ByteOrder.BIG)

        check(compressedSize.toLong() <= data.size - parseOffset)
        check(uncompressedSize >= compressedSize)

        val payload = readBytes(data, compressedSize.toLong().toInt())

        return ACBlock(uncompressedSize, compressedSize, payload, Pair(start, sourceOffset+parseOffset))
    }
}

class ACHeader(val magic: String, val blockSize: ULong, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val header = bwvalue("Compression Magic: $magic", relativeRangeTags(0, 4))
        val bs = bwvalue("Block Size: ${humanReadableByteCount(blockSize)}", relativeRangeTags(4, 8))
        return "<div class=\"roundbox generic\" $byteRangeDataTags>$header $bs</div>"
    }
}

class ACBlock(val origSize: ULong, val compSize: ULong, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>):
    ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val isUncompressed = compSize == origSize
        val tag = bwvalue(if (isUncompressed) "Raw Data Block" else "Compressed Block", rangeTagsFor(-1, -1))
        val origLen = bwvalue("Original Length: ${humanReadableByteCount(origSize)}", relativeRangeTags(0, 8))
        val compLen = bwvalue("Stored Length: ${humanReadableByteCount(compSize)}", relativeRangeTags(8, 8))
        val payload = if(isUncompressed) {
            ByteWitch.quickDecode(data, sourceByteRange.first+16)
        }
        else
            BWAnnotatedData(
                "Data elided, first 32B: ",
                data.untilIndex(32),
                Pair(sourceByteRange.first + 16, sourceByteRange.second)
            )

        val wrapped = wrapIfSameColour(payload, data, relativeRangeTags(16, data.size))

        return "<div class=\"roundbox generic\" $byteRangeDataTags>$tag $origLen $compLen $wrapped</div>"
    }
}

