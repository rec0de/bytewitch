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
                KTSeq(id = "field1", type = "u4"),
                KTSeq(id = "field2", type = "strz"),
                KTSeq(id = "field3", type = "s1")
            )
        )

        // Example data: field1=1, field2="Hello", field3=-127
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x00, 0x81.toByte())

        val decoder = Kaitai("example", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        // Validate the main result element
        checkElement(result, "example", Pair(0, 11), Pair(0, 0))
        check(result.endianness == ByteOrder.BIG) { "Expected endianness to be 'big', got '${result.endianness}'" }
        check(result.bytesListTree.size == 3) {
            "Expected bytesListTree to have 3 elements, got ${result.bytesListTree.size}"
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
}
