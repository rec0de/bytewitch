import bitmage.fromHex
import bitmage.hex
import decoders.*
import kotlin.test.Test

class MsgPackTest {
    @Test
    fun directDecode() {
        val sample = "8ca474727565c3a566616c7365c2a46e756c6cc0a1610ba1626fa163cd0457a164cd2b67a165ffa166d09ca167d1effea6737472696e67d925737068696e78206f6620626c61636b2071756172747a2c206a75646765206d7920766f7721a46c697374970102a568656c6c6fcb400921cac083126fd6ff67e3ed05d7ff0000000067e3ed05c70cff000000000000000067e3ed05".fromHex()
        val res = MsgPackParser.decode(sample, 0)

        check(res is OPDict)

        val entries = res.values
        val keys = entries.keys.map { (it as OPString).value }
        val values = entries.values.toList()

        check(keys == listOf("true", "false", "null", "a", "b", "c", "d", "e", "f", "g", "string", "list"))

        check(values[0] is OPTrue)
        check(values[10] is OPString)
        check(values[11] is OPArray)
        check((values[11] as OPArray).values.size == 7)
        check((values[11] as OPArray).values[4] is OPDate)
    }

    @Test
    fun detection() {
        val s1 = "8ca474727565c3a566616c7365c2a46e756c6cc0a1610ba1626fa163cd0457a164cd2b67a165ffa166d09ca167d1effea6737472696e67d925737068696e78206f6620626c61636b2071756172747a2c206a75646765206d7920766f7721a46c697374970102a568656c6c6fcb400921cac083126fd6ff67e3ed05d7ff0000000067e3ed05c70cff000000000000000067e3ed05".fromHex()
        val r1 = ByteWitch.analyze(s1, tryhard = false)
        check(r1.any { it.second is OPDict })
    }

}