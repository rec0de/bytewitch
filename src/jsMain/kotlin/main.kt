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

// track input changes to avoid unnecessary re-decodes
var lastInputBytes = mutableMapOf<Int, ByteArray>()

// save parsed messages for float view and SwiftSegFinder
var parsedMessages = mutableMapOf<Int, SSFParsedMessage>()

// choose between segment- and byte-wise sequence alignment
var showSegmentWiseAlignment = true

// settings
var ssfEnabled = true
var showInstances = true

fun main() {
    window.addEventListener("load", {
        // Initialize the Kaitai related UI elements
        KaitaiUI
        // Load Kaitai structs
        MainScope().launch {
            KaitaiUI.loadBundledStructs()
        }
        KaitaiUI.loadKaitaiStructsFromStorage()

        // Initialize managers
        LayoutManager
        SettingsManager
        DecoderListManager
        TextareaUtils

        val dataContainer = document.getElementById("data_container")!!
        val decodeBtn = document.getElementById("decode") as HTMLButtonElement
        val tryhardBtn = document.getElementById("tryhard") as HTMLButtonElement
        val uploadBtn = document.getElementById("upload") as HTMLButtonElement
        val addDataBox = document.getElementById("add_data") as HTMLElement
        val deleteDataBox = document.getElementById("delete_data") as HTMLElement

        val liveDecode = TwoWayCheckboxBinding(document.getElementById("livedecode") as HTMLInputElement, "livedecode")
        liveDecodeEnabled = liveDecode.checked

        decodeBtn.onclick = {
            tryhard = false
            decode(true, force = !liveDecodeEnabled)
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
            TextareaUtils.appendTextArea()
        }

        // to delete last text area
        deleteDataBox.onclick = {
            TextareaUtils.removeTextArea()
        }

        liveDecode.onChange = {
            liveDecodeEnabled = liveDecode.checked
            if (liveDecodeEnabled)
                decode(false)
        }

        // check for data stored in session storage with key "input-data-N" where N is strictly increasing
        // for each found key, append a text area. if a key is not found, stop
        run {
            var n = 0
            while (true) {
                val content = window.sessionStorage.getItem("input-data-$n")
                if (content != null) {
                    TextareaUtils.appendTextArea(content)
                    n++
                } else {
                    break
                }
            }
            if (n == 0) {
                TextareaUtils.appendTextArea()
            }
        }
        //TextareaUtils.appendTextArea()

        // a click anywhere clears any present selection
        // (as do specific keystrokes, but we'll see if we want to worry about those)
        document.onclick = {
            // avoid immediately clearing selection from click associated with select event
            val deltaTimeMs = Date().getTime() - (lastSelectionEvent ?: 0.0)
            if(deltaTimeMs > 250) {
                clearSelections()
            }
        }

        document.onkeydown = {
            val deltaTimeMs = Date().getTime() - (lastSelectionEvent ?: 0.0)
            if(deltaTimeMs > 250 && it.keyCode !in listOf(16, 17, 20)) {
                clearSelections()
            }
        }

        // force decode on page load
        decode(isLiveDecoding = true, force = true)
    })
}

fun clearSelections() {
    val inputs = document.querySelectorAll("#data_container .input_area")
    inputs.asList().forEach {
        val sizeLabel = (it as HTMLTextAreaElement).nextElementSibling!!
        val selectionLabel = sizeLabel.firstChild!!.nextSibling as HTMLSpanElement
        selectionLabel.innerText = ""
    }
}

// decode one specific byte sequence
fun decodeBytes(bytes: ByteArray, taIndex: Int) {
    val output = document.getElementById("output") as HTMLDivElement

    // decode input
    val result = ByteWitch.analyze(bytes, tryhard)

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
    val messageBoxTitle = document.createElement("H2") as HTMLHeadingElement
    messageBoxTitle.innerHTML = "Input ${taIndex + 1}"
    messageBox.appendChild(messageBoxTitle)

    if (!result.isEmpty()) {
        result.forEach {
            messageBox.appendChild(renderByteWitchResult(it, taIndex))
        }
        if (ssfEnabled) {
            messageBox.appendChild(decodeWithSSF(bytes, taIndex))
        }
    } else {
        if (bytes.isNotEmpty()) {
            messageBox.appendChild(document.createTextNode("No decoder could parse the input"))
        } else {
            messageBox.appendChild(document.createTextNode("No valid input to parse"))
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
// force: forces a decode even if the content has not changed (e.g., when decoders have changed)
fun decode(isLiveDecoding: Boolean, force: Boolean = false) {
    // don't decode if "live decode" is disabled and decode is not called from the "decode" button
    if (!(isLiveDecoding || liveDecodeEnabled)) return

    // clear bytefinder
    val bytefinder = document.getElementById("bytefinder") as HTMLDivElement
    val hexview = document.getElementById("hexview") as HTMLDivElement
    val textview = document.getElementById("textview") as HTMLDivElement
    bytefinder.style.display = "none"
    hexview.innerHTML = ""
    textview.innerHTML = ""
    // if any result is not empty, it will make the bytefinder visible

    val textareas = document.querySelectorAll(".input_area")
    for (i in 0 until textareas.length) {
        // get bytes from textarea
        val textarea = textareas[i] as HTMLTextAreaElement
        val sizeLabel = textarea.nextElementSibling as HTMLDivElement
        val inputText = textarea.value.trim()
        val bytes = ByteWitch.getBytesFromInputEncoding(inputText)
        (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = "" // clear selection info

        // only decode text area if input changed or the Kaitai struct changed
        val inputChanged = lastInputBytes[i]?.contentEquals(bytes)?.not() ?: true
        if (force || KaitaiUI.hasChangedSinceLastDecode() || inputChanged) {
            parsedMessages[i] = SSFParsedMessage(listOf(), bytes, i) // for float view if showSSFContent is set to false
            lastInputBytes[i] = bytes
            decodeBytes(bytes, i)
        }

        // set bytefinder visible if any results have been generated
        val messageBox = document.getElementById("message-output-$i") as HTMLDivElement
        if (messageBox.children.length > 1) {
            bytefinder.style.display = "flex"
        }
    }

    KaitaiUI.setChangedSinceLastDecode(false)

    if (ssfEnabled) {
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
}