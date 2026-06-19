package preprocessing

object Reverse : Preprocessor {
    override val command = "reverse"

    override fun process(args: String, payload: ByteArray): ByteArray {
        return payload.reversed().toByteArray()
    }
}