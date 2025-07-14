package decoders.SwiftSegFinder

import bitmage.hex
import bitmage.toHex
import decoders.BWAnnotatedData
import decoders.BWString

object SSFRenderer {

    // html view of the normal (non-editable) byte sequences
    fun render(parsed: SSFParsedMessage): String {
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
            val text = segmentBytes.decodeToString()

            // differentiate between field types
            when (segment.fieldType) {
                SSFField.STRING, SSFField.STRING_PAYLOAD -> """
                    <div class="ssffield roundbox data" $valueLengthTag $valueAlignId>
                        <div class="ssfvalue" $valueLengthTag>
                            $hex <span>→</span> "$text"
                        </div>
                    </div>
                """.trimIndent()

                SSFField.PAYLOAD_LENGTH_BIG_ENDIAN, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN -> {
                    val payloadLength = SSFUtil.tryParseLength(
                        bytes = bytes,
                        offset = start + sourceOffset,
                        lengthFieldSize = segmentBytes.size,
                        bigEndian = segment.fieldType == SSFField.PAYLOAD_LENGTH_BIG_ENDIAN
                    ) ?: 0

                    """
                        <div class="ssffield roundbox data" $valueLengthTag $valueAlignId>
                            <div class="ssfvalue" $valueLengthTag>
                                Payload length: "$payloadLength"
                            </div>
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
                        "$pre<div class=\"ssfvalue\" $valueLengthTag>$hex</div>$post"
                    } else {
                        "$pre${decode.renderHTML()}$post"
                    }
                }
            }
        }

        val content = "<div class=\"ssffield roundbox\"><div>${renderedFieldContents.joinToString("")}</div></div>"
        val editButton = "<div class=\"icon icon-edit edit-button\"></div>"

        return """
            <div class="ssf roundbox">
                <div class="view-default">$editButton$content</div>
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

            // create a own group for each byte
            val groupedHex = segmentBytes.mapIndexed { i, byte ->
                val offset = start + i
                val char = byte.toInt().toChar().let { c ->
                    if (c.code in 32..59 || c.code in 64..90 || c.code in 97..122) c else '.'
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

        val finishButton = "<div class=\"icon icon-finish finish-button\"></div>"
        return "$finishButton<div class='ssffield roundbox'><div><div class='ssfvalue' id=\"byteContainer\">${renderedFieldContents.joinToString("")}</div></div></div>"
    }
}