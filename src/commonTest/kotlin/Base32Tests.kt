import bitmage.*
import kotlin.test.Test

class Base32Tests {
    @Test
    fun unsignedInts() {
        val a = decodeBase32(listOf(12, 9, 28, 23, 8, 25, 8)).hex()
        check(a == "62797465") { "check a got $a expected 62797465" }
    }
}