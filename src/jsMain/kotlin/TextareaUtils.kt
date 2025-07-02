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

    output.removeChild(output.lastElementChild!!)

    // reset floatview
    val floatview = document.getElementById("floatview") as HTMLDivElement
    floatview.innerHTML = ""
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

// add content to textarea
fun appendTextareaWithContent(content: String) {
    val container = document.getElementById("data_container")!!
    val textareas = container.querySelectorAll(".input_area")

    // check if an empty text area already exists
    for (i in 0 until textareas.length) {
        val ta = textareas[i] as HTMLTextAreaElement
        if (ta.value.trim().isEmpty()) {
            // write content in empty text area
            ta.value = content
            return
        }
    }

    // create new textarea if no empty one exists
    val textarea = document.createElement("textarea") as HTMLTextAreaElement
    textarea.className = "data input_area"
    textarea.value = content
    container.appendChild(textarea)

    if (liveDecodeEnabled) {
        textarea.oninput = {
            decode(true)
        }
    }
}
