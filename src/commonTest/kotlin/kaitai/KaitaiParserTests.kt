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
        check(struct.seq[0].type is KTType.Primitive) { "Expected first field type to be primitive" }
        check((struct.seq[0].type as KTType.Primitive).type == "u4") { "Expected first field type to be 'u4', got '${struct.seq[0].type}'" }

        check(struct.seq[1].id == "field2") { "Expected second field id to be 'field2', got '${struct.seq[1].id}'" }
        check(struct.seq[1].type is KTType.Primitive) { "Expected second field type to be primitive" }
        check((struct.seq[1].type as KTType.Primitive).type == "str") { "Expected second field type to be 'str', got '${struct.seq[1].type}'" }
    }

    @Test
    fun testContents() {
        val yaml = """
            meta:
              id: contents
            seq:
              - id: magic1
                contents: JFIF
              - id: magic2
                contents:
                  - 0xca
                  - 0xfe
                  - 0xba
                  - 0xbe
              - id: magic3
                contents: [CAFE, 0, BABE]
              - id: magic4
                contents: [foo, 0, A, 0xa, 42]
              - id: magic5
                contents: [1, 0x55, '▒,3', 3]
        """.trimIndent()

        val struct = KaitaiParser.parseYaml(yaml)

        checkNotNull(struct) { "Failed to parse Kaitai YAML" }

        check(struct.seq.size == 5) { "Expected 5 sequence items, got ${struct.seq.size}" }

        // Check field magic1
        check(struct.seq[0].id == "magic1") { "Expected first field id to be 'magic1', got '${struct.seq[0].id}'" }
        check(struct.seq[0].contents == listOf("JFIF")) {
            "Expected field 'magic1' contents to be [JFIF], got '${struct.seq[0].contents}'"
        }

        // Check field magic2
        check(struct.seq[1].id == "magic2") { "Expected second field id to be 'magic2', got '${struct.seq[1].id}'" }
        check(struct.seq[1].contents == listOf("202", "254", "186", "190")) {
            "Expected field 'magic2' contents to be [202, 254, 186, 190], got '${struct.seq[1].contents}'"
        }

        // Check field magic3
        check(struct.seq[2].id == "magic3") { "Expected third field id to be 'magic3', got '${struct.seq[2].id}'" }
        check(struct.seq[2].contents == listOf("CAFE", "0", "BABE")) {
            "Expected field 'magic3' contents to be [CAFE, 0, BABE], got '${struct.seq[2].contents}'"
        }

        // Check field magic4
        check(struct.seq[3].id == "magic4") { "Expected fourth field id to be 'magic4', got '${struct.seq[3].id}'" }
        check(struct.seq[3].contents == listOf("foo", "0", "A", "10", "42")) {
            "Expected field 'magic4' contents to be [foo, 0, A, 10, 42], got '${struct.seq[3].contents}'"
        }

        // Check field magic5
        check(struct.seq[4].id == "magic5") { "Expected fifth field id to be 'magic5', got '${struct.seq[4].id}'" }
        check(struct.seq[4].contents == listOf("1", "85", "▒,3", "3")) {
            "Expected field 'magic5' contents to be [1, 85, '▒,3', 3], got '${struct.seq[4].contents}'"
        }
    }

    @Test
    fun testType() {
        val yaml = """
            meta:
              id: example
            seq:
              - id: field1
                type: u4
              - id: field2
                type:
                  switch-on: is_core_header
                  cases:
                    true: u2
                    false: u4
        """.trimIndent()

        val struct = KaitaiParser.parseYaml(yaml)

        checkNotNull(struct) { "Failed to parse Kaitai YAML" }

        check(struct.seq.size == 2) { "Expected 2 sequence items, got ${struct.seq.size}" }

        // Check field1 type
        check(struct.seq[0].id == "field1") { "Expected first field id to be 'field1', got '${struct.seq[0].id}'" }
        check(struct.seq[0].type is KTType.Primitive) { "Expected first field type to be primitive" }
        check((struct.seq[0].type as KTType.Primitive).type == "u4") {
            "Expected first field type to be 'u4', got '${(struct.seq[0].type as KTType.Primitive).type}'"
        }

        // Check field2 type
        check(struct.seq[1].id == "field2") { "Expected second field id to be 'field2', got '${struct.seq[1].id}'" }
        check(struct.seq[1].type is KTType.Switch) { "Expected second field type to be switch" }
        val switchType = struct.seq[1].type as KTType.Switch
        check(switchType.switchOn == "is_core_header") {
            "Expected switch-on to be 'is_core_header', got '${switchType.switchOn}'"
        }
        check(switchType.cases.size == 2) { "Expected 2 cases, got ${switchType.cases.size}" }
        check(switchType.cases["true"] == "u2") { "Expected case 'true' to be 'u2', got '${switchType.cases["true"]}'" }
        check(switchType.cases["false"] == "u4") { "Expected case 'false' to be 'u4', got '${switchType.cases["false"]}'" }
    }
}
