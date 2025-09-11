package kaitai

import decoders.ByteWitchResult
import decoders.KaitaiElement
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import kotlin.reflect.KClass

object KaitaiTestUtils {

    fun checkElement(
        element: ByteWitchResult,
        id: String,
        elementClass: KClass<*>? = null,
        sourceByteRange: Pair<Int, Int>? = null,
        sourceRangeBitOffset: Pair<Int, Int>? = null,
        value: dynamic = null,
        htmlInnerContent: String? = null
    ) {
        check(element is KaitaiElement) {
            "Expected element to be a KaitaiElement, got ${element::class.simpleName}"
        }
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
            val div = document.createElement("div") as HTMLDivElement
            div.innerHTML = rendered
            val renderedValue = div.querySelector(".kaitai-value")?.innerHTML ?: ""
            console.log(renderedValue)
            check(renderedValue.contains(htmlInnerContent)) {
                "Expected HTML content of field '$id' to contain '$htmlInnerContent', got '$renderedValue'"
            }
        }
    }
}
