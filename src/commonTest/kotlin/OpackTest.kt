import bitmage.fromHex
import bitmage.hex
import decoders.ECCurves
import decoders.OPDict
import decoders.OPString
import decoders.OpackObject
import decoders.OpackParser
import kotlin.test.Test

class OpackTest {
    @Test
    fun directDecode1() {
        val sample = "ec436d65740943706d640843706d6c0243707373024370696301437773740a4370746d084370617432014002e04370697301436c73740b4370636c361f85eb51b81ed53f43706d78360000000000005940".fromHex()
        val res = OpackParser.decode(sample, 0)

        check(res is OPDict)

        val entries = res.values
        val keys = entries.keys.map { (it as OPString).value }

        check(keys == listOf("met", "pmd", "pml", "pss", "pic", "wst", "ptm", "pat", "pis", "lst", "pcl", "pmx"))
    }

    @Test
    fun directDecode2() {
        val sample = "e1435f706491280601010320ffea68deb2284f5d276514678eb127dff6a87d5e062ddb0ef070f2cc2c265470190101".fromHex()
        val res = OpackParser.decode(sample, 0)

        check(res is OPDict)

        val entries = res.values
        val keys = entries.keys.map { (it as OPString).value }

        check(keys == listOf("_pd"))
    }

    @Test
    fun detection() {
        val s1 = "ec436d65740943706d640843706d6c0243707373024370696301437773740a4370746d084370617432014002e04370697301436c73740b4370636c361f85eb51b81ed53f43706d78360000000000005940".fromHex()
        val s2 = "e1435f706491280601010320ffea68deb2284f5d276514678eb127dff6a87d5e062ddb0ef070f2cc2c265470190101".fromHex()

        val r1 = ByteWitch.analyze(s1, tryhard = false)
        val r2 = ByteWitch.analyze(s2, tryhard = true)

        check(r1.any { it.second is OPDict })
        check(r2.any { it.second is OPDict })
    }

    @Test
    fun pyATVExamples() {
        val samples = listOf(
            "EF4163416403",
            "0512345678123456781234567812345678",
            "6F666F6F00",
            "D443666F6F43626172A0A1",
            "DF416103",
            "E3416102416244746573744163A2"
        )

        val results = samples.map { ByteWitch.analyze(it.fromHex(), tryhard = true) }

        samples.forEach {
            val result = ByteWitch.analyze(it.fromHex(), tryhard = false)
            check(result.any{ it.second is OpackObject }) { "Payload failed to detect as opack: $it" }
        }
    }

}