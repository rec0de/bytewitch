import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.fromHex
import kotlin.test.Test

class IntSignTests {
    @Test
    fun unsignedInts() {
        val a = UInt.fromBytes("80".fromHex(), ByteOrder.BIG)
        check(a == 128u) { "check a got $a expected 128" }

        val b = UInt.fromBytes("ff".fromHex(), ByteOrder.BIG)
        check(b == 255u) { "check b got $b expected 255" }

        val c = UInt.fromBytes("8101".fromHex(), ByteOrder.BIG)
        check(c == 33025u) { "check c got $c expected 33025" }

        val d = UInt.fromBytes("bebc2000".fromHex(), ByteOrder.BIG)
        check(d == 3200000000u) { "check d got $d expected 3200000000" }
    }

    @Test
    fun signedInts() {
        val a1 = Int.fromBytes("80".fromHex(), ByteOrder.BIG)
        val a2 = Int.fromBytes("80".fromHex(), ByteOrder.BIG, explicitlySigned = true)
        check(a1 == 128) { "check a1 got $a1 expected 128" }
        check(a2 == -128) { "check a2 got $a2 expected -128" }

        val b1 = Int.fromBytes("ff".fromHex(), ByteOrder.BIG)
        val b2 = Int.fromBytes("ff".fromHex(), ByteOrder.BIG, explicitlySigned = true)
        check(b1 == 255) { "check b1 got $b1 expected 255" }
        check(b2 == -1) { "check b2 got $b2 expected -1" }

        val c1 = Int.fromBytes("8101".fromHex(), ByteOrder.BIG)
        val c2 = Int.fromBytes("8101".fromHex(), ByteOrder.BIG, explicitlySigned = true)
        check(c1 == 33025) { "check c1 got $c1 expected 33025" }
        check(c2 == -32511) { "check c2 got $c2 expected -32511" }

        val d1 = Int.fromBytes("bebc2000".fromHex(), ByteOrder.BIG)
        val d2 = Int.fromBytes("bebc2000".fromHex(), ByteOrder.BIG, explicitlySigned = true)
        check(d1 == -1094967296)
        check(d2 == d1)
    }
}