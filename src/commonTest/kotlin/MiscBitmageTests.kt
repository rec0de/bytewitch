import bitmage.ByteOrder
import bitmage.fromBytes
import bitmage.fromHex
import bitmage.hex
import bitmage.stripLeadingZeros
import kotlin.test.Test

class MiscBitmageTests {
    @Test
    fun stripZeros() {
        val a = "010203040506".fromHex()
        check(a.stripLeadingZeros().hex() == a.hex())

        val b = "000102030405".fromHex()
        check(b.stripLeadingZeros().hex() == "0102030405")

        val c = "0000000001".fromHex()
        check(c.stripLeadingZeros().hex() == "01")

        val d = "00000000".fromHex()
        check(d.stripLeadingZeros().isEmpty())
    }
}