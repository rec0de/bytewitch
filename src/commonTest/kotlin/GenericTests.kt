import bitmage.fromHex
import decoders.Randomness
import decoders.web.WebSocket
import kotlin.test.Test

class GenericTests {

    @Test
    fun emptyPayloadNeverDetected() {
        val r1 = ByteWitch.analyze(byteArrayOf(), tryhard = false)
        val r2 = ByteWitch.analyze(byteArrayOf(), tryhard = false)
        val r3 = ByteWitch.quickDecode(byteArrayOf(), 0)

        println(r1)
        check(r1.isEmpty())
        println(r2)
        check(r2.isEmpty())
        println(r3)
        check(r3 == null)
    }

    @Test
    fun zeroPayloadNeverDetected() {
        (1..32).forEach { len ->
            val payload = ByteArray(len)
            val r = ByteWitch.analyze(payload, tryhard = false)
            println("Zero Payload length $len")
            println(r)
            check(r.isEmpty() || (r.size == 1 && r.first().first == Randomness.name))
        }
    }
}