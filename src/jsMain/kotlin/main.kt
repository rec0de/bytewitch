import SequenceAlignment.SegmentWiseSequenceAlignment
import SequenceAlignment.ByteWiseSequenceAlignment
import decoders.ByteWitchResult
import decoders.SwiftSegFinder.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.HTMLTextAreaElement


var liveDecodeEnabled = true
var currentHighlight: Element? = null
var tryhard = false

// save parsed messages for float view and SwiftSegFinder
var parsedMessages = mutableMapOf<Int, SSFParsedMessage>()

// choose between segment- and byte-wise sequence alignment
var showSegmentWiseAlignment = true

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
            tryhard = false
            decode(false)
        }

        tryhardBtn.onclick = {
            tryhard = true
            decode(false)
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
            appendTextArea("")
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

// use entropy decoder and attach output to messageBox
fun decodeWithEntropy() { // TODO this can be removed
    val  parsedMessages = SSFParser().parseEntropy(parsedMessages.values.toList())

    parsedMessages.forEach {
        val messageId = "message-output-${it.msgIndex}"
        var messageBox = document.getElementById(messageId) as HTMLDivElement

        // remove old ssf content
        val existingParsers = messageBox.querySelectorAll("h3")
        for (i in 0 until existingParsers.length) {
            val heading = existingParsers.item(i) as? HTMLHeadingElement ?: continue
            if (heading.innerText.lowercase() == "SwiftSegFinder") {
                heading.parentElement?.remove()
                break
            }
        }

        // add new ssf content
        val ssfResult = document.createElement("DIV") as HTMLDivElement
        val ssfName = document.createElement("H3") as HTMLHeadingElement
        ssfName.innerText = "SwiftSegFinder"

        val ssfContent = document.createElement("DIV") as HTMLDivElement
        ssfContent.classList.add("parsecontent")
        // ssfContent.innerHTML = SSFRenderer.render(it)
        ssfContent.innerHTML = SSFRenderer.renderByteWiseHTML(it)

        attachRangeListeners(ssfContent, it.msgIndex)
        attachSSFButtons(ssfContent, it.bytes, it.msgIndex)

        ssfResult.appendChild(ssfName)
        ssfResult.appendChild(ssfContent)

        messageBox.appendChild(ssfResult)
    }
}


// decode one specific byte sequence
fun decodeBytes(bytes: ByteArray, taIndex: Int, showSSFContent: Boolean) {
    val output = document.getElementById("output") as HTMLDivElement
    val bytefinder = document.getElementById("bytefinder") as HTMLDivElement
    val hexview = document.getElementById("hexview") as HTMLDivElement
    val textview = document.getElementById("textview") as HTMLDivElement
    val noDecodeYet = document.getElementById("no_decode_yet") as HTMLElement

    // Reset output
    hexview.innerHTML = ""
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
            messageBox.appendChild(renderByteWitchResult(it, taIndex))
        }

        // for SSF content
        if (showSSFContent) {
            messageBox.appendChild(decodeWithSSF(bytes, taIndex))
        }
    }
}

// render result of byte witch decoder
private fun renderByteWitchResult(it: Pair<String, ByteWitchResult>, taIndex: Int): HTMLDivElement {
    val parseResult = document.createElement("DIV") as HTMLDivElement

    val parseName = document.createElement("H3") as HTMLHeadingElement
    parseName.innerText = it.first

    val parseContent = document.createElement("DIV") as HTMLDivElement
    parseContent.classList.add("parsecontent")
    parseContent.innerHTML = it.second.renderHTML()

    attachRangeListeners(parseContent, taIndex)

    parseResult.appendChild(parseName)
    parseResult.appendChild(parseContent)

    return parseResult
}

// decode bytes with SwiftSegFinder and return HTML content
private fun decodeWithSSF(bytes: ByteArray, taIndex: Int): HTMLDivElement {
    val ssfParsed = SSFParser().parse(bytes, taIndex)
    parsedMessages[taIndex] = ssfParsed

    val ssfResult = document.createElement("DIV") as HTMLDivElement
    val ssfName = document.createElement("H3") as HTMLHeadingElement
    ssfName.innerText = "SwiftSegFinder"

    val ssfContent = document.createElement("DIV") as HTMLDivElement
    ssfContent.classList.add("parsecontent")
    ssfContent.innerHTML = if (showSegmentWiseAlignment) {
        SSFRenderer.renderSegmentWiseHTML(ssfParsed)
    } else {
        SSFRenderer.renderByteWiseHTML(ssfParsed)
    }


    attachRangeListeners(ssfContent, taIndex)
    attachSSFButtons(ssfContent, bytes, taIndex)

    ssfResult.appendChild(ssfName)
    ssfResult.appendChild(ssfContent)

    return ssfResult
}

// decode all text areas
fun decode(isLiveDecoding: Boolean) {
    val showSSFContent = true

    val textareas = document.querySelectorAll(".input_area")
    for (i in 0 until textareas.length) {
        // get bytes from textarea
        val textarea = textareas[i] as HTMLTextAreaElement
        val sizeLabel = textarea.nextElementSibling as HTMLDivElement
        val inputText = textarea.value.trim()
        val bytes = ByteWitch.getBytesFromInputEncoding(inputText)
        (sizeLabel.firstChild as HTMLSpanElement).innerText = "${bytes.size}B (0x${bytes.size.toString(16)})"
        (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = "" // clear selection info

        // only decode text area if input changed
        val oldBytes = parsedMessages[i]?.bytes
        if (oldBytes == null || !oldBytes.contentEquals(bytes)) {
            parsedMessages[i] = SSFParsedMessage(listOf(), bytes, i) // for float view if showSSFContent is set to false
            decodeBytes(bytes, i, showSSFContent)
        }
    }

    if (showSSFContent /*&& !isLiveDecoding*/) { // refine ssf fields and rerender html content
        val refined = SSFParser().refineSegmentsAcrossMessages(parsedMessages.values.toList())
        refined.forEach { msg ->
            parsedMessages[msg.msgIndex] = msg
            rerenderSSF(msg.msgIndex, msg)
        }
    } else { // show output of entropy decoder
        decodeWithEntropy()
    }

    // for sequence alignment
    if (tryhard && !isLiveDecoding && showSSFContent) {
        if (showSegmentWiseAlignment) {
            val alignedSegment = SegmentWiseSequenceAlignment.align(parsedMessages)
            attachSegmentWiseSequenceAlignmentListeners(alignedSegment)
        } else {
            val alignedSegment = ByteWiseSequenceAlignment.align(parsedMessages)
            attachByteWiseSequenceAlignmentListeners(alignedSegment)
        }

    }

    // TODO for testing purposes only
    // includeAlignmentForTesting()
}

/*fun includeAlignmentForTesting() {
    val output = document.getElementById("output") as HTMLDivElement
    val testingMessages = getTestingData()
    val messageBox = document.createElement("DIV") as HTMLDivElement
    messageBox.classList.add("message-output")
    for ((index, message) in testingMessages) {
        val ssfResult = document.createElement("DIV") as HTMLDivElement
        val ssfName = document.createElement("H3") as HTMLHeadingElement
        ssfName.innerText = "SwiftSegFinder $index"

        val ssfContent = document.createElement("DIV") as HTMLDivElement
        ssfContent.classList.add("parsecontent")

        if (message != null) {
            ssfContent.innerHTML = SSFRenderer.render(message)
        } else {
            ssfContent.innerText = "Error: message $index is null"
        }

        ssfResult.appendChild(ssfName)
        ssfResult.appendChild(ssfContent)
        messageBox.appendChild(ssfResult)
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