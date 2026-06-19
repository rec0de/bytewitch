package preprocessing

import kotlin.experimental.xor

object Xor : Preprocessor {
    override val command = "xor"
    override fun process(args: String, payload: ByteArray): ByteArray {
        val key = ByteWitch.parseDecimals(args) ?: return payload
        return payload.mapIndexed { i, v -> v xor key[i % key.size] }.toByteArray()
    }
}