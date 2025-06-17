package decoders

import bitmage.ByteOrder
import bitmage.hex
import bitmage.toBinaryString
import bitmage.toBooleanArray
import bitmage.toByteArray
import bitmage.toInt
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
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
    var endianness: ByteOrder
    var usedDisplayStyle: displayStyle = displayStyle.HEX
    var subTypes: MutableList<Type> = mutableListOf<Type>()

    init {
        if (yamlStruct.meta.endianness != undefined) {
            endianness = yamlStruct.meta.endianness
        } else {
            endianness = ByteOrder.LITTLE
        }

        if (elementStruct.size != undefined) {
            sizeInBits = elementStruct.size * 8
        } else {
            if (elementStruct["size-eos"]) {
                sizeIsUntilEOS = true
            } else if (yamlStruct.types[this.type] == undefined) {  // should be its own if not else if, as size-eos can be made of subtypes
                if (this.type == "strz") {  //totally wrong, is actually a shortcut for type = str + terminator = 0. Other terminator bytes can exist
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
        //val decodedResult: MutableMap<Any, Any> = mutableMapOf()

        val kaitaiInput = document.getElementById("kaitaiinput") as HTMLTextAreaElement
        val kaitaiYaml = JsYaml.load(kaitaiInput.value)

        return processSeq(kaitaiYaml.meta.id, kaitaiYaml, kaitaiYaml.seq, data.toBooleanArray(), sourceOffset)
    }

    fun processSeq(id: String, yamlStruct: dynamic, seqStruct: dynamic, data: BooleanArray, sourceOffsetInBits: Int) : KaitaiElement {
        var currentOffsetInBits = 0
        /*
        Entweder data als ByteArray und Bitshiften
        var test = 13  -> 1101
        test[6..8] = 1
        test and 0b00000111 >> 0 = -> 101
        test and 0b00111000 >> 3 = -> 001
        oder data als BooleanArray
        */
        val kaitaiBytesList = mutableListOf<KaitaiElement>()
        //val types = mutableSetOf<Type>()

        for (element in seqStruct) {
            val type = Type(yamlStruct, element)
            if (type.sizeIsUntilEOS) {
                type.sizeInBits = (data.size - currentOffsetInBits)
            }

            val elementId = element.id

            var kaitaiElement : KaitaiElement
            val value = data.sliceArray(currentOffsetInBits .. currentOffsetInBits + type.sizeInBits -1)
            val sourceByteRange = Pair((currentOffsetInBits + sourceOffsetInBits).toFloat()/8, (sourceOffsetInBits + currentOffsetInBits + type.sizeInBits).toFloat()/8)

            if (type.subTypes.isNotEmpty()) {
                kaitaiElement = processSeq(elementId, yamlStruct, yamlStruct.types[element.type].seq, value, sourceOffsetInBits + currentOffsetInBits)
            } else {
                val endianness = ByteOrder.LITTLE
                kaitaiElement = if (type.usedDisplayStyle == displayStyle.BINARY) {
                    KaitaiBinary(
                        elementId,
                        endianness,
                        value,
                        sourceByteRange
                    )
                } else { //displayStyle.HEX as the fallback
                    KaitaiBytes(
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

interface KaitaiElement : ByteWitchResult {
    val id: String
    val kaitaiBytesList: List<ByteWitchResult>? get() = null
    val value: BooleanArray? get() = null
}

class KaitaiResult(override val id: String, override val kaitaiBytesList: List<ByteWitchResult>, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${kaitaiBytesList.joinToString("") { it.renderHTML() }})</div>"
    }
}

class KaitaiBytes(override val id: String, val endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().hex()})h</div>"
    }
}

class KaitaiInteger(override val id: String, val endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().toInt(endianness)})h</div>"
    }
}

class KaitaiBinary(override val id: String, val endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.joinToString("") { if (it) "1" else "0" }})b</div>"
    }
}

class KaitaiString(override val id: String, val endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>I am a String</div>"
    }
}
/*
class KaitaiArray(val id: String, val endianness: ByteOrder, val value: ByteArray, override val sourceByteRange: Pair<Int, Int>): KaitaiElement {
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