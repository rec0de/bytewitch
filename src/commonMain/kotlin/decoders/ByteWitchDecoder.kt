package decoders

interface ByteWitchDecoder {

    val name: String

    fun decodesAsValid(data: ByteArray): Boolean {
        try {
            decode(data, 0)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun confidence(data: ByteArray): Double {
        return if(decodesAsValid(data)) 1.0 else 0.0
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