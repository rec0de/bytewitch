package decoders

import BWAnnotatedData
import BWGenericSequence
import BWString
import ByteWitchResult
import ParseCompanion
import AnnotatedResult
import AnnotatedResult.Type
import bitmage.ByteOrder
import htmlEscape
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

abstract class QuickDirtyDecoder: ParseCompanion() {
    protected var data: ByteArray = byteArrayOf()
    protected var sourceOffset: Int = 0
    protected var decoderName: String = "unknown"

    private val structs = mutableListOf<MutableList<AnnotatedResult>>()
    private val structStartOffsets = mutableListOf<Int>()

    private val currentStruct: MutableList<AnnotatedResult>
        get() = structs.last()

    protected val effectiveParsePosition: Int
        get() = sourceOffset + parseOffset

    enum class Format {
        DECIMAL, BYTES, HEX, LOOKUP
    }

    companion object {
        private fun humanReadableByteCount(len: ULong): String {
            val kb = 1024UL
            val mb = kb * 1024UL

            return when(len) {
                in 0UL..(5U*kb) -> "$len B"
                in 5U*kb..5U*mb -> "${len/kb} KiB"
                else -> "${len/mb} MiB"
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private var isInParse = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    protected fun start(bytes: ByteArray, sourceOffset: Int, decoderName: String) {
        check(isInParse.compareAndSet(expectedValue = false, newValue = true)) { "QuickDirtyParser reentry while parsing"}
        parseOffset = 0
        structs.clear()
        data = bytes
        this.decoderName = decoderName
        this.sourceOffset = sourceOffset

        // create root structure
        enterSubstruct()
    }

    @OptIn(ExperimentalAtomicApi::class)
    protected fun finish(): BWGenericSequence {
        isInParse.store(false)
        check(structs.size == 1) { "finish() called with open substruct" }

        val result = BWGenericSequence(currentStruct, Pair(structStartOffsets.last(), effectiveParsePosition))
        result.annotate(decoderName, Type.STRUCTURE)
        return result
    }

    protected fun enterSubstruct() {
        structs.add(mutableListOf())
        structStartOffsets.add(effectiveParsePosition)
    }

    protected fun leaveSubstruct(): BWGenericSequence {
        val subresult = BWGenericSequence(currentStruct, Pair(structStartOffsets.last(), effectiveParsePosition))
        subresult.annotate(decoderName, AnnotatedResult.Type.STRUCTURE)

        structs.removeAt(structs.lastIndex)
        structStartOffsets.removeAt(structStartOffsets.lastIndex)

        currentStruct.add(subresult)
        return subresult
    }

    protected fun addForeignParsedField(result: AnnotatedResult) {
        result.source = decoderName
        currentStruct.add(result)
    }

    // Data reading functions from here on

    protected fun processString(size: Int, label: String): String {
        val start = effectiveParsePosition
        val value = readString(data, size)
        val subresult = BWString("$label ${htmlEscape(value)}".trim(), Pair(start, effectiveParsePosition))
        subresult.annotate(decoderName, Type.UTF8)

        currentStruct.add(subresult)
        return value
    }

    protected fun processLengthPrefixedString(sizePrefixLen: Int, byteOrder: ByteOrder, label: String): String {
        val prefixInterpretation = if(byteOrder == ByteOrder.BIG) Type.LENGTH_FLAG_BE else Type.LENGTH_FLAG_LE
        val stringLength = processUInt(sizePrefixLen, byteOrder, "Length", Format.BYTES, interpretation = prefixInterpretation)

        return if(stringLength > 0U)
            processString(stringLength.toInt(), label)
        else
            ""
    }

    protected fun processBytes(size: Int, label: String, interpretation: Type = Type.UNKNOWN): ByteArray {
        val start = effectiveParsePosition
        val value = readBytes(data, size)
        val subresult = BWAnnotatedData(label, value, Pair(start, effectiveParsePosition))
        subresult.annotate(decoderName, interpretation)

        currentStruct.add(subresult)
        return value
    }

    protected fun processBytes(size: Int, interpretation: Type = Type.UNKNOWN, content: (bytes: ByteArray) -> String): ByteArray {
        val start = effectiveParsePosition
        val value = readBytes(data, size)
        val rendering = content(value)
        val subresult = BWString(rendering, Pair(start, effectiveParsePosition))
        subresult.annotate(decoderName, interpretation)

        currentStruct.add(subresult)
        return value
    }

    protected fun processInt(size: Int, byteOrder: ByteOrder, label: String, format: Format = Format.DECIMAL, explicitlySigned: Boolean = false, lookupMap: Map<Int, String>? = null, interpretation: Type? = null): Int {
        val start = effectiveParsePosition
        val value = readInt(data, size, explicitlySigned, byteOrder)
        val renderedValue = when(format) {
            Format.DECIMAL -> value.toString()
            Format.HEX -> "0x${value.toString(16).padStart(size*2, '0')}"
            Format.BYTES -> humanReadableByteCount(value.toULong())
            Format.LOOKUP -> lookupMap!!.getOrElse(value){ "Unknown($value)" }
        }

        val effectiveInterpretation = interpretation ?: Type.getIntType(signed = true, size, byteOrder)

        val fullLabel = "$label: $renderedValue"
        val subresult = BWString(fullLabel, Pair(start, effectiveParsePosition))
        subresult.annotate(decoderName, effectiveInterpretation)

        currentStruct.add(subresult)
        return value
    }

    protected fun processUInt(size: Int, byteOrder: ByteOrder, label: String, format: Format = Format.DECIMAL, lookupMap: Map<UInt, String>? = null, interpretation: Type? = null): UInt {
        val start = effectiveParsePosition
        val value = readUInt(data, size, byteOrder)
        val renderedValue = when(format) {
            Format.DECIMAL -> value.toString()
            Format.HEX -> "0x${value.toString(16).padStart(size*2, '0')}"
            Format.BYTES -> humanReadableByteCount(value.toULong())
            Format.LOOKUP -> lookupMap!!.getOrElse(value){ "Unknown($value)" }
        }

        val effectiveInterpretation = interpretation ?: Type.getIntType(signed = false, size, byteOrder)

        val fullLabel = "$label: $renderedValue"
        val subresult = BWString(fullLabel, Pair(start, effectiveParsePosition))
        subresult.annotate(decoderName, effectiveInterpretation)

        currentStruct.add(subresult)
        return value
    }

    protected fun processUInt(size: Int, byteOrder: ByteOrder, interpretation: Type? = null, renderer: (UInt) -> String,): UInt {
        val start = effectiveParsePosition
        val value = readUInt(data, size, byteOrder)
        val renderedValue = renderer(value)

        val effectiveInterpretation = interpretation ?: Type.getIntType(signed = false, size, byteOrder)

        val subresult = BWString(renderedValue, Pair(start, effectiveParsePosition))
        subresult.annotate(decoderName, effectiveInterpretation)

        currentStruct.add(subresult)
        return value
    }

    protected fun processULong(size: Int, byteOrder: ByteOrder, label: String, format: Format = Format.DECIMAL, lookupMap: Map<Int, String>? = null,  interpretation: Type? = null): ULong {
        val start = effectiveParsePosition
        val value = readULong(data, size, byteOrder)
        val renderedValue = when(format) {
            Format.DECIMAL -> value.toString()
            Format.HEX -> "0x${value.toString(16).padStart(size*2, '0')}"
            Format.BYTES -> humanReadableByteCount(value)
            Format.LOOKUP -> lookupMap!!.getOrElse(value.toInt()){ "Unknown($value)" }
        }

        val effectiveInterpretation = interpretation ?: Type.getIntType(signed = false, size, byteOrder)

        val fullLabel = "$label: $renderedValue"
        val subresult = BWString(fullLabel, Pair(start, effectiveParsePosition))
        subresult.annotate(decoderName, effectiveInterpretation)

        currentStruct.add(subresult)
        return value
    }
}