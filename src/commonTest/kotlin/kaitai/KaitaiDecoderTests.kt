package kaitai

import bitmage.ByteOrder
import bitmage.byteArrayOfInts
import bitmage.toBooleanArray
import decoders.Kaitai
import decoders.KaitaiBinary
import decoders.KaitaiBytes
import decoders.KaitaiList
import decoders.KaitaiResult
import decoders.KaitaiSignedInteger
import decoders.KaitaiString
import decoders.KaitaiUnsignedInteger
import kaitai.KaitaiTestUtils.checkElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
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
        checkElement(result, "example", Pair(0, 11), Pair(0, 0))
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        // Validate field1
        val field1 = result.bytesListTree["field1"]
        check(field1 is KaitaiUnsignedInteger) { "Expected field1 to be KaitaiUnsignedInteger, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", Pair(0, 4), Pair(0, 0), "field1(1)u")

        // Validate field2
        val field2 = result.bytesListTree["field2"]
        check(field2 is KaitaiString) { "Expected field2 to be KaitaiString, got ${field2::class.simpleName}" }
        val expectedField2Content = "Hello" + "\u0000" // Include null terminator
        checkElement(field2, "field2", Pair(4, 10), Pair(0, 0), "field2($expectedField2Content)utf8")

        // Validate field3
        val field3 = result.bytesListTree["field3"]
        check(field3 is KaitaiSignedInteger) { "Expected field3 to be KaitaiSignedInteger, got ${field3::class.simpleName}" }
        checkElement(field3, "field3", Pair(10, 11), Pair(0, 0), "field3(-127)s")
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
        checkElement(result, "unsigned_integers", Pair(0, data.size), Pair(0, 0))
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        // Validate field1
        val field1 = result.bytesListTree["field1"]
        check(field1 is KaitaiUnsignedInteger) { "Expected field1 to be KaitaiUnsignedInteger, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", Pair(0, 1), Pair(0, 0), "field1(1)u")

        // Validate field2
        val field2 = result.bytesListTree["field2"]
        check(field2 is KaitaiUnsignedInteger) { "Expected field2 to be KaitaiUnsignedInteger, got ${field2::class.simpleName}" }
        checkElement(field2, "field2", Pair(1, 3), Pair(0, 0), "field2(515)u")

        // Validate field3
        val field3 = result.bytesListTree["field3"]
        check(field3 is KaitaiUnsignedInteger) { "Expected field3 to be KaitaiUnsignedInteger, got ${field3::class.simpleName}" }
        checkElement(field3, "field3", Pair(3, 7), Pair(0, 0), "field3(67438087)u")

        // Validate field4
        val field4 = result.bytesListTree["field4"]
        check(field4 is KaitaiUnsignedInteger) { "Expected field4 to be KaitaiUnsignedInteger, got ${field4::class.simpleName}" }
        checkElement(field4, "field4", Pair(7, 15), Pair(0, 0), "field4(579005069656919567)u")

        // Validate field5
        val field5 = result.bytesListTree["field5"]
        check(field5 is KaitaiUnsignedInteger) { "Expected field5 to be KaitaiUnsignedInteger, got ${field5::class.simpleName}" }
        checkElement(field5, "field5", Pair(15, 16), Pair(0, 0), "field5(97)u")

        // Validate field6
        val field6 = result.bytesListTree["field6"]
        check(field6 is KaitaiUnsignedInteger) { "Expected field6 to be KaitaiUnsignedInteger, got ${field6::class.simpleName}" }
        checkElement(field6, "field6", Pair(16, 17), Pair(0, 0), "field6(165)u")

        // Validate field7
        val field7 = result.bytesListTree["field7"]
        check(field7 is KaitaiUnsignedInteger) { "Expected field7 to be KaitaiUnsignedInteger, got ${field7::class.simpleName}" }
        checkElement(field7, "field7", Pair(17, 19), Pair(0, 0), "field7(27413)u")

        // Validate field8
        val field8 = result.bytesListTree["field8"]
        check(field8 is KaitaiUnsignedInteger) { "Expected field8 to be KaitaiUnsignedInteger, got ${field8::class.simpleName}" }
        checkElement(field8, "field8", Pair(19, 21), Pair(0, 0), "field8(45402)u")
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
        checkElement(result, "signed_integers", Pair(0, data.size), Pair(0, 0))
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        // Validate field1
        val field1 = result.bytesListTree["field1"]
        check(field1 is KaitaiSignedInteger) { "Expected field1 to be KaitaiSignedInteger, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", Pair(0, 1), Pair(0, 0), "field1(122)s")

        // Validate field2
        val field2 = result.bytesListTree["field2"]
        check(field2 is KaitaiSignedInteger) { "Expected field2 to be KaitaiSignedInteger, got ${field2::class.simpleName}" }
        checkElement(field2, "field2", Pair(1, 3), Pair(0, 0), "field2(1441)s")

        // Validate field3
        val field3 = result.bytesListTree["field3"]
        check(field3 is KaitaiSignedInteger) { "Expected field3 to be KaitaiSignedInteger, got ${field3::class.simpleName}" }
        checkElement(field3, "field3", Pair(3, 7), Pair(0, 0), "field3(1354825755)s")

        // Validate field4
        val field4 = result.bytesListTree["field4"]
        check(field4 is KaitaiSignedInteger) { "Expected field4 to be KaitaiSignedInteger, got ${field4::class.simpleName}" }
        checkElement(field4, "field4", Pair(7, 15), Pair(0, 0), "field4(6917706421467349044)s")

        // Validate field5
        val field5 = result.bytesListTree["field5"]
        check(field5 is KaitaiSignedInteger) { "Expected field5 to be KaitaiSignedInteger, got ${field5::class.simpleName}" }
        checkElement(field5, "field5", Pair(15, 16), Pair(0, 0), "field5(-91)s")

        // Validate field6
        val field6 = result.bytesListTree["field6"]
        check(field6 is KaitaiSignedInteger) { "Expected field6 to be KaitaiSignedInteger, got ${field6::class.simpleName}" }
        checkElement(field6, "field6", Pair(16, 18), Pair(0, 0), "field6(-20134")

        // Validate field7
        val field7 = result.bytesListTree["field7"]
        check(field7 is KaitaiSignedInteger) { "Expected field7 to be KaitaiSignedInteger, got ${field7::class.simpleName}" }
        checkElement(field7, "field7", Pair(18, 22), Pair(0, 0), "field7(-520068864)s")

        // Validate field8
        val field8 = result.bytesListTree["field8"]
        check(field8 is KaitaiSignedInteger) { "Expected field8 to be KaitaiSignedInteger, got ${field8::class.simpleName}" }
        checkElement(field8, "field8", Pair(22, 30), Pair(0, 0), "field8(-792456140590940108)s")
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
        check(field1 is KaitaiResult) { "Expected field1 to be KaitaiResult, got ${field1::class.simpleName}" }
        checkElement(field1, "subElementsTooShort", Pair(0, 7), Pair(0, 0))
        check(field1.bytesListTree.size == 2) { "Expected subtype to have exactly 2 elements, got ${field1.bytesListTree.size}" }
        check(field1.bytesListTree["code"].value.contentEquals(byteArrayOfInts(0xc0, 0xde).toBooleanArray())) {
            "Expected subtype.code to be exactly 0xc0 0xde and not ${field1.bytesListTree["code"].value}"
        }
        check(field1.bytesListTree["name"].value.contentEquals(byteArrayOfInts(0x61, 0x62, 0x63, 0x64, 0x65).toBooleanArray())) {
            "Expected subtype.name to be exactly 0xc0de and not ${field1.bytesListTree["name"].value}"
        }

        val field2 = result.bytesListTree["subElementsJustRight"]
        check(field2 is KaitaiResult) { "Expected field2 to be KaitaiResult, got ${field2::class.simpleName}" }
        checkElement(field2, "subElementsJustRight", Pair(10, 17), Pair(0, 0))
        check(field2.bytesListTree.size == 2) { "Expected subtype to have exactly 2 elements, got ${field2.bytesListTree.size}" }
        check(field2.bytesListTree["code"].value.contentEquals(byteArrayOfInts(0xc0, 0xde).toBooleanArray())) {
            "Expected subtype.code to be exactly 0xc0de and not ${field2.bytesListTree["code"].value}"
        }
        check(field2.bytesListTree["name"].value.contentEquals(byteArrayOfInts(0x71, 0x72, 0x73, 0x74, 0x75).toBooleanArray())) {
            "Expected subtype.name to be exactly 0xc0de and not ${field2.bytesListTree["name"].value}"
        }

        val field3 = result.bytesListTree["subElementsSpeakForThemselves"]
        check(field3 is KaitaiResult) { "Expected field3 to be KaitaiResult, got ${field3::class.simpleName}" }
        checkElement(field3, "subElementsSpeakForThemselves", Pair(17, 24), Pair(0, 0))
        check(field3.bytesListTree.size == 2) { "Expected subtype to have exactly 2 elements, got ${field3.bytesListTree.size}" }
        check(field3.bytesListTree["code"].value.contentEquals(byteArrayOfInts(0xc0, 0xde).toBooleanArray())) {
            "Expected subtype.code to be exactly 0xc0de and not ${field3.bytesListTree["code"].value}"
        }
        check(field3.bytesListTree["name"].value.contentEquals(byteArrayOfInts(0x41, 0x42, 0x43, 0x44, 0x45).toBooleanArray())) {
            "Expected subtype.name to be exactly 0xc0de and not ${field3.bytesListTree["name"].value}"
        }
    }

    @Test
    fun testBitwiseOffsets() {
        val struct = KTStruct(
            seq = listOf(
                KTSeq(id = "a", type = KTType.Primitive("b4")),
                KTSeq(id = "b", type = KTType.Primitive("b5")),
                KTSeq(id = "c", type = KTType.Primitive("b7"))
            ),
        )

        val data = byteArrayOfInts(
            // subElementsTooShort, not a problem
            0x12, 0xab,
        )

        val decoder = Kaitai("bitwise_offsets", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        check(result.bytesListTree.size == struct.seq.size) {
            "Expected bytesListTree to have ${struct.seq.size} elements, got ${result.bytesListTree.size}"
        }

        val a = result.bytesListTree["a"]
        check(a is KaitaiBinary) { "Expected a to be KaitaiBinary, got ${a::class.simpleName}" }
        checkElement(a, "a", Pair(0,0), Pair(0, 4))

        val b = result.bytesListTree["b"]
        check(b is KaitaiBinary) { "Expected b to be KaitaiBinary, got ${b::class.simpleName}" }
        checkElement(b, "b", Pair(0, 1), Pair(4, 1))

        val c = result.bytesListTree["c"]
        check(c is KaitaiBinary) { "Expected c to be KaitaiBinary, got ${c::class.simpleName}" }
        checkElement(c, "c", Pair(1, 2), Pair(1, 0))
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
        check(repeat_by_expr is KaitaiList) { "Expected repeat_by_expr to be KaitaiList, got ${repeat_by_expr::class.simpleName}" }
        checkElement(repeat_by_expr, "repeat_by_expr", Pair(0,3), Pair(0, 0))
        check(repeat_by_expr.value.contentEquals(byteArrayOfInts(0xaa, 0xaa, 0xaa).toBooleanArray())) {
            "Expected repeat_by_expr to be exactly 0xaa, 0xaa, 0xaa and not ${repeat_by_expr.value}"
        }
        val aa0 = repeat_by_expr.bytesListTree[0]
        check(aa0 is KaitaiBytes) { "Expected repeat_by_expr.0 to be KaitaiBytes, got ${aa0::class.simpleName}" }
        checkElement(repeat_by_expr, "repeat_by_expr", Pair(0,1), Pair(0, 0))
        check(repeat_by_expr.value.contentEquals(byteArrayOfInts(0xaa).toBooleanArray())) {
            "Expected repeat_by_expr.0 to be exactly 0xaa and not ${aa0.value}"
        }
        val aa1 = repeat_by_expr.bytesListTree[1]
        check(aa1 is KaitaiBytes) { "Expected repeat_by_expr.1 to be KaitaiBytes, got ${aa1::class.simpleName}" }
        checkElement(aa1, "repeat_by_expr", Pair(1,2), Pair(0, 0))
        check(aa1.value.contentEquals(byteArrayOfInts(0xaa).toBooleanArray())) {
            "Expected repeat_by_expr.1 to be exactly 0xaa and not ${aa1.value}"
        }
        val aa2 = repeat_by_expr.bytesListTree[1]
        check(aa2 is KaitaiBytes) { "Expected repeat_by_expr.2 to be KaitaiBytes, got ${aa2::class.simpleName}" }
        checkElement(aa2, "repeat_by_expr", Pair(2,3), Pair(0, 0))
        check(aa2.value.contentEquals(byteArrayOfInts(0xaa).toBooleanArray())) {
            "Expected aa.2 to be exactly 0xaa and not ${aa2.value}"
        }

        val set_size = result.bytesListTree["set_size"]
        check(set_size is KaitaiResult) { "Expected set_size to be KaitaiList, got ${set_size::class.simpleName}" }
        checkElement(set_size, "set_size", Pair(3,7), Pair(0, 0))
        check(set_size.value.contentEquals(byteArrayOfInts(0x11, 0x22, 0x33, 0x44).toBooleanArray())) {
            "Expected set_size to be exactly 0x11, 0x22, 0x33, 0x44 and not ${set_size.value}"
        }
        val set_size_x = set_size.bytesListTree["x"]
        check(set_size_x is KaitaiList) { "Expected set_size.x to be KaitaiList, got ${set_size_x::class.simpleName}" }
        checkElement(set_size_x, "x", Pair(3,7), Pair(0, 0))
        check(set_size_x.value.contentEquals(byteArrayOfInts(0x11, 0x22, 0x33, 0x44).toBooleanArray())) {
            "Expected set_size.x to be exactly 0x11, 0x22 and not ${set_size_x.value}"
        }
        val set_size_x_0 = set_size.bytesListTree["x"]
        check(set_size_x_0 is KaitaiBytes) { "Expected set_size.x.0 to be KaitaiBytes, got ${set_size_x_0::class.simpleName}" }
        checkElement(set_size_x_0, "x", Pair(3,5), Pair(0, 0))
        check(set_size_x_0.value.contentEquals(byteArrayOfInts(0x11, 0x22).toBooleanArray())) {
            "Expected set_size.x.0 to be exactly 0x11, 0x22 and not ${set_size_x_0.value}"
        }
        val set_size_x_1 = set_size.bytesListTree["x"]
        check(set_size_x_1 is KaitaiBytes) { "Expected set_size.x.1 to be KaitaiBytes, got ${set_size_x_1::class.simpleName}" }
        checkElement(set_size_x_1, "x", Pair(5,7), Pair(0, 0))
        check(set_size_x_1.value.contentEquals(byteArrayOfInts(0x33, 0x44).toBooleanArray())) {
            "Expected set_size.x.1 to be exactly 0x33, 0x44 and not ${set_size_x_1.value}"
        }

        val simple_eos = result.bytesListTree["simple_eos"]
        check(simple_eos is KaitaiList) { "Expected simple_eos to be KaitaiList, got ${simple_eos::class.simpleName}" }
        checkElement(simple_eos, "simple_eos", Pair(7,9), Pair(0, 0))
        check(simple_eos.value.contentEquals(byteArrayOfInts(0x12, 0x34).toBooleanArray())) {
            "Expected simple_eos to be exactly 0x12, 0x34 and not ${simple_eos.value}"
        }
        val simple_eos_0 = simple_eos.bytesListTree[0]
        check(simple_eos_0 is KaitaiResult) { "Expected simple_eos.0 to be KaitaiResult, got ${simple_eos_0::class.simpleName}" }
        checkElement(simple_eos_0, "simple_eos", Pair(7,8), Pair(0, 0))
        check(simple_eos_0.value.contentEquals(byteArrayOfInts(0x12).toBooleanArray())) {
            "Expected simple_eos.0 to be exactly 0x12 and not ${simple_eos_0.value}"
        }
        val simple_eos_0_a = simple_eos_0.bytesListTree["a"]
        check(simple_eos_0_a is KaitaiBytes) { "Expected simple_eos.0.a to be KaitaiBytes, got ${simple_eos_0_a::class.simpleName}" }
        checkElement(simple_eos_0_a, "simple_eos", Pair(7,8), Pair(0, 0))
        check(simple_eos_0_a.value.contentEquals(byteArrayOfInts(0x12).toBooleanArray())) {
            "Expected simple_eos.0.a to be exactly 0x12 and not ${simple_eos_0_a.value}"
        }
        val simple_eos_1 = simple_eos.bytesListTree[0]
        check(simple_eos_1 is KaitaiResult) { "Expected simple_eos.1 to be KaitaiResult, got ${simple_eos_1::class.simpleName}" }
        checkElement(simple_eos_1, "simple_eos", Pair(8,9), Pair(0, 0))
        check(simple_eos_1.value.contentEquals(byteArrayOfInts(0x34).toBooleanArray())) {
            "Expected simple_eos.1 to be exactly 0x34 and not ${simple_eos_1.value}"
        }
        val simple_eos_1_a = simple_eos_0.bytesListTree["a"]
        check(simple_eos_1_a is KaitaiBytes) { "Expected simple_eos.1.a to be KaitaiBytes, got ${simple_eos_1_a::class.simpleName}" }
        checkElement(simple_eos_1_a, "simple_eos", Pair(8,9), Pair(0, 0))
        check(simple_eos_1_a.value.contentEquals(byteArrayOfInts(0x34).toBooleanArray())) {
            "Expected simple_eos.1.a to be exactly 0x34 and not ${simple_eos_1_a.value}"
        }
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
        check(header is KaitaiBytes) { "Expected a to be KaitaiBytes, got ${header::class.simpleName}" }
        checkElement(header, "header", Pair(0, 1), Pair(0, 0))
        check(header.value.contentEquals(byteArrayOfInts(0x12).toBooleanArray())) { "Expected header to be 0x12, got ${header.value}" }

        val dummy0 = result.bytesListTree["dummy0"]
        check(dummy0 is KaitaiBytes) { "Expected a to be KaitaiBytes, got ${dummy0::class.simpleName}" }
        check(dummy0.value.isEmpty()) { "Expected size to be 0, got ${dummy0.value.size}" }
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
        check(trailer is KaitaiBytes) { "Expected a to be KaitaiBytes, got ${trailer::class.simpleName}" }
        checkElement(trailer, "trailer", Pair(1, 2), Pair(0, 0))
        check(trailer.value.contentEquals(byteArrayOfInts(0xab).toBooleanArray())) { "Expected header to be 0x12, got ${trailer.value}" }
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
        check(root1 is KaitaiResult) { "Expected a to be KaitaiResult, got ${root1::class.simpleName}" }
        checkElement(root1, "root1", Pair(0,4), Pair(0, 0))

        val upper1 = root1.bytesListTree["upper1"]
        check(upper1 is KaitaiResult) { "Expected upper1 to be KaitaiResult, got ${upper1::class.simpleName}" }
        checkElement(upper1, "upper1", Pair(0, 1), Pair(0, 0))
        val upper2 = root1.bytesListTree["upper2"]
        check(upper2 is KaitaiResult) { "Expected upper2 to be KaitaiResult, got ${upper2::class.simpleName}" }
        checkElement(upper2, "upper2", Pair(1, 2), Pair(0, 0))
        val upper3 = root1.bytesListTree["upper3"]
        check(upper3 is KaitaiResult) { "Expected upper3 to be KaitaiResult, got ${upper3::class.simpleName}" }
        checkElement(upper3, "upper3", Pair(2, 3), Pair(0, 0))
        val upper4 = root1.bytesListTree["upper4"]
        check(upper4 is KaitaiResult) { "Expected upper4 to be KaitaiResult, got ${upper4::class.simpleName}" }
        checkElement(upper4, "upper4", Pair(3, 4), Pair(0, 0))

        val root2 = result.bytesListTree["root2"]
        check(root2 is KaitaiResult) { "Expected a to be KaitaiResult, got ${root2::class.simpleName}" }
        checkElement(root2, "root2", Pair(4,5), Pair(0, 0))
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
        check(field1 is KaitaiResult) { "Expected field1 to be KaitaiResult, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", Pair(0, 4), Pair(0, 0))
        check(field1.value.contentEquals(byteArrayOfInts(0x11, 0x11, 0x11, 0x22).toBooleanArray())) {
            "Expected field1 to be exactly 0x11, 0x11, 0x11, 0x22  and not ${field1.value}"
        }
        check(field1.bytesListTree.size == 2) { "Expected subtype to have exactly 2 elements, got ${field1.bytesListTree.size}" }

        val field1_sub1 = field1.bytesListTree["sub1"]
        check(field1_sub1 is KaitaiResult) { "Expected field1.sub1 to be KaitaiResult, got ${field1_sub1::class.simpleName}" }
        checkElement(field1_sub1, "sub1", Pair(0, 2), Pair(0, 0))
        check(field1_sub1.value.contentEquals(byteArrayOfInts(0x11, 0x11).toBooleanArray())) {
            "Expected field1.sub1 to be exactly 0x11, 0x11 and not ${field1_sub1.value}"
        }
        check(field1_sub1.bytesListTree.size == 1) { "Expected subtype to have exactly 1 elements, got ${field1_sub1.bytesListTree.size}" }

        val field1_sub1_leaf = field1_sub1.bytesListTree["leaf"]
        check(field1_sub1_leaf is KaitaiBytes) { "Expected field1.sub1.leaf to be KaitaiBytes, got ${field1_sub1_leaf::class.simpleName}" }
        checkElement(field1_sub1_leaf, "leaf", Pair(0, 2), Pair(0, 0))
        check(field1_sub1_leaf.value.contentEquals(byteArrayOfInts(0x11, 0x11).toBooleanArray())) {
            "Expected field1.sub1.leaf to be exactly 0x11, 0x11 and not ${field1_sub1_leaf.value}"
        }

        val field1_sub2 = field1.bytesListTree["sub2"]
        check(field1_sub2 is KaitaiResult) { "Expected field1.sub2 to be KaitaiResult, got ${field1_sub2::class.simpleName}" }
        checkElement(field1_sub2, "sub2", Pair(2, 4), Pair(0, 0))
        check(field1_sub2.value.contentEquals(byteArrayOfInts(0x11, 0x22).toBooleanArray())) {
            "Expected field1.sub2 to be exactly 0x11, 0x22 and not ${field1_sub2.value}"
        }
        check(field1_sub2.bytesListTree.size == 1) { "Expected subtype to have exactly 2 elements, got ${field1_sub2.bytesListTree.size}" }

        val field1_sub2_leaf = field1_sub2.bytesListTree["leaf"]
        check(field1_sub2_leaf is KaitaiBytes) { "Expected field1.sub2.leaf to be KaitaiBytes, got ${field1_sub2_leaf::class.simpleName}" }
        checkElement(field1_sub2_leaf, "leaf", Pair(2, 4), Pair(0, 0))
        check(field1_sub2_leaf.value.contentEquals(byteArrayOfInts(0x11, 0x22).toBooleanArray())) {
            "Expected field1.sub2.leaf to be exactly 0x11, 0x22 and not ${field1_sub2_leaf.value}"
        }

        val field2 = result.bytesListTree["field2"]
        check(field2 is KaitaiResult) { "Expected field2 to be KaitaiResult, got ${field2::class.simpleName}" }
        checkElement(field2, "field2", Pair(4, 8), Pair(0, 0))
        check(field2.value.contentEquals(byteArrayOfInts(0x22, 0x11, 0x22, 0x22).toBooleanArray())) {
            "Expected field2 to be exactly 0x22, 0x11, 0x22, 0x22  and not ${field2.value}"
        }
        check(field2.bytesListTree.size == 2) { "Expected subtype to have exactly 2 elements, got ${field2.bytesListTree.size}" }

        val field2_sub1 = field2.bytesListTree["sub1"]
        check(field2_sub1 is KaitaiResult) { "Expected field2.sub1 to be KaitaiResult, got ${field2_sub1::class.simpleName}" }
        checkElement(field2_sub1, "sub1", Pair(4, 6), Pair(0, 0))
        check(field2_sub1.value.contentEquals(byteArrayOfInts(0x22, 0x11).toBooleanArray())) {
            "Expected field2.sub1 to be exactly 0x22, 0x11 and not ${field2_sub1.value}"
        }
        check(field2_sub1.bytesListTree.size == 1) { "Expected subtype to have exactly 2 elements, got ${field2_sub1.bytesListTree.size}" }

        val field2_sub1_leaf = field2_sub1.bytesListTree["leaf"]
        check(field2_sub1_leaf is KaitaiBytes) { "Expected field2.sub1.leaf to be KaitaiBytes, got ${field2_sub1_leaf::class.simpleName}" }
        checkElement(field2_sub1_leaf, "leaf", Pair(4, 6), Pair(0, 0))
        check(field2_sub1_leaf.value.contentEquals(byteArrayOfInts(0x22, 0x11).toBooleanArray())) {
            "Expected field2.sub1.leaf to be exactly 0x22, 0x11 and not ${field2_sub1_leaf.value}"
        }

        val field2_sub2 = field2.bytesListTree["sub2"]
        check(field2_sub2 is KaitaiResult) { "Expected field2.sub2 to be KaitaiResult, got ${field2_sub2::class.simpleName}" }
        checkElement(field2_sub2, "sub2", Pair(6, 8), Pair(0, 0))
        check(field2_sub2.value.contentEquals(byteArrayOfInts(0x22, 0x22).toBooleanArray())) {
            "Expected field2.sub2 to be exactly 0x22, 0x22 and not ${field2_sub2.value}"
        }
        check(field2_sub2.bytesListTree.size == 1) { "Expected subtype to have exactly 2 elements, got ${field2_sub2.bytesListTree.size}" }

        val field2_sub2_leaf = field2_sub2.bytesListTree["leaf"]
        check(field2_sub2_leaf is KaitaiBytes) { "Expected field2.sub2.leaf to be KaitaiBytes, got ${field2_sub2_leaf::class.simpleName}" }
        checkElement(field2_sub2_leaf, "leaf", Pair(6, 8), Pair(0, 0))
        check(field2_sub2_leaf.value.contentEquals(byteArrayOfInts(0x22, 0x22).toBooleanArray())) {
            "Expected field2.sub2.leaf to be exactly 0x22, 0x22 and not ${field2_sub2_leaf.value}"
        }
    }
}
