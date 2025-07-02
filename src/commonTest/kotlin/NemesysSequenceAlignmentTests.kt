import decoders.Nemesys.NemesysField
import kotlin.test.Test
import kotlin.test.assertEquals
import SequenceAlignment.NemesysSequenceAlignment
import kotlin.test.assertContentEquals
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
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, NemesysField.UNKNOWN, NemesysField.UNKNOWN)
        assertEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityUnequalLength() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("00 00 00")
        val segmentB = ByteWitch.getBytesFromInputEncoding("00 00")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, NemesysField.UNKNOWN, NemesysField.UNKNOWN)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityEqualSegment() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("00 00 11")
        val segmentB = ByteWitch.getBytesFromInputEncoding("00 00")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, NemesysField.UNKNOWN, NemesysField.UNKNOWN)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityShiftedSegment() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("01 02 03")
        val segmentB = ByteWitch.getBytesFromInputEncoding("00 01 02 03 04")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, NemesysField.UNKNOWN, NemesysField.UNKNOWN)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityEqualsCanberraDistance() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("A3 B7")
        val segmentB = ByteWitch.getBytesFromInputEncoding("C7 01")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, NemesysField.UNKNOWN, NemesysField.UNKNOWN)
        val dist = NemesysSequenceAlignment.canberraDistance(segmentA, segmentB)
        assertEquals(dissim, dist/2)
    }

    @Test
    fun testCanberraUlmDissimilarityLengthField() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("01 02")
        val segmentB = ByteWitch.getBytesFromInputEncoding("34 78")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN, NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN)
        assertEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityLongLengthField() {
        val segmentA = ByteWitch.getBytesFromInputEncoding("0013627282")
        val segmentB = ByteWitch.getBytesFromInputEncoding("3488733711")
        val dissim = NemesysSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN, NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN)
        assertEquals(0.0, dissim)
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

    @Test
    fun testAveragePoolSegmentExactSplit() {
        val input = ByteWitch.getBytesFromInputEncoding("10203040")
        val result = NemesysSequenceAlignment.averagePoolSegment(input, 2)
        val expected = ByteWitch.getBytesFromInputEncoding("1838") // [10,20] → 18; [30,40] → 38
        assertContentEquals(expected, result)
    }

    @Test
    fun testAveragePoolSegmentUnevenSplit1() {
        val input = ByteWitch.getBytesFromInputEncoding("10203040506070")
        val result = NemesysSequenceAlignment.averagePoolSegment(input, 3)
        val expected = ByteWitch.getBytesFromInputEncoding("183860") // [10,20]→18; [30,40]→38; [50,60,70]→60
        assertContentEquals(expected, result)
    }

    @Test
    fun testAveragePoolSegmentUnevenSplit2() {
        val input = ByteWitch.getBytesFromInputEncoding("AB9DE2C34A")
        val result = NemesysSequenceAlignment.averagePoolSegment(input, 3)
        val expected = ByteWitch.getBytesFromInputEncoding("ABBF86") // [AB]→AB; [9D,E2]→BF; [C3,4A]→86
        assertContentEquals(expected, result)
    }

    @Test
    fun testAveragePoolSegmentEmptyInput() {
        val input = byteArrayOf()
        val result = NemesysSequenceAlignment.averagePoolSegment(input, 3)
        val expected = ByteWitch.getBytesFromInputEncoding("000000") // no input → erverything should be 0
        assertContentEquals(expected, result)
    }
}
