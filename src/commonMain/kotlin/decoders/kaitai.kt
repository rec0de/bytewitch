package decoders

import bitmage.hex
import bitmage.toBinaryString
import bitmage.toBooleanArray
import bitmage.toByteArray
import com.ionspin.kotlin.bignum.Endianness
import kotlin.js.iterator

@JsModule("js-yaml")
@JsNonModule
external object JsYaml {
    fun load(yaml: String): dynamic
    fun dump(obj: dynamic): String
}

enum class displayStyle {
    HEX, BINARY, NUMBER, STRING
}

class Type(yamlStruct: dynamic, elementStruct: dynamic) {

    var sizeInBits: Int = 0
    var sizeIsUntilEOS: Boolean = false
    var type: String = elementStruct.type.toString()
    var endianness: Endianness
    var usedDisplayStyle: displayStyle = displayStyle.HEX
    var subTypes: MutableList<Type> = mutableListOf<Type>()

    init {
        if (yamlStruct.meta.endianness != undefined) {
            endianness = yamlStruct.meta.endianness
        } else {
            endianness = Endianness.LITTLE
        }

        if (elementStruct.size != undefined) {
            sizeInBits = elementStruct.size * 8
        } else {
            if (elementStruct["size-eos"]) {
                sizeIsUntilEOS = true
            } else if (yamlStruct.types[this.type] == undefined) {  // should be its own if not else if, as size-eos can be made of subtypes
                if (this.type == "strz") {
                    sizeIsUntilEOS = true
                    usedDisplayStyle = displayStyle.STRING
                } else {
                    if (this.type.startsWith("s")) {  // signed int
                        sizeInBits = this.type.filter { it.isDigit() }.toInt() * 8
                        usedDisplayStyle = displayStyle.NUMBER
                    } else if (this.type.startsWith("u")) {  // unsigned int
                        sizeInBits = this.type.filter { it.isDigit() }.toInt() * 8
                        usedDisplayStyle = displayStyle.NUMBER
                    } else if (this.type.startsWith("f")) {  // float
                        sizeInBits = this.type.filter { it.isDigit() }.toInt() * 8
                        usedDisplayStyle = displayStyle.NUMBER
                    } else if (this.type.startsWith("b")) {  // binary
                        sizeInBits = this.type.filter { it.isDigit() }.toInt()
                        usedDisplayStyle = displayStyle.BINARY
                    } else {
                        throw RuntimeException()
                    }
                }
            } else {
                sizeInBits = 0
                for (subElementStruct in yamlStruct.types[this.type].seq) {
                    var subType = Type(yamlStruct, subElementStruct)
                    subTypes.add(subType)
                    sizeInBits += subType.sizeInBits
                }
            }
        }
    }
}

object Kaitai : ByteWitchDecoder {
    override val name = ""

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val ethernetStruct = """
meta:
  id: ethernet_frame
  title: Ethernet frame (layer 2, IEEE 802.3)
  xref:
    ieee: 802.3
    wikidata: Q11331406
  license: CC0-1.0
  ks-version: 0.8
  imports:
    - /network/ipv4_packet
    - /network/ipv6_packet
doc: |
  Ethernet frame is a OSI data link layer (layer 2) protocol data unit
  for Ethernet networks. In practice, many other networks and/or
  in-file dumps adopted the same format for encapsulation purposes.
doc-ref: https://ieeexplore.ieee.org/document/7428776
seq:
  - id: dst_mac
    size: 6
    doc: Destination MAC address
  - id: src_mac
    size: 6
    doc: Source MAC address
  - id: ether_type_1
    type: u2be
    enum: ether_type_enum
    doc: Either ether type or TPID if it is a IEEE 802.1Q frame
  - id: tci
    type: tag_control_info
    if: ether_type_1 == ether_type_enum::ieee_802_1q_tpid
  - id: ether_type_2
    type: u2be
    enum: ether_type_enum
    if: ether_type_1 == ether_type_enum::ieee_802_1q_tpid
  - id: body
    size-eos: true
    type:
      switch-on: ether_type
      cases:
        'ether_type_enum::ipv4': ipv4_packet
        'ether_type_enum::ipv6': ipv6_packet
instances:
  ether_type:
    value: |
      (ether_type_1 == ether_type_enum::ieee_802_1q_tpid) ? ether_type_2 : ether_type_1
    doc: |
      Ether type can be specified in several places in the frame. If
      first location bears special marker (0x8100), then it is not the
      real ether frame yet, an additional payload (`tci`) is expected
      and real ether type is upcoming next.
types:
  tag_control_info:
    doc: |
      Tag Control Information (TCI) is an extension of IEEE 802.1Q to
      support VLANs on normal IEEE 802.3 Ethernet network.
    seq:
      - id: priority
        type: b3
        doc: |
          Priority Code Point (PCP) is used to specify priority for
          different kinds of traffic.
      - id: drop_eligible
        type: b1
        doc: |
          Drop Eligible Indicator (DEI) specifies if frame is eligible
          to dropping while congestion is detected for certain classes
          of traffic.
      - id: vlan_id
        type: test
        doc: |
          VLAN Identifier (VID) specifies which VLAN this frame
          belongs to.
  test:
    seq:
      - id: a
        type: b4
      - id: b
        type: b8
enums:
  # https://www.iana.org/assignments/ieee-802-numbers/ieee-802-numbers.xhtml
  ether_type_enum:
    0x0800: ipv4
    0x0801: x_75_internet
    0x0802: nbs_internet
    0x0803: ecma_internet
    0x0804: chaosnet
    0x0805: x_25_level_3
    0x0806: arp
    0x8100: ieee_802_1q_tpid
    0x86dd: ipv6
    #0x88a8: ieee_802_1ad_tpid"""

        val ethernetYaml = JsYaml.load(ethernetStruct)

        return processSeq(ethernetYaml.meta.id, ethernetYaml, ethernetYaml.seq, data.toBooleanArray(), sourceOffset)
    }

    fun processSeq(id: String, yamlStruct: dynamic, seqStruct: dynamic, data: BooleanArray, sourceOffsetInBits: Int) : ByteWitchResult {
        var currentOffsetInBits = 0
        /*
        Entweder data als ByteArray und Bitshiften
        var test = 13  -> 1101
        test[6..8] = 1
        test and 0b00000111 >> 0 = -> 101
        test and 0b00111000 >> 3 = -> 001
        oder data als BooleanArray
        */
        val kaitaiBytesList = mutableListOf<ByteWitchResult>()
        //val types = mutableSetOf<Type>()

        for (element in seqStruct) {
            val type = Type(yamlStruct, element)
            if (type.sizeIsUntilEOS) {
                type.sizeInBits = (data.size - currentOffsetInBits)
            }

            val elementId = element.id

            var kaitaiElement : ByteWitchResult
            val value = data.sliceArray(currentOffsetInBits .. currentOffsetInBits + type.sizeInBits -1)
            val sourceByteRange = Pair((currentOffsetInBits + sourceOffsetInBits).toFloat()/8, (sourceOffsetInBits + currentOffsetInBits + type.sizeInBits).toFloat()/8)

            if (type.subTypes.isNotEmpty()) {
                kaitaiElement = processSeq(elementId, yamlStruct, yamlStruct.types[element.type].seq, value, sourceOffsetInBits + currentOffsetInBits)
            } else {
                val endianness = Endianness.LITTLE
                if (type.usedDisplayStyle == displayStyle.BINARY) {
                    kaitaiElement = KaitaiBinary(
                        elementId,
                        endianness,
                        value,
                        sourceByteRange
                    )
                } else { //displayStyle.HEX as the fallback
                    kaitaiElement = KaitaiBytes(
                        elementId,
                        endianness,
                        value,
                        sourceByteRange
                    )
                }
            }

            kaitaiBytesList.add(kaitaiElement)

            currentOffsetInBits += type.sizeInBits
        }

        return KaitaiResult(id, kaitaiBytesList, Pair(sourceOffsetInBits.toFloat()/8, (data.size + sourceOffsetInBits).toFloat()/8))
    }

    override fun confidence(data: ByteArray): Double {
        return 1.0
    }

    override fun decodesAsValid(data: ByteArray) = Pair(confidence(data) > 0.33, null)
}

class KaitaiResult(val id: String, val kaitaiBytesList: List<ByteWitchResult>, override val sourceByteRange: Pair<Float, Float>): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${kaitaiBytesList.joinToString("") { it.renderHTML() }})</div>"
    }
}

class KaitaiElement(val id: String, val endianness: Endianness, val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().hex()})h</div>"
    }
}

class KaitaiBytes(val id: String, val endianness: Endianness, val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().hex()})h</div>"
    }
}

class KaitaiNumber(val id: String, val endianness: Endianness, val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>I am a Number</div>"
    }
}

class KaitaiBinary(val id: String, val endianness: Endianness, val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.joinToString("") { if (it) "1" else "0" }})b</div>"
    }
}

class KaitaiString(val id: String, val endianness: Endianness, val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): ByteWitchResult {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>I am a String</div>"
    }
}
/*
class KaitaiArray(val id: String, val endianness: Endianness, val value: ByteArray, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override fun renderHTML(): String {
        val formattedValues = ""
        for (i in 0 until (value.size / valuesSizeBytes)) {
            formattedValues.plus("<span>${value.slice(i*valuesSizeBytes ..i*valuesSizeBytes+valuesSizeBytes)}</span>")
        }
        return "<div class=\"generic roundbox\" $byteRangeDataTags>" +
                formattedValues +
                "</div>"
    }
}
*/