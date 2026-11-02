package decoders

import BWAnnotatedData
import BWGenericSequence
import BWString
import ByteWitchResult
import ParseCompanion
import bitmage.ByteOrder
import htmlEscape
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

abstract class QuickDirtyDecoder: ParseCompanion() {
    protected var data: ByteArray = byteArrayOf()
    protected var sourceOffset: Int = 0

    private val structs = mutableListOf<MutableList<ByteWitchResult>>()
    private val structStartOffsets = mutableListOf<Int>()

    private val currentStruct: MutableList<ByteWitchResult>
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
    protected fun start(bytes: ByteArray, sourceOffset: Int) {
        check(isInParse.compareAndSet(expectedValue = false, newValue = true)) { "QuickDirtyParser reentry while parsing"}
        parseOffset = 0
        structs.clear()
        data = bytes
        this.sourceOffset = sourceOffset

        // create root structure
        enterSubstruct()
    }

    @OptIn(ExperimentalAtomicApi::class)
    protected fun finish(): BWGenericSequence {
        isInParse.store(false)
        check(structs.size == 1) { "finish() called with open substruct" }

        return BWGenericSequence(currentStruct, Pair(structStartOffsets.last(), effectiveParsePosition))
    }

    protected fun enterSubstruct() {
        structs.add(mutableListOf())
        structStartOffsets.add(effectiveParsePosition)
    }

    protected fun leaveSubstruct(): BWGenericSequence {
        val subresult = BWGenericSequence(currentStruct, Pair(structStartOffsets.last(), effectiveParsePosition))
        structs.removeAt(structs.lastIndex)
        structStartOffsets.removeAt(structStartOffsets.lastIndex)

        currentStruct.add(subresult)
        return subresult
    }

    protected fun addForeignParsedField(result: ByteWitchResult) {
        currentStruct.add(result)
    }

    // Data reading functions from here on

    protected fun processString(size: Int, label: String): String {
        val start = effectiveParsePosition
        val value = readString(data, size)
        currentStruct.add(BWString("$label ${htmlEscape(value)}".trim(), Pair(start, effectiveParsePosition)))
        return value
    }

    protected fun processBytes(size: Int, label: String): ByteArray {
        val start = effectiveParsePosition
        val value = readBytes(data, size)
        currentStruct.add(BWAnnotatedData(label, value, Pair(start, effectiveParsePosition)))
        return value
    }

    protected fun processBytes(size: Int, content: (bytes: ByteArray) -> String): ByteArray {
        val start = effectiveParsePosition
        val value = readBytes(data, size)
        val rendering = content(value)
        currentStruct.add(BWString(rendering, Pair(start, effectiveParsePosition)))
        return value
    }

    protected fun processInt(size: Int, byteOrder: ByteOrder, label: String, format: Format = Format.DECIMAL, explicitlySigned: Boolean = false, lookupMap: Map<Int, String>? = null): Int {
        val start = effectiveParsePosition
        val value = readInt(data, size, explicitlySigned, byteOrder)
        val renderedValue = when(format) {
            Format.DECIMAL -> value.toString()
            Format.HEX -> "0x${value.toString(16).padStart(size*2, '0')}"
            Format.BYTES -> humanReadableByteCount(value.toULong())
            Format.LOOKUP -> lookupMap!!.getOrElse(value){ "Unknown($value)" }
        }

        val fullLabel = "$label: $renderedValue"
        currentStruct.add(BWString(fullLabel, Pair(start, effectiveParsePosition)))
        return value
    }

    protected fun processUInt(size: Int, byteOrder: ByteOrder, label: String, format: Format = Format.DECIMAL, lookupMap: Map<UInt, String>? = null): UInt {
        val start = effectiveParsePosition
        val value = readUInt(data, size, byteOrder)
        val renderedValue = when(format) {
            Format.DECIMAL -> value.toString()
            Format.HEX -> "0x${value.toString(16).padStart(size*2, '0')}"
            Format.BYTES -> humanReadableByteCount(value.toULong())
            Format.LOOKUP -> lookupMap!!.getOrElse(value){ "Unknown($value)" }
        }

        val fullLabel = "$label: $renderedValue"
        currentStruct.add(BWString(fullLabel, Pair(start, effectiveParsePosition)))
        return value
    }

    protected fun processUInt(size: Int, byteOrder: ByteOrder, renderer: (UInt) -> String): UInt {
        val start = effectiveParsePosition
        val value = readUInt(data, size, byteOrder)
        val renderedValue = renderer(value)
        currentStruct.add(BWString(renderedValue, Pair(start, effectiveParsePosition)))
        return value
    }

    protected fun processULong(size: Int, byteOrder: ByteOrder, label: String, format: Format = Format.DECIMAL, lookupMap: Map<Int, String>? = null): ULong {
        val start = effectiveParsePosition
        val value = readULong(data, size, byteOrder)
        val renderedValue = when(format) {
            Format.DECIMAL -> value.toString()
            Format.HEX -> "0x${value.toString(16).padStart(size*2, '0')}"
            Format.BYTES -> humanReadableByteCount(value)
            Format.LOOKUP -> lookupMap!!.getOrElse(value.toInt()){ "Unknown($value)" }
        }

        val fullLabel = "$label: $renderedValue"
        currentStruct.add(BWString(fullLabel, Pair(start, effectiveParsePosition)))
        return value
    }
}