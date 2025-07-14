package ParserTests

import bitmage.fromHex
import decoders.SwiftSegFinder.SSFField
import decoders.SwiftSegFinder.SSFSegment

object TestMessageSamples {
    val testMessages = listOf(
        TestMessage(0,
            "081611b892473a80d6c641".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),
                SSFSegment(2, SSFField.UNKNOWN)
            )
        ),
        TestMessage(1,
            "08031163b719da7fd6c641".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),
                SSFSegment(2, SSFField.UNKNOWN)
            )
        ),
        TestMessage(2,
            "080b11c80664df7fd6c641".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),
                SSFSegment(2, SSFField.UNKNOWN)
            )
        ),
        TestMessage(3,
            "3081D0308183020100300F310D300B06035504030C0474657374302A300506032B6570032100FB16E6BD645FB03D755D0C207042BF80AA7CBA385BECDB9C19FCFE0BC95B1898A041303F06092A864886F70D01090E31323030302E0603551D1104273025A023060A2B060104018237140203A0150C136164647265737340646F6D61696E2E74657374300506032B6570034100529E457A71C5D6B67344653EEF0885FBF0F56DFC83445D1DCD6CF6B25E389E5B6EF222E31CEDDA21F393616A6A66568383506ADCBEC571BEC87F8C9902C1390B".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),
                SSFSegment(1, SSFField.UNKNOWN),
                SSFSegment(3, SSFField.UNKNOWN),
                SSFSegment(4, SSFField.UNKNOWN),
                SSFSegment(6, SSFField.UNKNOWN),
                SSFSegment(7, SSFField.UNKNOWN),
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(9, SSFField.UNKNOWN),
                SSFSegment(10, SSFField.UNKNOWN),
                SSFSegment(11, SSFField.UNKNOWN),
                SSFSegment(12, SSFField.UNKNOWN),
                SSFSegment(13, SSFField.UNKNOWN),
                SSFSegment(14, SSFField.UNKNOWN),
                SSFSegment(15, SSFField.UNKNOWN),
                SSFSegment(16, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.UNKNOWN),
                SSFSegment(20, SSFField.UNKNOWN),
                SSFSegment(21, SSFField.UNKNOWN),
                SSFSegment(22, SSFField.UNKNOWN),
                SSFSegment(26, SSFField.UNKNOWN),
                SSFSegment(27, SSFField.UNKNOWN),
                SSFSegment(28, SSFField.UNKNOWN),
                SSFSegment(29, SSFField.UNKNOWN),
                SSFSegment(30, SSFField.UNKNOWN),
                SSFSegment(31, SSFField.UNKNOWN),
                SSFSegment(32, SSFField.UNKNOWN),
                SSFSegment(35, SSFField.UNKNOWN),
                SSFSegment(37, SSFField.UNKNOWN),
                SSFSegment(70, SSFField.UNKNOWN),
                SSFSegment(71, SSFField.UNKNOWN),
                SSFSegment(72, SSFField.UNKNOWN),
                SSFSegment(73, SSFField.UNKNOWN),
                SSFSegment(74, SSFField.UNKNOWN),
                SSFSegment(75, SSFField.UNKNOWN),
                SSFSegment(76, SSFField.UNKNOWN),
                SSFSegment(85, SSFField.UNKNOWN),
                SSFSegment(86, SSFField.UNKNOWN),
                SSFSegment(87, SSFField.UNKNOWN),
                SSFSegment(88, SSFField.UNKNOWN),
                SSFSegment(89, SSFField.UNKNOWN),
                SSFSegment(90, SSFField.UNKNOWN),
                SSFSegment(91, SSFField.UNKNOWN),
                SSFSegment(92, SSFField.UNKNOWN),
                SSFSegment(93, SSFField.UNKNOWN),
                SSFSegment(96, SSFField.UNKNOWN),
                SSFSegment(98, SSFField.UNKNOWN),
                SSFSegment(99, SSFField.UNKNOWN),
                SSFSegment(100, SSFField.UNKNOWN),
                SSFSegment(101, SSFField.UNKNOWN),
                SSFSegment(102, SSFField.UNKNOWN),
                SSFSegment(103, SSFField.UNKNOWN),
                SSFSegment(104, SSFField.UNKNOWN),
                SSFSegment(114, SSFField.UNKNOWN),
                SSFSegment(115, SSFField.UNKNOWN),
                SSFSegment(116, SSFField.UNKNOWN),
                SSFSegment(117, SSFField.UNKNOWN),
                SSFSegment(118, SSFField.UNKNOWN),
                SSFSegment(137, SSFField.UNKNOWN),
                SSFSegment(138, SSFField.UNKNOWN),
                SSFSegment(139, SSFField.UNKNOWN),
                SSFSegment(140, SSFField.UNKNOWN),
                SSFSegment(141, SSFField.UNKNOWN),
                SSFSegment(144, SSFField.UNKNOWN),
                SSFSegment(146, SSFField.UNKNOWN)
            )
        ),
        TestMessage(4,
            "0821b10132010c3f2f5cd941da0104080010009a02bf010a3b636f6d2e6170706c652e74656c6570686f6e797574696c69746965732e63616c6c7365727669636573642e4661636554696d6550726f766964657212084661636554696d651a6466696c653a2f2f2f707269766174652f7661722f636f6e7461696e6572732f42756e646c652f4170706c69636174696f6e2f30323639344631412d303138312d343031342d423036342d4536303938333636464431342f4661636554696d652e6170702f2002280130013801400048006803680278019a02de010a17636f6d2e6170706c652e636f726574656c6570686f6e79200228053001380040014801680278018a0107080212033131328a0107080212033131308a0107080212033131328a0107080212033131308a0107080212033931318a0107080212033131328a0107080212033030308a01060802120230388a0107080212033131308a0107080212033939398a0107080212033131388a0107080212033131398a0107080212033132308a0107080212033132328a0107080212033931318a0107080212033131328a0108080212042a3931318a010808021204233931319a02450a31636f6d2e6170706c652e74656c6570686f6e797574696c69746965732e63616c6c7365727669636573642e54696e43616e200128013001380040004800680368027800".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),
                SSFSegment(2, SSFField.UNKNOWN),
                SSFSegment(12, SSFField.UNKNOWN),
                SSFSegment(15, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.UNKNOWN),
                SSFSegment(19, SSFField.UNKNOWN),
                SSFSegment(23, SSFField.UNKNOWN),
                SSFSegment(84, SSFField.UNKNOWN),
                SSFSegment(94, SSFField.UNKNOWN),
                SSFSegment(196, SSFField.UNKNOWN),
                SSFSegment(198, SSFField.UNKNOWN),
                SSFSegment(200, SSFField.UNKNOWN),
                SSFSegment(202, SSFField.UNKNOWN),
                SSFSegment(204, SSFField.UNKNOWN),
                SSFSegment(206, SSFField.UNKNOWN),
                SSFSegment(208, SSFField.UNKNOWN),
                SSFSegment(210, SSFField.UNKNOWN),
                SSFSegment(212, SSFField.UNKNOWN),
                SSFSegment(214, SSFField.UNKNOWN),
                SSFSegment(218, SSFField.UNKNOWN),
                SSFSegment(243, SSFField.UNKNOWN),
                SSFSegment(245, SSFField.UNKNOWN),
                SSFSegment(247, SSFField.UNKNOWN),
                SSFSegment(249, SSFField.UNKNOWN),
                SSFSegment(251, SSFField.UNKNOWN),
                SSFSegment(253, SSFField.UNKNOWN),
                SSFSegment(255, SSFField.UNKNOWN),
                SSFSegment(257, SSFField.UNKNOWN),
                SSFSegment(259, SSFField.UNKNOWN),
                SSFSegment(262, SSFField.UNKNOWN),
                SSFSegment(264, SSFField.UNKNOWN),
                SSFSegment(269, SSFField.UNKNOWN),
                SSFSegment(272, SSFField.UNKNOWN),
                SSFSegment(274, SSFField.UNKNOWN),
                SSFSegment(279, SSFField.UNKNOWN),
                SSFSegment(282, SSFField.UNKNOWN),
                SSFSegment(284, SSFField.UNKNOWN),
                SSFSegment(289, SSFField.UNKNOWN),
                SSFSegment(292, SSFField.UNKNOWN),
                SSFSegment(294, SSFField.UNKNOWN),
                SSFSegment(299, SSFField.UNKNOWN),
                SSFSegment(302, SSFField.UNKNOWN),
                SSFSegment(304, SSFField.UNKNOWN),
                SSFSegment(309, SSFField.UNKNOWN),
                SSFSegment(312, SSFField.UNKNOWN),
                SSFSegment(314, SSFField.UNKNOWN),
                SSFSegment(319, SSFField.UNKNOWN),
                SSFSegment(322, SSFField.UNKNOWN),
                SSFSegment(324, SSFField.UNKNOWN),
                SSFSegment(329, SSFField.UNKNOWN),
                SSFSegment(332, SSFField.UNKNOWN),
                SSFSegment(334, SSFField.UNKNOWN),
                SSFSegment(338, SSFField.UNKNOWN),
                SSFSegment(341, SSFField.UNKNOWN),
                SSFSegment(343, SSFField.UNKNOWN),
                SSFSegment(348, SSFField.UNKNOWN),
                SSFSegment(351, SSFField.UNKNOWN),
                SSFSegment(353, SSFField.UNKNOWN),
                SSFSegment(358, SSFField.UNKNOWN),
                SSFSegment(361, SSFField.UNKNOWN),
                SSFSegment(363, SSFField.UNKNOWN),
                SSFSegment(368, SSFField.UNKNOWN),
                SSFSegment(371, SSFField.UNKNOWN),
                SSFSegment(373, SSFField.UNKNOWN),
                SSFSegment(378, SSFField.UNKNOWN),
                SSFSegment(381, SSFField.UNKNOWN),
                SSFSegment(383, SSFField.UNKNOWN),
                SSFSegment(388, SSFField.UNKNOWN),
                SSFSegment(391, SSFField.UNKNOWN),
                SSFSegment(393, SSFField.UNKNOWN),
                SSFSegment(398, SSFField.UNKNOWN),
                SSFSegment(401, SSFField.UNKNOWN),
                SSFSegment(403, SSFField.UNKNOWN),
                SSFSegment(408, SSFField.UNKNOWN),
                SSFSegment(411, SSFField.UNKNOWN),
                SSFSegment(413, SSFField.UNKNOWN),
                SSFSegment(418, SSFField.UNKNOWN),
                SSFSegment(421, SSFField.UNKNOWN),
                SSFSegment(423, SSFField.UNKNOWN),
                SSFSegment(429, SSFField.UNKNOWN),
                SSFSegment(432, SSFField.UNKNOWN),
                SSFSegment(434, SSFField.UNKNOWN),
                SSFSegment(440, SSFField.UNKNOWN),
                SSFSegment(443, SSFField.UNKNOWN),
                SSFSegment(494, SSFField.UNKNOWN),
                SSFSegment(496, SSFField.UNKNOWN),
                SSFSegment(498, SSFField.UNKNOWN),
                SSFSegment(500, SSFField.UNKNOWN),
                SSFSegment(502, SSFField.UNKNOWN),
                SSFSegment(504, SSFField.UNKNOWN),
                SSFSegment(506, SSFField.UNKNOWN),
                SSFSegment(508, SSFField.UNKNOWN),
                SSFSegment(510, SSFField.UNKNOWN)
            )
        ),
        TestMessage(5,
            "62706c6973743137a0d0000000000000007f114870726564696374696f6e466f7243617465676f726965733a636f6e73756d65723a63726974657269613a6c696d69743a70726f7669646573466565646261636b3a7265706c793a007f111b76363040303a38513136513234403332513430423438403f353200a0d00000000000000011061101d0cc000000000000007724636c617373007d4e5344696374696f6e61727900784e532e6b65797300a0b7000000000000007b4e532e6f626a6563747300a0cc000000000000001101b0e0".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING), // BPLIST
                SSFSegment(6, SSFField.STRING),  // version
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.UNKNOWN), // predictionFor... type
                SSFSegment(19, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN), // predictionFor... length
                SSFSegment(20, SSFField.STRING), // predictionFor... text
                SSFSegment(92, SSFField.UNKNOWN), // v60@0:8Q16Q24@32Q40B48@?52 type
                SSFSegment(94, SSFField.PAYLOAD_LENGTH_BIG_ENDIAN), // v60@0:8Q16Q24@32Q40B48@?52 length
                SSFSegment(95, SSFField.STRING), // v60@0:8Q16Q24@32Q40B48@?52 text
                SSFSegment(122, SSFField.UNKNOWN),
                SSFSegment(131, SSFField.UNKNOWN), // 6 type
                SSFSegment(132, SSFField.UNKNOWN), // 6
                SSFSegment(133, SSFField.UNKNOWN), // 1 type
                SSFSegment(134, SSFField.UNKNOWN), // 1
                SSFSegment(135, SSFField.UNKNOWN),
                SSFSegment(144, SSFField.UNKNOWN), // class type
                SSFSegment(145, SSFField.STRING), // class
                SSFSegment(152, SSFField.UNKNOWN), // NSDIRECTONARY type
                SSFSegment(153, SSFField.STRING), // NSDIRECTONARY
                SSFSegment(166, SSFField.UNKNOWN), // NS.keys type
                SSFSegment(167, SSFField.STRING), // NS.keys
                SSFSegment(175, SSFField.UNKNOWN),
                SSFSegment(184, SSFField.UNKNOWN), // NS.objects type
                SSFSegment(185, SSFField.STRING), // NS.objects
                SSFSegment(196, SSFField.UNKNOWN),
                SSFSegment(205, SSFField.UNKNOWN), // 1 type
                SSFSegment(206, SSFField.UNKNOWN), // 1
                SSFSegment(207, SSFField.UNKNOWN), // true
                SSFSegment(208, SSFField.UNKNOWN) // end
            )
        ),
        TestMessage(6,
            "62706c697374313513c7000000000000801200000000a44776403a404040004f10ce61636b6e6f776c656467654f7574676f696e674d65737361676557697468475549443a616c7465726e61746543616c6c6261636b49443a666f724163636f756e7457697468556e6971756549443aa3108710881087a37f10a432303841303836302d373535392d343731362d424136352d434332363638334644413235407f10a436433836343332372d364441452d344133312d394137362d3731434135454533363839300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING), // bplist
                SSFSegment(6, SSFField.STRING), // version
                SSFSegment(8, SSFField.UNKNOWN), // bplist
                SSFSegment(22, SSFField.UNKNOWN),
                SSFSegment(24, SSFField.STRING), // v@:@@@
                SSFSegment(31, SSFField.UNKNOWN),
                SSFSegment(34, SSFField.UNKNOWN),
                SSFSegment(112, SSFField.UNKNOWN), // array
                SSFSegment(113, SSFField.UNKNOWN), // 7
                SSFSegment(115, SSFField.UNKNOWN), // 8
                SSFSegment(117, SSFField.UNKNOWN), // 7
                SSFSegment(119, SSFField.UNKNOWN), // array
                SSFSegment(120, SSFField.UNKNOWN), // 208A0860-7559-4716-BA65-CC26683FDA25
                SSFSegment(159, SSFField.UNKNOWN), // 0x
                SSFSegment(160, SSFField.UNKNOWN), // 6C864327-6DAE-4A31-9A76-71CA5EE36890
                SSFSegment(199, SSFField.UNKNOWN) // end
            )
        ),
        TestMessage(7,
            "62706c69737431351367000000000000801200000000d478636c69656e744944756576656e747c70726f636573732d6e616d657973686f756c644c6f6710977e55494b69742d4b6579626f6172647f109156697375616c50616972696e674861636b74506f737400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING), // bplist
                SSFSegment(6, SSFField.STRING), // version
                SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(22, SSFField.UNKNOWN), // array
                SSFSegment(23, SSFField.UNKNOWN), // clientID type
                SSFSegment(24, SSFField.STRING), // clientID
                SSFSegment(32, SSFField.UNKNOWN), // event type
                SSFSegment(33, SSFField.STRING), // event
                SSFSegment(38, SSFField.UNKNOWN), // process-name type
                SSFSegment(39, SSFField.STRING), // process-name
                SSFSegment(51, SSFField.UNKNOWN), // shouldLog type
                SSFSegment(52, SSFField.STRING), // shouldLog
                SSFSegment(61, SSFField.UNKNOWN), // 23
                SSFSegment(63, SSFField.UNKNOWN), // UIKit-Keyboard type
                SSFSegment(64, SSFField.STRING), // UIKit-Keyboard type
                SSFSegment(78, SSFField.UNKNOWN), // VisualPairingHack type
                SSFSegment(81, SSFField.STRING), // VisualPairingHack
                SSFSegment(98, SSFField.UNKNOWN), // Post type
                SSFSegment(99, SSFField.STRING), // Post
                SSFSegment(103, SSFField.UNKNOWN) // end
            )
        ),
        TestMessage(8,
            "86A26964CD0FC5B27375636365737350726F626162696C697479CB3FE0000000000000AA6973456C696769626C65C3A46E616D65AA4672616E7A204B61726CAA686967686C696768747392B55363686F6C61727368697020726563697069656E74B852656C6576616E7420776F726B20657870657269656E6365AA6174747269627574657382A6766973696F6ECB3FECCCCCCCCCCCCDAB6174686C6574696369736DCB3FD3333333333333".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),               // 86 (map with 6 entries)
                SSFSegment(1, SSFField.UNKNOWN),            // A2 69 64 -> "id" type
                SSFSegment(2, SSFField.STRING),            // A2 69 64 -> "id"
                SSFSegment(4, SSFField.UNKNOWN),            // CD 0F C5 -> 4037 type
                SSFSegment(5, SSFField.UNKNOWN),            // CD 0F C5 -> 4037
                SSFSegment(7, SSFField.UNKNOWN),            // B2 ... -> "successProbability" type
                SSFSegment(8, SSFField.STRING),            // B2 ... -> "successProbability"
                SSFSegment(26, SSFField.UNKNOWN),           // CB 3F E0 00 00 00 00 00 00 -> 0.5
                SSFSegment(35, SSFField.UNKNOWN),           // AA ... -> "isEligible" type
                SSFSegment(36, SSFField.STRING),           // AA ... -> "isEligible"
                SSFSegment(46, SSFField.UNKNOWN),          // C3 -> true
                SSFSegment(47, SSFField.UNKNOWN),           // A4 ... -> "name" type
                SSFSegment(48, SSFField.STRING),           // A4 ... -> "name"
                SSFSegment(52, SSFField.UNKNOWN),           // AA ... -> "Franz Karl" type
                SSFSegment(53, SSFField.STRING),           // AA ... -> "Franz Karl"
                SSFSegment(63, SSFField.UNKNOWN),           // AA ... -> "highlights" type
                SSFSegment(64, SSFField.STRING),           // AA ... -> "highlights"
                SSFSegment(74, SSFField.UNKNOWN),            // 92 -> array of 2 strings
                SSFSegment(75, SSFField.UNKNOWN),           // B5 ... -> "Scholarship recipient" type
                SSFSegment(76, SSFField.STRING),           // B5 ... -> "Scholarship recipient"
                SSFSegment(97, SSFField.UNKNOWN),           // B8 ... -> "Relevant work experience" type
                SSFSegment(98, SSFField.STRING),           // B8 ... -> "Relevant work experience"
                SSFSegment(122, SSFField.UNKNOWN),          // AA ... -> "attributes" type
                SSFSegment(123, SSFField.STRING),          // AA ... -> "attributes"
                SSFSegment(133, SSFField.UNKNOWN),             // 82 -> map with 2 entries
                SSFSegment(134, SSFField.UNKNOWN),          // A6 ... -> "vision" type
                SSFSegment(135, SSFField.STRING),          // A6 ... -> "vision"
                SSFSegment(141, SSFField.UNKNOWN),          // CB ... -> 0.9
                SSFSegment(150, SSFField.UNKNOWN),          // AB ... -> "athleticism" type
                SSFSegment(151, SSFField.STRING),          // AB ... -> "athleticism"
                SSFSegment(162, SSFField.UNKNOWN)           // CB ... -> 0.3
            )
        ),
        TestMessage(9,
            "86A26964CD0FC6B27375636365737350726F626162696C697479CB3FEA3D70A3D70A3DAA6973456C696769626C65C3A46E616D65AD416D69726120536F6C62657267AA686967686C696768747392B8496E7465726E6174696F6E616C20696E7465726E73686970B25075626C6973686564207265736561726368AA6174747269627574657382A6766973696F6E01AB6174686C6574696369736DCB3FE6666666666666".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),               // 86 (map mit 6 Einträgen)
                SSFSegment(1, SSFField.UNKNOWN),            // A2 69 64 → "id" type
                SSFSegment(2, SSFField.STRING),            // A2 69 64 → "id"
                SSFSegment(4, SSFField.UNKNOWN),            // CD 0F C6 → 4038 type
                SSFSegment(5, SSFField.UNKNOWN),            // CD 0F C6 → 4038
                SSFSegment(7, SSFField.UNKNOWN),            // B2 ... → "successProbability" type
                SSFSegment(8, SSFField.STRING),            // B2 ... → "successProbability"
                SSFSegment(26, SSFField.UNKNOWN),           // CB ... → 0.82
                SSFSegment(35, SSFField.UNKNOWN),           // AA ... → "isEligible" type
                SSFSegment(36, SSFField.STRING),           // AA ... → "isEligible"
                SSFSegment(46, SSFField.UNKNOWN),          // C3 → true
                SSFSegment(47, SSFField.UNKNOWN),           // A4 ... → "name" type
                SSFSegment(48, SSFField.STRING),           // A4 ... → "name"
                SSFSegment(52, SSFField.UNKNOWN),           // AD ... → "Amira Solberg" type
                SSFSegment(53, SSFField.STRING),           // AD ... → "Amira Solberg"
                SSFSegment(66, SSFField.UNKNOWN),           // AA ... → "highlights" type
                SSFSegment(67, SSFField.STRING),           // AA ... → "highlights"
                SSFSegment(77, SSFField.UNKNOWN),            // 92 → Array mit 2 Strings
                SSFSegment(78, SSFField.UNKNOWN),           // B8 ... → "International internship" type
                SSFSegment(79, SSFField.STRING),           // B8 ... → "International internship"
                SSFSegment(103, SSFField.UNKNOWN),          // B2 ... → "Published research" type
                SSFSegment(104, SSFField.STRING),          // B2 ... → "Published research"
                SSFSegment(122, SSFField.UNKNOWN),          // AA ... → "attributes" type
                SSFSegment(123, SSFField.STRING),          // AA ... → "attributes"
                SSFSegment(133, SSFField.UNKNOWN),             // 82 → Map mit 2 Einträgen
                SSFSegment(134, SSFField.UNKNOWN),          // A6 ... → "vision" type
                SSFSegment(135, SSFField.STRING),          // A6 ... → "vision"
                SSFSegment(141, SSFField.UNKNOWN),           // 01 → 1
                SSFSegment(142, SSFField.UNKNOWN),          // AB ... → "athleticism" type
                SSFSegment(143, SSFField.STRING),          // AB ... → "athleticism"
                SSFSegment(154, SSFField.UNKNOWN)           // CB ... → 0.7
            )
        ),
        TestMessage(10,
            "86A26964CD0FC7B27375636365737350726F626162696C697479CB3FD6666666666666AA6973456C696769626C65C2A46E616D65AD4A6F6E61732052696368746572AA686967686C696768747392AE566F6C756E7465657220776F726BB842617369632070726F6772616D6D696E6720736B696C6C73AA6174747269627574657382A6766973696F6ECB3FE3333333333333AB6174686C6574696369736DCB3FE999999999999A".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),               // 86 (map mit 6 Einträgen)
                SSFSegment(1, SSFField.UNKNOWN),               // A2 69 64 → "id" type
                SSFSegment(2, SSFField.STRING),                // A2 69 64 → "id"
                SSFSegment(4, SSFField.UNKNOWN),               // CD 0F C7 → 4039 type
                SSFSegment(5, SSFField.UNKNOWN),               // CD 0F C7 → 4039
                SSFSegment(7, SSFField.UNKNOWN),               // B2 ... → "successProbability" type
                SSFSegment(8, SSFField.STRING),                // B2 ... → "successProbability"
                SSFSegment(26, SSFField.UNKNOWN),              // CB ... → 0.35
                SSFSegment(35, SSFField.UNKNOWN),              // AA ... → "isEligible" type
                SSFSegment(36, SSFField.STRING),               // AA ... → "isEligible"
                SSFSegment(46, SSFField.UNKNOWN),              // C2 → false
                SSFSegment(47, SSFField.UNKNOWN),              // A4 ... → "name" type
                SSFSegment(48, SSFField.STRING),               // A4 ... → "name"
                SSFSegment(52, SSFField.UNKNOWN),              // AD ... → "Jonas Richter" type
                SSFSegment(53, SSFField.STRING),               // AD ... → "Jonas Richter"
                SSFSegment(66, SSFField.UNKNOWN),              // AA ... → "highlights" type
                SSFSegment(67, SSFField.STRING),               // AA ... → "highlights"
                SSFSegment(77, SSFField.UNKNOWN),              // 92 → array mit 2 Strings
                SSFSegment(78, SSFField.UNKNOWN),              // AE ... → "Volunteer work" type
                SSFSegment(79, SSFField.STRING),               // AE ... → "Volunteer work"
                SSFSegment(93, SSFField.UNKNOWN),              // B8 ... → "Basic programming skills" type
                SSFSegment(94, SSFField.STRING),               // B8 ... → "Basic programming skills"
                SSFSegment(118, SSFField.UNKNOWN),             // AA ... → "attributes" type
                SSFSegment(119, SSFField.STRING),              // AA ... → "attributes"
                SSFSegment(129, SSFField.UNKNOWN),             // 82 → map mit 2 Einträgen
                SSFSegment(130, SSFField.UNKNOWN),             // A6 ... → "vision" type
                SSFSegment(131, SSFField.STRING),              // A6 ... → "vision"
                SSFSegment(137, SSFField.UNKNOWN),             // CB ... → 0.6
                SSFSegment(146, SSFField.UNKNOWN),             // AB ... → "athleticism" type
                SSFSegment(147, SSFField.STRING),              // AB ... → "athleticism"
                SSFSegment(158, SSFField.UNKNOWN)              // CB ... → 0.8
            )
        ),
        TestMessage(11,
            "86A26964CD0FC8B27375636365737350726F626162696C697479CB3FEE147AE147AE14AA6973456C696769626C65C3A46E616D65A94C696E61204368656EAA686967686C696768747392B0546F70206F662068657220636C617373B04C656164657273686970206177617264AA6174747269627574657382A6766973696F6ECB3FEE666666666666AB6174686C6574696369736DCB3FE0000000000000".fromHex(),
            listOf(
                SSFSegment(0, SSFField.UNKNOWN),               // 86 → map mit 6 Einträgen
                SSFSegment(1, SSFField.UNKNOWN),               // A2 69 64 → "id" type
                SSFSegment(2, SSFField.STRING),                // A2 69 64 → "id"
                SSFSegment(4, SSFField.UNKNOWN),               // CD 0F C8 → 4040 type
                SSFSegment(5, SSFField.UNKNOWN),               // CD 0F C8 → 4040
                SSFSegment(7, SSFField.UNKNOWN),               // B2 ... → "successProbability" type
                SSFSegment(8, SSFField.STRING),                // B2 ... → "successProbability"
                SSFSegment(26, SSFField.UNKNOWN),              // CB ... → 0.94
                SSFSegment(35, SSFField.UNKNOWN),              // AA ... → "isEligible" type
                SSFSegment(36, SSFField.STRING),               // AA ... → "isEligible"
                SSFSegment(46, SSFField.UNKNOWN),              // C3 → true
                SSFSegment(47, SSFField.UNKNOWN),              // A4 ... → "name" type
                SSFSegment(48, SSFField.STRING),               // A4 ... → "name"
                SSFSegment(52, SSFField.UNKNOWN),              // A9 ... → "Lina Chen" type
                SSFSegment(53, SSFField.STRING),               // A9 ... → "Lina Chen"
                SSFSegment(62, SSFField.UNKNOWN),              // AA ... → "highlights" type
                SSFSegment(63, SSFField.STRING),               // AA ... → "highlights"
                SSFSegment(73, SSFField.UNKNOWN),              // 92 → array mit 2 Strings
                SSFSegment(74, SSFField.UNKNOWN),              // B0 ... → "Top of her class" type
                SSFSegment(75, SSFField.STRING),               // B0 ... → "Top of her class"
                SSFSegment(91, SSFField.UNKNOWN),              // B0 ... → "Leadership award" type
                SSFSegment(92, SSFField.STRING),               // B0 ... → "Leadership award"
                SSFSegment(108, SSFField.UNKNOWN),             // AA ... → "attributes" type
                SSFSegment(109, SSFField.STRING),              // AA ... → "attributes"
                SSFSegment(119, SSFField.UNKNOWN),             // 82 → map mit 2 Einträgen
                SSFSegment(120, SSFField.UNKNOWN),             // A6 ... → "vision" type
                SSFSegment(121, SSFField.STRING),              // A6 ... → "vision"
                SSFSegment(127, SSFField.UNKNOWN),             // CB ... → 0.95
                SSFSegment(136, SSFField.UNKNOWN),             // AB ... → "athleticism" type
                SSFSegment(137, SSFField.STRING),              // AB ... → "athleticism"
                SSFSegment(148, SSFField.UNKNOWN)              // CB ... → 0.5
            )
        ),
        TestMessage(12,
            "19010000026973626e00120000003937382d302d30362d3131323030382d3400027469746c650016000000546f204b696c6c2061204d6f636b696e67626972640003617574686f72002d0000000266697273744e616d65000700000048617270657200026c6173744e616d6500040000004c65650000107075626c69736865645965617200a80700000467656e72657300300000000230000800000046696374696f6e0002310008000000436c617373696300023200060000004c6567616c0000037072696365002700000001616d6f756e74007b14ae47e1fa29400263757272656e63790004000000555344000008617661696c61626c65000101726174696e6700333333333333134010696e53746f636b002200000000".fromHex(),
            listOf(
                // BSON-Dokumentlänge (Little Endian: 0x00000119 = 281 Dezimal)
                SSFSegment(0, SSFField.UNKNOWN),

                // "isbn": "978-0-06-112008-4"
                SSFSegment(5, SSFField.STRING),        // "isbn"
                SSFSegment(10, SSFField.STRING),       // "978-0-06-112008-4"

                // "title": "To Kill a Mockingbird"
                SSFSegment(33, SSFField.STRING),       // "title"
                SSFSegment(39, SSFField.STRING),       // "To Kill a Mockingbird"

                // "author": { "firstName": "Harper", "lastName": "Lee" }
                SSFSegment(66, SSFField.STRING),       // "author"
                SSFSegment(73, SSFField.UNKNOWN),      // Subdocument

                SSFSegment(78, SSFField.STRING),       // "firstName"
                SSFSegment(88, SSFField.STRING),       // "Harper"

                SSFSegment(100, SSFField.STRING),      // "lastName"
                SSFSegment(109, SSFField.STRING),      // "Lee"

                // "publishedYear": 1960
                SSFSegment(119, SSFField.STRING),      // "publishedYear"
                SSFSegment(133, SSFField.UNKNOWN),     // 1960

                // "genres": { "0": "Fiction", "1": "Classic", "2": "Legal" }
                SSFSegment(138, SSFField.STRING),      // "genres"
                SSFSegment(145, SSFField.UNKNOWN),     // Subdocument

                SSFSegment(150, SSFField.STRING),      // "0"
                SSFSegment(152, SSFField.STRING),      // "Fiction"

                SSFSegment(165, SSFField.STRING),      // "1"
                SSFSegment(167, SSFField.STRING),      // "Classic"

                SSFSegment(180, SSFField.STRING),      // "2"
                SSFSegment(182, SSFField.STRING),      // "Legal"

                // "price": { "amount": 12.99, "currency": "USD" }
                SSFSegment(194, SSFField.STRING),      // "price"
                SSFSegment(200, SSFField.UNKNOWN),     // Subdocument

                SSFSegment(205, SSFField.STRING),      // "amount"
                SSFSegment(212, SSFField.UNKNOWN),       // 12.99

                SSFSegment(221, SSFField.STRING),      // "currency"
                SSFSegment(230, SSFField.STRING),      // "USD"

                // "available": true
                SSFSegment(240, SSFField.STRING),      // "available"
                SSFSegment(250, SSFField.UNKNOWN),     // true

                // "rating": 4.8
                SSFSegment(252, SSFField.STRING),      // "rating"
                SSFSegment(259, SSFField.UNKNOWN),       // 4.8

                // "inStock": 34
                SSFSegment(268, SSFField.STRING),      // "inStock"
                SSFSegment(276, SSFField.UNKNOWN),     // 34

                // Dokument-Ende
                SSFSegment(280, SSFField.UNKNOWN)
            )
        ),
        TestMessage(13,
            "06010000026973626e00120000003937382d302d373433322d373335362d3500027469746c650010000000416e67656c7320262044656d6f6e730003617574686f72002c0000000266697273744e616d65000400000044616e00026c6173744e616d65000600000042726f776e0000107075626c69736865645965617200d00700000467656e726573002400000002300009000000546872696c6c657200023100080000004d7973746572790000037072696365002700000001616d6f756e74007b14ae47e1fa23400263757272656e63790004000000555344000008617661696c61626c65000101726174696e6700666666666666104010696e53746f636b000c00000000".fromHex(),
            listOf(
                // BSON-Dokumentlänge
                SSFSegment(0, SSFField.UNKNOWN),              // 06010000 → 262 Bytes

                // "isbn": "978-0-7432-7356-5"
                SSFSegment(5, SSFField.STRING),               // "isbn"
                SSFSegment(10, SSFField.STRING),              // "978-0-7432-7356-5"

                // "title": "Angels & Demons"
                SSFSegment(33, SSFField.STRING),              // "title"
                SSFSegment(39, SSFField.STRING),              // "Angels & Demons"

                // "author": { "firstName": "Dan", "lastName": "Brown" }
                SSFSegment(60, SSFField.STRING),              // "author"
                SSFSegment(67, SSFField.UNKNOWN),             // Subdocument

                SSFSegment(72, SSFField.STRING),              // "firstName"
                SSFSegment(82, SSFField.STRING),              // "Dan"

                SSFSegment(91, SSFField.STRING),              // "lastName"
                SSFSegment(100, SSFField.STRING),             // "Brown"

                // "publishedYear": 2000
                SSFSegment(112, SSFField.STRING),             // "publishedYear"
                SSFSegment(126, SSFField.UNKNOWN),            // 2000

                // "genres": { "0": "Thriller", "1": "Mystery" }
                SSFSegment(131, SSFField.STRING),             // "genres"
                SSFSegment(138, SSFField.UNKNOWN),            // Subdocument

                SSFSegment(143, SSFField.STRING),             // "0"
                SSFSegment(145, SSFField.STRING),             // "Thriller"

                SSFSegment(159, SSFField.STRING),             // "1"
                SSFSegment(161, SSFField.STRING),             // "Mystery"

                // "price": { "amount": 9.99, "currency": "USD" }
                SSFSegment(175, SSFField.STRING),             // "price"
                SSFSegment(181, SSFField.UNKNOWN),            // Subdocument

                SSFSegment(186, SSFField.STRING),             // "amount"
                SSFSegment(193, SSFField.UNKNOWN),              // 9.99

                SSFSegment(202, SSFField.STRING),             // "currency"
                SSFSegment(211, SSFField.STRING),             // "USD"

                // "available": true
                SSFSegment(221, SSFField.STRING),             // "available"
                SSFSegment(231, SSFField.UNKNOWN),            // true

                // "rating": 4.1
                SSFSegment(233, SSFField.STRING),             // "rating"
                SSFSegment(240, SSFField.UNKNOWN),              // 4.1

                // "inStock": 12
                SSFSegment(249, SSFField.STRING),             // "inStock"
                SSFSegment(257, SSFField.UNKNOWN),            // 12

                // Dokument-Ende
                SSFSegment(261, SSFField.UNKNOWN)
            )
        ),
        TestMessage(14,
            "1b010000026973626e00120000003937382d312d3235302d33303636392d3800027469746c650015000000546865204d69646e69676874204c6962726172790003617574686f72002c0000000266697273744e616d6500050000004d61747400026c6173744e616d650005000000486169670000107075626c69736865645965617200e40700000467656e72657300380000000230000800000046616e74617379000231000e0000005068696c6f736f70686963616c000232000800000046696374696f6e0000037072696365002300000010616d6f756e7400100000000263757272656e63790004000000555344000008617661696c61626c65000001726174696e6700000000000000124010696e53746f636b000000000000".fromHex(),
            listOf(
                // BSON-Dokumentlänge
                SSFSegment(0, SSFField.UNKNOWN),              // 1b010000 = 283 Bytes

                // "isbn": "978-1-250-30669-8"
                SSFSegment(5, SSFField.STRING),               // "isbn"
                SSFSegment(10, SSFField.STRING),              // "978-1-250-30669-8"

                // "title": "The Midnight Library"
                SSFSegment(33, SSFField.STRING),              // "title"
                SSFSegment(39, SSFField.STRING),              // "The Midnight Library"

                // "author": { "firstName": "Matt", "lastName": "Haig" }
                SSFSegment(65, SSFField.STRING),              // "author"
                SSFSegment(72, SSFField.UNKNOWN),             // Subdocument

                SSFSegment(77, SSFField.STRING),              // "firstName"
                SSFSegment(87, SSFField.STRING),              // "Matt"

                SSFSegment(97, SSFField.STRING),              // "lastName"
                SSFSegment(106, SSFField.STRING),             // "Haig"

                // "publishedYear": 2020
                SSFSegment(117, SSFField.STRING),             // "publishedYear"
                SSFSegment(131, SSFField.UNKNOWN),            // 2020

                // "genres": { "0": "Fantasy", "1": "Philosophical", "2": "Fiction" }
                SSFSegment(136, SSFField.STRING),             // "genres"
                SSFSegment(143, SSFField.UNKNOWN),            // Subdocument

                SSFSegment(148, SSFField.STRING),             // "0"
                SSFSegment(150, SSFField.STRING),             // "Fantasy"

                SSFSegment(163, SSFField.STRING),             // "1"
                SSFSegment(165, SSFField.STRING),             // "Philosophical"

                SSFSegment(184, SSFField.STRING),             // "2"
                SSFSegment(186, SSFField.STRING),             // "Fiction"

                // "price": { "amount": 16, "currency": "USD" }
                SSFSegment(200, SSFField.STRING),             // "price"
                SSFSegment(206, SSFField.UNKNOWN),            // Subdocument

                SSFSegment(211, SSFField.STRING),             // "amount"
                SSFSegment(218, SSFField.UNKNOWN),            // 16

                SSFSegment(223, SSFField.STRING),             // "currency"
                SSFSegment(232, SSFField.STRING),             // "USD"

                // "available": false
                SSFSegment(242, SSFField.STRING),             // "available"
                SSFSegment(252, SSFField.UNKNOWN),            // false

                // "rating": 4.5
                SSFSegment(254, SSFField.STRING),             // "rating"
                SSFSegment(261, SSFField.UNKNOWN),              // 4.5

                // "inStock": 0
                SSFSegment(270, SSFField.STRING),             // "inStock"
                SSFSegment(278, SSFField.UNKNOWN),            // 0

                // Dokument-Ende
                SSFSegment(282, SSFField.UNKNOWN)
            )
        ),
        TestMessage(15,
            "1e010000026973626e00120000003937382d312d393834382d373736372d3000027469746c65000e00000041746f6d6963204861626974730003617574686f72002e0000000266697273744e616d6500060000004a616d657300026c6173744e616d650006000000436c6561720000107075626c69736865645965617200e20700000467656e726573003c0000000230000a00000053656c662d68656c70000231000d00000050726f647563746976697479000232000b00000050737963686f6c6f67790000037072696365002700000001616d6f756e740000000000008032400263757272656e63790004000000555344000008617661696c61626c65000101726174696e67009a9999999999134010696e53746f636b003900000000".fromHex(),
            listOf(
                // BSON-Dokumentlänge
                SSFSegment(0, SSFField.UNKNOWN),              // 1e010000 → 286 Bytes

                // "isbn": "978-1-9848-7767-0"
                SSFSegment(5, SSFField.STRING),               // "isbn"
                SSFSegment(10, SSFField.STRING),              // "978-1-9848-7767-0"

                // "title": "Atomic Habits"
                SSFSegment(33, SSFField.STRING),              // "title"
                SSFSegment(39, SSFField.STRING),              // "Atomic Habits"

                // "author": { "firstName": "James", "lastName": "Clear" }
                SSFSegment(58, SSFField.STRING),              // "author"
                SSFSegment(65, SSFField.UNKNOWN),             // Subdocument

                SSFSegment(70, SSFField.STRING),              // "firstName"
                SSFSegment(80, SSFField.STRING),              // "James"

                SSFSegment(91, SSFField.STRING),              // "lastName"
                SSFSegment(100, SSFField.STRING),             // "Clear"

                // "publishedYear": 2018
                SSFSegment(112, SSFField.STRING),             // "publishedYear"
                SSFSegment(126, SSFField.UNKNOWN),            // 2018

                // "genres": { "0": "Self-help", "1": "Productivity", "2": "Psychology" }
                SSFSegment(131, SSFField.STRING),             // "genres"
                SSFSegment(138, SSFField.UNKNOWN),            // Subdocument

                SSFSegment(143, SSFField.STRING),             // "0"
                SSFSegment(145, SSFField.STRING),             // "Self-help"

                SSFSegment(160, SSFField.STRING),             // "1"
                SSFSegment(162, SSFField.STRING),             // "Productivity"

                SSFSegment(180, SSFField.STRING),             // "2"
                SSFSegment(182, SSFField.STRING),             // "Psychology"

                // "price": { "amount": 18.5, "currency": "USD" }
                SSFSegment(199, SSFField.STRING),             // "price"
                SSFSegment(205, SSFField.UNKNOWN),            // Subdocument

                SSFSegment(210, SSFField.STRING),             // "amount"
                SSFSegment(217, SSFField.UNKNOWN),              // 18.5

                SSFSegment(226, SSFField.STRING),             // "currency"
                SSFSegment(235, SSFField.STRING),             // "USD"

                // "available": true
                SSFSegment(245, SSFField.STRING),             // "available"
                SSFSegment(255, SSFField.UNKNOWN),            // true

                // "rating": 4.9
                SSFSegment(257, SSFField.STRING),             // "rating"
                SSFSegment(264, SSFField.UNKNOWN),              // 4.9

                // "inStock": 57
                SSFSegment(273, SSFField.STRING),             // "inStock"
                SSFSegment(281, SSFField.UNKNOWN),            // 57

                // Dokument-Ende
                SSFSegment(285, SSFField.UNKNOWN)
            )
        ),
        TestMessage(16,
            "62706c6973743030d4010203040506070852704751635165526c535f102438424139413938422d393842462d344331332d414545332d453430463946433631433344100b5a70726f64756374696f6e11c80008111416181b42444f0000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING), SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING), SSFSegment(20, SSFField.STRING), // 2->"pg", 3->"c"
                SSFSegment(22, SSFField.STRING), SSFSegment(24, SSFField.STRING), // 4->"e", 5->"ls"
                SSFSegment(27, SSFField.STRING), SSFSegment(66, SSFField.UNKNOWN), // 6->"...", 7->11
                SSFSegment(68, SSFField.STRING), SSFSegment(79, SSFField.UNKNOWN) // 8->"production", 9->51200
            )
        ),
        TestMessage(17,
            "62706c6973743030d40102030405060708526d53527047516351651114005f102441303634324536462d463239452d343346362d394441442d424437323846343832463933100b5a70726f64756374696f6e08111417191b1e45470000000000000101000000000000000900000000000000000000000000000052".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING), SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(17, SSFField.STRING), SSFSegment(20, SSFField.STRING), // 2->"ms", 3->"pG"
                SSFSegment(23, SSFField.STRING), SSFSegment(25, SSFField.STRING), // 4->"c", 5->"e"
                SSFSegment(27, SSFField.UNKNOWN), SSFSegment(30, SSFField.STRING), // 6->5120, 7->"..."
                SSFSegment(69, SSFField.UNKNOWN), SSFSegment(71, SSFField.STRING) // 8->11, 9->"production"
            )
        ),
        TestMessage(18,
            "62706c6973743030d301020304050651635270475165100a5f102438424139413938422d393842462d344331332d414545332d4534304639464336314333445a70726f64756374696f6e080f111416183f000000000000010100000000000000070000000000000000000000000000004a".fromHex(),
            listOf(
                SSFSegment(0, SSFField.STRING), SSFSegment(8, SSFField.UNKNOWN),
                SSFSegment(15, SSFField.STRING), SSFSegment(17, SSFField.STRING), // 2->"c", 3->"pG"
                SSFSegment(20, SSFField.STRING), SSFSegment(22, SSFField.UNKNOWN), // 4->"e", 5->10
                SSFSegment(24, SSFField.STRING), SSFSegment(63, SSFField.STRING) // 6->"...", 7->"production"
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