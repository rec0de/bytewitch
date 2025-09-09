package kaitai

import bitmage.booleanArrayOfInts
import bitmage.byteArrayOfInts
import decoders.Kaitai
import decoders.KaitaiBytes
import decoders.KaitaiElement
import decoders.KaitaiEnum
import decoders.KaitaiList
import decoders.KaitaiResult
import decoders.KaitaiSignedInteger
import decoders.KaitaiUnsignedInteger
import kaitai.KaitaiTestUtils.checkElement
import kotlin.test.Test

class KaitaiInstancesTests {
    @Test
    fun valueInstancesTest() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "valueInstances"
            ),
            seq = listOf(
                KTSeq(id = "integer", type = KTType.Primitive("s4")),
                KTSeq(id = "float", type = KTType.Primitive("f4")),
                KTSeq(id = "string", size = StringOrInt.IntValue(1), type = KTType.Primitive("str")),
                KTSeq(id = "boolean", type = KTType.Primitive("b1")),
                KTSeq(id = "bytes", size = StringOrInt.IntValue(4)),
                KTSeq(id = "list", type = KTType.Primitive("u1"), repeat = KTRepeat.EXPR, repeatExpr = StringOrInt.StringValue("3")),
                KTSeq(id = "result", type = KTType.Primitive("sub")),
                KTSeq(id = "enum", type = KTType.Primitive("u1"), enum = "ip_protocols"),
            ),
            types = mapOf(
                Pair(
                    "sub",
                    KTStruct(
                        seq = listOf(
                            KTSeq(id = "subInteger", type = KTType.Primitive("s4")),
                            KTSeq(id = "subFloat", type = KTType.Primitive("f4")),
                        )
                    )
                )
            ),
            enums = mapOf(
                Pair(
                    "ip_protocols",
                    KTEnum(
                        mapOf(
                            Pair(1, KTEnumValue(id = StringOrBoolean.StringValue("icmp"))),
                            Pair(6, KTEnumValue(id = StringOrBoolean.StringValue("tcp"))),
                            Pair(17, KTEnumValue(id = StringOrBoolean.StringValue("udp"))),
                        )
                    )
                ),
            ),
            instances = mapOf(
                Pair(
                    "instanceInteger",
                    KTSeq(value = "integer")
                ),
                Pair(
                    "instanceFloat",
                    KTSeq(value = "float")
                ),
                Pair(
                    "instanceString",
                    KTSeq(value = "string")
                ),
                Pair(
                    "instanceBoolean",
                    KTSeq(value = "boolean")
                ),
                Pair(
                    "instanceBytes",
                    KTSeq(value = "bytes")
                ),
                Pair(
                    "instanceBytes2",
                    KTSeq(value = "[0x00, 0x11, 0x22, 0x33]")
                ),
                Pair(
                    "instanceList",
                    KTSeq(value = "list")
                ),
                Pair(
                    "instanceResult",
                    KTSeq(value = "result")
                ),
                Pair(
                    "instanceEnum",
                    KTSeq(value = "enum")
                ),
            )
        )

        val data = byteArrayOfInts(
            0x00, 0x00, 0x00, 0xff, // integer = 255
            0x40, 0x40, 0x00, 0x00, // float = 3.0
            0x31, // string = 1
            0x80, // boolean = true
            0x11, 0x22, 0x33, 0x44, // bytes = 0x11, 0x22, 0x33, 0x44,
            0xaa, // list[0] = 170
            0xbb, // list[1] = 187
            0xcc, // list[2] = 204
            0x00, 0x00, 0x00, 0xff, // result.integer = 255
            0x40, 0x40, 0x00, 0x00, // result.float = 3.0
            0x11, // enum = udp
        )

        val decoder = Kaitai("valueInstances", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        var instance: KaitaiElement = result.bytesListTree["instanceInteger"]
        check((instance.value as Long) == 255L) {"Expected 255, got ${(instance.value as Long)}"}

        instance = result.bytesListTree["instanceFloat"]
        check((instance.value as Double) == 3.0) {"Expected 3.0, got ${(instance.value as Double)}"}

        instance = result.bytesListTree["instanceString"]
        checkElement(instance, "instanceString", value = "1")

        instance = result.bytesListTree["instanceBoolean"]
        check((instance.value as Boolean)) {"Expected true, got ${(instance.value as Boolean)}"}

        instance = result.bytesListTree["instanceBytes"]
        check((instance.value as List<Long>)[0] == 0x11L) { "Expected 17, got ${(instance.value as List<Long>)[0]}"}
        check((instance.value as List<Long>)[1] == 0x22L)
        check((instance.value as List<Long>)[2] == 0x33L)
        check((instance.value as List<Long>)[3] == 0x44L)

        instance = result.bytesListTree["instanceBytes2"]
        check(instance is KaitaiList) { "Expected KaitaiList, got ${instance::class.simpleName}" }
        var instanceElement = instance.bytesListTree[0]
        check(instanceElement is KaitaiSignedInteger) {"Expected instanceElement to be KaitaiSignedInteger, got ${instanceElement::class.simpleName}"}
        check((instanceElement.value as Long) == 0x00L) { "Expected 0, got ${(instanceElement.value as Long)}"}
        instanceElement = instance.bytesListTree[1]
        check(instanceElement is KaitaiSignedInteger) {"Expected instanceElement to be KaitaiSignedInteger, got ${instanceElement::class.simpleName}"}
        check((instanceElement.value as Long) == 0x11L)
        instanceElement = instance.bytesListTree[2]
        check(instanceElement is KaitaiSignedInteger) {"Expected instanceElement to be KaitaiSignedInteger, got ${instanceElement::class.simpleName}"}
        check((instanceElement.value as Long) == 0x22L)
        instanceElement = instance.bytesListTree[3]
        check(instanceElement is KaitaiSignedInteger) {"Expected instanceElement to be KaitaiSignedInteger, got ${instanceElement::class.simpleName}"}
        check((instanceElement.value as Long) == 0x33L)

        instance = result.bytesListTree["instanceList"]
        check(instance is KaitaiList) { "Expected KaitaiList, got ${instance::class.simpleName}" }
        var instanceSub = instance.bytesListTree[0]
        check(instanceSub is KaitaiSignedInteger) {"Expected instanceElement to be KaitaiSignedInteger, got ${instanceSub::class.simpleName}"}
        check((instanceSub.value as Long) == 170L) {"Expected 170 got ${(instanceSub.value as Long)}"}
        instanceSub = instance.bytesListTree[1]
        check(instanceSub is KaitaiSignedInteger) {"Expected instanceElement to be KaitaiSignedInteger, got ${instanceSub::class.simpleName}"}
        check((instanceSub.value as Long) == 187L) {"Expected 187 got ${(instanceSub.value as Long)}"}
        instanceSub = instance.bytesListTree[2]
        check(instanceSub is KaitaiSignedInteger) {"Expected instanceElement to be KaitaiSignedInteger, got ${instanceSub::class.simpleName}"}
        check((instanceSub.value as Long) == 204L) {"Expected 204 got ${(instanceSub.value as Long)}"}

        instance = result.bytesListTree["instanceResult"]
        check(instance is KaitaiResult) { "Expected KaitaiResult, got ${instance::class.simpleName}" }
        instanceSub = instance.bytesListTree["subInteger"]
        check(instanceSub is KaitaiSignedInteger) { "Expected KaitaiSignedInteger, got ${instance::class.simpleName}" }
        check((instanceSub.value as BooleanArray).contentEquals(booleanArrayOfInts(0x00, 0x00, 0x00, 0xFF))) {"Expected 255 got ${(instanceSub.value as Long)}"}
        instanceSub = instance.bytesListTree["subFloat"]
        check((instanceSub.value as BooleanArray).contentEquals(booleanArrayOfInts(0x40, 0x40, 0x00, 0x00))) {"Expected 3.0, got ${(instanceSub.value as Double)}"}

        instance = result.bytesListTree["enum"]
        check(instance is KaitaiEnum)
        check(instance.enum.second == "udp") {"Expected \"udp\", got ${instance.enum.second}"}
    }

    @Test
    fun quasiForwardReferenceTest() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "quasiForwardReference",
            ),
            seq = listOf(
                KTSeq(id = "a", type = KTType.Primitive("u1")),
                KTSeq(id = "b", size = StringOrInt.StringValue("i")),
            ),
            instances = mapOf(
                Pair("i", KTSeq(type = KTType.Primitive("u1"))),  // referenced by b
            )
        )

        val data = byteArrayOfInts(
            0x02,  // both a and i
            0x03, 0x04, 0x05, 0x05  // b
        )

        val decoder = Kaitai("quasiForwardReference", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        val a = result.bytesListTree["a"]
        checkElement(a, "a", KaitaiUnsignedInteger::class, Pair(0, 1), Pair(0, 0), booleanArrayOfInts(0x02))
        val b = result.bytesListTree["b"]
        checkElement(b, "b", KaitaiBytes::class, Pair(1, 3), Pair(0, 0), booleanArrayOfInts(0x03, 0x04))

        val i = result.bytesListTree["i"]
        checkElement(i, "i", KaitaiUnsignedInteger::class, value = booleanArrayOfInts(0x02))
    }

    @Test
    fun instanceInParentTest() {
        val struct = KTStruct(
            meta = KTMeta(
                id = "instanceInParent",
            ),
            seq = listOf(
                KTSeq(id = "a", type = KTType.Primitive("u1")),
                KTSeq(id = "b", type = KTType.Primitive("sub")),
                KTSeq(id = "c", type = KTType.Primitive("sub"), size = StringOrInt.StringValue("2")),
            ),
            types = mapOf(
                Pair(
                    "sub",
                    KTStruct(
                        seq = listOf(
                            KTSeq(
                                id = "x", size = StringOrInt.StringValue("_parent.i"),
                            ),
                        ),
                    ),
                ),
            ),
            instances = mapOf(
                Pair("i", KTSeq(type = KTType.Primitive("u1"))),  // referenced by b.x
            )
        )

        val data = byteArrayOfInts(
            0x02,  // both a and i
            0x03, 0x04,  // b
            0x05, 0x06,  // c, with substream
        )

        val decoder = Kaitai("instanceInParent", struct)
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }

        val a = result.bytesListTree["a"]
        checkElement(a, "a", KaitaiUnsignedInteger::class, Pair(0, 1), Pair(0, 0), booleanArrayOfInts(0x02))

        val b = result.bytesListTree["b"]
        check(b is KaitaiResult) { "Expected KaitaiResult, got ${b::class.simpleName}" }
        checkElement(b, "b", KaitaiResult::class, Pair(1, 3), Pair(0, 0), booleanArrayOfInts(0x03, 0x04))
        val bx = b.bytesListTree["x"]
        checkElement(bx, "x", KaitaiBytes::class, Pair(1, 3), Pair(0, 0), booleanArrayOfInts(0x03, 0x04))

        val c = result.bytesListTree["c"]
        check(c is KaitaiResult) { "Expected KaitaiResult, got ${c::class.simpleName}" }
        checkElement(c, "c", KaitaiResult::class, Pair(3, 5), Pair(0, 0), booleanArrayOfInts(0x05, 0x06))
        val cx = c.bytesListTree["x"]
        checkElement(cx, "x", KaitaiBytes::class, Pair(3, 5), Pair(0, 0), booleanArrayOfInts(0x05, 0x06))

        val i = result.bytesListTree["i"]
        checkElement(i, "i", KaitaiUnsignedInteger::class, value = booleanArrayOfInts(0x02))
    }
}