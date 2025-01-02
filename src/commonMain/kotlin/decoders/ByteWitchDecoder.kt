package decoders

interface ByteWitchDecoder {

    val name: String

    fun decodesAsValid(data: ByteArray): Pair<Boolean, ByteWitchResult?> {
        try {
            val decoded = decode(data, 0)
            return Pair(true, decoded)
        } catch (e: Exception) {
            return Pair(false, null)
        }
    }

    fun confidence(data: ByteArray): Double {
        return if(decodesAsValid(data).first) 1.0 else 0.0
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
}