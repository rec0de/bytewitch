package decoders.Nemesys

import bitmage.hex
import decoders.BWAnnotatedData
import decoders.BWString

object NemesysRenderer {

    // html view of the normal (non-editable) byte sequences
    fun render(parsed: NemesysParsedMessage): String {
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
                NemesysField.STRING -> """
                    <div class="nemesysfield roundbox data" $valueLengthTag $valueAlignId>
                        <div class="nemesysvalue" $valueLengthTag>
                            $hex <span>→</span> "$text"
                        </div>
                    </div>
                """.trimIndent()

                NemesysField.PAYLOAD_LENGTH -> {
                    val decode = ByteWitch.quickDecode(segmentBytes, start + sourceOffset)

                    // check if we have to wrap content
                    val requiresWrapping = decode == null || decode is BWString || decode is BWAnnotatedData
                    val pre = if (requiresWrapping) "<div style='color:blue' class=\"nemesysfield roundbox data\" $valueLengthTag $valueAlignId>" else "<div $valueAlignId>"
                    val post = if (requiresWrapping) "</div>" else "</div>"

                    // if it doesn't find a suitable decoder show the hex output
                    if (decode == null) {
                        "$pre<div style='color:blue' class=\"nemesysvalue\" $valueLengthTag>$hex</div>$post"
                    } else {
                        "$pre${decode.renderHTML()}$post"
                    }
                }

                else -> {
                    val decode = ByteWitch.quickDecode(segmentBytes, start + sourceOffset)

                    // check if we have to wrap content
                    val requiresWrapping = decode == null || decode is BWString || decode is BWAnnotatedData
                    val pre = if (requiresWrapping) "<div class=\"nemesysfield roundbox data\" $valueLengthTag $valueAlignId>" else "<div $valueAlignId>"
                    val post = if (requiresWrapping) "</div>" else "</div>"

                    // if it doesn't find a suitable decoder show the hex output
                    if (decode == null) {
                        "$pre<div class=\"nemesysvalue\" $valueLengthTag>$hex</div>$post"
                    } else {
                        "$pre${decode.renderHTML()}$post"
                    }
                }
            }
        }

        val content = "<div class=\"nemesysfield roundbox\"><div>${renderedFieldContents.joinToString("")}</div></div>"
        val editButton = "<div class=\"icon icon-edit edit-button\"></div>"

        return """
            <div class="nemesys roundbox">
                <div class="view-default">$editButton$content</div>
                <div class="view-editable" style="display:none;">${renderEditableHTML(parsed)}</div>
            </div>
        """.trimIndent()
    }

    // html view of editable byte sequences
    private fun renderEditableHTML(parsed: NemesysParsedMessage): String {
        val segments = parsed.segments
        val bytes = parsed.bytes

        val renderedFieldContents = segments.mapIndexed { index, segment ->
            val start = segment.offset
            val end = if (index + 1 < segments.size) segments[index + 1].offset else bytes.size
            val segmentBytes = bytes.sliceArray(start until end)

            // create byte-groups of two bytes
            val groupedHex = segmentBytes.hex().chunked(2).joinToString("<div class=\"separator-placeholder\"></div>") {
                "<div class='bytegroup'>$it</div>"
            }

            if (end != bytes.size) {
                "$groupedHex<div class='field-separator'>|</div>"
            } else {
                groupedHex
            }
        }

        val finishButton = "<div class=\"icon icon-finish finish-button\"></div>"
        return "$finishButton<div class='nemesysfield roundbox'><div><div class='nemesysvalue' id=\"byteContainer\">${renderedFieldContents.joinToString("")}</div></div></div>"
    }
}