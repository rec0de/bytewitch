package ParserTests

import bitmage.fromHex
import decoders.Nemesys.NemesysField
import decoders.Nemesys.NemesysSegment

object TestMessageSamples {
    val testMessages = listOf(
        TestMessage(0,
            "081611b892473a80d6c641".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),
                NemesysSegment(2, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(1,
            "08031163b719da7fd6c641".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),
                NemesysSegment(2, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(2,
            "080b11c80664df7fd6c641".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),
                NemesysSegment(2, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(3,
            "3081D0308183020100300F310D300B06035504030C0474657374302A300506032B6570032100FB16E6BD645FB03D755D0C207042BF80AA7CBA385BECDB9C19FCFE0BC95B1898A041303F06092A864886F70D01090E31323030302E0603551D1104273025A023060A2B060104018237140203A0150C136164647265737340646F6D61696E2E74657374300506032B6570034100529E457A71C5D6B67344653EEF0885FBF0F56DFC83445D1DCD6CF6B25E389E5B6EF222E31CEDDA21F393616A6A66568383506ADCBEC571BEC87F8C9902C1390B".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),
                NemesysSegment(1, NemesysField.UNKNOWN),
                NemesysSegment(3, NemesysField.UNKNOWN),
                NemesysSegment(4, NemesysField.UNKNOWN),
                NemesysSegment(6, NemesysField.UNKNOWN),
                NemesysSegment(7, NemesysField.UNKNOWN),
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(9, NemesysField.UNKNOWN),
                NemesysSegment(10, NemesysField.UNKNOWN),
                NemesysSegment(11, NemesysField.UNKNOWN),
                NemesysSegment(12, NemesysField.UNKNOWN),
                NemesysSegment(13, NemesysField.UNKNOWN),
                NemesysSegment(14, NemesysField.UNKNOWN),
                NemesysSegment(15, NemesysField.UNKNOWN),
                NemesysSegment(16, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.UNKNOWN),
                NemesysSegment(20, NemesysField.UNKNOWN),
                NemesysSegment(21, NemesysField.UNKNOWN),
                NemesysSegment(22, NemesysField.UNKNOWN),
                NemesysSegment(26, NemesysField.UNKNOWN),
                NemesysSegment(27, NemesysField.UNKNOWN),
                NemesysSegment(28, NemesysField.UNKNOWN),
                NemesysSegment(29, NemesysField.UNKNOWN),
                NemesysSegment(30, NemesysField.UNKNOWN),
                NemesysSegment(31, NemesysField.UNKNOWN),
                NemesysSegment(32, NemesysField.UNKNOWN),
                NemesysSegment(35, NemesysField.UNKNOWN),
                NemesysSegment(37, NemesysField.UNKNOWN),
                NemesysSegment(70, NemesysField.UNKNOWN),
                NemesysSegment(71, NemesysField.UNKNOWN),
                NemesysSegment(72, NemesysField.UNKNOWN),
                NemesysSegment(73, NemesysField.UNKNOWN),
                NemesysSegment(74, NemesysField.UNKNOWN),
                NemesysSegment(75, NemesysField.UNKNOWN),
                NemesysSegment(76, NemesysField.UNKNOWN),
                NemesysSegment(85, NemesysField.UNKNOWN),
                NemesysSegment(86, NemesysField.UNKNOWN),
                NemesysSegment(87, NemesysField.UNKNOWN),
                NemesysSegment(88, NemesysField.UNKNOWN),
                NemesysSegment(89, NemesysField.UNKNOWN),
                NemesysSegment(90, NemesysField.UNKNOWN),
                NemesysSegment(91, NemesysField.UNKNOWN),
                NemesysSegment(92, NemesysField.UNKNOWN),
                NemesysSegment(93, NemesysField.UNKNOWN),
                NemesysSegment(96, NemesysField.UNKNOWN),
                NemesysSegment(98, NemesysField.UNKNOWN),
                NemesysSegment(99, NemesysField.UNKNOWN),
                NemesysSegment(100, NemesysField.UNKNOWN),
                NemesysSegment(101, NemesysField.UNKNOWN),
                NemesysSegment(102, NemesysField.UNKNOWN),
                NemesysSegment(103, NemesysField.UNKNOWN),
                NemesysSegment(104, NemesysField.UNKNOWN),
                NemesysSegment(114, NemesysField.UNKNOWN),
                NemesysSegment(115, NemesysField.UNKNOWN),
                NemesysSegment(116, NemesysField.UNKNOWN),
                NemesysSegment(117, NemesysField.UNKNOWN),
                NemesysSegment(118, NemesysField.UNKNOWN),
                NemesysSegment(137, NemesysField.UNKNOWN),
                NemesysSegment(138, NemesysField.UNKNOWN),
                NemesysSegment(139, NemesysField.UNKNOWN),
                NemesysSegment(140, NemesysField.UNKNOWN),
                NemesysSegment(141, NemesysField.UNKNOWN),
                NemesysSegment(144, NemesysField.UNKNOWN),
                NemesysSegment(146, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(4,
            "0821b10132010c3f2f5cd941da0104080010009a02bf010a3b636f6d2e6170706c652e74656c6570686f6e797574696c69746965732e63616c6c7365727669636573642e4661636554696d6550726f766964657212084661636554696d651a6466696c653a2f2f2f707269766174652f7661722f636f6e7461696e6572732f42756e646c652f4170706c69636174696f6e2f30323639344631412d303138312d343031342d423036342d4536303938333636464431342f4661636554696d652e6170702f2002280130013801400048006803680278019a02de010a17636f6d2e6170706c652e636f726574656c6570686f6e79200228053001380040014801680278018a0107080212033131328a0107080212033131308a0107080212033131328a0107080212033131308a0107080212033931318a0107080212033131328a0107080212033030308a01060802120230388a0107080212033131308a0107080212033939398a0107080212033131388a0107080212033131398a0107080212033132308a0107080212033132328a0107080212033931318a0107080212033131328a0108080212042a3931318a010808021204233931319a02450a31636f6d2e6170706c652e74656c6570686f6e797574696c69746965732e63616c6c7365727669636573642e54696e43616e200128013001380040004800680368027800".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),
                NemesysSegment(2, NemesysField.UNKNOWN),
                NemesysSegment(12, NemesysField.UNKNOWN),
                NemesysSegment(15, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.UNKNOWN),
                NemesysSegment(19, NemesysField.UNKNOWN),
                NemesysSegment(23, NemesysField.UNKNOWN),
                NemesysSegment(84, NemesysField.UNKNOWN),
                NemesysSegment(94, NemesysField.UNKNOWN),
                NemesysSegment(196, NemesysField.UNKNOWN),
                NemesysSegment(198, NemesysField.UNKNOWN),
                NemesysSegment(200, NemesysField.UNKNOWN),
                NemesysSegment(202, NemesysField.UNKNOWN),
                NemesysSegment(204, NemesysField.UNKNOWN),
                NemesysSegment(206, NemesysField.UNKNOWN),
                NemesysSegment(208, NemesysField.UNKNOWN),
                NemesysSegment(210, NemesysField.UNKNOWN),
                NemesysSegment(212, NemesysField.UNKNOWN),
                NemesysSegment(214, NemesysField.UNKNOWN),
                NemesysSegment(218, NemesysField.UNKNOWN),
                NemesysSegment(243, NemesysField.UNKNOWN),
                NemesysSegment(245, NemesysField.UNKNOWN),
                NemesysSegment(247, NemesysField.UNKNOWN),
                NemesysSegment(249, NemesysField.UNKNOWN),
                NemesysSegment(251, NemesysField.UNKNOWN),
                NemesysSegment(253, NemesysField.UNKNOWN),
                NemesysSegment(255, NemesysField.UNKNOWN),
                NemesysSegment(257, NemesysField.UNKNOWN),
                NemesysSegment(259, NemesysField.UNKNOWN),
                NemesysSegment(262, NemesysField.UNKNOWN),
                NemesysSegment(264, NemesysField.UNKNOWN),
                NemesysSegment(269, NemesysField.UNKNOWN),
                NemesysSegment(272, NemesysField.UNKNOWN),
                NemesysSegment(274, NemesysField.UNKNOWN),
                NemesysSegment(279, NemesysField.UNKNOWN),
                NemesysSegment(282, NemesysField.UNKNOWN),
                NemesysSegment(284, NemesysField.UNKNOWN),
                NemesysSegment(289, NemesysField.UNKNOWN),
                NemesysSegment(292, NemesysField.UNKNOWN),
                NemesysSegment(294, NemesysField.UNKNOWN),
                NemesysSegment(299, NemesysField.UNKNOWN),
                NemesysSegment(302, NemesysField.UNKNOWN),
                NemesysSegment(304, NemesysField.UNKNOWN),
                NemesysSegment(309, NemesysField.UNKNOWN),
                NemesysSegment(312, NemesysField.UNKNOWN),
                NemesysSegment(314, NemesysField.UNKNOWN),
                NemesysSegment(319, NemesysField.UNKNOWN),
                NemesysSegment(322, NemesysField.UNKNOWN),
                NemesysSegment(324, NemesysField.UNKNOWN),
                NemesysSegment(329, NemesysField.UNKNOWN),
                NemesysSegment(332, NemesysField.UNKNOWN),
                NemesysSegment(334, NemesysField.UNKNOWN),
                NemesysSegment(338, NemesysField.UNKNOWN),
                NemesysSegment(341, NemesysField.UNKNOWN),
                NemesysSegment(343, NemesysField.UNKNOWN),
                NemesysSegment(348, NemesysField.UNKNOWN),
                NemesysSegment(351, NemesysField.UNKNOWN),
                NemesysSegment(353, NemesysField.UNKNOWN),
                NemesysSegment(358, NemesysField.UNKNOWN),
                NemesysSegment(361, NemesysField.UNKNOWN),
                NemesysSegment(363, NemesysField.UNKNOWN),
                NemesysSegment(368, NemesysField.UNKNOWN),
                NemesysSegment(371, NemesysField.UNKNOWN),
                NemesysSegment(373, NemesysField.UNKNOWN),
                NemesysSegment(378, NemesysField.UNKNOWN),
                NemesysSegment(381, NemesysField.UNKNOWN),
                NemesysSegment(383, NemesysField.UNKNOWN),
                NemesysSegment(388, NemesysField.UNKNOWN),
                NemesysSegment(391, NemesysField.UNKNOWN),
                NemesysSegment(393, NemesysField.UNKNOWN),
                NemesysSegment(398, NemesysField.UNKNOWN),
                NemesysSegment(401, NemesysField.UNKNOWN),
                NemesysSegment(403, NemesysField.UNKNOWN),
                NemesysSegment(408, NemesysField.UNKNOWN),
                NemesysSegment(411, NemesysField.UNKNOWN),
                NemesysSegment(413, NemesysField.UNKNOWN),
                NemesysSegment(418, NemesysField.UNKNOWN),
                NemesysSegment(421, NemesysField.UNKNOWN),
                NemesysSegment(423, NemesysField.UNKNOWN),
                NemesysSegment(429, NemesysField.UNKNOWN),
                NemesysSegment(432, NemesysField.UNKNOWN),
                NemesysSegment(434, NemesysField.UNKNOWN),
                NemesysSegment(440, NemesysField.UNKNOWN),
                NemesysSegment(443, NemesysField.UNKNOWN),
                NemesysSegment(494, NemesysField.UNKNOWN),
                NemesysSegment(496, NemesysField.UNKNOWN),
                NemesysSegment(498, NemesysField.UNKNOWN),
                NemesysSegment(500, NemesysField.UNKNOWN),
                NemesysSegment(502, NemesysField.UNKNOWN),
                NemesysSegment(504, NemesysField.UNKNOWN),
                NemesysSegment(506, NemesysField.UNKNOWN),
                NemesysSegment(508, NemesysField.UNKNOWN),
                NemesysSegment(510, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(5,
            "62706c6973743137a0d0000000000000007f114870726564696374696f6e466f7243617465676f726965733a636f6e73756d65723a63726974657269613a6c696d69743a70726f7669646573466565646261636b3a7265706c793a007f111b76363040303a38513136513234403332513430423438403f353200a0d00000000000000011061101d0cc000000000000007724636c617373007d4e5344696374696f6e61727900784e532e6b65797300a0b7000000000000007b4e532e6f626a6563747300a0cc000000000000001101b0e0".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING), // BPLIST
                NemesysSegment(6, NemesysField.STRING),  // version
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.UNKNOWN), // predictionFor... type
                NemesysSegment(19, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN), // predictionFor... length
                NemesysSegment(20, NemesysField.STRING), // predictionFor... text
                NemesysSegment(92, NemesysField.UNKNOWN), // v60@0:8Q16Q24@32Q40B48@?52 type
                NemesysSegment(94, NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN), // v60@0:8Q16Q24@32Q40B48@?52 length
                NemesysSegment(95, NemesysField.STRING), // v60@0:8Q16Q24@32Q40B48@?52 text
                NemesysSegment(122, NemesysField.UNKNOWN),
                NemesysSegment(131, NemesysField.UNKNOWN), // 6 type
                NemesysSegment(132, NemesysField.UNKNOWN), // 6
                NemesysSegment(133, NemesysField.UNKNOWN), // 1 type
                NemesysSegment(134, NemesysField.UNKNOWN), // 1
                NemesysSegment(135, NemesysField.UNKNOWN),
                NemesysSegment(144, NemesysField.UNKNOWN), // class type
                NemesysSegment(145, NemesysField.STRING), // class
                NemesysSegment(152, NemesysField.UNKNOWN), // NSDIRECTONARY type
                NemesysSegment(153, NemesysField.STRING), // NSDIRECTONARY
                NemesysSegment(166, NemesysField.UNKNOWN), // NS.keys type
                NemesysSegment(167, NemesysField.STRING), // NS.keys
                NemesysSegment(175, NemesysField.UNKNOWN),
                NemesysSegment(184, NemesysField.UNKNOWN), // NS.objects type
                NemesysSegment(185, NemesysField.STRING), // NS.objects
                NemesysSegment(196, NemesysField.UNKNOWN),
                NemesysSegment(205, NemesysField.UNKNOWN), // 1 type
                NemesysSegment(206, NemesysField.UNKNOWN), // 1
                NemesysSegment(207, NemesysField.UNKNOWN), // true
                NemesysSegment(208, NemesysField.UNKNOWN) // end
            )
        ),
        TestMessage(6,
            "62706c697374313513c7000000000000801200000000a44776403a404040004f10ce61636b6e6f776c656467654f7574676f696e674d65737361676557697468475549443a616c7465726e61746543616c6c6261636b49443a666f724163636f756e7457697468556e6971756549443aa3108710881087a37f10a432303841303836302d373535392d343731362d424136352d434332363638334644413235407f10a436433836343332372d364441452d344133312d394137362d3731434135454533363839300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING), // bplist
                NemesysSegment(6, NemesysField.STRING), // version
                NemesysSegment(8, NemesysField.UNKNOWN), // bplist
                NemesysSegment(22, NemesysField.UNKNOWN),
                NemesysSegment(24, NemesysField.STRING), // v@:@@@
                NemesysSegment(31, NemesysField.UNKNOWN),
                NemesysSegment(34, NemesysField.UNKNOWN),
                NemesysSegment(112, NemesysField.UNKNOWN), // array
                NemesysSegment(113, NemesysField.UNKNOWN), // 7
                NemesysSegment(115, NemesysField.UNKNOWN), // 8
                NemesysSegment(117, NemesysField.UNKNOWN), // 7
                NemesysSegment(119, NemesysField.UNKNOWN), // array
                NemesysSegment(120, NemesysField.UNKNOWN), // 208A0860-7559-4716-BA65-CC26683FDA25
                NemesysSegment(159, NemesysField.UNKNOWN), // 0x
                NemesysSegment(160, NemesysField.UNKNOWN), // 6C864327-6DAE-4A31-9A76-71CA5EE36890
                NemesysSegment(199, NemesysField.UNKNOWN) // end
            )
        ),
        TestMessage(7,
            "62706c69737431351367000000000000801200000000d478636c69656e744944756576656e747c70726f636573732d6e616d657973686f756c644c6f6710977e55494b69742d4b6579626f6172647f109156697375616c50616972696e674861636b74506f737400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING), // bplist
                NemesysSegment(6, NemesysField.STRING), // version
                NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(22, NemesysField.UNKNOWN), // array
                NemesysSegment(23, NemesysField.UNKNOWN), // clientID type
                NemesysSegment(24, NemesysField.STRING), // clientID
                NemesysSegment(32, NemesysField.UNKNOWN), // event type
                NemesysSegment(33, NemesysField.STRING), // event
                NemesysSegment(38, NemesysField.UNKNOWN), // process-name type
                NemesysSegment(39, NemesysField.STRING), // process-name
                NemesysSegment(51, NemesysField.UNKNOWN), // shouldLog type
                NemesysSegment(52, NemesysField.STRING), // shouldLog
                NemesysSegment(61, NemesysField.UNKNOWN), // 23
                NemesysSegment(63, NemesysField.UNKNOWN), // UIKit-Keyboard type
                NemesysSegment(64, NemesysField.STRING), // UIKit-Keyboard type
                NemesysSegment(78, NemesysField.UNKNOWN), // VisualPairingHack type
                NemesysSegment(81, NemesysField.STRING), // VisualPairingHack
                NemesysSegment(98, NemesysField.UNKNOWN), // Post type
                NemesysSegment(99, NemesysField.STRING), // Post
                NemesysSegment(103, NemesysField.UNKNOWN) // end
            )
        ),
        TestMessage(8,
            "86A26964CD0FC5B27375636365737350726F626162696C697479CB3FE0000000000000AA6973456C696769626C65C3A46E616D65AA4672616E7A204B61726CAA686967686C696768747392B55363686F6C61727368697020726563697069656E74B852656C6576616E7420776F726B20657870657269656E6365AA6174747269627574657382A6766973696F6ECB3FECCCCCCCCCCCCDAB6174686C6574696369736DCB3FD3333333333333".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),               // 86 (map with 6 entries)
                NemesysSegment(1, NemesysField.UNKNOWN),            // A2 69 64 -> "id" type
                NemesysSegment(2, NemesysField.STRING),            // A2 69 64 -> "id"
                NemesysSegment(4, NemesysField.UNKNOWN),            // CD 0F C5 -> 4037 type
                NemesysSegment(5, NemesysField.UNKNOWN),            // CD 0F C5 -> 4037
                NemesysSegment(7, NemesysField.UNKNOWN),            // B2 ... -> "successProbability" type
                NemesysSegment(8, NemesysField.STRING),            // B2 ... -> "successProbability"
                NemesysSegment(26, NemesysField.UNKNOWN),           // CB 3F E0 00 00 00 00 00 00 -> 0.5
                NemesysSegment(35, NemesysField.UNKNOWN),           // AA ... -> "isEligible" type
                NemesysSegment(36, NemesysField.STRING),           // AA ... -> "isEligible"
                NemesysSegment(46, NemesysField.UNKNOWN),          // C3 -> true
                NemesysSegment(47, NemesysField.UNKNOWN),           // A4 ... -> "name" type
                NemesysSegment(48, NemesysField.STRING),           // A4 ... -> "name"
                NemesysSegment(52, NemesysField.UNKNOWN),           // AA ... -> "Franz Karl" type
                NemesysSegment(53, NemesysField.STRING),           // AA ... -> "Franz Karl"
                NemesysSegment(63, NemesysField.UNKNOWN),           // AA ... -> "highlights" type
                NemesysSegment(64, NemesysField.STRING),           // AA ... -> "highlights"
                NemesysSegment(74, NemesysField.UNKNOWN),            // 92 -> array of 2 strings
                NemesysSegment(75, NemesysField.UNKNOWN),           // B5 ... -> "Scholarship recipient" type
                NemesysSegment(76, NemesysField.STRING),           // B5 ... -> "Scholarship recipient"
                NemesysSegment(97, NemesysField.UNKNOWN),           // B8 ... -> "Relevant work experience" type
                NemesysSegment(98, NemesysField.STRING),           // B8 ... -> "Relevant work experience"
                NemesysSegment(122, NemesysField.UNKNOWN),          // AA ... -> "attributes" type
                NemesysSegment(123, NemesysField.STRING),          // AA ... -> "attributes"
                NemesysSegment(133, NemesysField.UNKNOWN),             // 82 -> map with 2 entries
                NemesysSegment(134, NemesysField.UNKNOWN),          // A6 ... -> "vision" type
                NemesysSegment(135, NemesysField.STRING),          // A6 ... -> "vision"
                NemesysSegment(141, NemesysField.UNKNOWN),          // CB ... -> 0.9
                NemesysSegment(150, NemesysField.UNKNOWN),          // AB ... -> "athleticism" type
                NemesysSegment(151, NemesysField.STRING),          // AB ... -> "athleticism"
                NemesysSegment(162, NemesysField.UNKNOWN)           // CB ... -> 0.3
            )
        ),
        TestMessage(9,
            "86A26964CD0FC6B27375636365737350726F626162696C697479CB3FEA3D70A3D70A3DAA6973456C696769626C65C3A46E616D65AD416D69726120536F6C62657267AA686967686C696768747392B8496E7465726E6174696F6E616C20696E7465726E73686970B25075626C6973686564207265736561726368AA6174747269627574657382A6766973696F6E01AB6174686C6574696369736DCB3FE6666666666666".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),               // 86 (map mit 6 Einträgen)
                NemesysSegment(1, NemesysField.UNKNOWN),            // A2 69 64 → "id" type
                NemesysSegment(2, NemesysField.STRING),            // A2 69 64 → "id"
                NemesysSegment(4, NemesysField.UNKNOWN),            // CD 0F C6 → 4038 type
                NemesysSegment(5, NemesysField.UNKNOWN),            // CD 0F C6 → 4038
                NemesysSegment(7, NemesysField.UNKNOWN),            // B2 ... → "successProbability" type
                NemesysSegment(8, NemesysField.STRING),            // B2 ... → "successProbability"
                NemesysSegment(26, NemesysField.UNKNOWN),           // CB ... → 0.82
                NemesysSegment(35, NemesysField.UNKNOWN),           // AA ... → "isEligible" type
                NemesysSegment(36, NemesysField.STRING),           // AA ... → "isEligible"
                NemesysSegment(46, NemesysField.UNKNOWN),          // C3 → true
                NemesysSegment(47, NemesysField.UNKNOWN),           // A4 ... → "name" type
                NemesysSegment(48, NemesysField.STRING),           // A4 ... → "name"
                NemesysSegment(52, NemesysField.UNKNOWN),           // AD ... → "Amira Solberg" type
                NemesysSegment(53, NemesysField.STRING),           // AD ... → "Amira Solberg"
                NemesysSegment(66, NemesysField.UNKNOWN),           // AA ... → "highlights" type
                NemesysSegment(67, NemesysField.STRING),           // AA ... → "highlights"
                NemesysSegment(77, NemesysField.UNKNOWN),            // 92 → Array mit 2 Strings
                NemesysSegment(78, NemesysField.UNKNOWN),           // B8 ... → "International internship" type
                NemesysSegment(79, NemesysField.STRING),           // B8 ... → "International internship"
                NemesysSegment(103, NemesysField.UNKNOWN),          // B2 ... → "Published research" type
                NemesysSegment(104, NemesysField.STRING),          // B2 ... → "Published research"
                NemesysSegment(122, NemesysField.UNKNOWN),          // AA ... → "attributes" type
                NemesysSegment(123, NemesysField.STRING),          // AA ... → "attributes"
                NemesysSegment(133, NemesysField.UNKNOWN),             // 82 → Map mit 2 Einträgen
                NemesysSegment(134, NemesysField.UNKNOWN),          // A6 ... → "vision" type
                NemesysSegment(135, NemesysField.STRING),          // A6 ... → "vision"
                NemesysSegment(141, NemesysField.UNKNOWN),           // 01 → 1
                NemesysSegment(142, NemesysField.UNKNOWN),          // AB ... → "athleticism" type
                NemesysSegment(143, NemesysField.STRING),          // AB ... → "athleticism"
                NemesysSegment(154, NemesysField.UNKNOWN)           // CB ... → 0.7
            )
        ),
        TestMessage(10,
            "86A26964CD0FC7B27375636365737350726F626162696C697479CB3FD6666666666666AA6973456C696769626C65C2A46E616D65AD4A6F6E61732052696368746572AA686967686C696768747392AE566F6C756E7465657220776F726BB842617369632070726F6772616D6D696E6720736B696C6C73AA6174747269627574657382A6766973696F6ECB3FE3333333333333AB6174686C6574696369736DCB3FE999999999999A".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),               // 86 (map mit 6 Einträgen)
                NemesysSegment(1, NemesysField.UNKNOWN),               // A2 69 64 → "id" type
                NemesysSegment(2, NemesysField.STRING),                // A2 69 64 → "id"
                NemesysSegment(4, NemesysField.UNKNOWN),               // CD 0F C7 → 4039 type
                NemesysSegment(5, NemesysField.UNKNOWN),               // CD 0F C7 → 4039
                NemesysSegment(7, NemesysField.UNKNOWN),               // B2 ... → "successProbability" type
                NemesysSegment(8, NemesysField.STRING),                // B2 ... → "successProbability"
                NemesysSegment(26, NemesysField.UNKNOWN),              // CB ... → 0.35
                NemesysSegment(35, NemesysField.UNKNOWN),              // AA ... → "isEligible" type
                NemesysSegment(36, NemesysField.STRING),               // AA ... → "isEligible"
                NemesysSegment(46, NemesysField.UNKNOWN),              // C2 → false
                NemesysSegment(47, NemesysField.UNKNOWN),              // A4 ... → "name" type
                NemesysSegment(48, NemesysField.STRING),               // A4 ... → "name"
                NemesysSegment(52, NemesysField.UNKNOWN),              // AD ... → "Jonas Richter" type
                NemesysSegment(53, NemesysField.STRING),               // AD ... → "Jonas Richter"
                NemesysSegment(66, NemesysField.UNKNOWN),              // AA ... → "highlights" type
                NemesysSegment(67, NemesysField.STRING),               // AA ... → "highlights"
                NemesysSegment(77, NemesysField.UNKNOWN),              // 92 → array mit 2 Strings
                NemesysSegment(78, NemesysField.UNKNOWN),              // AE ... → "Volunteer work" type
                NemesysSegment(79, NemesysField.STRING),               // AE ... → "Volunteer work"
                NemesysSegment(93, NemesysField.UNKNOWN),              // B8 ... → "Basic programming skills" type
                NemesysSegment(94, NemesysField.STRING),               // B8 ... → "Basic programming skills"
                NemesysSegment(118, NemesysField.UNKNOWN),             // AA ... → "attributes" type
                NemesysSegment(119, NemesysField.STRING),              // AA ... → "attributes"
                NemesysSegment(129, NemesysField.UNKNOWN),             // 82 → map mit 2 Einträgen
                NemesysSegment(130, NemesysField.UNKNOWN),             // A6 ... → "vision" type
                NemesysSegment(131, NemesysField.STRING),              // A6 ... → "vision"
                NemesysSegment(137, NemesysField.UNKNOWN),             // CB ... → 0.6
                NemesysSegment(146, NemesysField.UNKNOWN),             // AB ... → "athleticism" type
                NemesysSegment(147, NemesysField.STRING),              // AB ... → "athleticism"
                NemesysSegment(158, NemesysField.UNKNOWN)              // CB ... → 0.8
            )
        ),
        TestMessage(11,
            "86A26964CD0FC8B27375636365737350726F626162696C697479CB3FEE147AE147AE14AA6973456C696769626C65C3A46E616D65A94C696E61204368656EAA686967686C696768747392B0546F70206F662068657220636C617373B04C656164657273686970206177617264AA6174747269627574657382A6766973696F6ECB3FEE666666666666AB6174686C6574696369736DCB3FE0000000000000".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.UNKNOWN),               // 86 → map mit 6 Einträgen
                NemesysSegment(1, NemesysField.UNKNOWN),               // A2 69 64 → "id" type
                NemesysSegment(2, NemesysField.STRING),                // A2 69 64 → "id"
                NemesysSegment(4, NemesysField.UNKNOWN),               // CD 0F C8 → 4040 type
                NemesysSegment(5, NemesysField.UNKNOWN),               // CD 0F C8 → 4040
                NemesysSegment(7, NemesysField.UNKNOWN),               // B2 ... → "successProbability" type
                NemesysSegment(8, NemesysField.STRING),                // B2 ... → "successProbability"
                NemesysSegment(26, NemesysField.UNKNOWN),              // CB ... → 0.94
                NemesysSegment(35, NemesysField.UNKNOWN),              // AA ... → "isEligible" type
                NemesysSegment(36, NemesysField.STRING),               // AA ... → "isEligible"
                NemesysSegment(46, NemesysField.UNKNOWN),              // C3 → true
                NemesysSegment(47, NemesysField.UNKNOWN),              // A4 ... → "name" type
                NemesysSegment(48, NemesysField.STRING),               // A4 ... → "name"
                NemesysSegment(52, NemesysField.UNKNOWN),              // A9 ... → "Lina Chen" type
                NemesysSegment(53, NemesysField.STRING),               // A9 ... → "Lina Chen"
                NemesysSegment(62, NemesysField.UNKNOWN),              // AA ... → "highlights" type
                NemesysSegment(63, NemesysField.STRING),               // AA ... → "highlights"
                NemesysSegment(73, NemesysField.UNKNOWN),              // 92 → array mit 2 Strings
                NemesysSegment(74, NemesysField.UNKNOWN),              // B0 ... → "Top of her class" type
                NemesysSegment(75, NemesysField.STRING),               // B0 ... → "Top of her class"
                NemesysSegment(91, NemesysField.UNKNOWN),              // B0 ... → "Leadership award" type
                NemesysSegment(92, NemesysField.STRING),               // B0 ... → "Leadership award"
                NemesysSegment(108, NemesysField.UNKNOWN),             // AA ... → "attributes" type
                NemesysSegment(109, NemesysField.STRING),              // AA ... → "attributes"
                NemesysSegment(119, NemesysField.UNKNOWN),             // 82 → map mit 2 Einträgen
                NemesysSegment(120, NemesysField.UNKNOWN),             // A6 ... → "vision" type
                NemesysSegment(121, NemesysField.STRING),              // A6 ... → "vision"
                NemesysSegment(127, NemesysField.UNKNOWN),             // CB ... → 0.95
                NemesysSegment(136, NemesysField.UNKNOWN),             // AB ... → "athleticism" type
                NemesysSegment(137, NemesysField.STRING),              // AB ... → "athleticism"
                NemesysSegment(148, NemesysField.UNKNOWN)              // CB ... → 0.5
            )
        ),
        TestMessage(12,
            "19010000026973626e00120000003937382d302d30362d3131323030382d3400027469746c650016000000546f204b696c6c2061204d6f636b696e67626972640003617574686f72002d0000000266697273744e616d65000700000048617270657200026c6173744e616d6500040000004c65650000107075626c69736865645965617200a80700000467656e72657300300000000230000800000046696374696f6e0002310008000000436c617373696300023200060000004c6567616c0000037072696365002700000001616d6f756e74007b14ae47e1fa29400263757272656e63790004000000555344000008617661696c61626c65000101726174696e6700333333333333134010696e53746f636b002200000000".fromHex(),
            listOf(
                // BSON-Dokumentlänge (Little Endian: 0x00000119 = 281 Dezimal)
                NemesysSegment(0, NemesysField.UNKNOWN),

                // "isbn": "978-0-06-112008-4"
                NemesysSegment(5, NemesysField.STRING),        // "isbn"
                NemesysSegment(10, NemesysField.STRING),       // "978-0-06-112008-4"

                // "title": "To Kill a Mockingbird"
                NemesysSegment(33, NemesysField.STRING),       // "title"
                NemesysSegment(39, NemesysField.STRING),       // "To Kill a Mockingbird"

                // "author": { "firstName": "Harper", "lastName": "Lee" }
                NemesysSegment(66, NemesysField.STRING),       // "author"
                NemesysSegment(73, NemesysField.UNKNOWN),      // Subdocument

                NemesysSegment(78, NemesysField.STRING),       // "firstName"
                NemesysSegment(88, NemesysField.STRING),       // "Harper"

                NemesysSegment(100, NemesysField.STRING),      // "lastName"
                NemesysSegment(109, NemesysField.STRING),      // "Lee"

                // "publishedYear": 1960
                NemesysSegment(119, NemesysField.STRING),      // "publishedYear"
                NemesysSegment(133, NemesysField.UNKNOWN),     // 1960

                // "genres": { "0": "Fiction", "1": "Classic", "2": "Legal" }
                NemesysSegment(138, NemesysField.STRING),      // "genres"
                NemesysSegment(145, NemesysField.UNKNOWN),     // Subdocument

                NemesysSegment(150, NemesysField.STRING),      // "0"
                NemesysSegment(152, NemesysField.STRING),      // "Fiction"

                NemesysSegment(165, NemesysField.STRING),      // "1"
                NemesysSegment(167, NemesysField.STRING),      // "Classic"

                NemesysSegment(180, NemesysField.STRING),      // "2"
                NemesysSegment(182, NemesysField.STRING),      // "Legal"

                // "price": { "amount": 12.99, "currency": "USD" }
                NemesysSegment(194, NemesysField.STRING),      // "price"
                NemesysSegment(200, NemesysField.UNKNOWN),     // Subdocument

                NemesysSegment(205, NemesysField.STRING),      // "amount"
                NemesysSegment(212, NemesysField.UNKNOWN),       // 12.99

                NemesysSegment(221, NemesysField.STRING),      // "currency"
                NemesysSegment(230, NemesysField.STRING),      // "USD"

                // "available": true
                NemesysSegment(240, NemesysField.STRING),      // "available"
                NemesysSegment(250, NemesysField.UNKNOWN),     // true

                // "rating": 4.8
                NemesysSegment(252, NemesysField.STRING),      // "rating"
                NemesysSegment(259, NemesysField.UNKNOWN),       // 4.8

                // "inStock": 34
                NemesysSegment(268, NemesysField.STRING),      // "inStock"
                NemesysSegment(276, NemesysField.UNKNOWN),     // 34

                // Dokument-Ende
                NemesysSegment(280, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(13,
            "06010000026973626e00120000003937382d302d373433322d373335362d3500027469746c650010000000416e67656c7320262044656d6f6e730003617574686f72002c0000000266697273744e616d65000400000044616e00026c6173744e616d65000600000042726f776e0000107075626c69736865645965617200d00700000467656e726573002400000002300009000000546872696c6c657200023100080000004d7973746572790000037072696365002700000001616d6f756e74007b14ae47e1fa23400263757272656e63790004000000555344000008617661696c61626c65000101726174696e6700666666666666104010696e53746f636b000c00000000".fromHex(),
            listOf(
                // BSON-Dokumentlänge
                NemesysSegment(0, NemesysField.UNKNOWN),              // 06010000 → 262 Bytes

                // "isbn": "978-0-7432-7356-5"
                NemesysSegment(5, NemesysField.STRING),               // "isbn"
                NemesysSegment(10, NemesysField.STRING),              // "978-0-7432-7356-5"

                // "title": "Angels & Demons"
                NemesysSegment(33, NemesysField.STRING),              // "title"
                NemesysSegment(39, NemesysField.STRING),              // "Angels & Demons"

                // "author": { "firstName": "Dan", "lastName": "Brown" }
                NemesysSegment(60, NemesysField.STRING),              // "author"
                NemesysSegment(67, NemesysField.UNKNOWN),             // Subdocument

                NemesysSegment(72, NemesysField.STRING),              // "firstName"
                NemesysSegment(82, NemesysField.STRING),              // "Dan"

                NemesysSegment(91, NemesysField.STRING),              // "lastName"
                NemesysSegment(100, NemesysField.STRING),             // "Brown"

                // "publishedYear": 2000
                NemesysSegment(112, NemesysField.STRING),             // "publishedYear"
                NemesysSegment(126, NemesysField.UNKNOWN),            // 2000

                // "genres": { "0": "Thriller", "1": "Mystery" }
                NemesysSegment(131, NemesysField.STRING),             // "genres"
                NemesysSegment(138, NemesysField.UNKNOWN),            // Subdocument

                NemesysSegment(143, NemesysField.STRING),             // "0"
                NemesysSegment(145, NemesysField.STRING),             // "Thriller"

                NemesysSegment(159, NemesysField.STRING),             // "1"
                NemesysSegment(161, NemesysField.STRING),             // "Mystery"

                // "price": { "amount": 9.99, "currency": "USD" }
                NemesysSegment(175, NemesysField.STRING),             // "price"
                NemesysSegment(181, NemesysField.UNKNOWN),            // Subdocument

                NemesysSegment(186, NemesysField.STRING),             // "amount"
                NemesysSegment(193, NemesysField.UNKNOWN),              // 9.99

                NemesysSegment(202, NemesysField.STRING),             // "currency"
                NemesysSegment(211, NemesysField.STRING),             // "USD"

                // "available": true
                NemesysSegment(221, NemesysField.STRING),             // "available"
                NemesysSegment(231, NemesysField.UNKNOWN),            // true

                // "rating": 4.1
                NemesysSegment(233, NemesysField.STRING),             // "rating"
                NemesysSegment(240, NemesysField.UNKNOWN),              // 4.1

                // "inStock": 12
                NemesysSegment(249, NemesysField.STRING),             // "inStock"
                NemesysSegment(257, NemesysField.UNKNOWN),            // 12

                // Dokument-Ende
                NemesysSegment(261, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(14,
            "1b010000026973626e00120000003937382d312d3235302d33303636392d3800027469746c650015000000546865204d69646e69676874204c6962726172790003617574686f72002c0000000266697273744e616d6500050000004d61747400026c6173744e616d650005000000486169670000107075626c69736865645965617200e40700000467656e72657300380000000230000800000046616e74617379000231000e0000005068696c6f736f70686963616c000232000800000046696374696f6e0000037072696365002300000010616d6f756e7400100000000263757272656e63790004000000555344000008617661696c61626c65000001726174696e6700000000000000124010696e53746f636b000000000000".fromHex(),
            listOf(
                // BSON-Dokumentlänge
                NemesysSegment(0, NemesysField.UNKNOWN),              // 1b010000 = 283 Bytes

                // "isbn": "978-1-250-30669-8"
                NemesysSegment(5, NemesysField.STRING),               // "isbn"
                NemesysSegment(10, NemesysField.STRING),              // "978-1-250-30669-8"

                // "title": "The Midnight Library"
                NemesysSegment(33, NemesysField.STRING),              // "title"
                NemesysSegment(39, NemesysField.STRING),              // "The Midnight Library"

                // "author": { "firstName": "Matt", "lastName": "Haig" }
                NemesysSegment(65, NemesysField.STRING),              // "author"
                NemesysSegment(72, NemesysField.UNKNOWN),             // Subdocument

                NemesysSegment(77, NemesysField.STRING),              // "firstName"
                NemesysSegment(87, NemesysField.STRING),              // "Matt"

                NemesysSegment(97, NemesysField.STRING),              // "lastName"
                NemesysSegment(106, NemesysField.STRING),             // "Haig"

                // "publishedYear": 2020
                NemesysSegment(117, NemesysField.STRING),             // "publishedYear"
                NemesysSegment(131, NemesysField.UNKNOWN),            // 2020

                // "genres": { "0": "Fantasy", "1": "Philosophical", "2": "Fiction" }
                NemesysSegment(136, NemesysField.STRING),             // "genres"
                NemesysSegment(143, NemesysField.UNKNOWN),            // Subdocument

                NemesysSegment(148, NemesysField.STRING),             // "0"
                NemesysSegment(150, NemesysField.STRING),             // "Fantasy"

                NemesysSegment(163, NemesysField.STRING),             // "1"
                NemesysSegment(165, NemesysField.STRING),             // "Philosophical"

                NemesysSegment(184, NemesysField.STRING),             // "2"
                NemesysSegment(186, NemesysField.STRING),             // "Fiction"

                // "price": { "amount": 16, "currency": "USD" }
                NemesysSegment(200, NemesysField.STRING),             // "price"
                NemesysSegment(206, NemesysField.UNKNOWN),            // Subdocument

                NemesysSegment(211, NemesysField.STRING),             // "amount"
                NemesysSegment(218, NemesysField.UNKNOWN),            // 16

                NemesysSegment(223, NemesysField.STRING),             // "currency"
                NemesysSegment(232, NemesysField.STRING),             // "USD"

                // "available": false
                NemesysSegment(242, NemesysField.STRING),             // "available"
                NemesysSegment(252, NemesysField.UNKNOWN),            // false

                // "rating": 4.5
                NemesysSegment(254, NemesysField.STRING),             // "rating"
                NemesysSegment(261, NemesysField.UNKNOWN),              // 4.5

                // "inStock": 0
                NemesysSegment(270, NemesysField.STRING),             // "inStock"
                NemesysSegment(278, NemesysField.UNKNOWN),            // 0

                // Dokument-Ende
                NemesysSegment(282, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(15,
            "1e010000026973626e00120000003937382d312d393834382d373736372d3000027469746c65000e00000041746f6d6963204861626974730003617574686f72002e0000000266697273744e616d6500060000004a616d657300026c6173744e616d650006000000436c6561720000107075626c69736865645965617200e20700000467656e726573003c0000000230000a00000053656c662d68656c70000231000d00000050726f647563746976697479000232000b00000050737963686f6c6f67790000037072696365002700000001616d6f756e740000000000008032400263757272656e63790004000000555344000008617661696c61626c65000101726174696e67009a9999999999134010696e53746f636b003900000000".fromHex(),
            listOf(
                // BSON-Dokumentlänge
                NemesysSegment(0, NemesysField.UNKNOWN),              // 1e010000 → 286 Bytes

                // "isbn": "978-1-9848-7767-0"
                NemesysSegment(5, NemesysField.STRING),               // "isbn"
                NemesysSegment(10, NemesysField.STRING),              // "978-1-9848-7767-0"

                // "title": "Atomic Habits"
                NemesysSegment(33, NemesysField.STRING),              // "title"
                NemesysSegment(39, NemesysField.STRING),              // "Atomic Habits"

                // "author": { "firstName": "James", "lastName": "Clear" }
                NemesysSegment(58, NemesysField.STRING),              // "author"
                NemesysSegment(65, NemesysField.UNKNOWN),             // Subdocument

                NemesysSegment(70, NemesysField.STRING),              // "firstName"
                NemesysSegment(80, NemesysField.STRING),              // "James"

                NemesysSegment(91, NemesysField.STRING),              // "lastName"
                NemesysSegment(100, NemesysField.STRING),             // "Clear"

                // "publishedYear": 2018
                NemesysSegment(112, NemesysField.STRING),             // "publishedYear"
                NemesysSegment(126, NemesysField.UNKNOWN),            // 2018

                // "genres": { "0": "Self-help", "1": "Productivity", "2": "Psychology" }
                NemesysSegment(131, NemesysField.STRING),             // "genres"
                NemesysSegment(138, NemesysField.UNKNOWN),            // Subdocument

                NemesysSegment(143, NemesysField.STRING),             // "0"
                NemesysSegment(145, NemesysField.STRING),             // "Self-help"

                NemesysSegment(160, NemesysField.STRING),             // "1"
                NemesysSegment(162, NemesysField.STRING),             // "Productivity"

                NemesysSegment(180, NemesysField.STRING),             // "2"
                NemesysSegment(182, NemesysField.STRING),             // "Psychology"

                // "price": { "amount": 18.5, "currency": "USD" }
                NemesysSegment(199, NemesysField.STRING),             // "price"
                NemesysSegment(205, NemesysField.UNKNOWN),            // Subdocument

                NemesysSegment(210, NemesysField.STRING),             // "amount"
                NemesysSegment(217, NemesysField.UNKNOWN),              // 18.5

                NemesysSegment(226, NemesysField.STRING),             // "currency"
                NemesysSegment(235, NemesysField.STRING),             // "USD"

                // "available": true
                NemesysSegment(245, NemesysField.STRING),             // "available"
                NemesysSegment(255, NemesysField.UNKNOWN),            // true

                // "rating": 4.9
                NemesysSegment(257, NemesysField.STRING),             // "rating"
                NemesysSegment(264, NemesysField.UNKNOWN),              // 4.9

                // "inStock": 57
                NemesysSegment(273, NemesysField.STRING),             // "inStock"
                NemesysSegment(281, NemesysField.UNKNOWN),            // 57

                // Dokument-Ende
                NemesysSegment(285, NemesysField.UNKNOWN)
            )
        ),
        TestMessage(16,
            "62706c6973743030d4010203040506070852704751635165526c535f102438424139413938422d393842462d344331332d414545332d453430463946433631433344100b5a70726f64756374696f6e11c80008111416181b42444f0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING), NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING), NemesysSegment(20, NemesysField.STRING), // 2->"pg", 3->"c"
                NemesysSegment(22, NemesysField.STRING), NemesysSegment(24, NemesysField.STRING), // 4->"e", 5->"ls"
                NemesysSegment(27, NemesysField.STRING), NemesysSegment(66, NemesysField.UNKNOWN), // 6->"...", 7->11
                NemesysSegment(68, NemesysField.STRING), NemesysSegment(79, NemesysField.UNKNOWN) // 8->"production", 9->51200
            )
        ),
        TestMessage(17,
            "62706c6973743030d40102030405060708526d53527047516351651114005f102441303634324536462d463239452d343346362d394441442d424437323846343832463933100b5a70726f64756374696f6e08111417191b1e45470000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING), NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(17, NemesysField.STRING), NemesysSegment(20, NemesysField.STRING), // 2->"ms", 3->"pG"
                NemesysSegment(23, NemesysField.STRING), NemesysSegment(25, NemesysField.STRING), // 4->"c", 5->"e"
                NemesysSegment(27, NemesysField.UNKNOWN), NemesysSegment(30, NemesysField.STRING), // 6->5120, 7->"..."
                NemesysSegment(69, NemesysField.UNKNOWN), NemesysSegment(71, NemesysField.STRING) // 8->11, 9->"production"
            )
        ),
        TestMessage(18,
            "62706c6973743030d301020304050651635270475165100a5f102438424139413938422d393842462d344331332d414545332d4534304639464336314333445a70726f64756374696f6e080f111416183f000000000000010100000000000000070000000000000000000000000000004a".fromHex(),
            listOf(
                NemesysSegment(0, NemesysField.STRING), NemesysSegment(8, NemesysField.UNKNOWN),
                NemesysSegment(15, NemesysField.STRING), NemesysSegment(17, NemesysField.STRING), // 2->"c", 3->"pG"
                NemesysSegment(20, NemesysField.STRING), NemesysSegment(22, NemesysField.UNKNOWN), // 4->"e", 5->10
                NemesysSegment(24, NemesysField.STRING), NemesysSegment(63, NemesysField.STRING) // 6->"...", 7->"production"
            )
        ),
    )

    // test sequence alignment
    val alignmentTests = listOf(
        SequenceAlignmentTest(
            messageAIndex = 0,
            messageBIndex = 1,
            expectedAlignments = setOf(
                Triple(0, 1, Pair(0, 0)),
                Triple(0, 1, Pair(1, 1))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 1,
            messageBIndex = 2,
            expectedAlignments = setOf(
                Triple(1, 2, Pair(0, 0)),
                Triple(1, 2, Pair(1, 1))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 0,
            messageBIndex = 2,
            expectedAlignments = setOf(
                Triple(0, 2, Pair(0, 0)),
                Triple(0, 2, Pair(1, 1))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 16,
            messageBIndex = 17,
            expectedAlignments = setOf(
                Triple(16, 17, Pair(0, 0)),
                Triple(16, 17, Pair(2, 3)),
                Triple(16, 17, Pair(6, 7)),
                Triple(16, 17, Pair(3, 4)),
                Triple(16, 17, Pair(7, 8)),
                Triple(16, 17, Pair(4, 5)),
                Triple(16, 17, Pair(8, 9))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 16,
            messageBIndex = 18,
            expectedAlignments = setOf(
                Triple(16, 18, Pair(0, 0)),
                Triple(16, 18, Pair(2, 3)),
                Triple(16, 18, Pair(6, 6)),
                Triple(16, 18, Pair(3, 2)),
                Triple(16, 18, Pair(7, 5)),
                Triple(16, 18, Pair(4, 4)),
                Triple(16, 18, Pair(8, 7))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 17,
            messageBIndex = 18,
            expectedAlignments = setOf(
                Triple(17, 18, Pair(0, 0)),
                Triple(17, 18, Pair(3, 3)),
                Triple(17, 18, Pair(7, 6)),
                Triple(17, 18, Pair(4, 2)),
                Triple(17, 18, Pair(8, 5)),
                Triple(17, 18, Pair(5, 4)),
                Triple(17, 18, Pair(9, 7))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 8,
            messageBIndex = 9,
            expectedAlignments = setOf(
                Triple(8, 9, Pair(0, 0)),
                Triple(9, 8, Pair(1, 1)),
                Triple(8, 9, Pair(2, 2)),
                Triple(9, 8, Pair(3, 3)),
                Triple(8, 9, Pair(4, 4)),
                Triple(9, 8, Pair(5, 5)),
                Triple(8, 9, Pair(6, 6)),
                Triple(9, 8, Pair(7, 7)),
                Triple(8, 9, Pair(8, 8)),
                Triple(9, 8, Pair(9, 9)),
                Triple(8, 9, Pair(10, 10)),
                Triple(9, 8, Pair(11, 11)),
                Triple(8, 9, Pair(12, 12)),
                Triple(9, 8, Pair(13, 13)),
                Triple(8, 9, Pair(14, 14)),
                Triple(8, 9, Pair(15, 15)),
                Triple(9, 8, Pair(16, 16)),
                Triple(8, 9, Pair(26, 26)),
                Triple(9, 8, Pair(27, 27)),
                Triple(8, 9, Pair(25, 25)),
                Triple(9, 8, Pair(28, 28)),
                Triple(8, 9, Pair(29, 29)),
                Triple(9, 8, Pair(30, 30)),
                Triple(9, 8, Pair(23, 23)),
                Triple(8, 9, Pair(24, 24)),
                Triple(9, 8, Pair(22, 22)),
                Triple(9, 8, Pair(17, 17))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 8,
            messageBIndex = 10,
            expectedAlignments = setOf(
                Triple(8, 10, Pair(0, 0)),
                Triple(10, 8, Pair(1, 1)),
                Triple(8, 10, Pair(2, 2)),
                Triple(10, 8, Pair(3, 3)),
                Triple(8, 10, Pair(4, 4)),
                Triple(10, 8, Pair(5, 5)),
                Triple(8, 10, Pair(6, 6)),
                Triple(10, 8, Pair(7, 7)),
                Triple(8, 10, Pair(8, 8)),
                Triple(10, 8, Pair(9, 9)),
                Triple(8, 10, Pair(10, 10)),
                Triple(10, 8, Pair(11, 11)),
                Triple(8, 10, Pair(12, 12)),
                Triple(10, 8, Pair(13, 13)),
                Triple(8, 10, Pair(14, 14)),
                Triple(8, 10, Pair(15, 15)),
                Triple(10, 8, Pair(16, 16)),
                Triple(8, 10, Pair(26, 26)),
                Triple(10, 8, Pair(27, 27)),
                Triple(8, 10, Pair(25, 25)),
                Triple(10, 8, Pair(28, 28)),
                Triple(8, 10, Pair(29, 29)),
                Triple(10, 8, Pair(30, 30)),
                Triple(10, 8, Pair(23, 23)),
                Triple(8, 10, Pair(24, 24)),
                Triple(10, 8, Pair(22, 22)),
                Triple(10, 8, Pair(17, 17))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 8,
            messageBIndex = 11,
            expectedAlignments = setOf(
                Triple(8, 11, Pair(0, 0)),
                Triple(11, 8, Pair(1, 1)),
                Triple(8, 11, Pair(2, 2)),
                Triple(11, 8, Pair(3, 3)),
                Triple(8, 11, Pair(4, 4)),
                Triple(11, 8, Pair(5, 5)),
                Triple(8, 11, Pair(6, 6)),
                Triple(11, 8, Pair(7, 7)),
                Triple(8, 11, Pair(8, 8)),
                Triple(11, 8, Pair(9, 9)),
                Triple(8, 11, Pair(10, 10)),
                Triple(11, 8, Pair(11, 11)),
                Triple(8, 11, Pair(12, 12)),
                Triple(11, 8, Pair(13, 13)),
                Triple(8, 11, Pair(14, 14)),
                Triple(8, 11, Pair(15, 15)),
                Triple(11, 8, Pair(16, 16)),
                Triple(8, 11, Pair(26, 26)),
                Triple(11, 8, Pair(27, 27)),
                Triple(8, 11, Pair(25, 25)),
                Triple(11, 8, Pair(28, 28)),
                Triple(8, 11, Pair(29, 29)),
                Triple(11, 8, Pair(30, 30)),
                Triple(11, 8, Pair(23, 23)),
                Triple(8, 11, Pair(24, 24)),
                Triple(11, 8, Pair(22, 22)),
                Triple(11, 8, Pair(17, 17))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 9,
            messageBIndex = 10,
            expectedAlignments = setOf(
                Triple(9, 10, Pair(0, 0)),
                Triple(10, 9, Pair(1, 1)),
                Triple(9, 10, Pair(2, 2)),
                Triple(10, 9, Pair(3, 3)),
                Triple(9, 10, Pair(4, 4)),
                Triple(10, 9, Pair(5, 5)),
                Triple(9, 10, Pair(6, 6)),
                Triple(10, 9, Pair(7, 7)),
                Triple(9, 10, Pair(8, 8)),
                Triple(10, 9, Pair(9, 9)),
                Triple(9, 10, Pair(10, 10)),
                Triple(10, 9, Pair(11, 11)),
                Triple(9, 10, Pair(12, 12)),
                Triple(10, 9, Pair(13, 13)),
                Triple(9, 10, Pair(14, 14)),
                Triple(9, 10, Pair(15, 15)),
                Triple(10, 9, Pair(16, 16)),
                Triple(9, 10, Pair(26, 26)),
                Triple(10, 9, Pair(27, 27)),
                Triple(9, 10, Pair(25, 25)),
                Triple(10, 9, Pair(28, 28)),
                Triple(9, 10, Pair(29, 29)),
                Triple(10, 9, Pair(30, 30)),
                Triple(10, 9, Pair(23, 23)),
                Triple(9, 10, Pair(24, 24)),
                Triple(10, 9, Pair(22, 22)),
                Triple(10, 9, Pair(17, 17)),
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 9,
            messageBIndex = 11,
            expectedAlignments = setOf(
                Triple(9, 11, Pair(0, 0)),
                Triple(11, 9, Pair(1, 1)),
                Triple(9, 11, Pair(2, 2)),
                Triple(11, 9, Pair(3, 3)),
                Triple(9, 11, Pair(4, 4)),
                Triple(11, 9, Pair(5, 5)),
                Triple(9, 11, Pair(6, 6)),
                Triple(11, 9, Pair(7, 7)),
                Triple(9, 11, Pair(8, 8)),
                Triple(11, 9, Pair(9, 9)),
                Triple(9, 11, Pair(10, 10)),
                Triple(11, 9, Pair(11, 11)),
                Triple(9, 11, Pair(12, 12)),
                Triple(11, 9, Pair(13, 13)),
                Triple(9, 11, Pair(14, 14)),
                Triple(9, 11, Pair(15, 15)),
                Triple(11, 9, Pair(16, 16)),
                Triple(9, 11, Pair(26, 26)),
                Triple(11, 9, Pair(27, 27)),
                Triple(9, 11, Pair(25, 25)),
                Triple(11, 9, Pair(28, 28)),
                Triple(9, 11, Pair(29, 29)),
                Triple(11, 9, Pair(30, 30)),
                Triple(11, 9, Pair(23, 23)),
                Triple(9, 11, Pair(24, 24)),
                Triple(11, 9, Pair(22, 22)),
                Triple(11, 9, Pair(17, 17)),
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 10,
            messageBIndex = 11,
            expectedAlignments = setOf(
                Triple(10, 11, Pair(0, 0)),
                Triple(11, 10, Pair(1, 1)),
                Triple(10, 11, Pair(2, 2)),
                Triple(11, 10, Pair(3, 3)),
                Triple(10, 11, Pair(4, 4)),
                Triple(11, 10, Pair(5, 5)),
                Triple(10, 11, Pair(6, 6)),
                Triple(11, 10, Pair(7, 7)),
                Triple(10, 11, Pair(8, 8)),
                Triple(11, 10, Pair(9, 9)),
                Triple(10, 11, Pair(10, 10)),
                Triple(11, 10, Pair(11, 11)),
                Triple(10, 11, Pair(12, 12)),
                Triple(11, 10, Pair(13, 13)),
                Triple(10, 11, Pair(14, 14)),
                Triple(10, 11, Pair(15, 15)),
                Triple(11, 10, Pair(16, 16)),
                Triple(10, 11, Pair(26, 26)),
                Triple(11, 10, Pair(27, 27)),
                Triple(10, 11, Pair(25, 25)),
                Triple(11, 10, Pair(28, 28)),
                Triple(10, 11, Pair(29, 29)),
                Triple(11, 10, Pair(30, 30)),
                Triple(11, 10, Pair(23, 23)),
                Triple(10, 11, Pair(24, 24)),
                Triple(11, 10, Pair(22, 22)),
                Triple(11, 10, Pair(17, 17))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 12,
            messageBIndex = 13,
            expectedAlignments = setOf(
                Triple(12, 13, Pair(0, 0)),
                Triple(13, 12, Pair(1, 1)),
                Triple(12, 13, Pair(2, 2)),
                Triple(13, 12, Pair(3, 3)),
                Triple(12, 13, Pair(4, 4)),
                Triple(13, 12, Pair(5, 5)),
                Triple(12, 13, Pair(6, 6)),
                Triple(13, 12, Pair(7, 7)),
                Triple(12, 13, Pair(8, 8)),
                Triple(13, 12, Pair(9, 9)),
                Triple(12, 13, Pair(10, 10)),
                Triple(13, 12, Pair(11, 11)),
                Triple(12, 13, Pair(12, 12)),
                Triple(13, 12, Pair(13, 13)),
                Triple(12, 13, Pair(14, 14)),
                Triple(13, 12, Pair(23, 25)),
                Triple(12, 13, Pair(26, 24)),
                Triple(13, 12, Pair(25, 27)),
                Triple(12, 13, Pair(28, 26)),
                Triple(13, 12, Pair(27, 29)),
                Triple(12, 13, Pair(31, 29)),
                Triple(13, 12, Pair(28, 30)),
                Triple(12, 13, Pair(32, 30)),
                Triple(13, 12, Pair(31, 33)),
                Triple(12, 13, Pair(21, 19)),
                Triple(13, 12, Pair(20, 22)),
                Triple(12, 13, Pair(23, 21)),
                Triple(13, 12, Pair(22, 24))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 12,
            messageBIndex = 14,
            expectedAlignments = setOf(
                Triple(12, 14, Pair(0, 0)),
                Triple(14, 12, Pair(1, 1)),
                Triple(12, 14, Pair(2, 2)),
                Triple(14, 12, Pair(3, 3)),
                Triple(12, 14, Pair(4, 4)),
                Triple(14, 12, Pair(5, 5)),
                Triple(12, 14, Pair(6, 6)),
                Triple(14, 12, Pair(7, 7)),
                Triple(12, 14, Pair(8, 8)),
                Triple(14, 12, Pair(9, 9)),
                Triple(12, 14, Pair(10, 10)),
                Triple(14, 12, Pair(11, 11)),
                Triple(12, 14, Pair(12, 12)),
                Triple(14, 12, Pair(13, 13)),
                Triple(12, 14, Pair(14, 14)),
                Triple(14, 12, Pair(25, 25)),
                Triple(12, 14, Pair(26, 26)),
                Triple(14, 12, Pair(27, 27)),
                Triple(12, 14, Pair(28, 28)),
                Triple(14, 12, Pair(29, 29)),
                Triple(12, 14, Pair(31, 31)),
                Triple(14, 12, Pair(30, 30)),
                Triple(12, 14, Pair(32, 32)),
                Triple(14, 12, Pair(33, 33)),
                Triple(12, 14, Pair(21, 21)),
                Triple(14, 12, Pair(22, 22)),
                Triple(12, 14, Pair(23, 23)),
                Triple(14, 12, Pair(24, 24))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 12,
            messageBIndex = 15,
            expectedAlignments = setOf(
                Triple(12, 15, Pair(0, 0)),
                Triple(15, 12, Pair(1, 1)),
                Triple(12, 15, Pair(2, 2)),
                Triple(15, 12, Pair(3, 3)),
                Triple(12, 15, Pair(4, 4)),
                Triple(15, 12, Pair(5, 5)),
                Triple(12, 15, Pair(6, 6)),
                Triple(15, 12, Pair(7, 7)),
                Triple(12, 15, Pair(8, 8)),
                Triple(15, 12, Pair(9, 9)),
                Triple(12, 15, Pair(10, 10)),
                Triple(15, 12, Pair(11, 11)),
                Triple(12, 15, Pair(12, 12)),
                Triple(15, 12, Pair(13, 13)),
                Triple(12, 15, Pair(14, 14)),
                Triple(15, 12, Pair(25, 25)),
                Triple(12, 15, Pair(26, 26)),
                Triple(15, 12, Pair(27, 27)),
                Triple(12, 15, Pair(28, 28)),
                Triple(15, 12, Pair(29, 29)),
                Triple(12, 15, Pair(31, 31)),
                Triple(15, 12, Pair(30, 30)),
                Triple(12, 15, Pair(32, 32)),
                Triple(15, 12, Pair(33, 33)),
                Triple(12, 15, Pair(21, 21)),
                Triple(15, 12, Pair(22, 22)),
                Triple(12, 15, Pair(23, 23)),
                Triple(15, 12, Pair(24, 24))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 13,
            messageBIndex = 14,
            expectedAlignments = setOf(
                Triple(13, 14, Pair(0, 0)),
                Triple(14, 13, Pair(1, 1)),
                Triple(13, 14, Pair(2, 2)),
                Triple(14, 13, Pair(3, 3)),
                Triple(13, 14, Pair(4, 4)),
                Triple(14, 13, Pair(5, 5)),
                Triple(13, 14, Pair(6, 6)),
                Triple(14, 13, Pair(7, 7)),
                Triple(13, 14, Pair(8, 8)),
                Triple(14, 13, Pair(9, 9)),
                Triple(13, 14, Pair(10, 10)),
                Triple(14, 13, Pair(11, 11)),
                Triple(13, 14, Pair(12, 12)),
                Triple(14, 13, Pair(13, 13)),
                Triple(13, 14, Pair(14, 14)),
                Triple(14, 13, Pair(25, 23)),
                Triple(13, 14, Pair(24, 26)),
                Triple(14, 13, Pair(27, 25)),
                Triple(13, 14, Pair(26, 28)),
                Triple(14, 13, Pair(29, 27)),
                Triple(13, 14, Pair(29, 31)),
                Triple(14, 13, Pair(30, 28)),
                Triple(13, 14, Pair(30, 32)),
                Triple(14, 13, Pair(33, 31)),
                Triple(13, 14, Pair(19, 21)),
                Triple(14, 13, Pair(22, 20)),
                Triple(13, 14, Pair(21, 23)),
                Triple(14, 13, Pair(24, 22))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 13,
            messageBIndex = 15,
            expectedAlignments = setOf(
                Triple(13, 15, Pair(0, 0)),
                Triple(15, 13, Pair(1, 1)),
                Triple(13, 15, Pair(2, 2)),
                Triple(15, 13, Pair(3, 3)),
                Triple(13, 15, Pair(4, 4)),
                Triple(15, 13, Pair(5, 5)),
                Triple(13, 15, Pair(6, 6)),
                Triple(15, 13, Pair(7, 7)),
                Triple(13, 15, Pair(8, 8)),
                Triple(15, 13, Pair(9, 9)),
                Triple(13, 15, Pair(10, 10)),
                Triple(15, 13, Pair(11, 11)),
                Triple(13, 15, Pair(12, 12)),
                Triple(15, 13, Pair(13, 13)),
                Triple(13, 15, Pair(14, 14)),
                Triple(15, 13, Pair(25, 23)),
                Triple(13, 15, Pair(24, 26)),
                Triple(15, 13, Pair(27, 25)),
                Triple(13, 15, Pair(26, 28)),
                Triple(15, 13, Pair(29, 27)),
                Triple(13, 15, Pair(29, 31)),
                Triple(15, 13, Pair(30, 28)),
                Triple(13, 15, Pair(30, 32)),
                Triple(15, 13, Pair(33, 31)),
                Triple(13, 15, Pair(19, 21)),
                Triple(15, 13, Pair(22, 20)),
                Triple(13, 15, Pair(21, 23)),
                Triple(15, 13, Pair(24, 22))
            )
        ),
        SequenceAlignmentTest(
            messageAIndex = 14,
            messageBIndex = 15,
            expectedAlignments = setOf(
                Triple(14, 15, Pair(0, 0)),
                Triple(15, 14, Pair(1, 1)),
                Triple(14, 15, Pair(2, 2)),
                Triple(15, 14, Pair(3, 3)),
                Triple(14, 15, Pair(4, 4)),
                Triple(15, 14, Pair(5, 5)),
                Triple(14, 15, Pair(6, 6)),
                Triple(15, 14, Pair(7, 7)),
                Triple(14, 15, Pair(8, 8)),
                Triple(15, 14, Pair(9, 9)),
                Triple(14, 15, Pair(10, 10)),
                Triple(15, 14, Pair(11, 11)),
                Triple(14, 15, Pair(12, 12)),
                Triple(15, 14, Pair(13, 13)),
                Triple(14, 15, Pair(14, 14)),
                Triple(15, 14, Pair(25, 25)),
                Triple(14, 15, Pair(26, 26)),
                Triple(15, 14, Pair(27, 27)),
                Triple(14, 15, Pair(28, 28)),
                Triple(15, 14, Pair(29, 29)),
                Triple(14, 15, Pair(31, 31)),
                Triple(15, 14, Pair(30, 30)),
                Triple(14, 15, Pair(32, 32)),
                Triple(15, 14, Pair(33, 33)),
                Triple(14, 15, Pair(21, 21)),
                Triple(15, 14, Pair(22, 22)),
                Triple(14, 15, Pair(23, 23)),
                Triple(15, 14, Pair(24, 24))
            )
        ),
    )
}