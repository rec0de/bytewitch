package preprocessing

import htmlEscape
import kotlin.experimental.and

object And : Preprocessor {
    override val command = "and"

    override val doc = "and: bitwise and input with argument, repeated. argument should be decimal, e.g. 0xCAFE 0b1001 1337"

    override fun process(args: String, payload: ByteArray): Pair<ByteArray, String?> {
        if(args.isBlank())
            return Pair(payload, doc)

        val key = ByteWitch.parseDecimals(args) ?: return Pair(payload, "and: malformed argument '$args)'")
        if(key.isEmpty())
            return Pair(payload, "and: empty argument")
        return Pair(payload.mapIndexed { i, v -> v and  key[i % key.size] }.toByteArray(), null)
    }
}