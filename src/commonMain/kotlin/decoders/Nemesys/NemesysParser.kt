package decoders.Nemesys

import decoders.*
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

class NemesysParser {
    // this finds all segment boundaries and returns a nemesys object that can be called to get the html code
    fun parse(bytes: ByteArray, msgIndex: Int): NemesysParsedMessage {
        val segments = findSegmentBoundaries(bytes)
        return NemesysParsedMessage(segments, bytes, msgIndex)
    }

    fun parseEntropy(messages: List<NemesysParsedMessage>): List<NemesysParsedMessage> {
        val boundaries = findEntropyBoundaries(messages)

        return boundaries
    }

    // parse bytewise and see every byte as one field without using Nemesys and without using Postprocessing
    fun parseBytewiseWithoutOptimization(bytes: ByteArray, msgIndex: Int): NemesysParsedMessage {
        val segments = setBytewiseSegmentBoundaries(bytes)

        return NemesysParsedMessage(segments, bytes, msgIndex)
    }

    // parse bytewise and see every byte as one field without using Nemesys
    fun parseBytewiseWithOptimization(bytes: ByteArray, msgIndex: Int): NemesysParsedMessage {
        val segments = setBytewiseSegmentBoundariesWithOptimization(bytes)

        return NemesysParsedMessage(segments, bytes, msgIndex)
    }

    // created my own byte wise parser using a sliding window
    fun parseSmartWithoutOptimization(bytes: ByteArray, msgIndex: Int): NemesysParsedMessage {
        val segments = mutableListOf<NemesysSegment>()
        var index = 0

        val decoders = listOf(Utf8Decoder, Utf16Decoder, IEEE754, GenericTLV) // try these decoders
        val minWindow = 4
        val maxWindow = 32
        val threshold = 0.8 // set confidence score

        while (index < bytes.size) {
            var bestConfidence = 0.0
            var bestDecoder: ByteWitchDecoder? = null
            var bestWindowSize = -1

            // sliding window approach
            for (windowSize in maxWindow downTo minWindow) {
                val end = index + windowSize
                if (end > bytes.size) break
                val window = bytes.sliceArray(index until end)

                for (decoder in decoders) {
                    val confidence = decoder.confidence(window)
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence
                        bestDecoder = decoder
                        bestWindowSize = windowSize
                    }
                }
            }

            // set field boundary based on the best decoder
            if (bestDecoder != null && bestConfidence >= threshold) {
                segments.add(NemesysSegment(index, NemesysUtil.setNemesysFieldfromDecoder(bestDecoder)))
                index += bestWindowSize
            } else {
                // Fallback: bytewise segmentation if no decoder could be found
                segments.add(NemesysSegment(index, NemesysField.UNKNOWN))
                index += 1
            }
        }

        return NemesysParsedMessage(segments, bytes, msgIndex)
    }


    // created my own byte wise parser using a sliding window - doesn't include pre processing
    fun parseSmartWithHalfOptimization(bytes: ByteArray, msgIndex: Int): NemesysParsedMessage {
        val segments = mutableListOf<NemesysSegment>()
        var index = 0

        val decoders = listOf(Utf8Decoder, Utf16Decoder, IEEE754, GenericTLV) // try these decoders
        val minWindow = 4
        val maxWindow = 32
        val threshold = 0.8 // set confidence score

        while (index < bytes.size) {
            var bestConfidence = 0.0
            var bestDecoder: ByteWitchDecoder? = null
            var bestWindowSize = -1

            for (windowSize in maxWindow downTo minWindow) {
                val end = index + windowSize
                if (end > bytes.size) break
                val window = bytes.sliceArray(index until end)

                for (decoder in decoders) {
                    val confidence = decoder.confidence(window)
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence
                        bestDecoder = decoder
                        bestWindowSize = windowSize
                    }
                }
            }

            // set field boundary based on the best decoder
            if (bestDecoder != null && bestConfidence >= threshold) {
                val fieldType = NemesysUtil.setNemesysFieldfromDecoder(bestDecoder)
                segments.add(NemesysSegment(index, fieldType))
                index += bestWindowSize
            } else {
                // Fallback: bytewise segmentation if no decoder could be found
                segments.add(NemesysSegment(index, NemesysField.UNKNOWN))
                index += 1
            }
        }

        // transform NemesysSegment boundaries to a list
        val boundaries = segments.map { it.offset }.toMutableList()
        val improvedSegments = postProcessing(boundaries, bytes) // do post processing

        return NemesysParsedMessage(improvedSegments, bytes, msgIndex)
    }

    fun parseSmartWithFullOptimization(bytes: ByteArray, msgIndex: Int): NemesysParsedMessage {
        val decoders = listOf(Utf8Decoder, Utf16Decoder, IEEE754, GenericTLV)
        val minWindow = 4
        val maxWindow = 32
        val threshold = 0.8

        val taken = BooleanArray(bytes.size) { false }

        // pre processing
        val fixedSegments = detectLengthPrefixedFields(bytes, taken).toMutableList()

        // find free byt ranges
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

        // iterate over free bytes
        val smartSegments = mutableListOf<NemesysSegment>()
        for ((start, end) in freeRanges) {
            var index = start
            while (index < end) {
                var bestConfidence = 0.0
                var bestDecoder: ByteWitchDecoder? = null
                var bestWindowSize = -1

                // use sliding windows appraoch
                for (windowSize in maxWindow downTo minWindow) {
                    val actualEnd = index + windowSize
                    if (actualEnd > end) break
                    val window = bytes.sliceArray(index until actualEnd)

                    for (decoder in decoders) {
                        val confidence = decoder.confidence(window)
                        if (confidence > bestConfidence) {
                            bestConfidence = confidence
                            bestDecoder = decoder
                            bestWindowSize = windowSize
                        }
                    }
                }

                // set boundaries based on the best decoder
                if (bestDecoder != null && bestConfidence >= threshold) {
                    smartSegments.add(
                        NemesysSegment(
                            index,
                            NemesysUtil.setNemesysFieldfromDecoder(bestDecoder)
                        )
                    )
                    for (i in index until index + bestWindowSize) taken[i] = true
                    index += bestWindowSize
                } else {
                    smartSegments.add(NemesysSegment(index, NemesysField.UNKNOWN))
                    taken[index] = true
                    index += 1
                }
            }
        }

        // for post processing
        val combinedSegments = (fixedSegments + smartSegments).sortedBy { it.offset }.distinctBy { it.offset }
        val boundaries = combinedSegments.map { it.offset }.toMutableList()
        val improvedSegments = postProcessing(boundaries, bytes)

        return NemesysParsedMessage(improvedSegments, bytes, msgIndex)
    }



    // create a random parser - just for comparison reasons
    fun parseRandom(bytes: ByteArray, msgIndex: Int): NemesysParsedMessage {
        val segments = mutableListOf<NemesysSegment>()

        // set seed for randomizer
        val seed = bytes.contentHashCode()
        val random = Random(seed)

        var index = 0
        while (index < bytes.size) {
            segments.add(NemesysSegment(index, NemesysField.UNKNOWN))

            // choose a random field length betwwen 1 and 8 bytes
            val fieldLength = 1 + random.nextInt(8)
            index += fieldLength
        }

        return NemesysParsedMessage(segments, bytes, msgIndex)
    }


    // every byte is one field. don't use postprocessing
    private fun setBytewiseSegmentBoundaries(bytes: ByteArray): List<NemesysSegment>{
        // val taken = BooleanArray(bytes.size) { false }

        // preProcessing to detect length fields
        // val fixedSegments = detectLengthPrefixedFields(bytes, taken)

        val dynamicSegments = mutableListOf<NemesysSegment>()
        for (i in bytes.indices) { // go through each byte
            /* if (!taken[i]) {
                val slice = byteArrayOf(bytes[i])
                // do some post processing to merge bytes together
                val type = postProcessing(mutableListOf(0), slice).firstOrNull()?.fieldType ?: NemesysField.UNKNOWN
                dynamicSegments.add(NemesysSegment(i, type))
            } */
            dynamicSegments.add(NemesysSegment(i, NemesysField.UNKNOWN))
        }

        // return (fixedSegments + dynamicSegments).sortedBy { it.offset }.distinctBy { it.offset }
        return (dynamicSegments).sortedBy { it.offset }.distinctBy { it.offset }
    }

    // every byte is one field. only use pre- and postprocessing to merge bytes together
    private fun setBytewiseSegmentBoundariesWithOptimization(bytes: ByteArray): List<NemesysSegment>{
        val taken = BooleanArray(bytes.size) { false }

        // preProcessing to detect length fields
        val fixedSegments = detectLengthPrefixedFields(bytes, taken)

        // find free ranges
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

        // set field boundaries after every byte
        val dynamicSegments = mutableListOf<NemesysSegment>()
        for ((start, end) in freeRanges) {
            val boundaries = (start..end).toMutableList()
            val segments = postProcessing(boundaries, bytes)
            dynamicSegments.addAll(segments)
        }

        return (fixedSegments + dynamicSegments).sortedBy { it.offset }.distinctBy { it.offset }
    }


    // count how often every segment exists
    fun countSegmentValues(messages: List<NemesysParsedMessage>, minSegmentLength: Int = 2): Map<ByteArray, Int> {
        val segmentValueCounts = mutableMapOf<ByteArray, Int>()

        for (msg in messages) {
            for ((index, segment) in msg.segments.withIndex()) {
                val segmentBytes = msg.bytes.sliceArray(NemesysUtil.getByteRange(msg, index))

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
    fun cropDistinct(messages: List<NemesysParsedMessage>): List<NemesysParsedMessage> {
        // count how often every segment exists
        val segmentValueCounts = countSegmentValues(messages)

        // only keep segments that occur in more than 10% of the message
        val threshold = (messages.size * 0.1).toInt().coerceAtLeast(1)
        val frequentValues = segmentValueCounts.filterValues { it >= threshold }.keys

        // refine messages
        return messages.map { msg ->
            val newSegments = mutableListOf<NemesysSegment>()

            // go through each segment of the message
            var i = 0
            while (i < msg.segments.size) {
                val curr = msg.segments[i]

                // don't split if it's a STRING field
                if (curr.fieldType == NemesysField.STRING) {
                    newSegments.add(curr)
                    i++
                    continue
                }

                val segmentBytes = msg.bytes.sliceArray(NemesysUtil.getByteRange(msg, i))

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
                    newSegments.add(NemesysSegment(curr.offset + relativeOffset, curr.fieldType))
                }

                i++
            }

            val distinctSorted = newSegments.sortedBy { it.offset }.distinctBy { it.offset }
            msg.copy(segments = distinctSorted)
        }
    }

    // tries to detect length field that determines the rest of the message size in all messages
    fun detectMessageLengthField(messages: List<NemesysParsedMessage>): List<NemesysParsedMessage> {
        // test 1 byte, 2 byte and 4 bytes length field of both endian
        val candidateConfigs = listOf(
            Pair(1, true),
            Pair(2, true),
            Pair(2, false),
            Pair(4, true),
            Pair(4, false)
        )

        // check if any configuration exists
        val validConfigs = candidateConfigs.filter { (size, isBigEndian) ->
            messages.all { msg ->
                detectLengthFieldInMessage(msg, size, isBigEndian) != null
            }
        }
        if (validConfigs.isEmpty()) return messages

        val (chosenSize, chosenEndian) = validConfigs.first() // take first configuration

        // create new segment in all messages
        return messages.map { msg ->
            val match = detectLengthFieldInMessage(msg, chosenSize, chosenEndian)!!
            val (lengthFieldOffset, lengthValue) = match
            val payloadStart = lengthFieldOffset + chosenSize
            // val payloadEnd = payloadStart + lengthValue


            val newSegments = msg.segments.toMutableList()

            // replace old segment if it already exists
            val replacedSegments = newSegments.filterNot { it.offset == lengthFieldOffset }.toMutableList()
            replacedSegments.add(
                NemesysSegment(
                    lengthFieldOffset,
                    if (chosenEndian)
                        NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN
                    else
                        NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN
                )
            )

            // add field for payload if no border exits yet
            val payloadOffsetExists = replacedSegments.any { it.offset == payloadStart }
            if (!payloadOffsetExists) {
                replacedSegments.add(NemesysSegment(payloadStart, NemesysField.UNKNOWN))
            }

            val finalSegments = replacedSegments.sortedBy { it.offset }
            msg.copy(segments = finalSegments)

            /*val newSegments = msg.segments.toMutableList()
            newSegments.add(NemesysSegment(lengthFieldOffset,
                if (chosenEndian) NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN else NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN))
            newSegments.add(NemesysSegment(payloadStart, NemesysField.UNKNOWN))

            msg.copy(segments = newSegments.sortedBy { it.offset }.distinctBy { it.offset })*/
        }
    }

    // find length field in one given message with specific endian and field size
    fun detectLengthFieldInMessage(
        msg: NemesysParsedMessage,
        lengthFieldSize: Int,
        bigEndian: Boolean
    ): Pair<Int, Int>? {
        val bytes = msg.bytes
        val segments = msg.segments.sortedBy { it.offset }

        for (offset in 0 until bytes.size - lengthFieldSize) {
            val length = NemesysUtil.tryParseLength(bytes, offset, lengthFieldSize, bigEndian) ?: continue
            val payloadStart = offset + lengthFieldSize
            val payloadEnd = payloadStart + length

            // payloadEnd must be message size
            if (payloadEnd != bytes.size) continue

            // length field must be of type UNKNOWN
            val currentSegment = findSegmentForOffset(segments, offset)
            if (currentSegment?.fieldType != NemesysField.UNKNOWN) continue

            return offset to length
        }

        return null
    }

    // get NemesysSegment given an offset
    fun findSegmentForOffset(segments: List<NemesysSegment>, offset: Int): NemesysSegment? {
        for (i in segments.indices) {
            val start = segments[i].offset
            val end = segments.getOrNull(i + 1)?.offset ?: Int.MAX_VALUE
            if (offset in start until end) return segments[i]
        }
        return null
    }

    // refine segments based on all messages
    fun refineSegmentsAcrossMessages(messages: List<NemesysParsedMessage>): List<NemesysParsedMessage> {
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

    // calculate information entropy H(Di) for every position
    fun calcBytewiseEntropy(messages: List<NemesysParsedMessage>): DoubleArray {
        val minLength = messages.minOf { it.bytes.size }
        val entropy = DoubleArray(minLength) { 0.0 }

        // to through all bytes
        for (i in 0 until minLength) {
            val counts = mutableMapOf<Int, Int>()
            for (msg in messages) {
                val byte = msg.bytes[i].toInt() and 0xFF
                counts[byte] = counts.getOrPut(byte) { 0 } + 1
            }

            val total = messages.size.toDouble()
            entropy[i] = counts.entries.sumOf { (v, c) ->
                val p = c / total
                -p * ln(p)// TODO ln or log2?
            }
        }

        return entropy
    }

    // calc Gain Ratio GR. GR(Di) means IGR from Di to Di+1. This says how much neighboring bytes belong together
    fun calcGainRatio(messages: List<NemesysParsedMessage>, entropy: DoubleArray): DoubleArray {
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

    private fun getHalfByte(bytes: ByteArray, halfByteIndex: Int): Int {
        val byteIndex = halfByteIndex / 2
        val isHigh = halfByteIndex % 2 == 0
        val byte = bytes[byteIndex].toInt() and 0xFF
        return if (isHigh) (byte shr 4) and 0x0F else byte and 0x0F
    }

    private fun calcHalfByteGainRatio(messages: List<NemesysParsedMessage>, entropy: DoubleArray): DoubleArray {
        val minHalfBytes = messages.minOf { it.bytes.size } * 2
        val gr = DoubleArray(minHalfBytes) { 0.0 }

        for (i in 0 until minHalfBytes - 1) {
            val pairCounts = mutableMapOf<Pair<Int, Int>, Int>()
            val countsI = mutableMapOf<Int, Int>()
            val countsJ = mutableMapOf<Int, Int>()

            for (msg in messages) {
                val byteArray = msg.bytes
                val nibbleA = getHalfByte(byteArray, i)
                val nibbleB = getHalfByte(byteArray, i + 1)

                pairCounts[nibbleA to nibbleB] = pairCounts.getOrPut(nibbleA to nibbleB) { 0 } + 1
                countsI[nibbleA] = countsI.getOrPut(nibbleA) { 0 } + 1
                countsJ[nibbleB] = countsJ.getOrPut(nibbleB) { 0 } + 1
            }

            val total = messages.size.toDouble()

            // H(Di, Di+1)
            val hXY = pairCounts.values.sumOf { count ->
                val p = count / total
                -p * ln(p)
            }

            // H(Di)
            val hX = countsI.values.sumOf { count ->
                val p = count / total
                -p * ln(p)
            }

            // H(Di+1)
            val hY = countsJ.values.sumOf { count ->
                val p = count / total
                -p * ln(p)
            }

            val igr = hY - (hXY - hX)
            gr[i] = if (entropy[i] > 0) igr / entropy[i] else 0.0
        }

        return gr
    }


    // set boundaries using Entropy and Gain Ratio
    fun getBoundariesUsingEntropy(messages: List<NemesysParsedMessage>, entropy: DoubleArray, gr: DoubleArray, threshold: Double): Set<Int> {
        val minLength = messages.minOf { it.bytes.size }
        val boundaries = mutableSetOf<Int>()

        for (i in 1 until minLength - 1) {
            // Rule 1: local maximum in entropy
            if (entropy[i] >= entropy[i - 1] && entropy[i] > entropy[i + 1]) {
                boundaries.add(i)
            }

            // Rule 3: local minimum in GR
            if (entropy[i] > 0 && gr[i] < gr[i - 1] && gr[i] < gr[i + 1]) {
                boundaries.add(i)
            }

            // Rule 4: GR < threshold
            if (entropy[i] > 0 && gr[i] < threshold) {
                boundaries.add(i)
            }
        }

        // Rule 2: Entropy changes from 0 to > 0. Detect boundaries like this: 34 AC | 00 00 00 A5
        for (i in 1 until minLength) {
            if (entropy[i - 1] == 0.0 && entropy[i] > 0.0) { // past entropy was 0 and now it changed to something higher
                val o = (i - 1 downTo 0).takeWhile { entropy[it] == 0.0 }.count() // check how many previous bytes with entropy 0 exist
                val delta = (i - o) % 4 // TODO not sure if that's correct
                boundaries.add(delta)
            }
        }

        return boundaries.sorted().toSet()
    }


    // set boundaries using Entropy and Gain Ratio
    private fun getBoundariesUsingHalbByteEntropy(messages: List<NemesysParsedMessage>, entropy: DoubleArray, gr: DoubleArray, threshold: Double): Set<Int> {
        val minHalfBytes = messages.minOf { it.bytes.size } * 2

        val boundaries = mutableSetOf<Int>()

        for (i in 1 until minHalfBytes - 1) {
            // Rule 1: local maximum in entropy
            if (entropy[i] >= entropy[i - 1] && entropy[i] > entropy[i + 1]) {
                boundaries.add(i)
            }

            // Rule 3: local minimum in GR
            if (entropy[i] > 0 && gr[i] < gr[i - 1] && gr[i] < gr[i + 1]) {
                boundaries.add(i)
            }

            // Rule 4: GR < threshold
            if (entropy[i] > 0 && gr[i] < threshold) {
                boundaries.add(i)
            }
        }


        // Rule 2: Entropy changes from 0 to > 0. Detect boundaries like this: 34 AC | 00 00 00 A5
        for (i in 1 until minHalfBytes) {
            if (entropy[i - 1] == 0.0 && entropy[i] > 0.0) {
                val o = (i - 1 downTo 0).takeWhile { entropy[it] == 0.0 }.count()
                val delta = (i - o) % 4
                boundaries.add(i - delta)
            }
        }

        return boundaries.sorted().toSet()
    }

    fun calcHalfByteEntropy(messages: List<NemesysParsedMessage>): DoubleArray {
        val minLength = messages.minOf { it.bytes.size }
        val entropy = DoubleArray(minLength * 2) { 0.0 } // 2 half-bytes per byte

        for (i in 0 until minLength) {
            val countsHigh = mutableMapOf<Int, Int>()
            val countsLow = mutableMapOf<Int, Int>()

            for (msg in messages) {
                val byte = msg.bytes[i].toInt() and 0xFF
                val highNibble = (byte shr 4) and 0x0F
                val lowNibble = byte and 0x0F

                countsHigh[highNibble] = countsHigh.getOrPut(highNibble) { 0 } + 1
                countsLow[lowNibble] = countsLow.getOrPut(lowNibble) { 0 } + 1
            }

            val total = messages.size.toDouble()

            entropy[i * 2] = countsHigh.entries.sumOf { (_, c) ->
                val p = c / total
                -p * ln(p)
            }

            entropy[i * 2 + 1] = countsLow.entries.sumOf { (_, c) ->
                val p = c / total
                -p * ln(p)
            }
        }

        return entropy
    }



    // entropy decoder for multiple messages
    fun findEntropyBoundaries(messages: List<NemesysParsedMessage>): List<NemesysParsedMessage> {
        if (messages.isEmpty()) return emptyList()

        // TODO calculation of entropy and gr could be made in one step. This saves performance but is harder to read.
        // get information entropy H(Di) for every byte position
        // val entropy = calcBytewiseEntropy(messages)
        val entropy = calcHalfByteEntropy(messages)

        // get Gain Ratio (gr)
        // val gr = calcGainRatio(messages, entropy)
        val gr = calcHalfByteGainRatio(messages, entropy)

        // get boundaries based on rules
        // val globalBoundaries = getBoundariesUsingEntropy(messages, entropy, gr, 0.01)
        val globalBoundaries = getBoundariesUsingHalbByteEntropy(messages, entropy, gr, 0.01)

        // postprocessing and return in right format
        return messages.map { message ->
            // Post Processing to improve local segmentation
            val localOffsets = globalBoundaries.filter { it < message.bytes.size }.toMutableList()

            val segments = postProcessing(localOffsets, message.bytes).toMutableList()
            // if want to be used without postProcessing (performs worse)
            /*val segments = localOffsets.map { offset ->
                NemesysSegment(offset = offset, fieldType = NemesysField.UNKNOWN)
            }*/


            /*val deltaBC = computeDeltaBC(message.bytes)

            // sigma should depend on the field length: Nemesys paper on page 5
            val smoothed = applyGaussianFilter(deltaBC, 0.6)

            // Safety check (it mostly enters if the bytes are too short)
            /*if (smoothed.isEmpty()) { // TODO ???
                segments.add(NemesysSegment(0, NemesysField.UNKNOWN))
                continue
            }*/

            // find extrema of smoothedDeltaBC
            val extrema = findExtremaInList(smoothed)

            // find all rising points from minimum to maximum in extrema list
            val rising = findRisingDeltas(extrema)

            // find inflection point in risingDeltas -> those are considered as boundaries
            val inflection = findInflectionPoints(rising, deltaBC)

            // merge consecutive text segments together
            // val boundaries = mergeCharSequences(preBoundaries, bytes)
            val improved = postProcessing(inflection.toMutableList(), message.bytes)

            // add relativeStart to the boundaries
            for ((relativeStart, type) in improved) {
                segments.add(NemesysSegment(relativeStart, type))
            }*/



            NemesysParsedMessage(segments, message.bytes, message.msgIndex)
        }
    }


    // find segmentation boundaries
    private fun findSegmentBoundaries(bytes: ByteArray): List<NemesysSegment> {
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

        val dynamicSegments = mutableListOf<NemesysSegment>()
        for ((start, end) in freeRanges) {
            val slice = bytes.sliceArray(start until end)
            val deltaBC = computeDeltaBC(slice)

            // sigma should depend on the field length: Nemesys paper on page 5
            val smoothed = applyGaussianFilter(deltaBC, 0.6)

            // Safety check (it mostly enters if the bytes are too short)
            if (smoothed.isEmpty()) {
                dynamicSegments.add(NemesysSegment(start, NemesysField.UNKNOWN))
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
                dynamicSegments.add(NemesysSegment(start + relativeStart, type))
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
    /*fun fieldIsTextSegment(start: Int, end: Int, bytes: ByteArray): Boolean {
        for (j in start until end) {
            if (!isPrintableChar(bytes[j])) {
                return false
            }
        }
        return true
    }*/

    // check if byte is a printable char
    fun isPrintableChar(byte: Byte): Boolean {
        return (byte == 0x0A.toByte() || byte == 0x0B.toByte() || byte == 0x0D.toByte() || (byte in 0x20..0x7E))
    }

    // merge consecutive fields together if both are printable char values
    fun mergeCharSequences(boundaries: MutableList<Int>, bytes: ByteArray): MutableList<NemesysSegment> {
        val mergedSegments = mutableListOf<NemesysSegment>()

        // if no boundary detected set start boundary to 0
        if (boundaries.isEmpty()) {
            mergedSegments.add(NemesysSegment(0, NemesysField.UNKNOWN))
            return mergedSegments
        }

        boundaries.add(0, 0)

        var i = 0

        while (i < boundaries.size) {
            // set start and end of segment
            val start = boundaries[i]
            val end = if (i + 1 < boundaries.size) boundaries[i + 1] else bytes.size

            var fieldType = NemesysField.UNKNOWN
            if (fieldIsTextSegment(start, end, bytes)) {
                fieldType = NemesysField.STRING

                // merge following segments together if they are also a text segments
                while (i + 1 < boundaries.size) {
                    // set new start and end of segment
                    val nextStart = boundaries[i + 1]
                    val nextEnd = if (i + 2 < boundaries.size) boundaries[i + 2] else bytes.size

                    if (fieldIsTextSegment(nextStart, nextEnd, bytes)) {
                        i++ // skip following segment because we merged it together
                        // fieldType = NemesysField.STRING // we have two consecutive text fields interpret it as a string
                    } else {
                        break
                    }
                }
            }

            mergedSegments.add(NemesysSegment(start, fieldType))
            i++
        }

        return mergedSegments
    }

    // merge char sequences together - this is the way how it's done by Stephan Kleber in his paper (explained in 10.4.4.2)
    private fun mergeCharSequences2(boundaries: MutableList<Int>, bytes: ByteArray): MutableList<NemesysSegment> {
        val mergedBoundaries = mutableListOf<NemesysSegment>()

        // if no boundary detected set start boundary to 0
        if (boundaries.isEmpty()) {
            mergedBoundaries.add(NemesysSegment(0, NemesysField.UNKNOWN))
            return mergedBoundaries
        }

        boundaries.add(0, 0)
        var i = 0

        while (i < boundaries.size) {
            // set start and end of segment
            val start = boundaries[i]
            var end = if (i + 1 < boundaries.size) boundaries[i + 1] else bytes.size
            var j = i + 1

            while (j + 1 < boundaries.size) {
                val nextStart = boundaries[j]
                val nextEnd = boundaries[j + 1]
                val nextSegment = bytes.sliceArray(nextStart until nextEnd)

                if (nextSegment.all { it >= 0 && it < 0x7f }) {
                    end = nextEnd
                    j++
                } else {
                    break
                }
            }

            val fullSegment = bytes.sliceArray(start until end)

            if (isCharSegment(fullSegment)) {
                mergedBoundaries.add(NemesysSegment(start, NemesysField.STRING))
                i = j
            } else {
                mergedBoundaries.add(NemesysSegment(start, NemesysField.UNKNOWN))
                i++
            }
        }

        return mergedBoundaries
    }

    // try to improve boundaries by shifting them a bit
    private fun postProcessing(boundaries: MutableList<Int>, bytes: ByteArray): List<NemesysSegment> {
        var result = mergeCharSequences(boundaries, bytes)
        result = slideCharWindow(result, bytes)
        result = nullByteTransitions(result, bytes)
        result = entropyMerge(result, bytes)
        result = splitFixed(result, bytes)

        return result
    }

    // calc Shannon-Entropy for one segment
    fun calculateShannonEntropy(segment: ByteArray): Double {
        // count the amount of bytes in the segment
        val frequency = mutableMapOf<Byte, Int>()
        for (byte in segment) {
            frequency[byte] = (frequency[byte] ?: 0) + 1
        }

        // calc entropy
        val total = segment.size.toDouble()
        var entropy = 0.0
        for ((_, count) in frequency) {
            val probability = count / total
            entropy -= probability * ln(probability) / ln(2.0)
        }

        return entropy
    }

    // split bytes at the beginning of the message
    fun splitFixed(
        segments: MutableList<NemesysSegment>,
        bytes: ByteArray
    ): MutableList<NemesysSegment> {
        if (segments.isEmpty()) return segments

        val firstSegment = segments[0]
        val nextOffset = if (segments.size > 1) segments[1].offset else bytes.size
        val firstSegmentLength = nextOffset - firstSegment.offset

        // check if first segment has more than two bytes and every bytes is below 0x10
        if (firstSegmentLength >= 2 &&
            bytes.slice(firstSegment.offset until nextOffset).all { it.toUByte().toInt() < 0x10 }
        ) {
            // split bytes in own segments
            val newSegments = mutableListOf<NemesysSegment>()
            for (offset in firstSegment.offset until nextOffset) {
                newSegments.add(NemesysSegment(offset, firstSegment.fieldType))
            }

            // add new segments at pos 0
            segments.removeAt(0)
            segments.addAll(0, newSegments)
        }

        return segments
    }


    // merge two segments based on their entropy
    fun entropyMerge(
        segments: List<NemesysSegment>,
        bytes: ByteArray
    ): MutableList<NemesysSegment> {
        val result = mutableListOf<NemesysSegment>()

        var index = 0
        while (index < segments.size) {
            // get current segment
            val (start, fieldType) = segments[index]
            val end = if (index + 1 < segments.size) segments[index + 1].offset else bytes.size
            val currentSegment = bytes.sliceArray(start until end)
            val currentEntropy = calculateShannonEntropy(currentSegment)

            if (index + 1 < segments.size) { // check if a following segment exists
                val (nextStart, nextFieldType) = segments[index + 1]
                if (fieldType == nextFieldType) {  // check that both field have the same field type
                    val nextEnd = if (index + 2 < segments.size) segments[index + 2].offset else bytes.size
                    val nextSegment = bytes.sliceArray(nextStart until nextEnd)
                    val nextEntropy = calculateShannonEntropy(nextSegment)

                    val entropyDiff = kotlin.math.abs(currentEntropy - nextEntropy)

                    if (currentEntropy > 0.7 && nextEntropy > 0.7 && entropyDiff < 0.05) {
                        // xor of the start bytes for both segments
                        val xorLength = minOf(2, currentSegment.size, nextSegment.size)
                        val xorStart1 = currentSegment.take(xorLength).toByteArray()
                        val xorStart2 = nextSegment.take(xorLength).toByteArray()
                        val xorResult = ByteArray(xorLength) { i -> (xorStart1[i].toInt() xor xorStart2[i].toInt()).toByte() }
                        val xorEntropy = calculateShannonEntropy(xorResult)

                        if (xorEntropy > 0.8) { // in the paper it's set to 0.95 instead of 0.8. Algorithm 3, however, says 0.8
                            // merge segments together
                            result.add(NemesysSegment(start, fieldType))
                            index += 2 // skip the following field because we want to merge it to this one
                            continue
                        }
                    }
                }
            }

            // add regular boundary if we didn't merge any segments
            result.add(NemesysSegment(start, fieldType))
            index++
        }

        return result
    }

    // shift null bytes to the right field
    fun nullByteTransitions(
        segments: MutableList<NemesysSegment>,
        bytes: ByteArray
    ): MutableList<NemesysSegment> {
        if (segments.size < 2) return segments.toMutableList()

        val result = mutableListOf<NemesysSegment>()
        result.add(segments[0])

        for (i in 1 until segments.size) {
            val (prevStart, prevType) = result.last()
            val (currStart, currType) = segments[i]

            Logger.log("Gegeben von $currStart")
            var newSegment: NemesysSegment? = NemesysSegment(currStart, currType)

            // Rule 1: allocate nullbytes to STRING field
            if (prevType == NemesysField.STRING) {
                // count null bytes after the segment
                var extra = 0
                while (currStart + extra < bytes.size && bytes[currStart + extra] == 0.toByte()) {
                    extra++
                }

                // only shift boundary if x0 bytes are less than 2 bytes long
                if (extra in 1..2) {
                    val shiftedOffset = currStart + extra
                    val exists = segments.any { it.offset == shiftedOffset }

                    if (!exists) { // only add boundary if it doesn't already exist
                        Logger.log("Rule 1")
                        newSegment = NemesysSegment(shiftedOffset, currType)
                    } else {
                        newSegment = null // don't override segment if it already exists
                    }
                }
            }

            // Rule 2: nullbytes before UNKNOWN
            if (newSegment != null && prevType != NemesysField.STRING && currType != NemesysField.STRING) {
                // count null bytes in front of the segment
                var count = 0
                var idx = currStart - 1
                while (idx >= prevStart && bytes[idx] == 0.toByte()) {
                    count++
                    idx--
                }

                // only shift boundary if x0 bytes are less than 2 bytes long
                if (count in 1..2) {
                    Logger.log("Rule 2")
                    newSegment = NemesysSegment(currStart - count, currType)
                }
            }

            if (newSegment != null) {
                Logger.log("Boundary at: ${newSegment.offset}")
                result.add(newSegment)
            }
        }

        return result.sortedBy { it.offset }.distinctBy { it.offset }.toMutableList()
    }



    // check if left or right byte of char sequence is also part of it
    fun slideCharWindow(segments: List<NemesysSegment>, bytes: ByteArray): MutableList<NemesysSegment> {
        val improved = mutableListOf<NemesysSegment>()

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
            if (type == NemesysField.STRING) {
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

                improved.add(NemesysSegment(newStart, NemesysField.STRING))
            } else {
                improved.add(NemesysSegment(start, type))
            }
        }

        return improved
    }



    // check if segment is a char sequence
    private fun isCharSegment(segment: ByteArray): Boolean {
        if (segment.size < 6) return false
        if (!segment.all { it >= 0 && it < 0x7f }) return false

        val nonZeroBytes = segment.filter { it != 0.toByte() }
        if (nonZeroBytes.isEmpty()) return false

        val mean = nonZeroBytes.map { it.toUByte().toInt() }.average()
        if (mean !in 50.0..115.0) return false

        val nonPrintable = nonZeroBytes.count { it < 0x20 || it == 0x7f.toByte() }
        val ratio = nonPrintable.toDouble() / segment.size
        return ratio < 0.33
    }

    // handle length field payload and add it to the result
    fun handlePrintablePayload(
        bytes: ByteArray, taken: BooleanArray, result: MutableList<NemesysSegment>,
        offset: Int, lengthFieldSize: Int, payloadLength: Int, bigEndian: Boolean
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
        if (bigEndian) {
            result.add(NemesysSegment(offset,NemesysField.PAYLOAD_LENGTH_BIG_ENDIAN))
        } else {
            result.add(NemesysSegment(offset,NemesysField.PAYLOAD_LENGTH_LITTLE_ENDIAN))
        }
        result.add(NemesysSegment(payloadStart,NemesysField.STRING_PAYLOAD))
        for (j in offset until payloadEnd) taken[j] = true

        return payloadEnd
    }

    // used to detect length fields and the corresponding payload
    fun checkLengthPrefixedSegment(
        bytes: ByteArray,
        taken: BooleanArray,
        result: MutableList<NemesysSegment>,
        offset: Int,
        lengthFieldSize: Int,
        bigEndian: Boolean
    ): Int? {
        if (offset + lengthFieldSize >= bytes.size) return null

        val length = NemesysUtil.tryParseLength(bytes, offset, lengthFieldSize, bigEndian) ?: return null

        val payloadStart = offset + lengthFieldSize
        val payloadEnd = payloadStart + length

        // check bounds and collisions
        if (payloadEnd > bytes.size || length < 3) return null
        if ((offset until payloadEnd).any { taken[it] }) return null

        return handlePrintablePayload(bytes, taken, result, offset, lengthFieldSize, length, bigEndian)
    }


    // detect length prefixes and the corresponding payload
    fun detectLengthPrefixedFields(bytes: ByteArray, taken: BooleanArray): List<NemesysSegment> {
        val segments = mutableListOf<NemesysSegment>()
        var i = 0

        while (i < bytes.size - 1) {
            // Try 2-byte length field (big endian)
            var newIndex = checkLengthPrefixedSegment(bytes, taken, segments, i, 2, true)
            if (newIndex != null) {
                i = newIndex
                continue
            }

            // Try 2-byte length field (little endian)
            newIndex = checkLengthPrefixedSegment(bytes, taken, segments, i, 2, false)
            if (newIndex != null) {
                i = newIndex
                continue
            }

            // Try 1-byte length field
            newIndex = checkLengthPrefixedSegment(bytes, taken, segments, i, 1, true)
            if (newIndex != null) {
                i = newIndex
                continue
            }

            i++
        }

        return segments.distinctBy { it.offset }.sortedBy { it.offset }
    }
}

// this object is used as a tool for nemesys classes
object NemesysUtil {
    // get actual length given the bytes and endian
    fun tryParseLength(
        bytes: ByteArray,
        offset: Int,
        lengthFieldSize: Int,
        bigEndian: Boolean
    ): Int? {
        return try {
            when (lengthFieldSize) {
                1 -> bytes[offset].toUByte().toInt()
                2 -> if (bigEndian)
                    (bytes[offset].toUByte().toInt() shl 8) or bytes[offset + 1].toUByte().toInt()
                else
                    (bytes[offset + 1].toUByte().toInt() shl 8) or bytes[offset].toUByte().toInt()
                4 -> if (bigEndian)
                    (bytes[offset].toUByte().toInt() shl 24) or
                            (bytes[offset + 1].toUByte().toInt() shl 16) or
                            (bytes[offset + 2].toUByte().toInt() shl 8) or
                            (bytes[offset + 3].toUByte().toInt())
                else
                    (bytes[offset + 3].toUByte().toInt() shl 24) or
                            (bytes[offset + 2].toUByte().toInt() shl 16) or
                            (bytes[offset + 1].toUByte().toInt() shl 8) or
                            (bytes[offset].toUByte().toInt())
                else -> null
            }
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    // to detect the range of a segment
    fun getByteRange(message: NemesysParsedMessage, index: Int): IntRange {
        val start = message.segments[index].offset
        val end = message.segments.getOrNull(index + 1)?.offset ?: message.bytes.size
        return start until end
    }

    fun setNemesysFieldfromDecoder(decoder: ByteWitchDecoder): NemesysField {
        return when (decoder.name.lowercase()) {
            "utf8" -> NemesysField.STRING
            "utf16" -> NemesysField.UNKNOWN // STRING_UTF16
            "ieee754" -> NemesysField.UNKNOWN // FLOAT
            else -> NemesysField.UNKNOWN
        }
    }
}
