package SequenceAlignment

import SequenceAlignment.AlignmentUtils.byteCanberra
import decoders.SwiftSegFinder.SSFField
import decoders.SwiftSegFinder.SSFParsedMessage

// object for byte wise sequence alignment. Instead of segments this returns byte indices
object ByteWiseSequenceAlignment : AlignmentResult<SSFParsedMessage> {
    // main function for sequence alignment
    override fun align(messages: Map<Int, SSFParsedMessage>): List<AlignedSequence> {
        val alignments = mutableListOf<AlignedSequence>()
        val thresholdSimilarity = 0.83

        for ((i, msgA) in messages) {
            for ((j, msgB) in messages) {
                if (i >= j) continue

                val bytesA = msgA.bytes
                val bytesB = msgB.bytes

                val nwMatrix = calcNeedlemanWunschByteMatrix(bytesA, bytesB, -1.0)

                // Traceback
                var x = bytesA.size
                var y = bytesB.size
                while (x > 0 && y > 0) {
                    val score = nwMatrix[x][y]
                    val diag = nwMatrix[x - 1][y - 1]
                    val up = nwMatrix[x - 1][y]
                    val left = nwMatrix[x][y - 1]

                    val sim = 1.0 - byteCanberra(bytesA[x - 1], bytesB[y - 1])
                    if (score == diag + sim) {
                        if (sim >= thresholdSimilarity) {
                            alignments.add(AlignedSequence(i, j, x - 1, y - 1, 1.0 - sim))
                        }
                        x--
                        y--
                    } else if (score == up - 1.0) {
                        x--
                    } else {
                        y--
                    }
                }
            }
        }

        return alignments
    }

    // calc Needleman-Wunsch Matrix NW
    fun calcNeedlemanWunschByteMatrix(a: ByteArray, b: ByteArray, gapPenalty: Double): Array<DoubleArray> {
        val m = a.size
        val n = b.size
        val matrix = Array(m + 1) { DoubleArray(n + 1) }

        // init matrix
        for (i in 0..m) matrix[i][0] = i * gapPenalty
        for (j in 0..n) matrix[0][j] = j * gapPenalty

        // fill matrix
        // go through each byte of both messages
        for (i in 1..m) {
            for (j in 1..n) {
                val sim = 1.0 - byteCanberra(a[i - 1], b[j - 1]) // compare similarity of two bytes

                val match = matrix[i - 1][j - 1] + sim
                // no 'mismatch' value needed because that's already included in matrixS by having a lower score
                val delete = matrix[i - 1][j] + gapPenalty
                val insert = matrix[i][j - 1] + gapPenalty
                matrix[i][j] = maxOf(match, delete, insert)
            }
        }

        return matrix
    }
}