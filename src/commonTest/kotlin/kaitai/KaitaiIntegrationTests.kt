package kaitai

import bitmage.byteArrayOfInts
import decoders.KaitaiResult
import decoders.KaitaiSignedInteger
import decoders.KaitaiString
import decoders.KaitaiUnsignedInteger
import kaitai.KaitaiTestUtils.checkElement
import kotlin.test.Test

class KaitaiIntegrationTests {

    @Test
    fun testImportsBasic() {
        val dateYaml = """
            meta:
              id: date
            seq:
              - id: year
                type: u2le
              - id: month
                type: u2le
              - id: day
                type: u2le
        """.trimIndent()

        val subTypeYaml = """
            meta:
              id: with_subtype
              imports:
                - inside_subtype
            seq:
              - id: field1
                type: s2
              - id: own
                type: with_subtype::sub
            types:
              sub:
                seq:
                  - id: a
                    type: s1
                  - id: b
                    type: s1
        """.trimIndent()

        val filelistYaml = """
            meta:
              id: filelist
              # this will import "date.ksy" and "/sub/with_subtype.ksy"
              imports:
                - date
                - /sub/with_subtype
            seq:
              - id: entries
                type: entry
              - id: list_sub
                type: with_subtype
              - id: list_sub_sub
                type: with_subtype::sub
            types:
              entry:
                seq:
                  - id: filename
                    type: strz
                    encoding: ASCII
                  # just use "date" type from date.ksy as if it was declared in
                  # current file
                  - id: timestamp
                    type: date
                  # you can access its members too!
                  - id: historical_data
                    size: 4
                    if: timestamp.year < 1970
        """.trimIndent()

        val data = byteArrayOfInts(
            0x66, 0x75, 0x6E, 0x6E, 0x79, 0x2E, 0x74, 0x78, 0x74, 0x00, // filename
            0xda, 0x07, 0x05, 0x00, 0x18, 0x00, // timestamp
            0x01, 0x02, 0x11, 0x12, // list_sub
            0x03, 0x04, // list_sub_sub
        )

        ByteWitch.registerKaitaiDecoder("date", dateYaml, "/date", ByteWitch.KaitaiDecoderType.BUILTIN)
        ByteWitch.registerKaitaiDecoder(
            "with_subtype",
            subTypeYaml,
            "/sub/with_subtype",
            ByteWitch.KaitaiDecoderType.BUILTIN
        )
        ByteWitch.registerKaitaiDecoder("filelist", filelistYaml, "/filelist", ByteWitch.KaitaiDecoderType.BUILTIN)

        val decoder = ByteWitch.builtinKaitaiDecoderListManager.getDecoder("filelist")
        checkNotNull(decoder) { "Kaitai decoder not found" }
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }
        checkElement(result, "filelist", KaitaiResult::class, Pair(0, 22), Pair(0, 0))

        // Check fields
        val fileListEntries = result.bytesListTree["entries"]
        check(fileListEntries is KaitaiResult) { "Expected KaitaiResult, got ${fileListEntries::class.simpleName}" }
        checkElement(fileListEntries, "entries", KaitaiResult::class, Pair(0, 16), Pair(0, 0))

        val entryFilename = fileListEntries.bytesListTree["filename"]
        checkElement(entryFilename, "filename", KaitaiString::class, Pair(0, 10), Pair(0, 0), htmlInnerContent = "funny.txt")

        val entryTimestamp = fileListEntries.bytesListTree["timestamp"]
        check(entryTimestamp is KaitaiResult) { "Expected KaitaiResult, got ${fileListEntries::class.simpleName}" }
        checkElement(entryTimestamp, "timestamp", KaitaiResult::class, Pair(10, 16), Pair(0, 0))

        val dateYear = entryTimestamp.bytesListTree["year"]
        checkElement(dateYear, "year", KaitaiUnsignedInteger::class, Pair(10, 12), Pair(0, 0), htmlInnerContent = "year(2010)u")
        val dateMonth = entryTimestamp.bytesListTree["month"]
        checkElement(dateMonth, "month", KaitaiUnsignedInteger::class, Pair(12, 14), Pair(0, 0), htmlInnerContent = "month(5)u")
        val dateDay = entryTimestamp.bytesListTree["day"]
        checkElement(dateDay, "day", KaitaiUnsignedInteger::class, Pair(14, 16), Pair(0, 0), htmlInnerContent = "day(24)u")

        val listSub = result.bytesListTree["list_sub"]
        check(listSub is KaitaiResult) { "Expected KaitaiResult, got ${listSub::class.simpleName}" }
        checkElement(listSub, "list_sub", KaitaiResult::class, Pair(16, 20), Pair(0, 0))

        val field1 = listSub.bytesListTree["field1"]
        checkElement(field1, "field1", KaitaiSignedInteger::class, Pair(16, 18), Pair(0, 0), htmlInnerContent = "field1(258)s")

        val own = listSub.bytesListTree["own"]
        check(own is KaitaiResult) { "Expected KaitaiResult, got $own" }
        checkElement(own, "own", KaitaiResult::class, Pair(18, 20))

        val ownA  = own.bytesListTree["a"]
        checkElement(ownA, "a", KaitaiSignedInteger::class, Pair(18, 19), Pair(0, 0), htmlInnerContent = "a(17)s")

        val ownB  = own.bytesListTree["b"]
        checkElement(ownB, "b", KaitaiSignedInteger::class, Pair(19, 20), Pair(0, 0), htmlInnerContent = "b(18)s")

        val listSubSub = result.bytesListTree["list_sub_sub"]
        check(listSubSub is KaitaiResult) { "Expected KaitaiResult, got $own" }
        checkElement(listSubSub, "list_sub_sub", KaitaiResult::class, Pair(20, 22))

        val subA  = listSubSub.bytesListTree["a"]
        checkElement(subA, "a", KaitaiSignedInteger::class, Pair(20, 21), Pair(0, 0), htmlInnerContent = "a(3)s")

        val subB  = listSubSub.bytesListTree["b"]
        checkElement(subB, "b", KaitaiSignedInteger::class, Pair(21, 22), Pair(0, 0), htmlInnerContent = "b(4)s")
    }

    @Test
    fun testImportsWithReferences() {
        val bottomYaml = """
            meta:
              id: bottom
            seq:
              - id: a
                type: u2
            instances:
              i:
                value: _root.a
        """.trimIndent()

        val topYaml = """
            meta:
              id: top
              imports:
                - bottom
            seq:
              - id: a
                type: u2
              - id: b
                type: bottom
                size: 2
            instances:
              j:
                value: _root.a
              k:
                value: b.a
        """.trimIndent()

        val mainYaml = """
            meta:
              id: main
              imports:
                - top
            seq:
              - id: a
                type: u2
              - id: b
                type: top
                size: 4
            instances:
              l:
                value: b.b.a
        """.trimIndent()

        val data = byteArrayOfInts(
            0x00, 0x01, // main.a
            0x00, 0x02, // top.a
            0x00, 0x03, // bottom.a
        )

        ByteWitch.registerKaitaiDecoder("bottom", bottomYaml, "/bottom", ByteWitch.KaitaiDecoderType.BUILTIN)
        ByteWitch.registerKaitaiDecoder("top", topYaml, "/top", ByteWitch.KaitaiDecoderType.BUILTIN)
        ByteWitch.registerKaitaiDecoder("main", mainYaml, "/main", ByteWitch.KaitaiDecoderType.BUILTIN)

        val decoder = ByteWitch.builtinKaitaiDecoderListManager.getDecoder("main")
        checkNotNull(decoder) { "Kaitai decoder not found" }
        val result = decoder.decode(data, 0)

        check(result is KaitaiResult) { "Expected KaitaiResult, got ${result::class.simpleName}" }
        checkElement(result, "main", KaitaiResult::class, Pair(0, 6), Pair(0, 0))

        // CHeck fields
        val mainA = result.bytesListTree["a"]
        checkElement(mainA, "a", KaitaiUnsignedInteger::class, Pair(0, 2), Pair(0, 0), htmlInnerContent = "a(1)u")

        val mainB = result.bytesListTree["b"]
        check(mainB is KaitaiResult) { "Expected KaitaiResult, got ${mainB::class.simpleName}" }
        checkElement(mainB, "b", KaitaiResult::class, Pair(2, 6), Pair(0, 0))

        val mainL = result.bytesListTree["l"]
        checkElement(mainL, "l", KaitaiSignedInteger::class, htmlInnerContent = "l(3)s")

        val topA = mainB.bytesListTree["a"]
        checkElement(topA, "a", KaitaiUnsignedInteger::class, Pair(2, 4), Pair(0, 0), htmlInnerContent = "a(2)u")

        val topB = mainB.bytesListTree["b"]
        check(topB is KaitaiResult) { "Expected KaitaiResult, got ${mainB::class.simpleName}" }
        checkElement(topB, "b", KaitaiResult::class, Pair(4, 6), Pair(0, 0))

        val topJ = mainB.bytesListTree["j"]
        checkElement(topJ, "j", KaitaiSignedInteger::class, htmlInnerContent = "j(2)s")

        val topK = mainB.bytesListTree["k"]
        checkElement(topK, "k", KaitaiSignedInteger::class, htmlInnerContent = "k(3)s")

        val bottomA = topB.bytesListTree["a"]
        checkElement(bottomA, "a", KaitaiUnsignedInteger::class, Pair(4, 6), Pair(0, 0), htmlInnerContent = "a(3)u")

        val bottomI = topB.bytesListTree["i"]
        checkElement(bottomI, "i", KaitaiSignedInteger::class, htmlInnerContent = "i(3)s")
    }
}