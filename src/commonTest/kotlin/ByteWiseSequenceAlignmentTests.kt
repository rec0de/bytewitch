import SequenceAlignment.AlignmentUtils.byteCanberra
import SequenceAlignment.ByteWiseSequenceAlignment.calcNeedlemanWunschByteMatrix
import decoders.Nemesys.NemesysField
import kotlin.test.Test
import kotlin.test.assertEquals
import SequenceAlignment.NemesysSequenceAlignment
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals

class ByteWiseSequenceAlignmentTests {

    @Test
    fun testCalcNeedlemanWunschByteMatrix() {
        val a = byteArrayOf(0x10, 0x20) // = [16, 32]
        val b = byteArrayOf(0x10, 0x30) // = [16, 48]
        val gapPenalty = -1.0

        // calc similarity
        val sim00 = 1.0 - byteCanberra(0x10, 0x10) // = 0.0 → sim = 1.0
        val sim11 = 1.0 - byteCanberra(0x20, 0x30) // = |32-48| / (32+48) = 16/80 = 0.2 → sim = 0.8

        val matrix = calcNeedlemanWunschByteMatrix(a, b, gapPenalty)

        val expected = arrayOf(
            doubleArrayOf( 0.0, -1.0, -2.0),
            doubleArrayOf(-1.0, sim00, maxOf(sim00 + gapPenalty, -2.0, -1.0 + gapPenalty)),
            doubleArrayOf(-2.0, maxOf(-1.0 + gapPenalty, sim00 + gapPenalty, -2.0 + gapPenalty), sim00 + sim11)
        )

        for (i in expected.indices) {
            for (j in expected[i].indices) {
                assertEquals(expected[i][j], matrix[i][j], absoluteTolerance = 0.0001, message = "Mismatch at ($i, $j)")
            }
        }
    }

}