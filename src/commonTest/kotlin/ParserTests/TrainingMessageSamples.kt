package ParserTests

import bitmage.fromHex
import decoders.SwiftSegFinder.SSFField
import decoders.SwiftSegFinder.SSFSegment

object TrainingMessageSamples {
    val testMessages = listOf(
        TestMessage(0,
            "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102437444431444343412d374330442d343145362d423337342d433133333935354443373634080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(6, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(13, SSFField.UNKNOWN),
                SSFSegment(14, SSFField.STRING),
                SSFSegment(21, SSFField.UNKNOWN),
                SSFSegment(22, SSFField.STRING),
                SSFSegment(31, SSFField.UNKNOWN),
                SSFSegment(32, SSFField.UNKNOWN),
                SSFSegment(33, SSFField.UNKNOWN),
                SSFSegment(35, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN),
                SSFSegment(36, SSFField.STRING),
                SSFSegment(72, SSFField.UNKNOWN)
            )
        ),
        TestMessage(1,
            "62706c6973743030d20102030457636f6d6d616e6459756e697175652d6964100b5f102446394532423231352d393431372d344141372d413439302d384446364539443445364639080d151f210000000000000101000000000000000500000000000000000000000000000048".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(6, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(13, SSFField.UNKNOWN),
                SSFSegment(14, SSFField.STRING),
                SSFSegment(21, SSFField.UNKNOWN),
                SSFSegment(22, SSFField.STRING),
                SSFSegment(31, SSFField.UNKNOWN),
                SSFSegment(32, SSFField.UNKNOWN),
                SSFSegment(33, SSFField.UNKNOWN),
                SSFSegment(35, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN),
                SSFSegment(36, SSFField.STRING),
                SSFSegment(72, SSFField.UNKNOWN)
            )
        ),
        TestMessage(2,
            "A5626964187B68757365726E616D6565616C69636565656D61696C71616C696365406578616D706C652E636F6D6770726F66696C65A263616765181E67636F756E747279674765726D616E796969735F616374697665F5".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),   // Start des gesamten Objekts
                SSFSegment(1, SSFField.UNKNOWN),   // "id" type
                SSFSegment(2, SSFField.STRING),   // "id"
                SSFSegment(4, SSFField.UNKNOWN),   // 123
                SSFSegment(6, SSFField.UNKNOWN),   // "username" type
                SSFSegment(7, SSFField.STRING),   // "username"
                SSFSegment(15, SSFField.UNKNOWN),  // "alice" type
                SSFSegment(16, SSFField.STRING),  // "alice"
                SSFSegment(21, SSFField.UNKNOWN),  // "email" type
                SSFSegment(22, SSFField.STRING),  // "email"
                SSFSegment(27, SSFField.UNKNOWN),  // "alice@example.com" type
                SSFSegment(28, SSFField.STRING),  // "alice@example.com"
                SSFSegment(45, SSFField.UNKNOWN),  // "profile" type
                SSFSegment(46, SSFField.STRING),  // "profile"
                SSFSegment(53, SSFField.UNKNOWN),  // verschachteltes Objekt beginnt
                SSFSegment(54, SSFField.UNKNOWN),  // "age" type
                SSFSegment(55, SSFField.STRING),  // "age"
                SSFSegment(58, SSFField.UNKNOWN),  // 30
                SSFSegment(60, SSFField.UNKNOWN),  // "country" type
                SSFSegment(61, SSFField.STRING),  // "country"
                SSFSegment(68, SSFField.UNKNOWN),  // "Germany" type
                SSFSegment(69, SSFField.STRING),  // "Germany"
                SSFSegment(76, SSFField.UNKNOWN),  // "is_active" type
                SSFSegment(77, SSFField.STRING),  // "is_active"
                SSFSegment(86, SSFField.UNKNOWN)   // true
            )
        ),
        TestMessage(3,
            "A56269640368757365726E616D6563626F6265656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E747279635553416969735F616374697665F4".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),   // Start des gesamten Objekts
                SSFSegment(1, SSFField.UNKNOWN),   // "id" type
                SSFSegment(2, SSFField.STRING),   // "id"
                SSFSegment(4, SSFField.UNKNOWN),   // 3
                SSFSegment(5, SSFField.UNKNOWN),   // "username" type
                SSFSegment(6, SSFField.STRING),   // "username"
                SSFSegment(14, SSFField.UNKNOWN),  // "bob" type
                SSFSegment(15, SSFField.STRING),  // "bob"
                SSFSegment(18, SSFField.UNKNOWN),  // "email" type
                SSFSegment(19, SSFField.STRING),  // "email"
                SSFSegment(24, SSFField.UNKNOWN),  // "bob@gmx.de" type
                SSFSegment(25, SSFField.STRING),  // "bob@gmx.de"
                SSFSegment(35, SSFField.UNKNOWN),  // "profile" type
                SSFSegment(36, SSFField.STRING),  // "profile"
                SSFSegment(43, SSFField.UNKNOWN),  // verschachteltes Objekt beginnt
                SSFSegment(44, SSFField.UNKNOWN),  // "age" type
                SSFSegment(45, SSFField.STRING),  // "age"
                SSFSegment(48, SSFField.UNKNOWN),  // 76
                SSFSegment(50, SSFField.UNKNOWN),  // "country" type
                SSFSegment(51, SSFField.STRING),  // "country"
                SSFSegment(58, SSFField.UNKNOWN),  // "USA" type
                SSFSegment(59, SSFField.STRING),  // "USA"
                SSFSegment(62, SSFField.UNKNOWN),  // "is_active" type
                SSFSegment(63, SSFField.STRING),  // "is_active"
                SSFSegment(72, SSFField.UNKNOWN)   // false
            )
        ),
        TestMessage(4,
            "A76269640C67766F726E616D65634D6178686E6163686E616D656A4D75737465726D616E6E68757365726E616D65636D617865656D61696C6A626F6240676D782E64656770726F66696C65A263616765184C67636F756E7472796A4175737472616C69656E6969735F616374697665F4".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),     // Start des Objekts
                SSFSegment(1, SSFField.UNKNOWN),     // "id" type
                SSFSegment(2, SSFField.STRING),     // "id"
                SSFSegment(4, SSFField.UNKNOWN),     // 12
                SSFSegment(5, SSFField.UNKNOWN),     // "vorname" type
                SSFSegment(6, SSFField.STRING),     // "vorname"
                SSFSegment(13, SSFField.UNKNOWN),    // "Max" type
                SSFSegment(14, SSFField.STRING),    // "Max"
                SSFSegment(17, SSFField.UNKNOWN),    // "nachname" type
                SSFSegment(18, SSFField.STRING),    // "nachname"
                SSFSegment(26, SSFField.UNKNOWN),    // "Mustermann" type
                SSFSegment(27, SSFField.STRING),    // "Mustermann"
                SSFSegment(37, SSFField.UNKNOWN),    // "username" type
                SSFSegment(38, SSFField.STRING),    // "username"
                SSFSegment(46, SSFField.UNKNOWN),    // "max" type
                SSFSegment(47, SSFField.STRING),    // "max"
                SSFSegment(50, SSFField.UNKNOWN),    // "email" type
                SSFSegment(51, SSFField.STRING),    // "email"
                SSFSegment(56, SSFField.UNKNOWN),    // "bob@gmx.de" type
                SSFSegment(57, SSFField.STRING),    // "bob@gmx.de"
                SSFSegment(67, SSFField.UNKNOWN),    // "profile" type
                SSFSegment(68, SSFField.UNKNOWN),    // "profile"
                SSFSegment(75, SSFField.UNKNOWN),    // verschachteltes Objekt beginnt
                SSFSegment(76, SSFField.UNKNOWN),    // "age" type
                SSFSegment(77, SSFField.STRING),    // "age"
                SSFSegment(80, SSFField.UNKNOWN),    // 76 type
                SSFSegment(81, SSFField.UNKNOWN),    // 76
                SSFSegment(82, SSFField.UNKNOWN),    // "country" type
                SSFSegment(83, SSFField.STRING),    // "country"
                SSFSegment(90, SSFField.UNKNOWN),    // "Australien" type
                SSFSegment(91, SSFField.STRING),    // "Australien" STRING
                SSFSegment(101, SSFField.UNKNOWN),   // "is_active" type
                SSFSegment(102, SSFField.STRING),   // "is_active"
                SSFSegment(111, SSFField.UNKNOWN)    // false
            )
        ),
        TestMessage(5,
            "A76269641870686E6163686E616D65674E65756D616E6E68757365726E616D656A6E65756D616E6E78587865656D61696C726E65756D616E6E406F75746C6F6F6B2E646566686F62627973A266686F62627931684675C39F62616C6C66686F626279326A4261736B657462616C6C6770726F66696C65A2636167651267636F756E7472796B446575747363686C616E646969735F616374697665F5".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),     // Start des Objekts
                SSFSegment(1, SSFField.UNKNOWN),     // "id" type
                SSFSegment(2, SSFField.STRING),     // "id"
                SSFSegment(4, SSFField.UNKNOWN),     // 112
                SSFSegment(6, SSFField.UNKNOWN),     // "nachname" type
                SSFSegment(7, SSFField.STRING),     // "nachname"
                SSFSegment(15, SSFField.UNKNOWN),    // "Neumann" type
                SSFSegment(16, SSFField.STRING),    // "Neumann"
                SSFSegment(23, SSFField.UNKNOWN),    // "username" type
                SSFSegment(24, SSFField.STRING),    // "username"
                SSFSegment(32, SSFField.UNKNOWN),    // "neumannxXx" type
                SSFSegment(33, SSFField.STRING),    // "neumannxXx"
                SSFSegment(43, SSFField.UNKNOWN),    // "email" type
                SSFSegment(44, SSFField.STRING),    // "email"
                SSFSegment(49, SSFField.UNKNOWN),    // "neumann@outlook.de" type
                SSFSegment(50, SSFField.STRING),    // "neumann@outlook.de"
                SSFSegment(68, SSFField.UNKNOWN),    // "hobbys" type
                SSFSegment(69, SSFField.STRING),    // "hobbys"
                SSFSegment(75, SSFField.UNKNOWN),    // verschachteltes "hobbys"-Objekt
                SSFSegment(76, SSFField.UNKNOWN),    // "hobby1" type
                SSFSegment(77, SSFField.STRING),    // "hobby1"
                SSFSegment(83, SSFField.UNKNOWN),    // "Fußball" type
                SSFSegment(84, SSFField.STRING),    // "Fußball"
                SSFSegment(92, SSFField.UNKNOWN),    // "hobby2" type
                SSFSegment(93, SSFField.STRING),    // "hobby2"
                SSFSegment(99, SSFField.UNKNOWN),    // "Basketball" type
                SSFSegment(100, SSFField.STRING),    // "Basketball"
                SSFSegment(110, SSFField.UNKNOWN),   // "profile" type
                SSFSegment(111, SSFField.STRING),   // "profile"
                SSFSegment(118, SSFField.UNKNOWN),   // verschachteltes "profile"-Objekt
                SSFSegment(119, SSFField.UNKNOWN),   // "age" type
                SSFSegment(120, SSFField.STRING),   // "age"
                SSFSegment(123, SSFField.UNKNOWN),   // 18
                SSFSegment(124, SSFField.UNKNOWN),   // "country" type
                SSFSegment(125, SSFField.STRING),   // "country"
                SSFSegment(132, SSFField.UNKNOWN),   // "Deutschland" type
                SSFSegment(133, SSFField.STRING),   // "Deutschland"
                SSFSegment(144, SSFField.UNKNOWN),   // "is_active" type
                SSFSegment(145, SSFField.STRING),   // "is_active"
                SSFSegment(154, SSFField.UNKNOWN)    // true
            )
        ),
        TestMessage(6,
            "62706c6973743030d30102030405065173516651625c636f6d2e6b696b2e636861741105ffa107d208090a0b516851645f102036366137626435396665376639613763323265623436336436646233393730342341d95c3031cf51a2080f1113152225272c2e30530000000000000101000000000000000c0000000000000000000000000000005c".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),     // BPList
                SSFSegment(6, SSFField.STRING),     // BPList version
                SSFSegment(8, SSFField.UNKNOWN),     // Start des Root-Objekts (Dictionary)
                SSFSegment(15, SSFField.UNKNOWN),    // "s" type
                SSFSegment(16, SSFField.STRING),    // "s"
                SSFSegment(17, SSFField.UNKNOWN),    // "f" type
                SSFSegment(18, SSFField.STRING),    // "f"
                SSFSegment(19, SSFField.UNKNOWN),    // "b" type
                SSFSegment(20, SSFField.STRING),    // "b"
                SSFSegment(21, SSFField.UNKNOWN),    // "com.kik.chat" type
                SSFSegment(22, SSFField.STRING),    // "com.kik.chat"
                SSFSegment(34, SSFField.UNKNOWN),    // 1535
                SSFSegment(37, SSFField.UNKNOWN),    // Start des Arrays
                SSFSegment(39, SSFField.UNKNOWN),    // Start des Dictionary-Objekts im Array
                SSFSegment(44, SSFField.UNKNOWN),    // "h" type
                SSFSegment(45, SSFField.STRING),    // "h"
                SSFSegment(46, SSFField.UNKNOWN),    // "d" type
                SSFSegment(47, SSFField.STRING),    // "d"
                SSFSegment(48, SSFField.UNKNOWN),    // "66a7bd59fe7f9a7c22eb463d6db39704" type
                SSFSegment(50, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN),    // "66a7bd59fe7f9a7c22eb463d6db39704" length
                SSFSegment(51, SSFField.STRING),    // "66a7bd59fe7f9a7c22eb463d6db39704"
                SSFSegment(83, SSFField.UNKNOWN),
            )
        ),
        TestMessage(7,
            "62706c6973743030d401020304050c0d0e517252704751635165d3060708090a0b5f102341505350726f746f636f6c417070546f6b656e47656e6572617465526573706f6e73655f101241505350726f746f636f6c436f6d6d616e645f102c41505350726f746f636f6c417070546f6b656e47656e6572617465526573706f6e7365546f70696348617368100210124f1014998c5d4c6f0bb047fc827e799bb288562a6196425f102438424139413938422d393842462d344331332d414545332d453430463946433631433344100d5a70726f64756374696f6e08111316181a21475c8b8d8fa6cdcf0000000000000101000000000000000f000000000000000000000000000000da".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),        // BPList Header (0–6)
                SSFSegment(6, SSFField.UNKNOWN),        // BPList version
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.UNKNOWN),       // Key: "r" type
                SSFSegment(18, SSFField.STRING),       // Key: "r" text
                SSFSegment(19, SSFField.UNKNOWN),       // Key: "pG" type
                SSFSegment(20, SSFField.STRING),       // Key: "pG" tet
                SSFSegment(22, SSFField.UNKNOWN),       // Key: "c" type
                SSFSegment(23, SSFField.STRING),       // Key: "c" text
                SSFSegment(24, SSFField.UNKNOWN),       // Key: "e" type
                SSFSegment(25, SSFField.STRING),       // Key: "e" text
                SSFSegment(26, SSFField.UNKNOWN),       // Value: dict (nested)
                SSFSegment(33, SSFField.UNKNOWN),       // Key: "APSProtocolAppTokenGenerateResponse" type
                SSFSegment(35, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN),       // "APSProtocolAppTokenGenerateResponse" length
                SSFSegment(36, SSFField.STRING),       // "APSProtocolAppTokenGenerateResponse" text
                SSFSegment(71, SSFField.UNKNOWN),       // Key: "APSProtocolCommand" type
                SSFSegment(73, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN),       // Key: "APSProtocolCommand" length
                SSFSegment(74, SSFField.STRING),       // Key: "APSProtocolCommand" text
                SSFSegment(92, SSFField.UNKNOWN),       // Key: "APSProtocolAppTokenGenerateResponseTopicHash" type
                SSFSegment(94, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN),       // Key: "APSProtocolAppTokenGenerateResponseTopicHash" length
                SSFSegment(95, SSFField.STRING),       // Key: "APSProtocolAppTokenGenerateResponseTopicHash" text
                SSFSegment(139, SSFField.UNKNOWN),      // Value: Int(2)
                SSFSegment(141, SSFField.UNKNOWN),      // Value: Int(18)
                SSFSegment(143, SSFField.UNKNOWN),
                SSFSegment(146, SSFField.UNKNOWN),      // Value: Data 998c…642 (24 Bytes)
                SSFSegment(166, SSFField.UNKNOWN),      // Value: UUID-String
                SSFSegment(205, SSFField.UNKNOWN),      // Value: Int(13)
                SSFSegment(207, SSFField.UNKNOWN),      // Value: "production"
                SSFSegment(208, SSFField.STRING),      // Value: "production" text
                SSFSegment(218, SSFField.UNKNOWN)       // end
            )
        ),
        TestMessage(8,
            "08171002".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),
                SSFSegment(1, SSFField.UNKNOWN),
                SSFSegment(2, SSFField.UNKNOWN),
                SSFSegment(3, SSFField.UNKNOWN)
            )
        ),
        TestMessage(9,
            "fe4781820001000000000000037777770369666303636f6d0000010001".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),
                SSFSegment(2, SSFField.UNKNOWN),
                SSFSegment(4, SSFField.UNKNOWN),
                SSFSegment(6, SSFField.UNKNOWN),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(10, SSFField.UNKNOWN),
                SSFSegment(12, SSFField.UNKNOWN),
                SSFSegment(25, SSFField.UNKNOWN),
                SSFSegment(27, SSFField.UNKNOWN)
            )
        ),
        TestMessage(10,
            "19040aec0000027b000012850a6400c8d23d06a2535ed71ed23d09faa4673315d23d09faa1766325d23d09faa17b4b10".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),
                SSFSegment(2, SSFField.UNKNOWN),
                SSFSegment(3, SSFField.UNKNOWN),
                SSFSegment(4, SSFField.UNKNOWN),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(12, SSFField.UNKNOWN),
                SSFSegment(16, SSFField.UNKNOWN),
                SSFSegment(24, SSFField.UNKNOWN),
                SSFSegment(32, SSFField.UNKNOWN),
                SSFSegment(40, SSFField.UNKNOWN)
            )
        ),
        TestMessage(11,
            "62706c6973743030d4010203040506070c582476657273696f6e592461726368697665725424746f7058246f626a6563747312000186a05f100f4e534b657965644172636869766572d208090a0b52313152313080048001a70d0e13141b1f2055246e756c6cd20f1011125624636c6173735a6964656e746966696572800380025f102753697269427574746f6e4964656e7469666965724c6f6e675072657373486f6d65427574746f6ed2151617185a24636c6173736e616d655824636c61737365735f101c534153427574746f6e4964656e7469666965725472616e73706f7274a2191a5f101c534153427574746f6e4964656e7469666965725472616e73706f7274584e534f626a656374d20f1c1d1e597472616e73706f727480068005233fd999999999999ad2151621225f101853415354696d65496e74657276616c5472616e73706f7274a2231a5f101853415354696d65496e74657276616c5472616e73706f727400080011001a00240029003200370049004e005100540056005800600066006b0072007d007f008100ab00b000bb00c400e300e60105010e0113011d011f0121012a012f014a014d0000000000000201000000000000002400000000000000000000000000000168".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING), // bplist
                SSFSegment(6, SSFField.STRING), // version
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(107, SSFField.UNKNOWN), // $class type
                SSFSegment(108, SSFField.STRING), // $class
                SSFSegment(114, SSFField.UNKNOWN),
                SSFSegment(176, SSFField.UNKNOWN), // $classname type
                SSFSegment(177, SSFField.STRING), // $classname
                SSFSegment(187, SSFField.UNKNOWN), // $classes type
                SSFSegment(188, SSFField.STRING), // $classes
                SSFSegment(196, SSFField.UNKNOWN),
                SSFSegment(261, SSFField.UNKNOWN), // NSObject type
                SSFSegment(262, SSFField.STRING), // NSObject
                SSFSegment(270, SSFField.UNKNOWN),
                SSFSegment(275, SSFField.UNKNOWN), // transport type
                SSFSegment(276, SSFField.STRING), // transport
                SSFSegment(285, SSFField.UNKNOWN),
                SSFSegment(289, SSFField.UNKNOWN), // 0.4
                SSFSegment(298, SSFField.UNKNOWN),
                SSFSegment(303, SSFField.UNKNOWN), // SASTimeIntervalTransport type
                SSFSegment(305, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN), // SASTimeIntervalTransport length
                SSFSegment(306, SSFField.STRING), // SASTimeIntervalTransport
                SSFSegment(330, SSFField.UNKNOWN),
                SSFSegment(333, SSFField.UNKNOWN), // SASTimeIntervalTransport type
                SSFSegment(335, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN), // SASTimeIntervalTransport length
                SSFSegment(336, SSFField.STRING), // SASTimeIntervalTransport
                SSFSegment(360, SSFField.UNKNOWN),
            )
        ),
        TestMessage(12,
            "62706c6973743137a09d000000000000007d6e6f746966794576656e743a007b76323440303a3840313600a09d00000000000000d09d000000000000007724636c617373007f11154157417474656e74696f6e4c6f73744576656e74007a74696d657374616d700023bb3f1656df6392407f1115617474656e74696f6e4c6f737454696d656f75740023000000000000000079746167496e646578001100".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING), // bplist
                SSFSegment(6, SSFField.STRING), // version
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.UNKNOWN), // notifyEvent: type
                SSFSegment(18, SSFField.STRING), // notifyEvent
                SSFSegment(31, SSFField.UNKNOWN), // v24@0:8@16 type
                SSFSegment(32, SSFField.STRING), // v24@0:8@16
                SSFSegment(43, SSFField.UNKNOWN),
                SSFSegment(52, SSFField.UNKNOWN), // array
                SSFSegment(61, SSFField.UNKNOWN), // $class type
                SSFSegment(62, SSFField.STRING), // $class
                SSFSegment(69, SSFField.UNKNOWN), // AWAttentionLostEvent type
                SSFSegment(71, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN), // AWAttentionLostEvent length
                SSFSegment(72, SSFField.STRING), // AWAttentionLostEvent
                SSFSegment(93, SSFField.UNKNOWN), // timestamp type
                SSFSegment(94, SSFField.STRING), // timestamp
                SSFSegment(104, SSFField.UNKNOWN), // 1176.968101833
                SSFSegment(113, SSFField.UNKNOWN), // attentionLostTimeout type
                SSFSegment(115, SSFField.UNKNOWN), // attentionLostTimeout length
                SSFSegment(116, SSFField.STRING), // attentionLostTimeout
                SSFSegment(137, SSFField.UNKNOWN), // 0
                SSFSegment(146, SSFField.UNKNOWN), // tagIndex type
                SSFSegment(147, SSFField.STRING), // tagIndex
                SSFSegment(156, SSFField.UNKNOWN), // 0
            )
        ),
        TestMessage(13,
            "b90005626964181a68757365726e616d65656672616e7a65656d61696c746672616e7a2e6261756d616e6e40676d782e64656770726f66696c65b9000263616765183667636f756e7472796a4e65746865726c616e646969735f616374697665f4".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),   // Start des gesamten Objekts
                SSFSegment(3, SSFField.UNKNOWN),   // "id" type
                SSFSegment(4, SSFField.STRING),    // "id"
                SSFSegment(6, SSFField.UNKNOWN),   // 26
                SSFSegment(8, SSFField.UNKNOWN),   // "username" type
                SSFSegment(9, SSFField.STRING),    // "username"
                SSFSegment(17, SSFField.UNKNOWN),  // "franz" type
                SSFSegment(18, SSFField.STRING),   // "franz"
                SSFSegment(23, SSFField.UNKNOWN),  // "email" type
                SSFSegment(24, SSFField.STRING),   // "email"
                SSFSegment(29, SSFField.UNKNOWN),  // "franz.baumann@gmx.de" type
                SSFSegment(30, SSFField.STRING),   // "franz.baumann@gmx.de"
                SSFSegment(50, SSFField.UNKNOWN),  // "profile" type
                SSFSegment(51, SSFField.STRING),   // "profile"
                SSFSegment(58, SSFField.UNKNOWN),  // verschachteltes Objekt beginnt
                SSFSegment(61, SSFField.UNKNOWN),  // "age" type
                SSFSegment(62, SSFField.STRING),   // "age"
                SSFSegment(65, SSFField.UNKNOWN),  // 54
                SSFSegment(67, SSFField.UNKNOWN),  // "country" type
                SSFSegment(68, SSFField.STRING),   // "country"
                SSFSegment(75, SSFField.UNKNOWN),  // "Netherland" type
                SSFSegment(76, SSFField.STRING),   // "Netherland"
                SSFSegment(86, SSFField.UNKNOWN),  // "is_active" type
                SSFSegment(87, SSFField.STRING),   // "is_active"
                SSFSegment(96, SSFField.UNKNOWN)   // false
            )
        ),
        TestMessage(14,
            "b90005626964183568757365726e616d65666e696b65333265656d61696c6e6e696b6540676d61696c2e636f6d6770726f66696c65b90002636167651567636f756e7472796a4e65746865726c616e646969735f616374697665f5".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),   // Start des gesamten Objekts
                SSFSegment(3, SSFField.UNKNOWN),   // "id" type
                SSFSegment(4, SSFField.STRING),    // "id"
                SSFSegment(6, SSFField.UNKNOWN),   // 53
                SSFSegment(8, SSFField.UNKNOWN),   // "username" type
                SSFSegment(9, SSFField.STRING),    // "username"
                SSFSegment(17, SSFField.UNKNOWN),  // "nike32" type
                SSFSegment(18, SSFField.STRING),   // "nike32"
                SSFSegment(24, SSFField.UNKNOWN),  // "email" type
                SSFSegment(25, SSFField.STRING),   // "email"
                SSFSegment(30, SSFField.UNKNOWN),  // "nike@gmail.com" type
                SSFSegment(31, SSFField.STRING),   // "nike@gmail.com"
                SSFSegment(45, SSFField.UNKNOWN),  // "profile" type
                SSFSegment(46, SSFField.STRING),   // "profile"
                SSFSegment(53, SSFField.UNKNOWN),  // verschachteltes Objekt beginnt
                SSFSegment(56, SSFField.UNKNOWN),  // "age" type
                SSFSegment(57, SSFField.STRING),   // "age"
                SSFSegment(60, SSFField.UNKNOWN),  // 21
                SSFSegment(61, SSFField.UNKNOWN),  // "country" type
                SSFSegment(62, SSFField.STRING),   // "country"
                SSFSegment(69, SSFField.UNKNOWN),  // "Netherland" type
                SSFSegment(70, SSFField.STRING),   // "Netherland"
                SSFSegment(80, SSFField.UNKNOWN),  // "is_active" type
                SSFSegment(81, SSFField.STRING),   // "is_active"
                SSFSegment(90, SSFField.UNKNOWN)   // true
            )
        ),
        TestMessage(15,
            "b9000562696419035568757365726e616d656c73696d6f6e6c65686d616e6e65656d61696c736c65686d616e6e393840676d61696c2e636f6d6770726f66696c65b9000263616765181a67636f756e7472796742656c6769756d6969735f616374697665f5".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),    // Start des Objekts
                SSFSegment(3, SSFField.UNKNOWN),    // "id" type
                SSFSegment(4, SSFField.STRING),     // "id"
                SSFSegment(6, SSFField.UNKNOWN),    // 853
                SSFSegment(9, SSFField.UNKNOWN),    // "username" type
                SSFSegment(10, SSFField.STRING),    // "username"
                SSFSegment(18, SSFField.UNKNOWN),   // "simonlehmann" type
                SSFSegment(19, SSFField.STRING),    // "simonlehmann"
                SSFSegment(31, SSFField.UNKNOWN),   // "email" type
                SSFSegment(32, SSFField.STRING),    // "email"
                SSFSegment(37, SSFField.UNKNOWN),   // "lehmann98@gmail.com" type
                SSFSegment(38, SSFField.STRING),    // "lehmann98@gmail.com"
                SSFSegment(57, SSFField.UNKNOWN),   // "profile" type
                SSFSegment(58, SSFField.STRING),    // "profile"
                SSFSegment(65, SSFField.UNKNOWN),   // verschachteltes Objekt beginnt
                SSFSegment(68, SSFField.UNKNOWN),   // "age" type
                SSFSegment(69, SSFField.STRING),    // "age"
                SSFSegment(72, SSFField.UNKNOWN),   // 26
                SSFSegment(74, SSFField.UNKNOWN),   // "country" type
                SSFSegment(75, SSFField.STRING),    // "country"
                SSFSegment(82, SSFField.UNKNOWN),   // "Belgium" type
                SSFSegment(83, SSFField.STRING),    // "Belgium"
                SSFSegment(90, SSFField.UNKNOWN),   // "is_active" type
                SSFSegment(91, SSFField.STRING),    // "is_active"
                SSFSegment(100, SSFField.UNKNOWN)   // true
            )
        ),
        TestMessage(16,
            "b9000562696419017d68757365726e616d656d776f6c6667616e674b61666b6165656d61696c6e4b61666b613132407765622e64656770726f66696c65b9000263616765182e67636f756e74727965496e6469616969735f616374697665f5".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),    // Start des Objekts
                SSFSegment(3, SSFField.UNKNOWN),    // "id" type
                SSFSegment(4, SSFField.STRING),     // "id"
                SSFSegment(6, SSFField.UNKNOWN),    // 381
                SSFSegment(9, SSFField.UNKNOWN),    // "username" type
                SSFSegment(10, SSFField.STRING),    // "username"
                SSFSegment(18, SSFField.UNKNOWN),   // "wolfgangKafka" type
                SSFSegment(19, SSFField.STRING),    // "wolfgangKafka"
                SSFSegment(32, SSFField.UNKNOWN),   // "email" type
                SSFSegment(33, SSFField.STRING),    // "email"
                SSFSegment(38, SSFField.UNKNOWN),   // "Kafka12@web.de" type
                SSFSegment(39, SSFField.STRING),    // "Kafka12@web.de"
                SSFSegment(53, SSFField.UNKNOWN),   // "profile" type
                SSFSegment(54, SSFField.STRING),    // "profile"
                SSFSegment(61, SSFField.UNKNOWN),   // verschachteltes Objekt beginnt
                SSFSegment(64, SSFField.UNKNOWN),   // "age" type
                SSFSegment(65, SSFField.STRING),    // "age"
                SSFSegment(68, SSFField.UNKNOWN),   // 46
                SSFSegment(70, SSFField.UNKNOWN),   // "country" type
                SSFSegment(71, SSFField.STRING),    // "country"
                SSFSegment(78, SSFField.UNKNOWN),   // "India" type
                SSFSegment(79, SSFField.STRING),    // "India"
                SSFSegment(84, SSFField.UNKNOWN),   // "is_active" type
                SSFSegment(85, SSFField.STRING),    // "is_active"
                SSFSegment(94, SSFField.UNKNOWN)    // true
            )
        ),
        TestMessage(17,
            "b900056269640268757365726e616d656872616c664f74746f65656d61696c6f4f74746f3231407961686f6f2e64656770726f66696c65b90002636167651567636f756e7472796553797269616969735f616374697665f4".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),    // Start des Objekts
                SSFSegment(3, SSFField.UNKNOWN),    // "id" type
                SSFSegment(4, SSFField.STRING),     // "id"
                SSFSegment(6, SSFField.UNKNOWN),    // 2
                SSFSegment(7, SSFField.UNKNOWN),    // "username" type
                SSFSegment(8, SSFField.STRING),     // "username"
                SSFSegment(16, SSFField.UNKNOWN),   // "ralfOtto" type
                SSFSegment(17, SSFField.STRING),    // "ralfOtto"
                SSFSegment(25, SSFField.UNKNOWN),   // "email" type
                SSFSegment(26, SSFField.STRING),    // "email"
                SSFSegment(31, SSFField.UNKNOWN),   // "Otto21@yahoo.de" type
                SSFSegment(32, SSFField.STRING),    // "Otto21@yahoo.de"
                SSFSegment(47, SSFField.UNKNOWN),   // "profile" type
                SSFSegment(48, SSFField.STRING),    // "profile"
                SSFSegment(55, SSFField.UNKNOWN),   // verschachteltes Objekt
                SSFSegment(58, SSFField.UNKNOWN),   // "age" type
                SSFSegment(59, SSFField.STRING),    // "age"
                SSFSegment(62, SSFField.UNKNOWN),   // 21
                SSFSegment(63, SSFField.UNKNOWN),   // "country" type
                SSFSegment(64, SSFField.STRING),    // "country"
                SSFSegment(71, SSFField.UNKNOWN),   // "Syria" type
                SSFSegment(72, SSFField.STRING),    // "Syria"
                SSFSegment(77, SSFField.UNKNOWN),   // "is_active" type
                SSFSegment(78, SSFField.STRING),    // "is_active"
                SSFSegment(87, SSFField.UNKNOWN)    // false
            )
        ),
        TestMessage(18,
            "62706c6973743030d4010203040506070851635165526c53527047100e5a70726f64756374696f6e11eff65f102438464532413931422d393341332d314333332d464544332d45413435393344373942454108111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),     // "bplist00"
                SSFSegment(8, SSFField.UNKNOWN),    // Beginn Dictionary
                SSFSegment(17, SSFField.STRING),    // "c"
                SSFSegment(19, SSFField.STRING),    // "e"
                SSFSegment(21, SSFField.STRING),    // "lS"
                SSFSegment(24, SSFField.STRING),    // "pG"
                SSFSegment(27, SSFField.UNKNOWN),   // 14 (für "c")
                SSFSegment(29, SSFField.STRING),    // "production" (für "e")
                SSFSegment(40, SSFField.STRING),    // 61430 (für "lS")
                SSFSegment(43, SSFField.STRING)     // UUID für "pG"
            )
        ),
        TestMessage(19,
            "62706c6973743030d4010203040506070851635165526c53527047101c5a70726f64756374696f6e11b6925f102441334245333743412d464133322d383436332d414633432d45333337434145453339463308111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),     // "bplist00"
                SSFSegment(8, SSFField.UNKNOWN),    // Beginn Dictionary
                SSFSegment(17, SSFField.STRING),    // "c"
                SSFSegment(19, SSFField.STRING),    // "e"
                SSFSegment(21, SSFField.STRING),    // "lS"
                SSFSegment(24, SSFField.STRING),    // "pG"
                SSFSegment(27, SSFField.UNKNOWN),   // 28 (für "c")
                SSFSegment(29, SSFField.STRING),    // "production" (für "e")
                SSFSegment(40, SSFField.STRING),    // 46738 (für "lS")
                SSFSegment(43, SSFField.STRING)     // UUID für "pG"
            )
        ),
        TestMessage(20,
            "62706c6973743030d4010203040506070851635165526c53527047101c5a70726f64756374696f6e11b6925f102441334333394634432d463734332d414344332d383337322d42324136413346373833444508111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING),
                SSFSegment(19, SSFField.STRING),
                SSFSegment(21, SSFField.STRING),
                SSFSegment(24, SSFField.STRING),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.STRING),
                SSFSegment(40, SSFField.STRING),
                SSFSegment(43, SSFField.STRING)
            )
        ),
        TestMessage(21,
            "62706c6973743030d4010203040506070851635165526c5352704710635a70726f64756374696f6e11d4315f102441424344454631322d333435362d373839302d414243442d45463132333435363738393008111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING),
                SSFSegment(19, SSFField.STRING),
                SSFSegment(21, SSFField.STRING),
                SSFSegment(24, SSFField.STRING),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.STRING),
                SSFSegment(40, SSFField.STRING),
                SSFSegment(43, SSFField.STRING)
            )
        ),
        TestMessage(22,
            "62706c6973743030d4010203040506070851635165526c5352704710255a70726f64756374696f6e11a8ca5f102442414444434146452d313131312d323232322d333333332d34343434353535353636363608111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING),
                SSFSegment(19, SSFField.STRING),
                SSFSegment(21, SSFField.STRING),
                SSFSegment(24, SSFField.STRING),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.STRING),
                SSFSegment(40, SSFField.STRING),
                SSFSegment(43, SSFField.STRING)
            )
        ),
        TestMessage(23,
            "62706c6973743030d4010203040506070851635165526c5352704710125a70726f64756374696f6e119a9e5f102443414645424142452d343332312d444342412d383736352d35363738353637383536373808111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING),
                SSFSegment(19, SSFField.STRING),
                SSFSegment(21, SSFField.STRING),
                SSFSegment(24, SSFField.STRING),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.STRING),
                SSFSegment(40, SSFField.STRING),
                SSFSegment(43, SSFField.STRING)
            )
        ),
        TestMessage(24,
            "62706c6973743030d4010203040506070851635165526c5352704710415a70726f64756374696f6e1180005f102430313233343536372d383941422d434445462d303132332d34353637383941424344454608111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING),
                SSFSegment(19, SSFField.STRING),
                SSFSegment(21, SSFField.STRING),
                SSFSegment(24, SSFField.STRING),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.STRING),
                SSFSegment(40, SSFField.STRING),
                SSFSegment(43, SSFField.STRING)
            )
        ),
        TestMessage(25,
            "62706c6973743030d4010203040506070851635165526c5352704710355a70726f64756374696f6e1130395f102446314532443343342d423541362d373839302d434445462d31333537394244463234363808111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING),
                SSFSegment(19, SSFField.STRING),
                SSFSegment(21, SSFField.STRING),
                SSFSegment(24, SSFField.STRING),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.STRING),
                SSFSegment(40, SSFField.STRING),
                SSFSegment(43, SSFField.STRING)
            )
        ),
        TestMessage(26,
            "62706c6973743030d4010203040506070851635165526c5352704710205a70726f64756374696f6e11eadb5f102431323334414243442d353637382d454639302d414243442d30303131323233333434353508111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING),
                SSFSegment(19, SSFField.STRING),
                SSFSegment(21, SSFField.STRING),
                SSFSegment(24, SSFField.STRING),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.STRING),
                SSFSegment(40, SSFField.STRING),
                SSFSegment(43, SSFField.STRING)
            )
        ),
        TestMessage(27,
            "62706c6973743030d4010203040506070851635165526c5352704710245a70726f64756374696f6e11b26e5f102442454546424545462d313031302d323032302d333033302d34303430353035303630373008111315181b1d282b0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING),
                SSFSegment(19, SSFField.STRING),
                SSFSegment(21, SSFField.STRING),
                SSFSegment(24, SSFField.STRING),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.STRING),
                SSFSegment(40, SSFField.STRING),
                SSFSegment(43, SSFField.STRING)
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