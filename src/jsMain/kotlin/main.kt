import bitmage.hex
import decoders.NemesysField
import decoders.NemesysObject
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

            attachNemesysButtons(parseContent, bytes)

            parseContent.children.asList().forEach { child ->
                attachRangeListeners(child)
            }

            parseResult.appendChild(parseName)
            parseResult.appendChild(parseContent)

            output.appendChild(parseResult)
        }
    }
}

// attach button handlers for nemesys
fun attachNemesysButtons(parseContent: Element, bytes: ByteArray) {
    attachEditButtonHandler(parseContent)
    attachFinishButtonHandler(parseContent, bytes)
}


// read out nemesys segments based on separators set by the user
fun rebuildSegmentsFromDOM(container: HTMLElement): List<Pair<Int, NemesysField>> {
    val byteElements = container.querySelectorAll(".bytegroup + .field-separator, .bytegroup")
    var byteOffset = 0
    val segmentOffsets = mutableListOf(0)

    for (i in 0 until byteElements.length) {
        val el = byteElements[i] as HTMLElement

        if (el.classList.contains("bytegroup")) {
            byteOffset += 1 // every bytegroup = 1 Chunk = 1x 2 Hex = 1 Byte
        }

        if (el.classList.contains("field-separator")) {
            segmentOffsets.add(byteOffset)
        }
    }

    val segments = segmentOffsets.mapIndexed { index, offset ->
        val fieldType = NemesysField.UNKNOWN // set to field type unknown
        offset to fieldType
    }

    return segments
}

// attach finish button handler for editable nemesys content
fun attachFinishButtonHandler(container: Element, originalBytes: ByteArray) {
    container.querySelectorAll(".finish-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLButtonElement
        button.addEventListener("click", {
            val oldWrapper = button.closest(".nemesys") as HTMLElement
            val byteContainer = oldWrapper.querySelector("#byteContainer") as HTMLElement

            // read out where to start in the byte sequence. This is important for the offset
            val dataStart = oldWrapper.getAttribute("data-start")?.toIntOrNull() ?: 0
            val dataEnd = oldWrapper.getAttribute("data-end")?.toIntOrNull() ?: originalBytes.size
            val slicedBytes = originalBytes.sliceArray(dataStart until dataEnd)

            // read out new segment structure based on separators
            val newSegments = rebuildSegmentsFromDOM(byteContainer)

            // create new nemesys object with new segments
            val newObject = NemesysObject(newSegments, slicedBytes, dataStart to dataEnd)
            val newHTML = newObject.renderHTML()

            val temp = document.createElement("div") as HTMLDivElement
            temp.innerHTML = newHTML

            val newWrapper = temp.firstElementChild as? HTMLElement
            if (newWrapper == null) {
                console.error("Newly rendered .nemesys could not be parsed")
                return@addEventListener
            }

            // replace old wrapper div
            oldWrapper.replaceWith(newWrapper)

            // attach new button handlers
            attachRangeListeners(newWrapper)
            attachEditButtonHandler(newWrapper)
            attachFinishButtonHandler(newWrapper, originalBytes)
        })
    }
}

// if edit button is pressed show editableView and hide prettyView
fun attachEditButtonHandler(container: Element) {
    container.querySelectorAll(".edit-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLButtonElement
        button.addEventListener("click", {
            val wrapper = button.closest(".nemesys") as HTMLElement
            val prettyView = wrapper.querySelector(".view-default") as HTMLElement
            val editableView = wrapper.querySelector(".view-editable") as HTMLElement

            // switch display mode of pretty and editable view
            prettyView.style.display = "none"
            editableView.style.display = "block"
            button.style.display = "none"

            // this is needed to work with the separator
            attachNemesysSeparatorHandlers()
        })
    }
}

fun attachNemesysSeparatorHandlers() {
    // get all separators
    val separators = document.querySelectorAll(".field-separator")

    for (i in 0 until separators.length) {
        val separator = separators[i] as HTMLElement

        var isDragging = false // to check if the user clicks on a separator right now
        var startX = 0.0 // save x position of dragged separator
        var currentSeparator: HTMLElement? = null // the separator that is currently pressed by the user
        var hoverTarget: HTMLElement? = null // the actual bytegroup that is hovered by the mouse with the separator
        var separatorTop: Double = 0.0 // needed to make sure that separator is only moved in the same line

        // start dragging separator when mouse is pressed down. remember separator, start position, ...
        separator.addEventListener("mousedown", { event ->
            event as MouseEvent
            isDragging = true
            startX = event.clientX.toDouble()
            separatorTop = separator.getBoundingClientRect().top
            currentSeparator = separator
            separator.style.zIndex = "100"
            separator.style.position = "relative"
            document.body?.style?.cursor = "ew-resize" // change cursor of mouse
            event.preventDefault()
        })

        // track separator movement and highlight potential drop target
        window.addEventListener("mousemove", { event ->
            if (!isDragging) return@addEventListener // do nothing if no separator is selected
            event as MouseEvent

            val dx = event.clientX - startX
            currentSeparator?.style?.transform = "translateX(${dx}px)"

            // only look at bytegroups on the same line (y-position)
            val byteGroups = document.querySelectorAll(".bytegroup")
            for (j in 0 until byteGroups.length) {
                val bg = byteGroups[j] as HTMLElement
                val rect = bg.getBoundingClientRect()

                val sameLine = kotlin.math.abs(rect.top - separatorTop) < 10 // tolerance in px
                if (!sameLine) continue

                val tolerance = rect.width * 0.3
                val within = event.clientX >= rect.left - tolerance && event.clientX <= rect.right + tolerance

                if (within) {
                    // update bytegroup to highlightbyte
                    hoverTarget?.let { it.classList.remove("highlightbyte") }
                    hoverTarget = bg
                    bg.classList.add("highlightbyte")
                    break
                }
            }
        })

        // drop or delete separator when mouse is released
        window.addEventListener("mouseup", { event ->
            if (!isDragging) return@addEventListener // return if no separator is selected
            isDragging = false
            event as MouseEvent
            document.body?.style?.cursor = "default"

            val dx = event.clientX - startX
            currentSeparator?.style?.transform = "translateX(0px)"
            currentSeparator?.style?.zIndex = "10"

            val target = hoverTarget
            val separator = currentSeparator

            // check how far the separator has been moved. 3 is just a threshold in px
            if (separator != null && kotlin.math.abs(dx) < 3) {
                // delete if it was just a click (with a little animation)
                deleteSeparator(separator)
            } else if (target != null && separator != null) {
                // move if it was dragged over a valid target
                moveSeparatorToTarget(separator, target)

            }

            hoverTarget?.let { it.classList.remove("highlightbyte") }
            hoverTarget = null
            currentSeparator = null
        })
    }
}

// move separator to specific target element
fun moveSeparatorToTarget(separator: HTMLElement, target: HTMLElement) {
    val parent = separator.parentElement!!
    val targetParent = target.parentElement!!

    if (parent == targetParent) {
        parent.removeChild(separator)
        if (target.nextSibling != null) {
            parent.insertBefore(separator, target.nextSibling)
        } else {
            parent.appendChild(separator)
        }
    }
}

// delete separator with a smooth animation
fun deleteSeparator(separator: HTMLElement) {
    separator.classList.add("remove-animate")
    window.setTimeout({
        separator.parentElement?.removeChild(separator) }, 200)
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