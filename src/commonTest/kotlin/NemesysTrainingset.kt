import bitmage.fromHex
import decoders.Nemesys.*
import kotlin.test.Test
import kotlin.test.assertTrue

class NemesysTrainingset {

    private val parser = NemesysParser()

    private fun printSegmentParsingResult(
        testNumber: Int,
        expectedSegments: List<NemesysSegment>,
        actualSegments: List<NemesysSegment>, ) {
        val truePositives = expectedSegments.count { expected ->
            actualSegments.any { actual ->
                actual.offset == expected.offset && actual.fieldType == expected.fieldType
            }
        }

        val falsePositives = actualSegments.count { actual ->
            expectedSegments.none { expected ->
                actual.offset == expected.offset && actual.fieldType == expected.fieldType
            }
        }

        val precision = truePositives.toDouble() / (truePositives + falsePositives).coerceAtLeast(1)
        val recall = truePositives.toDouble() / expectedSegments.size
        val f1 = 2 * precision * recall / (precision + recall).coerceAtLeast(1e-9)

        println("----- testSegmentParsing$testNumber -----")
        println("True Positives: $truePositives")
        println("False Positives: $falsePositives")
        println("Precision: ${(precision * 100).toInt()}%")
        println("Recall: ${(recall * 100).toInt()}%")
        println("F1 Score: ${(f1 * 100).toInt()}%")

        assertTrue(f1 >= 0.8, "F1 score should be at least 80%")
    }

    private fun printSequenceAlignmentResult(testNumber: Int, messages: Map<Int, NemesysParsedMessage>, expectedAlignments: Set<Triple<Int, Int, Pair<Int, Int>>>) {
        val alignments = NemesysSequenceAlignment.alignSegments(messages)
        val foundAlignments = alignments.map { Triple(it.protocolA, it.protocolB, it.segmentIndexA to it.segmentIndexB) }.toSet()

        val correctMatches = foundAlignments.intersect(expectedAlignments.toSet())
        val accuracy = correctMatches.size.toDouble() / expectedAlignments.size

        println("----- testSequenceAlignment$testNumber -----")
        println("Correct matches: ${correctMatches.size} of ${expectedAlignments.size}")
        println("Alignment Accuracy: ${(accuracy * 100).toInt()}%")

        assertTrue(accuracy >= 0.8, "At least 80% of the alignments should be correct")
    }

    @Test
    fun testSegmentParsing1() {
        val bytes = "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102437444431444343412d374330442d343145362d423337342d433133333935354443373634080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(13, NemesysField.UNKNOWN),
            NemesysSegment(21, NemesysField.UNKNOWN),
            NemesysSegment(31, NemesysField.UNKNOWN),
            NemesysSegment(33, NemesysField.UNKNOWN),
            NemesysSegment(72, NemesysField.UNKNOWN)
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(1, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing2() {
        val bytes = "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102446394532423231352d393431372d344141372d413439302d384446364539443445364639080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(13, NemesysField.UNKNOWN),
            NemesysSegment(21, NemesysField.UNKNOWN),
            NemesysSegment(31, NemesysField.UNKNOWN),
            NemesysSegment(33, NemesysField.UNKNOWN),
            NemesysSegment(72, NemesysField.UNKNOWN)
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(2, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing3() {
        val bytes = "A5626964187B68757365726E616D6565616C69636565656D61696C71616C696365406578616D706C652E636F6D6770726F66696C65A263616765181E67636F756E747279674765726D616E796969735F616374697665F5".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),   // Start des gesamten Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),   // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),   // 123
            NemesysSegment(6, NemesysField.UNKNOWN),   // "username"
            NemesysSegment(15, NemesysField.UNKNOWN),  // "alice"
            NemesysSegment(21, NemesysField.UNKNOWN),  // "email"
            NemesysSegment(27, NemesysField.UNKNOWN),  // "alice@example.com"
            NemesysSegment(45, NemesysField.UNKNOWN),  // "profile"
            NemesysSegment(53, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
            NemesysSegment(54, NemesysField.UNKNOWN),  // "age"
            NemesysSegment(58, NemesysField.UNKNOWN),  // 30
            NemesysSegment(60, NemesysField.UNKNOWN),  // "country"
            NemesysSegment(68, NemesysField.UNKNOWN),  // "Germany"
            NemesysSegment(76, NemesysField.UNKNOWN),  // "is_active"
            NemesysSegment(86, NemesysField.UNKNOWN)   // true
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(3, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing4() {
        val bytes = "A56269640368757365726E616D6563626F6265656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E747279635553416969735F616374697665F4".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),   // Start des gesamten Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),   // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),   // 3
            NemesysSegment(5, NemesysField.UNKNOWN),   // "username"
            NemesysSegment(14, NemesysField.UNKNOWN),  // "bob"
            NemesysSegment(18, NemesysField.UNKNOWN),  // "email"
            NemesysSegment(24, NemesysField.UNKNOWN),  // "bob@gmx.de"
            NemesysSegment(35, NemesysField.UNKNOWN),  // "profile"
            NemesysSegment(43, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
            NemesysSegment(44, NemesysField.UNKNOWN),  // "age"
            NemesysSegment(48, NemesysField.UNKNOWN),  // 76
            NemesysSegment(50, NemesysField.UNKNOWN),  // "country"
            NemesysSegment(58, NemesysField.UNKNOWN),  // "USA"
            NemesysSegment(62, NemesysField.UNKNOWN),  // "is_active"
            NemesysSegment(72, NemesysField.UNKNOWN)   // false
        )


        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(4, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing5() {
        val bytes = "A76269640C67766F726E616D65634D6178686E6163686E616D656A4D75737465726D616E6E68757365726E616D65636D617865656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E7472796A4175737472616C69656E6969735F616374697665F4".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),     // Start des Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),     // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),     // 12
            NemesysSegment(5, NemesysField.UNKNOWN),     // "vorname"
            NemesysSegment(13, NemesysField.UNKNOWN),    // "Max"
            NemesysSegment(17, NemesysField.UNKNOWN),    // "nachname"
            NemesysSegment(26, NemesysField.UNKNOWN),    // "Mustermann"
            NemesysSegment(37, NemesysField.UNKNOWN),    // "username"
            NemesysSegment(46, NemesysField.UNKNOWN),    // "max"
            NemesysSegment(50, NemesysField.UNKNOWN),    // "email"
            NemesysSegment(56, NemesysField.UNKNOWN),    // "bob@gmx.de"
            NemesysSegment(67, NemesysField.UNKNOWN),    // "profile"
            NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes Objekt beginnt
            NemesysSegment(76, NemesysField.UNKNOWN),    // "age"
            NemesysSegment(80, NemesysField.UNKNOWN),    // 76
            NemesysSegment(82, NemesysField.UNKNOWN),    // "country"
            NemesysSegment(90, NemesysField.UNKNOWN),    // "Australien"
            NemesysSegment(101, NemesysField.UNKNOWN),   // "is_active"
            NemesysSegment(111, NemesysField.UNKNOWN)    // false
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(5, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing6() {
        val bytes = "A76269641870686E6163686E616D65674E65756D616E6E68757365726E616D656A6E65756D616E6E78587865656D61696C726E65756D616E6E406F75746C6F6F6B2E646566686F62627973A266686F62627931684675C39F62616C6C66686F626279326A4261736B657462616C6C6770726F66696C65A2636167651267636F756E7472796B446575747363686C616E646969735F616374697665F5".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),     // Start des Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),     // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),     // 112
            NemesysSegment(6, NemesysField.UNKNOWN),     // "nachname"
            NemesysSegment(15, NemesysField.UNKNOWN),    // "Neumann"
            NemesysSegment(23, NemesysField.UNKNOWN),    // "username"
            NemesysSegment(32, NemesysField.UNKNOWN),    // "neumannxXx"
            NemesysSegment(43, NemesysField.UNKNOWN),    // "email"
            NemesysSegment(49, NemesysField.UNKNOWN),    // "neumann@outlook.de"
            NemesysSegment(68, NemesysField.UNKNOWN),    // "hobbys"
            NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes "hobbys"-Objekt
            NemesysSegment(76, NemesysField.UNKNOWN),    // "hobby1"
            NemesysSegment(83, NemesysField.UNKNOWN),    // "Fußball"
            NemesysSegment(92, NemesysField.UNKNOWN),    // "hobby2"
            NemesysSegment(99, NemesysField.UNKNOWN),    // "Basketball"
            NemesysSegment(110, NemesysField.UNKNOWN),   // "profile"
            NemesysSegment(118, NemesysField.UNKNOWN),   // verschachteltes "profile"-Objekt
            NemesysSegment(119, NemesysField.UNKNOWN),   // "age"
            NemesysSegment(123, NemesysField.UNKNOWN),   // 18
            NemesysSegment(124, NemesysField.UNKNOWN),   // "country"
            NemesysSegment(132, NemesysField.UNKNOWN),   // "Deutschland"
            NemesysSegment(144, NemesysField.UNKNOWN),   // "is_active"
            NemesysSegment(154, NemesysField.UNKNOWN)    // true
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(6, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing7() {
        val bytes = "62706c6973743030d30102030405065173516651625c636f6d2e6b696b2e636861741105ffa107d208090a0b516851645f102036366137626435396665376639613763323265623436336436646233393730342341d95c3031cf51a2080f1113152225272c2e30530000000000000101000000000000000c0000000000000000000000000000005c".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),     // Start der BPList
            NemesysSegment(8, NemesysField.UNKNOWN),     // Start des Root-Objekts (Dictionary)
            NemesysSegment(15, NemesysField.UNKNOWN),    // "s"
            NemesysSegment(17, NemesysField.UNKNOWN),    // "f"
            NemesysSegment(19, NemesysField.UNKNOWN),    // "b"
            NemesysSegment(21, NemesysField.UNKNOWN),    // "com.kik.chat"
            NemesysSegment(34, NemesysField.UNKNOWN),    // 1535
            NemesysSegment(37, NemesysField.UNKNOWN),    // Start des Arrays
            NemesysSegment(39, NemesysField.UNKNOWN),    // Start des Dictionary-Objekts im Array
            NemesysSegment(44, NemesysField.UNKNOWN),    // "h"
            NemesysSegment(46, NemesysField.UNKNOWN),    // "d"
            NemesysSegment(48, NemesysField.UNKNOWN),    // "66a7bd59fe7f9a7c22eb463d6db39704"
            NemesysSegment(83, NemesysField.UNKNOWN),
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(7, expectedSegments, actualSegments)
    }

    @Test
    fun testSequenceAlignment1() {
        val message1 = "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102437444431444343412d374330442d343145362d423337342d433133333935354443373634080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex()
        val message2 = "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102446394532423231352d393431372d344141372d413439302d384446364539443445364639080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex()

        val segments1 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(13, NemesysField.UNKNOWN),
            NemesysSegment(21, NemesysField.UNKNOWN),
            NemesysSegment(31, NemesysField.UNKNOWN),
            NemesysSegment(33, NemesysField.UNKNOWN),
            NemesysSegment(72, NemesysField.UNKNOWN)
        )
        val segments2 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(13, NemesysField.UNKNOWN),
            NemesysSegment(21, NemesysField.UNKNOWN),
            NemesysSegment(31, NemesysField.UNKNOWN),
            NemesysSegment(33, NemesysField.UNKNOWN),
            NemesysSegment(72, NemesysField.UNKNOWN)
        )

        val messages = mapOf(
            0 to NemesysParsedMessage(segments1, message1, 0),
            1 to NemesysParsedMessage(segments2, message2, 1)
        )


        val expectedAlignments = setOf(
            Triple(0, 1, Pair(0, 0)),
            Triple(0, 1, Pair(1, 1)),
            Triple(0, 1, Pair(2, 2)),
            Triple(0, 1, Pair(3, 3)),
            Triple(0, 1, Pair(4, 4)),
            Triple(0, 1, Pair(5, 5)),
            Triple(0, 1, Pair(6, 6))
        )

        printSequenceAlignmentResult(1, messages, expectedAlignments)
    }

    @Test
    fun testSequenceAlignment2() {
        val message1 = "A5626964187B68757365726E616D6565616C69636565656D61696C71616C696365406578616D706C652E636F6D6770726F66696C65A263616765181E67636F756E747279674765726D616E796969735F616374697665F5".fromHex()
        val message2 = "A56269640368757365726E616D6563626F6265656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E747279635553416969735F616374697665F4".fromHex()

        val segments1 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),   // Start des gesamten Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),   // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),   // 123
            NemesysSegment(6, NemesysField.UNKNOWN),   // "username"
            NemesysSegment(15, NemesysField.UNKNOWN),  // "alice"
            NemesysSegment(21, NemesysField.UNKNOWN),  // "email"
            NemesysSegment(27, NemesysField.UNKNOWN),  // "alice@example.com"
            NemesysSegment(45, NemesysField.UNKNOWN),  // "profile"
            NemesysSegment(53, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
            NemesysSegment(54, NemesysField.UNKNOWN),  // "age"
            NemesysSegment(58, NemesysField.UNKNOWN),  // 30
            NemesysSegment(60, NemesysField.UNKNOWN),  // "country"
            NemesysSegment(68, NemesysField.UNKNOWN),  // "Germany"
            NemesysSegment(76, NemesysField.UNKNOWN),  // "is_active"
            NemesysSegment(86, NemesysField.UNKNOWN)   // true
        )
        val segments2 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),   // Start des gesamten Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),   // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),   // 3
            NemesysSegment(5, NemesysField.UNKNOWN),   // "username"
            NemesysSegment(14, NemesysField.UNKNOWN),  // "bob"
            NemesysSegment(18, NemesysField.UNKNOWN),  // "email"
            NemesysSegment(24, NemesysField.UNKNOWN),  // "bob@gmx.de"
            NemesysSegment(35, NemesysField.UNKNOWN),  // "profile"
            NemesysSegment(43, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
            NemesysSegment(44, NemesysField.UNKNOWN),  // "age"
            NemesysSegment(48, NemesysField.UNKNOWN),  // 76
            NemesysSegment(50, NemesysField.UNKNOWN),  // "country"
            NemesysSegment(58, NemesysField.UNKNOWN),  // "USA"
            NemesysSegment(62, NemesysField.UNKNOWN),  // "is_active"
            NemesysSegment(72, NemesysField.UNKNOWN)   // false
        )

        val messages = mapOf(
            0 to NemesysParsedMessage(segments1, message1, 0),
            1 to NemesysParsedMessage(segments2, message2, 1)
        )


        val expectedAlignments = setOf(
            Triple(0, 1, Pair(0, 0)),
            Triple(0, 1, Pair(1, 1)),
            Triple(0, 1, Pair(2, 2)),
            Triple(0, 1, Pair(3, 3)),
            Triple(0, 1, Pair(4, 4)),
            Triple(0, 1, Pair(5, 5)),
            Triple(0, 1, Pair(6, 6)),
            Triple(0, 1, Pair(7, 7)),
            Triple(0, 1, Pair(8, 8)),
            Triple(0, 1, Pair(9, 9)),
            Triple(0, 1, Pair(10, 10)),
            Triple(0, 1, Pair(11, 11)),
            Triple(0, 1, Pair(12, 12)),
            Triple(0, 1, Pair(13, 13)),
            Triple(0, 1, Pair(14, 14))
        )

        printSequenceAlignmentResult(2, messages, expectedAlignments)
    }

    @Test
    fun testSequenceAlignment3() {
        val message1 = "A76269640C67766F726E616D65634D6178686E6163686E616D656A4D75737465726D616E6E68757365726E616D65636D617865656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E7472796A4175737472616C69656E6969735F616374697665F4".fromHex()
        val message2 = "A76269641870686E6163686E616D65674E65756D616E6E68757365726E616D656A6E65756D616E6E78587865656D61696C726E65756D616E6E406F75746C6F6F6B2E646566686F62627973A266686F62627931684675C39F62616C6C66686F626279326A4261736B657462616C6C6770726F66696C65A2636167651267636F756E7472796B446575747363686C616E646969735F616374697665F5".fromHex()

        val segments1 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),     // Start des Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),     // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),     // 12
            NemesysSegment(5, NemesysField.UNKNOWN),     // "vorname"
            NemesysSegment(13, NemesysField.UNKNOWN),    // "Max"
            NemesysSegment(17, NemesysField.UNKNOWN),    // "nachname"
            NemesysSegment(26, NemesysField.UNKNOWN),    // "Mustermann"
            NemesysSegment(37, NemesysField.UNKNOWN),    // "username"
            NemesysSegment(46, NemesysField.UNKNOWN),    // "max"
            NemesysSegment(50, NemesysField.UNKNOWN),    // "email"
            NemesysSegment(56, NemesysField.UNKNOWN),    // "bob@gmx.de"
            NemesysSegment(67, NemesysField.UNKNOWN),    // "profile"
            NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes Objekt beginnt
            NemesysSegment(76, NemesysField.UNKNOWN),    // "age"
            NemesysSegment(80, NemesysField.UNKNOWN),    // 76
            NemesysSegment(82, NemesysField.UNKNOWN),    // "country"
            NemesysSegment(90, NemesysField.UNKNOWN),    // "Australien"
            NemesysSegment(101, NemesysField.UNKNOWN),   // "is_active"
            NemesysSegment(111, NemesysField.UNKNOWN)    // false
        )
        val segments2 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),     // Start des Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),     // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),     // 112
            NemesysSegment(6, NemesysField.UNKNOWN),     // "nachname"
            NemesysSegment(15, NemesysField.UNKNOWN),    // "Neumann"
            NemesysSegment(23, NemesysField.UNKNOWN),    // "username"
            NemesysSegment(32, NemesysField.UNKNOWN),    // "neumannxXx"
            NemesysSegment(43, NemesysField.UNKNOWN),    // "email"
            NemesysSegment(49, NemesysField.UNKNOWN),    // "neumann@outlook.de"
            NemesysSegment(68, NemesysField.UNKNOWN),    // "hobbys"
            NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes "hobbys"-Objekt
            NemesysSegment(76, NemesysField.UNKNOWN),    // "hobby1"
            NemesysSegment(83, NemesysField.UNKNOWN),    // "Fußball"
            NemesysSegment(92, NemesysField.UNKNOWN),    // "hobby2"
            NemesysSegment(99, NemesysField.UNKNOWN),    // "Basketball"
            NemesysSegment(110, NemesysField.UNKNOWN),   // "profile"
            NemesysSegment(118, NemesysField.UNKNOWN),   // verschachteltes "profile"-Objekt
            NemesysSegment(119, NemesysField.UNKNOWN),   // "age"
            NemesysSegment(123, NemesysField.UNKNOWN),   // 18
            NemesysSegment(124, NemesysField.UNKNOWN),   // "country"
            NemesysSegment(132, NemesysField.UNKNOWN),   // "Deutschland"
            NemesysSegment(144, NemesysField.UNKNOWN),   // "is_active"
            NemesysSegment(154, NemesysField.UNKNOWN)    // true
        )

        val messages = mapOf(
            0 to NemesysParsedMessage(segments1, message1, 0),
            1 to NemesysParsedMessage(segments2, message2, 1)
        )


        val expectedAlignments = setOf(
            Triple(0, 1, Pair(0, 0)),
            Triple(0, 1, Pair(1, 1)),
            Triple(0, 1, Pair(2, 2)),
            Triple(0, 1, Pair(5, 3)),
            Triple(1, 0, Pair(4, 6)),
            Triple(0, 1, Pair(7, 5)),
            Triple(1, 0, Pair(6, 8)),
            Triple(0, 1, Pair(9, 7)),
            Triple(0, 1, Pair(10, 8)),
            Triple(1, 0, Pair(15, 11)),
            Triple(0, 1, Pair(12, 16)),
            Triple(1, 0, Pair(17, 13)),
            Triple(0, 1, Pair(14, 18)),
            Triple(1, 0, Pair(19, 15)),
            Triple(0, 1, Pair(16, 20)),
            Triple(1, 0, Pair(21, 17)),
            Triple(0, 1, Pair(18, 22))
        )

        printSequenceAlignmentResult(3, messages, expectedAlignments)
    }



}