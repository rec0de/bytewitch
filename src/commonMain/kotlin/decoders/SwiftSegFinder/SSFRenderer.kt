package decoders.SwiftSegFinder

import bitmage.hex
import bitmage.toHex
import decoders.BWAnnotatedData
import decoders.BWString
import htmlEscape

object SSFRenderer {

    // html view of the normal (non-editable) byte sequences
    fun renderSegmentWiseHTML(parsed: SSFParsedMessage): String {
        val sourceOffset = 0
        val msgIndex = parsed.msgIndex
        val segments = parsed.segments
        val bytes = parsed.bytes

        val renderedFieldContents = segments.mapIndexed { index, segment ->
            val start = segment.offset
            val end = if (index + 1 < segments.size) segments[index + 1].offset else bytes.size
            val segmentBytes = bytes.sliceArray(start until end)

            val valueLengthTag = " data-start='${start + sourceOffset}' data-end='${end + sourceOffset}'"
            val valueAlignId = " value-align-id='$msgIndex-$index'"
            val hex = segmentBytes.hex()
            val text = htmlEscape(segmentBytes.decodeToString())

            // differentiate between field types
            when (segment.fieldType) {
                SSFField.STRING, SSFField.STRING_PAYLOAD -> "<div class=\"ssffield roundbox data\" $valueLengthTag $valueAlignId>\"$text\"</div>"

                SSFField.PAYLOAD_LENGTH_BIG_ENDIAN, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN,
                SSFField.MESSAGE_LENGTH_BIG_ENDIAN, SSFField.MESSAGE_LENGTH_LITTLE_ENDIAN -> {
                    val payloadLength = SSFUtil.tryParseLength(
                        bytes = bytes,
                        offset = start + sourceOffset,
                        lengthFieldSize = segmentBytes.size,
                        bigEndian = segment.fieldType == SSFField.PAYLOAD_LENGTH_BIG_ENDIAN
                    ) ?: 0

                    """
                        <div class="ssffield roundbox data" $valueLengthTag $valueAlignId>
                            Length field: ${payloadLength}B
                        </div>
                    """.trimIndent()
                }

                else -> {
                    val decode = ByteWitch.quickDecode(segmentBytes, start + sourceOffset)

                    // check if we have to wrap content
                    val requiresWrapping = decode == null || decode is BWString || decode is BWAnnotatedData
                    val pre = if (requiresWrapping) "<div class=\"ssffield roundbox data\" $valueLengthTag $valueAlignId>" else "<div $valueAlignId>"
                    val post = if (requiresWrapping) "</div>" else "</div>"

                    // if it doesn't find a suitable decoder show the hex output
                    if (decode == null) {
                        "$pre$hex$post"
                    } else {
                        "$pre${decode.renderHTML()}$post"
                    }
                }
            }
        }

        val content = "<div class=\"ssffield segmentwise roundbox\"><div>${renderedFieldContents.joinToString("")}</div></div>"
        val editButton = "<div class=\"icon icon-edit edit-button\" title=\"Edit Segments\"></div>"
        val alignmentButton = "<div class=\"icon icon-alignment alignment-button\" title=\"Run Sequence Alignment\" style=\"display:none;\"></div>"
        val toggleButton = "<span class=\"alignment-toggle-legend\" style=\"display:none;\">S<div class=\"icon icon-toggle-left toggle-seqalign-button\" title=\"Use Bytewise Alignment\"></div>B</span>"
        val iconBar = "<div class=\"icon-bar\">$editButton$alignmentButton$toggleButton</div>"

        return """
            <div class="ssf roundbox">
                <div class="view-default">$iconBar$content</div>
                <div class="view-editable" style="display:none;">${renderEditableHTML(parsed)}</div>
            </div>
        """.trimIndent()
    }

    // html view for byte-wise sequence alignment
    fun renderByteWiseHTML(parsed: SSFParsedMessage): String {
        val segments = parsed.segments
        val bytes = parsed.bytes
        val msgIndex = parsed.msgIndex

        val renderedFieldContents = segments.mapIndexed { index, segment ->
            val start = segment.offset
            val end = if (index + 1 < segments.size) segments[index + 1].offset else bytes.size
            val segmentBytes = bytes.sliceArray(start until end)

            val groupedHex = segmentBytes.mapIndexed { i, byte ->
                val offset = start + i
                val char = byte.toInt().toChar().let { c ->
                    if (c.code == 0x20) '.'// replace space character with a dot
                    else if (c.code in 32..59 || c.code in 64..90 || c.code in 97..122) c
                    else '.'
                }

                val valueAlignId = " value-align-id='$msgIndex-$offset'"
                """
                    <div class='bytegroup' data-start='$offset' data-end='${offset + 1}' $valueAlignId>
                        <div class="byte-hex">${byte.toHex()}</div>
                        <div class='ascii-char'>$char</div>
                    </div>
                """.trimIndent()
            }.joinToString("")

            val valueLengthTag = " data-start='$start' data-end='$end'"

            "<div class=\"ssffield roundbox data bytewise\" $valueLengthTag>$groupedHex</div>".trimIndent()
        }

        val content = "<div class=\"ssffield roundbox\"><div>${renderedFieldContents.joinToString("")}</div></div>"
        val editButton = "<div class=\"icon icon-edit edit-button\"></div>"

        val alignmentButton = "<div class=\"icon icon-alignment alignment-button\" title=\"Run Sequence Alignment\" style=\"display:none;\"></div>"
        val toggleButton = "<span class=\"alignment-toggle-legend\" style=\"display:none;\">S<div class=\"icon icon-toggle-right toggle-seqalign-button\" title=\"Use Segmentwise Alignment\"></div>B</span>"
        val iconBar = "<div class=\"icon-bar\">$editButton$alignmentButton$toggleButton</div>"

        return """
            <div class="ssf roundbox">
                <div class="view-default">$iconBar$content</div>
                <div class="view-editable" style="display:none;">${renderEditableHTML(parsed)}</div>
            </div>
        """.trimIndent()
    }


    // html view of editable byte sequences
    private fun renderEditableHTML(parsed: SSFParsedMessage): String {
        val segments = parsed.segments
        val bytes = parsed.bytes

        val renderedFieldContents = segments.mapIndexed { index, segment ->
            val start = segment.offset
            val end = if (index + 1 < segments.size) segments[index + 1].offset else bytes.size
            val segmentBytes = bytes.sliceArray(start until end)

            // create an own group for each byte
            val groupedHex = segmentBytes.mapIndexed { i, byte ->
                val offset = start + i
                val char = byte.toInt().toChar().let { c ->
                    if (c.code == 0x20) '.'// replace space character with a dot
                    else if (c.code in 32..59 || c.code in 64..90 || c.code in 97..122) c
                    else '.'
                }
                """
                    <div class='bytegroup' data-start='$offset' data-end='${offset + 1}'>
                        ${byte.toHex()}
                        <div class='ascii-char'>$char</div>
                    </div>
                """.trimIndent()
            }.joinToString("<div class=\"separator-placeholder\"></div>")


            if (end != bytes.size) {
                "$groupedHex<div class='field-separator'>|</div>"
            } else {
                groupedHex
            }
        }

        val finishButton = "<div class=\"icon icon-finish finish-button\" title=\"Confirm Changes\"></div>"
        val iconBar = "<div class=\"icon-bar\">$finishButton</div>"

        return "$iconBar<div class='ssffield roundbox'><div><div id=\"byteContainer\">${renderedFieldContents.joinToString("")}</div></div></div>"
    }
}