package bitmage

import kotlin.math.pow

enum class ByteOrder {
    BIG, LITTLE
}

@OptIn(ExperimentalUnsignedTypes::class)
fun ByteArray.hex() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }
fun ByteArray.fromIndex(i: Int) = sliceArray(i until size)
fun ByteArray.untilIndex(i: Int) = sliceArray(0 until i)
fun ByteArray.decodeAsUTF16BE(): String {
    // oh my
    val shorts = this.toList().chunked(2).map { UInt.fromBytes(it.toByteArray(), ByteOrder.BIG).toUShort() }.toTypedArray()
    return shorts.map { Char(it) }.toCharArray().concatToString()
}

fun ByteArray.decodeAsUTF16LE(): String {
    val shorts = this.toList().chunked(2).map { UInt.fromBytes(it.toByteArray(), ByteOrder.LITTLE).toUShort() }.toTypedArray()
    return shorts.map { Char(it) }.toCharArray().concatToString()
}

fun ByteArray.indexOfSubsequence(target: ByteArray): Int {
    var targetPosition = -1
    var offset = 0
    var matchIndex = 0

    while(offset < size) {
        if(this[offset] == target[matchIndex]) {
            matchIndex += 1
            if(matchIndex == target.size) {
                targetPosition = offset - target.size + 1
                break
            }
        }
        else
            matchIndex = 0
        offset++
    }

    return targetPosition
}

fun ByteArray.indicesOfAllSubsequences(target: ByteArray): Set<Int> {
    var offset = 0
    var matchIndex = 0
    val matches = mutableSetOf<Int>()

    while(offset < size) {
        if(this[offset] == target[matchIndex]) {
            matchIndex += 1
            if(matchIndex == target.size) {
                matches.add(offset - target.size + 1)
                matchIndex = 0
            }
        }
        else
            matchIndex = 0
        offset++
    }

    return matches
}

// Integers

fun ULong.Companion.fromBytes(bytes: ByteArray, byteOrder: ByteOrder): ULong {
    check(bytes.size <= 8) { "trying to parse oversized bytearray ${bytes.hex()} as ULong" }
    val orderedBytes = if(byteOrder == ByteOrder.BIG) bytes.reversed() else bytes.toList()
    return orderedBytes.mapIndexed { index, byte ->  byte.toUByte().toULong() shl (index * 8)}.sum()
}
fun Long.Companion.fromBytes(bytes: ByteArray, byteOrder: ByteOrder) = ULong.fromBytes(bytes, byteOrder).toLong()

fun UInt.Companion.fromBytes(bytes: ByteArray, byteOrder: ByteOrder): UInt {
    check(bytes.size <= 4) { "trying to parse oversized bytearray ${bytes.hex()} as UInt" }
    return ULong.fromBytes(bytes, byteOrder).toUInt()
}
fun Int.Companion.fromBytes(bytes: ByteArray, byteOrder: ByteOrder, explicitlySigned: Boolean = false): Int {
    val value = UInt.fromBytes(bytes, byteOrder).toInt()

    // usually, we only get negative integers when the input is 4 bytes long and represents a signed negative int
    // however, we might want to interpret 1, 2, or 3-byte signed ints. these will be negative if their highest bit
    // is set, i.e. if the unsigned value is >= 1 shl bit-size
    return if(!explicitlySigned || bytes.size == 4 || value < 1 shl (bytes.size*8 - 1))
        value
    else {
        value - (1 shl (bytes.size*8))
    }
}

fun Long.toBytes(byteOrder: ByteOrder): ByteArray {
    val bytesBE = byteArrayOf(
        (this shr 56).toByte(),
        (this shr 48).toByte(),
        (this shr 40).toByte(),
        (this shr 32).toByte(),
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        (this shr 0).toByte()
    )
    return if(byteOrder == ByteOrder.BIG) bytesBE else bytesBE.reversed().toByteArray()
}
fun ULong.toBytes(byteOrder: ByteOrder) = this.toLong().toBytes(byteOrder)

fun Int.toBytes(byteOrder: ByteOrder): ByteArray {
    val bytesBE = byteArrayOf((this shr 24).toByte(), (this shr 16).toByte(), (this shr 8).toByte(), (this shr 0).toByte())
    return if(byteOrder == ByteOrder.BIG) bytesBE else bytesBE.reversed().toByteArray()
}
fun UInt.toBytes(byteOrder: ByteOrder) = this.toInt().toBytes(byteOrder)

// Floats

fun Float.Companion.fromBytes(bytes: ByteArray, byteOrder: ByteOrder): Float {
    check(bytes.size == 4) { "trying to read Float from incorrectly sized bytearray ${bytes.hex()}" }
    return Float.fromBits(Int.fromBytes(bytes, byteOrder))
}

fun Float.Companion.fromFP16Bytes(bytes: ByteArray, byteOrder: ByteOrder): Float {
    check(bytes.size == 2) { "trying to read FP16 from incorrectly sized bytearray ${bytes.hex()}" }
    val sign = bytes[0].toInt() shr 7
    val exponent = (bytes[0].toInt() shr 2) and 0x1f
    val mantissa = ((bytes[0].toInt() and 0x03) shl 8) or bytes[1].toInt()
    val signMultiplier = if(sign == 0) 1 else -1

    return when {
        exponent == 0 && mantissa == 0 -> 0.0f
        exponent == 0 -> signMultiplier * 0.000061035f *  (mantissa.toFloat() / 1024)
        exponent == 31 && mantissa != 0 -> Float.NaN
        exponent == 31 && sign == 1 -> Float.NEGATIVE_INFINITY
        exponent == 31 && sign == 0 -> Float.POSITIVE_INFINITY
        else -> signMultiplier * (2.0f).pow(exponent-15) * (1 + (mantissa.toFloat() / 1024))
    }
}

fun Float.toBytes(byteOrder: ByteOrder) = this.toBits().toBytes(byteOrder)

fun Double.Companion.fromBytes(bytes: ByteArray, byteOrder: ByteOrder): Double {
    check(bytes.size == 8) { "trying to read Double from incorrectly sized bytearray ${bytes.hex()}" }
    return Double.fromBits(Long.fromBytes(bytes, byteOrder))
}

fun Double.toBytes(byteOrder: ByteOrder) = this.toBits().toBytes(byteOrder)

// ByteArray reading convenience functions

fun ByteArray.readInt(byteOrder: ByteOrder): Int {
    check(size >= 4) { "trying to read Int from undersized bytearray ${this.hex()}" }
    return Int.fromBytes(this.sliceArray(0 until 4), byteOrder)
}

fun ByteArray.readLong(byteOrder: ByteOrder): Long {
    check(size >= 8) { "trying to read Long from undersized bytearray ${this.hex()}" }
    return Long.fromBytes(this.sliceArray(0 until 8), byteOrder)
}

fun ByteArray.readFloat(byteOrder: ByteOrder): Float {
    check(size >= 4) { "trying to read Float from undersized bytearray ${this.hex()}" }
    return Float.fromBytes(this.sliceArray(0 until 4), byteOrder)
}

fun ByteArray.readDouble(byteOrder: ByteOrder): Double {
    check(size >= 8) { "trying to read Double from undersized bytearray ${this.hex()}" }
    return Double.fromBytes(this.sliceArray(0 until 8), byteOrder)
}

fun String.fromHex(): ByteArray {
    check(length % 2 == 0) { "trying to parse hex string of uneven length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun Byte.toHex(): String = this.toUByte().toString(16).padStart(2, '0')



// by ephemient from https://slack-chats.kotlinlang.org/t/527242/i-have-a-bytearray-of-utf-16-encoded-bytes-read-from-a-cinte
@OptIn(ExperimentalUnsignedTypes::class)
fun UShortArray.utf16BEToUtf8(): UByteArray {
    var i = if (this.firstOrNull() == 0xFFEF.toUShort()) 1 else 0 // skip BOM
    val bytes = UByteArray((this.size - i) * 3)
    var j = 0
    while (i < this.size) {
        val codepoint = when (val unit = this[i++].toInt()) {
            in Char.MIN_HIGH_SURROGATE.code..Char.MAX_HIGH_SURROGATE.code -> {
                if (i !in this.indices) throw CharacterCodingException() // unpaired high surrogate
                val lowSurrogate = this[i++].toInt()
                val highSurrogate = unit
                if (lowSurrogate !in Char.MIN_LOW_SURROGATE.code..Char.MAX_LOW_SURROGATE.code) {
                    throw CharacterCodingException() // unpaired high surrogate
                }

                val code = ((highSurrogate - 0xd800) shl 10) or (lowSurrogate - 0xdc00) + 0x10000

                if (code !in 0x010000..0x10FFFF) {
                    throw CharacterCodingException() // non-canonical encoding
                }
                code
            }

            in Char.MIN_LOW_SURROGATE.code..Char.MAX_LOW_SURROGATE.code -> {
                throw CharacterCodingException() // unpaired low surrogate
            }

            else -> unit
        }
        when (codepoint) {
            in 0x00..0x7F -> bytes[j++] = codepoint.toUByte()
            in 0x80..0x07FF -> {
                bytes[j++] = 0xC0.or(codepoint and 0x07C0 shr 6).toUByte()
                bytes[j++] = 0x80.or(codepoint and 0x003F).toUByte()
            }

            in 0x0800..0xFFFF -> {
                bytes[j++] = 0xE0.or(codepoint and 0xF000 shr 12).toUByte()
                bytes[j++] = 0x80.or(codepoint and 0x0FC0 shr 6).toUByte()
                bytes[j++] = 0x80.or(codepoint and 0x003F).toUByte()
            }

            in 0x10000..0x10FFFF -> {
                bytes[j++] = 0xF0.or(codepoint and 0x3C0000 shr 18).toUByte()
                bytes[j++] = 0x80.or(codepoint and 0x03F000 shr 12).toUByte()
                bytes[j++] = 0x80.or(codepoint and 0x000FC0 shr 6).toUByte()
                bytes[j++] = 0x80.or(codepoint and 0x00003F).toUByte()
            }

            else -> throw IllegalStateException()
        }
    }
    return bytes.sliceArray(0 until j)
}

fun String.toUnicodeCodepoints(): List<Int> {
    var i = 0
    val codepoints = mutableListOf<Int>()
    while (i < this.length) {
        val codepoint = when (val unit = this[i++].code) {
            in Char.MIN_HIGH_SURROGATE.code..Char.MAX_HIGH_SURROGATE.code -> {
                if (i !in this.indices) throw CharacterCodingException() // unpaired high surrogate
                val lowSurrogate = this[i++].code
                val highSurrogate = unit
                if (lowSurrogate !in Char.MIN_LOW_SURROGATE.code..Char.MAX_LOW_SURROGATE.code) {
                    throw CharacterCodingException() // unpaired high surrogate
                }

                val code = ((highSurrogate - 0xd800) shl 10) or (lowSurrogate - 0xdc00) + 0x10000

                if (code !in 0x010000..0x10FFFF) {
                    throw CharacterCodingException() // non-canonical encoding
                }
                code
            }

            in Char.MIN_LOW_SURROGATE.code..Char.MAX_LOW_SURROGATE.code -> {
                throw CharacterCodingException() // unpaired low surrogate
            }

            else -> unit
        }
        codepoints.add(codepoint)
    }
    return codepoints
}

fun decodeBase32(values: List<Int>): ByteArray {
    var currentByte = 0
    var missingBits = 8
    val bytes = mutableListOf<Byte>()
    values.forEach { v ->
        val newBits = (v and 0x1F)
        //Logger.log("new bits: ${newBits.toString(2)}, current byte: ${currentByte.toString(2)}, missing bits $missingBits")
        // decoded character fits entirely into current byte
        if(missingBits == 5) {
            currentByte = currentByte or newBits
            //Logger.log("finishing byte: ${currentByte.toString(2)}")
            bytes.add(currentByte.toByte())
            currentByte = 0
            missingBits = 8
        }
        else if (missingBits > 5) {
            currentByte = currentByte or (newBits shl (missingBits-5))
            missingBits -= 5
            //Logger.log("adding bits: ${currentByte.toString(2)} now missing $missingBits")
        }
        else {
            val remainingBits = 5 - missingBits
            currentByte = currentByte or (newBits shr remainingBits)
            //Logger.log("finishing byte: ${currentByte.toString(2)}")
            bytes.add(currentByte.toByte())
            currentByte = newBits shl (8 - remainingBits)
            missingBits = 8 - remainingBits
            //Logger.log("adding bits: ${currentByte.toString(2)} now missing $missingBits")
        }
    }

    return bytes.toTypedArray().toByteArray()
}