import bitmage.fromHex
import bitmage.hex
import decoders.*
import kotlin.test.Test

class BsonTests {
    @Test
    fun directDecode() {
        val sample = "22000000103000010000000431001300000010300002000000103100030000000000".fromHex()
        val res = BsonParser.decode(sample, 0)

        check(res is OPDict)

        val entries = res.values
        check(entries.size == 2)
    }
}