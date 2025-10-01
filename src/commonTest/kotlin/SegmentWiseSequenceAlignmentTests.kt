import decoders.SwiftSegFinder.SSFField
import kotlin.test.Test
import kotlin.test.assertEquals
import SequenceAlignment.SegmentWiseSequenceAlignment
import bitmage.fromHex
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals

class SegmentWiseSequenceAlignmentTests {

    @Test
    fun testCanberraDistanceEqualSegments() {
        val segmentA = "010203".fromHex()
        val segmentB = "010203".fromHex()
        val dist = SegmentWiseSequenceAlignment.canberraDistance(segmentA, segmentB)
        assertEquals(0.0, dist)
    }

    @Test
    fun testCanberraDistanceUnequalSegments() {
        val segmentA = "0204".fromHex()
        val segmentB = "0001".fromHex()
        val dist = SegmentWiseSequenceAlignment.canberraDistance(segmentA, segmentB)
        assertEquals(1.6, dist)
    }

    @Test
    fun testCanberraUlmDissimilarityEqualLength() {
        val segmentA = "0000".fromHex()
        val segmentB = "0000".fromHex()
        val dissim = SegmentWiseSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, SSFField.UNKNOWN, SSFField.UNKNOWN)
        assertEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityUnequalLength() {
        val segmentA = "000000".fromHex()
        val segmentB = "0000".fromHex()
        val dissim = SegmentWiseSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, SSFField.UNKNOWN, SSFField.UNKNOWN)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityEqualSegment() {
        val segmentA = "000011".fromHex()
        val segmentB = "0000".fromHex()
        val dissim = SegmentWiseSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, SSFField.UNKNOWN, SSFField.UNKNOWN)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityShiftedSegment() {
        val segmentA = "010203".fromHex()
        val segmentB = "0001020304".fromHex()
        val dissim = SegmentWiseSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, SSFField.UNKNOWN, SSFField.UNKNOWN)
        assertNotEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityEqualsCanberraDistance() {
        val segmentA = "A3B7".fromHex()
        val segmentB = "C701".fromHex()
        val dissim = SegmentWiseSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, SSFField.UNKNOWN, SSFField.UNKNOWN)
        val dist = SegmentWiseSequenceAlignment.canberraDistance(segmentA, segmentB)
        assertEquals(dissim, dist/2)
    }

    @Test
    fun testCanberraUlmDissimilarityLengthField() {
        val segmentA = "0102".fromHex()
        val segmentB = "3478".fromHex()
        val dissim = SegmentWiseSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN)
        assertEquals(0.0, dissim)
    }

    @Test
    fun testCanberraUlmDissimilarityLongLengthField() {
        val segmentA = "0013627282".fromHex()
        val segmentB = "3488733711".fromHex()
        val dissim = SegmentWiseSequenceAlignment.canberraUlmDissimilarity(segmentA, segmentB, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN)
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

        val matrix = SegmentWiseSequenceAlignment.calcNeedlemanWunschMatrix(m, n, matrixS, gapPenalty)

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
        val input = "10203040".fromHex()
        val result = SegmentWiseSequenceAlignment.averagePoolSegment(input, 2)
        val expected = "1838".fromHex() // [10,20] → 18; [30,40] → 38
        assertContentEquals(expected, result)
    }

    @Test
    fun testAveragePoolSegmentUnevenSplit1() {
        val input = "10203040506070".fromHex()
        val result = SegmentWiseSequenceAlignment.averagePoolSegment(input, 3)
        val expected = "183860".fromHex() // [10,20]→18; [30,40]→38; [50,60,70]→60
        assertContentEquals(expected, result)
    }

    @Test
    fun testAveragePoolSegmentUnevenSplit2() {
        val input = "AB9DE2C34A".fromHex()
        val result = SegmentWiseSequenceAlignment.averagePoolSegment(input, 3)
        val expected = "ABBF86".fromHex() // [AB]→AB; [9D,E2]→BF; [C3,4A]→86
        assertContentEquals(expected, result)
    }

    @Test
    fun testAveragePoolSegmentEmptyInput() {
        val input = byteArrayOf()
        val result = SegmentWiseSequenceAlignment.averagePoolSegment(input, 3)
        val expected = "000000".fromHex() // no input → erverything should be 0
        assertContentEquals(expected, result)
    }
}
