import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.toUnicodeCodepoints
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


// replicate required Date functionality from JVM
expect class Date(timestamp: Long) {
    fun toAppleTimestamp(): Double
}

expect fun currentTimestamp(): Long

expect fun dateFromUTCString(string: String, fullYear: Boolean): Date

fun looksLikeUtf8String(data: ByteArray): Double {
    val string = data.decodeToString()
    val unusual = string.filter { !it.toString().matches(Regex("[a-zA-Z0-9\\-\\./,;\\(\\)_ \t\r\n<>]")) }

    val lengthBias = max((10-string.length).toDouble()/10, 0.0) * 0.6

    val veryWeird = string.any{ it.code == 0xFFFD || (it.code < 32 && it.code !in listOf(9, 10, 13)) || it.category in setOf(CharCategory.PRIVATE_USE, CharCategory.UNASSIGNED) }
    if(veryWeird) {
        //Logger.log("very weird")
        return 0.0
    }

    val unusualPercentage = unusual.length.toDouble() / string.length

    // ... ok we'll have to do some deep unicode magic to get good results here
    if(unusualPercentage > 0.2) {
        // strings are utf16 characters - for stuff outside the BMP, we have to convert to codepoints
        val codepoints = unusual.toUnicodeCodepoints()
        val unicodeLength = codepoints.size // better approximation of string length
        val nonLatinLetters = unusual.filter { it.isLetter() } // unusual letters ON BMP ONLY
        // unicode blocks emoticons, miscellaneous symbols and pictograms, supplemental symbols and pictograms
        val emoji = codepoints.count { it in 0x1F600..0x1F64F || it in 0x1F900..0x1F9FF || it in 0x1F300..0x1F5FF }

        // we consider BMP letters + emojis to be plausible by default
        val unusualValidLetterPercentage = (nonLatinLetters.length + emoji).toDouble() / max(unicodeLength, 1)

        // if the only uncommon characters are emoji, that's a very good sign
        val emojiExclusiveBonus = if(emoji == unicodeLength) 0.75 else 0.0

        // if the unicode codepoint length is shorter than the utf16 character length, we encountered at least one valid surrogate pair
        // this is unlikely to happen randomly
        val validSurrogatePairBonus = if(unicodeLength < unusual.length) 0.5 else 0.0

        // unlikely to be found in real text:
        val suspicious = codepoints.any {
            it in 0x1100..0x11ff ||
                    it in 0x17000..0x187FF || // partial korean syllables
                    it in 0x2CEB0..0x2EBEF || // tangut script from the yuan dynasty
                    it in 0x30000..0x3134F || // historic han characters
                    it in 0xA000..0xA48F ||   // yi syllables from nuosu
                    it in 0x18800..0x18AFF || // tangut components
                    it in 0xFB50..0xFDFF || // arabic presentation codes
                    it in 0x1B170..0x1B2FF || // nüshu women's script
                    it in 0x10600..0x1077F || // undeciphered linear a script
                    it in 0xA500..0xA63F || // vai language
                    it in 0x2F00..0x2FDF || // han radicals
                    it in 0x1400..0x167F || // canadian aboriginal script
                    it in 0x1200..0x137F // ethiopic
        }
        if(suspicious)
            return 0.0

        val korean = nonLatinLetters.any { it.code in 0xAC00..0xD7AF }
        val cyrillic = nonLatinLetters.any { it.code in 0x0400..0x04FF }
        val arabic = nonLatinLetters.any { it.code in 0x0600..0x06FF }
        val rareHan = codepoints.any { it in 0x20000..0x2A6DF || it in 0x2B820..0x2CEAF}
        val commonHan = nonLatinLetters.any { it.code in 0x3400..0x4DBF || it.code in 0x4E00..0x9FFF } // CJK extension A
        val rareLatin = nonLatinLetters.any { it.code in 0x1E00..0x1EFF }

        // we most likely will never see rare han characters - but if we do, we definitely should have seen a common character around it
        // also, cyrillic characters probably don't appear with han or korean ones
        if((rareHan && !commonHan) || (rareLatin && commonHan) || (cyrillic && (commonHan || korean || rareLatin)) || (arabic && (cyrillic || korean || commonHan || rareLatin)))
            return 0.0

        // korean text _may_ include han characters, but mixing many different scripts is suspicious
        val mixedScriptBias = if(korean && commonHan) -0.4 else 0.0
        val rarePenalty = if(rareHan || rareLatin) -0.3 else 0.0

        val nonExclusiveCJKPenalty = if(korean || commonHan)
            -nonLatinLetters.count { !(it.code in 0x3400..0x4DBF || it.code in 0x4E00..0x9FFF || it.code in 0xAC00..0xD7AF)}.toDouble()*2 / nonLatinLetters.length
        else
            0.0

        Logger.log("$nonLatinLetters mixed: $mixedScriptBias rare: $rarePenalty nonExCJK: $nonExclusiveCJKPenalty emojiBonus: $emojiExclusiveBonus validSurrogateBonus: $validSurrogatePairBonus")

        // no hard science behind this - low share of unusual characters is good
        // if we have lots of unusual chars we might still consider it if most unusual letters are valid unicode letters
        // this likely still misses emoji sequences
        val score = min((1 - unusualPercentage) + unusualValidLetterPercentage.pow(4) + mixedScriptBias + rarePenalty + nonExclusiveCJKPenalty + emojiExclusiveBonus + validSurrogatePairBonus - lengthBias*1.5, 1.0)
        Logger.log("$string: $score")
        return score
    }
    else
        return max(1 - unusualPercentage - lengthBias, 0.0)
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