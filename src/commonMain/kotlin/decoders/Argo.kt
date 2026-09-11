package decoders

import ParseCompanion

object Argo: ByteWitchDecoder, ParseCompanion() {
    override val name = "Argo"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        val header = readInt(data, 1)
        val entries = mutableListOf<ByteWitchResult>()

        entries.add(BWString("Header: 0b${header.toString(2).padStart(8, '0')}", Pair(sourceOffset, sourceOffset+1)))

        var startOffset: Int
        while(parseOffset < data.size) {
            startOffset = parseOffset
            val blockLength = readLabel(data)

            if(blockLength == 0)
                throw Exception("Argo block length has to be non-zero")

            val labelLength = parseOffset - startOffset
            val blockData = readBytes(data, blockLength)
            entries.add(TlvChainEntry(0, blockLength, blockData, Pair(sourceOffset+startOffset, sourceOffset+parseOffset), 0, labelLength))
        }

        if(entries.size <= 1)
            throw Exception("Argo requires at least one block after header")

        return BWGenericSequence(entries, Pair(sourceOffset, sourceOffset+parseOffset))
    }

    private fun readLabel(data: ByteArray): Int {
        val raw = readVarInt(data)

        val negative = raw and 0x01L == 0x01L
        val converted = if(negative) -raw/2 else raw/2
        return converted.toInt()
    }

    private fun readVarInt(data: ByteArray): Long {
        var continueFlag = true
        val numberBytes = mutableListOf<Int>()

        while(continueFlag) {
            val byte = readInt(data, 1)
            continueFlag = (byte and 0x80) != 0
            val value = byte and 0x7f
            numberBytes.add(value)
        }

        // little endian
        numberBytes.reverse()

        check(numberBytes.size <= 10){ "overly long varint: ${numberBytes.size} bytes" }

        // we might clip the top bits of a >64bit int here
        var assembled = 0L
        numberBytes.forEach {
            assembled = assembled shl 7
            assembled = assembled or it.toLong()
        }

        return assembled
    }
}