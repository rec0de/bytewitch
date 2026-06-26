package preprocessing

object Reverse : Preprocessor {
    override val command = "reverse"

    override val doc = "reverse: reverse bytes before decoding [no args]"

    override fun process(args: String, payload: ByteArray): Pair<ByteArray,String?> {
        return Pair(payload.reversed().toByteArray(), null)
    }
}