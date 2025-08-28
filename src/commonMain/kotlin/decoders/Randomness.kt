package decoders

import kotlin.math.*

object Randomness : ByteWitchDecoder {
    override val name = "randomness"

    override fun tryhardDecode(data: ByteArray) = null

    override fun confidence(data: ByteArray, sourceOffset: Int) = if(data.size >= 16) Pair(0.76, null) else Pair(0.00, null)

    private const val die = "\uD83C\uDFB2"
    private const val warning = "⚠\uFE0F"
    private const val alarm = "\uD83D\uDEA8"

    private enum class P {
        INSIGNIFICANT {
            override fun toString() = "$die random"
        },
        P_0_05 {
            override fun toString() = "$warning non-random @ p<0.05"
        },
        P_0_025 {
            override fun toString() = "$warning non-random @ p<0.025"
        },
        P_0_01 {
            override fun toString() = "$alarm non-random @ p<0.01"
        },
        P_0_001 {
            override fun toString() = "$alarm non-random @ p<0.001"
        }
    }

    private class Runs {
        var n1 = 0
        var n2 = 0
        var count = 0
        var current: Boolean? = null
        var currentRunLen = 0
        var maxRunLen = 0

        // stats from https://www.itl.nist.gov/div898/handbook/eda/section3/eda35d.htm
        fun calcExpectedRuns(n1: Int, n2: Int) = (2*n1*n2).toDouble()/(n1+n2) + 1
        fun calcRunSD(n1: Int, n2: Int) = (2*n1*n2*(2*n1*n2 - n1 - n2)).toDouble() / ((n1+n2)*(n1+n2)*(n1 + n2 - 1))
        fun calcZ(n1: Int, n2: Int) = ((count - calcExpectedRuns(n1, n2)) / calcRunSD(n1, n2)).absoluteValue

        fun calcPLevel(): P {
            return when(calcZ(n1, n2)) {
                in 0.0..1.96 -> P.INSIGNIFICANT
                in 1.96..2.24 -> P.P_0_05
                in 2.24..2.58 -> P.P_0_025
                in 2.58..3.17 -> P.P_0_01
                else -> P.P_0_001
            }
        }

        // WARNING: this assumes FAIR coin flips, i.e. uniformly random bits
        // this will not work with printable ascii run analysis by of biased bits
        fun longestRunProb(): P {
            // i'm getting to the end of my stats knowledge here, but:
            // if we look at the longest run of consecutive one or zero bits (e.g. high bits)
            // we should be able to get the probability of that run occurring _anywhere_ in the input
            // using a geometric distribution where we have a try for every bit and the probability
            // of success is 2^-(k-1) where k is the length of the run (minus one bc both all-one and all-zero works)
            // so the probability of the first run of that length to be in our message should be:
            // 1 - (1-p)^x = 1 - (1-2^-(k-1))^(n-k)
            val k = maxRunLen
            val n = n1+n2
            val runSuccessProb = (2.0).pow(-(k-1))
            val highBitMaxRunProb = 1 - (1 - runSuccessProb).pow(n-k)
            return when(highBitMaxRunProb) {
                in 0.0..0.001 -> P.P_0_001
                in 0.001..0.01 -> P.P_0_01
                in 0.01..0.025 -> P.P_0_025
                in 0.025..0.05 -> P.P_0_05
                else -> P.INSIGNIFICANT
            }
        }

        fun record(state: Boolean) {
            if(state) n1 += 1 else n2 += 1

            if(state == current) {
                currentRunLen += 1
                if(currentRunLen > maxRunLen)
                    maxRunLen = currentRunLen
            }
            else {
                current = state
                currentRunLen = 1
                count += 1
            }
        }
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val twoGramCounts = IntArray(4) { 0 }
        val fourGramCounts = IntArray(16) { 0 }
        val byteCounts = IntArray(256) { 0 }
        val oneCounts = IntArray(9) { 0 }
        var highBits = 0
        var lowBits = 0

        val highBitRuns = Runs()
        val lowBitRuns = Runs()
        val medianRuns = Runs()
        val printableRuns = Runs()

        data.forEach { byte ->
            val a = byte.toInt() and 0x03
            val b = (byte.toInt() shr 2) and 0x03
            val c = (byte.toInt() shr 4) and 0x03
            val d = (byte.toInt() shr 6) and 0x03
            twoGramCounts[a] += 1
            twoGramCounts[b] += 1
            twoGramCounts[c] += 1
            twoGramCounts[d] += 1

            val lowerNibble = (byte.toUByte().toInt() and 0x0F)
            val upperNibble = (byte.toUByte().toInt() and 0xF0) ushr 4
            fourGramCounts[lowerNibble] += 1
            fourGramCounts[upperNibble] += 1

            byteCounts[byte.toUByte().toInt()] += 1
            oneCounts[byte.countOneBits()] += 1

            val lowBit = byte.toInt() and 0x01
            val highBit = (byte.toInt() shr 7) and 0x01
            lowBits += lowBit
            highBits += highBit

            highBitRuns.record(highBit != 0)
            lowBitRuns.record(lowBit != 0)
            medianRuns.record(byte.toUByte() > 127u)
            printableRuns.record(byte in 32..126)
        }

        val expectedTwoGrams = data.size.toDouble() // * 4 (two-bit pairs per byte) / 4 (#bins)

        val entropyNibbles = fourGramCounts.map { it.toDouble() / (data.size*2) }.filter { it > 0 }.sumOf { - it * log2(it) }
        val entropyBytes = byteCounts.map { it.toDouble() / data.size }.filter { it > 0 }.sumOf { - it * log2(it) }

        val oneCountExpected = listOf(1, 8, 28, 56, 70, 56, 28, 8, 1).map { it.toDouble() / 256 }
        val oneCountChiSquare = oneCounts.mapIndexed { index, observed ->
            val expected = data.size * oneCountExpected[index]
            (observed - expected).pow(2) / expected
        }.sum()

        val twogramsChiSquare = twoGramCounts.sumOf { observed -> (observed - expectedTwoGrams).pow(2) / expectedTwoGrams }

        val expectedBits = data.size.toDouble() / 2
        val highBitsChiSquare = ((highBits - expectedBits).pow(2) / expectedBits) + (((data.size-highBits) - expectedBits).pow(2) / expectedBits)
        val lowBitsChiSquare = ((lowBits - expectedBits).pow(2) / expectedBits) + (((data.size-lowBits) - expectedBits).pow(2) / expectedBits)

        // let's try to 'prove' that the data is NOT random
        val oneCountsPLevel = when(oneCountChiSquare) {
            in 0.0..15.507 -> P.INSIGNIFICANT
            in 15.507..17.535 -> P.P_0_05
            in 17.535..20.090 -> P.P_0_025
            in 20.090..26.124 -> P.P_0_01
            else -> P.P_0_001
        }

        val twogramsPLevel = when(twogramsChiSquare) {
            in 0.0..7.815 -> P.INSIGNIFICANT
            in 7.815..9.348 -> P.P_0_05
            in 9.348..11.345 -> P.P_0_025
            in 11.345..16.266 -> P.P_0_01
            else -> P.P_0_001
        }

        val highBitsPLevel = when(highBitsChiSquare) {
            in 0.0..3.841 -> P.INSIGNIFICANT
            in 3.841..5.024 -> P.P_0_05
            in 5.024..6.635 -> P.P_0_025
            in 6.635..10.828 -> P.P_0_01
            else -> P.P_0_001
        }

        val lowBitsPLevel = when(lowBitsChiSquare) {
            in 0.0..3.841 -> P.INSIGNIFICANT
            in 3.841..5.024 -> P.P_0_05
            in 5.024..6.635 -> P.P_0_025
            in 6.635..10.828 -> P.P_0_01
            else -> P.P_0_001
        }

        val highBitRunPLevel = highBitRuns.calcPLevel()
        val highBitRunMaxLenPLevel = highBitRuns.longestRunProb()
        val lowBitRunPLevel = lowBitRuns.calcPLevel()
        val lowBitRunMaxLenPLevel = lowBitRuns.longestRunProb()
        val medianBitRunPLevel = medianRuns.calcPLevel()
        val medianBitRunMaxLenPLevel = medianRuns.longestRunProb()

        val finalJudgement = listOf(lowBitsPLevel, highBitsPLevel, twogramsPLevel, oneCountsPLevel, highBitRunPLevel, highBitRunMaxLenPLevel, lowBitRunPLevel, lowBitRunMaxLenPLevel, medianBitRunPLevel, medianBitRunMaxLenPLevel).sumOf {
            when(it) {
                P.INSIGNIFICANT -> 0
                P.P_0_05 -> 1
                P.P_0_025 -> 2
                P.P_0_01 -> 4
                P.P_0_001 -> 6
            }.toInt()
        }.toDouble() / 6

        val nibEntropyRound = (entropyNibbles*10).roundToInt().toDouble() / 10
        val byteEntropyRound = (entropyBytes*10).roundToInt().toDouble() / 10

        val byteEntropyEmoji = when {
            entropyBytes > 6.5 -> die
            entropyBytes > 5 -> "\uD83D\uDCDA"
            entropyBytes > 3 -> "📖"
            else -> "\uD83D\uDDD2\uFE0F"
        }

        val nibEntropyEmoji = when {
            entropyNibbles > 3.5 -> "\uD83C\uDFB2"
            entropyNibbles > 3.1 -> "\uD83D\uDCDA"
            entropyNibbles > 2.5 -> "📖"
            else -> "\uD83D\uDDD2\uFE0F"
        }

        val entropyNum = if(data.size > 100) byteEntropyRound else nibEntropyRound
        val entropyScale = if(data.size > 100) 8 else 4
        val entropyEmoji = if(data.size > 100) byteEntropyEmoji else nibEntropyEmoji
        val judgementString = when {
            finalJudgement > 0.8 -> "highly non-random"
            finalJudgement > 0.5 -> "non-random"
            finalJudgement > 0.25 -> "possibly non-random"
            else -> "random"
        }
        val emoji = when {
            (finalJudgement > 0.8) && entropyEmoji == die -> alarm
            (finalJudgement > 0.5) && entropyEmoji == die -> warning
            else -> entropyEmoji
        }

        if(inlineDisplay) {
            return BWAnnotatedData("<span title=\"entropy: $entropyNum/$entropyScale, looks $judgementString\">$emoji</span>", data, Pair(sourceOffset, sourceOffset+data.size))
        }
        else {
            val entropy = "<div class=\"bpvalue\">$emoji Looks <b>$judgementString</b>. Entropy: $entropyEmoji $entropyNum/${entropyScale}b</div>"
            val twogramsHTML = "<div class=\"bpvalue\">Distribution of 2-bit words: $twogramsPLevel</div>"
            val onecountsHTML = "<div class=\"bpvalue\">One bit counts per byte: $oneCountsPLevel</div>"
            val highBitsHTML = "<div class=\"bpvalue\">High bits: $highBitsPLevel, run count ${highBitRuns.calcPLevel()}, max run len=${highBitRuns.maxRunLen} ${highBitRuns.longestRunProb()}</div>"
            val lowBitsHTML = "<div class=\"bpvalue\">Low bits: $lowBitsPLevel, run count ${lowBitRuns.calcPLevel()}, max run len=${lowBitRuns.maxRunLen} ${lowBitRuns.longestRunProb()}</div>"
            val medianRunsHTML = "<div class=\"bpvalue\">Byte value runs: ${medianRuns.calcPLevel()}, max run len=${medianRuns.maxRunLen} ${medianRuns.longestRunProb()}</div>"
            //val asciiRunsHTML = "<div class=\"bpvalue\">Printable ASCII runs: ${printableRuns.calcPLevel()}, max run len=${printableRuns.maxRunLen}</div>"
            return RandomnessAnalysis(listOf(entropy, twogramsHTML, onecountsHTML, highBitsHTML, lowBitsHTML, medianRunsHTML), Pair(sourceOffset, sourceOffset+data.size))
        }
    }
}

class RandomnessAnalysis(private val boxes: List<String>, override val sourceByteRange: Pair<Int, Int>?): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"roundbox neutral\" style=\"flex-direction: column;\" $byteRangeDataTags> ${boxes.joinToString(" ")} </div>"
    }
}