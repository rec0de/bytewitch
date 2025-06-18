import bitmage.hex
import decoders.Nemesys.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.events.MouseEvent

import org.w3c.dom.events.Event


var liveDecodeEnabled = true
var currentHighlight: Element? = null

// save parsed messages for float view and nemesys
var parsedMessages = mutableMapOf<Int, NemesysParsedMessage>()

// global values for SequenceAlignment listeners
val alignmentMouseEnterListeners = mutableMapOf<String, (Event) -> Unit>()
val alignmentMouseLeaveListeners = mutableMapOf<String, (Event) -> Unit>()

fun main() {
    window.addEventListener("load", {
        val dataContainer = document.getElementById("data_container")!!
        val decodeBtn = document.getElementById("decode") as HTMLButtonElement
        val tryhardBtn = document.getElementById("tryhard") as HTMLButtonElement
        val uploadBtn = document.getElementById("upload") as HTMLButtonElement
        val addDataBox = document.getElementById("add_data") as HTMLElement
        val deleteDataBox = document.getElementById("delete_data") as HTMLElement

        val liveDecode = document.getElementById("livedecode") as HTMLInputElement
        liveDecodeEnabled = liveDecode.checked

        // input listener for text areas
        applyLiveDecodeListeners()

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
            fileInput.multiple = true // to upload multiple files

            fileInput.onchange = {
                val files = fileInput.files
                if (files != null) {
                    for (i in 0 until files.length) {
                        val file = files.item(i)
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
                }
            }

            // Trigger the file selection dialog
            fileInput.click()
        }

        // to add more text areas for protocols
        addDataBox.onclick = {
            val newTextarea = document.createElement("textarea") as HTMLTextAreaElement
            newTextarea.className = "data input_area"
            dataContainer.appendChild(newTextarea)

            // for live decode
            if (liveDecodeEnabled) {
                newTextarea.oninput = {
                    decode(false)
                }
            }
        }

        // to delete last text area
        deleteDataBox.onclick = {
            if (dataContainer.children.length > 1) { // there need to be at least one data container left
                removeTextArea(dataContainer)
            }
        }

        liveDecode.onchange = {
            liveDecodeEnabled = liveDecode.checked
            applyLiveDecodeListeners()
            0.0
        }
    })
}

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
                decode(false)
        }
    }
}

/*fun getTestingData(): MutableMap<Int, NemesysParsedMessage> {
    val message1 = "62706c6973743030d30102030405065173516651625c636f6d2e6b696b2e636861741105ffa107d208090a0b516851645f102036366137626435396665376639613763323265623436336436646233393730342341d95c3031cf51a2080f1113152225272c2e30530000000000000101000000000000000c0000000000000000000000000000005c".fromHex()
    // val message2 = "A76269641870686E6163686E616D65674E65756D616E6E68757365726E616D656A6E65756D616E6E78587865656D61696C726E65756D616E6E406F75746C6F6F6B2E646566686F62627973A266686F62627931684675C39F62616C6C66686F626279326A4261736B657462616C6C6770726F66696C65A2636167651267636F756E7472796B446575747363686C616E646969735F616374697665F5".fromHex()

    val segments1 = listOf(
        NemesysSegment(0, NemesysField.UNKNOWN),     // Start der BPList
        NemesysSegment(8, NemesysField.UNKNOWN),     // Start des Root-Objekts (Dictionary)
        NemesysSegment(15, NemesysField.UNKNOWN),    // "s"
        NemesysSegment(17, NemesysField.UNKNOWN),    // "f"
        NemesysSegment(19, NemesysField.UNKNOWN),    // "b"
        NemesysSegment(21, NemesysField.UNKNOWN),    // "com.kik.chat"
        NemesysSegment(34, NemesysField.UNKNOWN),    // 1535
        NemesysSegment(37, NemesysField.UNKNOWN),    // Start des Arrays
        NemesysSegment(39, NemesysField.UNKNOWN),    // Start des Dictionary-Objekts im Array
        NemesysSegment(44, NemesysField.UNKNOWN),    // "h"
        NemesysSegment(46, NemesysField.UNKNOWN),    // "d"
        NemesysSegment(48, NemesysField.UNKNOWN),    // "66a7bd59fe7f9a7c22eb463d6db39704"
        NemesysSegment(83, NemesysField.UNKNOWN),
    )

    val segments2 = listOf(
        NemesysSegment(0, NemesysField.UNKNOWN),     // Start des Objekts
        NemesysSegment(1, NemesysField.UNKNOWN),     // "id"
        NemesysSegment(4, NemesysField.UNKNOWN),     // 112
        NemesysSegment(6, NemesysField.UNKNOWN),     // "nachname"
        NemesysSegment(15, NemesysField.UNKNOWN),    // "Neumann"
        NemesysSegment(23, NemesysField.UNKNOWN),    // "username"
        NemesysSegment(32, NemesysField.UNKNOWN),    // "neumannxXx"
        NemesysSegment(43, NemesysField.UNKNOWN),    // "email"
        NemesysSegment(49, NemesysField.UNKNOWN),    // "neumann@outlook.de"
        NemesysSegment(68, NemesysField.UNKNOWN),    // "hobbys"
        NemesysSegment(75, NemesysField.UNKNOWN),    // verschachteltes "hobbys"-Objekt
        NemesysSegment(76, NemesysField.UNKNOWN),    // "hobby1"
        NemesysSegment(83, NemesysField.UNKNOWN),    // "Fußball"
        NemesysSegment(92, NemesysField.UNKNOWN),    // "hobby2"
        NemesysSegment(99, NemesysField.UNKNOWN),    // "Basketball"
        NemesysSegment(110, NemesysField.UNKNOWN),   // "profile"
        NemesysSegment(118, NemesysField.UNKNOWN),   // verschachteltes "profile"-Objekt
        NemesysSegment(119, NemesysField.UNKNOWN),   // "age"
        NemesysSegment(123, NemesysField.UNKNOWN),   // 18
        NemesysSegment(124, NemesysField.UNKNOWN),   // "country"
        NemesysSegment(132, NemesysField.UNKNOWN),   // "Deutschland"
        NemesysSegment(144, NemesysField.UNKNOWN),   // "is_active"
        NemesysSegment(154, NemesysField.UNKNOWN)    // true
    )

    val messages = mutableMapOf(
        0 to NemesysParsedMessage(segments1, message1, 0),
        // 1 to NemesysParsedMessage(segments2, message2, 1)
    )

    return messages
}

fun setupSelectableSegments() {
    val elements = document.querySelectorAll("[value-align-id]")
    for (i in 0 until elements.length) {
        val el = elements[i] as HTMLElement
        el.addEventListener("click", {
            toggleSegment(el)
        })
    }

    // start new group by pressing "n" on the keyboard
    document.addEventListener("keydown", { e ->
        if ((e as KeyboardEvent).key == "n") {
            if (currentGroup.size > 1) {
                selectedGroups.add(currentGroup.toMutableSet())
            }
            currentGroup.clear()
        }
    })
}

fun toggleSegment(el: HTMLElement) {
    val id = el.getAttribute("value-align-id") ?: return
    if (currentGroup.contains(id)) {
        currentGroup.remove(id)
        el.classList.remove("highlighted")
    } else {
        currentGroup.add(id)
        el.classList.add("highlighted")
    }
}

fun exportAlignments(): String {
    val triplets = mutableSetOf<String>()

    for (group in selectedGroups) {
        Logger.log("Group:")
        Logger.log(group)
        val items = group.map {
            val (msg, seg) = it.split("-").map { part -> part.toInt() }
            msg to seg
        }

        for (i in items.indices) {
            for (j in i + 1 until items.size) {
                val (msgA, segA) = items[i]
                val (msgB, segB) = items[j]
                triplets.add("Triple($msgA, $msgB, Pair($segA, $segB))")
            }
        }
    }

    return """val expectedAlignments = setOf(
        ${triplets.joinToString(",\n    ")}
    )"""
}*/


// decode one specific byte sequence
fun decodeBytes(tryhard: Boolean, bytes: ByteArray, taIndex: Int) {
    val output = document.getElementById("output") as HTMLDivElement
    val bytefinder = document.getElementById("bytefinder") as HTMLDivElement
    val floatview = document.getElementById("floatview") as HTMLDivElement
    val textview = document.getElementById("textview") as HTMLDivElement
    val noDecodeYet = document.getElementById("no_decode_yet") as HTMLElement

    // Reset output
    floatview.innerHTML = ""
    textview.innerHTML = ""
    bytefinder.style.display = "none"
    noDecodeYet.style.display = "none"

    // decode input
    val result = ByteWitch.analyze(bytes, tryhard)

    if (result.isNotEmpty()) {
        bytefinder.style.display = "flex"

        // check if message-output container already exists
        val messageId = "message-output-$taIndex"
        var messageBox = document.getElementById(messageId) as? HTMLDivElement

        if (messageBox == null) {
            messageBox = document.createElement("DIV") as HTMLDivElement
            messageBox.id = messageId
            messageBox.classList.add("message-output") // apply layout CSS
            output.appendChild(messageBox)
        } else {
            messageBox.innerHTML = "" // clear old content
        }

        result.forEach {
            val parseResult = document.createElement("DIV") as HTMLDivElement

            val parseName = document.createElement("H3") as HTMLHeadingElement
            parseName.innerText = it.first

            val parseContent = document.createElement("DIV") as HTMLDivElement
            parseContent.classList.add("parsecontent")
            parseContent.innerHTML = it.second.renderHTML()

            attachRangeListeners(parseContent, taIndex)

            parseResult.appendChild(parseName)
            parseResult.appendChild(parseContent)
            messageBox.appendChild(parseResult)
        }

        // for nemesys (and float view)
        val nemesysParsed = NemesysParser().parse(bytes, taIndex)
        parsedMessages[taIndex] = nemesysParsed // besides nemesys this is also needed for the float view

        val nemesysResult = document.createElement("DIV") as HTMLDivElement
        val nemesysName = document.createElement("H3") as HTMLHeadingElement
        nemesysName.innerText = "nemesysparser"

        val nemesysContent = document.createElement("DIV") as HTMLDivElement
        nemesysContent.classList.add("parsecontent")
        nemesysContent.innerHTML = NemesysRenderer.render(nemesysParsed)

        attachRangeListeners(nemesysContent, taIndex)
        attachNemesysButtons(nemesysContent, bytes, taIndex)

        nemesysResult.appendChild(nemesysName)
        nemesysResult.appendChild(nemesysContent)
        messageBox.appendChild(nemesysResult)
    }
}

// decode all text areas
fun decode(tryhard: Boolean) {
    val textareas = document.querySelectorAll(".input_area")
    for (i in 0 until textareas.length) {
        // get bytes from textarea
        val textarea = textareas[i] as HTMLTextAreaElement
        val inputText = textarea.value.trim()
        val bytes = ByteWitch.getBytesFromInputEncoding(inputText)

        // only decode text area if input changed
        val oldBytes = parsedMessages[i]?.bytes
        if (oldBytes == null || !oldBytes.contentEquals(bytes)) {
            decodeBytes(tryhard, bytes, i)
        }
    }

    // refine nemesys fields and rerender html content
    val refined = NemesysParser().refineSegmentsAcrossMessages(parsedMessages.values.toList())
    refined.forEach { msg ->
        parsedMessages[msg.msgIndex] = msg
        rerenderNemesys(msg.msgIndex, msg)
    }

    // for sequence alignment
    val alignedSegment = NemesysSequenceAlignment.alignSegments(parsedMessages)
    attachSequenceAlignmentListeners(alignedSegment)



    // TODO for testing purposes only
    // includeAlignmentForTesting()
}

/*fun includeAlignmentForTesting() {
    val output = document.getElementById("output") as HTMLDivElement
    val testingMessages = getTestingData()
    val messageBox = document.createElement("DIV") as HTMLDivElement
    messageBox.classList.add("message-output")
    for ((index, message) in testingMessages) {
        val nemesysResult = document.createElement("DIV") as HTMLDivElement
        val nemesysName = document.createElement("H3") as HTMLHeadingElement
        nemesysName.innerText = "nemesysparser $index"

        val nemesysContent = document.createElement("DIV") as HTMLDivElement
        nemesysContent.classList.add("parsecontent")

        if (message != null) {
            nemesysContent.innerHTML = NemesysRenderer.render(message)
        } else {
            nemesysContent.innerText = "Error: message $index is null"
        }

        nemesysResult.appendChild(nemesysName)
        nemesysResult.appendChild(nemesysContent)
        messageBox.appendChild(nemesysResult)
        output.appendChild(messageBox)
    }
    setupSelectableSegments()
    val btn = document.createElement("button") as HTMLElement
    btn.innerText = "Export Alignments"
    btn.onclick = {
        console.log(exportAlignments())
    }
    document.body?.appendChild(btn)
}*/

// rerender nemesys html view
fun rerenderNemesys(msgIndex: Int, parsed: NemesysParsedMessage) {
    val messageBox = document.getElementById("message-output-$msgIndex") as HTMLDivElement
    val oldWrapper = messageBox.querySelector(".nemesys") as HTMLDivElement

    // create new div with new nemesys content
    val temp = document.createElement("div") as HTMLDivElement
    val newHTML = NemesysRenderer.render(parsed)
    temp.innerHTML = newHTML

    val newWrapper = temp.firstElementChild as HTMLElement
    oldWrapper.replaceWith(newWrapper)

    // attach javascript handlers
    attachRangeListeners(newWrapper, msgIndex)
    attachEditButtonHandler(newWrapper)
    attachFinishButtonHandler(newWrapper, parsed.bytes, msgIndex)
}

// extract segment given the protocol id and segment id
fun extractBytes(protocol: Int, index: Int): ByteArray? { // TODO use function in NemesysUtil instead of this one
    val msg = parsedMessages[protocol] ?: return null
    val start = msg.segments.getOrNull(index)?.offset ?: return null
    val end = msg.segments.getOrNull(index + 1)?.offset ?: msg.bytes.size
    return msg.bytes.copyOfRange(start, end)
}

// attach sequence alignment listeners
fun attachSequenceAlignmentListeners(alignedSegments: List<AlignedSegment>) {
    // remove old sequence alignment listeners
    removeAllSequenceAlignmentListeners()

    // group all aligned segments
    // for example: if AlignedSegment(0, 1, 3, 2, 0.05) is given
    // then create add alignmentGroups["0-3"] = {"1-2", "0-3"} and alignmentGroups["1-2"] = {"0-3", "1-2"}
    val alignmentGroups = mutableMapOf<String, MutableSet<String>>()
    val alignmentColors = mutableMapOf<String, Triple<Float, Float, Float>>() // safe highlighting color
    val alignmentBytes = mutableMapOf<String, ByteArray>() // save byte segment of corresponding id
    for (segment in alignedSegments) {
        val idA = "${segment.protocolA}-${segment.segmentIndexA}"
        val idB = "${segment.protocolB}-${segment.segmentIndexB}"

        alignmentGroups.getOrPut(idA) { mutableSetOf() }.add(idB)
        alignmentGroups.getOrPut(idB) { mutableSetOf() }.add(idA)
        alignmentGroups[idA]!!.add(idA)
        alignmentGroups[idB]!!.add(idB)

        alignmentBytes[idA] = extractBytes(segment.protocolA, segment.segmentIndexA) ?: continue
        alignmentBytes[idB] = extractBytes(segment.protocolB, segment.segmentIndexB) ?: continue
    }

    // go through all groups and save color with the lowest difference to the nearest aligned segment
    for ((id, group) in alignmentGroups) {
        for (entry in group) {
            val thisBytes = alignmentBytes[entry] ?: continue

            val minDiff = group
                .filter { it != entry }
                .mapNotNull { other -> alignmentBytes[other]?.let { byteDistance(thisBytes, it) } }
                .minOrNull() ?: 1.0

            alignmentColors[entry] = getHslColorForDifference(minDiff)
        }
    }


    // set up event listeners for every value-align-id
    for (id in alignmentGroups.keys) {
        val el = document.querySelector("[value-align-id='${id}']") as? HTMLElement ?: continue

        // set style for all aligned elements
        val mouseEnterHandler: (Event) -> Unit = {
            alignmentGroups[id]?.forEach { linkedId ->
                val elements = document.querySelectorAll("[value-align-id='${linkedId}']")

                // set style
                val (h, s, l) = alignmentColors[linkedId] ?: Triple(0, 0, 1)
                for (i in 0 until elements.length) {
                    (elements[i] as HTMLElement).classList.add("hovered-alignment")
                    (elements[i] as HTMLElement).setAttribute("style", "background-color: hsla($h, $s%, $l%, 0.3);")
                }
            }
        }

        // remove styles after hovering
        val mouseLeaveHandler: (Event) -> Unit = {
            alignmentGroups[id]?.forEach { linkedId ->
                val elements = document.querySelectorAll("[value-align-id='${linkedId}']")
                for (i in 0 until elements.length) {
                    (elements[i] as HTMLElement).classList.remove("hovered-alignment")
                    (elements[i] as HTMLElement).removeAttribute("style")
                }
            }
        }


        el.addEventListener("mouseenter", mouseEnterHandler)
        el.addEventListener("mouseleave", mouseLeaveHandler)

        alignmentMouseEnterListeners[id] = mouseEnterHandler
        alignmentMouseLeaveListeners[id] = mouseLeaveHandler
    }
}

// detect the difference between two byte segments
fun byteDistance(a: ByteArray, b: ByteArray): Double {
    if (a.size != b.size) return 1.0
    return a.indices.count { a[it] != b[it] }.toDouble() / a.size
}

// return colour based on the difference
/*fun getRgbColorForDifference(diff: Double): Triple<Int, Int, Int> {
    val clampedDiff = diff.coerceIn(0.0, 1.0)
    val r = (clampedDiff * 255).toInt()
    val g = ((1 - clampedDiff) * 255).toInt()
    return Triple(r, g, 0)
}*/

// return colour based on the difference - we use HSL because it looks more natural
fun getHslColorForDifference(diff: Double): Triple<Float, Float, Float> {
    val clampedDiff = diff.coerceIn(0.0, 1.0).toFloat()
    val hue = 120f * (1 - clampedDiff) // green (120°) at 0.0 → red (0°) at 1.0
    return Triple(hue, 100f, 50f)
}


// remove old sequence alignment listeners
fun removeAllSequenceAlignmentListeners() {
    for ((id, enterHandler) in alignmentMouseEnterListeners) {
        val elements = document.querySelectorAll("[value-align-id='${id}']")
        for (i in 0 until elements.length) {
            (elements[i] as HTMLElement).removeEventListener("mouseenter", enterHandler)
        }
    }
    for ((id, leaveHandler) in alignmentMouseLeaveListeners) {
        val elements = document.querySelectorAll("[value-align-id='${id}']")
        for (i in 0 until elements.length) {
            (elements[i] as HTMLElement).removeEventListener("mouseleave", leaveHandler)
        }
    }
    alignmentMouseEnterListeners.clear()
    alignmentMouseLeaveListeners.clear()
}



// attach button handlers for nemesys
fun attachNemesysButtons(parseContent: Element, bytes: ByteArray, msgIndex: Int) {
    attachEditButtonHandler(parseContent)
    attachFinishButtonHandler(parseContent, bytes, msgIndex)
}


// read out nemesys segments based on separators set by the user
fun rebuildSegmentsFromDOM(container: HTMLElement, msgIndex: Int): List<NemesysSegment> {
    val byteElements = container.querySelectorAll(".bytegroup + .field-separator, .bytegroup")
    var byteOffset = 0
    val segmentOffsets = mutableListOf(0)

    for (i in 0 until byteElements.length) {
        val el = byteElements[i] as HTMLElement

        if (el.classList.contains("bytegroup")) {
            byteOffset += 1 // every bytegroup = 2 Hex = 1 Byte
        }

        if (el.classList.contains("field-separator")) {
            segmentOffsets.add(byteOffset)
        }
    }


    // an alternative would be to check the offset of the previous message. Based on this decide if we want to keep the field type or set it to unknown
    // In my opinion this behaves a bit weird and isn't the best solution.
    // Another way would be to just rerun a field type detection. This also behaves weird
    return segmentOffsets.map { offset ->
        NemesysSegment(offset, NemesysField.UNKNOWN)
    }
}

// attach finish button handler for editable nemesys content
fun attachFinishButtonHandler(container: Element, originalBytes: ByteArray, msgIndex: Int) {
    container.querySelectorAll(".finish-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLElement
        button.addEventListener("click", {
            val oldWrapper = button.closest(".nemesys") as HTMLElement
            val byteContainer = oldWrapper.querySelector("#byteContainer") as HTMLElement

            // read out where to start in the byte sequence. This is important for the offset
            val dataStart = oldWrapper.getAttribute("data-start")?.toIntOrNull() ?: 0
            val dataEnd = oldWrapper.getAttribute("data-end")?.toIntOrNull() ?: originalBytes.size
            val slicedBytes = originalBytes.sliceArray(dataStart until dataEnd)

            // read out new segment structure based on separators
            val newSegments = rebuildSegmentsFromDOM(byteContainer, msgIndex)

            val newParsed = NemesysParsedMessage(newSegments, slicedBytes, msgIndex)
            parsedMessages[msgIndex] = newParsed

            // render new html content
            rerenderNemesys(msgIndex, newParsed)

            // rerun sequence alignment
            val alignedSegment = NemesysSequenceAlignment.alignSegments(parsedMessages)
            attachSequenceAlignmentListeners(alignedSegment)
        })
    }
}

// if edit button is pressed show editableView and hide prettyView
fun attachEditButtonHandler(container: Element) {
    container.querySelectorAll(".edit-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLElement
        button.addEventListener("click", {
            val wrapper = button.closest(".nemesys") as HTMLElement
            val prettyView = wrapper.querySelector(".view-default") as HTMLElement
            val editableView = wrapper.querySelector(".view-editable") as HTMLElement

            // switch display mode of pretty and editable view
            prettyView.style.display = "none"
            editableView.style.display = "block"

            // this is needed to work with the separator
            attachNemesysSeparatorHandlers()
            attachSeparatorPlaceholderClickHandlers()
        })
    }
}

// separator handler to change boundaries of nemesys content
fun attachNemesysSeparatorHandlers() {
    // get all separators
    val separators = document.querySelectorAll(".field-separator")

    for (i in 0 until separators.length) {
        val separator = separators[i] as HTMLElement

        var isDragging = false // to check if the user clicks on a separator right now
        var startX = 0.0 // save x position of dragged separator
        var startY = 0.0 // save y position of dragged separator
        var offsetX = 0.0 // needed to move separator with the cursor
        var offsetY = 0.0 // needed to move separator with the cursor
        var currentSeparator: HTMLElement? = null // the separator that is currently pressed by the user
        var hoverTarget: HTMLElement? = null // the actual bytegroup that is hovered by the mouse with the separator. This determines the target position

        var clickStartTime = 0.0 // count click time to interpret is as deleted

        // start dragging separator when mouse is pressed down. remember separator, start position, ...
        separator.addEventListener("mousedown", { event ->
            event as MouseEvent
            isDragging = true
            currentSeparator = separator
            startX = event.clientX.toDouble()
            startY = event.clientY.toDouble()
            clickStartTime = window.performance.now() // remember start time

            val rect = separator.getBoundingClientRect()
            val parentRect = separator.offsetParent?.getBoundingClientRect() ?: rect

            offsetX = startX - rect.left
            offsetY = startY - rect.top

            // set separator style for dragging
            separator.classList.add("dragging")
            separator.style.position = "absolute"
            separator.style.zIndex = "1000"
            separator.style.width = "${rect.width}px"
            separator.style.height = "${rect.height}px"
            separator.style.left = "${rect.left - parentRect.left}px"
            separator.style.top = "${rect.top - parentRect.top}px"

            document.body?.style?.cursor = "move"

            event.preventDefault()
        })

        // track separator movement and highlight potential drop target
        window.addEventListener("mousemove", { event ->
            if (!isDragging) return@addEventListener // do nothing if no separator is selected
            event as MouseEvent

            val parentRect = currentSeparator?.offsetParent?.getBoundingClientRect()
            if (parentRect != null) {
                val newX = event.clientX - parentRect.left - offsetX
                val newY = event.clientY - parentRect.top - offsetY
                currentSeparator?.style?.left = "${newX}px"
                currentSeparator?.style?.top = "${newY}px"
            }

            // find new target position
            val byteGroups = document.querySelectorAll(".bytegroup")
            for (j in 0 until byteGroups.length) { // go through all byte groups
                val bg = byteGroups[j] as HTMLElement
                val rect = bg.getBoundingClientRect()
                val withinX = event.clientX >= rect.left && event.clientX <= rect.right
                val withinY = event.clientY >= rect.top && event.clientY <= rect.bottom

                // check if mouse is over current byte group
                if (withinX && withinY) {
                    // update bytegroup to highlightbyte and remove it from the last one
                    hoverTarget?.classList?.remove("highlightbyte")
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

            val clickEndTime = window.performance.now()
            val timeDiff = clickEndTime - clickStartTime

            val dx = event.clientX - startX
            val dy = event.clientY - startY
            val totalMovement = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())

            val separator = currentSeparator
            val target = hoverTarget

            // reset separator style after moving it
            separator?.classList?.remove("dragging")
            separator?.style?.position = ""
            separator?.style?.zIndex = ""
            separator?.style?.width = ""
            separator?.style?.height = ""
            separator?.style?.left = ""
            separator?.style?.top = ""

            // check how far the separator has been moved. 3 is just a threshold in px
            // if (separator != null && totalMovement < 3) {
            // check if it's just a short click or a long movement
            if (separator != null && timeDiff < 200 && totalMovement < 3) {
                // delete if it was just a click and replace it with a separator-placeholder
                val placeholder = document.createElement("div") as HTMLElement
                placeholder.className = "separator-placeholder"
                separator.parentElement?.replaceChild(placeholder, separator)
                attachSeparatorPlaceholderClickHandlers()
            } else if (separator != null && target != null) {
                // move if it was dragged over a valid target
                moveSeparatorToTarget(separator, target, event.clientX.toDouble())
            }

            hoverTarget?.classList?.remove("highlightbyte")
            hoverTarget = null
            currentSeparator = null
        })
    }
}

// move separator to specific target element
// separator is the separator that we wat to move
// target is the byte group that was last hovered by the mouse. this determines the target byte
// mouseX is needed to check if we want to move the separator on the left or right side of the target
fun moveSeparatorToTarget(separator: HTMLElement, target: HTMLElement, mouseX: Double) {
    val parent = separator.parentElement ?: return
    val targetParent = target.parentElement ?: return

    // check that separator hasn't moved into another editable nemesys field
    if (parent == targetParent) {
        // calc target position
        val rect = target.getBoundingClientRect()
        val insertBefore = mouseX < rect.left + rect.width / 2
        val targetSibling = if (insertBefore) target.previousElementSibling else target.nextElementSibling // targetSibling = should be placeholder

        // return if no placeholder exists at target position (e.g. end of sequence, separator hasn't moved, is already another field-separator)
        if (targetSibling !is HTMLElement || !targetSibling.classList.contains("separator-placeholder")) return

        // replace old separator with placeholder
        val placeholderClone = document.createElement("div") as HTMLElement
        placeholderClone.className = "separator-placeholder"
        parent.replaceChild(placeholderClone, separator)

        // replace target placeholder with separator
        targetParent.replaceChild(separator, targetSibling)

        attachSeparatorPlaceholderClickHandlers()
    }
}

// replace placeholder with separator if clicked
fun attachSeparatorPlaceholderClickHandlers() {
    val placeholders = document.querySelectorAll(".separator-placeholder")

    for (i in 0 until placeholders.length) {
        val placeholder = placeholders[i] as HTMLElement
        placeholder.addEventListener("click", {
            val fieldSeparator = document.createElement("div") as HTMLElement
            fieldSeparator.className = "field-separator"
            fieldSeparator.innerText = "|"

            placeholder.parentElement?.replaceChild(fieldSeparator, placeholder)
            attachNemesysSeparatorHandlers()
            attachSeparatorPlaceholderClickHandlers()
        })
    }
}


// set bytes in floatview and textview
fun setByteFinderContent(msgIndex: Int) {
    val bytes = parsedMessages[msgIndex]?.bytes ?: return

    val floatview = document.getElementById("floatview") as HTMLDivElement
    val textview = document.getElementById("textview") as HTMLDivElement
    val bytefinder = document.getElementById("bytefinder") as HTMLDivElement

    floatview.innerText = bytes.hex().chunked(16).joinToString(" ")
    textview.innerHTML = bytes.map { it.toInt().toChar() }.map { if(it.code in 32..59 || it.code in 64..90 || it.code in 97..122) it else '.' }.joinToString("")
    bytefinder.style.display = "flex"
}

// set bytes in floatview and textview and highlight segment
fun setByteFinderHighlight(start: Int, end: Int, msgIndex: Int) {
    val floatview = document.getElementById("floatview")!!

    // set byte sequence
    setByteFinderContent(msgIndex)

    // apply highlighting in floatview
    val range = document.createRange()
    val text = floatview.childNodes[0]!!
    range.setStart(text, start*2 + start/8)
    range.setEnd(text, end*2 + end/8)
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
        if (listOf("asn1", "protobuf", "generic", "bplist", "nsarchive", "opack", "nemesys").any { element.classList.contains(it) }) {
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

// read binary file and add content to textarea
fun readBinaryFile(file: File) {
    val reader = FileReader()

    reader.onload = {
        val arrayBuffer = reader.result as? ArrayBuffer
        if (arrayBuffer != null) {
            val hexContent = arrayBufferToHex(arrayBuffer) // Convert binary data to hex
            // Display hex content in the textarea
            appendTextareaWithContent(hexContent)
        } else {
            console.error("Failed to read binary file content")
        }
    }

    reader.onerror = {
        console.error("Failed to read the file: ${reader.error?.message}")
    }

    reader.readAsArrayBuffer(file) // Read binary data in the file
}

// read txt file and append to textarea
fun readFile(file: File) {
    val reader = FileReader()

    reader.onload = {
        val content = reader.result?.toString() // Safely convert `result` to a string
        if (content != null) {
            // Write the file content to the textarea
            appendTextareaWithContent(content)
        } else {
            console.error("File content is null")
        }
    }

    reader.onerror = {
        console.error("Failed to read the file: ${reader.error?.message}")
    }

    reader.readAsText(file) // Read the file content as text
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
            decode(false)
        }
    }
}

fun arrayBufferToHex(buffer: ArrayBuffer): String {
    val byteArray = Uint8Array(buffer) // Create a Uint8Array view for the buffer
    val dynamic = byteArray.asDynamic()
    return (0 until byteArray.length).joinToString("") { index ->
        val b16string = dynamic[index].toString(16) as String
        b16string.padStart(2, '0')
    }
}