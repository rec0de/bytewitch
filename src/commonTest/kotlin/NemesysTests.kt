import decoders.Nemesys
import bitmage.fromHex
import decoders.BWAnnotatedData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NemesysTests {
    @Test
    fun testDecodesAsValid() {
        val input = "123456".fromHex()
        val (isValid, error) = Nemesys.decodesAsValid(input)
        assertTrue(isValid)
        assertEquals(null, error)
    }

    @Test
    fun testConfidence() {
        val inputShort = "12".fromHex()
        val inputLong = "123456".fromHex()
        assertEquals(0.00, Nemesys.confidence(inputShort))
        assertEquals(0.76, Nemesys.confidence(inputLong))
    }

    @Test
    fun testDecode() {
        val message = "fe4781820001000000000000037777770369666303636f6d0000010001".fromHex()
        val result = Nemesys.decode(message, 0, true) // Adjust inlineDisplay if needed

        // Check if decoding returns a non-null result
        assertTrue(result != null, "Decode result should not be null")
    }
}