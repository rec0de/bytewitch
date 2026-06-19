package preprocessing

import kotlin.experimental.and

object And : Preprocessor {
    override val command = "and"
    override fun process(args: String, payload: ByteArray): ByteArray {
        val key = ByteWitch.parseDecimals(args) ?: return payload
        return payload.mapIndexed { i, v -> v and  key[i % key.size] }.toByteArray()
    }
}