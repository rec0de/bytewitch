package preprocessing

interface Preprocessor {
    val command: String
    fun process(args: String, payload: ByteArray): ByteArray
}