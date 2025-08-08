package kaitai

import kotlin.test.Test

class KaitaiParserTests {

    @Test
    fun testSimpleSequence() {
        val yaml = """
            meta:
              id: example
              title: Example Struct
              endian: le
            seq:
              - id: field1
                type: u4
              - id: field2
                type: str
        """.trimIndent()

        val struct = KaitaiParser.parseYaml(yaml)

        checkNotNull(struct) { "Failed to parse Kaitai YAML" }

        checkNotNull(struct.meta) { "Expected meta to be non-null" }
        check(struct.meta.id == "example") { "Expected id to be 'example', got '${struct.meta.id}'" }
        check(struct.meta.title == "Example Struct") { "Expected title to be 'Example Struct', got '${struct.meta.title}'" }
        check(struct.meta.endian == KTEndian.LE) { "Expected endian to be 'le', got '${struct.meta.endian}'" }

        check(struct.seq.size == 2) { "Expected 2 sequence items, got ${struct.seq.size}" }
        check(struct.seq[0].id == "field1") { "Expected first field id to be 'field1', got '${struct.seq[0].id}'" }
        check(struct.seq[0].type == "u4") { "Expected first field type to be 'u4', got '${struct.seq[0].type}'" }
        check(struct.seq[1].id == "field2") { "Expected second field id to be 'field2', got '${struct.seq[1].id}'" }
        check(struct.seq[1].type == "str") { "Expected second field type to be 'str', got '${struct.seq[1].type}'" }
    }
}
