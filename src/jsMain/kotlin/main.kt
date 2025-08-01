import SequenceAlignment.SegmentWiseSequenceAlignment
import SequenceAlignment.ByteWiseSequenceAlignment
import decoders.ByteWitchResult
import decoders.SwiftSegFinder.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.*
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.Date


var liveDecodeEnabled = true
var currentHighlight: Element? = null
var lastSelectionEvent: Double? = null
var tryhard = false

// save parsed messages for float view and SwiftSegFinder
var parsedMessages = mutableMapOf<Int, SSFParsedMessage>()

// choose between segment- and byte-wise sequence alignment
var showSegmentWiseAlignment = true

fun main() {
    window.addEventListener("load", {
        // Initialize the Kaitai related UI elements
        KaitaiUI
        // Load Kaitai structs
        MainScope().launch {
            KaitaiUI.loadBundledStructs()
        }
        KaitaiUI.loadKaitaiStructsFromStorage()

        val dataContainer = document.getElementById("data_container")!!
        val decodeBtn = document.getElementById("decode") as HTMLButtonElement
        val tryhardBtn = document.getElementById("tryhard") as HTMLButtonElement
        val uploadBtn = document.getElementById("upload") as HTMLButtonElement
        val addDataBox = document.getElementById("add_data") as HTMLElement
        val deleteDataBox = document.getElementById("delete_data") as HTMLElement

        val liveDecode = document.getElementById("livedecode") as HTMLInputElement
        liveDecodeEnabled = liveDecode.checked

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
            appendTextArea()
        }

        // to delete last text area
        deleteDataBox.onclick = {
            if (dataContainer.children.length > 1) { // there need to be at least one data container left
                removeTextArea(dataContainer)
            }
        }

        liveDecode.onchange = {
            liveDecodeEnabled = liveDecode.checked
            if (KaitaiUI.isLiveDecodeEnabled()) {
                ByteWitch.setKaitaiLiveDecoder(KaitaiUI.getInputValue())
                if (liveDecodeEnabled)
                    decode(false)
            }
            0.0
        }

        // init first textarea
        appendTextArea()

        // a click anywhere clears any present selection
        // (as do specific keystrokes, but we'll see if we want to worry about those)
        document.onclick = {
            // avoid immediately clearing selection from click associated with select event
            if(lastSelectionEvent != null && Date().getTime() - lastSelectionEvent!! > 250) {
                clearSelections()
            }
        }

        document.onkeydown = {
            if(lastSelectionEvent != null && Date().getTime() - lastSelectionEvent!! > 250 && it.keyCode !in listOf(16, 17, 20)) {
                clearSelections()
            }
        }

    })
}

fun clearSelections() {
    lastSelectionEvent = null
    val inputs = document.querySelectorAll("textarea")
    inputs.asList().forEach {
        val sizeLabel = (it as HTMLTextAreaElement).nextElementSibling!!
        val selectionLabel = sizeLabel.firstChild!!.nextSibling as HTMLSpanElement
        selectionLabel.innerText = ""
    }
}

// decode one specific byte sequence
fun decodeBytes(bytes: ByteArray, taIndex: Int) {
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

        messageBox.appendChild(decodeWithSSF(bytes, taIndex))
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
    val textareas = document.querySelectorAll(".input_area")
    for (i in 0 until textareas.length) {
        // get bytes from textarea
        val textarea = textareas[i] as HTMLTextAreaElement
        val sizeLabel = textarea.nextElementSibling as HTMLDivElement
        val inputText = textarea.value.trim()
        val bytes = ByteWitch.getBytesFromInputEncoding(inputText)
        (sizeLabel.firstChild as HTMLSpanElement).innerText = "${bytes.size}B (0x${bytes.size.toString(16)})"
        (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = "" // clear selection info

        // remember if this textarea has plain hex input so we can enable selection highlighting
        textarea.setAttribute("data-plainhex", ByteWitch.isPlainHex().toString())

        // only decode text area if input changed
        val oldBytes = parsedMessages[i]?.bytes
        if (oldBytes == null || !oldBytes.contentEquals(bytes)) {
            parsedMessages[i] = SSFParsedMessage(listOf(), bytes, i) // for float view if showSSFContent is set to false
            decodeBytes(bytes, i)
        }
    }

    // refine ssf fields and rerender html content
    val refined = SSFParser().refineSegmentsAcrossMessages(parsedMessages.values.toList())
    refined.forEach { msg ->
        parsedMessages[msg.msgIndex] = msg
        rerenderSSF(msg.msgIndex, msg)
    }

    // for sequence alignment
    if (tryhard && !isLiveDecoding) {
        if (showSegmentWiseAlignment) {
            val alignedSegment = SegmentWiseSequenceAlignment.align(parsedMessages)
            attachSegmentWiseSequenceAlignmentListeners(alignedSegment)
        } else {
            val alignedSegment = ByteWiseSequenceAlignment.align(parsedMessages)
            attachByteWiseSequenceAlignmentListeners(alignedSegment)
        }

    }
}