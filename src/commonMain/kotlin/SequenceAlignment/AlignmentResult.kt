package SequenceAlignment

interface AlignmentResult<T> {
    fun align(messages: Map<Int, T>): List<AlignedSegment>
}

data class AlignedSegment(
    val protocolA: Int,
    val protocolB: Int,
    val segmentIndexA: Int,
    val segmentIndexB: Int,
    val dissimilarity: Double
)