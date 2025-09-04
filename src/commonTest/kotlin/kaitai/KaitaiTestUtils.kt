package kaitai

import decoders.KaitaiElement
import kotlin.reflect.KClass

object KaitaiTestUtils {

    fun checkElement(
        element: KaitaiElement,
        id: String,
        elementClass: KClass<*>? = null,
        sourceByteRange: Pair<Int, Int>? = null,
        sourceRangeBitOffset: Pair<Int, Int>? = null,
        value: dynamic = null,
        htmlInnerContent: String? = null
    ) {
        check(element.id == id) {
            "Expected id to be '$id', got '${element.id}'"
        }
        if (elementClass != null) {
            check(elementClass.isInstance(element)) {
                "Expected '$id' to be of type ${elementClass.simpleName}, got ${element::class.simpleName}"
            }
        }
        if (sourceByteRange != null) {
            check(element.sourceByteRange == sourceByteRange) {
                "Expected sourceByteRange of field '$id' to be $sourceByteRange, got '${element.sourceByteRange}'"
            }
        }
        if (sourceRangeBitOffset != null) {
            check(element.sourceRangeBitOffset == sourceRangeBitOffset) {
                "Expected sourceRangeBitOffset of field '$id' to be $sourceRangeBitOffset, got '${element.sourceRangeBitOffset}'"
            }
        }
        if (value != null) {
            check(when (element.value) {
                is BooleanArray -> (element.value as BooleanArray).contentEquals(value)
                is List<dynamic> -> (element.value as List<dynamic>).withIndex().all {(i, it) -> it == (value as List<dynamic>)[i]}
                else -> element.value == value}) {
                "Expected value of field '$id' to be exactly '$value', got '${element.value}'"
            }
        }
        if (htmlInnerContent != null) {
            val rendered = element.renderHTML()
            check(rendered.contains(htmlInnerContent)) {
                "Expected HTML content of field '$id' to contain '$htmlInnerContent', got '$rendered'"
            }
        }
    }
}
