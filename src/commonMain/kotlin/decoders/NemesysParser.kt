package decoders

import bitmage.hex
import kotlin.math.ceil
import kotlin.math.exp

class NemesysParser {

    companion object : ByteWitchDecoder {
        override val name = "nemesysparser"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return NemesysParser().parse(data, sourceOffset)
        }

        override fun tryhardDecode(data: ByteArray): ByteWitchResult? {
            return null
        }

        override fun confidence(data: ByteArray) = if(data.size >= 3) 0.76 else 0.00
    }

    // check how similar consecutive bytes are
    fun bitCongruence(b1: Byte, b2: Byte): Double {
        var count = 0
        for (i in 0 until 8) {
            if (((b1.toInt() shr i) and 1) == ((b2.toInt() shr i) and 1)) {
                count++
            }
        }
        return count / 8.0
    }

    // get the delta of the bit congruence. Checkout how much consecutive bytes differ
    fun computeDeltaBC(bytes: ByteArray): DoubleArray {
        val n = bytes.size
        if (n < 3) return DoubleArray(0) // return empty array if it's too short

        val bc = DoubleArray(n - 1)
        for (i in 0 until n - 1) {
            bc[i] = bitCongruence(bytes[i], bytes[i + 1])
        }

        val deltaBC = DoubleArray(n - 2)
        for (i in 1 until n - 1) {
            deltaBC[i - 1] = bc[i] - bc[i - 1]
        }

        return deltaBC
    }

    // apply gaussian filter to smooth deltaBC. So we don't interpret every single change as a field boundary
    private fun applyGaussianFilter(deltaBC: DoubleArray, sigma: Double): DoubleArray {
        val radius = ceil(3 * sigma).toInt()
        val size = 2 * radius + 1 // calc kernel size
        val kernel = DoubleArray(size)  // kernel as DoubleArray
        var sum = 0.0

        // calc kernel and sum of values
        for (i in -radius..radius) {
            kernel[i + radius] = exp(-0.5 * (i * i) / (sigma * sigma))  // gaussian weight
            sum += kernel[i + radius]
        }

        // normalize kernel
        for (i in kernel.indices) {
            kernel[i] /= sum
        }

        // calc smoothed array
        val smoothed = DoubleArray(deltaBC.size)
        for (i in deltaBC.indices) {
            smoothed[i] = 0.0
            for (j in -radius..radius) {
                val idx = i + j
                if (idx >= 0 && idx < deltaBC.size) {
                    smoothed[i] += deltaBC[idx] * kernel[j + radius]  // weighted average
                }
            }
        }

        return smoothed
    }

    // get all local minimum and maximum of smoothed deltaBC
    // return List(Index, extrema) with extrema meaning(-1:minimum, 0:nothing, 1:maximum)
    fun findExtremaInList(smoothedDeltaBC: DoubleArray): List<Pair<Int, Int>> {
        val extrema = mutableListOf<Pair<Int, Int>>()

        // get extrema of first point
        if (smoothedDeltaBC.size > 1) {
            if (smoothedDeltaBC[0] < smoothedDeltaBC[1]) {
                extrema.add(0 to -1) // local minimum
            } else if (smoothedDeltaBC[0] > smoothedDeltaBC[1]) {
                extrema.add(0 to 1) // local maximum
            } else {
                extrema.add(0 to 0) // either minimum nor maximum
            }
        }

        // get extrema of middle points
        for (i in 1 until smoothedDeltaBC.size - 1) {
            if (smoothedDeltaBC[i] < smoothedDeltaBC[i - 1] && smoothedDeltaBC[i] < smoothedDeltaBC[i + 1]) {
                extrema.add(i to -1) // local minimum
            } else if (smoothedDeltaBC[i] > smoothedDeltaBC[i - 1] && smoothedDeltaBC[i] > smoothedDeltaBC[i + 1]) {
                extrema.add(i to 1) // local maximum
            } else {
                extrema.add(i to 0) // either minimum nor maximum
            }
        }

        // get extrema of last points
        if (smoothedDeltaBC.size > 1) {
            val lastIndex = smoothedDeltaBC.size - 1
            if (smoothedDeltaBC[lastIndex] < smoothedDeltaBC[lastIndex - 1]) {
                extrema.add(lastIndex to -1) // local minimum
            } else if (smoothedDeltaBC[lastIndex] > smoothedDeltaBC[lastIndex - 1]) {
                extrema.add(lastIndex to 1) // local maximum
            } else {
                extrema.add(lastIndex to 0) // either minimum nor maximum
            }
        }

        return extrema
    }

    // identify rising edges of minimums to maximums
    // extrema must be in format List(Index, min/max) with min/max meaning(-1:minimum, 0:nothing, 1:maximum)
    fun findRisingDeltas(extrema: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val risingDeltas = mutableListOf<Pair<Int, Int>>()
        var lastMinIndex: Int? = null
        for ((index, extremaType) in extrema) {
            if (extremaType == -1) {
                // save index of last mimimun
                lastMinIndex = index
            } else if (extremaType == 1 && lastMinIndex != null) {
                // saved edge if maximum was found and previous minimum exists
                risingDeltas.add(lastMinIndex to index)
                lastMinIndex = null // reset last minimum
            }
        }
        return risingDeltas
    }

    // find inflection point based on maximum delta in rising deltas
    // it adds +2 on the result to because the segment highlighting counts differently
    fun findInflectionPoints(risingDeltas: List<Pair<Int, Int>>, smoothedDeltaBC: DoubleArray): MutableList<Int> {
        val boundaries = mutableListOf<Int>()

        for ((minIndex, maxIndex) in risingDeltas) {
            var maxDeltaIndex = minIndex + 2
            var maxDeltaValue = 0.0

            for (i in minIndex..< maxIndex) {
                val delta = kotlin.math.abs(smoothedDeltaBC[i] - smoothedDeltaBC[i+1])
                if (delta > maxDeltaValue) {
                    maxDeltaValue = delta
                    maxDeltaIndex = i + 2
                }
            }

            boundaries.add(maxDeltaIndex)
        }

        return boundaries
    }

    // check if complete field consists of printable chars
    fun fieldIsTextSegment(start: Int, end: Int, bytes: ByteArray): Boolean {
        for (j in start until end) {
            if (!isPrintableChar(bytes[j])) {
                return false
            }
        }
        return true
    }

    // check if byte is a printable char
    fun isPrintableChar(byte: Byte): Boolean {
        return (byte == 0x09.toByte() || byte == 0x0A.toByte() || byte == 0x0D.toByte() || (byte in 0x20..0x7E))
    }

    // merge consecutive fields together if both are printable char values
    fun mergeCharSequences(boundaries: MutableList<Int>, bytes: ByteArray): List<Pair<Int, NemesysField>> {
        val mergedBoundaries = mutableListOf<Pair<Int, NemesysField>>()

        // if no boundary detected set start boundary to 0
        if (boundaries.isEmpty()) {
            mergedBoundaries.add(Pair(0, NemesysField.UNKNOWN))
            return mergedBoundaries
        }

        boundaries.add(0, 0)

        var i = 0

        while (i < boundaries.size) {
            // set start and end of segment
            val start = boundaries[i]
            val end = if (i + 1 < boundaries.size) boundaries[i + 1] else bytes.size

            var fieldType = NemesysField.UNKNOWN
            if (fieldIsTextSegment(start, end, bytes)) {
                // merge following segments together if they are also a text segments
                while (i + 1 < boundaries.size) {
                    // set new start and end of segment
                    val nextStart = boundaries[i + 1]
                    val nextEnd = if (i + 2 < boundaries.size) boundaries[i + 2] else bytes.size

                    if (fieldIsTextSegment(nextStart, nextEnd, bytes)) {
                        i++ // skip following segment because we merged it together
                        fieldType = NemesysField.STRING // we have two consecutive text fields interpret it as a string
                    } else {
                        break
                    }
                }
            }

            mergedBoundaries.add(Pair(start, fieldType))
            i++
        }

        return mergedBoundaries
    }

    // find segmentation boundaries
    private fun findSegmentBoundaries(bytes: ByteArray): List<Pair<Int, NemesysField>> {
        val deltaBC = computeDeltaBC(bytes)

        // sigma should depend on the field length: Nemesys paper on page 5
        val smoothedDeltaBC = applyGaussianFilter(deltaBC, 0.6)

        // Safety check (it mostly enters if the bytes are too short)
        if (smoothedDeltaBC.isEmpty()) return listOf<Pair<Int, NemesysField>>()

        // find extrema of smoothedDeltaBC
        val extrema = findExtremaInList(smoothedDeltaBC)

        // find all rising points from minimum to maximum in extrema list
        val risingDeltas = findRisingDeltas(extrema)

        // find inflection point in risingDeltas -> those are considered as boundaries
        val preBoundaries = findInflectionPoints(risingDeltas, smoothedDeltaBC)

        // merge consecutive text segments together
        val boundaries = mergeCharSequences(preBoundaries, bytes)

        return boundaries
    }

    // this finds all segment boundaries and returns a nemesys object that can be called to get the html code
    fun parse(bytes: ByteArray, sourceOffset: Int): NemesysObject {
        val segments = findSegmentBoundaries(bytes)

        return NemesysObject(segments, bytes, Pair(sourceOffset, sourceOffset+bytes.size))
    }
}

enum class NemesysField {
    UNKNOWN, STRING
}

// this object can be used to get useable html code from the nemesys parser
class NemesysObject(val segments: List<Pair<Int, NemesysField>>, val bytes: ByteArray, override val sourceByteRange: Pair<Int, Int>?) : ByteWitchResult {
    // html view of the normal (non-editable) byte sequences
    fun renderPrettyHTML(): String {
        // val updatedSegments = updateFieldType(segments, bytes) // TODO don't think this is needed because we check it later anyway by using NemesysField.UNKOWN
        val updatedSegments = segments

        val sourceOffset = sourceByteRange?.first ?: 0

        val renderedFieldContents = updatedSegments.mapIndexed { index, (start, fieldType) ->
            val end = if (index + 1 < updatedSegments.size) updatedSegments[index + 1].first else bytes.size
            val segmentBytes = bytes.sliceArray(start until end)

            val valueLengthTag = " data-start='${start+sourceOffset}' data-end='${end+sourceOffset}'"

            // differentiate between field types
            when (fieldType) {
                NemesysField.STRING -> "<div class=\"nemesysvalue\" $valueLengthTag>${segmentBytes.hex()} <span>→</span> \"${segmentBytes.decodeToString()}\"</div>"
                // NemesysField.UNKNOWN -> "<div class=\"nemesysvalue\" $valueLengthTag>${segmentBytes.hex()}</div>"
                NemesysField.UNKNOWN -> {
                    val decode = ByteWitch.quickDecode(segmentBytes, start+sourceOffset)

                    // if it's a nemesys object again, just show the hex output
                    if (decode == null || decode is NemesysObject) { // settings changed so it can't be a NemesysObject anymore but it can be null
                        // TODO maybe wrap it as in Protobuf.kt by using <div class="nemesysfield roundbox">
                        "<div class=\"nemesysvalue\" $valueLengthTag>${segmentBytes.hex()}</div>"
                    } else {
                        decode.renderHTML()
                    }
                }
            }
        }

        val content = "<div class=\"nemesysfield roundbox\"><div>${renderedFieldContents.joinToString("")}</div></div>"

        val editButton = "<div class=\"icon icon-edit edit-button\"></div>"

        val renderedContent = "<div class=\"nemesys roundbox\" $byteRangeDataTags>" +
                "<div class=\"view-default\">$editButton$content</div>" +
                "<div class=\"view-editable\" style=\"display:none;\">${renderEditableHTML()}</div>" +
                "</div>"

        return renderedContent
    }

    // html view of editable byte sequences
    private fun renderEditableHTML(): String {
        val renderedFieldContents = mutableListOf<String>()

        for ((index, segment) in segments.withIndex()) {
            val (start, fieldType) = segment
            val end = if (index + 1 < segments.size) segments[index + 1].first else bytes.size
            val segmentBytes = bytes.sliceArray(start until end)

            // create byte-groups of two bytes
            val groupedHex = segmentBytes.hex().chunked(2).joinToString("") {
                "<div class='bytegroup'>$it</div>"
            }

            if (end != bytes.size) {
                renderedFieldContents.add("$groupedHex<div class='field-separator'>|</div>")
            } else {
                renderedFieldContents.add(groupedHex)
            }
        }

        val finishButton = "<div class=\"icon icon-finish finish-button\"></div>"

        val renderedContent = "<div class='nemesysfield roundbox'><div>" +
                "<div class='nemesysvalue' id=\"byteContainer\">" +
                "${renderedFieldContents.joinToString("")}</div></div></div>"

        return "$finishButton$renderedContent"
    }

    override fun renderHTML(): String {
        return renderPrettyHTML()
    }

    // update field type. e.g. check if a byte sequence is utf8 decoded
    private fun updateFieldType(segments: List<Pair<Int, NemesysField>>, bytes: ByteArray): List<Pair<Int, NemesysField>> {
        return segments.mapIndexed { index, (start, fieldType) ->
            if (fieldType == NemesysField.UNKNOWN) {
                // get segmented byte sequence by using the start index and calc the end index with the following start index
                val end = if (index + 1 < segments.size) segments[index + 1].first else bytes.size
                val segmentBytes = bytes.sliceArray(start until end)

                start to checkFieldType(segmentBytes)
            } else {
                start to fieldType
            }
        }
    }

    // get the field type of a byte sequence
    private fun checkFieldType(bytes: ByteArray) : NemesysField{
        return NemesysField.UNKNOWN
    }

    // returns segments of the nemesys object
    fun getSegments(): List<Pair<Int, NemesysField>> {
        return segments
    }
}

// class for sequence alignment of nemesys object
object NemesysSequenceAlignment {

    // main function for sequence alignment
    fun alignSegments(
        floatviewMessages: Map<Int, ByteArray>,
        nemesysSegments: Map<Int, List<Pair<Int, NemesysField>>>
    ): List<AlignedSegment> {
        val alignments = mutableListOf<AlignedSegment>()

        // got through all messages
        for (i in floatviewMessages.keys) {
            for (j in floatviewMessages.keys) {
                if (i >= j) continue // to not compare messages twice

                // extract segments and bytes from list
                val segmentsA = nemesysSegments[i] ?: continue
                val segmentsB = nemesysSegments[j] ?: continue
                val bytesA = floatviewMessages[i] ?: continue
                val bytesB = floatviewMessages[j] ?: continue

                // compare all segments form A with all segments of B
                for (segmentAIndex in segmentsA.indices) {
                    // extract bytes from segmentA
                    val (startA, _) = segmentsA[segmentAIndex]
                    val endA = if (segmentAIndex + 1 < segmentsA.size) segmentsA[segmentAIndex + 1].first else bytesA.size
                    val segmentBytesA = bytesA.sliceArray(startA until endA)




                    // only save best match for segmentA
                    var bestMatch: AlignedSegment? = null

                    for (segmentBIndex in segmentsB.indices) {
                        // extract bytes from segmentB
                        val (startB, _) = segmentsB[segmentBIndex]
                        val endB = if (segmentBIndex + 1 < segmentsB.size) segmentsB[segmentBIndex + 1].first else bytesB.size
                        val segmentBytesB = bytesB.sliceArray(startB until endB)

                        val dmValue = calculateDissimilarity(segmentBytesA, segmentBytesB, startA, startB)
                        val current = AlignedSegment(i, j, segmentAIndex, segmentBIndex, dmValue)

                        // check if current match is better than the best match
                        if (bestMatch == null || dmValue < bestMatch.dissimilarity) {
                            bestMatch = current
                        }
                    }

                    // save best match for segmentA
                    bestMatch?.let { alignments.add(it) }
                }
            }
        }

        return alignments.sortedBy { it.dissimilarity } // sort by dissimilarity (smallest value first)
    }


    // calculate dissimilarity between two segments
    private fun calculateDissimilarity(segmentA: ByteArray, segmentB: ByteArray, startA: Int, startB: Int): Double {
        // check if segments have the same size
        val baseDm = if (segmentA.size == segmentB.size) {
            canberraDistance(segmentA, segmentB) / segmentA.size
        } else {
            // need sliding window approach (Canberra-Ulm Dissimilarity) if segments have different sizes
            canberraUlmDissimilarity(segmentA, segmentB)
        }

        // advanced position penalty
        val positionPenalty = computePositionPenalty(startA, startB, segmentA.size, segmentB.size)

        return baseDm + positionPenalty
    }

    // Canberra Distance for segments of the same size
    private fun canberraDistance(segmentA: ByteArray, segmentB: ByteArray): Double {
        var sum = 0.0
        for (i in segmentA.indices) {
            val ai = segmentA[i].toInt() and 0xFF
            val bi = segmentB[i].toInt() and 0xFF
            val denominator = ai + bi
            if (denominator != 0) {
                sum += kotlin.math.abs(ai - bi).toDouble() / denominator
            }
        }
        return sum
    }

    // Canberra-Ulm Dissimilarity for segments of different sizes (sliding window approach)
    private fun canberraUlmDissimilarity(segmentS: ByteArray, segmentT: ByteArray): Double {
        val shortSegment = if (segmentS.size <= segmentT.size) segmentS else segmentT
        val longSegment = if (segmentS.size > segmentT.size) segmentS else segmentT

        var minD = Double.MAX_VALUE

        // sliding window to search for the lowest dissimilarity
        for (offset in 0..(longSegment.size - shortSegment.size)) {
            val window = longSegment.sliceArray(offset until (offset + shortSegment.size))
            val dC = canberraDistance(shortSegment, window) / shortSegment.size
            if (dC < minD) {
                minD = dC
            }
        }

        val r = (longSegment.size - shortSegment.size).toDouble() / longSegment.size
        val pf = 1.0 // hyper parameter to set the non-linear penalty

        val dm = (shortSegment.size.toDouble() / longSegment.size.toDouble()) * minD +
                r +
                (1 - minD) * r * (shortSegment.size / (longSegment.size * longSegment.size) - pf)

        return dm
    }

    // position penalty for absolute and relative difference
    private fun computePositionPenalty(startA: Int, startB: Int, lenA: Int, lenB: Int): Double {
        val alpha = 0.02 // hyper parameter for absolute difference
        val beta = 0.03  // hyper parameter for relative difference

        // calc penalty for absolute difference
        val maxLen = maxOf(lenA, lenB).toDouble()
        val positionDiffAbs = kotlin.math.abs(startA - startB).toDouble()
        val penaltyAbs = positionDiffAbs / maxLen

        // calc penalty for relative difference
        val relativePosA = startA.toDouble() / lenA
        val relativePosB = startB.toDouble() / lenB
        val penaltyRel = kotlin.math.abs(relativePosA - relativePosB)

        return alpha * penaltyAbs + beta * penaltyRel
    }

}

// aligned segment for Nemesys
data class AlignedSegment(
    val protocolA: Int,
    val protocolB: Int,
    val segmentIndexA: Int,
    val segmentIndexB: Int,
    val dissimilarity: Double
)
