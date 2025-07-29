import bitmage.hex
import kotlinx.browser.document
import org.w3c.dom.*

// set bytes in hexview and textview
fun setByteFinderContent(msgIndex: Int) {
    val bytes = parsedMessages[msgIndex]?.bytes ?: return

    val hexview = document.getElementById("hexview") as HTMLDivElement
    val textview = document.getElementById("textview") as HTMLDivElement
    val bytefinder = document.getElementById("bytefinder") as HTMLDivElement

    hexview.innerText = bytes.hex().chunked(16).joinToString(" ")
    textview.innerHTML = bytes.map { it.toInt().toChar() }.map { if(it.code in 32..59 || it.code in 64..90 || it.code in 97..122) it else '.' }.joinToString("")
    bytefinder.style.display = "flex"
}

// set bytes in hexview and textview and highlight segment
fun setByteFinderHighlight(start: Int, end: Int, msgIndex: Int) {
    if(start < 0 || end < 0)
        return

    val hexview = document.getElementById("hexview")!!

    // set byte sequence
    setByteFinderContent(msgIndex)

    // apply highlighting in hexview
    hexview.innerHTML = hexview.textContent!! // re-set previous highlights
    val range = document.createRange()
    val text = hexview.childNodes[0]!! as Text
    range.setStart(text, start*2 + start/8)
    range.setEnd(text, minOf(end*2 + end/8, text.length))
    range.surroundContents(document.createElement("span"))

    // apply highlighting in textview
    val textview = document.getElementById("textview")!!
    textview.innerHTML = textview.textContent!! // re-set previous highlights
    val txtText = textview.childNodes[0]!!
    val txtRange = document.createRange()
    txtRange.setStart(txtText, start);
    txtRange.setEnd(txtText, end);
    txtRange.surroundContents(document.createElement("span"))
}


// attach range listener for float view
fun attachRangeListeners(element: Element, msgIndex: Int) {
    if (element.hasAttribute("data-start") && element.hasAttribute("data-end")) {
        val start = element.getAttribute("data-start")!!.toInt()
        val end = element.getAttribute("data-end")!!.toInt()

        element.addEventListener("click", { evt ->
            setByteFinderHighlight(start, end, msgIndex)
            evt.stopPropagation()
        })

        // highlightable elements
        if (listOf("asn1", "protobuf", "generic", "bplist", "nsarchive", "opack", "ssf").any { element.classList.contains(it) }) {
            element.addEventListener("mouseover", { evt ->
                if (currentHighlight != null)
                    currentHighlight!!.classList.remove("highlight")

                element.classList.add("highlight")
                currentHighlight = element
                evt.stopPropagation()
            })
        }
    }

    // also attach range listeners to child items
    element.children.asList().forEach { attachRangeListeners(it, msgIndex) }
}