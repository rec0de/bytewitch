package SequenceAlignment

// interface for different sequence alignment methods
interface AlignmentResult<T> {
    fun align(messages: Map<Int, T>): List<AlignedSequence>
}

// object das is often be used by sequence alignment methods
object AlignmentUtils {

    // scoring function for bytes using canberra
    fun byteCanberra(a: Byte, b: Byte): Double {
        val ai = a.toInt() and 0xFF
        val bi = b.toInt() and 0xFF
        val denominator = ai + bi
        return if (denominator == 0) 0.0 else kotlin.math.abs(ai - bi).toDouble() / denominator
    }
}

// data class for aligned sequences. Index can either refer for a segment or for a byte
data class AlignedSequence(
    val protocolA: Int,
    val protocolB: Int,
    val indexA: Int,
    val indexB: Int,
    val dissimilarity: Double
)