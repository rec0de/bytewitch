
actual object Logger {
    actual fun log(vararg o: Any?) = console.log(*o)
}

actual class Date actual constructor(timestamp: Long) {

    private val internal = kotlin.js.Date(timestamp)
    actual fun toAppleTimestamp(): Double {
        TODO("Not yet implemented")
    }

    override fun toString() = internal.toString()
}

actual fun currentTimestamp(): Long {
    return (kotlin.js.Date().getTime()).toLong()
}