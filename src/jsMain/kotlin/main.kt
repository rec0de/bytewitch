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

const val byteLimitSSFContent = 1000 // only show SwiftSegFinder for messages with a length below the defined threshold
const val maxLimitSequenceAlignment = 5000 // max total bytes across all eligible messages for auto sequence alignment
val ssfEligible = mutableSetOf<Int>() // msgIndex for protocol messages that show SwiftSegFinder output
private val HIGH_END_DECODERS = setOf( // set of confident decoders when SSF is not allowed to show up
    "bplist17", "bplist15", "bplist", "utf8" // TODO maybe use enum to not write the name of it twice.
)


var liveDecodeEnabled = true
var currentHighlight: Element? = null
var lastSelectionEvent: Double? = null

// track input changes to avoid unnecessary re-decodes
var lastInputBytes = mutableMapOf<Int, ByteArray>()

// save parsed messages for float view and SwiftSegFinder
var parsedMessages = mutableMapOf<Int, SSFParsedMessage>()

// choose between segment- and byte-wise sequence alignment
var showSegmentWiseAlignment = true

// settings
var ssfEnabled = true

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

        // input listener for text areas
        TextareaUtils.applyLiveDecodeListeners()

        decodeBtn.onclick = {
            mainDecode(isLiveDecoding = true, force = !liveDecodeEnabled, tryhard = false)
        }

        tryhardBtn.onclick = {
            mainDecode(isLiveDecoding = false, tryhard = true)
        }

        uploadBtn.onclick = {
            val fileInput = document.createElement("input") as HTMLInputElement
            fileInput.type = "file"
            fileInput.accept = "*" // Accept any file type
            fileInput.multiple = true // to upload multiple files

            fileInput.onchange = {
                val files = fileInput.files?.asList()

                files?.forEach {
                    if (it.type == "text/plain") {
                        // Handle .txt files
                        readFile(it)
                    } else {
                        // Handle binary files
                        readBinaryFile(it)
                    }
                }
            }

            // Trigger the file selection dialog
            fileInput.click()
        }

        // to add more text areas for protocols
        addDataBox.onclick = {
            TextareaUtils.appendTextArea("")
        }

        // to delete last text area
        deleteDataBox.onclick = {
            if (dataContainer.children.length > 1) { // there needs to be at least one data container left
                TextareaUtils.removeTextArea()
            }
        }

        liveDecode.onChange = {
            liveDecodeEnabled = liveDecode.checked
            TextareaUtils.applyLiveDecodeListeners()
            if (liveDecodeEnabled)
                mainDecode(false)
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
            val activeDebouncedSelection = lastSelectionEvent != null && Date().getTime() - lastSelectionEvent!! > 250
            if (activeDebouncedSelection) {
                clearSelections()
            }
        }

        document.onkeydown = {
            val activeDebouncedSelection = lastSelectionEvent != null && Date().getTime() - lastSelectionEvent!! > 250
            if (activeDebouncedSelection && it.keyCode !in listOf(16, 17, 20)) {
                clearSelections()
            }
        }

        // force decode on page load
        mainDecode(isLiveDecoding = true, force = true)
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
fun decodeSingleMessage(bytes: ByteArray, taIndex: Int, showSSFContent: Boolean, tryhard: Boolean) : Boolean {
    // decode input
    val result = ByteWitch.analyze(bytes, tryhard)

    val messageBox = getOrCreateMessageOutput(taIndex)

    if (result.isNotEmpty()) {
        result.forEach {
            messageBox.appendChild(renderByteWitchResult(it, taIndex))
        }

        // check if result needs a SwiftSegFinder decoding
        val allowSSF = ssfEnabled && showSSFContent && !hasHighEndHit(result)

        if (allowSSF) {
            // show SwiftSegFinder view
            ssfEligible.add(taIndex)
            messageBox.appendChild(decodeWithSSF(bytes, taIndex))
        } else {
            ssfEligible.remove(taIndex)
            (messageBox.querySelector(".ssf") as? HTMLElement)?.remove()

            // button for SSF rendering
            val btn = document.createElement("button") as HTMLButtonElement
            btn.className = "show-ssf-button"
            btn.textContent = "Show SwiftSegFinder"
            btn.setAttribute("data-msg-index", taIndex.toString())
            messageBox.appendChild(btn)

            attachShowSSFButtonHandler(messageBox, bytes, taIndex)
        }
        return true
    } else {
        messageBox.appendChild(document.createTextNode("No decoder could parse the input"))
        return false
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
fun decodeWithSSF(bytes: ByteArray, taIndex: Int): HTMLDivElement {
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

    ssfResult.appendChild(ssfName)
    ssfResult.appendChild(ssfContent)

    return ssfResult
}

// decode all text areas
// force: forces a decode even if the content has not changed (e.g., when decoders have changed)
fun mainDecode(isLiveDecoding: Boolean, force: Boolean = false, tryhard: Boolean = false) {
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
        val inputText = textarea.value.trim()
        val (bytes, _) = ByteWitch.getBytesFromInputEncoding(inputText)

        // set size label to payload size
        val sizeLabel = textarea.nextElementSibling as HTMLDivElement
        (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = "" // clear selection info

        var anyDecoderMatched = false

        // only decode text area if input changed, or the Kaitai struct changed, or we are doing manual tryhard decode
        val inputChanged = lastInputBytes[i]?.contentEquals(bytes)?.not() ?: true
        if (force || KaitaiUI.hasChangedSinceLastDecode() || inputChanged || (!isLiveDecoding && tryhard)) {
            lastInputBytes[i] = bytes
            if (bytes.isNotEmpty()) {
                parsedMessages[i] =
                    SSFParsedMessage(listOf(), bytes, i) // for float view if showSSFContent is set to false

                anyDecoderMatched = anyDecoderMatched.or(
                decodeSingleMessage(bytes, i, showSSFContent = bytes.size <= byteLimitSSFContent, tryhard)
                )
            } else {
                val messageBox = getOrCreateMessageOutput(i)
                messageBox.appendChild(document.createTextNode("No valid input to parse"))

                // delete from parsedMessages
                parsedMessages.remove(i)
                ssfEligible.remove(i)

                // reset hexview to the bytes of the first textarea
                val hexview = document.getElementById("hexview") as HTMLDivElement
                val textview = document.getElementById("textview") as HTMLDivElement
                hexview.innerText = ""
                textview.innerHTML = ""
            }
        }

        // set bytefinder visible if any results have been generated
        if (anyDecoderMatched) {
            // show the bytes of the first text area in the byte finder view
            setByteFinderContent(0)
        }
    }

    KaitaiUI.setChangedSinceLastDecode(false)

    val eligibleMsgs = getEligibleMsgsForSSF()
    if (eligibleMsgs.isNotEmpty()) {
        // refine ssf fields and rerender html content
        val refined = SSFParser().refineSegmentsAcrossMessages(eligibleMsgs.values.toList())
        refined.forEach { msg ->
            parsedMessages[msg.msgIndex] = msg
            if (msg.msgIndex in ssfEligible) { // only rerender SwiftSegFinder view if its eligible
                rerenderSSF(msg.msgIndex, msg)
            }
        }
    }

    if (!isLiveDecoding && autoRunSeqAlign(eligibleMsgs)) {
        if (showSegmentWiseAlignment) {
            val alignedSegment = SegmentWiseSequenceAlignment.align(eligibleMsgs)
            attachSegmentWiseSequenceAlignmentListeners(alignedSegment)
        } else {
            val alignedSegment = ByteWiseSequenceAlignment.align(eligibleMsgs)
            attachByteWiseSequenceAlignmentListeners(alignedSegment)
        }
    } else if(eligibleMsgs.size >= 2) {
        showStartSequenceAlignmentButton()
    }
}

fun getOrCreateMessageOutput(index: Int): HTMLDivElement {
    val messageId = "message-output-$index"
    var messageBox = document.getElementById(messageId) as? HTMLDivElement

    if (messageBox == null) {
        messageBox = document.createElement("DIV") as HTMLDivElement
        messageBox.id = messageId
        messageBox.classList.add("message-output") // apply layout CSS

        val output = document.getElementById("output") as HTMLDivElement
        output.appendChild(messageBox)
    } else {
        messageBox.innerHTML = "" // clear old content
    }

    val messageBoxTitle = document.createElement("H2") as HTMLHeadingElement
    messageBoxTitle.innerHTML = "Input ${index + 1}"
    messageBox.appendChild(messageBoxTitle)

    return messageBox
}

// check if decoder is confident to not show SwiftSegFinder
fun hasHighEndHit(results: List<Pair<String, ByteWitchResult>>): Boolean =
    results.any { (name, _) -> name in HIGH_END_DECODERS }

// only return Messages that are allowed for SwiftSegFinder view
fun getEligibleMsgsForSSF(): Map<Int, SSFParsedMessage> =
    parsedMessages.filter { (idx, msg) -> idx in ssfEligible }

// check if the messages are too long for auto sequence alignment
fun autoRunSeqAlign(msgs: Map<Int, SSFParsedMessage>): Boolean {
    if (msgs.size < 2) return false
    val totalBytes = msgs.values.sumOf { it.bytes.size }
    return totalBytes <= maxLimitSequenceAlignment
}
