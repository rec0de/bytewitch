package preprocessing

import htmlEscape

object BytewiseCalc : Preprocessor {
    override val command = "calc"

    override val doc = "calc: perform unsigned calculation on every byte. first arg: [+-*/] second arg: decimal (0x0a, 13, 0b101)"

    override fun process(args: String, payload: ByteArray): Pair<ByteArray, String?> {
        if(args.isBlank())
            return Pair(payload, doc)

        var argstring = args.trim()
        if(argstring.isEmpty())
            return Pair(payload, "calc: missing operator [+-*/]")
        val op = argstring[0]
        argstring = argstring.substring(1).trim()
        val arg = ByteWitch.parseDecimals(argstring) ?: return Pair(payload, "calc: malformed argument '$argstring'")

        if(arg.size != 1)
            return Pair(payload, "calc: argument should fit in one byte")
        val argInt = arg[0].toUByte()

        return when(op) {
            '+' -> Pair(payload.map { v -> (v.toUByte() + argInt).toByte() }.toByteArray(), null)
            '-' -> Pair(payload.map { v -> (v.toUByte() - argInt).toByte() }.toByteArray(), null)
            '*' -> Pair(payload.map { v -> (v.toUByte() * argInt).toByte() }.toByteArray(), null)
            '/' -> Pair(payload.map { v -> (v.toUByte() / argInt).toByte() }.toByteArray(), null)
            else -> Pair(payload, "calc: unsupported operand '$op'")
        }
    }
}