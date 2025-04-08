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
import org.w3c.dom.events.MouseEvent
import kotlin.math.roundToInt

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*


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

            attachNemesysSeparatorHandlers()
        }
    }
}

fun attachNemesysSeparatorHandlers() {
    val separators = document.querySelectorAll(".field-separator")

    for (i in 0 until separators.length) {
        val separator = separators[i] as HTMLElement

        var isDragging = false
        var startX = 0.0
        var currentSeparator: HTMLElement? = null
        var hoverTarget: HTMLElement? = null
        var separatorTop: Double = 0.0

        separator.addEventListener("mousedown", { event ->
            event as MouseEvent
            isDragging = true
            startX = event.clientX.toDouble()
            separatorTop = separator.getBoundingClientRect().top
            currentSeparator = separator
            separator.style.zIndex = "100"
            separator.style.position = "relative"
            document.body?.style?.cursor = "ew-resize"
            event.preventDefault()
        })

        window.addEventListener("mousemove", { event ->
            if (!isDragging) return@addEventListener
            event as MouseEvent

            val dx = event.clientX - startX
            currentSeparator?.style?.transform = "translateX(${dx}px)"

            // Nur bytegroups auf gleicher Zeile (Y-Position)
            val byteGroups = document.querySelectorAll(".bytegroup")
            for (j in 0 until byteGroups.length) {
                val bg = byteGroups[j] as HTMLElement
                val rect = bg.getBoundingClientRect()

                val sameLine = kotlin.math.abs(rect.top - separatorTop) < 10 // tolerance in px
                if (!sameLine) continue

                val tolerance = rect.width * 0.3
                val within = event.clientX >= rect.left - tolerance && event.clientX <= rect.right + tolerance

                if (within) {
                    hoverTarget?.let { it.classList.remove("highlightbyte") }
                    hoverTarget = bg
                    bg.classList.add("highlightbyte")
                    break
                }
            }
        })

        window.addEventListener("mouseup", { event ->
            if (!isDragging) return@addEventListener
            isDragging = false
            event as MouseEvent
            document.body?.style?.cursor = "default"

            val dx = event.clientX - startX
            currentSeparator?.style?.transform = "translateX(0px)"
            currentSeparator?.style?.zIndex = "10"

            val target = hoverTarget
            val separator = currentSeparator

            if (separator != null && kotlin.math.abs(dx) < 3) {
                // Nur Klick → sanft entfernen
                separator.classList.add("remove-animate")
                console.log("Soft-deleting separator...")

                window.setTimeout({
                    separator.parentElement?.removeChild(separator)
                    console.log("Separator removed after animation")
                }, 200)
            } else if (target != null && separator != null) {
                val parent = separator.parentElement!!
                val targetParent = target.parentElement!!

                if (parent == targetParent) {
                    parent.removeChild(separator)
                    if (target.nextSibling != null) {
                        parent.insertBefore(separator, target.nextSibling)
                    } else {
                        parent.appendChild(separator)
                    }
                    console.log("Separator moved to new position")
                }
            }

            hoverTarget?.let { it.classList.remove("highlightbyte") }
            hoverTarget = null
            currentSeparator = null
        })
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
        if(listOf("asn1", "protobuf", "generic", "bplist", "nsarchive", "opack", "nemesys").any { element.classList.contains(it) }) {
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
    return (0 until byteArray.length).joinToString("") { index ->
        byteArray.asDynamic()[index].toString(16).padStart(2, '0')
    }
}