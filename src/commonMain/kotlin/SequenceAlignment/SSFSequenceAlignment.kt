package SequenceAlignment

import decoders.SwiftSegFinder.SSFField
import decoders.SwiftSegFinder.SSFParsedMessage
import SequenceAlignment.AlignmentUtils.byteCanberra

// class for sequence alignment of SSF object
object SSFSequenceAlignment : AlignmentResult<SSFParsedMessage> {
    // main function for sequence alignment
    override fun align(messages: Map<Int, SSFParsedMessage>): List<AlignedSequence> {
        val alignments = mutableListOf<AlignedSequence>()
        val tresholdAlignedSegment = 0.17

        // get dissimilarity matrix (by using canberra-ulm dissimilarity)
        val sparseMatrixData = calcSparseSimilarityMatrix(messages, tresholdAlignedSegment)

        for ((pair, matrixS) in sparseMatrixData) {
            if (matrixS.isEmpty()) continue

            val (protoA, protoB) = pair

            // get maximum amount of segments in each protocol
            val maxA = matrixS.keys.maxOf { it.first } + 1
            val maxB = matrixS.keys.maxOf { it.second } + 1

            val gapPenalty = -1.0
            val matrixNW = calcNeedlemanWunschMatrix(maxA, maxB, matrixS, gapPenalty)

            // traceback
            var i = maxA
            var j = maxB
            while (i > 0 && j > 0) {
                val score = matrixNW[i][j]
                val diag = matrixNW[i - 1][j - 1]
                val up = matrixNW[i - 1][j]
                // val left = matrixNW[i][j - 1]

                val sim = matrixS[i - 1 to j - 1] ?: Double.NEGATIVE_INFINITY
                if (score == diag + sim) {
                    if (1.0 - sim < tresholdAlignedSegment) {
                        alignments.add(AlignedSequence(protoA, protoB, i - 1, j - 1, 1.0 - sim))
                    }

                    i--
                    j--
                } else if (score == up + gapPenalty) {
                    i--
                } else { // if (score == left + gapPenalty)
                    j--
                }
            }
        }

        return alignments
    }

    // calc Needleman-Wunsch Matrix NW
    fun calcNeedlemanWunschMatrix(m: Int, n:Int, matrixS: Map<Pair<Int, Int>, Double>, gapPenalty: Double): Array<DoubleArray> {
        val matrixNW = Array(m + 1) { DoubleArray(n + 1) }

        // init matrix
        for (i in 0..m) matrixNW[i][0] = i * gapPenalty
        for (j in 0..n) matrixNW[0][j] = j * gapPenalty

        // fill matrix
        for (i in 1..m) {
            for (j in 1..n) {
                // if no entry in matrixS so choose Double.NEGATIVE_INFINITY (because matrixS in a sparse Matrix)
                val sim = matrixS[i - 1 to j - 1] ?: Double.NEGATIVE_INFINITY

                val match = matrixNW[i - 1][j - 1] + sim
                // no 'mismatch' value needed because that's already included in matrixS by having a lower score
                val delete = matrixNW[i - 1][j] + gapPenalty
                val insert = matrixNW[i][j - 1] + gapPenalty
                matrixNW[i][j] = maxOf(match, delete, insert)
            }
        }

        return matrixNW
    }

    /**
     * calculate sparse dissimilarity matrix for field pairs between protocols
     *
     * @return Map< Pair(protocolA_ID, protocolB_ID) -> Map<Pair(segmentIndexA, segmentIndexB) -> similarityValue> >
     */
    private fun calcSparseSimilarityMatrix(
        messages: Map<Int, SSFParsedMessage>,
        similarityThreshold: Double
    ): Map<Pair<Int, Int>, Map<Pair<Int, Int>, Double>> {
        val result = mutableMapOf<Pair<Int, Int>, MutableMap<Pair<Int, Int>, Double>>()

        // go through all messages
        for (i in messages.keys) {
            for (j in messages.keys) {
                if (i >= j) continue // do not compare messages twice

                // extract segments and bytes
                val messageA = messages[i] ?: continue
                val messageB = messages[j] ?: continue

                val segmentsA = messageA.segments
                val segmentsB = messageB.segments
                val bytesA = messageA.bytes
                val bytesB = messageB.bytes

                val simMap = mutableMapOf<Pair<Int, Int>, Double>()

                // compare all segments form A with all segments of B
                for (segmentAIndex in segmentsA.indices) {
                    // extract bytes from segmentA
                    val (startA, typeA) = segmentsA[segmentAIndex]
                    val endA = if (segmentAIndex + 1 < segmentsA.size) segmentsA[segmentAIndex + 1].offset else bytesA.size
                    val segmentBytesA = bytesA.sliceArray(startA until endA)

                    for (segmentBIndex in segmentsB.indices) {
                        // extract bytes from segmentB
                        val (startB, typeB) = segmentsB[segmentBIndex]
                        val endB = if (segmentBIndex + 1 < segmentsB.size) segmentsB[segmentBIndex + 1].offset else bytesB.size
                        val segmentBytesB = bytesB.sliceArray(startB until endB)

                        val dissim = canberraUlmDissimilarity(segmentBytesA, segmentBytesB, typeA, typeB)
                        // val dissim = canberraDissimilarityByteWise(segmentBytesA, segmentBytesB, typeA, typeB)
                        // val dissim = canberraDissimilarityWithPooling(segmentBytesA, segmentBytesB)
                        val sim = 1.0 - dissim

                        if (sim >= similarityThreshold) {
                            // save matches for segmentA
                            simMap[segmentAIndex to segmentBIndex] = sim
                        }
                    }
                }

                result[i to j] = simMap
            }
        }

        return result
    }

    // Canberra Distance for segments of the same size
    fun canberraDistance(segmentA: ByteArray, segmentB: ByteArray): Double {
        var sum = 0.0
        for (i in segmentA.indices) {
            val ai = segmentA[i].toInt() and 0xFF
            val bi = segmentB[i].toInt() and 0xFF
            val denominator = ai + bi
            if (denominator != 0) {
                sum += kotlin.math.abs(ai - bi).toDouble() / denominator
            }
        }
        return sum
    }

    // Canberra-Ulm Dissimilarity for segments of different sizes (sliding window approach)
    fun canberraUlmDissimilarity(segmentS: ByteArray, segmentT: ByteArray, typeA: SSFField, typeB: SSFField): Double {
        val shortSegment = if (segmentS.size <= segmentT.size) segmentS else segmentT
        val longSegment = if (segmentS.size > segmentT.size) segmentS else segmentT

        var minD = Double.MAX_VALUE

        // if both segments are a payload length field so set canberra distance to 0
        if ((typeA == SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN && typeB == SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN)
            || (typeA == SSFField.PAYLOAD_LENGTH_BIG_ENDIAN && typeB == SSFField.PAYLOAD_LENGTH_BIG_ENDIAN)) {
            minD = 0.0
        } else {
            // sliding window to search for the lowest dissimilarity
            for (offset in 0..(longSegment.size - shortSegment.size)) {
                val window = longSegment.sliceArray(offset until (offset + shortSegment.size))
                val dC = canberraDistance(shortSegment, window) / shortSegment.size
                if (dC < minD) {
                    minD = dC
                }
            }
        }

        val r = (longSegment.size - shortSegment.size).toDouble() / longSegment.size
        val pf = 0.8 // hyper parameter to set the non-linear penalty

        val dm = (shortSegment.size.toDouble() / longSegment.size.toDouble()) * minD +
                r +
                (1 - minD) * r * (shortSegment.size / (longSegment.size * longSegment.size) - pf)

        return dm
    }

    // using canberra dissimilarity byte wise
    private fun canberraDissimilarityByteWise(segmentA: ByteArray, segmentB: ByteArray, typeA: SSFField, typeB: SSFField): Double {
        // if both segments are a payload length field so set canberra distance to 0
        if ((typeA == SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN && typeB == SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN)
            || (typeA == SSFField.PAYLOAD_LENGTH_BIG_ENDIAN && typeB == SSFField.PAYLOAD_LENGTH_BIG_ENDIAN)) {
            return 0.0
        }

        return needlemanWunschCanberra(segmentA, segmentB)
    }

    // canberra score using sequence alignment on two segments
    private fun needlemanWunschCanberra(segmentA: ByteArray, segmentB: ByteArray, gapPenalty: Double = 1.0): Double {
        val m = segmentA.size
        val n = segmentB.size
        val dp = Array(m + 1) { DoubleArray(n + 1) }

        // init
        for (i in 0..m) dp[i][0] = i * gapPenalty
        for (j in 0..n) dp[0][j] = j * gapPenalty

        // fill up matrix
        for (i in 1..m) {
            for (j in 1..n) {
                val match = dp[i - 1][j - 1] + byteCanberra(segmentA[i - 1], segmentB[j - 1])
                val delete = dp[i - 1][j] + gapPenalty
                val insert = dp[i][j - 1] + gapPenalty
                dp[i][j] = minOf(match, delete, insert)
            }
        }

        // dp[m][n] is the score on the bottom right of the matrix. It says the distance of the best alignment
        return dp[m][n] / maxOf(m, n).toDouble()
    }

    // Canberra Dissimilarity for segments of different sizes (using pooling)
    private fun canberraDissimilarityWithPooling(segmentA: ByteArray, segmentB: ByteArray): Double {
        val shortSegment = if (segmentA.size <= segmentB.size) segmentA else segmentB
        val longSegment = if (segmentA.size > segmentB.size) segmentA else segmentB

        // pool longer segment
        val pooledSegment = averagePoolSegment(longSegment, shortSegment.size)

        // now just use the regular canberraDistance with pooledSegment
        return canberraDistance(shortSegment, pooledSegment) / shortSegment.size
    }

    // average pooling of a segment to transform it in a lower dimension
    fun averagePoolSegment(segment: ByteArray, targetSize: Int): ByteArray {
        val pooled = ByteArray(targetSize)
        val chunkSize = segment.size.toDouble() / targetSize

        for (i in 0 until targetSize) {
            // chunk that we need to pool
            val start = (i * chunkSize).toInt()
            val end = ((i + 1) * chunkSize).toInt().coerceAtMost(segment.size)
            val chunk = segment.sliceArray(start until end)

            val avgValue = if (chunk.isNotEmpty()) {
                chunk.map { it.toInt() and 0xFF }.average().toInt() // calc average value
            } else {
                0
            }

            pooled[i] = avgValue.toByte()
        }

        return pooled
    }


    // position penalty for absolute and relative difference
    private fun computePositionPenalty(startA: Int, startB: Int, lenA: Int, lenB: Int): Double {
        val alpha = 0.02 // hyper parameter for absolute difference
        val beta = 0.03  // hyper parameter for relative difference

        // calc penalty for absolute difference
        val maxLen = maxOf(lenA, lenB).toDouble()
        val positionDiffAbs = kotlin.math.abs(startA - startB).toDouble()
        val penaltyAbs = positionDiffAbs / maxLen

        // calc penalty for relative difference
        val relativePosA = startA.toDouble() / lenA
        val relativePosB = startB.toDouble() / lenB
        val penaltyRel = kotlin.math.abs(relativePosA - relativePosB)

        return alpha * penaltyAbs + beta * penaltyRel
    }
}