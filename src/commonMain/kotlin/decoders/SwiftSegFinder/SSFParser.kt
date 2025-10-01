package decoders.SwiftSegFinder

import bitmage.ByteOrder
import bitmage.fromBytes
import decoders.*
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln

data class GaussianKernel(val sigma: Double, val radius: Int, val weights: DoubleArray)

class SSFParser {
    private val GAUSS_06: GaussianKernel = precomputeGaussianKernel(0.6)

    // this finds all segment boundaries and returns a SSF object that can be called to get the html code
    fun parse(bytes: ByteArray, msgIndex: Int): SSFParsedMessage {
        val segments = findSegmentBoundaries(bytes)
        return SSFParsedMessage(segments, bytes, msgIndex)
    }

    // count how often every segment exists
    fun countSegmentValues(messages: List<SSFParsedMessage>, minSegmentLength: Int = 2): Map<ByteArray, Int> {
        val segmentValueCounts = mutableMapOf<ByteArray, Int>()

        for (msg in messages) {
            for ((index, segment) in msg.segments.withIndex()) {
                val segmentBytes = msg.bytes.sliceArray(SSFUtil.getByteRange(msg, index))

                // filter some segments
                if (segmentBytes.size < minSegmentLength) continue
                if (segmentBytes.all { it == 0.toByte() }) continue

                // check for existing key
                val existingKey = segmentValueCounts.keys.find { it.contentEquals(segmentBytes) }

                if (existingKey != null) {
                    segmentValueCounts[existingKey] = segmentValueCounts.getValue(existingKey) + 1
                } else {
                    segmentValueCounts[segmentBytes.copyOf()] = 1
                }
            }
        }

        return segmentValueCounts
    }

    // Split segments if one segment of another message appears quite often
    fun cropDistinct(messages: List<SSFParsedMessage>): List<SSFParsedMessage> {
        // count how often every segment exists
        val segmentValueCounts = countSegmentValues(messages)

        // only keep segments that occur in more than 10% of the message
        val threshold = (messages.size * 0.1).toInt().coerceAtLeast(1)
        val frequentValues = segmentValueCounts.filterValues { it >= threshold }.keys

        // refine messages
        return messages.map { msg ->
            val newSegments = mutableListOf<SSFSegment>()

            // go through each segment of the message
            var i = 0
            while (i < msg.segments.size) {
                val curr = msg.segments[i]

                // don't split if it's a STRING field
                if (curr.fieldType != SSFField.UNKNOWN) {
                    newSegments.add(curr)
                    i++
                    continue
                }

                val segmentBytes = msg.bytes.sliceArray(SSFUtil.getByteRange(msg, i))

                var splitOffsets = mutableSetOf<Int>()
                splitOffsets.add(0) // segment start

                // compare each segment with every frequent occurring segment
                for (frequent in frequentValues) {
                    if (frequent.size >= segmentBytes.size) continue // skip if not smaller

                    // check if frequent segment is in current segment
                    var searchStart = 0
                    while (searchStart <= segmentBytes.size - frequent.size) { // check if it occurs multiple times
                        val subSegmentBytes = segmentBytes.sliceArray(searchStart until segmentBytes.size)
                        val idx = indexOfSubsequence(subSegmentBytes, frequent) // get index of the subsequenc

                        if (idx != -1) {
                            val absoluteIdx = searchStart + idx
                            splitOffsets.add(absoluteIdx)
                            splitOffsets.add(absoluteIdx + frequent.size)
                            searchStart = absoluteIdx + frequent.size
                        } else {
                            break
                        }
                    }
                }

                splitOffsets.add(segmentBytes.size) // end of the segment

                val sortedOffsets = splitOffsets.toList().sorted().distinct()
                for (j in 0 until sortedOffsets.size - 1) {
                    val relativeOffset = sortedOffsets[j]
                    newSegments.add(SSFSegment(curr.offset + relativeOffset, curr.fieldType))
                }

                i++
            }

            val distinctSorted = newSegments.sortedBy { it.offset }.distinctBy { it.offset }
            msg.copy(segments = distinctSorted)
        }
    }

    // tries to detect length field that determines the rest of the message size in all messages
    fun detectMessageLengthField(messages: List<SSFParsedMessage>): List<SSFParsedMessage> {
        // test 1 byte, 2 byte and 4 bytes length field of both endian
        // start with larger length fields to select largest fitting one
        val candidateConfigs = listOf(
            Pair(4, ByteOrder.BIG),
            Pair(4, ByteOrder.LITTLE),
            Pair(2, ByteOrder.BIG),
            Pair(2, ByteOrder.LITTLE),
            Pair(1, ByteOrder.BIG),
        )

        // check if any configuration exists
        val validConfigs = candidateConfigs.filter { (size, byteOrder) ->
            messages.all { msg ->
                detectLengthFieldInMessage(msg, size, byteOrder) != null
            }
        }
        if (validConfigs.isEmpty()) return messages

        val (chosenSize, chosenByteOrder) = validConfigs.first() // take first configuration

        // create new segment in all messages
        return messages.map { msg ->
            val match = detectLengthFieldInMessage(msg, chosenSize, chosenByteOrder)!!
            val (lengthFieldOffset, lengthValue) = match
            val payloadStart = lengthFieldOffset + chosenSize
            // val payloadEnd = payloadStart + lengthValue


            val newSegments = msg.segments.toMutableList()

            // replace old segment if it already exists
            val replacedSegments = newSegments.filterNot { it.offset == lengthFieldOffset }.toMutableList()
            replacedSegments.add(
                SSFSegment(
                    lengthFieldOffset,
            if (chosenByteOrder == ByteOrder.BIG)
                        SSFField.MESSAGE_LENGTH_BIG_ENDIAN
                    else
                        SSFField.MESSAGE_LENGTH_LITTLE_ENDIAN
                )
            )

            // add field for payload if no border exits yet
            val payloadOffsetExists = replacedSegments.any { it.offset == payloadStart }
            if (!payloadOffsetExists) {
                replacedSegments.add(SSFSegment(payloadStart, SSFField.UNKNOWN))
            }

            val finalSegments = replacedSegments.sortedBy { it.offset }
            msg.copy(segments = finalSegments)
        }
    }

    // find length field in one given message with specific endian and field size
    fun detectLengthFieldInMessage(
        msg: SSFParsedMessage,
        lengthFieldSize: Int,
        byteOrder: ByteOrder
    ): Pair<Int, Int>? {
        val bytes = msg.bytes
        val segments = msg.segments.sortedBy { it.offset }

        for (offset in 0 until bytes.size - lengthFieldSize) {
            val length = SSFUtil.tryParseLength(bytes, offset, lengthFieldSize, byteOrder) ?: continue
            val payloadStart = offset + lengthFieldSize
            val payloadEnd = payloadStart + length

            // payloadEnd must be message size
            if (payloadEnd != bytes.size) continue

            // length field must be of type UNKNOWN or already by a message length field because only one is allowed to exists
            val currentSegment = findSegmentForOffset(segments, offset)
            if (currentSegment?.fieldType != SSFField.UNKNOWN
                && currentSegment?.fieldType != SSFField.MESSAGE_LENGTH_BIG_ENDIAN
                && currentSegment?.fieldType != SSFField.MESSAGE_LENGTH_LITTLE_ENDIAN) continue

            return offset to length
        }

        return null
    }

    // get SSFSegment given an offset
    fun findSegmentForOffset(segments: List<SSFSegment>, offset: Int): SSFSegment? {
        for (i in segments.indices) {
            val start = segments[i].offset
            val end = segments.getOrNull(i + 1)?.offset ?: Int.MAX_VALUE
            if (offset in start until end) return segments[i]
        }
        return null
    }

    // refine segments based on all messages
    fun refineSegmentsAcrossMessages(messages: List<SSFParsedMessage>): List<SSFParsedMessage> {
        // maybe also implement PCA by Stephan Kleber (written in his dissertation), not sure if it's fast enough
        return messages
            .let(::cropDistinct)
            .let(::detectMessageLengthField)
    }


    // check if segment has subsequence and return the corresponding index
    fun indexOfSubsequence(segment: ByteArray, sub: ByteArray): Int {
        if (sub.isEmpty() || segment.size < sub.size) return -1

        for (i in 0..segment.size - sub.size) {
            if (segment.sliceArray(i until i + sub.size).contentEquals(sub)) return i
        }

        return -1
    }

    // calc Gain Ratio GR. GR(Di) means IGR from Di to Di+1. This says how much neighboring bytes belong together
    fun calcGainRatio(messages: List<SSFParsedMessage>, entropy: DoubleArray): DoubleArray {
        val minLength = messages.minOf { it.bytes.size }
        val gr = DoubleArray(minLength) { 0.0 }

        for (i in 0 until minLength - 1) { // don't need to check the last byte
            val pairCounts = mutableMapOf<Pair<Int, Int>, Int>() // how often byte combination [a,b] appear at pos i
            val countsI = mutableMapOf<Int, Int>() // how often byte a appears at position i
            val countsJ = mutableMapOf<Int, Int>() // how often byte b appears at position i

            // go through all messages for counting byte a and byte b
            for (msg in messages) {
                val a = msg.bytes[i].toInt() and 0xFF
                val b = msg.bytes[i + 1].toInt() and 0xFF
                pairCounts[a to b] = pairCounts.getOrPut(a to b) { 0 } + 1
                countsI[a] = countsI.getOrPut(a) { 0 } + 1
                countsJ[b] = countsJ.getOrPut(b) { 0 } + 1
            }

            val total = messages.size.toDouble()

            // Entropy of (i, i+1)
            val hXY = pairCounts.entries.sumOf { (_, count) ->
                val p = count / total
                -p * ln(p)
            }

            // Entropy of i
            val hX = countsI.entries.sumOf { (_, count) ->
                val p = count / total
                -p * ln(p)
            }

            // Entropy of i+1
            val hY = countsJ.entries.sumOf { (_, count) ->
                val p = count / total
                -p * ln(p)
            }

            // calculate igr
            val igr = hY - (hXY - hX)
            gr[i] = if (entropy[i] > 0) igr / entropy[i] else 0.0
        }

        return gr
    }

    // find segmentation boundaries
    private fun findSegmentBoundaries(bytes: ByteArray): List<SSFSegment> {
        val taken = BooleanArray(bytes.size) { false } // list of bytes that have already been assigned

        // pre processing
        val fixedSegments = detectLengthPrefixedFields(bytes, taken)

        // find all bytes without a corresponding segment
        val freeRanges = mutableListOf<Pair<Int, Int>>()
        var currentStart: Int? = null
        for (i in bytes.indices) {
            if (!taken[i]) {
                if (currentStart == null) currentStart = i
            } else {
                if (currentStart != null) {
                    freeRanges.add(currentStart to i)
                    currentStart = null
                }
            }
        }
        if (currentStart != null) {
            freeRanges.add(currentStart to bytes.size)
        }

        val dynamicSegments = mutableListOf<SSFSegment>()
        for ((start, end) in freeRanges) {
            val slice = bytes.sliceArray(start until end)
            val deltaBC = computeDeltaBC(slice)

            // sigma should depend on the field length: Nemesys paper on page 5
            val smoothed = applyGaussianFilter(deltaBC, GAUSS_06)

            // Safety check (it mostly enters if the bytes are too short)
            if (smoothed.isEmpty()) {
                dynamicSegments.add(SSFSegment(start, SSFField.UNKNOWN))
                continue
            }

            // find extrema of smoothedDeltaBC
            val extrema = findExtremaInList(smoothed)

            // find all rising points from minimum to maximum in extrema list
            val rising = findRisingDeltas(extrema)

            // find inflection point in risingDeltas -> those are considered as boundaries
            val inflection = findInflectionPoints(rising, deltaBC)

            // merge consecutive text segments together
            // val boundaries = mergeCharSequences(preBoundaries, bytes)
            val improved = postProcessing(inflection.toMutableList(), slice)

            // add relativeStart to the boundaries
            for ((relativeStart, type) in improved) {
                dynamicSegments.add(SSFSegment(start + relativeStart, type))
            }
        }

        // combine segments together
        return (fixedSegments + dynamicSegments).sortedBy { it.offset }.distinctBy { it.offset }
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

    // pre compute kernel value
    private fun precomputeGaussianKernel(sigma: Double): GaussianKernel {
        val radius = ceil(3 * sigma).toInt()
        val size = 2 * radius + 1 // calc kernel size
        val weights = DoubleArray(size) // kernel as DoubleArray
        var sum = 0.0
        for (i in -radius..radius) {
            weights[i + radius] = exp(-0.5 * (i * i) / (sigma * sigma)) // gaussian weight
            sum += weights[i + radius]
        }

        // normalize kernel
        for (i in weights.indices) weights[i] /= sum

        return GaussianKernel(sigma, radius, weights)
    }

    // apply gaussian filter to smooth deltaBC. So we don't interpret every single change as a field boundary
    private fun applyGaussianFilter(deltaBC: DoubleArray, kernel: GaussianKernel): DoubleArray {
        val r = kernel.radius
        val w = kernel.weights

        // calc smoothed array
        val smoothed = DoubleArray(deltaBC.size)
        for (i in deltaBC.indices) {
            var acc = 0.0
            for (j in -r..r) {
                val idx = i + j
                if (idx in deltaBC.indices) {
                    acc += deltaBC[idx] * w[j + r] // weighted average
                }
            }
            smoothed[i] = acc
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
    fun findInflectionPoints(risingDeltas: List<Pair<Int, Int>>, deltaBC: DoubleArray): MutableList<Int> {
        val boundaries = mutableListOf<Int>()
        // val minDistance = 2

        for ((minIndex, maxIndex) in risingDeltas) {
            var maxDeltaIndex = minIndex + 2
            var maxDeltaValue = 0.0

            for (i in minIndex..< maxIndex) {
                val delta = kotlin.math.abs(deltaBC[i] - deltaBC[i+1])
                if (delta > maxDeltaValue) {
                    maxDeltaValue = delta
                    maxDeltaIndex = i + 2
                }
            }

            // boundaries are not allowed to be right next to each other
            /*if (boundaries.isEmpty() || (maxDeltaIndex - boundaries.last()) >= minDistance) {
                boundaries.add(maxDeltaIndex)
            }*/

            boundaries.add(maxDeltaIndex)
        }

        return boundaries
    }

    // check if complete field consists of printable chars
    fun fieldIsTextSegment(start: Int, end: Int, bytes: ByteArray): Boolean {
        val length = end - start
        if (length <= 0) return false

        var nullByteCount = 0
        for (i in start until end) {
            val byte = bytes[i]
            if (byte == 0x00.toByte()) {
                nullByteCount++
            } else if (!isPrintableChar(byte)) {
                return false
            }
        }

        val nullRatio = nullByteCount.toDouble() / length
        return nullRatio < 0.33 // only accept sequence as text if less than 33% of the sequence are 0x00 bytes
    }

    // check if byte is a printable char
    fun isPrintableChar(byte: Byte): Boolean {
        return (byte == 0x0A.toByte() || byte == 0x0B.toByte() || byte == 0x0D.toByte() || (byte in 0x20..0x7E))
    }

    // merge consecutive fields together if both are printable char values
    fun mergeCharSequences(boundaries: MutableList<Int>, bytes: ByteArray): MutableList<SSFSegment> {
        val mergedSegments = mutableListOf<SSFSegment>()

        // if no boundary detected set start boundary to 0
        if (boundaries.isEmpty()) {
            mergedSegments.add(SSFSegment(0, SSFField.UNKNOWN))
            return mergedSegments
        }

        boundaries.add(0, 0)

        var i = 0

        while (i < boundaries.size) {
            // set start and end of segment
            val start = boundaries[i]
            val end = if (i + 1 < boundaries.size) boundaries[i + 1] else bytes.size

            var fieldType = SSFField.UNKNOWN
            if (fieldIsTextSegment(start, end, bytes)) {
                fieldType = SSFField.STRING

                // merge following segments together if they are also a text segments
                while (i + 1 < boundaries.size) {
                    // set new start and end of segment
                    val nextStart = boundaries[i + 1]
                    val nextEnd = if (i + 2 < boundaries.size) boundaries[i + 2] else bytes.size

                    if (fieldIsTextSegment(nextStart, nextEnd, bytes)) {
                        i++ // skip following segment because we merged it together
                        // fieldType = SSFField.STRING // we have two consecutive text fields interpret it as a string
                    } else {
                        break
                    }
                }
            }

            mergedSegments.add(SSFSegment(start, fieldType))
            i++
        }

        return mergedSegments
    }

    // try to improve boundaries by shifting them a bit
    private fun postProcessing(boundaries: MutableList<Int>, bytes: ByteArray): List<SSFSegment> {
        var result = mergeCharSequences(boundaries, bytes)
        result = slideCharWindow(result, bytes)
        result = nullByteTransitions(result, bytes)
        result = entropyMerge(result, bytes)
        result = splitFixed(result, bytes)

        return result
    }

    // split bytes at the beginning of the message
    fun splitFixed(
        segments: MutableList<SSFSegment>,
        bytes: ByteArray
    ): MutableList<SSFSegment> {
        if (segments.isEmpty()) return segments

        val firstSegment = segments[0]
        val nextOffset = if (segments.size > 1) segments[1].offset else bytes.size
        val firstSegmentLength = nextOffset - firstSegment.offset

        // check if first segment has more than two bytes and every bytes is below 0x10
        if (firstSegmentLength >= 2 &&
            bytes.slice(firstSegment.offset until nextOffset).all { it.toUByte().toInt() < 0x10 }
        ) {
            // split bytes in own segments
            val newSegments = mutableListOf<SSFSegment>()
            for (offset in firstSegment.offset until nextOffset) {
                newSegments.add(SSFSegment(offset, firstSegment.fieldType))
            }

            // add new segments at pos 0
            segments.removeAt(0)
            segments.addAll(0, newSegments)
        }

        return segments
    }

    // H in [0,1] using byte-alphabet (|A|=256)
    fun entropyBytesNormalized(segment: ByteArray): Double {
        if (segment.isEmpty()) return 0.0

        val counts = IntArray(256)
        for (b in segment) counts[b.toInt() and 0xFF]++

        val total = segment.size.toDouble()
        var h = 0.0
        for (c in counts) if (c > 0) {
            val p = c / total
            h -= p * ln(p)
        }

        return h / ln(256.0)
    }

    // compute the byte-wise xor of the first l bytes of two arrays
    fun xorPrefix(a: ByteArray, b: ByteArray, l: Int): ByteArray {
        val len = minOf(l, a.size, b.size)
        val out = ByteArray(len)

        for (i in 0 until len) {
            out[i] = (a[i].toInt() xor b[i].toInt()).toByte()
        }

        return out
    }

    // merge two segments based on their entropy
    fun entropyMerge(
        segments: List<SSFSegment>,
        bytes: ByteArray
    ): MutableList<SSFSegment> {
        val result = mutableListOf<SSFSegment>()

        var index = 0
        while (index < segments.size) {
            // get current segment
            val (start, fieldType) = segments[index]
            val end = if (index + 1 < segments.size) segments[index + 1].offset else bytes.size
            val segA = bytes.sliceArray(start until end)
            // val hA = entropyNibblesNormalized(segA) // nibble entropy of segment A
            val hA = entropyBytesNormalized(segA) // byte entropy of segment A

            if (index + 1 < segments.size) { // check if a following segment exists
                val (nextStart, nextType) = segments[index + 1]
                if (fieldType == nextType) { // check that both field have the same field type
                    val nextEnd = if (index + 2 < segments.size) segments[index + 2].offset else bytes.size
                    val segB = bytes.sliceArray(nextStart until nextEnd)
                    // val hB = entropyNibblesNormalized(segB) // nibble entropy of segment B
                    val hB = entropyBytesNormalized(segB) // byte entropy of segment B

                    val diff = kotlin.math.abs(hA - hB)
                    if (hA > 0.7 && hB > 0.7 && diff < 0.05) {
                        // xor of the start bytes for both segments
                        val xor = xorPrefix(segA, segB, 2)
                        // val hX = entropyNibblesNormalized(xor)
                        val hX = entropyBytesNormalized(xor)

                        if (hX > 0.8) { // in the paper it's set to 0.95 instead of 0.8. Algorithm 3, however, says 0.8
                            // merge segments together
                            result.add(SSFSegment(start, fieldType))
                            index += 2 // skip the following field because we want to merge it to this one
                            continue
                        }
                    }
                }
            }

            // add regular boundary if we didn't merge any segments
            result.add(SSFSegment(start, fieldType))
            index++
        }

        return result
    }

    // shift null bytes to the right field
    fun nullByteTransitions(
        segments: MutableList<SSFSegment>,
        bytes: ByteArray
    ): MutableList<SSFSegment> {
        if (segments.size < 2) return segments.toMutableList()

        val result = mutableListOf<SSFSegment>()
        result.add(segments[0])

        for (i in 1 until segments.size) {
            val (prevStart, prevType) = result.last()
            val (currStart, currType) = segments[i]

            var newSegment: SSFSegment? = SSFSegment(currStart, currType)

            // Rule 1: allocate nullbytes to STRING field
            if (prevType == SSFField.STRING) {
                // count null bytes after the segment
                var extra = 0
                while (currStart + extra < bytes.size && bytes[currStart + extra] == 0.toByte()) {
                    extra++
                }

                // only shift boundary if x0 bytes are less than 2 bytes long
                if (extra in 1..2) {
                    //Logger.log("shifting $extra null bytes into string at position $currStart")
                    val shiftedOffset = currStart + extra
                    val exists = segments.any { it.offset == shiftedOffset }

                    if (!exists) { // only add boundary if it doesn't already exist
                        newSegment = SSFSegment(shiftedOffset, currType)
                    } else {
                        newSegment = null // don't override segment if it already exists
                    }
                }
            }

            // Rule 2: nullbytes before UNKNOWN
            if (newSegment != null && prevType != SSFField.STRING && currType != SSFField.STRING) {
                // count null bytes in front of the segment
                var count = 0
                var idx = currStart - 1
                while (idx >= prevStart && bytes[idx] == 0.toByte()) {
                    count++
                    idx--
                }

                // only shift boundary if x0 bytes are less than 4 bytes long
                if (count in 1..3) {
                    //Logger.log("shifting $count null bytes into unknown at position $currStart")
                    newSegment = SSFSegment(currStart - count, currType)
                }
            }

            if (newSegment != null) {
                result.add(newSegment)
            }
        }

        return result.sortedBy { it.offset }.distinctBy { it.offset }.toMutableList()
    }



    // check if left or right byte of char sequence is also part of it
    fun slideCharWindow(segments: List<SSFSegment>, bytes: ByteArray): MutableList<SSFSegment> {
        val improved = mutableListOf<SSFSegment>()

        var newEnd = 0

        for (i in segments.indices) {
            // get start and end of segment
            var (start, type) = segments[i]
            val end = if (i + 1 < segments.size) segments[i + 1].offset else bytes.size

            // need to check if we already changed the start value in the last round by shifting the boundary
            if (start < newEnd) {
                start = newEnd
            }

            // only shift boundaries if it's a string field
            if (type == SSFField.STRING) {
                var newStart = start
                newEnd = end

                // check left side
                while (newStart > 0 && isPrintableChar(bytes[newStart - 1])) {
                    newStart--
                }

                // check right side
                while (newEnd < bytes.size && isPrintableChar(bytes[newEnd])) {
                    newEnd++
                }

                // remove all boundaries between the new start and end
                improved.removeAll { it.offset in newStart until newEnd }

                improved.add(SSFSegment(newStart, SSFField.STRING))
            } else {
                improved.add(SSFSegment(start, type))
            }
        }

        return improved
    }

    // handle length field payload and add it to the result
    fun handlePrintablePayload(
        bytes: ByteArray, taken: BooleanArray, result: MutableList<SSFSegment>,
        offset: Int, lengthFieldSize: Int, payloadLength: Int, byteOrder: ByteOrder
    ): Int? {
        // check if payload is printable
        val payloadStart = offset + lengthFieldSize
        val payloadEnd = payloadStart + payloadLength
        val payload = bytes.sliceArray(payloadStart until payloadEnd)
        if (!payload.all { isPrintableChar(it) }) return null

        // check if all following three bytes are also printable
        val lookaheadEnd = minOf(payloadEnd + 3, bytes.size)
        if (payloadEnd < lookaheadEnd) { // skip if there are no follow-up bytes
            val lookahead = bytes.sliceArray(payloadEnd until lookaheadEnd)
            if (lookahead.all { isPrintableChar(it) }) {
                return null
            }
        }

        // finally add fields
        if (byteOrder == ByteOrder.BIG) {
            result.add(SSFSegment(offset,SSFField.PAYLOAD_LENGTH_BIG_ENDIAN))
        } else {
            result.add(SSFSegment(offset,SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN))
        }
        result.add(SSFSegment(payloadStart,SSFField.STRING_PAYLOAD))
        for (j in offset until payloadEnd) taken[j] = true

        return payloadEnd
    }

    // used to detect length fields and the corresponding payload
    fun checkLengthPrefixedSegment(
        bytes: ByteArray,
        taken: BooleanArray,
        result: MutableList<SSFSegment>,
        offset: Int,
        lengthFieldSize: Int,
        byteOrder: ByteOrder
    ): Int? {
        if (offset + lengthFieldSize >= bytes.size) return null

        val length = SSFUtil.tryParseLength(bytes, offset, lengthFieldSize, byteOrder) ?: return null

        val payloadStart = offset + lengthFieldSize
        val payloadEnd = payloadStart + length

        // check bounds and collisions
        if (payloadEnd > bytes.size || length < 3) return null
        if ((offset until payloadEnd).any { taken[it] }) return null

        return handlePrintablePayload(bytes, taken, result, offset, lengthFieldSize, length, byteOrder)
    }


    // detect length prefixes and the corresponding payload
    fun detectLengthPrefixedFields(bytes: ByteArray, taken: BooleanArray): List<SSFSegment> {
        val segments = mutableListOf<SSFSegment>()
        var i = 0

        while (i < bytes.size - 1) {
            // Try 2-byte length field (big endian)
            var newIndex = checkLengthPrefixedSegment(bytes, taken, segments, i, 2, ByteOrder.BIG)
            if (newIndex != null) {
                i = newIndex
                continue
            }

            // Try 2-byte length field (little endian)
            newIndex = checkLengthPrefixedSegment(bytes, taken, segments, i, 2, ByteOrder.LITTLE)
            if (newIndex != null) {
                i = newIndex
                continue
            }

            // Try 1-byte length field
            newIndex = checkLengthPrefixedSegment(bytes, taken, segments, i, 1, ByteOrder.BIG)
            if (newIndex != null) {
                i = newIndex
                continue
            }

            i++
        }

        return segments.distinctBy { it.offset }.sortedBy { it.offset }
    }
}

// this object is used as a tool for SwiftSegFinder classes
object SSFUtil {
    // get actual length given the bytes and endian
    fun tryParseLength(
        bytes: ByteArray,
        offset: Int,
        lengthFieldSize: Int,
        byteOrder: ByteOrder
    ): Int? {
        return try {
            when (lengthFieldSize) {
                1 -> bytes[offset].toUByte().toInt()
                2 -> Int.fromBytes(bytes.sliceArray(offset until offset+2), byteOrder, explicitlySigned = false)
                4 -> Int.fromBytes(bytes.sliceArray(offset until offset+4), byteOrder, explicitlySigned = false)
                else -> null
            }
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    // to detect the range of a segment
    fun getByteRange(message: SSFParsedMessage, index: Int): IntRange {
        val start = message.segments[index].offset
        val end = message.segments.getOrNull(index + 1)?.offset ?: message.bytes.size
        return start until end
    }
}
