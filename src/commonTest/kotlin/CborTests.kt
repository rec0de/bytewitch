import bitmage.fromHex
import bitmage.hex
import decoders.*
import kotlin.test.Test

class CborTests {
    @Test
    fun directDecode() {
        val sample = "8201820203".fromHex()
        val res = CborParser.decode(sample, 0)

        check(res is OPArray)

        val entries = res.values
        check(entries.size == 2)

        check(entries[0] is OPInt)
        check(entries[1] is OPArray)

        val one = entries[0] as OPInt
        check(one.value.toInt() == 1)

        val nested = entries[1] as OPArray
        check(nested.values.size == 2)
        check((nested.values[0] as OPInt).value.toInt() == 2)
        check((nested.values[1] as OPInt).value.toInt() == 3)
    }

    @Test
    fun directDecodeIndefinite() {
        val sample = "9f01820203ff".fromHex()
        val res = CborParser.decode(sample, 0)

        check(res is OPArray)

        val entries = res.values
        check(entries.size == 2)

        check(entries[0] is OPInt)
        check(entries[1] is OPArray)

        val one = entries[0] as OPInt
        check(one.value.toInt() == 1)

        val nested = entries[1] as OPArray
        check(nested.values.size == 2)
        check((nested.values[0] as OPInt).value.toInt() == 2)
        check((nested.values[1] as OPInt).value.toInt() == 3)
    }

    @Test
    fun referenceExamples() {
        val samples = listOf("1903e8", "1a000f4240", "1b000000e8d4a51000", "1bffffffffffffffff", "c249010000000000000000", "3bffffffffffffffff", "c349010000000000000000", "3903e7", "f90000", "f98000", "f93c00", "fb3ff199999999999a", "f93e00", "f97bff", "fa47c35000", "fa7f7fffff", "fb7e37e43c8800759c", "f90001", "f90400", "f9c400", "fbc010666666666666", "f97c00", "f97e00", "f9fc00", "fa7f800000", "fa7fc00000", "faff800000", "fb7ff0000000000000", "fb7ff8000000000000", "fbfff0000000000000", "c074323031332d30332d32315432303a30343a30305a", "c11a514b67b0", "c1fb41d452d9ec200000", "d74401020304", "d818456449455446", "d82076687474703a2f2f7777772e6578616d706c652e636f6d", "4401020304", "6449455446", "62225c", "62c3bc", "63e6b0b4", "64f0908591", "83010203", "8301820203820405", "98190102030405060708090a0b0c0d0e0f101112131415161718181819", "a201020304", "a26161016162820203", "826161a161626163", "a56161614161626142616361436164614461656145", "5f42010243030405ff", "7f657374726561646d696e67ff", "9f018202039f0405ffff", "9f01820203820405ff", "83018202039f0405ff", "83019f0203ff820405", "9f0102030405060708090a0b0c0d0e0f101112131415161718181819ff", "bf61610161629f0203ffff", "826161bf61626163ff", "bf6346756ef563416d7421ff")

        samples.forEach {
            val bytes = it.fromHex()
            Logger.log("Trying to decode: $it")
            Logger.log(CborParser.decode(bytes, 0))
        }
    }

    @Test
    fun detection() {
        val s1 = "9f01820203ff".fromHex()
        val s2 = "8201820203".fromHex()
        val r1 = ByteWitch.analyze(s1, tryhard = false)
        val r2 = ByteWitch.analyze(s2, tryhard = false)
        check(r1.any { it.second is OPArray })
        check(r2.any { it.second is OPArray })
    }

}