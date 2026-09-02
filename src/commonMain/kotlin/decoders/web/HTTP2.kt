package decoders.web

import ParseCompanion
import bitmage.ByteOrder
import decoders.ByteWitchDecoder
import decoders.ByteWitchResult
import decoders.bwvalue

object HTTP2: ByteWitchDecoder, ParseCompanion() {
    override val name = "HTTP/2"

    private val typeMap = mapOf<Int, String>(
        0 to "DATA",
        1 to "HEADERS",
        2 to "PRIORITY",
        3 to "RST_STREAM",
        4 to "SETTINGS",
        5 to "PUSH_PROMISE",
        6 to "PING",
        7 to "GOAWAY",
        8 to "WINDOW_UPDATE",
        9 to "CONTINUATION",
    )

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0

        val frames = mutableListOf<HTTPFrame>()

        while(parseOffset < data.size) {
            val start = sourceOffset + parseOffset
            val len = readInt(data, 3, false, ByteOrder.BIG)
            val type = readInt(data, 1)

            check(type < 10) { "unexpected http frame type: $type"}

            val flags = readInt(data, 1)
            val stream = readInt(data, 4)
            val payload = readBytes(data, len)

            if(flags == 0 && stream == 0 && type == 0 && len == 0)
                throw Exception("HTTP2: all-zero frame is most likely not actually http")

            frames.add(HTTPFrame(len, type, flags, stream, payload, Pair(start, sourceOffset+parseOffset)))
        }

        return HTTPFrameCollection(frames, Pair(sourceOffset, sourceOffset+parseOffset))
    }

    class HTTPFrameCollection(val frames: List<HTTPFrame>, override val sourceByteRange: Pair<Int, Int>):
        ByteWitchResult {
        override val colour = ByteWitchResult.Colour.GENERIC

        override fun renderHTML(): String {
            return "<div class=\"roundbox generic\" $byteRangeDataTags>${frames.joinToString(" ") { wrapIfSameColour(it) }}</div>"
        }
    }

    class HTTPFrame(val len: Int, val type: Int, val flags: Int, val stream: Int, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>):
        ByteWitchResult {
        override val colour = ByteWitchResult.Colour.GENERIC

        override fun renderHTML(): String {
            val typeField = bwvalue("Type: ${typeMap.getOrElse(type) { "UNK($type)" }}", relativeRangeTags(3, 1))
            val flagsField = bwvalue("Flags: 0b${flags.toString(2).padStart(8, '0')}", relativeRangeTags(4, 1))
            val streamField = bwvalue("Stream: $stream", relativeRangeTags(5, 4))

            val decodeAttempt = ByteWitch.quickDecode(data, sourceByteRange.first + 9)
            val payloadField = wrapIfSameColour(decodeAttempt, data, relativeRangeTags(9, data.size))

            return "<div class=\"roundbox generic\" $byteRangeDataTags>$typeField $flagsField $streamField $payloadField</div>"
        }
    }
}