import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import bitmage.fromHex
import decoders.NemesysField
import decoders.NemesysParser

class NemesysParserTests {

    private val parser = NemesysParser()

    @Test
    fun testConfidence() {
        val inputShort = "12".fromHex()
        val inputLong = "123456".fromHex()
        assertEquals(0.00, NemesysParser.confidence(inputShort))
        assertEquals(0.76, NemesysParser.confidence(inputLong))
    }

    @Test
    fun testDecode() {
        val message = "fe4781820001000000000000037777770369666303636f6d0000010001".fromHex()
        val result = NemesysParser.decode(message, 0, true) // Adjust inlineDisplay if needed

        // Check if decoding returns a non-null result
        assertTrue(result != null, "Decode result should not be null")
    }

    @Test
    fun testFindExtremaInList() {
        val input = doubleArrayOf(1.0, 3.0, 1.0, 5.0, 1.0, 7.0, 1.0)
        val expected = listOf(
            0 to -1, 1 to 1, 2 to -1, 3 to 1, 4 to -1, 5 to 1, 6 to -1
        )
        val result = parser.findExtremaInList(input)
        assertEquals(expected, result)
    }

    @Test
    fun testFindExtremaWithFlatValues() {
        val input = doubleArrayOf(2.0, 2.0, 2.0, 2.0)
        val expected = listOf(
            0 to 0, // neither min nor max
            1 to 0, // neither min nor max
            2 to 0, // neither min nor max
            3 to 0  // neither min nor max
        )
        val result = parser.findExtremaInList(input)
        assertEquals(expected, result)
    }

    @Test
    fun testFindExtremaWithSingleElement() {
        val input = doubleArrayOf(5.0)
        val expected = emptyList<Pair<Int, Int>>() // No extrema in a single element
        val result = parser.findExtremaInList(input)
        assertEquals(expected, result)
    }

    @Test
    fun testFindExtremaWithTwoElements() {
        val input = doubleArrayOf(5.0, 2.0)
        val expected = listOf(
            0 to 1, // first is a max
            1 to -1 // second is a min
        )
        val result = parser.findExtremaInList(input)
        assertEquals(expected, result)
    }

    @Test
    fun testBitCongruenceIdenticalBytes() {
        assertEquals(1.0, parser.bitCongruence("AA".fromHex()[0], "AA".fromHex()[0]))
    }

    @Test
    fun testBitCongruenceCompletelyDifferentBytes() {
        assertEquals(0.0, parser.bitCongruence("00".fromHex()[0], "FF".fromHex()[0]))
    }

    @Test
    fun testBitCongruenceHalfMatchingBits() {
        assertEquals(0.5, parser.bitCongruence("AF".fromHex()[0], "5F".fromHex()[0]))
    }

    @Test
    fun testBitCongruenceThreeQuarterMatchingBits() {
        assertEquals(0.75, parser.bitCongruence("BF".fromHex()[0], "7F".fromHex()[0]))
    }

    @Test
    fun testComputeDeltaBCWithValidMessage() {
        val message = "FE478182".fromHex()
        val expected = doubleArrayOf(0.125, 0.25)
        val result = parser.computeDeltaBC(message)
        assertEquals(expected.toList(), result.toList())
    }

    @Test
    fun testComputeDeltaBCWithShortMessage() {
        val message = "AA".fromHex()
        val expected = doubleArrayOf()
        val result = parser.computeDeltaBC(message)
        assertEquals(expected.toList(), result.toList())
    }

    @Test
    fun testComputeDeltaBCWithTwoBytes() {
        val message = "AA5F".fromHex()
        val expected = doubleArrayOf()
        val result = parser.computeDeltaBC(message)
        assertEquals(expected.toList(), result.toList())
    }

    @Test
    fun testFindRisingDeltas() {
        val extrema = listOf(
            0 to -1, 1 to 1, 2 to -1, 4 to 1, 5 to -1, 7 to 1
        )
        val expected = listOf(0 to 1, 2 to 4, 5 to 7)
        val result = parser.findRisingDeltas(extrema)
        assertEquals(expected, result)
    }

    @Test
    fun testFindRisingDeltasNoPairs() {
        val extrema = listOf(
            0 to -1,  // local minimum
            2 to -1,  // local minimum
            4 to -1   // local minimum
        )
        val expected = emptyList<Pair<Int, Int>>()
        val result = parser.findRisingDeltas(extrema)
        assertEquals(expected, result)
    }

    @Test
    fun testFindRisingDeltasOnlyMaxima() {
        val extrema = listOf(
            1 to 1,  // local maximum
            3 to 1,  // local maximum
            5 to 1   // local maximum
        )
        val expected = emptyList<Pair<Int, Int>>()
        val result = parser.findRisingDeltas(extrema)
        assertEquals(expected, result)
    }

    @Test
    fun testFindInflectionPoints() {
        val risingDeltas = listOf(0 to 3, 2 to 5)
        val smoothedDeltaBC = doubleArrayOf(0.1, 0.3, 0.5, 0.2, 0.8, 0.1)
        val expected = listOf(4, 6)
        val result = parser.findInflectionPoints(risingDeltas, smoothedDeltaBC)
        assertEquals(expected, result)
    }

    @Test
    fun testFindInflectionPointsEmptyInput() {
        val risingDeltas = emptyList<Pair<Int, Int>>()
        val smoothedDeltaBC = doubleArrayOf(0.1, 0.3, 0.5)
        val expected = emptyList<Int>()
        val result = parser.findInflectionPoints(risingDeltas, smoothedDeltaBC)
        assertEquals(expected, result)
    }

    @Test
    fun testIsPrintableChar() {
        assertTrue(parser.isPrintableChar(0x41)) // 'A'
        assertTrue(parser.isPrintableChar(0x7E)) // '~'
        assertTrue(parser.isPrintableChar(0x20)) // ' '
        assertTrue(parser.isPrintableChar(0x09)) // '\t'
        assertTrue(parser.isPrintableChar(0x0A)) // '\n'
        assertTrue(parser.isPrintableChar(0x0D)) // '\r'
        assertFalse(parser.isPrintableChar(0x19)) // Non-printable
    }

    @Test
    fun testFieldIsTextSegment() {
        val message = "48656C6C6F20576F726C6421".fromHex() // "Hello World!"
        assertTrue(parser.fieldIsTextSegment(0, message.size, message)) // Whole message is text
        assertTrue(parser.fieldIsTextSegment(0, 5, message)) // "Hello"
        assertFalse(parser.fieldIsTextSegment(0, 6, "48656C6C6F00".fromHex())) // "Hello" + non-printable
    }

    @Test
    fun testMergeCharSequences() {
        val message = "48656C6C6F00576F726C6421".fromHex() // "Hello\0World!"
        val boundaries = mutableListOf(5, 6) // Segments: ["Hello"], ["\0"], ["World!"]

        val expectedValue1 = mutableListOf<Pair<Int, NemesysField>>()
        expectedValue1.add(Pair(0, NemesysField.UNKNOWN))
        expectedValue1.add(Pair(5, NemesysField.UNKNOWN))
        expectedValue1.add(Pair(6, NemesysField.UNKNOWN))
        val merged = parser.mergeCharSequences(boundaries, message)
        assertEquals(expectedValue1, merged) // No merge since '\0' breaks text sequence

        val message2 = "48656C6C6F20576F726C6421".fromHex() // "Hello World!"
        val boundaries2 = mutableListOf(6) // ["Hello "], ["World!"]

        val expectedValue2 = mutableListOf<Pair<Int, NemesysField>>()
        expectedValue2.add(Pair(0, NemesysField.STRING))
        val merged2 = parser.mergeCharSequences(boundaries2, message2)
        assertEquals(expectedValue2, merged2) // Full merge, as both are text segments
    }
}
