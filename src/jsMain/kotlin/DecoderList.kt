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
import org.w3c.dom.events.Event
import kotlin.math.abs

data class ChipListItem(
    val node: HTMLDivElement,
    val id: String,
    val displayName: String,
    val sortable: Boolean,
    val toggleable: Boolean,
    val deletable: Boolean,
) {
    //val divElement = node.querySelector(".chip") as HTMLDivElement
    val divElement = node
    val nameElement = node.querySelector(".chip-name") as HTMLSpanElement
    val separatorElement = node.querySelector(".chip-separator") as HTMLDivElement
    val buttonDivElement = node.querySelector(".chip-buttons") as HTMLDivElement
    val enableButton = node.querySelector(".chip-enable-btn") as HTMLDivElement
    val deleteButton = node.querySelector(".chip-delete-btn") as HTMLDivElement

    var isEnabled: Boolean = true

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
        statusEnable()
        divElement.setAttribute("data-chip-id", id)
        divElement.setAttribute("draggable", sortable.toString())

        if (sortable) {
            divElement.ondragstart = { event ->
                console.log("ondragstart", event)
                event.dataTransfer?.effectAllowed = "move"
                event.dataTransfer?.setData("id", id)
            }
        }

        nameElement.innerHTML = displayName

        if (toggleable) {
            enableButton.onclick = { event ->
                onToggleClick()
            }
        } else {
            enableButton.style.display = "none"
        }

        if (deletable) {
            deleteButton.onclick = { event ->
                onDeleteClick()
            }
        } else {
            deleteButton.style.display = "none"
        }

        if (!toggleable && !deletable) {
            separatorElement.style.display = "none"
            buttonDivElement.style.display = "none"
        }
    }

    /**
     * Sets the delete callback function
     */
    fun setDeleteCallback(callback: (String) -> Unit) {
        deleteCallback = callback
    }

    /**
     * Sets the toggle callback function
     */
    fun setToggleCallback(callback: (String) -> Unit) {
        toggleCallback = callback
    }

    /**
     * Handles the delete button click
     */
    fun onDeleteClick() {
        deleteCallback?.invoke(id)
    }

    /**
     * Handles the toggle button click
     */
    fun onToggleClick() {
        toggleCallback?.invoke(id)
    }

    fun setStatus(value: Boolean) {
        isEnabled = value

        divElement.classList.toggle("chip-enabled", isEnabled)
        divElement.classList.toggle("chip-disabled", !isEnabled)

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


/**
 * A customizable list of chip elements with support for sorting, deletion, and enabling/disabling
 */
class ChipList(
    val node: HTMLDivElement,
    private val canSort: Boolean = true,
    private val canDelete: Boolean = true,
    private val canToggleEnabled: Boolean = true
) {
    private val itemStore = mutableMapOf<String, ChipListItem>()

    private var itemToggleCallback: ((String, Boolean) -> Unit)? = null

    // drag and drop functionality
    var container = node
    var draggedElement: HTMLElement? = null
    var placeholder: HTMLElement? = null
    var linebreaker: HTMLElement? = null
    var isDragging = false

    companion object {
        fun createFromTemplate(): DocumentFragment {
            val template = document.getElementById("decoder-list-template") as HTMLTemplateElement
            return template.content.cloneNode(true) as DocumentFragment
        }
    }

    init {
        node.classList.toggle("sortable", canSort)

        node.ondragstart = { event ->
            handleDragStart(event)
        }
        node.ondragend = { event ->
            handleDragEnd(event)
        }
        node.ondragover = { event ->
            handleDragOver(event)
        }
        node.ondragenter = { event ->
            handleDragEnter(event)
        }
        node.ondrop = { event ->
            handleDrop(event)
        }
    }

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

        // Clean up visual feedback
        draggedElement?.classList?.remove("dragging")
        node.classList.remove("drag-active")

        // Remove placeholder
        removePlaceholder()
        removeLinebreaker()

        draggedElement = null
    }

    fun handleDragOver(event: DragEvent) {
        if (!isDragging) return

        event.preventDefault()
        event.dataTransfer?.dropEffect = "move"

        // Find the closest chip element
        val anchorPoint = getBestAnchorPoint(event.clientX, event.clientY)
        // Insert placeholder
        removeLinebreaker()
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

        onOrderChange()
    }

    fun createPlaceholder() {
        val p = document.createElement("div") as HTMLDivElement
        p.className = "drop-indicator active"
        p.style.width = "2px"
        p.style.height = "32px"
        p.style.backgroundColor = "#2196f3"
        p.style.borderRadius = "1px"
        p.style.margin = "0 4px"
        placeholder = p
    }

    fun createLinebreaker() {
        val lb = document.createElement("div") as HTMLDivElement
        lb.className = "linebreaker"
        lb.style.width = "100%"
        linebreaker = lb
    }

    fun removePlaceholder() {
        placeholder?.parentNode?.removeChild(placeholder!!)
        placeholder = null
    }

    fun removeLinebreaker() {
        linebreaker?.parentNode?.removeChild(linebreaker!!)
        linebreaker = null
    }

    private data class Point(val x: Double, val y: Double)
    private data class AnchorPoint(
        val point: Point,
        val leftElement: HTMLElement?,
        val rightElement: HTMLElement?,
        val isOnNewLine: Boolean = false
    )

    private fun getBestAnchorPoint(x: Int, y: Int): AnchorPoint {
        var draggableElements = container.querySelectorAll(".chip").asList()

        // remove all elements with class "debug-dot"
        /*document.querySelectorAll(".debug-dot").asList().forEach {
            it.parentNode?.removeChild(it)
        }*/

        val anchorPoints: MutableList<AnchorPoint> = mutableListOf()
        fun renderDot(p: Point, color: String = "red") {
            val dot = document.createElement("div") as HTMLDivElement
            dot.addClass("debug-dot")
            dot.style.display = "block"
            dot.style.position = "absolute"
            dot.style.width = "6px"
            dot.style.height = "6px"
            dot.style.backgroundColor = color
            dot.style.borderRadius = "3px"
            dot.style.left = "${p.x - 3}px"
            dot.style.top = "${p.y - 3}px"
            dot.style.zIndex = "1000"
            document.body!!.appendChild(dot)
        }

        for (i in -1 until draggableElements.size) {
            val leftNode = draggableElements.getOrNull(i) as? HTMLElement
            val rightNode = draggableElements.getOrNull(i + 1) as? HTMLElement
            val leftBox: DOMRect = leftNode?.getBoundingClientRect() ?: run {
                val rightBox = rightNode!!.getBoundingClientRect()
                DOMRect(rightBox.left, rightBox.top, 0.0, rightBox.height)
            }
            val rightBox: DOMRect = rightNode?.getBoundingClientRect() ?: run {
                val leftBox = leftNode!!.getBoundingClientRect()
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
                anchorPoints.add(AnchorPoint(midPoint, leftNode, rightNode, false))
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
                anchorPoints.add(AnchorPoint(midPoint1, leftNode, rightNode, false))
                anchorPoints.add(AnchorPoint(midPoint2, leftNode, rightNode, true))
            }
            //renderDot(rightCenter, "green")
        }
        /*anchorPoints.forEach { midPoint ->
            renderDot(midPoint.point, if (midPoint.isOnNewLine) "blue" else "yellow")
        }*/

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

        //renderDot(closest.anchorPoint!!.point, "red")
        return closest.anchorPoint!!
    }

    fun onOrderChange() {
        // Get the current order of chip IDs
        container.dispatchEvent(
            Event("orderChanged")
        )
    }

    fun setItemToggleCallback(callback: (String, Boolean) -> Unit) {
        itemToggleCallback = callback
    }

    private fun getItemKeysOrdered(): List<String> {
        //return container.querySelectorAll(".chip").asList().mapNotNull { it.getAttribute("data-chip-id") }
        return container.querySelectorAll(".chip").asList().mapNotNull { node -> (node as HTMLElement).getAttribute("data-chip-id") }
    }

    /**
     * Gets all items in the list regardless of their enabled state
     */
    val allItems: List<ChipListItem>
        get() = getItemKeysOrdered().mapNotNull { key -> itemStore[key] }


    /**
     * Gets only enabled items
     */
    val enabledItems: List<ChipListItem>
        get() = allItems.filter { item -> item.isEnabled }

    /**
     * Gets only disabled items
     */
    val disabledItems: List<ChipListItem>
        get() = allItems.filter { item -> !item.isEnabled }

    /**
     * Adds an item to the list
     */
    fun addItem(item: ChipListItem) {
        if (canDelete) {
            item.setDeleteCallback { itemId ->
                console.log("ChipList Lambda deleteItem", itemId)
                deleteItem(itemId)
            }
        }

        if (canToggleEnabled) {
            item.setToggleCallback { itemId ->
                console.log("ChipList Lambda toggleItem", itemId)
                toggleItem(itemId)
            }
        }

        itemStore[item.id] = item
        node.appendChild(item.node)
    }

    /**
     * Adds an item to the list by creating a new ChipListItem
     */
    fun addItem(id: String, displayName: String, isEnabled: Boolean = true) {
        addItem(ChipListItem(
            node = ChipListItem.createFromTemplate(),
            id = id,
            displayName = displayName,
            sortable = canSort,
            toggleable = canToggleEnabled,
            deletable = canDelete,
        ))
    }

    /**
     * Removes an item from the list by ID
     */
    fun deleteItem(id: String): Boolean {
        return itemStore.remove(id)?.let {
            node.removeChild(it.node)
            true
        } ?: false
    }

    /**
     * Toggles the enabled state of an item
     */
    fun toggleItem(id: String): Boolean {
        return itemStore[id]?.let {
            it.statusToggle()
            itemToggleCallback?.invoke(id, it.isEnabled)
            true
        } ?: false
    }

    /**
     * Sets the enabled state of an item
     */
    fun setItemEnabled(id: String, enabled: Boolean): Boolean {
        return itemStore[id]?.let {
            it.setStatus(enabled)
            true
        } ?: false
    }

    /**
     * Finds an item by ID
     */
    fun getItem(id: String): ChipListItem? {
        return itemStore[id]
    }

    /**
     * Clears all items from the list
     */
    fun clear() {
        allItems.forEach { item ->
            node.removeChild(item.node)
        }
        itemStore.clear()
    }

    /**
     * Gets the current size of the list
     */
    val size: Int
        get() = itemStore.size

    /**
     * Checks if the list is empty
     */
    val isEmpty: Boolean
        get() = itemStore.isEmpty()

}
