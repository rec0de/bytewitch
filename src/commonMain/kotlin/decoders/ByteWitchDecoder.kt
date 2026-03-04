package decoders

import bitmage.fromIndex
import bitmage.hex
import bitmage.untilIndex
import kotlin.math.max

interface ByteWitchDecoder {

    val name: String

    fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?>  {
        try {
            val decoded = decode(data, sourceOffset)
            return Pair(1.0, decoded)
        } catch (e: Exception) {
            return Pair(0.0, null)
        }
    }

    fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean = false): ByteWitchResult

    /*
        tryhard decoding may relax some restrictions, accept partial decodes, apply slight brute force, etc
     */
    fun tryhardDecode(data: ByteArray): ByteWitchResult? {
        try {
            val decodableSegments = findDecodableSegments(data).sortedBy { it.first }

            return if(decodableSegments.isNotEmpty()) {
                //Logger.log("[tryhard-$name] decodable segments: ${decodableSegments.joinToString(", ") { "(${it.first}-${it.second})" }}")

                val decoded = decodableSegments.mapNotNull {
                    try {
                        val dataSlice = data.sliceArray(it.first until it.second)
                        decode(dataSlice, it.first)
                    } catch (e: Exception) {
                        null
                    }
                }

                //Logger.log("[tryhard-$name] decoded segments: ${decoded.joinToString(", "){ "(${it.sourceByteRange?.first}-${it.sourceByteRange?.second})" }}")

                val totalDecodedBytes = decoded.sumOf {
                    val range = if (it is BPListObject) it.rootByteRange else it.sourceByteRange
                    (range?.second ?: 0) - (range?.first ?: 0)
                }

                // make sure decoded bytes account for at least 20% of payload
                if(totalDecodedBytes.toDouble() / data.size < 0.2)
                    return null

                val undecodedFragments = mutableListOf<BWRangeTaggedData>()
                var decodedUntil = 0

                decoded.forEach {
                    val range = if(it is BPListObject) it.rootByteRange else it.sourceByteRange
                    if(range != null && range.first > decodedUntil) {
                        undecodedFragments.add(BWRangeTaggedData(data.sliceArray(decodedUntil until range.first), decodedUntil))
                    }
                    decodedUntil = max(decodedUntil, range?.second ?: 0)
                }

                if(decodedUntil < data.size)
                    undecodedFragments.add(BWRangeTaggedData(data.sliceArray(decodedUntil until data.size), decodedUntil))

                val merged = decoded.map { Pair(it, null) } + undecodedFragments.map { Pair(null, it) }
                val sorted = merged.sortedBy {
                    val firstStart = if(it.first is BPListObject) (it.first as BPListObject).rootByteRange?.first else it.first?.sourceByteRange?.first
                    firstStart ?: it.second?.start
                }

                return MultiPartialDecode(sorted, Pair(0, data.size))
            }
            else
                decode(data, 0)
        }
        catch (e: Exception) {
            Logger.log(e.toString())
            return null
        }
    }

    // Decoders may support finding data they can decode (i.e. based on headers) within larger blobs
    // if so, they may implement this method to surface that info to ByteWitch for better decoding
    // returns start and end offset of identified segments, start is inclusive, end is exclusive (start until end in kotlin terms)
    fun findDecodableSegments(data: ByteArray): List<Pair<Int, Int>> {
        return emptyList()
    }
}