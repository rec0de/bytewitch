import bitmage.ByteOrder
import bitmage.fromBytes
import kotlin.math.max
import kotlin.math.min


// replicate required Date functionality from JVM
expect class Date(timestamp: Long) {
    fun toAppleTimestamp(): Double
}

expect fun currentTimestamp(): Long

expect fun dateFromUTCString(string: String, fullYear: Boolean): Date

fun looksLikeUtf16String(string: String): Double {
    val printableASCII = string.filter { it.code in 32..126 || it.code in 9..13 }
    val weirdASCII = string.filter { it.code in 0..8 || it.code in 14..31 }
    val veryWeird = string.any { it.code == 0xFFFD || it.category in setOf(CharCategory.UNASSIGNED, CharCategory.PRIVATE_USE) }

    if(veryWeird || string.isEmpty())
        return 0.0

    val lengthBias = max((10-string.length).toDouble()/10, 0.0) * 0.6

    // string is mostly printable ASCII, seems plausible
    val asciiPercentage = printableASCII.length.toDouble() / string.length
    if(asciiPercentage > 0.8)
        return max(asciiPercentage - lengthBias, 0.0)

    // at this point, we have no unassigned or private use characters, and any surrogates are in valid pairs
    // let's sort the characters into some bins matching the largest character blocks on the BMP
    val bins = IntArray(18){ 0 }

    string.forEach {
        when(it.code) {
            in 0x4e00..0x9fff -> bins[0] += 1 // CJK Unified Ideographs
            in 0xac00..0xd7af -> bins[1] += 1 // Hangul Syllables
            in 0x3400..0x4dbf -> bins[2] += 1 // CJK Extension A
            in 0xd800..0xdfff -> bins[3] += 1 // Surrogates (high or low)
            in 0xa000..0xa48f -> bins[4] += 1 // Yi Syllables
            in 0xfb50..0xfdff -> bins[5] += 1 // Arabic Presentation Forms-A
            in 0x1400..0x167f -> bins[6] += 1 // Unified Canadian Aboriginal Syllabics
            in 0xf900..0xfaff -> bins[7] += 1 // CJK Compatibility Ideographs
            in 0x1200..0x137f -> bins[8] += 1 // Ethiopic
            in 0xa500..0xa63f -> bins[9] += 1 // Vai
            in 0x2e80..0x2eff -> bins[10] += 1 // CJK Radicals and unlikely chars
            in 0xa800..0xa95f, in 0xa980..0xaaff -> bins[11] += 1 // Various Brahmic scripts
            in 0x0590..0x8ff -> bins[12] += 1 // Various Semitic & right-to-left
            in 0x2200..0x26ff -> bins[13] += 1 // Symbols, Math
            in 0x0032..0x007e -> bins[14] += 1 // Basic Latin (ASCII)
            in 0x0080..0x024f -> bins[15] += 1 // Extended Latin
            in 0x0400..0x058f -> bins[16] += 1 // Various Cyrillic
            else -> bins[17] += 1 // others
        }
    }

    Logger.log(bins.joinToString(" "))
    
    val rares = listOf(bins[2], bins[4], bins[5], bins[6], bins[7], bins[10], weirdASCII.length)
    val multipleRares = rares.count { it > 0 } > 1
    val rareNonAsciiShare = rares.sum().toDouble() / (string.length - printableASCII.length)
    val hasRareCJK = bins[2] > 0
    // multiple different rare characters are super suspicious
    // a message consisting of only rare characters from one particular bin, however, is plausible
    // and single rare CJK characters may also occur in otherwise common CJK text
    val rareCharactersPenalty = if(multipleRares || weirdASCII.isNotEmpty()) rareNonAsciiShare * 5 else if(hasRareCJK) rareNonAsciiShare else (1-rareNonAsciiShare)

    // CJK characters are the most likely to generate "randomly" and should usually not co-occur with non-CJK characters, excluding common ASCII
    val mixedCJKnonCJKPenalty = if((bins[0]+bins[1]+bins[2]) > 0 && (bins[5] + bins[6] + bins[8] + bins[9] + bins[11] + bins[12] + bins[15] + bins[16]) > 0) 1.0 else 0.0

    // han characters and hangul syllables make up for most codepoints - therefore both occurring in the same decode is likely for non-Unicode data
    // however, some han characters also appear in korean texts - if 70% of characters are hangul, we consider this a plausible korean text
    // the reverse (some hangul mixed with mostly han characters) is highly unlikely
    val mixedHanHangulPenalty = if((bins[0]+bins[2]) > 0 && bins[1] > 0 && (bins[1].toDouble()/(bins[0]+bins[1]+bins[2]) < 0.7)) 1.0 else 0.0

    // randomly generated valid surrogate pairs should be extremely rare
    val surrogatesBonus = bins[3]

    // codepoints spread over many blocks / alphabets are a red flag
    val binCountPenalty = max(bins.count { it > 0 } - 3, 0)

    val score = max(0.0, 1.0 + surrogatesBonus*0.25 + asciiPercentage/2 - rareCharactersPenalty*2 - mixedCJKnonCJKPenalty * 0.5 - mixedHanHangulPenalty*0.3 - binCountPenalty*0.25 - lengthBias)
    Logger.log("rare $rareCharactersPenalty, mixCJK $mixedCJKnonCJKPenalty, mixHan $mixedHanHangulPenalty, surrogate $surrogatesBonus, ascii $asciiPercentage, bins $binCountPenalty, length $lengthBias, final $score, string $string")

    return score
}

fun looksLikeUtf8String(data: ByteArray): Double {
    val string = data.decodeToString()

    // there are a lot of ways to cause decoding errors
    // random / non-UTF-8 bytes should trigger them reliably
    if(string.any { it.code == 0xFFFD })
        return 0.0

    // if the string has no decoding errors and contains multi-byte UTF8 sequences
    // we can be pretty sure this is a valid string
    if(string.length < min(data.size.toDouble() - 3, data.size * 0.8))
        return 1.0

    // otherwise, default to generic UTF16 judgment
    return looksLikeUtf16String(string)
}

fun dateFromAppleTimestamp(timestamp: Double): Date {
    return Date((timestamp*1000).toLong() + 978307200000)
}

fun htmlEscape(string: String): String {
    val toEscape = "<>&'\""
    return string.map { char ->
        if(char in toEscape) "&#${char.code};" else "$char"
    }.joinToString("")
}

open class ParseCompanion {
    protected var parseOffset = 0

    protected fun readBytes(bytes: ByteArray, length: Int): ByteArray {
        val sliced = bytes.sliceArray(parseOffset until parseOffset + length)
        parseOffset += length
        return sliced
    }

    protected fun readLengthPrefixedString(bytes: ByteArray, sizePrefixLen: Int): String? {
        val len = readInt(bytes, sizePrefixLen)
        return if(len == 0) null else readString(bytes, len)
    }

    protected fun readString(bytes: ByteArray, size: Int): String {
        val str = bytes.sliceArray(parseOffset until parseOffset +size).decodeToString()
        parseOffset += size
        return str
    }

    protected fun readInt(bytes: ByteArray, size: Int): Int {
        val int = Int.fromBytes(bytes.sliceArray(parseOffset until parseOffset +size), ByteOrder.BIG)
        parseOffset += size
        return int
    }

    protected fun readUInt(bytes: ByteArray, size: Int): UInt {
        val int = UInt.fromBytes(bytes.sliceArray(parseOffset until parseOffset +size), ByteOrder.BIG)
        parseOffset += size
        return int
    }
}