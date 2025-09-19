import bitmage.fromHex
import decoders.SwiftSegFinder.*
import kotlin.collections.listOf
import kotlin.test.*


class SSFParserTests {

    private val parser = SSFParser()

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

        val expectedValue1 = mutableListOf<SSFSegment>()
        expectedValue1.add(SSFSegment(0, SSFField.STRING))
        expectedValue1.add(SSFSegment(5, SSFField.UNKNOWN))
        expectedValue1.add(SSFSegment(6, SSFField.STRING))
        val merged = parser.mergeCharSequences(boundaries, message)
        assertEquals(expectedValue1, merged) // No merge since '\0' breaks text sequence

        val message2 = "48656C6C6F20576F726C6421".fromHex() // "Hello World!"
        val boundaries2 = mutableListOf(6) // ["Hello "], ["World!"]

        val expectedValue2 = mutableListOf<SSFSegment>()
        expectedValue2.add(SSFSegment(0, SSFField.STRING))
        val merged2 = parser.mergeCharSequences(boundaries2, message2)
        assertEquals(expectedValue2, merged2) // Full merge, as both are text segments
    }

    @Test
    fun testSlideCharWindowExpandSingleStringField() {
        val bytes = "2A48656C6C6F2A".fromHex() // "*Hello*"
        val segments = listOf(
            SSFSegment(0, SSFField.UNKNOWN),
            SSFSegment(1, SSFField.STRING)
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            SSFSegment(0, SSFField.STRING) // expand left and right to include "*"
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowExpandSingleStringFieldTwice() {
        val bytes = "2A2A48656C6C6F2A".fromHex() // "**Hello*"
        val segments = listOf(
            SSFSegment(0, SSFField.UNKNOWN),
            SSFSegment(1, SSFField.UNKNOWN),
            SSFSegment(2, SSFField.STRING)
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            SSFSegment(0, SSFField.STRING) // expand left and right to include "*"
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowNoExpansionDueToNonPrintableLeft() {
        val bytes = "0048656C6C6F".fromHex() // "\0Hello"
        val segments = listOf(
            SSFSegment(0, SSFField.UNKNOWN),
            SSFSegment(1, SSFField.STRING)
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            SSFSegment(0, SSFField.UNKNOWN),
            SSFSegment(1, SSFField.STRING) // no expansion due to \0 before
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowMixedFieldTypes() {
        val bytes = "48656C6C6F002A".fromHex() // "Hello\0*"
        val segments = listOf(
            SSFSegment(0, SSFField.STRING),  // "Hello"
            SSFSegment(5, SSFField.UNKNOWN)  // \0
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            SSFSegment(0, SSFField.STRING),  // no expansion, already correct
            SSFSegment(5, SSFField.UNKNOWN)  // stays the same
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowExpandRightOnly() {
        val bytes = "0048656C6C6F".fromHex() // "\0Hello"
        val segments = listOf(
            SSFSegment(0, SSFField.UNKNOWN), // \0
            SSFSegment(1, SSFField.STRING)   // Hello
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            SSFSegment(0, SSFField.UNKNOWN),
            SSFSegment(1, SSFField.STRING) // no expansion left, no space right
        )

        assertEquals(expected, result)
    }

    @Test
    fun testSlideCharWindowMultipleStringSegments() {
        val bytes = "4869FF6D7954657874".fromHex() // "Hi" + 0xFF + "myText"
        val segments = listOf(
            SSFSegment(0, SSFField.STRING), // "Hi"
            SSFSegment(3, SSFField.STRING)  // "myText"
        )

        val result = parser.slideCharWindow(segments, bytes)
        val expected = listOf(
            SSFSegment(0, SSFField.STRING),
            SSFSegment(3, SSFField.STRING)
        )

        assertEquals(expected, result)
    }

    @Test
    fun testNullByteTransitions() {
        // check if 00 is added to the previous string
        var bytes = "556c6d00037a".fromHex()

        var segments = mutableListOf<SSFSegment>()
        segments.add(SSFSegment(0, SSFField.STRING))
        segments.add(SSFSegment(3, SSFField.UNKNOWN))

        var expectedResult = mutableListOf<SSFSegment>()
        expectedResult.add(SSFSegment(0, SSFField.STRING))
        expectedResult.add(SSFSegment(4, SSFField.UNKNOWN))
        var actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)


        bytes = "7724636c617373007f1115".fromHex()
        segments = mutableListOf<SSFSegment>()
        segments.add(SSFSegment(0, SSFField.STRING))
        segments.add(SSFSegment(8, SSFField.UNKNOWN))

        expectedResult = mutableListOf<SSFSegment>()
        expectedResult.add(SSFSegment(0, SSFField.STRING))
        expectedResult.add(SSFSegment(8, SSFField.UNKNOWN))
        actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)


        // check if 00 is added to the next field
        bytes = "015bf9000003".fromHex()

        segments = mutableListOf<SSFSegment>()
        segments.add(SSFSegment(0, SSFField.UNKNOWN))
        segments.add(SSFSegment(4, SSFField.UNKNOWN))

        expectedResult = mutableListOf<SSFSegment>()
        expectedResult.add(SSFSegment(0, SSFField.UNKNOWN))
        expectedResult.add(SSFSegment(3, SSFField.UNKNOWN))
        actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)


        // check that 00 won't be added to the string because x0 row is too long
        bytes = "7d6e6f746966794576656e743a00000000".fromHex()

        segments = mutableListOf<SSFSegment>()
        segments.add(SSFSegment(0, SSFField.STRING))
        segments.add(SSFSegment(13, SSFField.UNKNOWN))

        expectedResult = mutableListOf<SSFSegment>()
        expectedResult.add(SSFSegment(0, SSFField.STRING))
        expectedResult.add(SSFSegment(13, SSFField.UNKNOWN))
        actualResult = parser.nullByteTransitions(segments, bytes)

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun testEntropy_AllZeros() {
        val bytes = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val entropy = parser.entropyBytesNormalized(bytes)
        // val entropy = parser.calculateShannonEntropy(bytes)
        assertEquals(0.0, entropy, 0.0001)
    }

    @Test
    fun testEntropy_AllSameByte() {
        val bytes = byteArrayOf(0x41, 0x41, 0x41, 0x41) // 'A'
        val entropy = parser.entropyBytesNormalized(bytes)
        // val entropy = parser.calculateShannonEntropy(bytes)
        assertEquals(0.0, entropy, 0.0001)
    }

    @Test
    fun testEntropy_TwoValuesEqualDistribution() {
        val bytes = byteArrayOf(0x41, 0x42, 0x41, 0x42) // 'A', 'B', 'A', 'B'
        val entropy = parser.entropyBytesNormalized(bytes)
        val expected = 0.125
        /*val entropy = parser.calculateShannonEntropy(bytes)
        val expected = 1.0*/
        assertEquals(expected, entropy, 0.0001)
    }

    @Test
    fun testEntropy_FourUniqueValues() {
        val bytes = byteArrayOf(0x41, 0x42, 0x43, 0x44) // 'A', 'B', 'C', 'D'
        val entropy = parser.entropyBytesNormalized(bytes)
        val expected = 0.25
        /* val entropy = parser.calculateShannonEntropy(bytes)
        val expected = 2.0 */
        assertEquals(expected, entropy, 0.0001)
    }

    @Test
    fun testEntropy_EmptyInput() {
        val bytes = byteArrayOf()
        val entropy = parser.entropyBytesNormalized(bytes)
        assertEquals(0.0, entropy, 0.0001)
    }

    @Test
    fun testNoMergeDueToLowEntropy() {
        val bytes = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x11.toByte(), 0x11.toByte())
        val segments = listOf(SSFSegment(0, SSFField.UNKNOWN), SSFSegment(2, SSFField.UNKNOWN))

        val result = parser.entropyMerge(segments, bytes)
        assertEquals(segments, result) // no merging, because of low entropy
    }

    @Test
    fun testNoMergeDueToLowXorEntropy() {
        val bytes = byteArrayOf(
            0x8A.toByte(), 0x4F.toByte(), 0x2C.toByte(), 0x10.toByte(), // Segment 1
            0x8A.toByte(), 0x4F.toByte(), 0x2D.toByte(), 0x10.toByte() // Segment 2
        )
        val segments = listOf(SSFSegment(0, SSFField.UNKNOWN), SSFSegment(4, SSFField.UNKNOWN))
        // no merging because first two bytes are too similar
        val expected = listOf(SSFSegment(0, SSFField.UNKNOWN), SSFSegment(4, SSFField.UNKNOWN))
        val result = parser.entropyMerge(segments, bytes)
        assertEquals(expected, result)
    }

    @Test
    fun testNoMergeDueToDifferentFieldTypes() {
        val bytes = byteArrayOf(0x8A.toByte(), 0x4F.toByte(), 0x2C.toByte(), 0x10.toByte(), 0x4B.toByte(), 0xD1.toByte(), 0x33.toByte(), 0x27.toByte())
        val segments = listOf(SSFSegment(0, SSFField.STRING), SSFSegment(4, SSFField.UNKNOWN))
        val expected = segments // no merging because of different field types
        val result = parser.entropyMerge(segments, bytes)
        assertEquals(expected, result)
    }

    @Test
    fun testHandlePrintablePayload_validString() {
        val bytes = "0748656C6C6F2121".fromHex() // 07 'Hello!!'
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<SSFSegment>()

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
            SSFSegment(0, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN),
            SSFSegment(1, SSFField.STRING_PAYLOAD)
        ), result)
        assertTrue(taken.slice(0..7).all { it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_valid1Byte() {
        val bytes = "0648656C6C6F21".fromHex() // 06 + "Hello!"
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<SSFSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 1, false)

        assertEquals(7, nextIndex) // i + 1 + 6
        assertEquals(
            listOf(
                SSFSegment(0, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN),
                SSFSegment(1, SSFField.STRING_PAYLOAD)
            ),
            result
        )
        assertTrue(taken.slice(0..6).all { it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_valid2ByteLE() {
        val bytes = "060048656C6C6F21".fromHex() // 06 00 + "Hello!"
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<SSFSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 2, false)

        assertEquals(8, nextIndex) // i + 2 + 6
        assertEquals(
            listOf(
                SSFSegment(0, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN),
                SSFSegment(2, SSFField.STRING_PAYLOAD)
            ),
            result
        )
        assertTrue(taken.slice(0..7).all { it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_rejectsShortPayload() {
        val bytes = "024142".fromHex() // 02 + "AB"
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<SSFSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 1, false)

        assertNull(nextIndex)
        assertTrue(result.isEmpty())
        assertTrue(taken.all { !it })
    }

    @Test
    fun testCheckLengthPrefixedSegment_rejectsContinuationText() {
        val bytes = "0548656C6C6F576F".fromHex() // 05 + "HelloWo"
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<SSFSegment>()

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
        val result = mutableListOf<SSFSegment>()

        val nextIndex = parser.checkLengthPrefixedSegment(bytes, taken, result, 0, 1, false)

        assertNull(nextIndex)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testHandlePrintablePayload_rejectsContinuation() {
        val bytes = "0548656C6C6F576F72".fromHex() // 05 'Hello' + 'Wor...'
        val taken = BooleanArray(bytes.size) { false }
        val result = mutableListOf<SSFSegment>()

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
            SSFSegment(2, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN),
            SSFSegment(3, SSFField.STRING_PAYLOAD),
            SSFSegment(8, SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN),
            SSFSegment(10, SSFField.STRING_PAYLOAD)
        )

        assertEquals(expected, result)
    }

    @Test
    fun testCountSegmentValues_countsCorrectly() {
        val msg1 = SSFParsedMessage(
            segments = listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(5, SSFField.UNKNOWN)
            ),
            bytes = "48656C6C6F123456".fromHex(), // "Hello" + 0x12 0x34 0x56
            msgIndex = 0
        )

        val msg2 = SSFParsedMessage(
            segments = listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(5, SSFField.UNKNOWN)
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
        val msg1 = SSFParsedMessage(
            segments = listOf(SSFSegment(0, SSFField.UNKNOWN)),
            bytes = "ABCD1234EF".fromHex(), // Contains "1234"
            msgIndex = 0
        )

        val msg2 = SSFParsedMessage(
            segments = listOf(SSFSegment(0, SSFField.UNKNOWN)),
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
        val length = SSFUtil.tryParseLength(bytes, 0, 1, true)
        assertEquals(10, length)
    }

    @Test
    fun testTryParseLength_2Byte_BigEndian() {
        val bytes = byteArrayOf(0x01, 0x02) // 0x0102 = 258
        val length = SSFUtil.tryParseLength(bytes, 0, 2, true)
        assertEquals(258, length)
    }

    @Test
    fun testTryParseLength_2Byte_LittleEndian() {
        val bytes = byteArrayOf(0x02, 0x01) // 0x0102 = 258 (little endian)
        val length = SSFUtil.tryParseLength(bytes, 0, 2, false)
        assertEquals(258, length)
    }

    @Test
    fun testTryParseLength_4Byte_BigEndian() {
        val bytes = byteArrayOf(0x00, 0x00, 0x01, 0x00) // 256
        val length = SSFUtil.tryParseLength(bytes, 0, 4, true)
        assertEquals(256, length)
    }

    @Test
    fun testTryParseLength_4Byte_LittleEndian() {
        val bytes = byteArrayOf(0x00, 0x01, 0x00, 0x00) // 256
        val length = SSFUtil.tryParseLength(bytes, 0, 4, false)
        assertEquals(256, length)
    }

    @Test
    fun testTryParseLength_invalidLengthSize_returnsNull() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        val length = SSFUtil.tryParseLength(bytes, 0, 3, true) // unsupported size
        assertNull(length)
    }

    @Test
    fun testFindSegmentForOffset_findsCorrectSegment() {
        val segments = listOf(
            SSFSegment(0, SSFField.UNKNOWN),
            SSFSegment(5, SSFField.STRING),
            SSFSegment(10, SSFField.STRING_PAYLOAD)
        )

        assertEquals(SSFField.UNKNOWN, parser.findSegmentForOffset(segments, 0)?.fieldType)
        assertEquals(SSFField.UNKNOWN, parser.findSegmentForOffset(segments, 4)?.fieldType)
        assertEquals(SSFField.STRING, parser.findSegmentForOffset(segments, 5)?.fieldType)
        assertEquals(SSFField.STRING, parser.findSegmentForOffset(segments, 9)?.fieldType)
        assertEquals(SSFField.STRING_PAYLOAD, parser.findSegmentForOffset(segments, 10)?.fieldType)
        assertEquals(SSFField.STRING_PAYLOAD, parser.findSegmentForOffset(segments, 999999)?.fieldType)
    }

    @Test
    fun testFindSegmentForOffset_returnsNullIfBeforeFirstSegment() {
        val segments = listOf(
            SSFSegment(5, SSFField.STRING),
            SSFSegment(10, SSFField.STRING_PAYLOAD)
        )

        assertNull(parser.findSegmentForOffset(segments, 0))
        assertNull(parser.findSegmentForOffset(segments, 4))
    }

    @Test
    fun testFindSegmentForOffset_returnsNullIfEmptyList() {
        val segments = emptyList<SSFSegment>()
        assertNull(parser.findSegmentForOffset(segments, 0))
    }

    @Test
    fun testFindSegmentForOffset_exactlyOnSegmentStart() {
        val segments = listOf(
            SSFSegment(3, SSFField.UNKNOWN),
            SSFSegment(7, SSFField.STRING)
        )

        assertEquals(SSFField.UNKNOWN, parser.findSegmentForOffset(segments, 3)?.fieldType)
        assertEquals(SSFField.STRING, parser.findSegmentForOffset(segments, 7)?.fieldType)
    }

    @Test
    fun testFindSegmentForOffset_exactlyOnSegmentEnd_returnsPrevious() {
        val segments = listOf(
            SSFSegment(0, SSFField.UNKNOWN),
            SSFSegment(5, SSFField.STRING)
        )
        // Offset 5 is start of next segment, so should return segment[1]
        assertEquals(SSFField.STRING, parser.findSegmentForOffset(segments, 5)?.fieldType)
        // Offset 4 is still within segment[0]
        assertEquals(SSFField.UNKNOWN, parser.findSegmentForOffset(segments, 4)?.fieldType)
    }

    @Test
    fun testDetectLengthFieldInMessage_valid1ByteLE() {
        // 03 41 42 43 → length=3, payload="ABC", UNKNOWN segment
        val msg = SSFParsedMessage(
            segments = listOf(SSFSegment(0, SSFField.UNKNOWN)),
            bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertEquals(0 to 3, result)
    }

    @Test
    fun testDetectLengthFieldInMessage_valid2ByteBE() {
        // 00 03 41 42 43 → BE: length = 3
        val msg = SSFParsedMessage(
            segments = listOf(SSFSegment(0, SSFField.UNKNOWN)),
            bytes = byteArrayOf(0x00, 0x03, 0x41, 0x42, 0x43),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 2, bigEndian = true)
        assertEquals(0 to 3, result)
    }

    @Test
    fun testDetectLengthFieldInMessage_rejectsWrongSegmentType() {
        // valid length, but segment is STRING not UNKNOWN
        val msg = SSFParsedMessage(
            segments = listOf(SSFSegment(0, SSFField.STRING)),
            bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertNull(result)
    }

    @Test
    fun testDetectLengthFieldInMessage_rejectsIfPayloadEndNotEqualToMessageSize() {
        // length = 3, but message size is 5 → should be rejected
        val msg = SSFParsedMessage(
            segments = listOf(SSFSegment(0, SSFField.UNKNOWN)),
            bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43, 0x44),
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertNull(result)
    }

    @Test
    fun testDetectLengthFieldInMessage_returnsNullIfNoMatch() {
        // no valid length field
        val msg = SSFParsedMessage(
            segments = listOf(SSFSegment(0, SSFField.UNKNOWN)),
            bytes = byteArrayOf(0x7F, 0x00), // length = 127, too long
            msgIndex = 0
        )

        val result = parser.detectLengthFieldInMessage(msg, 1, bigEndian = false)
        assertNull(result)
    }

    @Test
    fun testDetectLengthFieldInMessage_findsCorrectOffsetAmongMultiple() {
        // 02 00 03 41 42 43 → LE: offset 2 → length = 3, payload="ABC"
        val msg = SSFParsedMessage(
            segments = listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(2, SSFField.UNKNOWN)
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
            SSFParsedMessage(
                segments = listOf(SSFSegment(0, SSFField.UNKNOWN)),
                bytes = byteArrayOf(0x03, 0x41, 0x42, 0x43), // length = 3, payload = "ABC"
                msgIndex = 0
            ),
            SSFParsedMessage(
                segments = listOf(SSFSegment(0, SSFField.UNKNOWN)),
                bytes = byteArrayOf(0x02, 0x41, 0x42), // length = 2, payload = "AB"
                msgIndex = 1
            )
        )

        val actual = parser.detectMessageLengthField(messages)

        assertEquals(2, actual.size)

        actual.forEachIndexed { index, msg ->
            val segments = msg.segments.sortedBy { it.offset }
            val lengthField = segments.find { it.fieldType.name.startsWith("MESSAGE_LENGTH") }
            val unknownAfterLength = segments.find { it.offset > (lengthField?.offset ?: -1) }

            assertNotNull(lengthField, "Length field missing in msg[$index]")
            assertEquals(SSFField.UNKNOWN, unknownAfterLength?.fieldType)
        }
    }

    @Test
    fun testDetectMessageLengthField_returnsOriginalWhenNoValidConfig() {
        val messages = listOf(
            SSFParsedMessage(
                segments = listOf(SSFSegment(0, SSFField.STRING)),
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
            SSFParsedMessage(
                segments = listOf(
                    SSFSegment(0, SSFField.UNKNOWN),
                    SSFSegment(2, SSFField.STRING)
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
}
