import SequenceAlignment.ByteWiseSequenceAlignment
import SequenceAlignment.NemesysSequenceAlignment
import decoders.ByteWitchResult
import decoders.Nemesys.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.HTMLTextAreaElement


var liveDecodeEnabled = true
var currentHighlight: Element? = null
var tryhard = false

// save parsed messages for float view and nemesys
var parsedMessages = mutableMapOf<Int, NemesysParsedMessage>()

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
            val newTextarea = document.createElement("textarea") as HTMLTextAreaElement
            newTextarea.className = "data input_area"
            dataContainer.appendChild(newTextarea)

            // for live decode
            if (liveDecodeEnabled) {
                newTextarea.oninput = {
                    decode(true)
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



// use entropy decoder and attach output to messageBox
fun decodeWithEntropy() { // TODO this can be removed
    val  nemesysParsedMessages = NemesysParser().parseEntropy(parsedMessages.values.toList())

    nemesysParsedMessages.forEach {
        val messageId = "message-output-${it.msgIndex}"
        var messageBox = document.getElementById(messageId) as HTMLDivElement

        // remove old nemesys content
        val existingParsers = messageBox.querySelectorAll("h3")
        for (i in 0 until existingParsers.length) {
            val heading = existingParsers.item(i) as? HTMLHeadingElement ?: continue
            if (heading.innerText.lowercase() == "nemesysparser") {
                heading.parentElement?.remove()
                break
            }
        }

        // add new nemesys content
        val nemesysResult = document.createElement("DIV") as HTMLDivElement
        val nemesysName = document.createElement("H3") as HTMLHeadingElement
        nemesysName.innerText = "nemesysparser"

        val nemesysContent = document.createElement("DIV") as HTMLDivElement
        nemesysContent.classList.add("parsecontent")
        // nemesysContent.innerHTML = NemesysRenderer.render(it)
        nemesysContent.innerHTML = NemesysRenderer.renderByteWiseHTML(it)

        attachRangeListeners(nemesysContent, it.msgIndex)
        attachNemesysButtons(nemesysContent, it.bytes, it.msgIndex)

        nemesysResult.appendChild(nemesysName)
        nemesysResult.appendChild(nemesysContent)

        messageBox.appendChild(nemesysResult)
    }
}


// decode one specific byte sequence
fun decodeBytes(bytes: ByteArray, taIndex: Int, showNemesysContent: Boolean) {
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
            messageBox.appendChild(renderByteWitchResult(it, taIndex))
        }

        // for nemesys content
        if (showNemesysContent) {
            messageBox.appendChild(decodeWithNemesys(bytes, taIndex))
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

// decode bytes with nemesys and return HTML content
private fun decodeWithNemesys(bytes: ByteArray, taIndex: Int): HTMLDivElement {
    val nemesysParsed = NemesysParser().parse(bytes, taIndex)
    parsedMessages[taIndex] = nemesysParsed

    val nemesysResult = document.createElement("DIV") as HTMLDivElement
    val nemesysName = document.createElement("H3") as HTMLHeadingElement
    nemesysName.innerText = "nemesysparser"

    val nemesysContent = document.createElement("DIV") as HTMLDivElement
    nemesysContent.classList.add("parsecontent")
    // nemesysContent.innerHTML = NemesysRenderer.render(nemesysParsed)
    nemesysContent.innerHTML = NemesysRenderer.renderByteWiseHTML(nemesysParsed)

    attachRangeListeners(nemesysContent, taIndex)
    attachNemesysButtons(nemesysContent, bytes, taIndex)

    nemesysResult.appendChild(nemesysName)
    nemesysResult.appendChild(nemesysContent)

    return nemesysResult
}

// decode all text areas
fun decode(isLiveDecoding: Boolean) {
    val showNemesysContent = true

    val textareas = document.querySelectorAll(".input_area")
    for (i in 0 until textareas.length) {
        // get bytes from textarea
        val textarea = textareas[i] as HTMLTextAreaElement
        val inputText = textarea.value.trim()
        val bytes = ByteWitch.getBytesFromInputEncoding(inputText)

        // only decode text area if input changed
        val oldBytes = parsedMessages[i]?.bytes
        if (oldBytes == null || !oldBytes.contentEquals(bytes)) {
            parsedMessages[i] = NemesysParsedMessage(listOf(), bytes, i) // for float view if showNemesysContent is set to false
            decodeBytes(bytes, i, showNemesysContent)
        }
    }


    if (showNemesysContent) { // refine nemesys fields and rerender html content
        val refined = NemesysParser().refineSegmentsAcrossMessages(parsedMessages.values.toList())
        refined.forEach { msg ->
            parsedMessages[msg.msgIndex] = msg
            rerenderNemesys(msg.msgIndex, msg)
        }
    } else { // show output of entropy decoder
        decodeWithEntropy()
    }

    // for sequence alignment
    if (tryhard && !isLiveDecoding && showNemesysContent) {
        // val alignedSegment = NemesysSequenceAlignment.align(parsedMessages)
        // attachSequenceAlignmentListeners(alignedSegment)
        val alignedSegment = ByteWiseSequenceAlignment.align(parsedMessages)
        attachByteWiseSequenceAlignmentListeners(alignedSegment)
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
