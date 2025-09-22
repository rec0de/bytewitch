import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.DOMRect
import org.w3c.dom.DocumentFragment
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLTemplateElement
import org.w3c.dom.asList
import kotlin.math.abs

data class ChipListItem(
    val node: HTMLDivElement,
    val id: String,
    val displayName: String,
    val sortable: Boolean,
    val toggleable: Boolean,
    val editable: Boolean,
    val deletable: Boolean,
    var isEnabled: Boolean = true
) {
    val nameElement = node.querySelector(".chip-name") as HTMLSpanElement
    val separatorElement = node.querySelector(".chip-separator") as HTMLDivElement
    val buttonDivElement = node.querySelector(".chip-buttons") as HTMLDivElement
    val enableButton = node.querySelector(".chip-enable-btn") as HTMLDivElement
    val editButton = node.querySelector(".chip-edit-btn") as HTMLDivElement
    val deleteButton = node.querySelector(".chip-delete-btn") as HTMLDivElement

    var editCallback: ((String) -> Unit)? = null
    var deleteCallback: ((String) -> Unit)? = null
    var toggleCallback: ((String) -> Unit)? = null

    companion object {
        fun createFromTemplate(): HTMLDivElement {
            val template = document.getElementById("decoder-chip-template") as HTMLTemplateElement
            val cloned = template.content.cloneNode(true) as DocumentFragment
            return cloned.querySelector(".chip") as HTMLDivElement
        }
    }

    init {
        setStatus(isEnabled)

        node.setAttribute("data-chip-id", id)
        node.setAttribute("draggable", sortable.toString())

        nameElement.innerHTML = displayName

        if (toggleable) {
            enableButton.onclick = { event ->
                onToggleClick()
            }
        } else {
            enableButton.style.display = "none"
        }

        if (editable) {
            editButton.onclick = { event ->
                onEditClick()
            }
        } else {
            editButton.style.display = "none"
        }

        if (deletable) {
            deleteButton.onclick = { event ->
                onDeleteClick()
            }
        } else {
            deleteButton.style.display = "none"
        }

        if (!toggleable && !editable && !deletable) {
            separatorElement.style.display = "none"
            buttonDivElement.style.display = "none"
        }
    }

    fun setEditCallback(callback: (String) -> Unit) {
        editCallback = callback
    }

    fun setDeleteCallback(callback: (String) -> Unit) {
        deleteCallback = callback
    }

    fun setToggleCallback(callback: (String) -> Unit) {
        toggleCallback = callback
    }

    fun onEditClick() {
        editCallback?.invoke(id)
    }

    fun onDeleteClick() {
        deleteCallback?.invoke(id)
    }

    fun onToggleClick() {
        toggleCallback?.invoke(id)
    }

    fun setStatus(value: Boolean) {
        isEnabled = value

        node.classList.toggle("chip-enabled", isEnabled)
        node.classList.toggle("chip-disabled", !isEnabled)

        enableButton.classList.toggle("icon-toggle-right-black", isEnabled)
        enableButton.classList.toggle("icon-toggle-left-black", !isEnabled)
    }

    fun statusDisable() {
        setStatus(false)
    }

    fun statusEnable() {
        setStatus(true)
    }

    fun statusToggle() {
        setStatus(!isEnabled)
    }

}

typealias EventListener = (List<String>) -> Unit

class ChipList(
    val node: HTMLDivElement,
    private val canSort: Boolean = true,
    private val canDelete: Boolean = true,
    private val canEdit: Boolean = true,
    private val canToggleEnabled: Boolean = true,
    private val deleteConfirmationCallback: ((String) -> Boolean)? = null,
) {
    private val itemStore = mutableMapOf<String, ChipListItem>()
    private val dragAndDropHandler = DragAndDropHandler()
    private val noDecodersMessage: HTMLDivElement
    private val eventListeners: MutableMap<String, MutableSet<EventListener>> = mutableMapOf()
    private var defaultOrder: List<String> = listOf()

    init {
        node.classList.toggle("sortable", canSort)

        val d = document.createElement("div") as HTMLDivElement
        d.innerHTML = "no decoders available"
        d.style.fontStyle = "italic"
        d.style.color = "gray"
        d.style.padding = "8px"
        noDecodersMessage = d;
        node.appendChild(noDecodersMessage)

        node.ondragstart = { event ->
            dragAndDropHandler.handleDragStart(event)
        }
        node.ondragend = { event ->
            dragAndDropHandler.handleDragEnd(event)
        }
        node.ondragover = { event ->
            dragAndDropHandler.handleDragOver(event)
        }
        node.ondragenter = { event ->
            dragAndDropHandler.handleDragEnter(event)
        }
        node.ondrop = { event ->
            dragAndDropHandler.handleDrop(event)
        }
    }

    fun getItemIdsOrdered(): List<String> {
        //return container.querySelectorAll(".chip").asList().mapNotNull { it.getAttribute("data-chip-id") }
        return node.querySelectorAll(".chip").asList()
            .mapNotNull { node -> (node as HTMLElement).getAttribute("data-chip-id") }
    }

    val allItems: List<ChipListItem>
        get() = getItemIdsOrdered().mapNotNull { key -> itemStore[key] }

    val enabledItems: List<ChipListItem>
        get() = allItems.filter { item -> item.isEnabled }

    val disabledItems: List<ChipListItem>
        get() = allItems.filter { item -> !item.isEnabled }

    fun addItem(item: ChipListItem) {
        if (canEdit) {
            item.setEditCallback { itemId ->
                dispatchEvents("itemEdited", affectedIds = listOf(itemId))
            }
        }

        if (canDelete) {
            item.setDeleteCallback { itemId ->
                deleteItem(itemId)
            }
        }

        if (canToggleEnabled) {
            item.setToggleCallback { itemId ->
                toggleItem(itemId)
            }
        }

        itemStore[item.id] = item
        node.appendChild(item.node)

        if (node.contains(noDecodersMessage)) {
            node.removeChild(noDecodersMessage)
        }
        dispatchEvents("itemAdded", affectedIds = listOf(item.id))
    }

    fun addItem(id: String, displayName: String, isEnabled: Boolean = true) {
        addItem(
            ChipListItem(
                node = ChipListItem.createFromTemplate(),
                id = id,
                displayName = displayName,
                sortable = canSort,
                toggleable = canToggleEnabled,
                editable = canEdit,
                deletable = canDelete,
                isEnabled = isEnabled
            )
        )
    }

    fun deleteItem(id: String): Boolean {
        // If a delete confirmation callback is set, call it and check the result. Otherwise, proceed with deletion
        val deleteConfirmed = deleteConfirmationCallback?.invoke(id) ?: true
        if (!deleteConfirmed) return false

        return itemStore.remove(id)?.let {
            node.removeChild(it.node)
            if (itemStore.isEmpty()) {
                node.appendChild(noDecodersMessage)
            }
            dispatchEvents("itemRemoved", affectedIds = listOf(id))
            true
        } ?: false
    }

    fun toggleItem(id: String): Boolean {
        return itemStore[id]?.let {
            it.statusToggle()
            when (it.isEnabled) {
                true -> dispatchEvents("itemEnabled", affectedIds = listOf(id))
                false -> dispatchEvents("itemDisabled", affectedIds = listOf(id))
            }
            true
        } ?: false
    }

    fun setItemStatus(id: String, status: Boolean): Boolean {
        return itemStore[id]?.let {
            it.setStatus(status)
            true
        } ?: false
    }

    fun setItemStatusForAll(status: Boolean) {
        allItems.forEach { item -> item.setStatus(status) }
    }

    fun getItem(id: String): ChipListItem? {
        return itemStore[id]
    }

    fun clear() {
        allItems.forEach { item ->
            node.removeChild(item.node)
        }
        val affectedIds = getItemIdsOrdered()
        itemStore.clear()
        node.appendChild(noDecodersMessage)
        dispatchEvents("listCleared", affectedIds = affectedIds)
    }

    val size: Int
        get() = itemStore.size

    val isEmpty: Boolean
        get() = itemStore.isEmpty()

    fun saveDefaultOrder() {
        defaultOrder = getItemIdsOrdered()
    }

    fun getDefaultOrder(): List<String> {
        return defaultOrder.ifEmpty {
            getItemIdsOrdered()
        }
    }

    fun resetDefaultOrder() {
        setOrder(getDefaultOrder())
    }

    fun setOrder(order: List<String>): Boolean {
        if (order.toSet() != itemStore.keys) return false
        var referenceNode = itemStore[order.last()]!!.node
        order.reversed().forEach { id ->
            val reorderedNode = itemStore[id]!!.node
            node.insertBefore(reorderedNode, referenceNode)
            referenceNode = reorderedNode
        }
        dispatchEvents("orderChanged", affectedIds = order)
        return true
    }

    private fun dispatchEvents(vararg eventTypes: String, affectedIds: List<String> = listOf()) {
        /*
        Supported event types:
        - itemAdded
        - itemRemoved
        - itemEnabled
        - itemDisabled
        - itemEdited
        - listCleared
        - orderChanged
         */
        eventTypes.forEach { eventType ->
            eventListeners[eventType]?.forEach { listener ->
                listener(affectedIds)
            }
        }
    }

    fun addEventListener(eventType: String, listener: EventListener) {
        if (eventListeners[eventType] == null) {
            eventListeners[eventType] = mutableSetOf()
        }
        eventListeners[eventType]?.add(listener)
    }

    fun removeEventListener(eventType: String, listener: EventListener) {
        eventListeners[eventType]?.remove(listener)
    }

    private data class Point(val x: Double, val y: Double)
    private data class AnchorPoint(
        val point: Point,
        val leftElement: HTMLElement?,
        val rightElement: HTMLElement?,
        val isOnNewLine: Boolean = false
    )

    private inner class DragAndDropHandler {
        private var container = node
        private var draggedElement: HTMLElement? = null
        private var placeholder: HTMLElement? = null
        private var linebreaker: HTMLElement? = null
        private var isDragging = false

        fun handleDragStart(event: DragEvent) {
            draggedElement = event.target as HTMLElement
            if (draggedElement == null || !draggedElement!!.classList.contains("chip")) return

            isDragging = true
            draggedElement!!.classList.add("dragging")
            container.classList.add("drag-active")

            createPlaceholder()

            event.dataTransfer?.effectAllowed = "move"
            event.dataTransfer?.setData("text/html", draggedElement!!.outerHTML)
        }

        fun handleDragEnd(event: DragEvent) {
            if (!isDragging) return

            isDragging = false

            draggedElement?.classList?.remove("dragging")
            node.classList.remove("drag-active")

            removePlaceholder()
            removeLinebreaker()

            draggedElement = null
        }

        fun handleDragOver(event: DragEvent) {
            if (!isDragging) return

            event.preventDefault()
            event.dataTransfer?.dropEffect = "move"

            removeLinebreaker()
            val anchorPoint = getBestAnchorPoint(event.clientX, event.clientY)
            if (anchorPoint.rightElement == null) {
                container.appendChild(placeholder!!)
            } else {
                if (anchorPoint.isOnNewLine) {
                    createLinebreaker()
                    container.insertBefore(linebreaker!!, anchorPoint.rightElement)
                }
                container.insertBefore(placeholder!!, anchorPoint.rightElement)
            }
        }

        fun handleDragEnter(event: DragEvent) {
            event.preventDefault()
        }

        fun handleDrop(event: DragEvent) {
            if (!isDragging) return

            event.preventDefault()

            placeholder?.parentNode?.insertBefore(draggedElement!!, placeholder)
            removePlaceholder()
            removeLinebreaker()

            dispatchEvents("orderChanged", affectedIds = getItemIdsOrdered())
        }

        private fun createPlaceholder() {
            val p = document.createElement("div") as HTMLDivElement
            p.className = "chip-drop-placeholder active"
            placeholder = p
        }

        private fun createLinebreaker() {
            val lb = document.createElement("div") as HTMLDivElement
            lb.className = "chip-drop-linebreaker"
            linebreaker = lb
        }

        private fun removePlaceholder() {
            placeholder?.parentNode?.removeChild(placeholder!!)
            placeholder = null
        }

        private fun removeLinebreaker() {
            linebreaker?.parentNode?.removeChild(linebreaker!!)
            linebreaker = null
        }

        private fun renderDot(p: Point, color: String = "red") {
            val dot = document.createElement("div") as HTMLDivElement
            dot.addClass("debug-dot")
            dot.style.backgroundColor = color
            dot.style.left = "${p.x - 3}px"
            dot.style.top = "${p.y - 3}px"
            document.body!!.appendChild(dot)
        }

        private fun getBestAnchorPoint(x: Int, y: Int): AnchorPoint {
            var draggableElements = container.querySelectorAll(".chip").asList()

            val anchorPoints: MutableList<AnchorPoint> = mutableListOf()

            for (i in -1 until draggableElements.size) {
                val leftElement = draggableElements.getOrNull(i) as? HTMLElement
                val rightElement = draggableElements.getOrNull(i + 1) as? HTMLElement
                val leftBox: DOMRect = leftElement?.getBoundingClientRect() ?: run {
                    val rightBox = rightElement!!.getBoundingClientRect()
                    DOMRect(rightBox.left, rightBox.top, 0.0, rightBox.height)
                }
                val rightBox: DOMRect = rightElement?.getBoundingClientRect() ?: run {
                    val leftBox = leftElement!!.getBoundingClientRect()
                    DOMRect(leftBox.right, leftBox.top, 0.0, leftBox.height)
                }
                val leftCenter = Point(leftBox.right - (leftBox.width / 2), leftBox.top + (leftBox.height / 2))
                val rightCenter = Point(rightBox.right - (rightBox.width / 2), rightBox.top + (rightBox.height / 2))

                val inSameRow = leftCenter.y > rightBox.top && leftCenter.y < rightBox.bottom
                // || rightCenter.y > leftBox.top && rightCenter.y < leftBox.bottom

                // midpoint between the two elements
                if (inSameRow) {
                    val midPoint = Point(
                        (leftBox.right + rightBox.left) / 2,
                        (leftCenter.y + rightCenter.y) / 2
                    )
                    anchorPoints.add(AnchorPoint(midPoint, leftElement, rightElement, false))
                } else {
                    // since the left and right elements are not in the same row, we create two midpoints
                    // end of first row
                    val midPoint1 = Point(
                        leftBox.right,
                        leftCenter.y
                    )
                    // start of second row
                    val midPoint2 = Point(
                        rightBox.left,
                        rightCenter.y
                    )
                    anchorPoints.add(AnchorPoint(midPoint1, leftElement, rightElement, false))
                    anchorPoints.add(AnchorPoint(midPoint2, leftElement, rightElement, true))
                }
            }
            data class Closest(val anchorPoint: AnchorPoint?, val xOffset: Double, val yOffset: Double)

            val closest = anchorPoints.fold(
                Closest(
                    null,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY
                )
            ) { accumulator, anchorPoint ->
                val xOffset = abs(x - anchorPoint.point.x)
                val yOffset = abs(y - anchorPoint.point.y)
                when {
                    yOffset < accumulator.yOffset -> {
                        Closest(anchorPoint, xOffset, yOffset)
                    }

                    yOffset > accumulator.yOffset -> {
                        accumulator
                    }

                    xOffset < accumulator.xOffset -> {
                        Closest(anchorPoint, xOffset, yOffset)
                    }

                    xOffset > accumulator.xOffset -> {
                        accumulator
                    }

                    else -> accumulator
                }

            }

            return closest.anchorPoint!!
        }
    }

}
