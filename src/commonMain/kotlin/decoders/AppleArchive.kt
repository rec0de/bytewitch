package decoders

import Date
import ParseCompanion
import bitmage.ByteOrder
import bitmage.hex

/*
    Apple Archive (.aar / .yaa), magic "AA01" or legacy "YAA1" - the container format used by
    macOS/iOS's `aa` tool, OTA payloads, etc. Decodes a single entry (one file/dir/symlink/...);
    see https://theapplewiki.com/wiki/Apple_Archive for the format this is based on.

    Layout: magic(4) + header size(2, LE, counts the whole header including magic+size field
    itself, so always >= 6) + a run of 4-byte-tagged fields up to that many bytes total:

      - a tag ending in a digit N (e.g. "UID1", "MOD2") holds a fixed N-byte value
      - "TYP1" (covered by the digit rule) holds a 1-byte type char, see [typeNames]
      - "PATP"/"LNKP" hold a path / symlink-target string, length-prefixed by a 2-byte LE integer
      - a tag ending in "MT" (e.g. "MTMT" modification time, "CTMT" change time) holds a 12-byte
        timestamp: 8-byte LE seconds since epoch + 4-byte LE nanoseconds
      - "DATx"/"XATx" (content / extended attributes) are blobs: x is a size code fixing the
        width of the length that follows - 'A' = 2 bytes, 'B' = 4, 'C' = 8 - but unlike every
        other field, a blob's actual bytes are NOT part of the header: they're appended after it
        ends, in declaration order (confirmed both empirically and by the wiki page above)

    The declared header size is the authority for where the header ends - fields are read until
    exactly that many bytes are consumed, which also doubles as validation: a mismatch, or a
    field this decoder doesn't recognize, throws rather than guessing.
*/
object AppleArchive : ByteWitchDecoder, ParseCompanion() {
    override val name = "Apple Archive"

    private val magics = setOf("AA01", "YAA1")

    private val typeNames = mapOf(
        'F' to "file",
        'D' to "directory",
        'L' to "symlink",
        'P' to "fifo",
        'C' to "char special",
        'B' to "block special",
        'S' to "socket",
        'W' to "whiteout",
        'R' to "door",
        'T' to "port",
        'M' to "metadata"
    )

    private val tags = mapOf(
        "ACL" to "Access Control List",
        "BTM" to "Backup Time",
        "CKS" to "Checksum",
        "CLC" to "Clone Cluster ID",
        "CTM" to "Creation Time",
        "DAT" to "Data",
        "DEV" to "Device ID",
        "DE2" to "Device Minor",
        "DUZ" to "Disk Usage",
        "FLG" to "Flags",
        "GID" to "Group ID",
        "GIN" to "Group Name",
        "HLC" to "Hard Link Cluster ID",
        "IDX" to "Reference Archive Offset",
        "IDZ" to "Reference Archive Size",
        "INO" to "Inode Number",
        "LNK" to "Symbolic Link Path",
        "MOD" to "Access Modes",
        "MTM" to "Modification Time",
        "NLK" to "Hard Link Count",
        "PAT" to "Path",
        "SH1" to "SHA1 Hash",
        "SH2" to "SHA2-256 Hash",
        "SH3" to "SHA2-384 Hash",
        "SH5" to "SHA2-512 Hash",
        "SIZ" to "Uncompressed Size",
        "SLC" to "Identical Data Cluster ID",
        "TYP" to "Entry Type",
        "UID" to "User ID",
        "UIN" to "User Name",
        "XAT" to "Extended Attributes",
        "YAF" to "Archived Fields List",
        "AFT" to "Padding After File",
    )

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        val entries = mutableListOf<ByteWitchResult>()

        while(data.size > parseOffset && data.sliceArray(parseOffset until parseOffset+4).decodeToString() in magics) {
            entries.add(decodeSingle(data, sourceOffset))
        }

        check(entries.isNotEmpty()) { "Apple Archive: no valid entry" }

        // little hack to better visually distinguish different entries
        return MultiPartialDecode(entries.map { Pair(it, null) }, Pair(sourceOffset, sourceOffset+parseOffset))
    }

    fun decodeSingle(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        val chips = mutableListOf<ByteWitchResult>()

        var start = parseOffset + sourceOffset
        val magic = readString(data, 4)
        check(magic in magics) { "invalid Apple Archive magic: $magic" }

        val headerSize = readInt(data, 2, explicitlySigned = false, ByteOrder.LITTLE)
        check(headerSize >= 6) { "AppleArchive: header size $headerSize smaller than magic+size field itself" }
        check(headerSize <= data.size) { "AppleArchive: header size $headerSize larger than whole payload" }

        chips.add(AARHeader(magic, headerSize, Pair(start, sourceOffset+parseOffset)))


        // For blobs, keep track of their size and the position of the placeholder in the chips list
        val expectedBlobs = mutableListOf<Pair<Int,Int>>()
        val headerEnd = start + headerSize

        while (parseOffset < headerEnd) {
            start = parseOffset + sourceOffset
            val tag = readString(data, 3)
            val subtype = readBytes(data, 1).first().toInt().toChar()

            when(tag) {
                // Basic UINTs
                "CLC", "DEV", "DE2", "DUZ", "FLG", "GID", "HLC", "IDX", "IDZ", "INO", "MOD", "NLK", "SIZ", "SLC", "TYP", "UID", "AFT" -> {
                    val valueLength = when(subtype) {
                        '1' -> 1
                        '2' -> 2
                        '4' -> 4
                        '8' -> 8
                        else -> throw Exception("AppleArchive: Expected UINT subtype for tag $tag but got $subtype")
                    }

                    val value = readULong(data, valueLength, ByteOrder.LITTLE)

                    if(tag == "TYP")
                        chips.add(AAREntry(tags[tag]!!, typeNames[value.toInt().toChar()]!!, valueLength, Pair(start, parseOffset+sourceOffset)))
                    else
                        chips.add(AAREntry(tags[tag]!!, value.toString(), valueLength,Pair(start, parseOffset+sourceOffset)))
                }
                // Strings
                "GIN", "PAT", "LNK", "UIN" -> {
                    check(subtype == 'P') { "AppleArchive: String type tag $tag has unexpected subtype $subtype" }
                    val value = readLengthPrefixedString(data, 2, ByteOrder.LITTLE) ?: "∅"
                    chips.add(AAREntry(tags[tag]!!, value, value.encodeToByteArray().size, Pair(start, parseOffset+sourceOffset)))
                }
                // Timestamps
                "BTM", "CTM", "MTM" -> {
                    val valueLength: Int
                    val value = when(subtype) {
                        'S' -> {
                            valueLength = 8
                            Date(readLong(data, 8, ByteOrder.LITTLE) * 1000)
                        }
                        'T' -> {
                            valueLength = 12
                            val seconds = readLong(data, 8, ByteOrder.LITTLE)
                            val nano = readLong(data, 4, ByteOrder.LITTLE)
                            // Date object lacks precision for nano time, so we just kinda drop it here
                            Date(seconds * 1000 + nano / 1000)
                        }
                        else -> throw Exception("AppleArchive: Expected TIMESPEC subtype for tag $tag but got $subtype")
                    }
                    chips.add(AAREntry(tags[tag]!!, value.toString(), valueLength, Pair(start, parseOffset+sourceOffset)))
                }
                // Blobs
                "ACL", "DAT", "XAT", "YAF" -> {
                    val valueStart = parseOffset
                    val blobLen = when(subtype) {
                        'A' -> readLong(data, 2, ByteOrder.LITTLE)
                        'B' -> readLong(data, 4, ByteOrder.LITTLE)
                        'C' -> readLong(data, 8, ByteOrder.LITTLE)
                        else -> throw Exception("AppleArchive: Expected BLOB subtype for tag $tag but got $subtype")
                    }.toInt() // we hope no one is putting >2GB files into bytewitch, so int cast should be ok here
                    val valueSize = parseOffset - valueStart
                    chips.add(AARBlobPlaceholder(tags[tag]!!, valueSize, blobLen, Pair(start, parseOffset+sourceOffset)))
                    expectedBlobs.add(Pair(chips.lastIndex, blobLen))
                }
                // Hashes
                "CKS", "SH1", "SH2", "SH3", "SH5" -> {
                    val valueSize = when(subtype) {
                        'F' -> 4
                        'G' -> 20
                        'H' -> 32
                        'I' -> 48
                        'J' -> 64
                        else -> throw Exception("AppleArchive: Expected HASH subtype for tag $tag but got $subtype")
                    }
                    val hash = readBytes(data, valueSize)
                    chips.add(AAREntry(tags[tag]!!, "0x${hash.hex()}", valueSize, Pair(start, parseOffset+sourceOffset)))
                }
                else -> {
                    val valueStart = parseOffset
                    val stringValue = when(subtype) {
                        '1' -> readULong(data, 1, ByteOrder.LITTLE).toString()
                        '2' -> readULong(data, 2, ByteOrder.LITTLE).toString()
                        '4' -> readULong(data, 4, ByteOrder.LITTLE).toString()
                        '8' -> readULong(data, 8, ByteOrder.LITTLE).toString()
                        'A' -> {
                            val len = readLong(data, 2, ByteOrder.LITTLE)
                            expectedBlobs.add(Pair(chips.size, len.toInt()))
                            "blob length ${len}B"
                        }
                        'B' -> {
                            val len = readLong(data, 4, ByteOrder.LITTLE)
                            expectedBlobs.add(Pair(chips.size, len.toInt()))
                            "blob length ${len}B"
                        }
                        'C' -> {
                            val len = readLong(data, 8, ByteOrder.LITTLE)
                            expectedBlobs.add(Pair(chips.size, len.toInt()))
                            "blob length ${len}B"
                        }
                        'F' -> readBytes(data, 4).hex()
                        'G' -> readBytes(data, 20).hex()
                        'H' -> readBytes(data, 32).hex()
                        'I' -> readBytes(data, 48).hex()
                        'J' -> readBytes(data, 64).hex()
                        'S' -> Date(readLong(data, 8, ByteOrder.LITTLE) * 1000).toString()
                        'T' -> Date(readLong(data, 8, ByteOrder.LITTLE) * 1000 + readLong(data, 4, ByteOrder.LITTLE) / 1000).toString()
                        'P' -> readLengthPrefixedString(data, 2, ByteOrder.LITTLE)
                        else -> throw Exception("AppleArchive: Unknown subtype $subtype")
                    }
                    chips.add(AAREntry("unknown ($tag)", stringValue ?: "∅", parseOffset-valueStart, Pair(start, parseOffset+sourceOffset)))
                }
            }
        }

        if(parseOffset < data.size) {
            expectedBlobs.forEachIndexed { index, blobInfo ->
                start = parseOffset + sourceOffset
                val blobIndex = blobInfo.first
                val size = blobInfo.second
                val data = readBytes(data, size)
                val placeholder = chips.removeAt(blobIndex) as AARBlobPlaceholder // remove placeholder
                val fullBlob = placeholder.addData(BWRangeTaggedData(data, start))
                chips.add(blobIndex, fullBlob) // place full blob at same position
            }
        }


        return BWGenericSequence(chips, Pair(sourceOffset, sourceOffset+parseOffset))
    }
}

class AARHeader(val magic: String, val length: Int, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val header = bwvalue("Archive Header: $magic", relativeRangeTags(0, 4))
        val len = bwvalue("Length: $length", relativeRangeTags(4, 2))
        return "<div class=\"roundbox generic\" $byteRangeDataTags>$header $len</div>"
    }
}

class AAREntry(val label: String, val valueRendering: String, val valueSize: Int, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val tag = bwvalue(label, relativeRangeTags(0, 3))
        val content = bwvalue(valueRendering, rangeTagsFor(sourceByteRange.second-valueSize, sourceByteRange.second))
        return "<div class=\"roundbox generic\" $byteRangeDataTags>$tag $content</div>"
    }
}

class AARBlob(val label: String, val sizeSize: Int, val size: Int, val data: BWRangeTaggedData, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val tag = bwvalue(label, relativeRangeTags(0, 3))
        val len = bwvalue("Length: $size", relativeRangeTags(4, sizeSize))

        val quickDecode = ByteWitch.quickDecode(data.data, data.start)
        val subresult = wrapIfSameColour(quickDecode, data.data, rangeTagsFor(data.start, data.start+data.data.size))

        return "<div class=\"roundbox generic\" $byteRangeDataTags>$tag $len $subresult</div>"
    }
}

class AARBlobPlaceholder(val label: String, val sizeSize: Int, val size: Int, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        val tag = bwvalue(label, relativeRangeTags(0, 3))
        val len = bwvalue("Length: $size", relativeRangeTags(4, sizeSize))
        val content = bwvalue("⚠\uFE0F missing blob", rangeTagsFor(-1, -1))
        return "<div class=\"roundbox generic\" $byteRangeDataTags>$tag $len $content</div>"
    }

    fun addData(data: BWRangeTaggedData): AARBlob {
        return AARBlob(label, sizeSize, size, data, sourceByteRange)
    }
}