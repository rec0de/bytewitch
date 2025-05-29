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
            NemesysSegment(0, NemesysField.STRING),
            NemesysSegment(6, NemesysField.STRING),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(13, NemesysField.UNKNOWN),
            NemesysSegment(14, NemesysField.STRING),
            NemesysSegment(21, NemesysField.UNKNOWN),
            NemesysSegment(22, NemesysField.STRING),
            NemesysSegment(31, NemesysField.UNKNOWN),
            NemesysSegment(32, NemesysField.UNKNOWN),
            NemesysSegment(33, NemesysField.UNKNOWN),
            NemesysSegment(35, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),
            NemesysSegment(36, NemesysField.STRING),
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
            NemesysSegment(0, NemesysField.STRING),
            NemesysSegment(6, NemesysField.STRING),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(13, NemesysField.UNKNOWN),
            NemesysSegment(14, NemesysField.STRING),
            NemesysSegment(21, NemesysField.UNKNOWN),
            NemesysSegment(22, NemesysField.STRING),
            NemesysSegment(31, NemesysField.UNKNOWN),
            NemesysSegment(32, NemesysField.UNKNOWN),
            NemesysSegment(33, NemesysField.UNKNOWN),
            NemesysSegment(35, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),
            NemesysSegment(36, NemesysField.STRING),
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
            NemesysSegment(1, NemesysField.UNKNOWN),   // "id" type
            NemesysSegment(2, NemesysField.STRING),   // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),   // 123
            NemesysSegment(6, NemesysField.UNKNOWN),   // "username" type
            NemesysSegment(7, NemesysField.STRING),   // "username"
            NemesysSegment(15, NemesysField.UNKNOWN),  // "alice" type
            NemesysSegment(16, NemesysField.STRING),  // "alice"
            NemesysSegment(21, NemesysField.UNKNOWN),  // "email" type
            NemesysSegment(22, NemesysField.STRING),  // "email"
            NemesysSegment(27, NemesysField.UNKNOWN),  // "alice@example.com" type
            NemesysSegment(28, NemesysField.STRING),  // "alice@example.com"
            NemesysSegment(45, NemesysField.UNKNOWN),  // "profile" type
            NemesysSegment(46, NemesysField.STRING),  // "profile"
            NemesysSegment(53, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
            NemesysSegment(54, NemesysField.UNKNOWN),  // "age" type
            NemesysSegment(55, NemesysField.STRING),  // "age"
            NemesysSegment(58, NemesysField.UNKNOWN),  // 30
            NemesysSegment(60, NemesysField.UNKNOWN),  // "country" type
            NemesysSegment(61, NemesysField.STRING),  // "country"
            NemesysSegment(68, NemesysField.UNKNOWN),  // "Germany" type
            NemesysSegment(69, NemesysField.STRING),  // "Germany"
            NemesysSegment(76, NemesysField.UNKNOWN),  // "is_active" type
            NemesysSegment(77, NemesysField.STRING),  // "is_active"
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
            NemesysSegment(1, NemesysField.UNKNOWN),   // "id" type
            NemesysSegment(2, NemesysField.STRING),   // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),   // 3
            NemesysSegment(5, NemesysField.UNKNOWN),   // "username" type
            NemesysSegment(6, NemesysField.STRING),   // "username"
            NemesysSegment(14, NemesysField.UNKNOWN),  // "bob" type
            NemesysSegment(15, NemesysField.STRING),  // "bob"
            NemesysSegment(18, NemesysField.UNKNOWN),  // "email" type
            NemesysSegment(19, NemesysField.STRING),  // "email"
            NemesysSegment(24, NemesysField.UNKNOWN),  // "bob@gmx.de" type
            NemesysSegment(25, NemesysField.STRING),  // "bob@gmx.de"
            NemesysSegment(35, NemesysField.UNKNOWN),  // "profile" type
            NemesysSegment(36, NemesysField.STRING),  // "profile"
            NemesysSegment(43, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
            NemesysSegment(44, NemesysField.UNKNOWN),  // "age" type
            NemesysSegment(45, NemesysField.STRING),  // "age"
            NemesysSegment(48, NemesysField.UNKNOWN),  // 76
            NemesysSegment(50, NemesysField.UNKNOWN),  // "country" type
            NemesysSegment(51, NemesysField.STRING),  // "country"
            NemesysSegment(58, NemesysField.UNKNOWN),  // "USA" type
            NemesysSegment(59, NemesysField.STRING),  // "USA"
            NemesysSegment(62, NemesysField.UNKNOWN),  // "is_active" type
            NemesysSegment(63, NemesysField.STRING),  // "is_active"
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
            NemesysSegment(1, NemesysField.UNKNOWN),     // "id" type
            NemesysSegment(2, NemesysField.STRING),     // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),     // 12
            NemesysSegment(5, NemesysField.UNKNOWN),     // "vorname" type
            NemesysSegment(6, NemesysField.STRING),     // "vorname"
            NemesysSegment(13, NemesysField.UNKNOWN),    // "Max" type
            NemesysSegment(14, NemesysField.STRING),    // "Max"
            NemesysSegment(17, NemesysField.UNKNOWN),    // "nachname" type
            NemesysSegment(18, NemesysField.STRING),    // "nachname"
            NemesysSegment(26, NemesysField.UNKNOWN),    // "Mustermann" type
            NemesysSegment(27, NemesysField.STRING),    // "Mustermann"
            NemesysSegment(37, NemesysField.UNKNOWN),    // "username" type
            NemesysSegment(38, NemesysField.STRING),    // "username"
            NemesysSegment(46, NemesysField.UNKNOWN),    // "max" type
            NemesysSegment(47, NemesysField.STRING),    // "max"
            NemesysSegment(50, NemesysField.UNKNOWN),    // "email" type
            NemesysSegment(51, NemesysField.STRING),    // "email"
            NemesysSegment(56, NemesysField.UNKNOWN),    // "bob@gmx.de" type
            NemesysSegment(57, NemesysField.STRING),    // "bob@gmx.de"
            NemesysSegment(67, NemesysField.UNKNOWN),    // "profile" type
            NemesysSegment(68, NemesysField.UNKNOWN),    // "profile"
            NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes Objekt beginnt
            NemesysSegment(76, NemesysField.UNKNOWN),    // "age" type
            NemesysSegment(77, NemesysField.STRING),    // "age"
            NemesysSegment(80, NemesysField.UNKNOWN),    // 76 type
            NemesysSegment(81, NemesysField.UNKNOWN),    // 76
            NemesysSegment(82, NemesysField.UNKNOWN),    // "country" type
            NemesysSegment(83, NemesysField.STRING),    // "country"
            NemesysSegment(90, NemesysField.UNKNOWN),    // "Australien" type
            NemesysSegment(91, NemesysField.STRING),    // "Australien" STRING
            NemesysSegment(101, NemesysField.UNKNOWN),   // "is_active" type
            NemesysSegment(102, NemesysField.STRING),   // "is_active"
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
            NemesysSegment(1, NemesysField.UNKNOWN),     // "id" type
            NemesysSegment(2, NemesysField.STRING),     // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),     // 112
            NemesysSegment(6, NemesysField.UNKNOWN),     // "nachname" type
            NemesysSegment(7, NemesysField.STRING),     // "nachname"
            NemesysSegment(15, NemesysField.UNKNOWN),    // "Neumann" type
            NemesysSegment(16, NemesysField.STRING),    // "Neumann"
            NemesysSegment(23, NemesysField.UNKNOWN),    // "username" type
            NemesysSegment(24, NemesysField.STRING),    // "username"
            NemesysSegment(32, NemesysField.UNKNOWN),    // "neumannxXx" type
            NemesysSegment(33, NemesysField.STRING),    // "neumannxXx"
            NemesysSegment(43, NemesysField.UNKNOWN),    // "email" type
            NemesysSegment(44, NemesysField.STRING),    // "email"
            NemesysSegment(49, NemesysField.UNKNOWN),    // "neumann@outlook.de" type
            NemesysSegment(50, NemesysField.STRING),    // "neumann@outlook.de"
            NemesysSegment(68, NemesysField.UNKNOWN),    // "hobbys" type
            NemesysSegment(69, NemesysField.STRING),    // "hobbys"
            NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes "hobbys"-Objekt
            NemesysSegment(76, NemesysField.UNKNOWN),    // "hobby1" type
            NemesysSegment(77, NemesysField.STRING),    // "hobby1"
            NemesysSegment(83, NemesysField.UNKNOWN),    // "Fußball" type
            NemesysSegment(84, NemesysField.STRING),    // "Fußball"
            NemesysSegment(92, NemesysField.UNKNOWN),    // "hobby2" type
            NemesysSegment(93, NemesysField.STRING),    // "hobby2"
            NemesysSegment(99, NemesysField.UNKNOWN),    // "Basketball" type
            NemesysSegment(100, NemesysField.STRING),    // "Basketball"
            NemesysSegment(110, NemesysField.UNKNOWN),   // "profile" type
            NemesysSegment(111, NemesysField.STRING),   // "profile"
            NemesysSegment(118, NemesysField.UNKNOWN),   // verschachteltes "profile"-Objekt
            NemesysSegment(119, NemesysField.UNKNOWN),   // "age" type
            NemesysSegment(120, NemesysField.STRING),   // "age"
            NemesysSegment(123, NemesysField.UNKNOWN),   // 18
            NemesysSegment(124, NemesysField.UNKNOWN),   // "country" type
            NemesysSegment(125, NemesysField.STRING),   // "country"
            NemesysSegment(132, NemesysField.UNKNOWN),   // "Deutschland" type
            NemesysSegment(133, NemesysField.STRING),   // "Deutschland"
            NemesysSegment(144, NemesysField.UNKNOWN),   // "is_active" type
            NemesysSegment(145, NemesysField.STRING),   // "is_active"
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
            NemesysSegment(0, NemesysField.STRING),     // BPList
            NemesysSegment(6, NemesysField.STRING),     // BPList version
            NemesysSegment(8, NemesysField.UNKNOWN),     // Start des Root-Objekts (Dictionary)
            NemesysSegment(15, NemesysField.UNKNOWN),    // "s" type
            NemesysSegment(16, NemesysField.STRING),    // "s"
            NemesysSegment(17, NemesysField.UNKNOWN),    // "f" type
            NemesysSegment(18, NemesysField.STRING),    // "f"
            NemesysSegment(19, NemesysField.UNKNOWN),    // "b" type
            NemesysSegment(20, NemesysField.STRING),    // "b"
            NemesysSegment(21, NemesysField.UNKNOWN),    // "com.kik.chat" type
            NemesysSegment(22, NemesysField.STRING),    // "com.kik.chat"
            NemesysSegment(34, NemesysField.UNKNOWN),    // 1535
            NemesysSegment(37, NemesysField.UNKNOWN),    // Start des Arrays
            NemesysSegment(39, NemesysField.UNKNOWN),    // Start des Dictionary-Objekts im Array
            NemesysSegment(44, NemesysField.UNKNOWN),    // "h" type
            NemesysSegment(45, NemesysField.STRING),    // "h"
            NemesysSegment(46, NemesysField.UNKNOWN),    // "d" type
            NemesysSegment(47, NemesysField.STRING),    // "d"
            NemesysSegment(48, NemesysField.UNKNOWN),    // "66a7bd59fe7f9a7c22eb463d6db39704" type
            NemesysSegment(50, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),    // "66a7bd59fe7f9a7c22eb463d6db39704" length
            NemesysSegment(51, NemesysField.STRING),    // "66a7bd59fe7f9a7c22eb463d6db39704"
            NemesysSegment(83, NemesysField.UNKNOWN),
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(7, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing8() {
        val bytes = "62706c6973743030d401020304050c0d0e517252704751635165d3060708090a0b5f102341505350726f746f636f6c417070546f6b656e47656e6572617465526573706f6e73655f101241505350726f746f636f6c436f6d6d616e645f102c41505350726f746f636f6c417070546f6b656e47656e6572617465526573706f6e7365546f70696348617368100210124f1014998c5d4c6f0bb047fc827e799bb288562a6196425f102438424139413938422d393842462d344331332d414545332d453430463946433631433344100d5a70726f64756374696f6e08111316181a21475c8b8d8fa6cdcf0000000000000101000000000000000f000000000000000000000000000000da".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.STRING),        // BPList Header (0–6)
            NemesysSegment(6, NemesysField.UNKNOWN),        // BPList version
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(17, NemesysField.UNKNOWN),       // Key: "r" type
            NemesysSegment(18, NemesysField.STRING),       // Key: "r" text
            NemesysSegment(19, NemesysField.UNKNOWN),       // Key: "pG" type
            NemesysSegment(20, NemesysField.STRING),       // Key: "pG" tet
            NemesysSegment(22, NemesysField.UNKNOWN),       // Key: "c" type
            NemesysSegment(23, NemesysField.STRING),       // Key: "c" text
            NemesysSegment(24, NemesysField.UNKNOWN),       // Key: "e" type
            NemesysSegment(25, NemesysField.STRING),       // Key: "e" text
            NemesysSegment(26, NemesysField.UNKNOWN),       // Value: dict (nested)
            NemesysSegment(33, NemesysField.UNKNOWN),       // Key: "APSProtocolAppTokenGenerateResponse" type
            NemesysSegment(35, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),       // "APSProtocolAppTokenGenerateResponse" length
            NemesysSegment(36, NemesysField.STRING),       // "APSProtocolAppTokenGenerateResponse" text
            NemesysSegment(71, NemesysField.UNKNOWN),       // Key: "APSProtocolCommand" type
            NemesysSegment(73, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),       // Key: "APSProtocolCommand" length
            NemesysSegment(74, NemesysField.STRING),       // Key: "APSProtocolCommand" text
            NemesysSegment(92, NemesysField.UNKNOWN),       // Key: "APSProtocolAppTokenGenerateResponseTopicHash" type
            NemesysSegment(94, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),       // Key: "APSProtocolAppTokenGenerateResponseTopicHash" length
            NemesysSegment(95, NemesysField.STRING),       // Key: "APSProtocolAppTokenGenerateResponseTopicHash" text
            NemesysSegment(139, NemesysField.UNKNOWN),      // Value: Int(2)
            NemesysSegment(141, NemesysField.UNKNOWN),      // Value: Int(18)
            NemesysSegment(143, NemesysField.UNKNOWN),
            NemesysSegment(146, NemesysField.UNKNOWN),      // Value: Data 998c…642 (24 Bytes)
            NemesysSegment(166, NemesysField.UNKNOWN),      // Value: UUID-String
            NemesysSegment(205, NemesysField.UNKNOWN),      // Value: Int(13)
            NemesysSegment(207, NemesysField.UNKNOWN),      // Value: "production"
            NemesysSegment(208, NemesysField.STRING),      // Value: "production" text
            NemesysSegment(218, NemesysField.UNKNOWN)       // end
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(8, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing9() {
        val bytes = "08171002".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(1, NemesysField.UNKNOWN),
            NemesysSegment(2, NemesysField.UNKNOWN),
            NemesysSegment(3, NemesysField.UNKNOWN)
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(9, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing10() {
        val bytes = "fe4781820001000000000000037777770369666303636f6d0000010001".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(2, NemesysField.UNKNOWN),
            NemesysSegment(4, NemesysField.UNKNOWN),
            NemesysSegment(6, NemesysField.UNKNOWN),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(10, NemesysField.UNKNOWN),
            NemesysSegment(12, NemesysField.UNKNOWN),
            NemesysSegment(25, NemesysField.UNKNOWN),
            NemesysSegment(27, NemesysField.UNKNOWN)
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(10, expectedSegments, actualSegments)
    }

    @Test
    fun testSegmentParsing11() {
        val bytes = "19040aec0000027b000012850a6400c8d23d06a2535ed71ed23d09faa4673315d23d09faa1766325d23d09faa17b4b10".fromHex()

        val expectedSegments = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),
            NemesysSegment(2, NemesysField.UNKNOWN),
            NemesysSegment(3, NemesysField.UNKNOWN),
            NemesysSegment(4, NemesysField.UNKNOWN),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(12, NemesysField.UNKNOWN),
            NemesysSegment(16, NemesysField.UNKNOWN),
            NemesysSegment(24, NemesysField.UNKNOWN),
            NemesysSegment(32, NemesysField.UNKNOWN),
            NemesysSegment(40, NemesysField.UNKNOWN)
        )

        val parsed = NemesysParser().parse(bytes, msgIndex = 0)
        val actualSegments = parsed.segments

        printSegmentParsingResult(11, expectedSegments, actualSegments)
    }

    @Test
    fun testSequenceAlignment1() {
        val message1 = "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102437444431444343412d374330442d343145362d423337342d433133333935354443373634080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex()
        val message2 = "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102446394532423231352d393431372d344141372d413439302d384446364539443445364639080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex()

        val segments1 = listOf(
            NemesysSegment(0, NemesysField.STRING),
            NemesysSegment(6, NemesysField.STRING),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(13, NemesysField.UNKNOWN),
            NemesysSegment(14, NemesysField.STRING),
            NemesysSegment(21, NemesysField.UNKNOWN),
            NemesysSegment(22, NemesysField.STRING),
            NemesysSegment(31, NemesysField.UNKNOWN),
            NemesysSegment(32, NemesysField.UNKNOWN),
            NemesysSegment(33, NemesysField.UNKNOWN),
            NemesysSegment(35, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),
            NemesysSegment(36, NemesysField.STRING),
            NemesysSegment(72, NemesysField.UNKNOWN)
        )
        val segments2 = listOf(
            NemesysSegment(0, NemesysField.STRING),
            NemesysSegment(6, NemesysField.STRING),
            NemesysSegment(8, NemesysField.UNKNOWN),
            NemesysSegment(13, NemesysField.UNKNOWN),
            NemesysSegment(14, NemesysField.STRING),
            NemesysSegment(21, NemesysField.UNKNOWN),
            NemesysSegment(22, NemesysField.STRING),
            NemesysSegment(31, NemesysField.UNKNOWN),
            NemesysSegment(32, NemesysField.UNKNOWN),
            NemesysSegment(33, NemesysField.UNKNOWN),
            NemesysSegment(35, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN),
            NemesysSegment(36, NemesysField.STRING),
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
            Triple(0, 1, Pair(6, 6)),
            Triple(0, 1, Pair(7, 7)),
            Triple(0, 1, Pair(8, 8)),
            Triple(0, 1, Pair(9, 9)),
            Triple(0, 1, Pair(10, 10)),
            Triple(0, 1, Pair(11, 11)),
            Triple(0, 1, Pair(12, 12))
        )

        printSequenceAlignmentResult(1, messages, expectedAlignments)
    }

    @Test
    fun testSequenceAlignment2() {
        val message1 = "A5626964187B68757365726E616D6565616C69636565656D61696C71616C696365406578616D706C652E636F6D6770726F66696C65A263616765181E67636F756E747279674765726D616E796969735F616374697665F5".fromHex()
        val message2 = "A56269640368757365726E616D6563626F6265656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E747279635553416969735F616374697665F4".fromHex()

        val segments1 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),   // Start des gesamten Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),   // "id" type
            NemesysSegment(2, NemesysField.STRING),   // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),   // 123
            NemesysSegment(6, NemesysField.UNKNOWN),   // "username" type
            NemesysSegment(7, NemesysField.STRING),   // "username"
            NemesysSegment(15, NemesysField.UNKNOWN),  // "alice" type
            NemesysSegment(16, NemesysField.STRING),  // "alice"
            NemesysSegment(21, NemesysField.UNKNOWN),  // "email" type
            NemesysSegment(22, NemesysField.STRING),  // "email"
            NemesysSegment(27, NemesysField.UNKNOWN),  // "alice@example.com" type
            NemesysSegment(28, NemesysField.STRING),  // "alice@example.com"
            NemesysSegment(45, NemesysField.UNKNOWN),  // "profile" type
            NemesysSegment(46, NemesysField.STRING),  // "profile"
            NemesysSegment(53, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
            NemesysSegment(54, NemesysField.UNKNOWN),  // "age" type
            NemesysSegment(55, NemesysField.STRING),  // "age"
            NemesysSegment(58, NemesysField.UNKNOWN),  // 30
            NemesysSegment(60, NemesysField.UNKNOWN),  // "country" type
            NemesysSegment(61, NemesysField.STRING),  // "country"
            NemesysSegment(68, NemesysField.UNKNOWN),  // "Germany" type
            NemesysSegment(69, NemesysField.STRING),  // "Germany"
            NemesysSegment(76, NemesysField.UNKNOWN),  // "is_active" type
            NemesysSegment(77, NemesysField.STRING),  // "is_active"
            NemesysSegment(86, NemesysField.UNKNOWN)   // true
        )
        val segments2 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),   // Start des gesamten Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),   // "id" type
            NemesysSegment(2, NemesysField.STRING),   // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),   // 3
            NemesysSegment(5, NemesysField.UNKNOWN),   // "username" type
            NemesysSegment(6, NemesysField.STRING),   // "username"
            NemesysSegment(14, NemesysField.UNKNOWN),  // "bob" type
            NemesysSegment(15, NemesysField.STRING),  // "bob"
            NemesysSegment(18, NemesysField.UNKNOWN),  // "email" type
            NemesysSegment(19, NemesysField.STRING),  // "email"
            NemesysSegment(24, NemesysField.UNKNOWN),  // "bob@gmx.de" type
            NemesysSegment(25, NemesysField.STRING),  // "bob@gmx.de"
            NemesysSegment(35, NemesysField.UNKNOWN),  // "profile" type
            NemesysSegment(36, NemesysField.STRING),  // "profile"
            NemesysSegment(43, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
            NemesysSegment(44, NemesysField.UNKNOWN),  // "age" type
            NemesysSegment(45, NemesysField.STRING),  // "age"
            NemesysSegment(48, NemesysField.UNKNOWN),  // 76
            NemesysSegment(50, NemesysField.UNKNOWN),  // "country" type
            NemesysSegment(51, NemesysField.STRING),  // "country"
            NemesysSegment(58, NemesysField.UNKNOWN),  // "USA" type
            NemesysSegment(59, NemesysField.STRING),  // "USA"
            NemesysSegment(62, NemesysField.UNKNOWN),  // "is_active" type
            NemesysSegment(63, NemesysField.STRING),  // "is_active"
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
            Triple(1, 0, Pair(3, 3)),
            Triple(0, 1, Pair(4, 4)),
            Triple(1, 0, Pair(5, 5)),
            Triple(0, 1, Pair(6, 6)),
            Triple(1, 0, Pair(7, 7)),
            Triple(0, 1, Pair(8, 8)),
            Triple(1, 0, Pair(9, 9)),
            Triple(0, 1, Pair(10, 10)),
            Triple(1, 0, Pair(11, 11)),
            Triple(0, 1, Pair(12, 12)),
            Triple(1, 0, Pair(13, 13)),
            Triple(0, 1, Pair(14, 14)),
            Triple(1, 0, Pair(15, 15)),
            Triple(0, 1, Pair(16, 16)),
            Triple(0, 1, Pair(17, 17)),
            Triple(0, 1, Pair(18, 18)),
            Triple(0, 1, Pair(19, 19)),
            Triple(1, 0, Pair(20, 20)),
            Triple(0, 1, Pair(21, 21)),
            Triple(1, 0, Pair(22, 22)),
            Triple(0, 1, Pair(23, 23)),
            Triple(1, 0, Pair(24, 24)),
        )

        printSequenceAlignmentResult(2, messages, expectedAlignments)
    }

    @Test
    fun testSequenceAlignment3() {
        val message1 = "A76269640C67766F726E616D65634D6178686E6163686E616D656A4D75737465726D616E6E68757365726E616D65636D617865656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E7472796A4175737472616C69656E6969735F616374697665F4".fromHex()
        val message2 = "A76269641870686E6163686E616D65674E65756D616E6E68757365726E616D656A6E65756D616E6E78587865656D61696C726E65756D616E6E406F75746C6F6F6B2E646566686F62627973A266686F62627931684675C39F62616C6C66686F626279326A4261736B657462616C6C6770726F66696C65A2636167651267636F756E7472796B446575747363686C616E646969735F616374697665F5".fromHex()

        val segments1 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),     // Start des Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),     // "id" type
            NemesysSegment(2, NemesysField.STRING),     // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),     // 12
            NemesysSegment(5, NemesysField.UNKNOWN),     // "vorname" type
            NemesysSegment(6, NemesysField.STRING),     // "vorname"
            NemesysSegment(13, NemesysField.UNKNOWN),    // "Max" type
            NemesysSegment(14, NemesysField.STRING),    // "Max"
            NemesysSegment(17, NemesysField.UNKNOWN),    // "nachname" type
            NemesysSegment(18, NemesysField.STRING),    // "nachname"
            NemesysSegment(26, NemesysField.UNKNOWN),    // "Mustermann" type
            NemesysSegment(27, NemesysField.STRING),    // "Mustermann"
            NemesysSegment(37, NemesysField.UNKNOWN),    // "username" type
            NemesysSegment(38, NemesysField.STRING),    // "username"
            NemesysSegment(46, NemesysField.UNKNOWN),    // "max" type
            NemesysSegment(47, NemesysField.STRING),    // "max"
            NemesysSegment(50, NemesysField.UNKNOWN),    // "email" type
            NemesysSegment(51, NemesysField.STRING),    // "email"
            NemesysSegment(56, NemesysField.UNKNOWN),    // "bob@gmx.de" type
            NemesysSegment(57, NemesysField.STRING),    // "bob@gmx.de"
            NemesysSegment(67, NemesysField.UNKNOWN),    // "profile" type
            NemesysSegment(68, NemesysField.UNKNOWN),    // "profile"
            NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes Objekt beginnt
            NemesysSegment(76, NemesysField.UNKNOWN),    // "age" type
            NemesysSegment(77, NemesysField.STRING),    // "age"
            NemesysSegment(80, NemesysField.UNKNOWN),    // 76 type
            NemesysSegment(81, NemesysField.UNKNOWN),    // 76
            NemesysSegment(82, NemesysField.UNKNOWN),    // "country" type
            NemesysSegment(83, NemesysField.STRING),    // "country"
            NemesysSegment(90, NemesysField.UNKNOWN),    // "Australien" type
            NemesysSegment(91, NemesysField.STRING),    // "Australien" STRING
            NemesysSegment(101, NemesysField.UNKNOWN),   // "is_active" type
            NemesysSegment(102, NemesysField.STRING),   // "is_active"
            NemesysSegment(111, NemesysField.UNKNOWN)    // false
        )
        val segments2 = listOf(
            NemesysSegment(0, NemesysField.UNKNOWN),     // Start des Objekts
            NemesysSegment(1, NemesysField.UNKNOWN),     // "id" type
            NemesysSegment(2, NemesysField.STRING),     // "id"
            NemesysSegment(4, NemesysField.UNKNOWN),     // 112
            NemesysSegment(6, NemesysField.UNKNOWN),     // "nachname" type
            NemesysSegment(7, NemesysField.STRING),     // "nachname"
            NemesysSegment(15, NemesysField.UNKNOWN),    // "Neumann" type
            NemesysSegment(16, NemesysField.STRING),    // "Neumann"
            NemesysSegment(23, NemesysField.UNKNOWN),    // "username" type
            NemesysSegment(24, NemesysField.STRING),    // "username"
            NemesysSegment(32, NemesysField.UNKNOWN),    // "neumannxXx" type
            NemesysSegment(33, NemesysField.STRING),    // "neumannxXx"
            NemesysSegment(43, NemesysField.UNKNOWN),    // "email" type
            NemesysSegment(44, NemesysField.STRING),    // "email"
            NemesysSegment(49, NemesysField.UNKNOWN),    // "neumann@outlook.de" type
            NemesysSegment(50, NemesysField.STRING),    // "neumann@outlook.de"
            NemesysSegment(68, NemesysField.UNKNOWN),    // "hobbys" type
            NemesysSegment(69, NemesysField.STRING),    // "hobbys"
            NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes "hobbys"-Objekt
            NemesysSegment(76, NemesysField.UNKNOWN),    // "hobby1" type
            NemesysSegment(77, NemesysField.STRING),    // "hobby1"
            NemesysSegment(83, NemesysField.UNKNOWN),    // "Fußball" type
            NemesysSegment(84, NemesysField.STRING),    // "Fußball"
            NemesysSegment(92, NemesysField.UNKNOWN),    // "hobby2" type
            NemesysSegment(93, NemesysField.STRING),    // "hobby2"
            NemesysSegment(99, NemesysField.UNKNOWN),    // "Basketball" type
            NemesysSegment(100, NemesysField.STRING),    // "Basketball"
            NemesysSegment(110, NemesysField.UNKNOWN),   // "profile" type
            NemesysSegment(111, NemesysField.STRING),   // "profile"
            NemesysSegment(118, NemesysField.UNKNOWN),   // verschachteltes "profile"-Objekt
            NemesysSegment(119, NemesysField.UNKNOWN),   // "age" type
            NemesysSegment(120, NemesysField.STRING),   // "age"
            NemesysSegment(123, NemesysField.UNKNOWN),   // 18
            NemesysSegment(124, NemesysField.UNKNOWN),   // "country" type
            NemesysSegment(125, NemesysField.STRING),   // "country"
            NemesysSegment(132, NemesysField.UNKNOWN),   // "Deutschland" type
            NemesysSegment(133, NemesysField.STRING),   // "Deutschland"
            NemesysSegment(144, NemesysField.UNKNOWN),   // "is_active" type
            NemesysSegment(145, NemesysField.STRING),   // "is_active"
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
            Triple(0, 1, Pair(3, 3)),
            Triple(0, 1, Pair(8, 4)),
            Triple(0, 1, Pair(9, 5)),
            Triple(0, 1, Pair(10, 6)),
            Triple(0, 1, Pair(11, 7)),
            Triple(0, 1, Pair(12, 8)),
            Triple(0, 1, Pair(13, 9)),
            Triple(0, 1, Pair(14, 10)),
            Triple(0, 1, Pair(15, 11)),
            Triple(0, 1, Pair(16, 12)),
            Triple(0, 1, Pair(17, 13)),
            Triple(0, 1, Pair(18, 14)),
            Triple(0, 1, Pair(19, 15)),
            Triple(0, 1, Pair(24, 31)),
            Triple(1, 0, Pair(30, 23)),
            Triple(0, 1, Pair(26, 32)),
            Triple(1, 0, Pair(33, 27)),
            Triple(0, 1, Pair(28, 34)),
            Triple(1, 0, Pair(35, 29)),
            Triple(0, 1, Pair(30, 36)),
            Triple(1, 0, Pair(37, 31)),
            Triple(0, 1, Pair(32, 38)),
            Triple(1, 0, Pair(39, 33))
        )

        printSequenceAlignmentResult(3, messages, expectedAlignments)
    }



}