package decoders.web

import ParseCompanion
import bitmage.ByteOrder
import bitmage.hex
import decoders.BWGenericData
import decoders.BWGenericSequence
import decoders.BWString
import decoders.ByteWitchDecoder
import decoders.ByteWitchResult
import kotlin.experimental.xor

object WebSocket: ByteWitchDecoder, ParseCompanion() {
    override val name = "WebSocket"

    val opcodes = mapOf<Int, String>(
        0 to "continuation",
        1 to "text",
        2 to "binary",
        8 to "close",
        9 to "ping",
        10 to "pong"
    )

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0

        val subresults = mutableListOf<ByteWitchResult>()

        val header1 = readInt(data, 1)
        check(header1 and 0b01110000 == 0) { "WebSocket reserved header flags not zero" }
        val opcode = header1 and 0x0F
        val fin = header1 and 0x80 != 0

        check(opcode in opcodes.keys) { "WebSocket: Unsupported opcode $opcode"}
        subresults.add(BWString("WebSocket Frame: ${opcodes[opcode]}${if(fin) ", FIN" else ""}", Pair(sourceOffset, sourceOffset+parseOffset)))

        var start = parseOffset+sourceOffset
        val header2 = readInt(data, 1)
        val masked = header2 and 0x80 != 0
        val length = header2 and 0x7F
        var effectiveLength = length.toLong()

        check(effectiveLength > 0){ "WebSocket payload length should be > 0" }

        if(length == 126) {
            effectiveLength = readInt(data, 2, byteOrder = ByteOrder.BIG, explicitlySigned = false).toLong()
            check(effectiveLength > 125){ "WebSocket payload length not encoded in least bits" }
        }
        else if(length == 127) {
            effectiveLength = readLong(data, 8, byteOrder = ByteOrder.BIG)
            check(effectiveLength > 65535){ "WebSocket payload length not encoded in least bits" }
        }

        subresults.add(BWString("Length: ${effectiveLength}B${if(masked) ", masked" else ""}", Pair(start, sourceOffset+parseOffset)))

        var maskKey: ByteArray? = null
        if(masked) {
            start = parseOffset+sourceOffset
            maskKey = readBytes(data, 4)
            subresults.add(BWString("Masking key: 0x${maskKey.hex()}", Pair(start, sourceOffset+parseOffset)))
        }

        check(effectiveLength.toInt() == data.size - parseOffset)

        start = parseOffset+sourceOffset
        val payload = readBytes(data, effectiveLength.toInt())

        val effectivePayload = if(maskKey != null) payload.mapIndexed { i, v -> v xor maskKey[i % 4] }.toByteArray() else payload

        subresults.add(BWGenericData(effectivePayload, Pair(start, sourceOffset+parseOffset)))

        return BWGenericSequence(subresults, Pair(sourceOffset, sourceOffset+parseOffset))
    }
}