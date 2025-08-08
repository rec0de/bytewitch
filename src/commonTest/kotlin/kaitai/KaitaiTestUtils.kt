package kaitai

import decoders.KaitaiElement

object KaitaiTestUtils {

    fun checkElement(
        element: KaitaiElement,
        id: String,
        sourceByteRange: Pair<Int, Int>? = null,
        sourceRangeBitOffset: Pair<Int, Int>? = null,
        htmlInnerContent: String? = null
    ) {
        check(element.id == id) { "Expected id to be '$id', got '${element.id}'" }
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
        if (htmlInnerContent != null) {
            val rendered = element.renderHTML()
            check(rendered.contains(htmlInnerContent)) {
                "Expected HTML content of field '$id' to contain '$htmlInnerContent', got '$rendered'"
            }
        }
    }
}
