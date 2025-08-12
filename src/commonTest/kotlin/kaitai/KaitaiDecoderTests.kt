package kaitai

import bitmage.ByteOrder
import decoders.Kaitai
import decoders.KaitaiResult
import decoders.KaitaiSignedInteger
import decoders.KaitaiString
import decoders.KaitaiUnsignedInteger
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

        // Example data: field1=1, field2="Hello", field3=-127
        val data = byteArrayOf(
            0x00, 0x00, 0x00, 0x01,
            0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x00,
            0x81.toByte(),
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
        val field1 = result.bytesListTree[0]
        check(field1 is KaitaiUnsignedInteger) { "Expected field1 to be KaitaiUnsignedInteger, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", Pair(0, 4), Pair(0, 0), "field1(1)u")

        // Validate field2
        val field2 = result.bytesListTree[1]
        check(field2 is KaitaiString) { "Expected field2 to be KaitaiString, got ${field2::class.simpleName}" }
        val expectedField2Content = "Hello" + "\u0000" // Include null terminator
        checkElement(field2, "field2", Pair(4, 10), Pair(0, 0), "field2($expectedField2Content)utf8")

        // Validate field3
        val field3 = result.bytesListTree[2]
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

        // Example data: field1=1, field2=515, field3=67438087, field4=579005069656919567,
        // field5=97, field6=-91, field7=27413, field8=45402
        val data = byteArrayOf(
            0x01,
            0x02, 0x03,
            0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x61,
            0xa5.toByte(),
            0x6b, 0x15,
            0xb1.toByte(), 0x5a,
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
        val field1 = result.bytesListTree[0]
        check(field1 is KaitaiUnsignedInteger) { "Expected field1 to be KaitaiUnsignedInteger, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", Pair(0, 1), Pair(0, 0), "field1(1)u")

        // Validate field2
        val field2 = result.bytesListTree[1]
        check(field2 is KaitaiUnsignedInteger) { "Expected field2 to be KaitaiUnsignedInteger, got ${field2::class.simpleName}" }
        checkElement(field2, "field2", Pair(1, 3), Pair(0, 0), "field2(515)u")

        // Validate field3
        val field3 = result.bytesListTree[2]
        check(field3 is KaitaiUnsignedInteger) { "Expected field3 to be KaitaiUnsignedInteger, got ${field3::class.simpleName}" }
        checkElement(field3, "field3", Pair(3, 7), Pair(0, 0), "field3(67438087)u")

        // Validate field4
        val field4 = result.bytesListTree[3]
        check(field4 is KaitaiUnsignedInteger) { "Expected field4 to be KaitaiUnsignedInteger, got ${field4::class.simpleName}" }
        checkElement(field4, "field4", Pair(7, 15), Pair(0, 0), "field4(579005069656919567)u")

        // Validate field5
        val field5 = result.bytesListTree[4]
        check(field5 is KaitaiUnsignedInteger) { "Expected field5 to be KaitaiUnsignedInteger, got ${field5::class.simpleName}" }
        checkElement(field5, "field5", Pair(15, 16), Pair(0, 0), "field5(97)u")

        // Validate field6
        val field6 = result.bytesListTree[5]
        check(field6 is KaitaiUnsignedInteger) { "Expected field6 to be KaitaiUnsignedInteger, got ${field6::class.simpleName}" }
        checkElement(field6, "field6", Pair(16, 17), Pair(0, 0), "field6(165)u")

        // Validate field7
        val field7 = result.bytesListTree[6]
        check(field7 is KaitaiUnsignedInteger) { "Expected field7 to be KaitaiUnsignedInteger, got ${field7::class.simpleName}" }
        checkElement(field7, "field7", Pair(17, 19), Pair(0, 0), "field7(27413)u")

        // Validate field8
        val field8 = result.bytesListTree[7]
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

        // Example data: field1=122, field2=1441, field3=1354825755, field4=6917706421467349044,
        // field5=-91, field6=-20134, field7=-520068864, field8=-792456140590940108
        val data = byteArrayOf(
            0x7a,
            0x05, 0xa1.toByte(),
            0x50, 0xc1.toByte(), 0x00, 0x1b,
            0x60, 0x00, 0xa1.toByte(), 0x56, 0xb8.toByte(), 0x00, 0x00, 0x34,
            0xa5.toByte(),
            0xb1.toByte(), 0x5a,
            0xe1.toByte(), 0x00, 0x61, 0x00,
            0xf5.toByte(), 0x00, 0xa1.toByte(), 0x56, 0xb8.toByte(), 0x00, 0x00, 0x34,
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
        val field1 = result.bytesListTree[0]
        check(field1 is KaitaiSignedInteger) { "Expected field1 to be KaitaiSignedInteger, got ${field1::class.simpleName}" }
        checkElement(field1, "field1", Pair(0, 1), Pair(0, 0), "field1(122)s")

        // Validate field2
        val field2 = result.bytesListTree[1]
        check(field2 is KaitaiSignedInteger) { "Expected field2 to be KaitaiSignedInteger, got ${field2::class.simpleName}" }
        checkElement(field2, "field2", Pair(1, 3), Pair(0, 0), "field2(1441)s")

        // Validate field3
        val field3 = result.bytesListTree[2]
        check(field3 is KaitaiSignedInteger) { "Expected field3 to be KaitaiSignedInteger, got ${field3::class.simpleName}" }
        checkElement(field3, "field3", Pair(3, 7), Pair(0, 0), "field3(1354825755)s")

        // Validate field4
        val field4 = result.bytesListTree[3]
        check(field4 is KaitaiSignedInteger) { "Expected field4 to be KaitaiSignedInteger, got ${field4::class.simpleName}" }
        checkElement(field4, "field4", Pair(7, 15), Pair(0, 0), "field4(6917706421467349044)s")

        // Validate field5
        val field5 = result.bytesListTree[4]
        check(field5 is KaitaiSignedInteger) { "Expected field5 to be KaitaiSignedInteger, got ${field5::class.simpleName}" }
        checkElement(field5, "field5", Pair(15, 16), Pair(0, 0), "field5(-91)s")

        // Validate field6
        val field6 = result.bytesListTree[5]
        check(field6 is KaitaiSignedInteger) { "Expected field6 to be KaitaiSignedInteger, got ${field6::class.simpleName}" }
        checkElement(field6, "field6", Pair(16, 18), Pair(0, 0), "field6(-20134")

        // Validate field7
        val field7 = result.bytesListTree[6]
        check(field7 is KaitaiSignedInteger) { "Expected field7 to be KaitaiSignedInteger, got ${field7::class.simpleName}" }
        checkElement(field7, "field7", Pair(18, 22), Pair(0, 0), "field7(-520068864)s")

        // Validate field8
        val field8 = result.bytesListTree[7]
        check(field8 is KaitaiSignedInteger) { "Expected field8 to be KaitaiSignedInteger, got ${field8::class.simpleName}" }
        checkElement(field8, "field8", Pair(22, 30), Pair(0, 0), "field8(-792456140590940108)s")
    }
}
