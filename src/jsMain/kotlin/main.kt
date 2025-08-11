import bitmage.hex
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.*
import org.w3c.files.File
import org.w3c.files.FileReader


var liveDecodeEnabled = true
var currentHighlight: Element? = null


fun main() {
    window.addEventListener("load", {
        // Initialize the Kaitai related UI elements
        KaitaiUI
        // Load Kaitai structs
        MainScope().launch {
            KaitaiUI.loadBundledStructs()
        }
        KaitaiUI.loadKaitaiStructsFromStorage()

        LayoutManager
        LayoutManager.updateLayout()

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

        liveDecode.onchange = { enabled ->
            liveDecodeEnabled = liveDecode.checked
            if (liveDecodeEnabled)
                decode(false)
            0.0
        }
    })
}

fun decode(tryhard: Boolean) {
    console.log("decode(tryhard=$tryhard)")
    val input = document.getElementById("data") as HTMLTextAreaElement
    val output = document.getElementById("output") as HTMLDivElement

    val bytes = ByteWitch.getBytesFromInputEncoding(input.value)
    val result = ByteWitch.analyze(bytes, tryhard)

    if (result.isNotEmpty()) {
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
    textview.innerHTML = bytes.map { it.toInt().toChar() }
        .map { if (it.code in 32..59 || it.code in 64..90 || it.code in 97..122) it else '.' }.joinToString("")
    bytefinder.style.display = "flex"
}

fun setByteFinderHighlight(start: Int, end: Int, startBitOffset: Int, endBitOffset: Int) {
    val hexview = document.getElementById("hexview")!!
    hexview.innerHTML = hexview.textContent!! // re-set previous highlights
    val range = document.createRange()
    val text = hexview.childNodes[0]!! as Text
    val startHex = start * 2 + start / 8 + if (startBitOffset > 3) 1 else 0
    val endHex = end * 2 + end / 8 + if (endBitOffset > 4) 2 else if (endBitOffset > 0) 1 else 0
    range.setStart(text, startHex)
    range.setEnd(text, minOf(endHex, text.length))
    range.surroundContents(document.createElement("span"))

    val textview = document.getElementById("textview")!!
    textview.innerHTML = textview.textContent!! // re-set previous highlights
    val txtText = textview.childNodes[0]!!
    val txtRange = document.createRange()
    txtRange.setStart(txtText, start);
    txtRange.setEnd(txtText, end + (if (endBitOffset > 0) 1 else 0))
    txtRange.surroundContents(document.createElement("span"))
}

fun attachRangeListeners(element: Element) {
    if (element.hasAttribute("data-start") && element.hasAttribute("data-end")) {
        val start = element.getAttribute("data-start")!!.toInt()
        val end = element.getAttribute("data-end")!!.toInt()
        val startBitOffset = element.getAttribute("data-start-bit-offset")?.toInt() ?: 0
        val endBitOffset = element.getAttribute("data-end-bit-offset")?.toInt() ?: 0

        element.addEventListener("click", { evt ->
            console.log("$start (+ $startBitOffset bits) to $end (+ $endBitOffset bits)")
            setByteFinderHighlight(start, end, startBitOffset, endBitOffset)
            evt.stopPropagation()
        })

        // highlightable elements
        if (listOf(
                "asn1",
                "protobuf",
                "generic",
                "bplist",
                "nsarchive",
                "opack",
                "neutral"
            ).any { element.classList.contains(it) }
        ) {
            element.addEventListener("mouseover", { evt ->
                if (currentHighlight != null)
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