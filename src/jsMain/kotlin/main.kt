import bitmage.fromHex
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
import org.w3c.dom.events.KeyboardEvent


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

fun getTestingData(): MutableMap<Int, NemesysParsedMessage> {
    val message1 = "0821b10132010c3f2f5cd941da0104080010009a02bf010a3b636f6d2e6170706c652e74656c6570686f6e797574696c69746965732e63616c6c7365727669636573642e4661636554696d6550726f766964657212084661636554696d651a6466696c653a2f2f2f707269766174652f7661722f636f6e7461696e6572732f42756e646c652f4170706c69636174696f6e2f30323639344631412d303138312d343031342d423036342d4536303938333636464431342f4661636554696d652e6170702f2002280130013801400048006803680278019a02de010a17636f6d2e6170706c652e636f726574656c6570686f6e79200228053001380040014801680278018a0107080212033131328a0107080212033131308a0107080212033131328a0107080212033131308a0107080212033931318a0107080212033131328a0107080212033030308a01060802120230388a0107080212033131308a0107080212033939398a0107080212033131388a0107080212033131398a0107080212033132308a0107080212033132328a0107080212033931318a0107080212033131328a0108080212042a3931318a010808021204233931319a02450a31636f6d2e6170706c652e74656c6570686f6e797574696c69746965732e63616c6c7365727669636573642e54696e43616e200128013001380040004800680368027800".fromHex()

    val segments1 = listOf(
        NemesysSegment(0, NemesysField.UNKNOWN),
        NemesysSegment(2, NemesysField.UNKNOWN),
        NemesysSegment(12, NemesysField.UNKNOWN),
        NemesysSegment(15, NemesysField.UNKNOWN),
        NemesysSegment(17, NemesysField.UNKNOWN),
        NemesysSegment(19, NemesysField.UNKNOWN),
        NemesysSegment(23, NemesysField.UNKNOWN),
        NemesysSegment(84, NemesysField.UNKNOWN),
        NemesysSegment(94, NemesysField.UNKNOWN),
        NemesysSegment(196, NemesysField.UNKNOWN),
        NemesysSegment(198, NemesysField.UNKNOWN),
        NemesysSegment(200, NemesysField.UNKNOWN),
        NemesysSegment(202, NemesysField.UNKNOWN),
        NemesysSegment(204, NemesysField.UNKNOWN),
        NemesysSegment(206, NemesysField.UNKNOWN),
        NemesysSegment(208, NemesysField.UNKNOWN),
        NemesysSegment(210, NemesysField.UNKNOWN),
        NemesysSegment(212, NemesysField.UNKNOWN),
        NemesysSegment(214, NemesysField.UNKNOWN),
        NemesysSegment(218, NemesysField.UNKNOWN),
        NemesysSegment(243, NemesysField.UNKNOWN),
        NemesysSegment(245, NemesysField.UNKNOWN),
        NemesysSegment(247, NemesysField.UNKNOWN),
        NemesysSegment(249, NemesysField.UNKNOWN),
        NemesysSegment(251, NemesysField.UNKNOWN),
        NemesysSegment(253, NemesysField.UNKNOWN),
        NemesysSegment(255, NemesysField.UNKNOWN),
        NemesysSegment(257, NemesysField.UNKNOWN),
        NemesysSegment(259, NemesysField.UNKNOWN),
        NemesysSegment(262, NemesysField.UNKNOWN),
        NemesysSegment(264, NemesysField.UNKNOWN),
        NemesysSegment(269, NemesysField.UNKNOWN),
        NemesysSegment(272, NemesysField.UNKNOWN),
        NemesysSegment(274, NemesysField.UNKNOWN),
        NemesysSegment(279, NemesysField.UNKNOWN),
        NemesysSegment(282, NemesysField.UNKNOWN),
        NemesysSegment(284, NemesysField.UNKNOWN),
        NemesysSegment(289, NemesysField.UNKNOWN),
        NemesysSegment(292, NemesysField.UNKNOWN),
        NemesysSegment(294, NemesysField.UNKNOWN),
        NemesysSegment(299, NemesysField.UNKNOWN),
        NemesysSegment(302, NemesysField.UNKNOWN),
        NemesysSegment(304, NemesysField.UNKNOWN),
        NemesysSegment(309, NemesysField.UNKNOWN),
        NemesysSegment(312, NemesysField.UNKNOWN),
        NemesysSegment(314, NemesysField.UNKNOWN),
        NemesysSegment(319, NemesysField.UNKNOWN),
        NemesysSegment(322, NemesysField.UNKNOWN),
        NemesysSegment(324, NemesysField.UNKNOWN),
        NemesysSegment(329, NemesysField.UNKNOWN),
        NemesysSegment(332, NemesysField.UNKNOWN),
        NemesysSegment(334, NemesysField.UNKNOWN),
        NemesysSegment(338, NemesysField.UNKNOWN),
        NemesysSegment(341, NemesysField.UNKNOWN),
        NemesysSegment(343, NemesysField.UNKNOWN),
        NemesysSegment(348, NemesysField.UNKNOWN),
        NemesysSegment(351, NemesysField.UNKNOWN),
        NemesysSegment(353, NemesysField.UNKNOWN),
        NemesysSegment(358, NemesysField.UNKNOWN),
        NemesysSegment(361, NemesysField.UNKNOWN),
        NemesysSegment(363, NemesysField.UNKNOWN),
        NemesysSegment(368, NemesysField.UNKNOWN),
        NemesysSegment(371, NemesysField.UNKNOWN),
        NemesysSegment(373, NemesysField.UNKNOWN),
        NemesysSegment(378, NemesysField.UNKNOWN),
        NemesysSegment(381, NemesysField.UNKNOWN),
        NemesysSegment(383, NemesysField.UNKNOWN),
        NemesysSegment(388, NemesysField.UNKNOWN),
        NemesysSegment(391, NemesysField.UNKNOWN),
        NemesysSegment(393, NemesysField.UNKNOWN),
        NemesysSegment(398, NemesysField.UNKNOWN),
        NemesysSegment(401, NemesysField.UNKNOWN),
        NemesysSegment(403, NemesysField.UNKNOWN),
        NemesysSegment(408, NemesysField.UNKNOWN),
        NemesysSegment(411, NemesysField.UNKNOWN),
        NemesysSegment(413, NemesysField.UNKNOWN),
        NemesysSegment(418, NemesysField.UNKNOWN),
        NemesysSegment(421, NemesysField.UNKNOWN),
        NemesysSegment(423, NemesysField.UNKNOWN),
        NemesysSegment(429, NemesysField.UNKNOWN),
        NemesysSegment(432, NemesysField.UNKNOWN),
        NemesysSegment(434, NemesysField.UNKNOWN),
        NemesysSegment(440, NemesysField.UNKNOWN),
        NemesysSegment(443, NemesysField.UNKNOWN),
        NemesysSegment(494, NemesysField.UNKNOWN),
        NemesysSegment(496, NemesysField.UNKNOWN),
        NemesysSegment(498, NemesysField.UNKNOWN),
        NemesysSegment(500, NemesysField.UNKNOWN),
        NemesysSegment(502, NemesysField.UNKNOWN),
        NemesysSegment(504, NemesysField.UNKNOWN),
        NemesysSegment(506, NemesysField.UNKNOWN),
        NemesysSegment(508, NemesysField.UNKNOWN),
        NemesysSegment(510, NemesysField.UNKNOWN)
    )



    val messages = mutableMapOf(
        0 to NemesysParsedMessage(segments1, message1, 0)
    )

    return messages
}



val selectedGroups = mutableListOf<MutableSet<String>>()
var currentGroup = mutableSetOf<String>()

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
}


fun decode(tryhard: Boolean) {
    val output = document.getElementById("output") as HTMLDivElement
    val floatview = document.getElementById("floatview") as HTMLDivElement
    val bytefinder = document.getElementById("bytefinder") as HTMLDivElement

    // Reset output
    output.innerHTML = ""
    floatview.innerHTML = ""
    bytefinder.style.display = "none"

    // decode all inputs
    val textareas = document.querySelectorAll(".input_area")
    for (i in 0 until textareas.length) {
        val textarea = textareas[i] as HTMLTextAreaElement
        val inputText = textarea.value.trim()
        if (inputText.isEmpty()) continue // only decode input text areas that are in use

        // decode input
        val bytes = ByteWitch.getBytesFromInputEncoding(inputText)
        val result = ByteWitch.analyze(bytes, tryhard)



        if (result.isNotEmpty()) {
            bytefinder.style.display = "flex"

            // create a container for this message
            val messageBox = document.createElement("DIV") as HTMLDivElement
            messageBox.classList.add("message-output")

            // TODO currently if an input changes it reloads all parser
            result.forEach {
                val parseResult = document.createElement("DIV") as HTMLDivElement

                val parseName = document.createElement("H3") as HTMLHeadingElement
                parseName.innerText = it.first

                val parseContent = document.createElement("DIV") as HTMLDivElement
                parseContent.classList.add("parsecontent")
                parseContent.innerHTML = it.second.renderHTML()

                attachRangeListeners(parseContent, i)

                parseResult.appendChild(parseName)
                parseResult.appendChild(parseContent)
                messageBox.appendChild(parseResult)
            }

            // for nemesys (and float view)
            val nemesysParsed = NemesysParser().parse(bytes, i)
            parsedMessages[i] = nemesysParsed // besides nemesys this is also needed for the float view

            val nemesysResult = document.createElement("DIV") as HTMLDivElement
            val nemesysName = document.createElement("H3") as HTMLHeadingElement
            nemesysName.innerText = "nemesysparser"

            val nemesysContent = document.createElement("DIV") as HTMLDivElement
            nemesysContent.classList.add("parsecontent")
            nemesysContent.innerHTML = NemesysRenderer.render(nemesysParsed)

            attachRangeListeners(nemesysContent, i)
            attachNemesysButtons(nemesysContent, bytes, i)

            nemesysResult.appendChild(nemesysName)
            nemesysResult.appendChild(nemesysContent)
            messageBox.appendChild(nemesysResult)

            output.appendChild(messageBox)
        }
    }

    // refine nemesys fields and rerender html content
    val refined = NemesysParser().refineSegmentsAcrossMessages(parsedMessages.values.toList())
    refined.forEach { msg ->
        parsedMessages[msg.msgIndex] = msg
        rerenderNemesys(msg.msgIndex, msg)
    }

    val alignedSegment = NemesysSequenceAlignment.alignSegments(parsedMessages)
    attachSequenceAlignmentListeners(alignedSegment)



    // TODO for testing purposes only
    includeAlignmentForTesting()

}

fun includeAlignmentForTesting() {
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
            nemesysContent.innerText = "Fehler: Nachricht $index ist null."
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
}

// rerender nemesys html view
fun rerenderNemesys(msgIndex: Int, parsed: NemesysParsedMessage) {
    val output = document.getElementById("output") as? HTMLDivElement ?: return
    val messageBox = output.children[msgIndex] as? HTMLDivElement ?: return
    val oldWrapper = messageBox.querySelector(".nemesys") as? HTMLElement ?: return

    val newHTML = NemesysRenderer.render(parsed)
    val temp = document.createElement("div") as HTMLDivElement
    temp.innerHTML = newHTML

    val newWrapper = temp.firstElementChild as? HTMLElement
    if (newWrapper == null) {
        console.error("Newly rendered .nemesys could not be parsed")
        return
    }

    oldWrapper.replaceWith(newWrapper)

    // attach javascript handlers
    attachRangeListeners(newWrapper, msgIndex)
    attachEditButtonHandler(newWrapper)
    attachFinishButtonHandler(newWrapper, parsed.bytes, msgIndex)
}


// attach sequence alignment listeners
fun attachSequenceAlignmentListeners(alignedSegments: List<AlignedSegment>) {
    // remove old sequence alignment listeners
    removeAllSequenceAlignmentListeners()

    // group all aligned segments
    // for example: if AlignedSegment(0, 1, 3, 2, 0.05) is given
    // then create add alignmentGroups["0-3"] = {"1-2", "0-3"} and alignmentGroups["1-2"] = {"0-3", "1-2"}
    val alignmentGroups = mutableMapOf<String, MutableSet<String>>()
    for (segment in alignedSegments) {
        val idA = "${segment.protocolA}-${segment.segmentIndexA}"
        val idB = "${segment.protocolB}-${segment.segmentIndexB}"

        alignmentGroups.getOrPut(idA) { mutableSetOf() }.add(idB)
        alignmentGroups.getOrPut(idB) { mutableSetOf() }.add(idA)
        alignmentGroups[idA]!!.add(idA)
        alignmentGroups[idB]!!.add(idB)
    }

    // set up event listeners for every value-align-id
    for (id in alignmentGroups.keys) {
        val el = document.querySelector("[value-align-id='${id}']") as? HTMLElement ?: continue

        val mouseEnterHandler: (Event) -> Unit = {
            alignmentGroups[id]?.forEach { linkedId ->
                val elements = document.querySelectorAll("[value-align-id='${linkedId}']")
                for (i in 0 until elements.length) {
                    (elements[i] as HTMLElement).classList.add("hovered-alignment")
                }
            }
        }
        val mouseLeaveHandler: (Event) -> Unit = {
            alignmentGroups[id]?.forEach { linkedId ->
                val elements = document.querySelectorAll("[value-align-id='${linkedId}']")
                for (i in 0 until elements.length) {
                    (elements[i] as HTMLElement).classList.remove("hovered-alignment")
                }
            }
        }

        el.addEventListener("mouseenter", mouseEnterHandler)
        el.addEventListener("mouseleave", mouseLeaveHandler)

        alignmentMouseEnterListeners[id] = mouseEnterHandler
        alignmentMouseLeaveListeners[id] = mouseLeaveHandler
    }
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
        var hoverTarget: HTMLElement? = null // the actual bytegroup that is hovered by the mouse with the separator

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

            // find new position
            val byteGroups = document.querySelectorAll(".bytegroup")
            for (j in 0 until byteGroups.length) {
                val bg = byteGroups[j] as HTMLElement
                val rect = bg.getBoundingClientRect()
                val withinX = event.clientX >= rect.left && event.clientX <= rect.right
                val withinY = event.clientY >= rect.top && event.clientY <= rect.bottom

                // update bytegroup to highlightbyte
                if (withinX && withinY) {
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



// attach range listener for float view
fun attachRangeListeners(element: Element, msgIndex: Int) {
    if (element.hasAttribute("data-start") && element.hasAttribute("data-end")) {
        val start = element.getAttribute("data-start")!!.toInt()
        val end = element.getAttribute("data-end")!!.toInt()

        element.addEventListener("click", { evt ->
            val floatview = document.getElementById("floatview")!!

            // set byte sequence
            val message = parsedMessages[msgIndex]?.bytes ?: return@addEventListener
            floatview.innerHTML = message.hex()

            // apply highlighting
            val range = document.createRange()
            range.setStart(floatview.firstChild!!, start * 2)
            range.setEnd(floatview.firstChild!!, end * 2)
            range.surroundContents(document.createElement("span"))

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