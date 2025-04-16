import bitmage.hex
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import kotlinx.dom.createElement
import org.w3c.dom.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import kotlinx.dom.createElement
import org.w3c.dom.*
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array


var liveDecodeEnabled = true
var currentHighlight: Element? = null


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
    val output = document.getElementById("output") as HTMLDivElement
    val floatview = document.getElementById("floatview") as HTMLDivElement
    val bytefinder = document.getElementById("bytefinder") as HTMLDivElement

    val bytes = ByteWitch.getBytesFromInputEncoding(input.value)
    val result = ByteWitch.analyze(bytes, tryhard)

    if(result.isNotEmpty()) {
        output.clear()
        floatview.innerText = bytes.hex()
        bytefinder.style.display = "flex"

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

fun attachRangeListeners(element: Element) {
    if(element.hasAttribute("data-start") && element.hasAttribute("data-end")) {
        val start = element.getAttribute("data-start")!!.toInt()
        val end =  element.getAttribute("data-end")!!.toInt()
        element.addEventListener("click", { evt ->
            console.log("$start to $end")
            val floatview = document.getElementById("floatview")!!
            floatview.innerHTML = floatview.textContent!! // re-set previous highlights
            val text = floatview.childNodes[0]!!
            val range = document.createRange()
            range.setStart(text, start*2);
            range.setEnd(text, end*2);
            range.surroundContents(document.createElement("span"))

            evt.stopPropagation()
        })

        // highlightable elements
        if(listOf("asn1", "protobuf", "generic", "bplist", "nsarchive", "opack").any { element.classList.contains(it) }) {
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