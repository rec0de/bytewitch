package preprocessing

import htmlEscape
import kotlin.experimental.xor

object Xor : Preprocessor {
    override val command = "xor"

    override val doc = "xor: xor input with argument, repeated. argument should be decimal, e.g. 0xCAFE 0b1001 1337"

    override fun process(args: String, payload: ByteArray): Pair<ByteArray, String?> {

        if(args.isBlank())
            return Pair(payload, doc)

        val key = ByteWitch.parseDecimals(args) ?: return Pair(payload, "xor: malformed argument '$args'")
        if(key.isEmpty())
            return Pair(payload, "xor: empty argument")
        return Pair(payload.mapIndexed { i, v -> v xor key[i % key.size] }.toByteArray(), null)
    }
}