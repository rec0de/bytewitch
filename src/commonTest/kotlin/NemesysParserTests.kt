import bitmage.fromHex
import decoders.Nemesys.*
import kotlin.collections.listOf
import kotlin.test.*


class NemesysParserTests {

    private val parser = NemesysParser()

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
        assertTrue(parser.isPrintableChar(0x41), "0x41 should be printable") // 'A'
        assertTrue(parser.isPrintableChar(0x7E), "0x7E should be printable") // '~'
        assertTrue(parser.isPrintableChar(0x20), "0x20 should be printable") // ' '
        assertTrue(parser.isPrintableChar(0x0B), "0x0B should be printable") // '\t'
        assertTrue(parser.isPrintableChar(0x0A), "0x0A should be printable") // '\n'
        assertTrue(parser.isPrintableChar(0x0D), "0x0D should be printable") // '\r'
        assertFalse(parser.isPrintableChar(0x19), "0x19 should not be printable") // Non-printable
    }

    @Test
    fun testFieldIsTextSegment() {
        val message = "48656C6C6F20576F726C6421".fromHex() // "Hello World!"
        assertTrue(parser.fieldIsTextSegment(0, message.size, message), "Whole message should be text") // Whole message is text
        assertTrue(parser.fieldIsTextSegment(0, 5, message), "Byte 0 to 5 should be text") // "Hello"
        assertFalse(parser.fieldIsTextSegment(0, 6, "48656C6C6FEE".fromHex()), "not everything is printable") // "Hello" + non-printable
    }

    @Test
    fun testMergeCharSequences() {
        val message = "48656C6C6F00576F726C6421".fromHex() // "Hello\0World!"
        val boundaries = mutableListOf(5, 6) // Segments: ["Hello"], ["\0"], ["World!"]

        val expectedValue1 = mutableListOf<NemesysSegment>()
        expectedValue1.add(NemesysSegment(0, NemesysField.STRING))
        expectedValue1.add(NemesysSegment(5, NemesysField.UNKNOWN))
        expectedValue1.add(NemesysSegment(6, NemesysField.STRING))
        val merged = parser.mergeCharSequences(boundaries, message)
        assertEquals(expectedValue1, merged) // No merge since '\0' breaks text sequence

        val message2 = "48656C6C6F20576F726C6421".fromHex() // "Hello World!"
        val boundaries2 = mutableListOf(6) // ["Hello "], ["World!"]

        val expectedValue2 = mutableListOf<NemesysSegment>()
        expectedValue2.add(NemesysSegment(0, NemesysField.STRING))
        val merged2 = parser.mergeCharSequences(boundaries2, message2)
        assertEquals(expectedValue2, merged2) // Full merge, as both are text segments
    }

    @Test
    fun testSlideCharWindowExpandSingleStringField() {
        val bytes = "2A48656C6C6F2A".fromHex() // "*Hello*"
        val segments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(1, NemesysField.STRING)
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            NemesysSegment(0, NemesysField.STRING) // expand left and right to include "*"
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowExpandSingleStringFieldTwice() {
        val bytes = "2A2A48656C6C6F2A".fromHex() // "**Hello*"
        val segments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(1, NemesysField.UNKNOWN),
            NemesysSegment(2, NemesysField.STRING)
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            NemesysSegment(0, NemesysField.STRING) // expand left and right to include "*"
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowNoExpansionDueToNonPrintableLeft() {
        val bytes = "0048656C6C6F".fromHex() // "\0Hello"
        val segments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(1, NemesysField.STRING)
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(1, NemesysField.STRING) // no expansion due to \0 before
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowMixedFieldTypes() {
        val bytes = "48656C6C6F002A".fromHex() // "Hello\0*"
        val segments = listOf(
            NemesysSegment(0, NemesysField.STRING),  // "Hello"
            NemesysSegment(5, NemesysField.UNKNOWN)  // \0
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            NemesysSegment(0, NemesysField.STRING),  // no expansion, already correct
            NemesysSegment(5, NemesysField.UNKNOWN)  // stays the same
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowExpandRightOnly() {
        val bytes = "0048656C6C6F".fromHex() // "\0Hello"
        val segments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN), // \0
            NemesysSegment(1, NemesysField.STRING)   // Hello
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(1, NemesysField.STRING) // no expansion left, no space right
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowMultipleStringSegments() {
        val bytes = "4869FF6D7954657874".fromHex() // "Hi" + 0xFF + "myText"
        val segments = listOf(
            NemesysSegment(0, NemesysField.STRING), // "Hi"
            NemesysSegment(3, NemesysField.STRING)  // "myText"
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            NemesysSegment(0, NemesysField.STRING),
            NemesysSegment(3, NemesysField.STRING)
        )

        assertEquals(expected, result)
    }

    @Test
    fun testNullByteTransitions() {
        // check if 00 is added to the previous string
        var bytes = "556c6d00037a".fromHex()

        var segments = mutableListOf<NemesysSegment>()
        segments.add(NemesysSegment(0, NemesysField.STRING))
        segments.add(NemesysSegment(3, NemesysField.UNKNOWN))

        var expectedResult = mutableListOf<NemesysSegment>()
        expectedResult.add(NemesysSegment(0, NemesysField.STRING))
        expectedResult.add(NemesysSegment(4, NemesysField.UNKNOWN))
        var actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)


        bytes = "7724636c617373007f1115".fromHex()
        segments = mutableListOf<NemesysSegment>()
        segments.add(NemesysSegment(0, NemesysField.STRING))
        segments.add(NemesysSegment(8, NemesysField.UNKNOWN))

        expectedResult = mutableListOf<NemesysSegment>()
        expectedResult.add(NemesysSegment(0, NemesysField.STRING))
        expectedResult.add(NemesysSegment(8, NemesysField.UNKNOWN))
        actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)


        // check if 00 is added to the next field
        bytes = "015bf9000003".fromHex()

        segments = mutableListOf<NemesysSegment>()
        segments.add(NemesysSegment(0, NemesysField.UNKNOWN))
        segments.add(NemesysSegment(4, NemesysField.UNKNOWN))

        expectedResult = mutableListOf<NemesysSegment>()
        expectedResult.add(NemesysSegment(0, NemesysField.UNKNOWN))
        expectedResult.add(NemesysSegment(3, NemesysField.UNKNOWN))
        actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)


        // check that 00 won't be added to the string because x0 row is too long
        bytes = "7d6e6f746966794576656e743a00000000".fromHex()

        segments = mutableListOf<NemesysSegment>()
        segments.add(NemesysSegment(0, NemesysField.STRING))
        segments.add(NemesysSegment(13, NemesysField.UNKNOWN))

        expectedResult = mutableListOf<NemesysSegment>()
        expectedResult.add(NemesysSegment(0, NemesysField.STRING))
        expectedResult.add(NemesysSegment(13, NemesysField.UNKNOWN))
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
        val segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN), NemesysSegment(2, NemesysField.UNKNOWN))

        val result = parser.entropyMerge(segments, bytes)
        assertEquals(segments, result) // no merging, because of low entropy
    }

    @Test
    fun testMergeWithHighEntropyAndHighXorEntropy() {
        val bytes = byteArrayOf(
            0x8A.toByte(), 0x4F.toByte(), 0x2C.toByte(), 0x10.toByte(), // Segment 1
            0x4B.toByte(), 0xD1.toByte(), 0x33.toByte(), 0x27.toByte()  // Segment 2
        )
        val segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN), NemesysSegment(4, NemesysField.UNKNOWN))
        val expected = listOf(NemesysSegment(0, NemesysField.UNKNOWN)) // merge together
        val result = parser.entropyMerge(segments, bytes)
        assertEquals(expected, result)
    }

    @Test
    fun testNoMergeDueToLowXorEntropy() {
        val bytes = byteArrayOf(
            0x8A.toByte(), 0x4F.toByte(), 0x2C.toByte(), 0x10.toByte(), // Segment 1
            0x8A.toByte(), 0x4F.toByte(), 0x2D.toByte(), 0x10.toByte() // Segment 2
        )
        val segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN), NemesysSegment(4, NemesysField.UNKNOWN))
        // no merging because first two bytes are too similar
        val expected = listOf(NemesysSegment(0, NemesysField.UNKNOWN), NemesysSegment(4, NemesysField.UNKNOWN))
        val result = parser.entropyMerge(segments, bytes)
        assertEquals(expected, result)
    }

    @Test
    fun testNoMergeDueToDifferentFieldTypes() {
        val bytes = byteArrayOf(0x8A.toByte(), 0x4F.toByte(), 0x2C.toByte(), 0x10.toByte(), 0x4B.toByte(), 0xD1.toByte(), 0x33.toByte(), 0x27.toByte())
        val segments = listOf(NemesysSegment(0, NemesysField.STRING), NemesysSegment(4, NemesysField.UNKNOWN))
        val expected = segments // no merging because of different field types
        val result = parser.entropyMerge(segments, bytes)
        assertEquals(expected, result)
    }

    @Test
    fun testHandlePrintablePayload_validString() {
        val bytes = "0748656C6C6F2121".fromHex() // 07 'Hello!!'
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<NemesysSegment>()

        val newIndex = parser.handlePrintablePayload(
            bytes = bytes,
            taken = taken,
            result = result,
            offset = 0,
            lengthFieldSize = 1,
            payloadLength = 7,
            bigEndian = false
        )

        assertEquals(8, newIndex)
        assertEquals(listOf(
            NemesysSegment(0, NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN),
            NemesysSegment(1, NemesysField.STRING_PAYLOAD)
        ), result)
        assertTrue(taken.slice(0..7).all { it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_valid1Byte() {
        val bytes = "0648656C6C6F21".fromHex() // 06 + "Hello!"
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<NemesysSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 1, false)

        assertEquals(7, nextIndex) // i + 1 + 6
        assertEquals(
            listOf(
                NemesysSegment(0, NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN),
                NemesysSegment(1, NemesysField.STRING_PAYLOAD)
            ),
            result
        )
        assertTrue(taken.slice(0..6).all { it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_valid2ByteLE() {
        val bytes = "060048656C6C6F21".fromHex() // 06 00 + "Hello!"
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<NemesysSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 2, false)

        assertEquals(8, nextIndex) // i + 2 + 6
        assertEquals(
            listOf(
                NemesysSegment(0, NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN),
                NemesysSegment(2, NemesysField.STRING_PAYLOAD)
            ),
            result
        )
        assertTrue(taken.slice(0..7).all { it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_rejectsShortPayload() {
        val bytes = "024142".fromHex() // 02 + "AB"
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<NemesysSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 1, false)

        assertNull(nextIndex)
        assertTrue(result.isEmpty())
        assertTrue(taken.all { !it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_rejectsContinuationText() {
        val bytes = "0548656C6C6F576F".fromHex() // 05 + "HelloWo"
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<NemesysSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 1, false)

        assertNull(nextIndex)
        assertTrue(result.isEmpty())
        assertTrue(taken.all { !it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_rejectsOverlappingTaken() {
        val bytes = "0548656C6C6F21".fromHex()
        val taken = BooleanArray(bytes.size) { false }
        taken[3] = true // simulate overlap
        val result = mutableListOf<NemesysSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 1, false)

        assertNull(nextIndex)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testHandlePrintablePayload_rejectsContinuation() {
        val bytes = "0548656C6C6F576F72".fromHex() // 05 'Hello' + 'Wor...'
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<NemesysSegment>()

        val newIndex = parser.handlePrintablePayload(
            bytes = bytes,
            taken = taken,
            result = result,
            offset = 0,
            lengthFieldSize = 1,
            payloadLength = 5,
            bigEndian = false
        )

        assertNull(newIndex)
        assertTrue(result.isEmpty())
        assertTrue(taken.all { !it })
    }

    @Test
    fun testDetectLengthPrefixedFields_mixed1and2Byte() {
        // 01 48 ('H') → too short
        // 05 48656C6C6F → valid 1-byte (Hello)
        // 06 00 576F726C6421 → valid 2-byte LE (06 00 → 'World!')
        val bytes = "01480548656C6C6F0600576F726C6421".fromHex()
        val taken = BooleanArray(bytes.size) { false }

        val result = parser.detectLengthPrefixedFields(bytes, taken)

        val expected = listOf(
            NemesysSegment(2, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),
            NemesysSegment(3, NemesysField.STRING_PAYLOAD),
            NemesysSegment(8, NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN),
            NemesysSegment(10, NemesysField.STRING_PAYLOAD)
        )

        assertEquals(expected, result)
    }

    @Test
    fun testCountSegmentValues_countsCorrectly() {
        val msg1 = NemesysParsedMessage(
            segments = listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(5, NemesysField.UNKNOWN)
            ),
            bytes = "48656C6C6F123456".fromHex(), // "Hello" + 0x12 0x34 0x56
            msgIndex = 0
        )

        val msg2 = NemesysParsedMessage(
            segments = listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(5, NemesysField.UNKNOWN)
            ),
            bytes = "48656C6C6F999999".fromHex(), // "Hello" + 0x99 0x99 0x99
            msgIndex = 1
        )

        val result = parser.countSegmentValues(listOf(msg1, msg2))

        val hello = "48656C6C6F".fromHex()
        val tail1 = "123456".fromHex()
        val tail2 = "999999".fromHex()

        val helloEntry = result.entries.find { it.key.contentEquals(hello) }
        val tail1Entry = result.entries.find { it.key.contentEquals(tail1) }
        val tail2Entry = result.entries.find { it.key.contentEquals(tail2) }

        assertEquals(2, helloEntry?.value)
        assertEquals(1, tail1Entry?.value)
        assertEquals(1, tail2Entry?.value)
    }


    @Test
    fun testRefineSegmentsAcrossMessages_splitsOnFrequentValue() {
        val msg1 = NemesysParsedMessage(
            segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN)),
            bytes = "ABCD1234EF".fromHex(), // Contains "1234"
            msgIndex = 0
        )

        val msg2 = NemesysParsedMessage(
            segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN)),
            bytes = "1234".fromHex(), // Just the "frequent" sequence
            msgIndex = 1
        )

        val refined = parser.cropDistinct(listOf(msg1, msg2))

        val refinedMsg1 = refined.first { it.msgIndex == 0 }

        val segmentOffsets = refinedMsg1.segments.map { it.offset }
        assertEquals(listOf(0, 2, 4), segmentOffsets) // AB CD | 12 34 | EF
    }

    @Test
    fun testIndexOfSubsequence_findsCorrectIndex() {
        val main = "0011223344556677".fromHex()
        val sub = "334455".fromHex()

        val idx = parser.indexOfSubsequence(main, sub)
        assertEquals(3, idx)
    }

    @Test
    fun testIndexOfSubsequence_notFound() {
        val main = "001122334455".fromHex()
        val sub = "778899".fromHex()

        val idx = parser.indexOfSubsequence(main, sub)
        assertEquals(-1, idx)
    }

    @Test
    fun testTryParseLength_1Byte() {
        val bytes = byteArrayOf(0x0A)
        val length = NemesysUtil.tryParseLength(bytes, 0, 1, true)
        assertEquals(10, length)
    }

    @Test
    fun testTryParseLength_2Byte_BigEndian() {
        val bytes = byteArrayOf(0x01, 0x02) // 0x0102 = 258
        val length = NemesysUtil.tryParseLength(bytes, 0, 2, true)
        assertEquals(258, length)
    }

    @Test
    fun testTryParseLength_2Byte_LittleEndian() {
        val bytes = byteArrayOf(0x02, 0x01) // 0x0102 = 258 (little endian)
        val length = NemesysUtil.tryParseLength(bytes, 0, 2, false)
        assertEquals(258, length)
    }

    @Test
    fun testTryParseLength_4Byte_BigEndian() {
        val bytes = byteArrayOf(0x00, 0x00, 0x01, 0x00) // 256
        val length = NemesysUtil.tryParseLength(bytes, 0, 4, true)
        assertEquals(256, length)
    }

    @Test
    fun testTryParseLength_4Byte_LittleEndian() {
        val bytes = byteArrayOf(0x00, 0x01, 0x00, 0x00) // 256
        val length = NemesysUtil.tryParseLength(bytes, 0, 4, false)
        assertEquals(256, length)
    }

    @Test
    fun testTryParseLength_invalidLengthSize_returnsNull() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        val length = NemesysUtil.tryParseLength(bytes, 0, 3, true) // unsupported size
        assertNull(length)
    }

    @Test
    fun testFindSegmentForOffset_findsCorrectSegment() {
        val segments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(5, NemesysField.STRING),
            NemesysSegment(10, NemesysField.STRING_PAYLOAD)
        )

        assertEquals(NemesysField.UNKNOWN, parser.findSegmentForOffset(segments, 0)?.fieldType)
        assertEquals(NemesysField.UNKNOWN, parser.findSegmentForOffset(segments, 4)?.fieldType)
        assertEquals(NemesysField.STRING, parser.findSegmentForOffset(segments, 5)?.fieldType)
        assertEquals(NemesysField.STRING, parser.findSegmentForOffset(segments, 9)?.fieldType)
        assertEquals(NemesysField.STRING_PAYLOAD, parser.findSegmentForOffset(segments, 10)?.fieldType)
        assertEquals(NemesysField.STRING_PAYLOAD, parser.findSegmentForOffset(segments, 999999)?.fieldType)
    }

    @Test
    fun testFindSegmentForOffset_returnsNullIfBeforeFirstSegment() {
        val segments = listOf(
            NemesysSegment(5, NemesysField.STRING),
            NemesysSegment(10, NemesysField.STRING_PAYLOAD)
        )

        assertNull(parser.findSegmentForOffset(segments, 0))
        assertNull(parser.findSegmentForOffset(segments, 4))
    }

    @Test
    fun testFindSegmentForOffset_returnsNullIfEmptyList() {
        val segments = emptyList<NemesysSegment>()
        assertNull(parser.findSegmentForOffset(segments, 0))
    }

    @Test
    fun testFindSegmentForOffset_exactlyOnSegmentStart() {
        val segments = listOf(
            NemesysSegment(3, NemesysField.UNKNOWN),
            NemesysSegment(7, NemesysField.STRING)
        )

        assertEquals(NemesysField.UNKNOWN, parser.findSegmentForOffset(segments, 3)?.fieldType)
        assertEquals(NemesysField.STRING, parser.findSegmentForOffset(segments, 7)?.fieldType)
    }

    @Test
    fun testFindSegmentForOffset_exactlyOnSegmentEnd_returnsPrevious() {
        val segments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(5, NemesysField.STRING)
        )
        // Offset 5 is start of next segment, so should return segment[1]
        assertEquals(NemesysField.STRING, parser.findSegmentForOffset(segments, 5)?.fieldType)
        // Offset 4 is still within segment[0]
        assertEquals(NemesysField.UNKNOWN, parser.findSegmentForOffset(segments, 4)?.fieldType)
    }

    @Test
    fun testDetectLengthFieldInMessage_valid1ByteLE() {
        // 03 41 42 43 → length=3, payload="ABC", UNKNOWN segment
        val msg = NemesysParsedMessage(
            segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN)),
            bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertEquals(0 to 3, result)
    }

    @Test
    fun testDetectLengthFieldInMessage_valid2ByteBE() {
        // 00 03 41 42 43 → BE: length = 3
        val msg = NemesysParsedMessage(
            segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN)),
            bytes = byteArrayOf(0x00, 0x03, 0x41, 0x42, 0x43),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 2, bigEndian = true)
        assertEquals(0 to 3, result)
    }

    @Test
    fun testDetectLengthFieldInMessage_rejectsWrongSegmentType() {
        // valid length, but segment is STRING not UNKNOWN
        val msg = NemesysParsedMessage(
            segments = listOf(NemesysSegment(0, NemesysField.STRING)),
            bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertNull(result)
    }

    @Test
    fun testDetectLengthFieldInMessage_rejectsIfPayloadEndNotEqualToMessageSize() {
        // length = 3, but message size is 5 → should be rejected
        val msg = NemesysParsedMessage(
            segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN)),
            bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43, 0x44),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertNull(result)
    }

    @Test
    fun testDetectLengthFieldInMessage_returnsNullIfNoMatch() {
        // no valid length field
        val msg = NemesysParsedMessage(
            segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN)),
            bytes = byteArrayOf(0x7F, 0x00), // length = 127, too long
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertNull(result)
    }

    @Test
    fun testDetectLengthFieldInMessage_findsCorrectOffsetAmongMultiple() {
        // 02 00 03 41 42 43 → LE: offset 2 → length = 3, payload="ABC"
        val msg = NemesysParsedMessage(
            segments = listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(2, NemesysField.UNKNOWN)
            ),
            bytes = byteArrayOf(0x02, 0x00, 0x03, 0x41, 0x42, 0x43),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertEquals(2 to 3, result)
    }

    @Test
    fun testDetectMessageLengthField_setsLengthSegmentsWhenValidGlobally() {
        val messages = listOf(
            NemesysParsedMessage(
                segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN)),
                bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43), // length = 3, payload = "ABC"
                msgIndex = 0
            ),
            NemesysParsedMessage(
                segments = listOf(NemesysSegment(0, NemesysField.UNKNOWN)),
                bytes = byteArrayOf(0x02, 0x41, 0x42), // length = 2, payload = "AB"
                msgIndex = 1
            )
        )

        val actual = parser.detectMessageLengthField(messages)

        assertEquals(2, actual.size)

        actual.forEachIndexed { index, msg ->
            val segments = msg.segments.sortedBy { it.offset }
            val lengthField = segments.find { it.fieldType.name.startsWith("PAYLOAD_LENGTH") }
            val unknownAfterLength = segments.find { it.offset > (lengthField?.offset ?: -1) }

            assertNotNull(lengthField, "Length field missing in msg[$index]")
            assertEquals(NemesysField.UNKNOWN, unknownAfterLength?.fieldType)
        }
    }

    @Test
    fun testDetectMessageLengthField_returnsOriginalWhenNoValidConfig() {
        val messages = listOf(
            NemesysParsedMessage(
                segments = listOf(NemesysSegment(0, NemesysField.STRING)),
                bytes = byteArrayOf(0xFF.toByte(), 0x00), // length = 255 (invalid for short payload)
                msgIndex = 0
            )
        )

        val result = parser.detectMessageLengthField(messages)

        assertEquals(messages, result)
    }

    @Test
    fun testDetectMessageLengthField_preservesOriginalSegments() {
        val messages = listOf(
            NemesysParsedMessage(
                segments = listOf(
                    NemesysSegment(0, NemesysField.UNKNOWN),
                    NemesysSegment(2, NemesysField.STRING)
                ),
                bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43), // length = 3, payload = "ABC"
                msgIndex = 0
            )
        )

        val result = parser.detectMessageLengthField(messages)

        val newMsg = result.first()
        val segmentOffsets = newMsg.segments.map { it.offset }

        // don't delete other fields
        assertTrue(segmentOffsets.contains(2))
        // contain length field
        assertTrue(segmentOffsets.contains(0))
        // add payload field
        assertTrue(segmentOffsets.contains(1))
    }

    @Test
    fun testBoundaries_rule1_detectsLocalEntropyMaximum() {
        val bytes1 = "1111223344".fromHex()
        val bytes2 = "1111333344".fromHex()
        val bytes3 = "1111443344".fromHex()

        val messages = listOf(bytes1, bytes2, bytes3).mapIndexed { index, bytes ->
            NemesysParsedMessage(
                segments = emptyList(),
                bytes = bytes,
                msgIndex = index
            )
        }

        val entropy = parser.calcBytewiseEntropy(messages)
        val gr = parser.calcGainRatio(messages, entropy)

        val boundaries = parser.getBoundariesUsingEntropy(messages, entropy, gr, threshold = 0.01)

        // position 2 has higher entropy than neighboring bytes. So set boundary
        assertEquals(true, boundaries.contains(2), "Expected boundary at entropy peak position 2")
    }


    @Test
    fun testBoundaries_rule2_detectsBoundaryAfterStaticZeros() {
        val bytes1 = "000000A1BB".fromHex()
        val bytes2 = "000000A2CC".fromHex()
        val bytes3 = "000000A3DD".fromHex()

        val messages = listOf(bytes1, bytes2, bytes3).mapIndexed { index, bytes ->
            NemesysParsedMessage(
                segments = emptyList(),
                bytes = bytes,
                msgIndex = index
            )
        }

        val entropy = parser.calcBytewiseEntropy(messages)
        val gr = parser.calcGainRatio(messages, entropy)

        val boundaries = parser.getBoundariesUsingEntropy(messages, entropy, gr, threshold = 0.01)

        // set boundary before static zeros
        assertEquals(true, boundaries.contains(0), "Expected field boundary at start of static block (offset 0)")
    }

    @Test
    fun testBoundaryDetectedAfterStaticBlock_rule2() {
        val messages = listOf(
            NemesysParsedMessage(emptyList(), "000000AA".fromHex(), 0),
            NemesysParsedMessage(emptyList(), "000000BB".fromHex(), 1),
            NemesysParsedMessage(emptyList(), "000000CC".fromHex(), 2)
        )

        val result = parser.findEntropyBoundaries(messages)

        // set boundary at pos 0 before the static block starts
        val segmentOffsets = result[0].segments.map { it.offset }
        assertEquals(true, segmentOffsets.contains(0), "Expected boundary at start of static block")
    }

    @Test
    fun testEntropy_zeroWhenAllValuesSame() {
        val msg1 = NemesysParsedMessage(emptyList(), "FF01A1".fromHex(), 0)
        val msg2 = NemesysParsedMessage(emptyList(), "FF02A2".fromHex(), 1)
        val msg3 = NemesysParsedMessage(emptyList(), "FF03A3".fromHex(), 2)

        val result = parser.calcBytewiseEntropy(listOf(msg1, msg2, msg3))

        // entropy at byte 0 should be 0.0 as it's FF for all messages
        assertEquals(0.0, result[0], 0.0001)
    }

    @Test
    fun testEntropy_highWhenUniformDistribution() {
        val msg1 = NemesysParsedMessage(emptyList(), "00".fromHex(), 0)
        val msg2 = NemesysParsedMessage(emptyList(), "FF".fromHex(), 1)

        val result = parser.calcBytewiseEntropy(listOf(msg1, msg2))

        // ln(2) = 0.6931
        assertEquals(kotlin.math.ln(2.0), result[0], 0.0001)
    }


    @Test
    fun testEntropy_lowButNotZeroWhenBiasedDistribution() {
        val msg1 = NemesysParsedMessage(emptyList(), "FF".fromHex(), 0)
        val msg2 = NemesysParsedMessage(emptyList(), "FF".fromHex(), 1)
        val msg3 = NemesysParsedMessage(emptyList(), "00".fromHex(), 2)

        val result = parser.calcBytewiseEntropy(listOf(msg1, msg2, msg3))

        // 2x FF, 1x 00 should be between 0 and ln(2)
        assertTrue(result[0] in 0.0..kotlin.math.ln(2.0))
        assertTrue(result[0] > 0.0)
    }

    @Test
    fun testEntropy_truncatesToShortestMessage() {
        val msg1 = NemesysParsedMessage(emptyList(), "FF00AA".fromHex(), 0)
        val msg2 = NemesysParsedMessage(emptyList(), "FF00".fromHex(), 1)

        val result = parser.calcBytewiseEntropy(listOf(msg1, msg2))

        // shortest messages has length 2, so the result size should be 2 as well
        assertEquals(2, result.size)
    }

    @Test
    fun testGainRatio_zeroWhenEntropyIsZero() {
        val messages = listOf(
            NemesysParsedMessage(emptyList(), "AA11".fromHex(), 0),
            NemesysParsedMessage(emptyList(), "AA22".fromHex(), 1),
            NemesysParsedMessage(emptyList(), "AA33".fromHex(), 2)
        )

        // entropy at position 0 is 0 as all bytes are "AA"
        val entropy = parser.calcBytewiseEntropy(messages)
        val gr = parser.calcGainRatio(messages, entropy)

        assertEquals(0.0, gr[0], 0.0001)
    }

    @Test
    fun testGainRatio_highWhenPairCorrelationIsStrong() {
        val messages = listOf(
            NemesysParsedMessage(emptyList(), "0011".fromHex(), 0),
            NemesysParsedMessage(emptyList(), "2233".fromHex(), 1),
            NemesysParsedMessage(emptyList(), "4455".fromHex(), 2)
        )

        val entropy = parser.calcBytewiseEntropy(messages)
        val gr = parser.calcGainRatio(messages, entropy)

        // Gain Ratio should be high as all pairs are linked to each other
        assertTrue(gr[0] > 0.5, "Expected high GR at position 0 due to strong pairing")
    }

    @Test
    fun testGainRatio_lowerWhenPairingIsWeak() {
        val messages = listOf(
            NemesysParsedMessage(emptyList(), "00AA".fromHex(), 0),
            NemesysParsedMessage(emptyList(), "00BB".fromHex(), 1),
            NemesysParsedMessage(emptyList(), "00CC".fromHex(), 2)
        )

        val entropy = parser.calcBytewiseEntropy(messages)
        val gr = parser.calcGainRatio(messages, entropy)

        // byte 0 is always "OO" byte byte 1 changes a lot. So, GR should be 0
        assertEquals(0.0, gr[0], 0.0001)
    }

    @Test
    fun testGainRatio_skipsLastByte() {
        val messages = listOf(
            NemesysParsedMessage(emptyList(), "1122".fromHex(), 0),
            NemesysParsedMessage(emptyList(), "334455".fromHex(), 1)
        )

        val entropy = parser.calcBytewiseEntropy(messages)
        val gr = parser.calcGainRatio(messages, entropy)

        // shortest messages is two bytes long, so gr shouldn't be longer
        assertEquals(2, gr.size)
        assertEquals(0.0, gr[1], 0.0001)
    }
}
