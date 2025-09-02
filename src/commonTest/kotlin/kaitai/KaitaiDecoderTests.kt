package kaitai

import bitmage.ByteOrder
import bitmage.booleanArrayOfInts
import bitmage.byteArrayOfInts
import decoders.*
import kaitai.KaitaiTestUtils.checkElement
import kotlin.test.Test

class KaitaiDecoderTests {

    @Test
    fun testSimpleSequence() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "example"
            ),
            seq = listOf(
                KTSeq(id = "field1", type = KTType.Primitive("u4")),
                KTSeq(id = "field2", type = KTType.Primitive("strz")),
                KTSeq(id = "field3", type = KTType.Primitive("s1")),
            )
        )

        val data = byteArrayOfInts(
            0x00, 0x00, 0x00, 0x01,  // field1 = 1
            0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x00, // field2 = "Hello"
            0x81,  // field3 = -127
        )

        val decoder = Kaitai("example", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        checkElement(result, "example", KaitaiResult::class, Pair(0, 11), Pair(0, 0))
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        // Validate field1
        val field1 = result.bytesListTree["field1"]
        checkElement(field1, "field1", KaitaiUnsignedInteger::class, Pair(0, 4), Pair(0, 0), booleanArrayOfInts(0x00, 0x00, 0x00, 0x01), "field1(1)u")

        // Validate field2
        val field2 = result.bytesListTree["field2"]
        val expectedField2Content = "Hello" + "\u0000" // Include null terminator
        checkElement(field2, "field2", KaitaiString::class, Pair(4, 10), Pair(0, 0), booleanArrayOfInts(0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x00), "field2($expectedField2Content)utf8")

        // Validate field3
        val field3 = result.bytesListTree["field3"]
        checkElement(field3, "field3", KaitaiSignedInteger::class, Pair(10, 11), Pair(0, 0), booleanArrayOfInts(0x81), "field3(-127)s")
    }

    @Test
    fun endiannessTest() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "endianness",
                endian = KTEndian.LE
            ),
            seq = listOf(
                KTSeq(id = "a", type = KTType.Primitive("b17")),
                KTSeq(id = "b", type = KTType.Primitive("b15")),
            )
        )

        val data = byteArrayOfInts(
            0x00, 0x01,
            0x80, 0xff,
        )

        val decoder = Kaitai("endianness", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        val a = result.bytesListTree["a"]
        checkElement(a, "a", KaitaiUnsignedInteger::class, Pair(0, 2), Pair(0, 1), booleanArrayOfInts(0x00, 0x00, 0x00, 0x03))

        val b = result.bytesListTree["b"]
        checkElement(b, "b", KaitaiUnsignedInteger::class, Pair(2, 4), Pair(1, 0), booleanArrayOfInts(0x00, 0xff))
    }

    @Test
    fun testBooleans() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "booleans",
            ),
            seq = listOf(
                KTSeq(id = "field1", type = KTType.Primitive("b1")),
                KTSeq(id = "field2", type = KTType.Primitive("b1")),
            )
        )

        val data = byteArrayOfInts(
            0x80,  // field1 = 1, field2 = 0, rest = 00_0000
        )

        val decoder = Kaitai("bools", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        checkElement(result, "booleans", KaitaiResult::class, Pair(0, 0), Pair(0, 2))  // whether this is correct or not idk, but it's how it is implemented right now at the end of processSeq
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        // Validate field1
        val field1 = result.bytesListTree["field1"]
        check(field1 is KaitaiBoolean) { "Expected field1 to be KaitaiBoolean, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", KaitaiBoolean::class, Pair(0, 0), Pair(0, 1), htmlInnerContent = "field1(true)b")

        // Validate field2
        val field2 = result.bytesListTree["field2"]
        check(field2 is KaitaiBoolean) { "Expected field2 to be KaitaiBoolean, got ${field2::class.simpleName}" }
        checkElement(field2, "field2", KaitaiBoolean::class, Pair(0, 0), Pair(1, 2), htmlInnerContent = "field2(false)b")
    }

    @Test
    fun testUnsignedIntegers() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "unsigned_integers",
            ),
            seq = listOf(
                KTSeq(id = "field1", type = KTType.Primitive("u1")),
                KTSeq(id = "field2", type = KTType.Primitive("u2")),
                KTSeq(id = "field3", type = KTType.Primitive("u4")),
                KTSeq(id = "field4", type = KTType.Primitive("u8")),

                KTSeq(id = "field5", type = KTType.Primitive("u1")),
                KTSeq(id = "field6", type = KTType.Primitive("u1")),

                KTSeq(id = "field7", type = KTType.Primitive("u2")),
                KTSeq(id = "field8", type = KTType.Primitive("u2")),
            )
        )

        val data = byteArrayOfInts(
            0x01,  // field1 = 1
            0x02, 0x03,  // field2 = 515
            0x04, 0x05, 0x06, 0x07,  // field3 = 67438087
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,  // field4 = 579005069656919567
            0x61,  // field5 = 97
            0xa5,  // field6 = -91
            0x6b, 0x15,  // field7 = 27413
            0xb1, 0x5a,  // field8 = 45402
        )

        val decoder = Kaitai("numbers", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        checkElement(result, "unsigned_integers", KaitaiResult::class, Pair(0, data.size), Pair(0, 0))
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        // Validate field1
        val field1 = result.bytesListTree["field1"]
        check(field1 is KaitaiUnsignedInteger) { "Expected field1 to be KaitaiUnsignedInteger, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", KaitaiUnsignedInteger::class, Pair(0, 1), Pair(0, 0), htmlInnerContent = "field1(1)u")

        // Validate field2
        val field2 = result.bytesListTree["field2"]
        check(field2 is KaitaiUnsignedInteger) { "Expected field2 to be KaitaiUnsignedInteger, got ${field2::class.simpleName}" }
        checkElement(field2, "field2", KaitaiUnsignedInteger::class, Pair(1, 3), Pair(0, 0), htmlInnerContent = "field2(515)u")

        // Validate field3
        val field3 = result.bytesListTree["field3"]
        check(field3 is KaitaiUnsignedInteger) { "Expected field3 to be KaitaiUnsignedInteger, got ${field3::class.simpleName}" }
        checkElement(field3, "field3", KaitaiUnsignedInteger::class, Pair(3, 7), Pair(0, 0), htmlInnerContent = "field3(67438087)u")

        // Validate field4
        val field4 = result.bytesListTree["field4"]
        check(field4 is KaitaiUnsignedInteger) { "Expected field4 to be KaitaiUnsignedInteger, got ${field4::class.simpleName}" }
        checkElement(field4, "field4", KaitaiUnsignedInteger::class, Pair(7, 15), Pair(0, 0), htmlInnerContent = "field4(579005069656919567)u")

        // Validate field5
        val field5 = result.bytesListTree["field5"]
        check(field5 is KaitaiUnsignedInteger) { "Expected field5 to be KaitaiUnsignedInteger, got ${field5::class.simpleName}" }
        checkElement(field5, "field5", KaitaiUnsignedInteger::class, Pair(15, 16), Pair(0, 0), htmlInnerContent = "field5(97)u")

        // Validate field6
        val field6 = result.bytesListTree["field6"]
        check(field6 is KaitaiUnsignedInteger) { "Expected field6 to be KaitaiUnsignedInteger, got ${field6::class.simpleName}" }
        checkElement(field6, "field6", KaitaiUnsignedInteger::class, Pair(16, 17), Pair(0, 0), htmlInnerContent = "field6(165)u")

        // Validate field7
        val field7 = result.bytesListTree["field7"]
        check(field7 is KaitaiUnsignedInteger) { "Expected field7 to be KaitaiUnsignedInteger, got ${field7::class.simpleName}" }
        checkElement(field7, "field7", KaitaiUnsignedInteger::class, Pair(17, 19), Pair(0, 0), htmlInnerContent = "field7(27413)u")

        // Validate field8
        val field8 = result.bytesListTree["field8"]
        check(field8 is KaitaiUnsignedInteger) { "Expected field8 to be KaitaiUnsignedInteger, got ${field8::class.simpleName}" }
        checkElement(field8, "field8", KaitaiUnsignedInteger::class, Pair(19, 21), Pair(0, 0), htmlInnerContent = "field8(45402)u")
    }

    @Test
    fun testSignedIntegers() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "signed_integers",
            ),
            seq = listOf(
                KTSeq(id = "field1", type = KTType.Primitive("s1")),
                KTSeq(id = "field2", type = KTType.Primitive("s2")),
                KTSeq(id = "field3", type = KTType.Primitive("s4")),
                KTSeq(id = "field4", type = KTType.Primitive("s8")),

                KTSeq(id = "field5", type = KTType.Primitive("s1")),
                KTSeq(id = "field6", type = KTType.Primitive("s2")),
                KTSeq(id = "field7", type = KTType.Primitive("s4")),
                KTSeq(id = "field8", type = KTType.Primitive("s8")),
            )
        )

        val data = byteArrayOfInts(
            0x7a,  // field1 = 122
            0x05, 0xa1,  // field2 = 1441
            0x50, 0xc1, 0x00, 0x1b,  // field3 = 1354825755
            0x60, 0x00, 0xa1, 0x56, 0xb8, 0x00, 0x00, 0x34,  // field4 = 6917706421467349044
            0xa5,  // field5 = -91
            0xb1, 0x5a,  // field6 = -20134
            0xe1, 0x00, 0x61, 0x00,  // field7 = -520068864
            0xf5, 0x00, 0xa1, 0x56, 0xb8, 0x00, 0x00, 0x34,  // field8 = -792456140590940108
        )

        val decoder = Kaitai("signed_numbers", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        checkElement(result, "signed_integers", KaitaiResult::class, Pair(0, data.size), Pair(0, 0))
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        // Validate field1
        val field1 = result.bytesListTree["field1"]
        checkElement(field1, "field1", KaitaiSignedInteger::class, Pair(0, 1), Pair(0, 0), htmlInnerContent = "field1(122)s")

        // Validate field2
        val field2 = result.bytesListTree["field2"]
        checkElement(field2, "field2", KaitaiSignedInteger::class, Pair(1, 3), Pair(0, 0), htmlInnerContent = "field2(1441)s")

        // Validate field3
        val field3 = result.bytesListTree["field3"]
        checkElement(field3, "field3", KaitaiSignedInteger::class, Pair(3, 7), Pair(0, 0), htmlInnerContent = "field3(1354825755)s")

        // Validate field4
        val field4 = result.bytesListTree["field4"]
        checkElement(field4, "field4", KaitaiSignedInteger::class, Pair(7, 15), Pair(0, 0), htmlInnerContent = "field4(6917706421467349044)s")

        // Validate field5
        val field5 = result.bytesListTree["field5"]
        checkElement(field5, "field5", KaitaiSignedInteger::class, Pair(15, 16), Pair(0, 0), htmlInnerContent = "field5(-91)s")

        // Validate field6
        val field6 = result.bytesListTree["field6"]
        checkElement(field6, "field6", KaitaiSignedInteger::class, Pair(16, 18), Pair(0, 0), htmlInnerContent = "field6(-20134")

        // Validate field7
        val field7 = result.bytesListTree["field7"]
        checkElement(field7, "field7", KaitaiSignedInteger::class, Pair(18, 22), Pair(0, 0), htmlInnerContent = "field7(-520068864)s")

        // Validate field8
        val field8 = result.bytesListTree["field8"]
        checkElement(field8, "field8", KaitaiSignedInteger::class, Pair(22, 30), Pair(0, 0), htmlInnerContent = "field8(-792456140590940108)s")
    }

    @Test
    fun testSubstreamSizeBoundaries() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "subElementsTooShort", type = KTType.Primitive("sub"), size = StringOrInt.IntValue(10)),
                KTSeq(id = "subElementsJustRight", type = KTType.Primitive("sub"), size = StringOrInt.IntValue(7)),
                KTSeq(id = "subElementsSpeakForThemselves", type = KTType.Primitive("sub"))
            ),
            types = mapOf(
                Pair("sub",
                    KTStruct(
                        seq = listOf(
                            KTSeq(id = "code", type = KTType.Primitive("u2")),
                            KTSeq(id = "name", type = KTType.Primitive("str"), size = StringOrInt.IntValue(5)),
                        )
                    )
                )
            )
        )

        val data = byteArrayOfInts(
            // subElementsTooShort, not a problem
            0xc0, 0xde,
            0x61, 0x62, 0x63, 0x64, 0x65,
            0xff, 0xff, 0xff,  // should be highlighted in the bytefinder when clicking on subElementsTooShort, but not visible in the decoder

            // subElementsJustRight, obv. fine
            0xc0, 0xde,
            0x71, 0x72, 0x73, 0x74, 0x75,

            // subElementsSpeakForThemselves, obv. fine
            0xc0, 0xde,
            0x41, 0x42, 0x43, 0x44, 0x45,

            // subElementsTooLong, should never be visible
            0xc0, 0xde,
            0x91, 0x92, 0x93, 0x94, 0x95
        )

        val decoder = Kaitai("substream_size_boundaries", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        val field1 = result.bytesListTree["subElementsTooShort"]
        checkElement(field1, "subElementsTooShort", KaitaiResult::class, Pair(0, 7), Pair(0, 0))
        check(field1.bytesListTree!!.size == 2) { "Expected subtype to have exactly 2 elements, got ${field1.bytesListTree!!.size}" }
        check((field1.bytesListTree!!["code"].value as BooleanArray).contentEquals(booleanArrayOfInts(0xc0, 0xde))) {
            "Expected subtype.code to be exactly 0xc0 0xde and not ${field1.bytesListTree!!["code"].value}"
        }
        check((field1.bytesListTree!!["name"].value as BooleanArray).contentEquals(booleanArrayOfInts(0x61, 0x62, 0x63, 0x64, 0x65))) {
            "Expected subtype.name to be exactly 0xc0de and not ${field1.bytesListTree!!["name"].value}"
        }

        val field2 = result.bytesListTree["subElementsJustRight"]
        checkElement(field2, "subElementsJustRight", KaitaiResult::class, Pair(10, 17), Pair(0, 0))
        check(field2.bytesListTree!!.size == 2) { "Expected subtype to have exactly 2 elements, got ${field2.bytesListTree!!.size}" }
        check((field2.bytesListTree!!["code"].value as BooleanArray).contentEquals(booleanArrayOfInts(0xc0, 0xde))) {
            "Expected subtype.code to be exactly 0xc0de and not ${field2.bytesListTree!!["code"].value}"
        }
        check((field2.bytesListTree!!["name"].value as BooleanArray).contentEquals(booleanArrayOfInts(0x71, 0x72, 0x73, 0x74, 0x75))) {
            "Expected subtype.name to be exactly 0xc0de and not ${field2.bytesListTree!!["name"].value}"
        }

        val field3 = result.bytesListTree["subElementsSpeakForThemselves"]

        checkElement(field3, "subElementsSpeakForThemselves", KaitaiResult::class, Pair(17, 24), Pair(0, 0))
        check(field3.bytesListTree!!.size == 2) { "Expected subtype to have exactly 2 elements, got ${field3.bytesListTree!!.size}" }
        check((field3.bytesListTree!!["code"].value as BooleanArray).contentEquals(booleanArrayOfInts(0xc0, 0xde))) {
            "Expected subtype.code to be exactly 0xc0de and not ${field3.bytesListTree!!["code"].value}"
        }
        check((field3.bytesListTree!!["name"].value as BooleanArray).contentEquals(booleanArrayOfInts(0x41, 0x42, 0x43, 0x44, 0x45))) {
            "Expected subtype.name to be exactly 0xc0de and not ${field3.bytesListTree!!["name"].value}"
        }
    }

    @Test
    fun testBitwiseOffsets() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "a", type = KTType.Primitive("b4")),
                KTSeq(id = "b", type = KTType.Primitive("b5")),
                KTSeq(id = "c", type = KTType.Primitive("b8")),
                KTSeq(id = "d", type = KTType.Primitive("b3")),
                KTSeq(id = "e", type = KTType.Primitive("u1")),
                KTSeq(id = "f", type = KTType.Primitive("b2")),
                KTSeq(id = "g", type = KTType.Primitive("u1")),
                KTSeq(id = "h", type = KTType.Primitive("b17")),
                KTSeq(id = "i", type = KTType.Primitive("b15")),
            ),
        )

        val data = byteArrayOfInts(
            // subElementsTooShort, not a problem
            0x11, 0x22,
            0x33, 0x44,
            0x55, 0x66,
            0x00, 0x01,
            0x80, 0xff
        )

        val decoder = Kaitai("bitwise_offsets", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        val a = result.bytesListTree["a"]
        checkElement(a, "a", KaitaiUnsignedInteger::class, Pair(0,0), Pair(0, 4), booleanArrayOf(false, false, false, false, false, false, false, true))

        val b = result.bytesListTree["b"]
        checkElement(b, "b", KaitaiUnsignedInteger::class, Pair(0, 1), Pair(4, 1), booleanArrayOf(false, false, false, false, false, false, true, false))

        val c = result.bytesListTree["c"]
        checkElement(c, "c", KaitaiUnsignedInteger::class, Pair(1, 2), Pair(1, 1), booleanArrayOf(false, true, false, false, false, true, false, false))

        val d = result.bytesListTree["d"]
        checkElement(d, "d", KaitaiUnsignedInteger::class, Pair(2, 2), Pair(1, 4),booleanArrayOf(false, false, false, false, false, false, true, true))

        val e = result.bytesListTree["e"]
        checkElement(e, "e", KaitaiUnsignedInteger::class, Pair(3, 4), Pair(0, 0), booleanArrayOfInts(0x44))

        val f = result.bytesListTree["f"]
        checkElement(f, "f", KaitaiUnsignedInteger::class, Pair(4, 4), Pair(0, 2), booleanArrayOf(false, false, false, false, false, false, false, true))

        val g = result.bytesListTree["g"]
        checkElement(g, "g", KaitaiUnsignedInteger::class, Pair(5, 6), Pair(0, 0), booleanArrayOfInts(0x66))

        val h = result.bytesListTree["h"]
        checkElement(h, "h", KaitaiUnsignedInteger::class, Pair(6, 8), Pair(0, 1), booleanArrayOfInts(0x00, 0x00, 0x00, 0x03))

        val i = result.bytesListTree["i"]
        checkElement(i, "i", KaitaiUnsignedInteger::class, Pair(8, 10), Pair(1, 0), booleanArrayOfInts(0x00, 0xff))
    }

    @Test
    fun testRepeatKey() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "repeat_by_expr", size = StringOrInt.IntValue(1), repeat = KTRepeat.EXPR, repeatExpr = "3"),
                KTSeq(id = "set_size", size = StringOrInt.IntValue(4), type = KTType.Primitive("repeating_sub")),
                KTSeq(id = "simple_eos", type = KTType.Primitive("simple_sub"), repeat = KTRepeat.EOS)
            ),
            types = mapOf(
                Pair(
                    "simple_sub",
                    KTStruct(
                        seq = listOf(
                            KTSeq(id = "a", size = StringOrInt.IntValue(1))
                        )
                    )
                ),
                Pair(
                    "repeating_sub",
                    KTStruct(
                        seq = listOf(
                            KTSeq(id = "x", size = StringOrInt.IntValue(2), repeat = KTRepeat.EOS),
                        )
                    )
                )
            )
        )

        val data = byteArrayOfInts(
            0xaa, 0xbb, 0xcc,
            0x11, 0x22, 0x33, 0x44,
            0x12, 0x34
        )

        val decoder = Kaitai("bitwise_offsets", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        val repeat_by_expr = result.bytesListTree["repeat_by_expr"]
        checkElement(repeat_by_expr, "repeat_by_expr", KaitaiList::class, Pair(0,3), Pair(0, 0), booleanArrayOfInts(0xaa, 0xbb, 0xcc))
        val aa0 = repeat_by_expr.bytesListTree!![0]
        checkElement(aa0, "repeat_by_expr", KaitaiBytes::class, Pair(0,1), Pair(0, 0), booleanArrayOfInts(0xaa))
        val aa1 = repeat_by_expr.bytesListTree!![1]
        checkElement(aa1, "repeat_by_expr", KaitaiBytes::class, Pair(1,2), Pair(0, 0), booleanArrayOfInts(0xbb))
        val aa2 = repeat_by_expr.bytesListTree!![2]
        checkElement(aa2, "repeat_by_expr", KaitaiBytes::class, Pair(2,3), Pair(0, 0), booleanArrayOfInts(0xcc))

        val set_size = result.bytesListTree["set_size"]
        checkElement(set_size, "set_size", KaitaiResult::class, Pair(3,7), Pair(0, 0), booleanArrayOfInts(0x11, 0x22, 0x33, 0x44))
        val set_size_x = set_size.bytesListTree!!["x"]
        checkElement(set_size_x, "x", KaitaiList::class, Pair(3,7), Pair(0, 0), booleanArrayOfInts(0x11, 0x22, 0x33, 0x44))
        val set_size_x_0 = set_size_x.bytesListTree!![0]
        checkElement(set_size_x_0, "x", KaitaiBytes::class, Pair(3,5), Pair(0, 0), booleanArrayOfInts(0x11, 0x22))
        val set_size_x_1 = set_size_x.bytesListTree!![1]
        checkElement(set_size_x_1, "x", KaitaiBytes::class, Pair(5,7), Pair(0, 0),booleanArrayOfInts(0x33, 0x44))

        val simple_eos = result.bytesListTree["simple_eos"]
        checkElement(simple_eos, "simple_eos", KaitaiList::class, Pair(7,9), Pair(0, 0), booleanArrayOfInts(0x12, 0x34))
        val simple_eos_0 = simple_eos.bytesListTree!![0]
        checkElement(simple_eos_0, "simple_eos", KaitaiResult::class, Pair(7,8), Pair(0, 0), booleanArrayOfInts(0x12))
        val simple_eos_0_a = simple_eos_0.bytesListTree!!["a"]
        checkElement(simple_eos_0_a, "a", KaitaiBytes::class, Pair(7,8), Pair(0, 0), booleanArrayOfInts(0x12))
        val simple_eos_1 = simple_eos.bytesListTree!![1]
        checkElement(simple_eos_1, "simple_eos", KaitaiResult::class, Pair(8,9), Pair(0, 0), booleanArrayOfInts(0x34))
        val simple_eos_1_a = simple_eos_1.bytesListTree!!["a"]
        checkElement(simple_eos_1_a, "a", KaitaiBytes::class, Pair(8,9), Pair(0, 0), booleanArrayOfInts(0x34))

    }

    @Test
    fun testDummyElements() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "header", size = StringOrInt.IntValue(1)),
                KTSeq(id = "dummy0", size = StringOrInt.IntValue(0)),
                KTSeq(id = "dummy1", type = KTType.Primitive("dummy1")),
                KTSeq(id = "dummy2", type = KTType.Primitive("dummy2")),
                KTSeq(id = "dummy3", type = KTType.Primitive("dummy3")),
                KTSeq(id = "dummy4", type = KTType.Primitive("dummy4")),
                KTSeq(id = "trailer", size = StringOrInt.IntValue(1)),
            ),
            types = mapOf(
                Pair(
                    "dummy1",
                    KTStruct()
                ),
                Pair(
                    "dummy2",
                    KTStruct(
                        doc = "This type is intentionally left blank."
                    )
                ),
                Pair(
                    "dummy3",
                    KTStruct(
                        seq = listOf(),
                        instances = mapOf(),
                        types = mapOf()
                    )
                ),
                Pair(
                    "dummy4",
                    KTStruct(
                        seq = listOf(
                            KTSeq(id = "no_value", size = StringOrInt.IntValue(0)),
                        )
                    )
                )
            )
        )

        val data = byteArrayOfInts(
            0x12, 0xab,
        )

        val decoder = Kaitai("dummy_elements", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        check(result.bytesListTree.size == struct.seq.size && result.bytesListTree.size == 7) {
            "Expected bytesListTree to have 7 elements, got ${struct.seq.size} / got ${result.bytesListTree.size}"
        }

        val header = result.bytesListTree["header"]
        checkElement(header, "header", KaitaiBytes::class, Pair(0, 1), Pair(0, 0), booleanArrayOfInts(0x12))

        val dummy0 = result.bytesListTree["dummy0"]
        check(dummy0 is KaitaiBytes) { "Expected a to be KaitaiBytes, got ${dummy0::class.simpleName}" }
        check((dummy0.value as BooleanArray).isEmpty()) { "Expected size to be 0, got ${(dummy0.value as BooleanArray).size}" }
        val dummy1 = result.bytesListTree["dummy1"]
        check(dummy1 is KaitaiResult) { "Expected a to be KaitaiResult, got ${dummy1::class.simpleName}" }
        check(dummy1.value.isEmpty()) { "Expected size to be 0, got ${dummy1.value.size}" }
        val dummy2 = result.bytesListTree["dummy2"]
        check(dummy2 is KaitaiResult) { "Expected a to be KaitaiResult, got ${dummy2::class.simpleName}" }
        check(dummy2.value.isEmpty()) { "Expected size to be 0, got ${dummy2.value.size}" }
        val dummy3 = result.bytesListTree["dummy3"]
        check(dummy3 is KaitaiResult) { "Expected a to be KaitaiResult, got ${dummy3::class.simpleName}" }
        check(dummy3.value.isEmpty()) { "Expected size to be 0, got ${dummy3.value.size}" }
        val dummy4 = result.bytesListTree["dummy4"]
        check(dummy4 is KaitaiResult) { "Expected a to be KaitaiResult, got ${dummy4::class.simpleName}" }
        check(dummy4.value.isEmpty()) { "Expected size to be 0, got ${dummy4.value.size}" }

        val trailer = result.bytesListTree["trailer"]
        checkElement(trailer, "trailer", KaitaiBytes::class, Pair(1, 2), Pair(0, 0), booleanArrayOfInts(0xab))
    }

    @Test
    fun testTypeReferences() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "root1", type = KTType.Primitive("sub")),
                KTSeq(id = "root2", type = KTType.Primitive("sub::subsub")),
            ),
            types = mapOf(
                Pair("sub",
                    KTStruct(
                        seq = listOf(
                            KTSeq(id = "upper1", type = KTType.Primitive("sub::subsub")),  // from parent type
                            KTSeq(id = "upper2", type = KTType.Primitive("sub::subsub::subsubsub")),  // from parent type
                            KTSeq(id = "upper3", type = KTType.Primitive("subsub")),  // from subtypes of current type
                            KTSeq(id = "upper4", type = KTType.Primitive("subsub::subsubsub")),  // from subtypes of current type
                        ),
                        types = mapOf(
                            Pair("subsub",
                                KTStruct(
                                    seq = listOf(
                                        KTSeq(id = "lower1", size = StringOrInt.IntValue(1)),
                                    ),
                                    types = mapOf(
                                        Pair("subsubsub",
                                            KTStruct(
                                                seq = listOf(
                                                    KTSeq(id = "leaf1", size = StringOrInt.IntValue(1)),
                                                ),
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val data = byteArrayOfInts(

            0x00, 0x00, 0x00, 0x00,  // root1
            0x00,  // root2

        )

        val decoder = Kaitai("bitwise_offsets", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        val root1 = result.bytesListTree["root1"]
        checkElement(root1, "root1", KaitaiResult::class, Pair(0,4), Pair(0, 0))

        val upper1 = root1.bytesListTree!!["upper1"]
        checkElement(upper1, "upper1", KaitaiResult::class, Pair(0, 1), Pair(0, 0))
        val upper2 = root1.bytesListTree!!["upper2"]
        checkElement(upper2, "upper2", KaitaiResult::class, Pair(1, 2), Pair(0, 0))
        val upper3 = root1.bytesListTree!!["upper3"]
        checkElement(upper3, "upper3", KaitaiResult::class, Pair(2, 3), Pair(0, 0))
        val upper4 = root1.bytesListTree!!["upper4"]
        checkElement(upper4, "upper4", KaitaiResult::class, Pair(3, 4), Pair(0, 0))

        val root2 = result.bytesListTree["root2"]
        checkElement(root2, "root2", KaitaiResult::class, Pair(4,5), Pair(0, 0))
    }

    @Test
    fun testNestedSubtypes() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "field1", type = KTType.Primitive("sub")),
                KTSeq(id = "field2", type = KTType.Primitive("sub"))
            ),
            types = mapOf(
                Pair("sub",
                    KTStruct(
                        seq = listOf(
                            KTSeq(id = "sub1", type = KTType.Primitive("subsub")),
                            KTSeq(id = "sub2", type = KTType.Primitive("subsub"))
                        ),
                        types = mapOf(
                            Pair("subsub",
                                KTStruct(
                                    seq = listOf(
                                        KTSeq(id = "leaf", size = StringOrInt.IntValue(2))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val data = byteArrayOfInts(
            // field1
            0x11, 0x11,  // sub1 leaf
            0x11, 0x22,  // sub2 leaf
            // field2
            0x22, 0x11,  // sub1 leaf
            0x22, 0x22,  // sub2 leaf
            )

        val decoder = Kaitai("nested_subtypes", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        val field1 = result.bytesListTree["field1"]
        checkElement(field1, "field1", KaitaiResult::class, Pair(0, 4), Pair(0, 0), booleanArrayOfInts(0x11, 0x11, 0x11, 0x22))
        check(field1.bytesListTree!!.size == 2) { "Expected subtype to have exactly 2 elements, got ${field1.bytesListTree!!.size}" }
        val field1_sub1 = field1.bytesListTree!!["sub1"]
        checkElement(field1_sub1, "sub1", KaitaiResult::class, Pair(0, 2), Pair(0, 0), booleanArrayOfInts(0x11, 0x11))
        check(field1_sub1.bytesListTree!!.size == 1) { "Expected subtype to have exactly 1 elements, got ${field1_sub1.bytesListTree!!.size}" }
        val field1_sub1_leaf = field1_sub1.bytesListTree!!["leaf"]
        checkElement(field1_sub1_leaf, "leaf", KaitaiBytes::class, Pair(0, 2), Pair(0, 0), booleanArrayOfInts(0x11, 0x11))
        val field1_sub2 = field1.bytesListTree!!["sub2"]
        checkElement(field1_sub2, "sub2", KaitaiResult::class, Pair(2, 4), Pair(0, 0), booleanArrayOfInts(0x11, 0x22))
        check(field1_sub2.bytesListTree!!.size == 1) { "Expected subtype to have exactly 2 elements, got ${field1_sub2.bytesListTree!!.size}" }
        val field1_sub2_leaf = field1_sub2.bytesListTree!!["leaf"]
        checkElement(field1_sub2_leaf, "leaf", KaitaiBytes::class, Pair(2, 4), Pair(0, 0), booleanArrayOfInts(0x11, 0x22))

        val field2 = result.bytesListTree["field2"]
        checkElement(field2, "field2", KaitaiResult::class, Pair(4, 8), Pair(0, 0), booleanArrayOfInts(0x22, 0x11, 0x22, 0x22))
        check(field2.bytesListTree!!.size == 2) { "Expected subtype to have exactly 2 elements, got ${field2.bytesListTree!!.size}" }
        val field2_sub1 = field2.bytesListTree!!["sub1"]
        checkElement(field2_sub1, "sub1", KaitaiResult::class, Pair(4, 6), Pair(0, 0), booleanArrayOfInts(0x22, 0x11))
        check(field2_sub1.bytesListTree!!.size == 1) { "Expected subtype to have exactly 2 elements, got ${field2_sub1.bytesListTree!!.size}" }
        val field2_sub1_leaf = field2_sub1.bytesListTree!!["leaf"]
        checkElement(field2_sub1_leaf, "leaf", KaitaiBytes::class, Pair(4, 6), Pair(0, 0), booleanArrayOfInts(0x22, 0x11))
        val field2_sub2 = field2.bytesListTree!!["sub2"]
        checkElement(field2_sub2, "sub2", KaitaiResult::class, Pair(6, 8), Pair(0, 0), booleanArrayOfInts(0x22, 0x22))
        check(field2_sub2.bytesListTree!!.size == 1) { "Expected subtype to have exactly 2 elements, got ${field2_sub2.bytesListTree!!.size}" }
        val field2_sub2_leaf = field2_sub2.bytesListTree!!["leaf"]
        checkElement(field2_sub2_leaf, "leaf", KaitaiBytes::class, Pair(6, 8), Pair(0, 0), booleanArrayOfInts(0x22, 0x22))

    }

    @Test
    fun testTypeReferences2() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id="element_sub", type= KTType.Primitive("sub"))
            ),
            types = mapOf(
                Pair("sub", KTStruct(
                    seq = listOf(
                        KTSeq(id="element_subsub", type= KTType.Primitive("subsub"))
                    ),
                    types = mapOf(
                        Pair("subsub", KTStruct(
                            seq = listOf(
                                KTSeq(id="element_a", type= KTType.Primitive("a")),
                                KTSeq(id="element_b", type= KTType.Primitive("b")),
                                KTSeq(id="element_c", type= KTType.Primitive("c")),
                            ),
                            types = mapOf(
                                Pair("a", KTStruct(
                                    seq = listOf(
                                        KTSeq(id="subsub_a", type= KTType.Primitive("u4"))
                                    )
                                ))
                            )
                        )),
                        Pair("a", KTStruct(
                            seq = listOf(
                                KTSeq(id="sub_a", type= KTType.Primitive("u2"))
                            )
                        )),
                        Pair("b", KTStruct(
                            seq = listOf(
                                KTSeq(id="sub_b", type= KTType.Primitive("u2"))
                            )
                        ))
                    )
                )),
                Pair("a", KTStruct(
                    seq = listOf(
                        KTSeq(id="root_a", type= KTType.Primitive("u1"))
                    )
                )),
                Pair("b", KTStruct(
                    seq = listOf(
                        KTSeq(id="root_b", type= KTType.Primitive("u1"))
                    )
                )),
                Pair("c", KTStruct(
                    seq = listOf(
                        KTSeq(id="root_c", type= KTType.Primitive("u1"))
                    )
                ))
            )
        )

        val data = byteArrayOfInts(
            0xaa, 0xaa, 0xaa, 0xaa, // for type sub::subsub::a from sub::subsub
            0xbb, 0xbb, // for type sub::b from sub::subsub
            0xcc, // for type c from sub::subsub
        )

        val decoder = Kaitai("type_references_2", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        var element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_a"]
        checkElement(element, id="element_a", elementClass= KaitaiResult::class, sourceByteRange=Pair(0, 4), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xaa, 0xaa, 0xaa, 0xaa))
        element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_a"].bytesListTree!!["subsub_a"]
        checkElement(element, id="subsub_a", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(0, 4), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xaa, 0xaa, 0xaa, 0xaa))

        element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_b"]
        checkElement(element, id="element_b", elementClass= KaitaiResult::class, sourceByteRange=Pair(4, 6), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xbb, 0xbb))
        element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_b"].bytesListTree!!["sub_b"]
        checkElement(element, id="sub_b", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(4, 6), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xbb, 0xbb))

        element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_c"]
        checkElement(element, id="element_c", elementClass= KaitaiResult::class, sourceByteRange=Pair(6, 7), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xcc))
        element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_c"].bytesListTree!!["root_c"]
        checkElement(element, id="root_c", elementClass= KaitaiUnsignedInteger::class, sourceByteRange=Pair(6, 7), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xcc))
    }

    @Test
    fun testEnumReferences() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id="element_sub", type= KTType.Primitive("sub")),
                KTSeq(id="enumWithPath", type= KTType.Primitive("u1"), enum="sub::subsub::enum_a")
            ),
            types = mapOf(
                Pair("sub", KTStruct(
                    seq = listOf(
                        KTSeq(id="element_subsub", type= KTType.Primitive("subsub"))
                    ),
                    types = mapOf(
                        Pair("subsub", KTStruct(
                            seq = listOf(
                                KTSeq(id="element_a", type= KTType.Primitive("u1"), enum="enum_a"),
                                KTSeq(id="element_b", type= KTType.Primitive("u1"), enum="enum_b"),
                                KTSeq(id="element_c", type= KTType.Primitive("u1"), enum="enum_c"),
                            ),
                            enums = mapOf(
                                Pair("enum_a",
                                    KTEnum(
                                        mapOf(
                                            Pair(1, KTEnumValue(id= StringOrBoolean.StringValue("subsub_a"))),
                                            Pair(4, KTEnumValue(id= StringOrBoolean.StringValue("subsub_a_from_root")))
                                        )
                                    )
                                )
                            )
                        )),
                    ),
                    enums = mapOf(
                        Pair("enum_a",
                            KTEnum(
                                mapOf(
                                    Pair(1, KTEnumValue(id= StringOrBoolean.StringValue("sub_a")))
                                )
                            )
                        ),
                        Pair("enum_b",
                            KTEnum(
                                mapOf(
                                    Pair(2, KTEnumValue(id= StringOrBoolean.StringValue("sub_b")))
                                )
                            )
                        )
                    )
                )),
            ),
            enums = mapOf(
                Pair("enum_a",
                    KTEnum(
                        mapOf(
                            Pair(1, KTEnumValue(id= StringOrBoolean.StringValue("root_a")))
                        )
                    )
                ),
                Pair("enum_b",
                    KTEnum(
                        mapOf(
                            Pair(2, KTEnumValue(id= StringOrBoolean.StringValue("root_b")))
                        )
                    )
                ),
                Pair("enum_c",
                    KTEnum(
                        mapOf(
                            Pair(3, KTEnumValue(id= StringOrBoolean.StringValue("root_c")))
                        )
                    )
                )
            )
        )

        val data = byteArrayOfInts(
            0x01, // for enum sub::subsub::enum_a from sub::subsub
            0x02, // for enum sub::enum_b from sub::subsub
            0x03, // for enum enum_c from sub::subsub
            0x04, // for enum sub::subsub::enum_a from root
        )

        val decoder = Kaitai("type_references_2", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        var element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_a"]
        check(element is KaitaiEnum) { "Expected KaitaiEnum, got ${element::class.simpleName}" }
        checkElement(element, id="element_a", elementClass=KaitaiEnum::class, sourceByteRange=Pair(0, 1), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x01))
        var enum: Pair<KTEnum?, String> = Pair(struct.types["sub"]!!.types["subsub"]!!.enums["enum_a"], "subsub_a")
        check(element.enum.first === enum.first) {"Expected ${enum.first}, got ${element.enum.first}"}
        check(element.enum.second == enum.second) {"Expected ${enum.second}, got ${element.enum.second}"}

        element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_b"]
        check(element is KaitaiEnum) { "Expected KaitaiEnum, got ${element::class.simpleName}" }
        checkElement(element, id="element_b", elementClass=KaitaiEnum::class, sourceByteRange=Pair(1, 2), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x02))
        enum = Pair(struct.types["sub"]!!.enums["enum_b"], "sub_b")
        check(element.enum.first === enum.first) {"Expected ${enum.first}, got ${element.enum.first}"}
        check(element.enum.second == enum.second) {"Expected ${enum.second}, got ${element.enum.second}"}

        element = result.bytesListTree["element_sub"].bytesListTree!!["element_subsub"].bytesListTree!!["element_c"]
        check(element is KaitaiEnum) { "Expected KaitaiEnum, got ${element::class.simpleName}" }
        checkElement(element, id="element_c", elementClass=KaitaiEnum::class, sourceByteRange=Pair(2, 3), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x03))
        enum = Pair(struct.enums["enum_c"], "root_c")
        check(element.enum.first === enum.first) {"Expected ${enum.first}, got ${element.enum.first}"}
        check(element.enum.second == enum.second) {"Expected ${enum.second}, got ${element.enum.second}"}

        element = result.bytesListTree["enumWithPath"]
        check(element is KaitaiEnum) { "Expected KaitaiEnum, got ${element::class.simpleName}" }
        checkElement(element, id="enumWithPath", elementClass=KaitaiEnum::class, sourceByteRange=Pair(3, 4), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x04))
        enum = Pair(struct.types["sub"]!!.types["subsub"]!!.enums["enum_a"], "subsub_a_from_root")
        check(element.enum.first === enum.first) {"Expected ${enum.first}, got ${element.enum.first}"}
        check(element.enum.second == enum.second) {"Expected ${enum.second}, got ${element.enum.second}"}
    }


    @Test
    fun testEnumTypes() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "protocol", type = KTType.Primitive("u4"), enum = "ip_protocols"),
                KTSeq(id = "protocol_huge", type = KTType.Primitive("u4"), enum = "ip_protocols"),
                KTSeq(id = "verbose_negative", type = KTType.Primitive("s4"), enum = "verbose_levels"),
                KTSeq(id = "verbose_positive", type = KTType.Primitive("s4"), enum = "verbose_levels"),
                KTSeq(id = "flags", type = KTType.Primitive("b1"), enum = "bit_flags"),
                KTSeq(id = "padding", type = KTType.Primitive("u1"), enum = "has_padding"),
            ),
            enums = mapOf(
                Pair(
                    "ip_protocols",
                    KTEnum(
                        mapOf(
                            Pair(1, KTEnumValue(id = StringOrBoolean.StringValue("icmp"))),
                            Pair(6, KTEnumValue(id = StringOrBoolean.StringValue("tcp"))),
                            Pair(17, KTEnumValue(id = StringOrBoolean.StringValue("udp"))),
                            Pair(3000000000, KTEnumValue(id = StringOrBoolean.StringValue("some_protocol"))),
                        )
                    )
                ),
                Pair(
                    "verbose_levels",
                    KTEnum(
                        mapOf(
                            Pair(-1, KTEnumValue(id = StringOrBoolean.StringValue("negative"), doc = "No verbosity")),
                            Pair(0, KTEnumValue(id = StringOrBoolean.StringValue("none"), doc = "No verbosity")),
                            Pair(1, KTEnumValue(id = StringOrBoolean.StringValue("low"), doc = "Low verbosity", docRef = listOf("https://example.com/low"))),
                            Pair(2, KTEnumValue(id = StringOrBoolean.StringValue("medium"), docRef = listOf("https://example.com/medium"))),
                        )
                    )
                ),
                Pair(
                    "bit_flags",
                    KTEnum(
                        mapOf(
                            Pair(0, KTEnumValue(id = StringOrBoolean.StringValue("flag1"))),
                            Pair(1, KTEnumValue(id = StringOrBoolean.StringValue("flag2"))),
                        )
                    )
                ),
                Pair(
                    "has_padding",
                    KTEnum(
                        mapOf(
                            Pair(0x01, KTEnumValue(id = StringOrBoolean.BooleanValue(false))),
                            Pair(0x10, KTEnumValue(id = StringOrBoolean.BooleanValue(true))),
                        )
                    )
                ),
            )
        )

        val data = byteArrayOfInts(
            0x00, 0x00, 0x00, 0x06,  // ip_protocols: (6: tcp)
            0xB2, 0xD0, 0x5E, 0x00, // ip_protocols: (3000000000: some_protocol)
            0xff, 0xff, 0xff, 0xff, // verbose_levels: (-1: negative)
            0x00, 0x00, 0x00, 0x02, // verbose_levels: (2: medium)
            0x80, // bit_flags: (1: flag2) + bx000_0000
            0x10, // has_padding: (0x10: true)
        )

        val decoder = Kaitai("enum_types", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate field protocol
        var protocol = result.bytesListTree["protocol"]
        check(protocol is KaitaiEnum) { "Expected KaitaiEnum, got ${protocol::class.simpleName}" }
        checkElement(protocol, id="protocol", elementClass=KaitaiEnum::class, sourceByteRange=Pair(0, 4), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x00, 0x00, 0x00, 0x06))
        var enum: Pair<KTEnum?, String> = Pair(struct.enums["ip_protocols"], "tcp")
        check(protocol.enum.first === enum.first) {"Expected ${enum.first}, got ${protocol.enum.first}"}
        check(protocol.enum.second == enum.second) {"Expected ${enum.second}, got ${protocol.enum.second}"}

        // Validate field protocol_huge
        protocol = result.bytesListTree["protocol_huge"]
        check(protocol is KaitaiEnum) { "Expected KaitaiEnum, got ${protocol::class.simpleName}" }
        checkElement(protocol, id="protocol_huge", elementClass=KaitaiEnum::class, sourceByteRange=Pair(4, 8), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xB2, 0xD0, 0x5E, 0x00))
        enum = Pair(struct.enums["ip_protocols"], "some_protocol")
        check(protocol.enum.first === enum.first) {"Expected ${enum.first}, got ${protocol.enum.first}"}
        check(protocol.enum.second == enum.second) {"Expected ${enum.second}, got ${protocol.enum.second}"}

        // Validate field verbose negative
        protocol = result.bytesListTree["verbose_negative"]
        check(protocol is KaitaiEnum) { "Expected KaitaiEnum, got ${protocol::class.simpleName}" }
        checkElement(protocol, id="verbose_negative", elementClass=KaitaiEnum::class, sourceByteRange=Pair(8, 12), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0xff, 0xff, 0xff, 0xff))
        enum = Pair(struct.enums["verbose_levels"], "negative")
        check(protocol.enum.first === enum.first) {"Expected ${enum.first}, got ${protocol.enum.first}"}
        check(protocol.enum.second == enum.second) {"Expected ${enum.second}, got ${protocol.enum.second}"}

        // Validate field verbose positive
        protocol = result.bytesListTree["verbose_positive"]
        check(protocol is KaitaiEnum) { "Expected KaitaiEnum, got ${protocol::class.simpleName}" }
        checkElement(protocol, id="verbose_positive", elementClass=KaitaiEnum::class, sourceByteRange=Pair(12, 16), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x00, 0x00, 0x00, 0x02))
        enum = Pair(struct.enums["verbose_levels"], "medium")
        check(protocol.enum.first === enum.first) {"Expected ${enum.first}, got ${protocol.enum.first}"}
        check(protocol.enum.second == enum.second) {"Expected ${enum.second}, got ${protocol.enum.second}"}

        // Validate field flags
        protocol = result.bytesListTree["flags"]
        check(protocol is KaitaiEnum) { "Expected KaitaiEnum, got ${protocol::class.simpleName}" }
        checkElement(protocol, id="flags", elementClass=KaitaiEnum::class, sourceByteRange=Pair(16, 16), sourceRangeBitOffset=Pair(0, 1), value=booleanArrayOf(true))
        enum = Pair(struct.enums["bit_flags"], "flag2")
        check(protocol.enum.first === enum.first) {"Expected ${enum.first}, got ${protocol.enum.first}"}
        check(protocol.enum.second == enum.second) {"Expected ${enum.second}, got ${protocol.enum.second}"}

        // Validate field padding
        protocol = result.bytesListTree["padding"]
        check(protocol is KaitaiEnum) { "Expected KaitaiEnum, got ${protocol::class.simpleName}" }
        checkElement(protocol, id="padding", elementClass=KaitaiEnum::class, sourceByteRange=Pair(17, 18), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x10))
        enum = Pair(struct.enums["has_padding"], "true")
        check(protocol.enum.first === enum.first) {"Expected ${enum.first}, got ${protocol.enum.first}"}
        check(protocol.enum.second == enum.second) {"Expected ${enum.second}, got ${protocol.enum.second}"}
    }

    @Test
    fun testIfConditions() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "if_false", type = KTType.Primitive("u1"), ifCondition = StringOrBoolean.StringValue("1 == 2")),
                KTSeq(id = "if_true", type = KTType.Primitive("u1"), ifCondition = StringOrBoolean.BooleanValue(true)),
            )
        )

        val data = byteArrayOfInts(
            0x11,
            0x22
        )

        val decoder = Kaitai("enum_types", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        try {
            result.bytesListTree["if_false"]
            check(false)
        } catch (e: Exception) {
            check(e.message == "Could not find element with id if_false") {"Exception \"Could not find element with id if_false\" was expected but $e was thrown."}
        }

        val element = result.bytesListTree["if_true"]
        checkElement(element, id="if_true", elementClass=KaitaiUnsignedInteger::class, sourceByteRange=Pair(0, 1), sourceRangeBitOffset=Pair(0, 0), value=booleanArrayOfInts(0x11))
    }

    @Test
    fun testSwitchOn() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "switch-on",
            ),
            seq = listOf(
                KTSeq(id = "signature", size = StringOrInt.IntValue(4)),
                KTSeq(id = "field", type = KTType.Primitive("sub")),
                KTSeq(id = "number_type1", type = KTType.Primitive("u2")),
                KTSeq(id = "number_body1", type = KTType.Switch(
                    switchOn = "number_type1",
                    cases = mapOf(
                        "0x0201" to "u2",
                        "0x0403" to "s2",
                        "0x0605" to "u4",
                        "0x0807" to "s4"
                    )
                )),
                KTSeq(id = "number_type2", type = KTType.Primitive("u2")),
                KTSeq(id = "number_body2", type = KTType.Switch(
                    switchOn = "number_type2",
                    cases = mapOf(
                        "513" to "u2",
                        "1027" to "s2",
                        "1541" to "u4",
                        "2055" to "s4"
                    )
                )),
                KTSeq(id = "number_type_default", type = KTType.Primitive("u1")),
                KTSeq(id = "number_body_default", type = KTType.Switch(
                    switchOn = "number_type_default",
                    cases = mapOf(
                        "0x01" to "u1",
                        "0x02" to "s1",
                        "_" to "u2",
                    )
                )),
            ),
            types = mapOf(
                Pair(
                    "sub",
                    KTStruct(
                        meta = KTMeta(
                            endian = KTEndian.Switch(
                                switchOn = "_root.signature",
                                cases = mapOf(
                                    "[0xde, 0x12, 0x04, 0x95]" to KTEndianEnum.LE,
                                    "[0x95, 0x04, 0x12, 0xde]" to KTEndianEnum.BE
                                )
                            )
                        ),
                        seq = listOf(
                            KTSeq(id = "sub-field", type = KTType.Primitive("u2"))
                        )
                    )
                )
            )
        )

        val data = byteArrayOfInts(
            0xde, 0x12, 0x04, 0x95, // signature
            0x11, 0x00, // field (17 le)
            0x04, 0x03, // number_type1 (1027)
            0xFF, 0xB2, // number_body1 (-78)
            0x06, 0x05, // number_type2 (1541)
            0xFF, 0xB2, 0xF2, 0x38, // number_body1 (4289917496)
            0x03, // number_type_default (3)
            0x04, 0x01 // number_body_default (1025)
        )

        val decoder = Kaitai("switch-on", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        checkElement(result, "switch-on", KaitaiResult::class, Pair(0, 19), Pair(0, 0))
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        // Validate field signature
        val fieldSignature = result.bytesListTree["signature"]
        checkElement(fieldSignature, "signature", KaitaiBytes::class, Pair(0, 4), Pair(0, 0), booleanArrayOfInts(0xde, 0x12, 0x04, 0x95))

        // Validate field 'field'
        val field = result.bytesListTree["field"]
        checkElement(field, "field", KaitaiResult::class, Pair(4, 6), Pair(0, 0))

        // Validate field sub-field
        val subField = field.bytesListTree?.get("sub-field")
        checkNotNull(subField) { "Expected sub-field to be part pf field" }
        checkElement(subField, "sub-field", KaitaiUnsignedInteger::class, Pair(4, 6), Pair(0, 0), htmlInnerContent = "sub-field(17)u")

        // Validate field number_type1
        val fieldNumberType1 = result.bytesListTree["number_type1"]
        checkElement(fieldNumberType1, "number_type1", KaitaiUnsignedInteger::class, Pair(6, 8), Pair(0, 0), htmlInnerContent = "number_type1(1027)u")

        // Validate field number_body1
        val fieldNumberBody1 = result.bytesListTree["number_body1"]
        checkElement(fieldNumberBody1, "number_body1", KaitaiSignedInteger::class, Pair(8, 10), Pair(0, 0), htmlInnerContent = "number_body1(-78)s")

        // Validate field number_type2
        val fieldNumberType2 = result.bytesListTree["number_type2"]
        checkElement(fieldNumberType2, "number_type2", KaitaiUnsignedInteger::class, Pair(10, 12), Pair(0, 0), htmlInnerContent = "number_type2(1541)u")

        // Validate field number_body2
        val fieldNumberBody2 = result.bytesListTree["number_body2"]
        checkElement(fieldNumberBody2, "number_body2", KaitaiUnsignedInteger::class, Pair(12, 16), Pair(0, 0), htmlInnerContent = "number_body2(4289917496)u")

        // Validate field number_type_default
        val fieldNumberTypeDefault = result.bytesListTree["number_type_default"]
        checkElement(fieldNumberTypeDefault, "number_type_default", KaitaiUnsignedInteger::class, Pair(16, 17), Pair(0, 0), htmlInnerContent = "number_type_default(3)u")

        // Validate field number_body_default
        val fieldNumberBodyDefault = result.bytesListTree["number_body_default"]
        checkElement(fieldNumberBodyDefault, "number_body_default", KaitaiUnsignedInteger::class, Pair(17, 19), Pair(0, 0), htmlInnerContent = "number_body_default(1025)u")
    }
}
