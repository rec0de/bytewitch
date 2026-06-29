import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

actual object Logger {
    actual fun tag(tag: String, msg: String) = log("[$tag] $msg")
    actual fun log(vararg o: Any?) = console.log(*o)
    actual fun showUserVisibleMessage(msg: String) {
        val div = (document.getElementById("log") as HTMLDivElement)
        div.innerText = msg
        div.style.display = "block"
    }
    actual fun clearUserVisibleMessage() {
        val elem = document.getElementById("log")
        if(elem != null)
            (elem as HTMLDivElement).style.display = "none"
    }
}

actual class Date actual constructor(timestamp: Long) {
    private val internal = kotlin.js.Date(timestamp)
    override fun toString() = internal.toString()
}

actual fun currentTimestamp(): Long {
    return (kotlin.js.Date().getTime()).toLong()
}

actual fun dateFromUTCString(string: String, fullYear: Boolean): Date {
    var timezone = "Z"
    var time = string

    if(!string.endsWith("Z")) {
        val parts = string.split("-", "+")
        val positive = string.contains("+")

        time = parts[0]
        val offset = parts[1]
        val oh = offset.substring(0..1)
        val om = offset.substring(1..2)
        timezone = if(positive) "+$oh:$om" else "-$oh:$om"
    }
    else
        time = time.removeSuffix("Z")

    var century = ""
    // extract century and reduce to two-character year format
    if(fullYear) {
        century = time.substring(0..1)
        time = time.substring(2)
    }

    val year = time.substring(0..1)
    val month = time.substring(2..3)
    val day = time.substring(4..5)
    val hour = time.substring(6..7)
    val minute = time.substring(8..9)
    val second = if(time.length > 11) time.substring(10..11) else "00"

    // ... there could be fractional seconds here in generalized time formats but let's just ignore those for now

    if(!fullYear)
        century = if(year.toInt() < 50) "20" else "19"

    val canonicalString = "$century$year-$month-${day}T${hour}:$minute:$second.000$timezone"

    Logger.log(canonicalString)

    return Date(kotlin.js.Date.parse(canonicalString).toLong())
}