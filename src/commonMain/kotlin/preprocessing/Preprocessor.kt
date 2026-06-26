package preprocessing

interface Preprocessor {
    val command: String
    val doc: String
    fun process(args: String, payload: ByteArray): Pair<ByteArray, String?>
}