import kotlinx.browser.document
import org.w3c.dom.*

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


// input listener for live decode of all text areas
fun applyLiveDecodeListeners() {
    val textareas = document.querySelectorAll(".input_area")
    for (i in 0 until textareas.length) {
        val ta = textareas[i] as HTMLTextAreaElement
        ta.oninput = {
            if (liveDecodeEnabled)
                decode(true)
        }
    }
}

fun appendTextArea(content: String) {
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
    sizeLabel.appendChild(document.createElement("span"))

    wrapper.appendChild(textarea)
    wrapper.appendChild(sizeLabel)
    container.appendChild(wrapper)

    if (liveDecodeEnabled) {
        textarea.oninput = {
            decode(true)
        }
    }
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
