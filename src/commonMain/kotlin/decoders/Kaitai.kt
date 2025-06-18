package decoders

import bitmage.ByteOrder
import bitmage.hex
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

enum class DisplayStyle {
    HEX, BINARY, SIGNED_INTEGER, UNSIGNED_INTEGER, FLOAT, STRING
}

// acts just like a MutableList except it also has the added features
class MutableListTree<T>(private val innerList: MutableList<T> = mutableListOf()) : MutableList<T> by innerList {
    var parent: MutableListTree<T>? = null
        get() { return parent }
        set(value) {field = value}

    var root: MutableListTree<T>? = null
        get() {
            var rootCandidate: MutableListTree<T> = this
            while (rootCandidate.parent != null) {
                rootCandidate = rootCandidate.parent!!
            }
            return rootCandidate
        }
        private set
}

class Type(completeStruct: dynamic, currentElementStruct: dynamic) {
    var sizeInBits: Int = 0
    var sizeIsUntilEOS: Boolean = false
    var type: String = currentElementStruct.type.toString()
    var endianness: ByteOrder
    var usedDisplayStyle: DisplayStyle = DisplayStyle.HEX
    var subTypes: MutableList<Type> = mutableListOf<Type>()

    fun parseBuiltinType() {
        if (this.type == "strz") {  // TODO totally wrong, is actually a shortcut for type = str + terminator = 0. Other terminator bytes can exist
            sizeIsUntilEOS = true
            usedDisplayStyle = DisplayStyle.STRING
        } else {
            if (this.type.startsWith("s")) {  // signed int
                sizeInBits = this.type.filter { it.isDigit() }.toInt() * 8
                usedDisplayStyle = DisplayStyle.UNSIGNED_INTEGER
            } else if (this.type.startsWith("u")) {  // unsigned int
                sizeInBits = this.type.filter { it.isDigit() }.toInt() * 8
                usedDisplayStyle = DisplayStyle.SIGNED_INTEGER
            } else if (this.type.startsWith("f")) {  // float
                sizeInBits = this.type.filter { it.isDigit() }.toInt() * 8
                usedDisplayStyle = DisplayStyle.FLOAT
            } else if (this.type.startsWith("b")) {  // binary
                sizeInBits = this.type.filter { it.isDigit() }.toInt()
                usedDisplayStyle = DisplayStyle.BINARY
            } else {
                throw RuntimeException()
            }
        }
    }

    init {
        if (completeStruct.meta.endianness != undefined) {
            endianness = completeStruct.meta.endianness
        } else {
            endianness = ByteOrder.LITTLE
        }

        if (currentElementStruct.size != undefined) {
            sizeInBits = currentElementStruct.size * 8
        } else {
            if (currentElementStruct["size-eos"]) {
                sizeIsUntilEOS = true
            } else if (completeStruct.types[this.type] == undefined) {  // TODO should be its own if not else if, as size-eos can be made of subtypes
                // TODO could also be an imported type instead...
                parseBuiltinType()
            } else {
                sizeInBits = 0
                for (subElementStruct in completeStruct.types[this.type].seq) {
                    var subType = Type(completeStruct, subElementStruct)
                    subTypes.add(subType)
                    sizeInBits += subType.sizeInBits
                }
            }
        }
    }
}

object Kaitai : ByteWitchDecoder {
    override val name = "Kaitai"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val kaitaiInput = document.getElementById("kaitaiinput") as HTMLTextAreaElement
        val kaitaiYaml = JsYaml.load(kaitaiInput.value)

        return processSeq(kaitaiYaml.meta.id, null, kaitaiYaml, kaitaiYaml.seq, data.toBooleanArray(), sourceOffset)
    }

    fun processSeq(id: String, parentBytesListTree: MutableListTree<KaitaiElement>?, completeStruct: dynamic, currentSeqStruct: dynamic, data: BooleanArray, sourceOffsetInBits: Int) : KaitaiElement {
        var currentOffsetInBits = 0
        /*
        Entweder data als ByteArray und Bitshiften
        var test = 13  -> 1101
        test[6..8] = 1
        test and 0b00000111 >> 0 = -> 101
        test and 0b00111000 >> 3 = -> 001
        oder data als BooleanArray so wie aktuell. Vermutlich inperformant, aber wohl gut genug
        */
        val bytesListTree = MutableListTree<KaitaiElement>()
        bytesListTree.parent = parentBytesListTree
        //val types = mutableSetOf<Type>()

        for (seqElement in currentSeqStruct) {
            val type = Type(completeStruct, seqElement)
            if (type.sizeIsUntilEOS) {
                type.sizeInBits = (data.size - currentOffsetInBits)
            }

            val elementId = seqElement.id

            var kaitaiElement : KaitaiElement
            val value = data.sliceArray(currentOffsetInBits .. currentOffsetInBits + type.sizeInBits -1)
            val sourceByteRange = Pair((currentOffsetInBits + sourceOffsetInBits).toFloat()/8, (sourceOffsetInBits + currentOffsetInBits + type.sizeInBits).toFloat()/8)

            if (type.subTypes.isNotEmpty()) {
                kaitaiElement = processSeq(elementId, bytesListTree, completeStruct, completeStruct.types[seqElement.type].seq, value, sourceOffsetInBits + currentOffsetInBits)
            } else {
                kaitaiElement = if (type.usedDisplayStyle == DisplayStyle.BINARY) {
                    KaitaiBinary(
                        elementId,
                        type.endianness,
                        value,
                        sourceByteRange
                    )
                } else { //displayStyle.HEX as the fallback
                    KaitaiBytes(
                        elementId,
                        type.endianness,
                        value,
                        sourceByteRange
                    )
                }
            }

            bytesListTree.add(kaitaiElement)

            currentOffsetInBits += type.sizeInBits
        }

        return KaitaiResult(id, bytesListTree, Pair(sourceOffsetInBits.toFloat()/8, (data.size + sourceOffsetInBits).toFloat()/8))
    }

    override fun confidence(data: ByteArray): Double {
        return 1.0
    }

    override fun decodesAsValid(data: ByteArray) = Pair(confidence(data) > 0.33, null)
}

interface KaitaiElement : ByteWitchResult {
    val id: String
    val bytesListTree: MutableListTree<KaitaiElement>? get() = null
    val value: BooleanArray? get() = null
}

class KaitaiResult(override val id: String, override val bytesListTree: MutableListTree<KaitaiElement>, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${bytesListTree.joinToString("") { it.renderHTML() }})</div>"
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