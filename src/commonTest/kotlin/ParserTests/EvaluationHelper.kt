package ParserTests

import SequenceAlignment.AlignedSegment
import SequenceAlignment.NemesysSequenceAlignment
import decoders.Nemesys.NemesysParsedMessage
import decoders.Nemesys.NemesysSegment
import decoders.Nemesys.NemesysUtil
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.test.assertTrue

// to test segmentation
data class TestMessage(val index: Int, val message: ByteArray, val segments: List<NemesysSegment>)

// to test segmentation for multiple similar messages
data class MessageGroup(val typeId: Int, val messages: List<TestMessage>)

// to test sequence alignment
data class SequenceAlignmentTest(val messageAIndex: Int, val messageBIndex: Int, val expectedAlignments: Set<Triple<Int, Int, Pair<Int, Int>>>)


object EvaluationHelper {
    private var totalTP = 0
    private var totalFP = 0
    private var totalFN = 0

    private val fmsScores = mutableListOf<Double>()

    fun printSegmentParsingResult(
        testNumber: Int,
        expectedSegments: List<NemesysSegment>,
        actualSegments: List<NemesysSegment>) {

        val tolerance = 0
        val tp = expectedSegments.count { e -> actualSegments.any { a -> abs(a.offset - e.offset) <= tolerance } }
        val fp = actualSegments.count { a -> expectedSegments.none { e -> abs(a.offset - e.offset) <= tolerance } }
        val fn = expectedSegments.count { e -> actualSegments.none { a -> abs(a.offset - e.offset) <= tolerance } }


        totalTP += tp
        totalFP += fp
        totalFN += fn

        val precision = tp.toDouble() / (tp + fp).coerceAtLeast(1)
        val recall = tp.toDouble() / expectedSegments.size
        val f1 = 2 * precision * recall / (precision + recall).coerceAtLeast(1e-9)

        println("----- testSegmentParsing$testNumber -----")
        println("True Positives: $tp")
        println("False Positives: $fp")
        println("Precision: ${(precision * 100).toInt()}%")
        println("Recall: ${(recall * 100).toInt()}%")
        println("F1 Score: ${(f1 * 100).toInt()}%")
    }

    // use Nemesys FMS-Score
    fun printFMSScore(
        testNumber: Int,
        expectedSegments: List<NemesysSegment>,
        actualSegments: List<NemesysSegment>,
        gamma: Double = 2.0
    ) {
        val real = expectedSegments.map { it.offset }.sorted()
        val inferred = actualSegments.map { it.offset }.sorted().toSet()

        // compute sigmaR for each real field boundary. Exclude r0 and r|R|
        val deltas = mutableListOf<Double>()
        for (k in 1 until real.size - 1) {
            val rkMinus1 = real[k - 1]
            val rk = real[k]
            val rkPlus1 = real[k + 1]

            val lowerBound = rkMinus1 + (rk - rkMinus1) / 2
            val upperBound = rk + (rkPlus1 - rk) / 2

            // find closest inferred boundary in the given scope
            val candidates = inferred.filter { it in lowerBound until upperBound }
            val delta = if (candidates.isEmpty()) { // no candidate found
                Double.NEGATIVE_INFINITY
            } else { // exact match => 0, else <0 or >0
                candidates.minOf { abs(it - rk).toDouble() }.let { d ->
                    if (candidates.any { it == rk }) 0.0 else d
                }
            }

            deltas.add(delta)
        }

        // Specificity Penalty
        val specificityPenalty = exp(-((real.size - inferred.size).toDouble().pow(2)) / real.size.toDouble().pow(2))

        // Match Gain
        val matchGain = deltas.sumOf { deltaR ->
            when (deltaR) {
                Double.NEGATIVE_INFINITY -> 0.0
                0.0 -> 1.0
                else -> exp(-(deltaR / gamma).pow(2))
            }
        } / deltas.size.coerceAtLeast(1)

        // FMS Score
        val fms = specificityPenalty * matchGain
        fmsScores.add(fms)

        /*println("----- testFMSScore$testNumber -----")
        println("Real Boundaries: ${real.size}, Inferred: ${inferred.size}")
        println("Match Gain: $matchGain")
        println("Specificity Penalty: $specificityPenalty")
        println("FMS Score: ${(fms * 100).toInt()}%")*/
    }

    fun printFinalScore() {
        val precision = totalTP.toDouble() / (totalTP + totalFP).coerceAtLeast(1)
        val recall = totalTP.toDouble() / (totalTP + totalFN).coerceAtLeast(1)
        val finalF1 = 2 * precision * recall / (precision + recall).coerceAtLeast(1e-9)

        println("===== Final Evaluation =====")
        println("Total True Positives: $totalTP")
        println("Total False Positives: $totalFP")
        println("Total False Negatives: $totalFN")
        println("Final Precision: ${(precision * 100).toInt()}%")
        println("Final Recall: ${(recall * 100).toInt()}%")
        println("Final F1 Score: ${(finalF1 * 100).toInt()}%")
        println()
        println()
        println()

        // reset evaluation:
        totalTP = 0
        totalFP = 0
        totalFN = 0

        assertTrue(false, "F1 score should be at least 80%")
    }


    fun printFinalFMSScore() {
        if (fmsScores.isEmpty()) {
            println("No FMS scores to evaluate.")
            return
        }

        val averageFMS = fmsScores.average()
        val minFMS = fmsScores.minOrNull() ?: 0.0
        val maxFMS = fmsScores.maxOrNull() ?: 0.0

        println("===== Final FMS Evaluation =====")
        println("Total Tests: ${fmsScores.size}")
        println("Lowest FMS Score: ${(minFMS * 100).toInt()}%")
        println("Highest FMS Score: ${(maxFMS * 100).toInt()}%")
        println("Average FMS Score: ${(averageFMS * 100).toInt()}%")

        // assertTrue(false, "Average FMS score should be at least 80%")
    }


    fun printSequenceAlignmentResult(
        testNumber: Int,
        messages: Map<Int, NemesysParsedMessage>,
        expectedAlignments: Set<Triple<Int, Int, Pair<Int, Int>>>
    ) {
        // needed to change Triple(it.protocolB, it.protocolA, it.segmentIndexB to it.segmentIndexA) to
        // Triple(it.protocolA, it.protocolB, it.segmentIndexA to it.segmentIndexB) to compare it
        fun normalize(triple: Triple<Int, Int, Pair<Int, Int>>): Triple<Int, Int, Pair<Int, Int>> {
            val (a, b, pair) = triple
            return if (a < b || (a == b && pair.first <= pair.second)) {
                Triple(a, b, pair)
            } else {
                Triple(b, a, pair.second to pair.first)
            }
        }

        val alignments = NemesysSequenceAlignment.align(messages)
        val foundAlignments = alignments.map { Triple(it.protocolA, it.protocolB, it.segmentIndexA to it.segmentIndexB) }.toSet()

        // normalise sequence alignment so both Triples have the same order
        val normalizedExpected = expectedAlignments.map { normalize(it) }.toSet()
        val normalizedFound = foundAlignments.map { normalize(it) }.toSet()

        // TODO we need to call "val refined = NemesysParser().refineSegmentsAcrossMessages(listOf(parsedMessages))"

        val tp = normalizedFound.intersect(normalizedExpected).size
        val fp = normalizedFound.subtract(normalizedExpected).size
        val fn = normalizedExpected.subtract(normalizedFound).size

        totalTP += tp
        totalFP += fp
        totalFN += fn

        val precision = tp.toDouble() / (tp + fp).coerceAtLeast(1)
        val recall = tp.toDouble() / (tp + fn).coerceAtLeast(1)
        val f1 = 2 * precision * recall / (precision + recall).coerceAtLeast(1e-9)

        println("----- testSequenceAlignment$testNumber -----")
        println("True Positives: $tp")
        println("False Positives: $fp")
        println("False Negatives: $fn")
        println("Precision: ${(precision * 100).toInt()}%")
        println("Recall: ${(recall * 100).toInt()}%")
        println("F1 Score: ${(f1 * 100).toInt()}%")
    }


    fun printSegmentationWithSequenceAlignmentResult(
        testNumber: Int,
        actualMessages: Map<Int, NemesysParsedMessage>,
        expectedSegments: Map<Int, NemesysParsedMessage>,
        expectedAlignments: Set<Triple<Int, Int, Pair<Int, Int>>>
    ) {
        val actualAlignments = NemesysSequenceAlignment.align(actualMessages)

        // get the byte-wise alignment
        val actualByteAlignments = createByteAlignments(actualMessages, actualAlignments)
        val expectedByteAlignments = createByteAlignments(expectedSegments, convertExpectedAlignmentsToAligned(expectedAlignments))

        val (tp, fp, fn) = computeByteLevelConfusionMatrix(actualByteAlignments, expectedByteAlignments)

        totalTP += tp
        totalFP += fp
        totalFN += fn

        val precision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
        val recall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
        val f1 = if (precision + recall > 0) 2 * precision * recall / (precision + recall) else 0.0

        println("----- testSegmentationWithSequenceAlignment$testNumber -----")
        println("True Positive Bytes: $tp")
        println("False Positive Bytes: $fp")
        println("False Negative Bytes: $fn")
        println("Precision: ${(precision * 100).toInt()}%")
        println("Recall: ${(recall * 100).toInt()}%")
        println("F1 Score: ${(f1 * 100).toInt()}%")
    }

    // calculate byte-wise sequence alignment result
    private fun createByteAlignments(
        messages: Map<Int, NemesysParsedMessage>, // Map<ProtocolIndex, NemesysParsedMessage>
        alignments: List<AlignedSegment>
    ): Map<Pair<Int, Int>, Set<Pair<Int, Int>>> { // Map<Pair<ProtocolIndex, ByteIndex>, Set<Pair<ProtocolIndex, ByteIndex>>>
        val result = mutableMapOf<Pair<Int, Int>, MutableSet<Pair<Int, Int>>>()

        for ((protoA, protoB, segIdxA, segIdxB, _) in alignments) {
            val msgA = messages[protoA] ?: continue
            val msgB = messages[protoB] ?: continue

            val rangeA = NemesysUtil.getByteRange(msgA, segIdxA)
            val rangeB = NemesysUtil.getByteRange(msgB, segIdxB)

            for (byteA in rangeA) {
                val keyA = protoA to byteA
                result.getOrPut(keyA) { mutableSetOf() }.addAll(rangeB.map { protoB to it })
            }

            for (byteB in rangeB) {
                val keyB = protoB to byteB
                result.getOrPut(keyB) { mutableSetOf() }.addAll(rangeA.map { protoA to it })
            }
        }

        return result
    }

    private fun convertExpectedAlignmentsToAligned(
        expected: Set<Triple<Int, Int, Pair<Int, Int>>>
    ): List<AlignedSegment> {
        return expected.map { (protoA, protoB, pair) ->
            AlignedSegment(protoA, protoB, pair.first, pair.second, 0.0)
        }
    }

    private fun computeByteLevelConfusionMatrix(
        actual: Map<Pair<Int, Int>, Set<Pair<Int, Int>>>,
        expected: Map<Pair<Int, Int>, Set<Pair<Int, Int>>>
    ): Triple<Int, Int, Int> {
        val actualPairs = actual.flatMap { (a, bs) ->
            bs.map { b -> canonicalPair(a, b) }
        }.toSet()

        val expectedPairs = expected.flatMap { (a, bs) ->
            bs.map { b -> canonicalPair(a, b) }
        }.toSet()

        val tpPairs = actualPairs intersect expectedPairs
        val fpPairs = actualPairs subtract expectedPairs
        val fnPairs = expectedPairs subtract actualPairs

        return Triple(tpPairs.size, fpPairs.size, fnPairs.size)
    }

    private fun canonicalPair(a: Pair<Int, Int>, b: Pair<Int, Int>): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        return if (a.first < b.first || (a.first == b.first && a.second <= b.second)) {
            a to b
        } else {
            b to a
        }
    }
}