package decoders

import ParseCompanion
import bitmage.ByteOrder
import bitmage.fromHex
import bitmage.hex

// https://learn.microsoft.com/en-us/windows/win32/cmpapi/
object MSZIP : ByteWitchDecoder, ParseCompanion() {
    override val name = "MSZIP"

    private val crc_mul =
        (0 until 256).map {
            // Multiplication with x^8 in reversed CRC32 polynomial
            (0 until 8).fold(it.toUInt()) { acc, _ -> (acc shr 1) xor (if (acc and 1u != 0u) 0xEDB88320u else 0u) }
        }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        // Some test data:
        // >>> compress(b"Hello, world!" + b"\x00" * 64678 + b"This is some test data").hex()
        // '0a51e5c01800c102c9fc000000000000008000000000000043000000434bedc1310d00200c0030a4c03f2108816fc9121eec2384b6736756f45b27d76800000000000000000000000000000000000000000000000000000000000000f0a70748000000434bedc2c10900210c00b0553a940b080ae7437cb4fbe3ad212404000000000000000000000000000000000000000000000000000000000000785afb56c63fcf9e51332b46af7e01'

        // The magic bytes are hardcoded in CompressOrDecompress of Cabinet.dll.
        val magic = readBytes(data, 6)
        check(magic.contentEquals("0a51e5c01800".fromHex())) { "expecting Win32 Compression magic bytes" }

        // CRC over other header values
        val actual_crc = readUInt(data, 1)
        val computed_crc = crc32(data.sliceArray(parseOffset until parseOffset + 17)) and 0xFFu
        check(actual_crc == computed_crc) { "computed $computed_crc header checksum but read $actual_crc" }

        // Note: This header format is shared for all Win32 Compression API algorithms (XPRESS, XPRESS_HUFF, LZMS)
        //       but we only implement MSZIP here. If other Win32 Compression formats are added in the future,
        //       we can use this header parser for all of them.
        val algorithm = readUInt(data, 1)
        check(algorithm == 0x02u) { "data is Win32 Compression, but not MSZIP data" }

        val decompressed_length = readULong(data, 8, ByteOrder.LITTLE)
        val first_chunk_decompressed_length = readULong(data, 8, ByteOrder.LITTLE)
        val header =
            MSZIPHeader(
                actual_crc,
                decompressed_length,
                first_chunk_decompressed_length,
                Pair(sourceOffset, sourceOffset + parseOffset)
            )
        val chunks = mutableListOf<MSZIPChunk>()

        while (parseOffset < data.size) {
            val start = sourceOffset + parseOffset
            val chunk_size = readInt(data, 4, false, ByteOrder.LITTLE)
            val chunk_magic = readUInt(data, 2, ByteOrder.LITTLE)
            check(chunk_magic == 0x4B43u) { "expected 'CK' chunk magic bytes but found $chunk_magic" }
            val chunk_data = readBytes(data, chunk_size - 2)
            chunks.add(MSZIPChunk(chunk_size, chunk_data, Pair(start, sourceOffset + parseOffset)))
        }

        return MSZIPData(header, chunks, Pair(sourceOffset, sourceOffset + parseOffset))
    }

    private fun crc32(data: ByteArray): UInt {
        // MSZIP uses CRC32(magic bytes) = 0xE73FDBADu as the initial checksum value
        return 0xFFFFFFFFu xor
            data.toUByteArray().fold(0xE73FDBADu xor 0xFFFFFFFFu) { acc, byte ->
                var top = acc.toUByte() xor byte
                (acc shr 8) xor crc_mul[top.toInt()]
            }
    }
}

class MSZIPData(val header: MSZIPHeader, val chunks: List<MSZIPChunk>, override val sourceByteRange: Pair<Int, Int>) :
    ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"roundbox generic largecollection\" $byteRangeDataTags>${header.renderHTML()} ${chunks.joinToString(" ") { "<div class=\"bpvalue\">${it.renderHTML()}</div>" }}</div>"
    }
}

class MSZIPHeader(
    val crc: UInt,
    val decompressedLength: ULong,
    val firstChunkLength: ULong,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {

    override fun renderHTML(): String {
        val magicHTML = "<div class=\"bpvalue\" ${relativeRangeTags(0, 6)}>WIN32_COMPRESSED_DATA</div>"
        val crcHTML = "<div class=\"bpvalue\" ${relativeRangeTags(6, 1)}>CRC: 0x${crc.toString(16)}</div>"
        val algorithmHTML = "<div class=\"bpvalue\" ${relativeRangeTags(7, 1)}>COMPRESS_ALGORITHM_MSZIP</div>"
        val totalLengthHTML = "<div class=\"bpvalue\" ${relativeRangeTags(8, 8)}>Decompressed: $decompressedLength B</div>"
        val firstChunkLengthHTML =
            "<div class=\"bpvalue\" ${relativeRangeTags(16, 8)}>First Chunk Decompressed: $firstChunkLength B</div>"
        return "<div class=\"flexy\">$magicHTML $crcHTML $algorithmHTML $totalLengthHTML $firstChunkLengthHTML</div>"
    }
}

class MSZIPChunk(val size: Int, val compressedData: ByteArray, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML(): String {
        val sizeHTML = "<div class=\"bpvalue\" ${relativeRangeTags(0, 4)}>$size B</div>"
        val magicHTML = "<div class=\"bpvalue\" ${relativeRangeTags(4, 2)}>CK</div>"
        val compressedDataHTML =
            "<div class=\"bpvalue data\" ${relativeRangeTags(6, compressedData.size)}>${compressedData.hex()}</div>"
        return "<div class=\"flexy\" $byteRangeDataTags>$sizeHTML $magicHTML $compressedDataHTML</div>"
    }
}
