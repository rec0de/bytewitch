package ParserTests

import SequenceAlignment.AlignedSegment
import SequenceAlignment.NemesysSequenceAlignment
import bitmage.fromHex
import decoders.Nemesys.NemesysField
import decoders.Nemesys.NemesysParsedMessage
import decoders.Nemesys.NemesysSegment
import decoders.Nemesys.NemesysUtil
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.test.assertTrue

object TrainingMessageSamples {
    val testMessages = listOf(
        TestMessage(0,
            "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102437444431444343412d374330442d343145362d423337342d433133333935354443373634080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex(),
            listOf(
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
        ),
        TestMessage(1,
            "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102446394532423231352d393431372d344141372d413439302d384446364539443445364639080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex(),
            listOf(
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
        ),
        TestMessage(2,
            "A5626964187B68757365726E616D6565616C69636565656D61696C71616C696365406578616D706C652E636F6D6770726F66696C65A263616765181E67636F756E747279674765726D616E796969735F616374697665F5".fromHex(),
            listOf(
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
        ),
        TestMessage(3,
            "A56269640368757365726E616D6563626F6265656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E747279635553416969735F616374697665F4".fromHex(),
            listOf(
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
        ),
        TestMessage(4,
            "A76269640C67766F726E616D65634D6178686E6163686E616D656A4D75737465726D616E6E68757365726E616D65636D617865656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E7472796A4175737472616C69656E6969735F616374697665F4".fromHex(),
            listOf(
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
        ),
        TestMessage(5,
            "A76269641870686E6163686E616D65674E65756D616E6E68757365726E616D656A6E65756D616E6E78587865656D61696C726E65756D616E6E406F75746C6F6F6B2E646566686F62627973A266686F62627931684675C39F62616C6C66686F626279326A4261736B657462616C6C6770726F66696C65A2636167651267636F756E7472796B446575747363686C616E646969735F616374697665F5".fromHex(),
            listOf(
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
        ),
        TestMessage(6,
            "62706c6973743030d30102030405065173516651625c636f6d2e6b696b2e636861741105ffa107d208090a0b516851645f102036366137626435396665376639613763323265623436336436646233393730342341d95c3031cf51a2080f1113152225272c2e30530000000000000101000000000000000c0000000000000000000000000000005c".fromHex(),
            listOf(
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
        ),
        TestMessage(7,
            "62706c6973743030d401020304050c0d0e517252704751635165d3060708090a0b5f102341505350726f746f636f6c417070546f6b656e47656e6572617465526573706f6e73655f101241505350726f746f636f6c436f6d6d616e645f102c41505350726f746f636f6c417070546f6b656e47656e6572617465526573706f6e7365546f70696348617368100210124f1014998c5d4c6f0bb047fc827e799bb288562a6196425f102438424139413938422d393842462d344331332d414545332d453430463946433631433344100d5a70726f64756374696f6e08111316181a21475c8b8d8fa6cdcf0000000000000101000000000000000f000000000000000000000000000000da".fromHex(),
            listOf(
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
        ),
        TestMessage(8,
            "08171002".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),
                NemesysSegment(1, NemesysField.UNKNOWN),
                NemesysSegment(2, NemesysField.UNKNOWN),
                NemesysSegment(3, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(9,
            "fe4781820001000000000000037777770369666303636f6d0000010001".fromHex(),
            listOf(
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
        ),
        TestMessage(10,
            "19040aec0000027b000012850a6400c8d23d06a2535ed71ed23d09faa4673315d23d09faa1766325d23d09faa17b4b10".fromHex(),
            listOf(
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
        ),
        TestMessage(11,
            "62706c6973743030d4010203040506070c582476657273696f6e592461726368697665725424746f7058246f626a6563747312000186a05f100f4e534b657965644172636869766572d208090a0b52313152313080048001a70d0e13141b1f2055246e756c6cd20f1011125624636c6173735a6964656e746966696572800380025f102753697269427574746f6e4964656e7469666965724c6f6e675072657373486f6d65427574746f6ed2151617185a24636c6173736e616d655824636c61737365735f101c534153427574746f6e4964656e7469666965725472616e73706f7274a2191a5f101c534153427574746f6e4964656e7469666965725472616e73706f7274584e534f626a656374d20f1c1d1e597472616e73706f727480068005233fd999999999999ad2151621225f101853415354696d65496e74657276616c5472616e73706f7274a2231a5f101853415354696d65496e74657276616c5472616e73706f727400080011001a00240029003200370049004e005100540056005800600066006b0072007d007f008100ab00b000bb00c400e300e60105010e0113011d011f0121012a012f014a014d0000000000000201000000000000002400000000000000000000000000000168".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING), // bplist
                NemesysSegment(6, NemesysField.STRING), // version
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(107, NemesysField.UNKNOWN), // $class type
                NemesysSegment(108, NemesysField.STRING), // $class
                NemesysSegment(114, NemesysField.UNKNOWN),
                NemesysSegment(176, NemesysField.UNKNOWN), // $classname type
                NemesysSegment(177, NemesysField.STRING), // $classname
                NemesysSegment(187, NemesysField.UNKNOWN), // $classes type
                NemesysSegment(188, NemesysField.STRING), // $classes
                NemesysSegment(196, NemesysField.UNKNOWN),
                NemesysSegment(261, NemesysField.UNKNOWN), // NSObject type
                NemesysSegment(262, NemesysField.STRING), // NSObject
                NemesysSegment(270, NemesysField.UNKNOWN),
                NemesysSegment(275, NemesysField.UNKNOWN), // transport type
                NemesysSegment(276, NemesysField.STRING), // transport
                NemesysSegment(285, NemesysField.UNKNOWN),
                NemesysSegment(289, NemesysField.UNKNOWN), // 0.4
                NemesysSegment(298, NemesysField.UNKNOWN),
                NemesysSegment(303, NemesysField.UNKNOWN), // SASTimeIntervalTransport type
                NemesysSegment(305, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN), // SASTimeIntervalTransport length
                NemesysSegment(306, NemesysField.STRING), // SASTimeIntervalTransport
                NemesysSegment(330, NemesysField.UNKNOWN),
                NemesysSegment(333, NemesysField.UNKNOWN), // SASTimeIntervalTransport type
                NemesysSegment(335, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN), // SASTimeIntervalTransport length
                NemesysSegment(336, NemesysField.STRING), // SASTimeIntervalTransport
                NemesysSegment(360, NemesysField.UNKNOWN),
            )
        ),
        TestMessage(12,
            "62706c6973743137a09d000000000000007d6e6f746966794576656e743a007b76323440303a3840313600a09d00000000000000d09d000000000000007724636c617373007f11154157417474656e74696f6e4c6f73744576656e74007a74696d657374616d700023bb3f1656df6392407f1115617474656e74696f6e4c6f737454696d656f75740023000000000000000079746167496e646578001100".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING), // bplist
                NemesysSegment(6, NemesysField.STRING), // version
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.UNKNOWN), // notifyEvent: type
                NemesysSegment(18, NemesysField.STRING), // notifyEvent
                NemesysSegment(31, NemesysField.UNKNOWN), // v24@0:8@16 type
                NemesysSegment(32, NemesysField.STRING), // v24@0:8@16
                NemesysSegment(43, NemesysField.UNKNOWN),
                NemesysSegment(52, NemesysField.UNKNOWN), // array
                NemesysSegment(61, NemesysField.UNKNOWN), // $class type
                NemesysSegment(62, NemesysField.STRING), // $class
                NemesysSegment(69, NemesysField.UNKNOWN), // AWAttentionLostEvent type
                NemesysSegment(71, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN), // AWAttentionLostEvent length
                NemesysSegment(72, NemesysField.STRING), // AWAttentionLostEvent
                NemesysSegment(93, NemesysField.UNKNOWN), // timestamp type
                NemesysSegment(94, NemesysField.STRING), // timestamp
                NemesysSegment(104, NemesysField.UNKNOWN), // 1176.968101833
                NemesysSegment(113, NemesysField.UNKNOWN), // attentionLostTimeout type
                NemesysSegment(115, NemesysField.UNKNOWN), // attentionLostTimeout length
                NemesysSegment(116, NemesysField.STRING), // attentionLostTimeout
                NemesysSegment(137, NemesysField.UNKNOWN), // 0
                NemesysSegment(146, NemesysField.UNKNOWN), // tagIndex type
                NemesysSegment(147, NemesysField.STRING), // tagIndex
                NemesysSegment(156, NemesysField.UNKNOWN), // 0
            )
        ),
        TestMessage(13,
            "b90005626964181a68757365726e616d65656672616e7a65656d61696c746672616e7a2e6261756d616e6e40676d782e64656770726f66696c65b9000263616765183667636f756e7472796a4e65746865726c616e646969735f616374697665f4".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),   // Start des gesamten Objekts
                NemesysSegment(3, NemesysField.UNKNOWN),   // "id" type
                NemesysSegment(4, NemesysField.STRING),    // "id"
                NemesysSegment(6, NemesysField.UNKNOWN),   // 26
                NemesysSegment(8, NemesysField.UNKNOWN),   // "username" type
                NemesysSegment(9, NemesysField.STRING),    // "username"
                NemesysSegment(17, NemesysField.UNKNOWN),  // "franz" type
                NemesysSegment(18, NemesysField.STRING),   // "franz"
                NemesysSegment(23, NemesysField.UNKNOWN),  // "email" type
                NemesysSegment(24, NemesysField.STRING),   // "email"
                NemesysSegment(29, NemesysField.UNKNOWN),  // "franz.baumann@gmx.de" type
                NemesysSegment(30, NemesysField.STRING),   // "franz.baumann@gmx.de"
                NemesysSegment(50, NemesysField.UNKNOWN),  // "profile" type
                NemesysSegment(51, NemesysField.STRING),   // "profile"
                NemesysSegment(58, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
                NemesysSegment(61, NemesysField.UNKNOWN),  // "age" type
                NemesysSegment(62, NemesysField.STRING),   // "age"
                NemesysSegment(65, NemesysField.UNKNOWN),  // 54
                NemesysSegment(67, NemesysField.UNKNOWN),  // "country" type
                NemesysSegment(68, NemesysField.STRING),   // "country"
                NemesysSegment(75, NemesysField.UNKNOWN),  // "Netherland" type
                NemesysSegment(76, NemesysField.STRING),   // "Netherland"
                NemesysSegment(86, NemesysField.UNKNOWN),  // "is_active" type
                NemesysSegment(87, NemesysField.STRING),   // "is_active"
                NemesysSegment(96, NemesysField.UNKNOWN)   // false
            )
        ),
        TestMessage(14,
            "b90005626964183568757365726e616d65666e696b65333265656d61696c6e6e696b6540676d61696c2e636f6d6770726f66696c65b90002636167651567636f756e7472796a4e65746865726c616e646969735f616374697665f5".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),   // Start des gesamten Objekts
                NemesysSegment(3, NemesysField.UNKNOWN),   // "id" type
                NemesysSegment(4, NemesysField.STRING),    // "id"
                NemesysSegment(6, NemesysField.UNKNOWN),   // 53
                NemesysSegment(8, NemesysField.UNKNOWN),   // "username" type
                NemesysSegment(9, NemesysField.STRING),    // "username"
                NemesysSegment(17, NemesysField.UNKNOWN),  // "nike32" type
                NemesysSegment(18, NemesysField.STRING),   // "nike32"
                NemesysSegment(24, NemesysField.UNKNOWN),  // "email" type
                NemesysSegment(25, NemesysField.STRING),   // "email"
                NemesysSegment(30, NemesysField.UNKNOWN),  // "nike@gmail.com" type
                NemesysSegment(31, NemesysField.STRING),   // "nike@gmail.com"
                NemesysSegment(45, NemesysField.UNKNOWN),  // "profile" type
                NemesysSegment(46, NemesysField.STRING),   // "profile"
                NemesysSegment(53, NemesysField.UNKNOWN),  // verschachteltes Objekt beginnt
                NemesysSegment(56, NemesysField.UNKNOWN),  // "age" type
                NemesysSegment(57, NemesysField.STRING),   // "age"
                NemesysSegment(60, NemesysField.UNKNOWN),  // 21
                NemesysSegment(61, NemesysField.UNKNOWN),  // "country" type
                NemesysSegment(62, NemesysField.STRING),   // "country"
                NemesysSegment(69, NemesysField.UNKNOWN),  // "Netherland" type
                NemesysSegment(70, NemesysField.STRING),   // "Netherland"
                NemesysSegment(80, NemesysField.UNKNOWN),  // "is_active" type
                NemesysSegment(81, NemesysField.STRING),   // "is_active"
                NemesysSegment(90, NemesysField.UNKNOWN)   // true
            )
        ),
        TestMessage(15,
            "b9000562696419035568757365726e616d656c73696d6f6e6c65686d616e6e65656d61696c736c65686d616e6e393840676d61696c2e636f6d6770726f66696c65b9000263616765181a67636f756e7472796742656c6769756d6969735f616374697665f5".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),    // Start des Objekts
                NemesysSegment(3, NemesysField.UNKNOWN),    // "id" type
                NemesysSegment(4, NemesysField.STRING),     // "id"
                NemesysSegment(6, NemesysField.UNKNOWN),    // 853
                NemesysSegment(9, NemesysField.UNKNOWN),    // "username" type
                NemesysSegment(10, NemesysField.STRING),    // "username"
                NemesysSegment(18, NemesysField.UNKNOWN),   // "simonlehmann" type
                NemesysSegment(19, NemesysField.STRING),    // "simonlehmann"
                NemesysSegment(31, NemesysField.UNKNOWN),   // "email" type
                NemesysSegment(32, NemesysField.STRING),    // "email"
                NemesysSegment(37, NemesysField.UNKNOWN),   // "lehmann98@gmail.com" type
                NemesysSegment(38, NemesysField.STRING),    // "lehmann98@gmail.com"
                NemesysSegment(57, NemesysField.UNKNOWN),   // "profile" type
                NemesysSegment(58, NemesysField.STRING),    // "profile"
                NemesysSegment(65, NemesysField.UNKNOWN),   // verschachteltes Objekt beginnt
                NemesysSegment(68, NemesysField.UNKNOWN),   // "age" type
                NemesysSegment(69, NemesysField.STRING),    // "age"
                NemesysSegment(72, NemesysField.UNKNOWN),   // 26
                NemesysSegment(74, NemesysField.UNKNOWN),   // "country" type
                NemesysSegment(75, NemesysField.STRING),    // "country"
                NemesysSegment(82, NemesysField.UNKNOWN),   // "Belgium" type
                NemesysSegment(83, NemesysField.STRING),    // "Belgium"
                NemesysSegment(90, NemesysField.UNKNOWN),   // "is_active" type
                NemesysSegment(91, NemesysField.STRING),    // "is_active"
                NemesysSegment(100, NemesysField.UNKNOWN)   // true
            )
        ),
        TestMessage(16,
            "b9000562696419017d68757365726e616d656d776f6c6667616e674b61666b6165656d61696c6e4b61666b613132407765622e64656770726f66696c65b9000263616765182e67636f756e74727965496e6469616969735f616374697665f5".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),    // Start des Objekts
                NemesysSegment(3, NemesysField.UNKNOWN),    // "id" type
                NemesysSegment(4, NemesysField.STRING),     // "id"
                NemesysSegment(6, NemesysField.UNKNOWN),    // 381
                NemesysSegment(9, NemesysField.UNKNOWN),    // "username" type
                NemesysSegment(10, NemesysField.STRING),    // "username"
                NemesysSegment(18, NemesysField.UNKNOWN),   // "wolfgangKafka" type
                NemesysSegment(19, NemesysField.STRING),    // "wolfgangKafka"
                NemesysSegment(32, NemesysField.UNKNOWN),   // "email" type
                NemesysSegment(33, NemesysField.STRING),    // "email"
                NemesysSegment(38, NemesysField.UNKNOWN),   // "Kafka12@web.de" type
                NemesysSegment(39, NemesysField.STRING),    // "Kafka12@web.de"
                NemesysSegment(53, NemesysField.UNKNOWN),   // "profile" type
                NemesysSegment(54, NemesysField.STRING),    // "profile"
                NemesysSegment(61, NemesysField.UNKNOWN),   // verschachteltes Objekt beginnt
                NemesysSegment(64, NemesysField.UNKNOWN),   // "age" type
                NemesysSegment(65, NemesysField.STRING),    // "age"
                NemesysSegment(68, NemesysField.UNKNOWN),   // 46
                NemesysSegment(70, NemesysField.UNKNOWN),   // "country" type
                NemesysSegment(71, NemesysField.STRING),    // "country"
                NemesysSegment(78, NemesysField.UNKNOWN),   // "India" type
                NemesysSegment(79, NemesysField.STRING),    // "India"
                NemesysSegment(84, NemesysField.UNKNOWN),   // "is_active" type
                NemesysSegment(85, NemesysField.STRING),    // "is_active"
                NemesysSegment(94, NemesysField.UNKNOWN)    // true
            )
        ),
        TestMessage(17,
            "b900056269640268757365726e616d656872616c664f74746f65656d61696c6f4f74746f3231407961686f6f2e64656770726f66696c65b90002636167651567636f756e7472796553797269616969735f616374697665f4".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),    // Start des Objekts
                NemesysSegment(3, NemesysField.UNKNOWN),    // "id" type
                NemesysSegment(4, NemesysField.STRING),     // "id"
                NemesysSegment(6, NemesysField.UNKNOWN),    // 2
                NemesysSegment(7, NemesysField.UNKNOWN),    // "username" type
                NemesysSegment(8, NemesysField.STRING),     // "username"
                NemesysSegment(16, NemesysField.UNKNOWN),   // "ralfOtto" type
                NemesysSegment(17, NemesysField.STRING),    // "ralfOtto"
                NemesysSegment(25, NemesysField.UNKNOWN),   // "email" type
                NemesysSegment(26, NemesysField.STRING),    // "email"
                NemesysSegment(31, NemesysField.UNKNOWN),   // "Otto21@yahoo.de" type
                NemesysSegment(32, NemesysField.STRING),    // "Otto21@yahoo.de"
                NemesysSegment(47, NemesysField.UNKNOWN),   // "profile" type
                NemesysSegment(48, NemesysField.STRING),    // "profile"
                NemesysSegment(55, NemesysField.UNKNOWN),   // verschachteltes Objekt
                NemesysSegment(58, NemesysField.UNKNOWN),   // "age" type
                NemesysSegment(59, NemesysField.STRING),    // "age"
                NemesysSegment(62, NemesysField.UNKNOWN),   // 21
                NemesysSegment(63, NemesysField.UNKNOWN),   // "country" type
                NemesysSegment(64, NemesysField.STRING),    // "country"
                NemesysSegment(71, NemesysField.UNKNOWN),   // "Syria" type
                NemesysSegment(72, NemesysField.STRING),    // "Syria"
                NemesysSegment(77, NemesysField.UNKNOWN),   // "is_active" type
                NemesysSegment(78, NemesysField.STRING),    // "is_active"
                NemesysSegment(87, NemesysField.UNKNOWN)    // false
            )
        ),
        TestMessage(18,
            "62706c6973743030d4010203040506070851635165526c53527047100e5a70726f64756374696f6e11eff65f102438464532413931422d393341332d314333332d464544332d45413435393344373942454108111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),     // "bplist00"
                NemesysSegment(8, NemesysField.UNKNOWN),    // Beginn Dictionary
                NemesysSegment(17, NemesysField.STRING),    // "c"
                NemesysSegment(19, NemesysField.STRING),    // "e"
                NemesysSegment(21, NemesysField.STRING),    // "lS"
                NemesysSegment(24, NemesysField.STRING),    // "pG"
                NemesysSegment(27, NemesysField.UNKNOWN),   // 14 (für "c")
                NemesysSegment(29, NemesysField.STRING),    // "production" (für "e")
                NemesysSegment(40, NemesysField.STRING),    // 61430 (für "lS")
                NemesysSegment(43, NemesysField.STRING)     // UUID für "pG"
            )
        ),
        TestMessage(19,
            "62706c6973743030d4010203040506070851635165526c53527047101c5a70726f64756374696f6e11b6925f102441334245333743412d464133322d383436332d414633432d45333337434145453339463308111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),     // "bplist00"
                NemesysSegment(8, NemesysField.UNKNOWN),    // Beginn Dictionary
                NemesysSegment(17, NemesysField.STRING),    // "c"
                NemesysSegment(19, NemesysField.STRING),    // "e"
                NemesysSegment(21, NemesysField.STRING),    // "lS"
                NemesysSegment(24, NemesysField.STRING),    // "pG"
                NemesysSegment(27, NemesysField.UNKNOWN),   // 28 (für "c")
                NemesysSegment(29, NemesysField.STRING),    // "production" (für "e")
                NemesysSegment(40, NemesysField.STRING),    // 46738 (für "lS")
                NemesysSegment(43, NemesysField.STRING)     // UUID für "pG"
            )
        ),
        TestMessage(20,
            "62706c6973743030d4010203040506070851635165526c53527047101c5a70726f64756374696f6e11b6925f102441334333394634432d463734332d414344332d383337322d42324136413346373833444508111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING),
                NemesysSegment(19, NemesysField.STRING),
                NemesysSegment(21, NemesysField.STRING),
                NemesysSegment(24, NemesysField.STRING),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.STRING),
                NemesysSegment(40, NemesysField.STRING),
                NemesysSegment(43, NemesysField.STRING)
            )
        ),
        TestMessage(21,
            "62706c6973743030d4010203040506070851635165526c5352704710635a70726f64756374696f6e11d4315f102441424344454631322d333435362d373839302d414243442d45463132333435363738393008111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING),
                NemesysSegment(19, NemesysField.STRING),
                NemesysSegment(21, NemesysField.STRING),
                NemesysSegment(24, NemesysField.STRING),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.STRING),
                NemesysSegment(40, NemesysField.STRING),
                NemesysSegment(43, NemesysField.STRING)
            )
        ),
        TestMessage(22,
            "62706c6973743030d4010203040506070851635165526c5352704710255a70726f64756374696f6e11a8ca5f102442414444434146452d313131312d323232322d333333332d34343434353535353636363608111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING),
                NemesysSegment(19, NemesysField.STRING),
                NemesysSegment(21, NemesysField.STRING),
                NemesysSegment(24, NemesysField.STRING),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.STRING),
                NemesysSegment(40, NemesysField.STRING),
                NemesysSegment(43, NemesysField.STRING)
            )
        ),
        TestMessage(23,
            "62706c6973743030d4010203040506070851635165526c5352704710125a70726f64756374696f6e119a9e5f102443414645424142452d343332312d444342412d383736352d35363738353637383536373808111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING),
                NemesysSegment(19, NemesysField.STRING),
                NemesysSegment(21, NemesysField.STRING),
                NemesysSegment(24, NemesysField.STRING),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.STRING),
                NemesysSegment(40, NemesysField.STRING),
                NemesysSegment(43, NemesysField.STRING)
            )
        ),
        TestMessage(24,
            "62706c6973743030d4010203040506070851635165526c5352704710415a70726f64756374696f6e1180005f102430313233343536372d383941422d434445462d303132332d34353637383941424344454608111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING),
                NemesysSegment(19, NemesysField.STRING),
                NemesysSegment(21, NemesysField.STRING),
                NemesysSegment(24, NemesysField.STRING),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.STRING),
                NemesysSegment(40, NemesysField.STRING),
                NemesysSegment(43, NemesysField.STRING)
            )
        ),
        TestMessage(25,
            "62706c6973743030d4010203040506070851635165526c5352704710355a70726f64756374696f6e1130395f102446314532443343342d423541362d373839302d434445462d31333537394244463234363808111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING),
                NemesysSegment(19, NemesysField.STRING),
                NemesysSegment(21, NemesysField.STRING),
                NemesysSegment(24, NemesysField.STRING),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.STRING),
                NemesysSegment(40, NemesysField.STRING),
                NemesysSegment(43, NemesysField.STRING)
            )
        ),
        TestMessage(26,
            "62706c6973743030d4010203040506070851635165526c5352704710205a70726f64756374696f6e11eadb5f102431323334414243442d353637382d454639302d414243442d30303131323233333434353508111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING),
                NemesysSegment(19, NemesysField.STRING),
                NemesysSegment(21, NemesysField.STRING),
                NemesysSegment(24, NemesysField.STRING),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.STRING),
                NemesysSegment(40, NemesysField.STRING),
                NemesysSegment(43, NemesysField.STRING)
            )
        ),
        TestMessage(27,
            "62706c6973743030d4010203040506070851635165526c5352704710245a70726f64756374696f6e11b26e5f102442454546424545462d313031302d323032302d333033302d34303430353035303630373008111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING),
                NemesysSegment(19, NemesysField.STRING),
                NemesysSegment(21, NemesysField.STRING),
                NemesysSegment(24, NemesysField.STRING),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.STRING),
                NemesysSegment(40, NemesysField.STRING),
                NemesysSegment(43, NemesysField.STRING)
            )
        )
    )

    // group of messages with a similar structure
    val messageGroups = listOf(
        MessageGroup(0, testMessages.withIndex().filter { it.index in listOf(2, 3, 13, 14, 15, 16, 17) }.map { it.value }),
        MessageGroup(1, testMessages.withIndex().filter { it.index in listOf(18, 19, 20, 21, 22, 23, 24, 25, 26, 27) }.map { it.value })
    )

    // test sequence alignment
    val alignmentTests = listOf(
        SequenceAlignmentTest(
            messageAIndex = 0,
            messageBIndex = 1,
            expectedAlignments = setOf(
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
        ),
        SequenceAlignmentTest(
            messageAIndex = 2,
            messageBIndex = 3,
            expectedAlignments = setOf(
                Triple(2, 3, Pair(0, 0)),
                Triple(2, 3, Pair(1, 1)),
                Triple(2, 3, Pair(2, 2)),
                Triple(3, 2, Pair(3, 3)),
                Triple(2, 3, Pair(4, 4)),
                Triple(3, 2, Pair(5, 5)),
                Triple(2, 3, Pair(6, 6)),
                Triple(3, 2, Pair(7, 7)),
                Triple(2, 3, Pair(8, 8)),
                Triple(3, 2, Pair(9, 9)),
                Triple(2, 3, Pair(10, 10)),
                Triple(3, 2, Pair(11, 11)),
                Triple(2, 3, Pair(12, 12)),
                Triple(3, 2, Pair(13, 13)),
                Triple(2, 3, Pair(14, 14)),
                Triple(3, 2, Pair(15, 15)),
                Triple(2, 3, Pair(16, 16)),
                Triple(2, 3, Pair(17, 17)),
                Triple(2, 3, Pair(18, 18)),
                Triple(2, 3, Pair(19, 19)),
                Triple(3, 2, Pair(20, 20)),
                Triple(2, 3, Pair(21, 21)),
                Triple(3, 2, Pair(22, 22)),
                Triple(2, 3, Pair(23, 23)),
                Triple(3, 2, Pair(24, 24)),
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 4,
            messageBIndex = 5,
            expectedAlignments = setOf(
                Triple(4, 5, Pair(0, 0)),
                Triple(4, 5, Pair(1, 1)),
                Triple(4, 5, Pair(2, 2)),
                Triple(4, 5, Pair(3, 3)),
                Triple(4, 5, Pair(8, 4)),
                Triple(4, 5, Pair(9, 5)),
                Triple(4, 5, Pair(10, 6)),
                Triple(4, 5, Pair(11, 7)),
                Triple(4, 5, Pair(12, 8)),
                Triple(4, 5, Pair(13, 9)),
                Triple(4, 5, Pair(14, 10)),
                Triple(4, 5, Pair(15, 11)),
                Triple(4, 5, Pair(16, 12)),
                Triple(4, 5, Pair(17, 13)),
                Triple(4, 5, Pair(18, 14)),
                Triple(4, 5, Pair(19, 15)),
                Triple(4, 5, Pair(24, 31)),
                Triple(5, 4, Pair(30, 23)),
                Triple(4, 5, Pair(26, 32)),
                Triple(5, 4, Pair(33, 27)),
                Triple(4, 5, Pair(28, 34)),
                Triple(5, 4, Pair(35, 29)),
                Triple(4, 5, Pair(30, 36)),
                Triple(5, 4, Pair(37, 31)),
                Triple(4, 5, Pair(32, 38)),
                Triple(5, 4, Pair(39, 33))
            )
        )
    )
}