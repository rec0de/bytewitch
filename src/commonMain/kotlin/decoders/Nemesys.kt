package decoders

import ByteWitch
import Logger
import ParseCompanion
import bitmage.fromHex
import bitmage.hex
import org.w3c.dom.mediacapture.DoubleRange
import kotlin.math.exp
import kotlin.math.ceil
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

object Nemesys : ByteWitchDecoder {
    override val name = "Nemesys"

    override fun tryhardDecode(data: ByteArray) = null

    override fun decodesAsValid(data: ByteArray) = Pair(true, null)

    override fun confidence(data: ByteArray) = if(data.size >= 3) 0.76 else 0.00

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        // for segmentation
        val segmentBoundaries = findSegmentBoundaries(data)

        // show amount of segmentations
        val segmentInfo = "Detected ${segmentBoundaries.size} boundaries: "

        // hex representation with colored segments
        val hexData = highlightSegments(data, segmentBoundaries)

        return if (inlineDisplay) {
            BWAnnotatedData(segmentInfo + hexData, "".fromHex(), Pair(sourceOffset, sourceOffset + data.size))
        } else {
            BWAnnotatedData("$segmentInfo $hexData", "".fromHex(), Pair(sourceOffset, sourceOffset + data.size))
        }
    }

    private fun highlightSegments(data: ByteArray, boundaries: List<Int>): String {
        if (boundaries.isEmpty()) return data.hex() // if no segments are found

        val colors = listOf("#F6B9D3", "#FFE5B8", "#C9D8A3", "#F3DDC6", "#FEF1B0") // different colors
        var colorIndex = 0

        val result = StringBuilder()
        var lastIndex = 0

        for (boundary in boundaries + data.size) {
            if (boundary > lastIndex) {
                val segment = data.sliceArray(lastIndex until boundary).hex()
                result.append("<span style=\"background:${colors[colorIndex % colors.size]}\">$segment</span>")
                colorIndex++
            }
            lastIndex = boundary
        }

        return result.toString()
    }

    private fun bitCongruence(b1: Byte, b2: Byte): Double {
        var count = 0
        for (i in 0 until 8) {
            if (((b1.toInt() shr i) and 1) == ((b2.toInt() shr i) and 1)) {
                count++
            }
        }
        return count / 8.0
    }

    private fun computeDeltaBC(message: ByteArray): DoubleArray {
        val n = message.size
        if (n < 3) return DoubleArray(0) // return empty array if it's too short

        val bc = DoubleArray(n - 1)
        for (i in 0 until n - 1) {
            bc[i] = bitCongruence(message[i], message[i + 1])
        }

        val deltaBC = DoubleArray(n - 2)
        for (i in 1 until n - 1) {
            deltaBC[i - 1] = bc[i] - bc[i - 1]
        }

        return deltaBC
    }

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
    private fun findExtremaInList(smoothedDeltaBC: DoubleArray): List<Pair<Int, Int>> {
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
    private fun findRisingDeltas(extrema: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
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
    private fun findInflectionPoints(risingDeltas: List<Pair<Int, Int>>, smoothedDeltaBC: DoubleArray): List<Int> {
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

    // find segmentation boundaries
    private fun findSegmentBoundaries(message: ByteArray): List<Int> {
        val deltaBC = computeDeltaBC(message)

        // sigma should depend on the field length: Nemesys paper on page 5
        val smoothedDeltaBC = applyGaussianFilter(deltaBC, 0.6)

        // Safety check
        if (smoothedDeltaBC.isEmpty()) return mutableListOf<Int>()

        // find extrema of smoothedDeltaBC
        val extrema = findExtremaInList(smoothedDeltaBC)

        // find all rising points from minimum to maximum in extrema list
        val risingDeltas = findRisingDeltas(extrema)

        // find inflection point in risingDeltas -> those are considered as boundaries
        val boundaries = findInflectionPoints(risingDeltas, smoothedDeltaBC)

        return boundaries
    }
}