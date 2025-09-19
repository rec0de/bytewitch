import SequenceAlignment.SegmentWiseSequenceAlignment
import SequenceAlignment.ByteWiseSequenceAlignment
import decoders.ByteWitchResult
import decoders.SwiftSegFinder.*
import kotlinx.browser.document
import kotlinx.browser.window
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
            mainDecode(isLiveDecoding = false, tryhard = false)
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
            appendTextArea("")
        }

        // to delete last text area
        deleteDataBox.onclick = {
            if (dataContainer.children.length > 1) { // there needs to be at least one data container left
                removeTextArea(dataContainer)
            }
        }

        liveDecode.onchange = {
            liveDecodeEnabled = liveDecode.checked
            applyLiveDecodeListeners()
            0.0
        }

        // init first textarea
        appendTextArea()

        // a click anywhere clears any present selection
        // (as do specific keystrokes, but we'll see if we want to worry about those)
        document.onclick = {
            // avoid immediately clearing selection from click associated with select event
            if (lastSelectionEvent != null && Date().getTime() - lastSelectionEvent!! > 250) {
                clearSelections()
            }
        }

        document.onkeydown = {
            val activeDebouncedSelection = lastSelectionEvent != null && Date().getTime() - lastSelectionEvent!! > 250
            if (activeDebouncedSelection && it.keyCode !in listOf(16, 17, 20)) {
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
fun decodeSingleMessage(bytes: ByteArray, taIndex: Int, showSSFContent: Boolean, tryhard: Boolean) {
    val output = document.getElementById("output") as HTMLDivElement

    // return if no data is given or just a half byte
    if (bytes.isEmpty())
        return

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

    result.forEach {
        messageBox.appendChild(renderByteWitchResult(it, taIndex))
    }

    // check if result needs a SwiftSegFinder decoding
    val allowSSF = showSSFContent && !hasHighEndHit(result)

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
fun mainDecode(isLiveDecoding: Boolean, tryhard: Boolean) {
    val textareas = document.querySelectorAll(".input_area")

    for (i in 0 until textareas.length) {
        // get bytes from textarea
        val textarea = textareas[i] as HTMLTextAreaElement
        val inputText = textarea.value.trim()
        val (bytes, encoding) = ByteWitch.getBytesFromInputEncoding(inputText)

        // set size label to payload size
        val sizeLabel = textarea.nextElementSibling as HTMLDivElement
        (sizeLabel.firstChild as HTMLSpanElement).innerText = "${bytes.size}B (0x${bytes.size.toString(16)})"
        (sizeLabel.firstChild!!.nextSibling as HTMLSpanElement).innerText = "" // clear selection info

        // set encoding label
        val encodingLabel = sizeLabel.nextElementSibling as HTMLDivElement
        encodingLabel.innerText = encoding.label

        // remember if this textarea has plain hex input so we can enable selection highlighting
        textarea.setAttribute("data-plainhex", (encoding == ByteWitch.Encoding.HEX).toString())

        // only decode text area if input changed, or we are doing manual tryhard decode
        val oldBytes = parsedMessages[i]?.bytes
        if (oldBytes == null || !oldBytes.contentEquals(bytes) || (!isLiveDecoding && tryhard)) {
            if (bytes.isNotEmpty()) {
                parsedMessages[i] =
                    SSFParsedMessage(listOf(), bytes, i) // for float view if showSSFContent is set to false

                decodeSingleMessage(bytes, i, showSSFContent = bytes.size <= byteLimitSSFContent, tryhard)
            }
            else if (inputText.isEmpty()) { // if no bytes are set in textview and not even a half byte is set so delete output
                // delete from output view
                val output = document.getElementById("output") as HTMLDivElement
                val target = document.getElementById("message-output-$i") as? HTMLDivElement
                if (target != null) {
                    output.removeChild(target)
                }

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
    }

    // hide "no decode yet" message if we got at least one hit
    val output = document.getElementById("output") as HTMLDivElement
    if(output.childNodes.asList().any { it is HTMLDivElement }) {
        (document.getElementById("no_decode_yet") as HTMLElement).style.display = "none"
    }

    // show the bytes of the first text area in the byte finder view
    setByteFinderContent(0)

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
