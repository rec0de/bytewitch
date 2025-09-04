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
        check(struct.meta.endian is KTEndian.Primitive) { "Expected endian to be primitive" }
        check(struct.meta.endian.value == KTEndianEnum.LE) { "Expected endian to be 'le', got '${struct.meta.endian}'" }

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
                contents: [foo, "0", A, "0x0a", "42"]
              - id: magic5
                contents: [1, 0x55, '▒,3', 3]
        """.trimIndent()

        val struct = KaitaiParser.parseYaml(yaml)

        checkNotNull(struct) { "Failed to parse Kaitai YAML" }

        check(struct.seq.size == 5) { "Expected 5 sequence items, got ${struct.seq.size}" }

        // Check field magic1
        check(struct.seq[0].id == "magic1") { "Expected first field id to be 'magic1', got '${struct.seq[0].id}'" }
        check(struct.seq[0].contents == listOf(StringOrInt.StringValue("JFIF"))) {
            "Expected field 'magic1' contents to be [\"JFIF\"], got ${struct.seq[0].contents}"
        }

        // Check field magic2
        check(struct.seq[1].id == "magic2") { "Expected second field id to be 'magic2', got '${struct.seq[1].id}'" }
        check(struct.seq[1].contents == listOf(StringOrInt.IntValue(202), StringOrInt.IntValue(254), StringOrInt.IntValue(186), StringOrInt.IntValue(190))) {
            "Expected field 'magic2' contents to be [202, 254, 186, 190], got ${struct.seq[1].contents}"
        }

        // Check field magic3
        check(struct.seq[2].id == "magic3") { "Expected third field id to be 'magic3', got '${struct.seq[2].id}'" }
        check(struct.seq[2].contents == listOf(StringOrInt.StringValue("CAFE"), StringOrInt.IntValue(0), StringOrInt.StringValue("BABE"))) {
            "Expected field 'magic3' contents to be [\"CAFE\", 0, \"BAB0\"], got ${struct.seq[2].contents}"
        }

        // Check field magic4
        check(struct.seq[3].id == "magic4") { "Expected fourth field id to be 'magic4', got '${struct.seq[3].id}'" }
        check(struct.seq[3].contents == listOf(StringOrInt.StringValue("foo"), StringOrInt.StringValue("0"), StringOrInt.StringValue("A"),StringOrInt.StringValue("0x0a"), StringOrInt.StringValue("42"))) {
            "Expected field 'magic4' contents to be [\"foo\", \"0\", \"A\", \"0xa\", \"42\"], got ${struct.seq[3].contents}"
        }

        // Check field magic5
        check(struct.seq[4].id == "magic5") { "Expected fifth field id to be 'magic5', got '${struct.seq[4].id}'" }
        check(struct.seq[4].contents == listOf(StringOrInt.IntValue(1), StringOrInt.IntValue(85), StringOrInt.StringValue("▒,3"), StringOrInt.IntValue(3))) {
            "Expected field 'magic5' contents to be [1, 85, \"▒,3\", 3], got ${struct.seq[4].contents}"
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

    @Test
    fun testEnums() {
        val yaml = """
            meta:
              id: example
            seq:
              - id: protocol
                type: u1
                enum: ip_protocol
              - id: verbose
                type: s4
                enum: verbose_levels
              - id: flags
                type: u1
                enum: bit_flags
              - id: padding
                type: u1
                enum: has_padding
            enums:
              ip_protocol:
                1: icmp
                6: tcp
                17: udp
              verbose_levels:
                0:
                  id: none
                  doc: No verbosity
                1:
                  id: low
                  doc: Low verbosity
                  doc-ref: https://example.com/low
                2:
                  id: medium
                  doc-ref: https://example.com/medium
                3:
                  id: high
              bit_flags:
                0x00: flag1
                0x10: flag2
              has_padding:
                0: false
                1: true
        """.trimIndent()

        val struct = KaitaiParser.parseYaml(yaml)

        checkNotNull(struct) { "Failed to parse Kaitai YAML" }

        check(struct.seq.size == 4) { "Expected 4 sequence items, got ${struct.seq.size}" }

        // Check field protocol
        check(struct.seq[0].id == "protocol") { "Expected first field id to be 'protocol', got '${struct.seq[0].id}'" }
        check(struct.seq[0].type is KTType.Primitive) { "Expected first field type to be primitive" }
        check((struct.seq[0].type as KTType.Primitive).type == "u1") {
            "Expected first field type to be 'u1', got '${(struct.seq[0].type as KTType.Primitive).type}'"
        }
        check(struct.seq[0].enum == "ip_protocol") {
            "Expected field 'protocol' to have enum 'ip_protocol', got '${struct.seq[0].enum}'"
        }

        // Check field verbose
        check(struct.seq[1].id == "verbose") { "Expected second field id to be 'verbose', got '${struct.seq[1].id}'" }
        check(struct.seq[1].type is KTType.Primitive) { "Expected second field type to be primitive" }
        check((struct.seq[1].type as KTType.Primitive).type == "s4") {
            "Expected second field type to be 's4', got '${(struct.seq[1].type as KTType.Primitive).type}'"
        }
        check(struct.seq[1].enum == "verbose_levels") {
            "Expected field 'verbose' to have enum 'verbose_levels', got '${struct.seq[1].enum}'"
        }

        // Check field flags
        check(struct.seq[2].id == "flags") { "Expected third field id to be 'flags', got '${struct.seq[2].id}'" }
        check(struct.seq[2].type is KTType.Primitive) { "Expected third field type to be primitive" }
        check((struct.seq[2].type as KTType.Primitive).type == "u1") {
            "Expected third field type to be 'u1', got '${(struct.seq[2].type as KTType.Primitive).type}'"
        }
        check(struct.seq[2].enum == "bit_flags") {
            "Expected field 'flags' to have enum 'bit_flags', got '${struct.seq[2].enum}'"
        }

        // Check field padding
        check(struct.seq[3].id == "padding") { "Expected fourth field id to be 'padding', got '${struct.seq[3].id}'" }
        check(struct.seq[3].type is KTType.Primitive) { "Expected fourth field type to be primitive" }
        check((struct.seq[3].type as KTType.Primitive).type == "u1") {
            "Expected fourth field type to be 'u1', got '${(struct.seq[3].type as KTType.Primitive).type}'"
        }
        check(struct.seq[3].enum == "has_padding") {
            "Expected field 'padding' to have enum 'has_padding', got '${struct.seq[3].enum}'"
        }

        // Check enums
        check(struct.enums.size == 4) { "Expected 4 enum, got ${struct.enums.size}" }

        // Check ip_protocol enum
        val ipProtocol = struct.enums["ip_protocol"]
        checkNotNull(ipProtocol) { "Expected enum 'ip_protocol' to be defined" }
        check(ipProtocol.values.size == 3) { "Expected enum 'ip_protocol' to have 3 values, got ${ipProtocol.values.size}" }

        check(ipProtocol[1]?.id is StringOrBoolean.StringValue) { "Expected enum 'ip_protocol[1]' to be 'stringValue'" }
        check(ipProtocol[1]?.id.toString() == "icmp") { "Expected enum value 1 to be 'icmp', got '${ipProtocol[1]?.id.toString()}'" }

        check(ipProtocol[6]?.id is StringOrBoolean.StringValue) { "Expected enum 'ip_protocol[6]' to be 'stringValue'" }
        check(ipProtocol[6]?.id.toString() == "tcp") { "Expected enum value 6 to be 'tcp', got '${ipProtocol[6]?.id.toString()}'" }

        check(ipProtocol[17]?.id is StringOrBoolean.StringValue) { "Expected enum 'ip_protocol[17]' to be 'stringValue'" }
        check(ipProtocol[17]?.id.toString() == "udp") { "Expected enum value 17 to be 'udp', got '${ipProtocol[17]?.id.toString()}'" }

        // Check verbose_levels enum
        val verboseLevels = struct.enums["verbose_levels"]
        checkNotNull(verboseLevels) { "Expected enum 'verbose_levels' to be defined" }
        check(verboseLevels.values.size == 4) { "Expected enum 'verbose_levels' to have 4 values, got ${verboseLevels.values.size}" }

        check(verboseLevels[0]?.id is StringOrBoolean.StringValue) { "Expected enum 'verbose_levels[0]' to be 'stringValue'" }
        check(verboseLevels[0]?.id.toString() == "none") { "Expected enum value 0 to be 'none', got '${verboseLevels[0]?.id.toString()}'" }
        check(verboseLevels[0]?.doc == "No verbosity") {
            "Expected enum value 0 doc to be 'No verbosity', got '${verboseLevels[0]?.doc}'"
        }

        check(verboseLevels[1]?.id is StringOrBoolean.StringValue) { "Expected enum 'verbose_levels[1]' to be 'stringValue'" }
        check(verboseLevels[1]?.id.toString() == "low") { "Expected enum value 1 to be 'low', got '${verboseLevels[1]?.id.toString()}'" }
        check(verboseLevels[1]?.doc == "Low verbosity") {
            "Expected enum value 1 doc to be 'Low verbosity', got '${verboseLevels[1]?.doc}'"
        }
        check(verboseLevels[1]?.docRef?.get(0) == "https://example.com/low") {
            "Expected enum value 1 doc-ref to be 'https://example.com/low', got '${verboseLevels[1]?.docRef}'"
        }

        check(verboseLevels[2]?.id is StringOrBoolean.StringValue) { "Expected enum 'verbose_levels[2]' to be 'stringValue'" }
        check(verboseLevels[2]?.id.toString() == "medium") { "Expected enum value 2 to be 'medium', got '${verboseLevels[2]?.id.toString()}'" }
        check(verboseLevels[2]?.docRef?.get(0) == "https://example.com/medium") {
            "Expected enum value 2 doc-ref to be 'https://example.com/medium', got '${verboseLevels[2]?.docRef}'"
        }

        check(verboseLevels[3]?.id is StringOrBoolean.StringValue) { "Expected enum 'verbose_levels[3]' to be 'stringValue'" }
        check(verboseLevels[3]?.id.toString() == "high") { "Expected enum value 3 to be 'high', got '${verboseLevels[3]?.id.toString()}'" }
        check(verboseLevels[3]?.doc == null) { "Expected enum value 3 doc to be null, got '${verboseLevels[3]?.doc}'" }
        check(verboseLevels[3]?.docRef == null) { "Expected enum value 3 doc-ref to be null, got '${verboseLevels[3]?.docRef}'" }

        // Check bit_flags enum
        val bitFlags = struct.enums["bit_flags"]
        checkNotNull(bitFlags) { "Expected enum 'bit_flags' to be defined" }
        check(bitFlags.values.size == 2) { "Expected enum 'bit_flags' to have 2 values, got ${bitFlags.values.size}" }

        check(bitFlags[0x00]?.id is StringOrBoolean.StringValue) { "Expected enum 'bit_flags[0x00]' to be 'stringValue'" }
        check(bitFlags[0x00]?.id.toString() == "flag1") { "Expected enum value 0x00 to be 'flag1', got '${bitFlags[0x00]?.id.toString()}'" }

        check(bitFlags[0x10]?.id is StringOrBoolean.StringValue) { "Expected enum 'bit_flags[0x10]' to be 'stringValue'" }
        check(bitFlags[0x10]?.id.toString() == "flag2") { "Expected enum value 0x10 to be 'flag2', got '${bitFlags[0x10]?.id.toString()}'" }

        // Check has_padding enum
        val hasPadding = struct.enums["has_padding"]
        checkNotNull(hasPadding) { "Expected enum 'has_padding' to be defined" }
        check(hasPadding.values.size == 2) { "Expected enum 'has_padding' to have 2 values, got ${hasPadding.values.size}" }

        check(hasPadding[0]?.id is StringOrBoolean.BooleanValue) { "Expected enum 'has_padding[0]' to be 'booleanValue'" }
        check(hasPadding[0]?.id.toString() == "false") { "Expected enum value 0 to be 'false', got '${hasPadding[0]?.id.toString()}'" }

        check(hasPadding[1]?.id is StringOrBoolean.BooleanValue) { "Expected enum 'has_padding[1]' to be 'booleanValue'" }
        check(hasPadding[1]?.id.toString() == "true") { "Expected enum value 1 to be 'true', got '${hasPadding[0]?.id.toString()}'" }
    }
}
