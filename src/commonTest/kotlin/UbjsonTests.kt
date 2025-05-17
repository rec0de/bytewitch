import bitmage.fromHex
import bitmage.hex
import decoders.*
import kotlin.test.Test

class UbjsonTests {
    @Test
    fun directDecode() {
        val sample = "5b2443235503616263".fromHex()
        val res = UbjsonParser.decode(sample, 0)

        check(res is OPArray)

        val entries = res.values
        check(entries.size == 3)

        check(entries[0] is OPString)

        val one = entries[0] as OPString
        check(one.value == "a")
    }

    @Test
    fun detection() {
        val s1 = "5b2443235503616263".fromHex()
        //val s2 = "8201820203".fromHex()
        val r1 = ByteWitch.analyze(s1, tryhard = false)
        //val r2 = ByteWitch.analyze(s2, tryhard = false)
        check(r1.any { it.second is OPArray })
        //check(r2.any { it.second is OPArray })
    }

}