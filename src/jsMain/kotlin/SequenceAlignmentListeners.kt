import SequenceAlignment.AlignedSequence
import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlinx.browser.document
import decoders.Nemesys.*

// global values for SequenceAlignment listeners
val alignmentMouseEnterListeners = mutableMapOf<String, (Event) -> Unit>()
val alignmentMouseLeaveListeners = mutableMapOf<String, (Event) -> Unit>()


// attach sequence alignment listeners
fun attachSequenceAlignmentListeners(alignedSegments: List<AlignedSequence>) {
    // remove old sequence alignment listeners
    removeAllSequenceAlignmentListeners()

    // group all aligned segments
    // for example: if AlignedSequence(0, 1, 3, 2, 0.05) is given
    // then create add alignmentGroups["0-3"] = {"1-2", "0-3"} and alignmentGroups["1-2"] = {"0-3", "1-2"}
    val alignmentGroups = mutableMapOf<String, MutableSet<String>>()
    val alignmentColors = mutableMapOf<String, Triple<Float, Float, Float>>() // safe highlighting color
    val alignmentBytes = mutableMapOf<String, ByteArray>() // save byte segment of corresponding id
    for (segment in alignedSegments) {
        val idA = "${segment.protocolA}-${segment.indexA}"
        val idB = "${segment.protocolB}-${segment.indexB}"

        alignmentGroups.getOrPut(idA) { mutableSetOf() }.add(idB)
        alignmentGroups.getOrPut(idB) { mutableSetOf() }.add(idA)
        alignmentGroups[idA]!!.add(idA)
        alignmentGroups[idB]!!.add(idB)

        val msgA = parsedMessages[segment.protocolA]
        val msgB = parsedMessages[segment.protocolB]
        if (msgA != null) {
            alignmentBytes[idA] = msgA.bytes.sliceArray(NemesysUtil.getByteRange(msgA, segment.indexA))
        }
        if (msgB != null) {
            alignmentBytes[idB] = msgB.bytes.sliceArray(NemesysUtil.getByteRange(msgB, segment.indexB))
        }
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

// attach sequence alignment listeners
fun attachByteWiseSequenceAlignmentListeners(alignedSegments: List<AlignedSequence>) {
    // remove old sequence alignment listeners
    removeAllSequenceAlignmentListeners()

    // group all aligned segments
    // for example: if AlignedSegment(0, 1, 3, 2, 0.05) is given
    // then create add alignmentGroups["0-3"] = {"1-2", "0-3"} and alignmentGroups["1-2"] = {"0-3", "1-2"}
    val alignmentGroups = mutableMapOf<String, MutableSet<String>>()
    val alignmentColors = mutableMapOf<String, Triple<Float, Float, Float>>() // safe highlighting color
    val alignmentBytes = mutableMapOf<String, ByteArray>() // save byte segment of corresponding id
    for (segment in alignedSegments) {
        val idA = "${segment.protocolA}-${segment.indexA}"
        val idB = "${segment.protocolB}-${segment.indexB}"

        alignmentGroups.getOrPut(idA) { mutableSetOf() }.add(idB)
        alignmentGroups.getOrPut(idB) { mutableSetOf() }.add(idA)
        alignmentGroups[idA]!!.add(idA)
        alignmentGroups[idB]!!.add(idB)

        val msgA = parsedMessages[segment.protocolA]
        val msgB = parsedMessages[segment.protocolB]
        if (msgA != null) {
            // alignmentBytes[idA] = msgA.bytes.sliceArray(NemesysUtil.getByteRange(msgA, segment.segmentIndexA))
            alignmentBytes[idA] = byteArrayOf(msgA.bytes[segment.indexA])
        }
        if (msgB != null) {
            // alignmentBytes[idB] = msgB.bytes.sliceArray(NemesysUtil.getByteRange(msgB, segment.segmentIndexB))
            alignmentBytes[idB] = byteArrayOf(msgB.bytes[segment.indexB])
        }
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