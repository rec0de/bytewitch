package decoders.apple

import BWGenericData
import ByteWitchDecoder
import ByteWitchResult
import bitmage.ByteOrder
import decoders.QuickDirtyDecoder
import kotlin.uuid.Uuid


class AppleBundle : QuickDirtyDecoder() {


    companion object : ByteWitchDecoder {
        override val name: String = "Apple Bundle"
        private const val MIN_SIZE = 952

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return AppleBundle().decode(data, sourceOffset)
        }
    }

    // map range names to <Offset, Size>
    private val rangeMap = mutableMapOf<String, Pair<Int, Int>>()

    fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        check(data.size >= MIN_SIZE) { "BUND: data too short" }
        rangeMap.clear()
        start(data, sourceOffset)

        // skip over unknown1, unknown2, pad
        parseOffset += 8

        processBytes(4) { magic ->
            val magicString = magic.reversedArray().decodeToString()
            check(magicString == "BUND") { "BUND: unexpected magic $magicString"}
            "BUND magic"
        }

        // skip over padding
        parseOffset += 2

        val type = processInt(2, ByteOrder.LITTLE, "Type", Format.DECIMAL)

        when (type) {
            4 -> {
                // skip over reserved data
                parseOffset += 120

                val numRanges = processULong(8, ByteOrder.LITTLE, "Range count", Format.DECIMAL).toInt()
                processBytes(16){ bytes ->
                    "UUID: ${Uuid.fromByteArray(bytes)}"
                }

                // skip over reserved
                parseOffset += 384

                val configOffset = processULong(8, ByteOrder.LITTLE, "Config offset", Format.HEX)
                processULong(8, ByteOrder.LITTLE, "Config padding", Format.BYTES)
                val configSize = processULong(8, ByteOrder.LITTLE, "Config size", Format.BYTES)

                // skip over reserved
                parseOffset += 72

                repeat(numRanges) {
                    readRange()
                }

                // ASN1 config data may be contained in urst range at given offset, split out to make parsable by ASN1 parser
                if (configSize > 0UL && rangeMap.containsKey("urst")) {
                    val effectiveConfigOffset = rangeMap["urst"]!!.first + configOffset.toInt()
                    val configData = data.sliceArray(effectiveConfigOffset until effectiveConfigOffset+configSize.toInt())
                    addForeignParsedField(BWGenericData(configData, Pair(effectiveConfigOffset+sourceOffset, effectiveConfigOffset+sourceOffset+configSize.toInt())))
                }
            }
            else -> throw Exception("BUND: unsupported bundle type $type")
        }

        return finish()
    }

    private fun readRange() {
        enterSubstruct()

        processInt(4, ByteOrder.LITTLE, "Type", Format.DECIMAL)
        val name = processBytes(4){ name -> "Name: ${name.reversedArray().decodeToString()}" }.reversedArray().decodeToString()
        val offset = processULong(8, ByteOrder.LITTLE, "Offset", Format.HEX)
        val size = processULong(8, ByteOrder.LITTLE, "Size", Format.BYTES)

        val content = data.sliceArray(offset.toInt() until (offset+size).toInt())
        addForeignParsedField(BWGenericData(content, Pair(sourceOffset+offset.toInt(), sourceOffset+offset.toInt()+size.toInt())))

        rangeMap[name] = Pair(offset.toInt(), size.toInt())

        leaveSubstruct()
    }
}