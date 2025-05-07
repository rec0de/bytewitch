import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import bitmage.fromHex
import decoders.NemesysField
import decoders.NemesysParser
import kotlin.collections.listOf


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

    @Test
    fun testNullByteTransitions() {
        // check if 00 is added to the previous string
        var bytes = "556c6d00037a".fromHex()

        var segments = mutableListOf<Pair<Int, NemesysField>>()
        segments.add(Pair(0, NemesysField.STRING))
        segments.add(Pair(3, NemesysField.UNKNOWN))

        var expectedResult = mutableListOf<Pair<Int, NemesysField>>()
        expectedResult.add(Pair(0, NemesysField.STRING))
        expectedResult.add(Pair(4, NemesysField.UNKNOWN))
        var actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)


        // check if 00 is added to the next field
        bytes = "015bf9000003".fromHex()

        segments = mutableListOf<Pair<Int, NemesysField>>()
        segments.add(Pair(0, NemesysField.UNKNOWN))
        segments.add(Pair(4, NemesysField.UNKNOWN))

        expectedResult = mutableListOf<Pair<Int, NemesysField>>()
        expectedResult.add(Pair(0, NemesysField.UNKNOWN))
        expectedResult.add(Pair(3, NemesysField.UNKNOWN))
        actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)


        // check that 00 won't be added to the string because x0 row is too long
        bytes = "7d6e6f746966794576656e743a00000000".fromHex()

        segments = mutableListOf<Pair<Int, NemesysField>>()
        segments.add(Pair(0, NemesysField.STRING))
        segments.add(Pair(13, NemesysField.UNKNOWN))

        expectedResult = mutableListOf<Pair<Int, NemesysField>>()
        expectedResult.add(Pair(0, NemesysField.STRING))
        expectedResult.add(Pair(13, NemesysField.UNKNOWN))
        actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun testEntropy_AllZeros() {
        val bytes = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val entropy = parser.calculateShannonEntropy(bytes)
        assertEquals(0.0, entropy, 0.0001)
    }

    @Test
    fun testEntropy_AllSameByte() {
        val bytes = byteArrayOf(0x41, 0x41, 0x41, 0x41) // 'A'
        val entropy = parser.calculateShannonEntropy(bytes)
        assertEquals(0.0, entropy, 0.0001)
    }

    @Test
    fun testEntropy_TwoValuesEqualDistribution() {
        val bytes = byteArrayOf(0x41, 0x42, 0x41, 0x42) // 'A', 'B', 'A', 'B'
        val entropy = parser.calculateShannonEntropy(bytes)
        val expected = 1.0
        assertEquals(expected, entropy, 0.0001)
    }

    @Test
    fun testEntropy_FourUniqueValues() {
        val bytes = byteArrayOf(0x41, 0x42, 0x43, 0x44) // 'A', 'B', 'C', 'D'
        val entropy = parser.calculateShannonEntropy(bytes)
        val expected = 2.0
        assertEquals(expected, entropy, 0.0001)
    }

    @Test
    fun testEntropy_RealisticRandomData() {
        val bytes = byteArrayOf(0xA7.toByte(), 0xD2.toByte(), 0x1B, 0x94.toByte())
        val entropy = parser.calculateShannonEntropy(bytes)
        assertTrue(entropy > 1.5 && entropy < 2.1)
    }

    @Test
    fun testEntropy_EmptyInput() {
        val bytes = byteArrayOf()
        val entropy = parser.calculateShannonEntropy(bytes)
        assertEquals(0.0, entropy, 0.0001)
    }

    @Test
    fun testNoMergeDueToLowEntropy() {
        val bytes = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x11.toByte(), 0x11.toByte())
        val segments = listOf(0 to NemesysField.UNKNOWN, 2 to NemesysField.UNKNOWN)

        val result = parser.entropyMerge(segments, bytes)
        assertEquals(segments, result) // no merging, because of low entropy
    }

    @Test
    fun testMergeWithHighEntropyAndHighXorEntropy() {
        val bytes = byteArrayOf(
            0x8A.toByte(), 0x4F.toByte(), 0x2C.toByte(), 0x10.toByte(), // Segment 1
            0x4B.toByte(), 0xD1.toByte(), 0x33.toByte(), 0x27.toByte()  // Segment 2
        )
        val segments = listOf(0 to NemesysField.UNKNOWN, 4 to NemesysField.UNKNOWN)
        val expected = listOf(0 to NemesysField.UNKNOWN) // merge together
        val result = parser.entropyMerge(segments, bytes)
        assertEquals(expected, result)
    }

    @Test
    fun testNoMergeDueToLowXorEntropy() {
        val bytes = byteArrayOf(
            0x8A.toByte(), 0x4F.toByte(), 0x2C.toByte(), 0x10.toByte(), // Segment 1
            0x8A.toByte(), 0x4F.toByte(), 0x2D.toByte(), 0x10.toByte() // Segment 2
        )
        val segments = listOf(0 to NemesysField.UNKNOWN, 4 to NemesysField.UNKNOWN)
        // no merging because first two bytes are too similar
        val expected = listOf(0 to NemesysField.UNKNOWN, 4 to NemesysField.UNKNOWN)
        val result = parser.entropyMerge(segments, bytes)
        assertEquals(expected, result)
    }

    @Test
    fun testNoMergeDueToDifferentFieldTypes() {
        val bytes = byteArrayOf(0x8A.toByte(), 0x4F.toByte(), 0x2C.toByte(), 0x10.toByte(), 0x4B.toByte(), 0xD1.toByte(), 0x33.toByte(), 0x27.toByte())
        val segments = listOf(0 to NemesysField.STRING, 4 to NemesysField.UNKNOWN)
        val expected = segments // no merging because of different field types
        val result = parser.entropyMerge(segments, bytes)
        assertEquals(expected, result)
    }
}
