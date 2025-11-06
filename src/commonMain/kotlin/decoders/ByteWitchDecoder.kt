package decoders

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
            return decode(data, 0)
        }
        catch (e: Exception) {
            Logger.log(e.toString())
            return null
        }
    }

    // Decoders may support finding data they can decode (i.e. based on headers) within larger blobs
    // if so, they may implement this method to surface that info to ByteWitch for better decoding
    fun findDecodableSegments(data: ByteArray): List<Pair<Int, Int>> {
        return emptyList()
    }
}