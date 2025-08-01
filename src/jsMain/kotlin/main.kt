import bitmage.hex
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import org.w3c.dom.*
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.js.Date
import kotlin.math.floor
import kotlin.math.roundToInt


var liveDecodeEnabled = true
var currentHighlight: Element? = null
var lastSelectionEvent: Double? = null


fun main() {
    window.addEventListener("load", {
        val input = document.getElementById("data") as HTMLTextAreaElement
        val decodeBtn = document.getElementById("decode") as HTMLButtonElement
        val tryhardBtn = document.getElementById("tryhard") as HTMLButtonElement
        val uploadBtn = document.getElementById("upload") as HTMLButtonElement

        val liveDecode = document.getElementById("livedecode") as HTMLInputElement
        liveDecodeEnabled = liveDecode.checked

        input.oninput = {
            if (liveDecodeEnabled)
                decode(false)
        }

        input.onselect = {
            lastSelectionEvent = Date().getTime()

            // we can only do the offset and range calculations if we have plain hex input (i.e. no base64, hexdump)
            if(ByteWitch.isPlainHex()) {
                Logger.log("selected ${input.selectionStart} to ${input.selectionEnd}")

                val prefix = input.value.substring(0, input.selectionStart!!)
                val sizeLabel = input.nextElementSibling as HTMLDivElement

                val r = Regex("#[^\n]*$")
                if(r.containsMatchIn(prefix))
                    sizeLabel.innerText = "" // selection starts in a comment
                else {
                    val selection = input.value.substring(input.selectionStart!!, input.selectionEnd!!)
                    val offset = ByteWitch.stripCommentsAndFilterHex(prefix).length.toDouble()/2
                    val range = ByteWitch.stripCommentsAndFilterHex(selection).length.toDouble()/2


                    (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = " — selected ${range}B at offset $offset (0x${floor(offset).roundToInt().toString(16)})"
                }
            }
        }

        // a click anywhere clears any present selection
        // (as do specific keystrokes, but we'll see if we want to worry about those)
        document.onclick = {
            // avoid immediately clearing selection from click associated with select event
            if(lastSelectionEvent != null && Date().getTime() - lastSelectionEvent!! > 250) {
                lastSelectionEvent = null
                val inputs = document.querySelectorAll("textarea")
                inputs.asList().forEach {
                    val sizeLabel = (it as HTMLTextAreaElement).nextElementSibling!!
                    val selectionLabel = sizeLabel.firstChild!!.nextSibling as HTMLSpanElement
                    selectionLabel.innerText = ""
                }
            }
        }

        document.onkeydown = {
            if(lastSelectionEvent != null && Date().getTime() - lastSelectionEvent!! > 250 && it.keyCode !in listOf(16, 17, 20)) {
                lastSelectionEvent = null
                val inputs = document.querySelectorAll("textarea")
                inputs.asList().forEach {
                    val sizeLabel = (it as HTMLTextAreaElement).nextElementSibling!!
                    val selectionLabel = sizeLabel.firstChild!!.nextSibling as HTMLSpanElement
                    selectionLabel.innerText = ""
                }
            }
        }

        decodeBtn.onclick = {
            decode(false)
        }

        tryhardBtn.onclick = {
            decode(true)
        }

        uploadBtn.onclick = {
            val fileInput = document.createElement("input") as HTMLInputElement
            fileInput.type = "file"
            fileInput.accept = "*" // Accept any file type

            fileInput.onchange = {
                val file = fileInput.files?.item(0)
                if (file != null) {
                    if (file.type == "text/plain") {
                        // Handle .txt files
                        readFile(file)
                    } else {
                        // Handle binary files
                        readBinaryFile(file)
                    }
                }
            }

            // Trigger the file selection dialog
            fileInput.click()
        }

        liveDecode.onchange = {
            liveDecodeEnabled = liveDecode.checked
            0.0
        }
    })
}

fun decode(tryhard: Boolean) {
    val input = document.getElementById("data") as HTMLTextAreaElement
    val sizeLabel = input.nextElementSibling as HTMLDivElement
    val output = document.getElementById("output") as HTMLDivElement

    val bytes = ByteWitch.getBytesFromInputEncoding(input.value)
    (sizeLabel.firstChild as HTMLSpanElement).innerText = "${bytes.size}B (0x${bytes.size.toString(16)})" // recalc payload size
    (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = "" // clear selection info

    // no point in analyzing empty bytes
    if (bytes.isEmpty()) { return }

    val result = ByteWitch.analyze(bytes, tryhard)

    if(result.isNotEmpty()) {
        output.clear()
        setByteFinderContent(bytes)

        result.forEach {
            val parseResult = document.createElement("DIV") as HTMLDivElement

            val parseName = document.createElement("H3") as HTMLHeadingElement
            parseName.innerText = it.first

            val parseContent = document.createElement("DIV") as HTMLDivElement
            parseContent.classList.add("parsecontent")
            parseContent.innerHTML = it.second.renderHTML()

            parseContent.children.asList().forEach { child ->
                attachRangeListeners(child)
            }

            parseResult.appendChild(parseName)
            parseResult.appendChild(parseContent)

            output.appendChild(parseResult)
        }
    }
}

fun setByteFinderContent(bytes: ByteArray) {
    val hexview = document.getElementById("hexview") as HTMLDivElement
    val textview = document.getElementById("textview") as HTMLDivElement
    val bytefinder = document.getElementById("bytefinder") as HTMLDivElement

    hexview.innerText = bytes.hex().chunked(16).joinToString(" ")
    textview.innerHTML = bytes.map { it.toInt().toChar() }.map { if(it.code in 32..59 || it.code in 64..90 || it.code in 97..122) it else '.' }.joinToString("")
    bytefinder.style.display = "flex"
}

fun setByteFinderHighlight(start: Int, end: Int) {
    if(start < 0 || end < 0)
        return

    val hexview = document.getElementById("hexview")!!
    hexview.innerHTML = hexview.textContent!! // re-set previous highlights
    val range = document.createRange()
    val text = hexview.childNodes[0]!! as Text
    range.setStart(text, start*2 + start/8)
    range.setEnd(text, minOf(end*2 + end/8, text.length))
    range.surroundContents(document.createElement("span"))

    val textview = document.getElementById("textview")!!
    textview.innerHTML = textview.textContent!! // re-set previous highlights
    val txtText = textview.childNodes[0]!!
    val txtRange = document.createRange()
    txtRange.setStart(txtText, start);
    txtRange.setEnd(txtText, end);
    txtRange.surroundContents(document.createElement("span"))
}

fun attachRangeListeners(element: Element) {
    if(element.hasAttribute("data-start") && element.hasAttribute("data-end")) {
        val start = element.getAttribute("data-start")!!.toInt()
        val end =  element.getAttribute("data-end")!!.toInt()
        element.addEventListener("click", { evt ->
            console.log("$start to $end")
            setByteFinderHighlight(start, end)
            evt.stopPropagation()
        })

        // highlightable elements
        if(listOf("asn1", "protobuf", "generic", "bplist", "nsarchive", "opack", "neutral").any { element.classList.contains(it) }) {
            element.addEventListener("mouseover", { evt ->
                if(currentHighlight != null)
                    currentHighlight!!.classList.remove("highlight")

                element.classList.add("highlight")
                currentHighlight = element
                evt.stopPropagation()
            })
        }
    }
    element.children.asList().forEach { attachRangeListeners(it) }
}

fun readBinaryFile(file: File) {
    val input = document.getElementById("data") as HTMLTextAreaElement
    val reader = FileReader()

    reader.onload = {
        val arrayBuffer = reader.result as? ArrayBuffer
        if (arrayBuffer != null) {
            val hexContent = arrayBufferToHex(arrayBuffer) // Convert binary data to hex
            input.value = hexContent // Display hex content in the textarea
        } else {
            console.error("Failed to read binary file content")
        }
    }

    reader.onerror = {
        console.error("Failed to read the file: ${reader.error?.message}")
    }

    reader.readAsArrayBuffer(file) // Read binary data in the file
}

fun readFile(file: File) {
    val input = document.getElementById("data") as HTMLTextAreaElement
    val reader = FileReader()

    reader.onload = {
        val content = reader.result?.toString() // Safely convert `result` to a string
        if (content != null) {
            input.value = content // Write the file content to the textarea
        } else {
            console.error("File content is null")
        }
    }

    reader.onerror = {
        console.error("Failed to read the file: ${reader.error?.message}")
    }

    reader.readAsText(file) // Read the file content as text
}

fun arrayBufferToHex(buffer: ArrayBuffer): String {
    val byteArray = Uint8Array(buffer) // Create a Uint8Array view for the buffer
    val dynamic = byteArray.asDynamic()
    return (0 until byteArray.length).joinToString("") { index ->
        val b16string = dynamic[index].toString(16) as String
        b16string.padStart(2, '0')
    }
}