import kotlinx.browser.document
import org.w3c.dom.*
import kotlin.js.Date
import kotlin.math.floor
import kotlin.math.roundToInt

// remove text area from view and corresponding listeners
fun removeTextArea(dataContainer: Element) {
    val lastIndex = dataContainer.children.length - 1
    if (lastIndex < 0) return

    // delete text area from view
    dataContainer.removeChild(dataContainer.lastElementChild!!)

    parsedMessages.remove(lastIndex)

    // TODO need to remove alignment listeners

    // delete from output view
    val output = document.getElementById("output") as HTMLDivElement
    val messageOutputs = output.querySelectorAll(".message-output")
    if (messageOutputs.length > 0) {
        // remove last child
        output.removeChild(messageOutputs[messageOutputs.length - 1] as HTMLDivElement)
    }

    // reset hexview
    val hexview = document.getElementById("hexview") as HTMLDivElement
    hexview.innerHTML = ""
}

fun appendTextArea(content: String = "") {
    val container = document.getElementById("data_container")!!

    // create new textareaContainer if no empty one exists
    val wrapper = document.createElement("div") as HTMLDivElement
    wrapper.className = "textareaContainer"

    val textarea = document.createElement("textarea") as HTMLTextAreaElement
    textarea.className = "data input_area"
    textarea.placeholder = "hex, base64, or hexdump. use # as line comment in hex mode."
    textarea.value = content

    val bytes = ByteWitch.getBytesFromInputEncoding(content)
    val byteSizeText = "${bytes.size}B (0x${bytes.size.toString(16)})"

    val sizeText = document.createElement("span") as HTMLSpanElement
    sizeText.innerText = byteSizeText

    val sizeLabel = document.createElement("div") as HTMLDivElement
    sizeLabel.className = "sizeLabel"
    sizeLabel.appendChild(sizeText)
    sizeLabel.appendChild(document.createElement("span")) // selection info

    wrapper.appendChild(textarea)
    wrapper.appendChild(sizeLabel)
    container.appendChild(wrapper)

    textarea.oninput = {
        if(liveDecodeEnabled)
            decode(true)

        // update byte size label
        val bytes = ByteWitch.getBytesFromInputEncoding(textarea.value)
        (sizeLabel.firstChild as HTMLSpanElement).innerText = "${bytes.size}B (0x${bytes.size.toString(16)})"

        // update "plain hex" attribute on textarea
        textarea.setAttribute("data-plainhex", ByteWitch.isPlainHex().toString())
    }

    textarea.addEventListener("selectionchange", {
        lastSelectionEvent = Date().getTime()

        // we can only do the offset and range calculations if we have plain hex input (i.e. no base64, hexdump)
        if (textarea.getAttribute("data-plainhex") == "true") {
            val selectionStart = textarea.selectionStart!!
            val selectionEnd = textarea.selectionEnd!!

            clearSelections()
            if (selectionStart == selectionEnd) {
                return@addEventListener
            }
            Logger.log("selected ${selectionStart} to ${selectionEnd}")

            val prefix = textarea.value.substring(0, selectionStart)
            val sizeLabel = textarea.nextElementSibling as HTMLDivElement

            val r = Regex("#[^\n]*$")
            if (r.containsMatchIn(prefix))
                sizeLabel.innerText = "" // selection starts in a comment
            else {
                val selection = textarea.value.substring(selectionStart, selectionEnd)
                val range = ByteWitch.stripCommentsAndFilterHex(selection).length.toDouble() / 2
                val offset = ByteWitch.stripCommentsAndFilterHex(prefix).length.toDouble() / 2
                val offsetHex = "0x" + floor(offset).roundToInt().toString(16)

                (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText =
                    " — selected ${range}B at offset $offset ($offsetHex)"
            }
        }
    })
}


// add content to textarea if no empty one exits yet
fun appendTextareaForFileUpload(content: String) {
    val container = document.getElementById("data_container")!!
    val textareas = container.querySelectorAll(".input_area")

    // check if an empty text area already exists
    for (i in 0 until textareas.length) {
        val ta = textareas[i] as HTMLTextAreaElement
        if (ta.value.trim().isEmpty()) {
            ta.value = content
            if (liveDecodeEnabled) decode(true)
            return
        }
    }

    appendTextArea(content)
}