package decoders

import ParseCompanion
import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.hex
import bitmage.readInt

object DMAP : ByteWitchDecoder, ParseCompanion() {
    override val name = "DMAP / KeyBag"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        val tlvs = readTLVs(data, sourceOffset)
        return DmapResult(tlvs, Pair(sourceOffset, sourceOffset+data.size))
    }

    private fun readTLVs(data: ByteArray, sourceOffset: Int): List<DmapTlv> {
        val payloads = mutableListOf<DmapTlv>()
        var start: Int

        while(parseOffset < data.size) {
            start = parseOffset + sourceOffset
            val key = readBytes(data, 4)
            val length = readUInt(data, 4, ByteOrder.BIG)

            check(key.all { it.toInt().toChar().isLetter() }) { "DMAP key should be ASCII letters, was ${key.hex()}" }
            check(length <= (data.size - parseOffset).toUInt()) { "DMAP TLV length longer than remaining data: $length"}

            val value = readBytes(data, length.toInt())
            payloads.add(DmapTlv(key.decodeToString(), length.toInt(), value, Pair(start, parseOffset+sourceOffset)))
        }

        return payloads
    }

    override fun findDecodableSegments(data: ByteArray): List<Pair<Int, Int>> {
        return super.findDecodableSegments(data)
    }
}


class DmapResult(val tlvs: List<DmapTlv>, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    override fun renderHTML(): String {
        return if(tlvs.size == 1)
            tlvs.first().renderHTML()
        else
            "<div class=\"generic roundbox\" $byteRangeDataTags>${tlvs.joinToString("") { "<div class=\"bwvalue\">${it.renderHTML()}</div>" }}</div>"
    }
}

data class DmapTlv(
    val key: String,
    val length: Int,
    val value: ByteArray,
    override val sourceByteRange: Pair<Int, Int>
) : ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    private val supportedTypes = mapOf<String, Int>(
        "VERS" to 4,
        "TYPE" to 4,
        "WRAP" to 4,
        "ITER" to 4,
        "CLAS" to 4,
        "DPIC" to 4,
    )

    override fun renderHTML(): String {
        val decode = ByteWitch.quickDecode(value, sourceByteRange.second - value.size)

        // reduce unnecessary visual nesting
        val payloadHTML = when {
            semanticRenderingSupported(key, length) -> "<div class=\"bwvalue\" ${relativeRangeTags(8, length)}>${renderSupported(key, value)}</div>"
            decode is DmapResult -> "<div class=\"bwvalue flexy\">${decode.tlvs.joinToString(" ") { it.renderHTML() }}</div>"
            else -> wrapIfDifferentColour(decode, value, relativeRangeTags(8, length))

        }

        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\" ${relativeRangeTags(0, 4)}>$key</div><div class=\"bpvalue\" ${relativeRangeTags(4, 4)}>Len: $length B</div>$payloadHTML</div>"
    }

    private fun semanticRenderingSupported(key: String, length: Int): Boolean {
        return supportedTypes.contains(key) && supportedTypes[key] == length
    }

    private fun renderSupported(key: String, value: ByteArray): String {
        // based on https://github.com/dinosec/iphone-dataprotection
        return when(key) {
            "VERS" -> "Version ${value.readInt(ByteOrder.BIG)}"
            "ITER", "DPIC" -> "${value.readInt(ByteOrder.BIG)} iterations"
            "TYPE" -> {
                when(val id = value.readInt(ByteOrder.BIG)) {
                    0 -> "System (0)"
                    1 -> "Backup (1)"
                    2 -> "Escrow (2)"
                    3 -> "iCloud OTA (3)"
                    else -> "unknown ($id)"
                }
            }
            "WRAP" -> {
                when(val id = value.readInt(ByteOrder.BIG)) {
                    0 -> "None? (0)"
                    1 -> "AES with key 0x835 (1)"
                    2 -> "AES with passcode key (2)"
                    else -> "unknown ($id)"
                }
            }
            "CLAS" -> {
                when(val id = value.readInt(ByteOrder.BIG)) {
                    1 -> "ProtectionComplete / DPC A(1)"
                    2 -> "ProtectionCompleteUnlessOpen / DPC B (2)"
                    3 -> "ProtectionCompleteUntilFirstUserAuthentication / DPC C (3)"
                    4 -> "ProtectionNone / DPC D (4)"
                    5 -> "ProtectionRecovery (5)"
                    6 -> "AccessibleWhenUnlocked (6)"
                    7 -> "AccessibleAfterFirstUnlock (7)"
                    8 -> "AccessibleAlways (8)"
                    9 -> "AccessibleWhenUnlockedThisDeviceOnly (9)"
                    10 -> "AccessibleAfterFirstUnlockThisDeviceOnly (10)"
                    11 -> "AccessibleAlwaysThisDeviceOnly (11)"
                    else -> "unknown ($id)"
                }
            }
            else -> throw Exception("this should be unreachable")
        }
    }
}