package decoders

import bitmage.ByteOrder
import bitmage.hex
import bitmage.toBooleanArray
import bitmage.toByteArray
import bitmage.toInt
import bitmage.toUInt
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
        get() { return field }
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

class Type(val completeStruct: dynamic, val currentElementStruct: dynamic, val bytesListTree: MutableListTree<KaitaiElement>) {  // TODO: integrate Type Object and it's things into the seq parsing, as a lot of stuff from there is actually needed here
    var sizeInBits: UInt = 0u
    var sizeIsUntilEOS: Boolean = false
    var sizeIsUntilTerminator: Boolean = false
    var terminator: ByteArray? = null
    var type: String = currentElementStruct.type.toString()
    var endianness: ByteOrder
    var usedDisplayStyle: DisplayStyle = DisplayStyle.HEX
    var subTypes: MutableList<Type> = mutableListOf<Type>()

    fun parseBuiltinType() {
        if (type == "strz") {
            usedDisplayStyle = DisplayStyle.STRING
            sizeIsUntilTerminator = true
            terminator = ByteArray(0x00)
        } else if (type == "str") {
            usedDisplayStyle = DisplayStyle.STRING
            sizeIsUntilTerminator = true
            terminator = currentElementStruct.terminator // TODO: Make proper method out of this. Could probably be a value defined somewhere else aswell :(
        } else {
            if (Regex("^s\\d+(le|be)?\$").matches(type)) {  // signed int
                sizeInBits = type.filter { it.isDigit() }.toUInt() * 8u
                usedDisplayStyle = DisplayStyle.UNSIGNED_INTEGER
            } else if (Regex("^u\\d+(le|be)?\$").matches(type)) {  // unsigned int
                sizeInBits = type.filter { it.isDigit() }.toUInt() * 8u
                usedDisplayStyle = DisplayStyle.SIGNED_INTEGER
            } else if (Regex("^f\\d+(le|be)?\$").matches(type)) {  // float
                sizeInBits = type.filter { it.isDigit() }.toUInt() * 8u
                usedDisplayStyle = DisplayStyle.FLOAT
            } else if (Regex("^b\\d+(le|be)?\$").matches(type)) {  // binary
                sizeInBits = type.filter { it.isDigit() }.toUInt()
                usedDisplayStyle = DisplayStyle.BINARY
            } else {
                throw RuntimeException("Attempted to parse as builtin type $type but that doesn't seem to be a valid type")
            }
            if (type.endsWith("be")) { // We sometimes need to overwrite the byteorder even after parsing the endianness
                this.endianness = ByteOrder.BIG
            } else if (type.endsWith("le")) {
                this.endianness = ByteOrder.LITTLE
            }
        }
    }

    // TODO: This is a duplicate of the one in Kaitai.kt, should be replaced as soon as the parse methods are moved to a companion object
    fun parseEndian(currentElementStruct: dynamic, completeStruct: dynamic) : ByteOrder {
        // TODO: endian is significantly more complicated with switch-on and the fact that its in a different location than what I assume here
        val actualStruct = if (currentElementStruct.endian != undefined) {
            currentElementStruct
        } else if (completeStruct.endian != undefined) {
            completeStruct
        } else {
            return ByteOrder.BIG
        }

        if (actualStruct.endian == "be") {
            return ByteOrder.BIG
        } else {
            return ByteOrder.LITTLE
        }
    }

    // TODO: See the comment above, this is a duplicate of the one in Kaitai.kt
    fun parseValue(value: dynamic, bytesListTree: MutableListTree<KaitaiElement>) : BooleanArray {
        val flattenedArray = js("[value].flat(2)")  // handle both cases of 0x0a as well as [0x0a]
        var fullyFlatArray = booleanArrayOf()
        for (element in flattenedArray) {
            if (element is Int) {
                fullyFlatArray += (element as Int).toByte().toBooleanArray()
            } else {
                try {
                    fullyFlatArray += parseReference(element, bytesListTree)
                } catch (e: Exception) {
                    for (i in 0..element.length - 1) {
                        fullyFlatArray += (element.charCodeAt(i) as Int).toByte().toBooleanArray()
                    }
                }
            }
        }
        return fullyFlatArray
    }

    // TODO: See the comment above, this is a duplicate of the one in Kaitai.kt
    fun parseReference(reference: String, bytesListTree: MutableListTree<KaitaiElement>): BooleanArray {
        // if it is not a valid reference, i.e. there exists no element in the seq with the id @param: reference, then an exception is thrown
        if (reference.startsWith("_root.")) //_root.magic
            return parseReference(reference.removePrefix("_root."), bytesListTree.root!!)
        else if (reference.startsWith("_parent."))
            return parseReference(reference.removePrefix("_parent."), bytesListTree.parent!!)
        else (
                if (reference.contains("."))
                    return parseReference(reference.substringAfter("."), bytesListTree.find { it.id == reference.substringBefore(".") }!!.bytesListTree!!)
                else
                    return bytesListTree.find { it.id == reference }!!.value
                )
    }

    init {
        endianness = parseEndian(currentElementStruct, completeStruct)

        if (currentElementStruct.size != undefined) {
            val parsedValue = parseValue(currentElementStruct.size, bytesListTree)
            sizeInBits = parsedValue.toByteArray().toUInt(endianness) * 8u
        } else {
            if (currentElementStruct.contents != undefined) {
                val tmp = parseValue(currentElementStruct.contents, bytesListTree)
                sizeInBits = tmp.size.toUInt()
                console.log(sizeInBits)
            } else if (currentElementStruct["size-eos"]) {
                sizeIsUntilEOS = true
            } else if ((completeStruct.types != undefined) && completeStruct.types[this.type] != undefined) {  // parse subtypes
                sizeInBits = 0u
                for (subElementStruct in completeStruct.types[this.type].seq) {
                    var subType = Type(completeStruct, subElementStruct, bytesListTree)
                    subTypes.add(subType)
                    sizeInBits += subType.sizeInBits
                }
            } else {  // TODO should be its own if not else if, as size-eos can be made of subtypes
                // TODO could also be an imported type instead...
                parseBuiltinType()
            }
        }
    }
}

// TODO[IMPORTANT]: Move the ByteWitchDecoder into a companion object as the parse methods are static and do not need an instance. See the other decoders for examples
class Kaitai(val kaitaiName: String, val kaitaiStruct: String) : ByteWitchDecoder {
    override val name = "Kaitai-$kaitaiName"
    //lateinit var completeStruct: JsYaml

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        /*
        val kaitaiInput = document.getElementById("kaitaiinput") as HTMLTextAreaElement
        val kaitaiYaml = JsYaml.load(kaitaiInput.value)
        //completeStruct = kaitaiYaml
         */
        val kaitaiYaml = JsYaml.load(kaitaiStruct)
        val result = try {  // JS Exceptions don't get simply logged to console but instead trigger the big red overlay. We convert JS Errors to Kotlin Exceptions here
            processSeq(kaitaiYaml.meta.id, null, kaitaiYaml, kaitaiYaml.seq, data.toBooleanArray(), sourceOffset)
        } catch (e: dynamic) {
            throw Exception("Unexpected JS Exception has been thrown:\n$e")
        }

        return result
    }

    fun parseEndian(currentElementStruct: dynamic, completeStruct: dynamic) : ByteOrder {
        // TODO: endian is significantly more complicated with switch-on and the fact that its in a different location than what I assume here
        val actualStruct = if (currentElementStruct.endian != undefined) {
            currentElementStruct
        } else if (completeStruct.endian != undefined) {
            completeStruct
        } else {
            return ByteOrder.BIG
        }

        if (actualStruct.endian == "be") {
            return ByteOrder.BIG
        } else {
            return ByteOrder.LITTLE
        }
    }

    fun parseReference(reference: String, bytesListTree: MutableListTree<KaitaiElement>): BooleanArray {
        // if it is not a valid reference, i.e. there exists no element in the seq with the id @param: reference, then an exception is thrown
        if (reference.startsWith("_root.")) //_root.magic
            return parseReference(reference.removePrefix("_root."), bytesListTree.root!!)
        else if (reference.startsWith("_parent.")) 
            return parseReference(reference.removePrefix("_parent."), bytesListTree.parent!!)
        else (
            if (reference.contains("."))
                return parseReference(reference.substringAfter("."), bytesListTree.find { it.id == reference.substringBefore(".") }!!.bytesListTree!!)
            else
                return bytesListTree.find { it.id == reference }!!.value
        )
    }

    fun parseValue(value: dynamic, bytesListTree: MutableListTree<KaitaiElement>) : BooleanArray {
        val flattenedArray = js("[value].flat(2)")  // handle both cases of 0x0a as well as [0x0a]
        var fullyFlatArray = booleanArrayOf()
        for (element in flattenedArray) {
            if (element is Int) {
                fullyFlatArray += (element as Int).toByte().toBooleanArray()
            } else {
                try {
                    fullyFlatArray += parseReference(element, bytesListTree)
                } catch (e: Exception) {
                    for (i in 0..element.length - 1) {
                        fullyFlatArray += (element.charCodeAt(i) as Int).toByte().toBooleanArray()
                    }
                }
            }
        }
        return fullyFlatArray
    }

    fun checkContentsKey(seqElement: dynamic, dataBytes: BooleanArray, bytesListTree: MutableListTree<KaitaiElement>, kaitaiElement: KaitaiElement): Boolean {
        return parseValue(seqElement.contents, bytesListTree).contentEquals(dataBytes)
    }

    fun checkValidKey(seqElement: dynamic, dataBytes: BooleanArray, bytesListTree: MutableListTree<KaitaiElement>, kaitaiElement: KaitaiElement): Boolean {
        if (seqElement.valid.min != undefined || seqElement.valid.max != undefined) {
            val parsedValue = dataBytes.toByteArray().toUInt(kaitaiElement.endianness)
            val parsedValidMin = parseValue(seqElement.valid.min, bytesListTree).toByteArray().toUInt(kaitaiElement.endianness)
            if ((seqElement.valid.min != undefined) && (parsedValue < parsedValidMin)) {
                return false
            }
            val parsedValidMax = parseValue(seqElement.valid.max, bytesListTree).toByteArray().toUInt(kaitaiElement.endianness)
            if ((seqElement.valid.max != undefined) && (parsedValue > parsedValidMax)) {
                return false
            }
            return true
//            for (i in 0..value.toByteArray().size-1) {
//                if ((valid.min != undefined) && (value.toByteArray()[i].toUByte() < parseValue(valid.min, bytesListTree).toByteArray()[i].toUByte())) {
//                    return false
//                }
//                //if ((valid.max != undefined) && (value.toByteArray()[i].toUByte() > parseValue(valid.max, bytesListTree).toByteArray().padToSize(value.toByteArray().size, endianness)[i].toUByte())) {
//                if ((valid.max != undefined) && (value.toByteArray()[i].toUByte() > parseValue(valid.max, bytesListTree).toByteArray()[i].toUByte())) {
//                    return false
//                }
//            }
        } else if (seqElement.valid["any-of"] != undefined) {
            for (i in 0..seqElement.valid["any-of"].length-1) {
                if (parseValue(seqElement.valid["any-of"][i], bytesListTree).contentEquals(dataBytes)) {
                    return true
                }
            }
            return false
        } else if (seqElement.valid.expr != undefined) {
            // TODO parse arbitrary expression
        } else {
            val valueToCheckAgainst = if (seqElement.valid.eq != undefined) {
                parseValue(seqElement.valid.eq, bytesListTree)
            } else {
                parseValue(seqElement.valid, bytesListTree)
            }
            return valueToCheckAgainst.contentEquals(dataBytes)
        }
        return true
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

        val endianness = parseEndian(completeStruct, completeStruct)

        for (seqElement in currentSeqStruct) {
            val type = Type(completeStruct, seqElement, bytesListTree)
            if (type.sizeIsUntilEOS) {
                type.sizeInBits = (data.size - currentOffsetInBits).toUInt()
            }
            val elementId = seqElement.id

            var kaitaiElement : KaitaiElement
            val value = data.sliceArray(currentOffsetInBits .. currentOffsetInBits + type.sizeInBits.toInt() -1)
            val sourceByteRange = Pair((currentOffsetInBits + sourceOffsetInBits).toFloat()/8, (sourceOffsetInBits + currentOffsetInBits + type.sizeInBits.toInt()).toFloat()/8)

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

            if ((seqElement.contents != undefined) && !checkContentsKey(seqElement, value, bytesListTree, kaitaiElement)) {
                throw Exception("Value of bytes does not align with expected contents value.")
            }
            if ((seqElement.valid != undefined) && !checkValidKey(seqElement, value, bytesListTree, kaitaiElement)) {
                throw Exception("Value of bytes does not align with expected valid value.")
            }

            bytesListTree.add(kaitaiElement)

            currentOffsetInBits += type.sizeInBits.toInt()
        }

        return KaitaiResult(id, endianness, bytesListTree, Pair(sourceOffsetInBits.toFloat()/8, (data.size + sourceOffsetInBits).toFloat()/8))
    }

    override fun confidence(data: ByteArray): Double {
        return 1.0
    }

    override fun decodesAsValid(data: ByteArray) = Pair(confidence(data) > 0.33, null)
}

interface KaitaiElement : ByteWitchResult {
    val id: String
    val bytesListTree: MutableListTree<KaitaiElement>? get() = null
    val value: BooleanArray
    var endianness: ByteOrder
}

class KaitaiResult(override val id: String, override var endianness: ByteOrder,
                   override val bytesListTree: MutableListTree<KaitaiElement>, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${bytesListTree.joinToString("") { it.renderHTML() }})</div>"
    }

    // KaitaiResult result does not really have a value itself, but if it's called we want to deliver a reasonable result
    override val value: BooleanArray
        get() {
            var result = booleanArrayOf()
            for (element in bytesListTree) {
                result += element.value
            }
            return result
        }
}

class KaitaiBytes(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().hex()})h</div>"
    }
}

class KaitaiInteger(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.toByteArray().toInt(endianness)})h</div>"
    }
}

class KaitaiBinary(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
    override fun renderHTML(): String {
        return "<div class=\"generic roundbox\" $byteRangeDataTags>${id}(${value.joinToString("") { if (it) "1" else "0" }})b</div>"
    }
}

class KaitaiString(override val id: String, override var endianness: ByteOrder, override val value: BooleanArray, override val sourceByteRange: Pair<Float, Float>): KaitaiElement {
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