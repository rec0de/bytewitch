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
    ssfEligible.remove(lastIndex)

    removeAllSequenceAlignmentListeners()
    if (ssfEligible.size >= 2) {
        showStartSequenceAlignmentButton()
    } else {
        hideStartSequenceAlignmentButton()
        hideSequenceAlignmentToggleButton()

        // switch to segmentwise view if only one ssf item is left
        if (!showSegmentWiseAlignment && ssfEligible.isNotEmpty()) {
            showSegmentWiseAlignment = true
            ssfEligible.forEach { idx ->
                parsedMessages[idx]?.let { parsed ->
                    rerenderSSF(idx, parsed)
                }
            }
        }
    }

    // delete from output view
    val output = document.getElementById("output") as HTMLDivElement
    val target = document.getElementById("message-output-$lastIndex") as? HTMLDivElement
    if (target != null) {
        output.removeChild(target)
    }

    // reset hexview to the bytes of the first textarea
    setByteFinderContent(0)
}

fun appendTextArea(content: String = "") {
    val container = document.getElementById("data_container")!!

    // create new textareaContainer if no empty one exists
    val wrapper = document.createElement("div") as HTMLDivElement
    wrapper.className = "textareaContainer"

    val textarea = document.createElement("textarea") as HTMLTextAreaElement
    textarea.className = "data input_area"
    textarea.placeholder = "hex, base64, hexdump, or plaintext. use # as line comment in hex mode."
    textarea.value = content

    val (bytes, encoding) = ByteWitch.getBytesFromInputEncoding(content)
    val byteSizeText = "${bytes.size}B (0x${bytes.size.toString(16)})"

    val sizeText = document.createElement("span") as HTMLSpanElement
    sizeText.innerText = byteSizeText

    val sizeLabel = document.createElement("div") as HTMLDivElement
    sizeLabel.className = "sizeLabel"
    sizeLabel.appendChild(sizeText)
    sizeLabel.appendChild(document.createElement("span"))

    val encodingLabel = document.createElement("div") as HTMLDivElement
    encodingLabel.className = "encoding"
    encodingLabel.innerText = encoding.label

    wrapper.appendChild(textarea)
    wrapper.appendChild(sizeLabel)
    wrapper.appendChild(encodingLabel)
    container.appendChild(wrapper)

    textarea.oninput = {
        if(liveDecodeEnabled)
            mainDecode(isLiveDecoding = true, tryhard = false)
    }

    textarea.addEventListener("selectionchange", {
        // we can only do the offset and range calculations if we have plain hex input (i.e. no base64, hexdump)
        if(textarea.getAttribute("data-plainhex") == "true") {
            clearSelections()
            Logger.log("selected ${textarea.selectionStart} to ${textarea.selectionEnd}")

            val prefix = textarea.value.substring(0, textarea.selectionStart!!)
            val sizeLabel = textarea.nextElementSibling as HTMLDivElement

            val r = Regex("#[^\n]*$")
            if(r.containsMatchIn(prefix) || (textarea.selectionEnd?:0) - (textarea.selectionStart?:0) == 0)
                (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = "" // selection starts in a comment
            else {
                lastSelectionEvent = Date().getTime()

                val selection = textarea.value.substring(textarea.selectionStart!!, textarea.selectionEnd!!)
                val offset = ByteWitch.stripComments(prefix).length.toDouble()/2
                val range = ByteWitch.stripComments(selection).length.toDouble()/2

                (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = " — selected ${range}B (0x${floor(range).roundToInt().toString(16)}) at offset $offset (0x${floor(offset).roundToInt().toString(16)})"
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
            if (liveDecodeEnabled) mainDecode(isLiveDecoding = true, tryhard = false)
            return
        }
    }

    appendTextArea(content)
}
