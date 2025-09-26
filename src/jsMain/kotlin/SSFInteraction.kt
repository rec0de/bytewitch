import SequenceAlignment.SegmentWiseSequenceAlignment
import SequenceAlignment.ByteWiseSequenceAlignment
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import decoders.SwiftSegFinder.*

// button to show SwiftSegFinder if it's not eligible to show by default
fun attachShowSSFButtonHandler(container: Element, originalBytes: ByteArray, msgIndex: Int) {
    container.querySelectorAll(".show-ssf-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLButtonElement
        button.addEventListener("click", {
            button.remove()

            // add message to ssfEligible messages
            ssfEligible.add(msgIndex)

            // render and attach SSF view
            val messageBox = document.getElementById("message-output-$msgIndex") as HTMLDivElement

            val ssfEl = decodeWithSSF(originalBytes, msgIndex)
            messageBox.appendChild(ssfEl)

            attachSSFButtons(ssfEl, originalBytes, msgIndex)

            if (ssfEligible.size >= 2) {
                showStartSequenceAlignmentButton()
            }
        })
    }
}

// to start sequence alignment
fun attachStartSequenceAlignmentButtonHandler(container: Element) {
    container.querySelectorAll(".alignment-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLElement
        button.addEventListener("click", {
            // rerender SwiftSegFinder view
            val eligibleMsgs = getEligibleMsgsForSSF()
            eligibleMsgs.forEach { (msgIndex, parsed) ->
                rerenderSSF(msgIndex, parsed)
            }

            // align sequence
            if (showSegmentWiseAlignment) {
                val aligned = SegmentWiseSequenceAlignment.align(eligibleMsgs)
                attachSegmentWiseSequenceAlignmentListeners(aligned)
            } else {
                val aligned = ByteWiseSequenceAlignment.align(eligibleMsgs)
                attachByteWiseSequenceAlignmentListeners(aligned)
            }
        })
    }
}

// to toggle between sequence and segment wise sequence alignment
fun attachToggleSequenceAlignmentButtonHandler(container: Element) {
    container.querySelectorAll(".toggle-seqalign-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLElement
        button.addEventListener("click", {
            showSegmentWiseAlignment = !showSegmentWiseAlignment

            // rerender SwiftSegFinder view
            val eligibleMsgs = getEligibleMsgsForSSF()
            eligibleMsgs.forEach { (msgIndex, parsed) ->
                rerenderSSF(msgIndex, parsed)
            }

            // realign sequence
            if (showSegmentWiseAlignment) {
                val aligned = SegmentWiseSequenceAlignment.align(eligibleMsgs)
                attachSegmentWiseSequenceAlignmentListeners(aligned)
            } else {
                val aligned = ByteWiseSequenceAlignment.align(eligibleMsgs)
                attachByteWiseSequenceAlignmentListeners(aligned)
            }
        })
    }
}

// attach finish button handler for editable SwiftSegFinder content
fun attachFinishButtonHandler(container: Element, originalBytes: ByteArray, msgIndex: Int) {
    container.querySelectorAll(".finish-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLElement
        button.addEventListener("click", {
            val oldWrapper = button.closest(".ssf") as HTMLElement
            val byteContainer = oldWrapper.querySelector("#byteContainer") as HTMLElement

            // read out where to start in the byte sequence. This is important for the offset
            val dataStart = oldWrapper.getAttribute("data-start")?.toIntOrNull() ?: 0
            val dataEnd = oldWrapper.getAttribute("data-end")?.toIntOrNull() ?: originalBytes.size
            val slicedBytes = originalBytes.sliceArray(dataStart until dataEnd)

            // get new segment boundaries
            val newOffsets = rebuildSegmentsFromDOM(byteContainer, msgIndex)

            val prevGlobal = parsedMessages[msgIndex]?.segments.orEmpty()
            val prevInSlice = prevGlobal
                .filter { it.offset in dataStart until dataEnd }
                .map { SSFSegment(it.offset - dataStart, it.fieldType) }

            // re-tag segments
            val retagged = retagSegments(prevInSlice, newOffsets, slicedBytes.size)

            val newParsed = SSFParsedMessage(retagged, slicedBytes, msgIndex)
            parsedMessages[msgIndex] = newParsed

            // render new html content
            rerenderSSF(msgIndex, newParsed)

            // show sequence alignment buttons
            val eligibleMsgs = getEligibleMsgsForSSF()
            if (autoRunSeqAlign(eligibleMsgs)) { // check if auto run of sequence alignment is allowed
                if (showSegmentWiseAlignment) { // check if we were in bytewise or segmentwise view
                    val alignedSegment = SegmentWiseSequenceAlignment.align(eligibleMsgs)
                    attachSegmentWiseSequenceAlignmentListeners(alignedSegment)
                } else {
                    val alignedSegment = ByteWiseSequenceAlignment.align(eligibleMsgs)
                    attachByteWiseSequenceAlignmentListeners(alignedSegment)
                }
            } else if(eligibleMsgs.size >= 2) {
                showStartSequenceAlignmentButton() // show start button for sequence alignment
            }
        })
    }
}

// re-tag segments if the boundaries didn't change
fun retagSegments(
    prev: List<SSFSegment>?,
    newOffsetsRaw: List<Int>,
    totalSize: Int
): List<SSFSegment> {
    val newOffsets = newOffsetsRaw.distinct().sorted()
    if (newOffsets.isEmpty()) return listOf(SSFSegment(0, SSFField.UNKNOWN))

    val prevSegs = (prev ?: emptyList()).sortedBy { it.offset }

    fun endAtOffsets(i: Int, offsets: List<Int>, total: Int) =
        if (i + 1 < offsets.size) offsets[i + 1] else total

    fun endAtPrev(i: Int, segs: List<SSFSegment>, total: Int) =
        if (i + 1 < segs.size) segs[i + 1].offset else total

    fun isPayloadLen(t: SSFField) =
        t == SSFField.PAYLOAD_LENGTH_BIG_ENDIAN || t == SSFField.PAYLOAD_LENGTH_LITTLE_ENDIAN

    val result = ArrayList<SSFSegment>(newOffsets.size)

    // go through new segments
    for (i in newOffsets.indices) {
        val start = newOffsets[i]
        val end = endAtOffsets(i, newOffsets, totalSize)

        // find segment with same boundaries
        val idx = prevSegs.indexOfFirst { it.offset == start }
        val typ = if (idx >= 0 && endAtPrev(idx, prevSegs, totalSize) == end) {
            prevSegs[idx].fieldType // set old segment type again
        } else {
            SSFField.UNKNOWN
        }

        result.add(SSFSegment(start, typ))
    }

    // PAYLOAD_LENGTH and STRING_PAYLOAD can only appear as a pair. If one doesn't exist set both to unknown

    // 1. check if a string payload always follow after a payload field if not set payload length to unkown
    for (i in result.indices) {
        if (isPayloadLen(result[i].fieldType)) {
            if (i + 1 >= result.size || result[i + 1].fieldType != SSFField.STRING_PAYLOAD) {
                result[i] = SSFSegment(result[i].offset, SSFField.UNKNOWN)
            }
        }
    }

    // 2. check if payload_length field is set before a string field
    for (i in result.indices) {
        if (result[i].fieldType == SSFField.STRING_PAYLOAD) {
            if (i == 0 || !isPayloadLen(result[i - 1].fieldType)) {
                result[i] = SSFSegment(result[i].offset, SSFField.STRING) // just set to String not to unkown
            }
        }
    }

    return result
}

// if edit button is pressed show editableView and hide prettyView
fun attachEditButtonHandler(container: Element) {
    container.querySelectorAll(".edit-button").asList().forEach { btnElement ->
        val button = btnElement as HTMLElement
        button.addEventListener("click", {
            val wrapper = button.closest(".ssf") as HTMLElement
            val prettyView = wrapper.querySelector(".view-default") as HTMLElement
            val editableView = wrapper.querySelector(".view-editable") as HTMLElement

            // switch display mode of pretty and editable view
            prettyView.style.display = "none"
            editableView.style.display = "block"

            // this is needed to work with the separator
            attachSSFSeparatorHandlers()
            attachSeparatorPlaceholderClickHandlers()
        })
    }
}

// read out SwiftSegFinder segments based on separators set by the user
fun rebuildSegmentsFromDOM(container: HTMLElement, msgIndex: Int): List<Int> {
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

    return segmentOffsets
}

// rerender SwiftSegFinder html view
fun rerenderSSF(msgIndex: Int, parsed: SSFParsedMessage) {
    // return if it is not rendered yet
    if (document.getElementById("message-output-$msgIndex") == null)
        return

    val messageBox = document.getElementById("message-output-$msgIndex") as HTMLDivElement
    val oldWrapper = messageBox.querySelector(".ssf") as HTMLDivElement

    // create new div with new SSF content
    val temp = document.createElement("div") as HTMLDivElement
    val newHTML = if (showSegmentWiseAlignment) {
        SSFRenderer.renderSegmentWiseHTML(parsed)
    } else {
        SSFRenderer.renderByteWiseHTML(parsed)
    }
    temp.innerHTML = newHTML

    val newWrapper = temp.firstElementChild as HTMLElement
    oldWrapper.replaceWith(newWrapper)

    // attach javascript handlers
    attachSSFButtons(newWrapper, parsed.bytes, msgIndex)
}


// attach button handlers for SSF
fun attachSSFButtons(parseContent: Element, bytes: ByteArray, msgIndex: Int) {
    attachRangeListeners(parseContent, msgIndex)
    attachStartSequenceAlignmentButtonHandler(parseContent)
    attachToggleSequenceAlignmentButtonHandler(parseContent)
    attachEditButtonHandler(parseContent)
    attachFinishButtonHandler(parseContent, bytes, msgIndex)
}

// separator handler to change boundaries of SwiftSegFinder content
fun attachSSFSeparatorHandlers() {
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

    // check that separator hasn't moved into another editable SwiftSegFinder field
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
            attachSSFSeparatorHandlers()
            attachSeparatorPlaceholderClickHandlers()
        })
    }
}