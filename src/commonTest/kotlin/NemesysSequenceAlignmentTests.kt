import kotlin.test.Test
import kotlin.test.assertEquals
import decoders.Nemesys.NemesysSequenceAlignment
import kotlin.test.assertNotEquals

class NemesysSequenceAlignmentTests {

    @Test
    fun testCanberraDistanceEqualSegments() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("01 02 03")
        val segmentB = ByteWitch.getBytesFromInputEncoding("01 02 03")
        val dist = NemesysSequenceAlignment.canberraDistance(segmentA, segmentB)
        assertEquals(0.0, dist)
    }

    @Test
    fun testCanberraDistanceUnequalSegments() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("02 04")
        val segmentB = ByteWitch.getBytesFromInputEncoding("00 01")
        val dist = NemesysSequenceAlignment.canberraDistance(segmentA, segmentB)
        assertEquals(1.6, dist)
    }

    @Test
    fun testCanberraUlmDissimilarityEqualLength() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("00 00")
        val segmentB = ByteWitch.getBytesFromInputEncoding("00 00")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB)
        assertEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityUnequalLength() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("00 00 00")
        val segmentB = ByteWitch.getBytesFromInputEncoding("00 00")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityEqualSegment() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("00 00 11")
        val segmentB = ByteWitch.getBytesFromInputEncoding("00 00")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityShiftedSegment() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("01 02 03")
        val segmentB = ByteWitch.getBytesFromInputEncoding("00 01 02 03 04")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityEqualsCanberraDistance() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("A3 B7")
        val segmentB = ByteWitch.getBytesFromInputEncoding("C7 01")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB)
        val dist = NemesysSequenceAlignment.canberraDistance(segmentA, segmentB)
        assertEquals(dissim, dist/2)
    }

    @Test
    fun testCalcNeedlemanWunschMatrix() {
        val m = 2
        val n = 2
        val gapPenalty = -1.0

        // Sparse Similarity Matrix
        val matrixS = mapOf(
            0 to 0 to 0.9,  // high similarity
            1 to 1 to 0.8   // medium similarity
        )

        val matrix = NemesysSequenceAlignment.calcNeedlemanWunschMatrix(m, n, matrixS, gapPenalty)

        val expected = arrayOf(
            doubleArrayOf(0.0, -1.0, -2.0),
            doubleArrayOf(-1.0, 0.9, -0.1),
            doubleArrayOf(-2.0, -0.1, 1.7)
        )

        for (i in 0..m) {
            for (j in 0..n) {
                assertEquals(expected[i][j], matrix[i][j], absoluteTolerance = 0.0001)
            }
        }
    }
}
