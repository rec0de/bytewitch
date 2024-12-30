import bitmage.decodeAsUTF16BE
import bitmage.fromHex
import kotlin.test.Test

class StringTests {
    @Test
    fun overlongUTF8decode() {
        var bytes = "627974657769746368e079".fromHex()
        var string = bytes.decodeToString()

        // we expect a utf8 decode error in there
        check(string.any { it.code == 0xFFFD }){ "overlong utf8 should produce decoding error (0xe079)" }

        bytes = "627974657769746368f08c".fromHex()
        string = bytes.decodeToString()

        // we expect a utf8 decode error in there
        check(string.any { it.code == 0xFFFD }){ "overlong utf8 should produce decoding error (0xf08c)" }
    }

    @Test
    fun illegalUTF8bytes() {
        var bytes = "627974657769746368c0".fromHex()
        var string = bytes.decodeToString()
        check(string.any { it.code == 0xFFFD }){ "illegal byte should produce decoding error (0xc0)" }

        bytes = "627974657769746368c1".fromHex()
        string = bytes.decodeToString()
        check(string.any { it.code == 0xFFFD }){ "illegal byte should produce decoding error (0xc1)" }

        bytes = "62797465f769746368".fromHex()
        string = bytes.decodeToString()
        check(string.any { it.code == 0xFFFD }){ "illegal byte should produce decoding error (0xf7)" }
    }

    @Test
    fun invalidContinuationByte() {
        val bytes = "62799074657769746368".fromHex()
        val string = bytes.decodeToString()
        check(string.any { it.code == 0xFFFD }){ "illegal continuation byte should produce decoding error (0x90)" }
    }

    @Test
    fun incompleteCharacter() {
        var bytes = "e29c".fromHex()
        var string = bytes.decodeToString()
        check(string.any { it.code == 0xFFFD }){ "incomplete three-byte character should cause decoding error" }

        bytes = "e26279".fromHex()
        string = bytes.decodeToString()
        check(string.any { it.code == 0xFFFD }){ "premature non-continuation should produce decoding error" }
    }

    @Test
    fun tooLargeCharacter() {
        val bytes = "f4951111".fromHex()
        val string = bytes.decodeToString()
        check(string.any { it.code == 0xFFFD }){ "overly large character value should cause decoding error" }
    }

    @Test
    fun invalidUTF8detection() {
        val invalidStrings = listOf(
            "627974657769746368e079",
            "627974657769746368f08c",
            "627974657769746368c0",
            "627974657769746368c1",
            "62799074657769746368",
            "e29c",
            "e26279",
            "f4951111"
        )

        invalidStrings.forEach {
            check(looksLikeUtf8String(it.fromHex()) == 0.0) { "invalid utf8 should produce 0.0 confidence 0x$it" }
        }
    }

    @Test
    fun multilingualUnicodeDetection() {
        val strings = listOf(
            "00620077d83eddd9",
            "5b5782825deb5a463010006200773011",
            "0cac0cc80c9f0ccd00200cae0cbe0c9f0c970cbe0ca40cbf",
            "4f4d51437d4459735deb",
            "062c0627062f064806af063100200628062706cc062a",
            "bc14c774d2b80020b9c8b140",
            "006200e6007400690020006e006f0072006e",
            "0431043004390442043e04320430044f0020043204350434044c043c0430"
        )

        strings.forEach {
            check(looksLikeUtf16String(it.fromHex().decodeAsUTF16BE()) >= 0.7) { "valid utf16 should yield confidence > 0.75 0x$it" }
        }

        strings.forEach {
            check(looksLikeUtf8String(it.fromHex().decodeAsUTF16BE().encodeToByteArray()) >= 0.7) { "valid utf8 should yield confidence > 0.75 0x$it" }
        }
    }

    @Test
    fun mixedHanHangulDetection() {
        // excerpt from korean wikipedia featuring some han characters, should still get detected as valid text
        val strings = listOf(
            "d751b9c8bc9500289ed19b546cd5002c0020c601c5b4003a00200062006c00610063006b0020006d006100670069006300290020b610b2940020d751b9c8c22000289ed19b5488530029c7400020c804d1b5c801c73cb85c0020c0acc545d558ace00020c774ae30c801c7780020baa9c801c7440020c704d5740020cd08c790c5f0c801c7780020d798c774b0980020b9c8bc95c7440020c0acc6a9d558b2940020ac83c7440020b9d0d558ba70002c0020d2b9d78800200031003400350036b1440020c694d558b124c2a40020d558d2c0b9acbe0cac000020c124ba85d588b4efc7740020ad50d68cbc95c5d0c11c0020ae08c9c0d558b2940020c77cacf10020ac00c9c00020b9c8bc95c7440020b9d0d55cb2e4002e005b0031005d005b0032005d0020",
            "adf8b7ecb0980020b2f9c2dc0020acbdc131bd80ccad0020c124acc4c5d00020cc38c5ecd588b3580020c870c120cd1db3c5bd800020ac74cd95acfc0020ae30c2180020c0acc0ac0020ac8cc774c774ce5800287b3961764e000029b29400200031003900320036b144c5d00020300ac870c120acfc0020ac74cd95300b0028671d9bae30685efa7bc90029c5d0c11c0020201cd3c9ba74b3c4b2940020bd80c9c0c7580020acbdacc4c5d00020bd99c5ecc11c0020ad81d61500285f135f620029c73cb85c0020d558ace000200028202600290020c758c7a50028d604c7ac0020c11cc6b8d2b9bcc4c2dcccad0020d0dcd3c9d6400029c7400020c911c5590020b4a4cabdc5d00020b530b85c0020c124ce58d558c600b2e4201dace00020ae30c220d55c0020c810c7440020bcfc0020b54c0020c560cd08c5d00020c124acc4c790b2940020ac74bb3c0020baa8c591c74400202018672c2019c7740020c544b2cc002020185f132019c73cb85c0020c778c2ddd558c600b2e4002e"
        )

        strings.forEach {
            check(looksLikeUtf16String(it.fromHex().decodeAsUTF16BE()) >= 0.7) { "valid utf16 should yield confidence > 0.75 0x$it" }
        }

        strings.forEach {
            check(looksLikeUtf8String(it.fromHex().decodeAsUTF16BE().encodeToByteArray()) >= 0.7) { "valid utf8 should yield confidence > 0.75 0x$it" }
        }
    }

    @Test
    fun invalidUTF16detection() {
        val strings = listOf(
            "FDC08BBD09DFBEF4",
            "6F1BB37D6785CABC",
            "2EAC6BF13A9B17AF0000AC26",
            "A383D54CBA1DBA6700002A4E",
            "86B560F65D49123373B39E53000093DB",
            "41BF93153F6496C3D0DB73E8683E1574",
            "2689CD267E9E838627B8BCE747655A8D0FC84D7232DCDC8B"
        )

        strings.forEach {
            check(looksLikeUtf16String(it.fromHex().decodeAsUTF16BE()) < 0.5) { "random bytes should yield confidence < 0.5 0x$it" }
        }
    }

    @Test
    fun utf16decode() {
        var bytes = "006200790074006500770069007400630068".fromHex()
        var string = bytes.decodeAsUTF16BE()
        check(string == "bytewitch"){ "utf16 decoded to unexpected value $string (expected bytewitch)" }

        bytes = "5b5782825deb5a46".fromHex()
        string = bytes.decodeAsUTF16BE()
        check(string == "字节巫婆"){ "utf16 decoded to unexpected value $string (expected 字节巫婆)" }

        bytes = "d83eddd9".fromHex()
        string = bytes.decodeAsUTF16BE()
        check(string == "\uD83E\uDDD9"){ "utf16 decoded to unexpected value $string (expected \uD83E\uDDD9)" }
    }
}