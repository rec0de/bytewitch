import kotlinx.browser.document
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
        fun createFromTemplate() : HTMLDivElement {
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
                console.log("enableButton.onclick")
                onToggleClick()
            }
        } else {
            enableButton.style.display = "none"
        }

        if (deletable) {
            deleteButton.onclick = { event ->
                console.log("deleteButton.onclick")
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

    /*
    fun renderHTML(
        canDelete: Boolean = false,
        canToggleEnabled: Boolean = false,
        canSort: Boolean = false
    ): String {
        val enabledClass = if (isEnabled) "chip-enabled" else "chip-disabled"
        val sortableAttr = if (canSort) "draggable=\"true\"" else ""
        val dataId = "data-chip-id=\"$id\""

        val buttonsHtml = buildString {
            if (canToggleEnabled || canDelete) {
                append("<div class=\"chip-separator\"></div>")
                append("<div class=\"chip-buttons\">")

                if (canToggleEnabled) {
                    val toggleText = if (isEnabled) "Disable" else "Enable"
                    val toggleClass = if (isEnabled) "disable-btn" else "enable-btn"
                    append("<button class=\"chip-btn $toggleClass\" onclick=\"chipList.toggleItem('$id')\">$toggleText</button>")
                }

                if (canDelete) {
                    append("<button class=\"chip-btn delete-btn\" onclick=\"chipList.deleteItem('$id')\">×</button>")
                }

                append("</div>")
            }
        }

        return """
            <div class="chip $enabledClass" $dataId $sortableAttr>
                <span class="chip-name" ${if (canSort) "style=\"cursor: move;\"" else ""}>$displayName</span>
                $buttonsHtml
            </div>
        """.trimIndent()
    }*/
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
    private val _items = mutableListOf<ChipListItem>()

    private var itemToggleCallback: ((String, Boolean) -> Unit)? = null

    // drag and drop functionality
    var container = node
    var draggedElement: HTMLElement? = null
    var placeholder: HTMLElement? = null
    var isDragging = false
    
    companion object {
        fun createFromTemplate() : DocumentFragment {
            val template = document.getElementById("decoder-list-template") as HTMLTemplateElement
            return template.content.cloneNode(true) as DocumentFragment
        }
    }

    init {
        node.classList.toggle("sortable", canSort)

        node.ondragstart = { event ->
            console.log("ChipList.ondragstart", event)
            handleDragStart(event)
        }
        node.ondragend = { event ->
            console.log("ChipList.ondragend", event)
            handleDragEnd(event)
        }
        node.ondragover = { event ->
            //console.log("ChipList.ondragover", event)
            handleDragOver(event)
        }
        node.ondragenter = { event ->
            //console.log("ChipList.ondragenter", event)
            handleDragEnter(event)
        }
        node.ondrop = { event ->
            console.log("ChipList.ondrop", event)
            handleDrop(event)
        }
    }

    fun handleDragStart(event: DragEvent) {

        //if (!event.target?.classList.contains("chip")) return

        this.draggedElement = event.target as HTMLElement
        this.isDragging = true

        // Add visual feedback
        this.draggedElement?.classList?.add("dragging")
        this.container.classList.add("drag-active")

        // Create placeholder
        this.createPlaceholder()

        // Set drag data
        event.dataTransfer?.effectAllowed = "move"
        event.dataTransfer?.setData("text/html", this.draggedElement!!.outerHTML)
    }

    fun handleDragEnd(event: DragEvent) {
        if (!isDragging) return

        isDragging = false

        // Clean up visual feedback
        draggedElement?.classList?.remove("dragging")
        node.classList.remove("drag-active")

        // Remove placeholder
        removePlaceholder()

        draggedElement = null
    }

    fun handleDragOver(event: DragEvent) {
        if (!this.isDragging) return

        event.preventDefault()
        event.dataTransfer?.dropEffect = "move"

        // Find the closest chip element
        val afterElement = getDragAfterElement(event.clientX, event.clientY)
        val draggedElement = draggedElement

        if (afterElement == null) {
            // Append to the end
            this.container.appendChild(placeholder!!)
        } else {
            // Insert before the found element
            this.container.insertBefore(placeholder!!, afterElement)
        }
    }

    fun handleDragEnter(event: DragEvent) {
        event.preventDefault()
    }

    fun handleDrop(event: DragEvent) {
        if (!isDragging) return

        event.preventDefault()

        // Replace placeholder with the dragged element
        placeholder?.parentNode?.insertBefore(draggedElement!!, placeholder)
        removePlaceholder()

        // Trigger custom event for order change
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

    fun removePlaceholder() {
        placeholder?.parentNode?.removeChild(placeholder!!)
        placeholder = null
    }

    fun getDragAfterElement(x: Int, y: Int): HTMLElement? {
        val draggableElements = container.querySelectorAll(".chip").asList()

        draggableElements.forEach { element ->
            val child = element as HTMLElement
            val childId = child.getAttribute("data-chip-id")
            val box = child.getBoundingClientRect()
            val childCenterX = box.right - (box.width / 2)
            val childCenterY = box.top + (box.height / 2)
            val xOffset = abs(x - childCenterX)
            val yOffset = abs(y - childCenterY)
            val cursorInRowOfElement = y >= box.top && y <= box.bottom
            //console.log("$childId: ($xOffset, $yOffset)", cursorInRowOfElement)
        }

        data class Closest(val element: HTMLElement?, val xOffset: Double, val yOffset: Double)
        //data class Closest(val element: HTMLElement?, val offset: Double)

        val closest = draggableElements.fold(Closest(null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)) {
        //val closest = draggableElements.fold(Closest(null, Double.NEGATIVE_INFINITY)) {
            accumulator, element ->
            val child = element as HTMLElement
            val childId = child.getAttribute("data-chip-id")
            val box = child.getBoundingClientRect()
            val childCenterX = box.right - (box.width / 2)
            val childCenterY = box.top + (box.height / 2)
            val xOffset = abs(x - childCenterX)
            val yOffset = abs(y - childCenterY)

            val cursorInCurrentRow = y >= box.top && y <= box.bottom
            if (cursorInCurrentRow) {
                //console.log("cursor is in row of", childId)

            }

            when {
                yOffset < accumulator.yOffset -> {
                    Closest(child, xOffset, yOffset)
                }
                yOffset > accumulator.yOffset -> {
                    accumulator
                }
                xOffset < accumulator.xOffset -> {
                    Closest(child, xOffset, yOffset)
                }
                xOffset > accumulator.xOffset -> {
                    accumulator
                }
                else -> accumulator
            }

            /*
            if (x < box.left) {
                console.log("grabbed element is left of", childId)
            }
            if (x > box.left) {
                console.log("grabbed element is right of", childId)
            }
            if (y < box.top) {
                console.log("grabbed element is above of", childId)
            }
            if (y > box.top) {
                console.log("grabbed element is below of", childId)
            }*/

            /*
            when {
                y < box.top -> {
                    // Mouse is above this element -> prev row
                    Closest(child, offset)
                }
                y > box.bottom -> {
                    // Mouse is below this element -> next row
                    accumulator
                }
                offset < 0 && offset > accumulator.offset -> {
                    Closest(child, offset)
                }
                else -> accumulator
            }
             */
        }
        var closestElement = closest.element
        val box = closestElement!!.getBoundingClientRect()
        val closestCenterX = box.right - (box.width / 2)
        val rightOfCenter = x > closestCenterX
        val cursorBelowCurrentRow = y > box.bottom

        if (rightOfCenter) {
            closestElement = closestElement.nextElementSibling as? HTMLElement
        }
        //return closest.element
        val cursorBreak = !cursorBelowCurrentRow
        //console.log("rightOfCenter: $rightOfCenter", "cursorBelowCurrentRow: $cursorBelowCurrentRow")
        return closestElement
    }
    
    fun onOrderChange() {
        // Get the current order of chip IDs
        val chips = container.querySelectorAll(".chip")
        /*
        val order = chips.map(chip => chip.getAttribute("data-chip-id"))

        // You can dispatch a custom event or call a callback here
        console.log("New order:", order)

        // Dispatch custom event
        val event = CustomEvent("chipOrderChanged", {
            detail: { order: order }
        })
        container.dispatchEvent(event)
        */
    }

    fun setItemToggleCallback(callback: (String, Boolean) -> Unit) {
        itemToggleCallback = callback
    }

    /**
     * Gets all items in the list regardless of their enabled state
     */
    val allItems: List<ChipListItem>
        get() = _items.toList()

    /**
     * Gets only enabled items
     */
    val enabledItems: List<ChipListItem>
        get() = _items.filter { it.isEnabled }

    /**
     * Gets only disabled items
     */
    val disabledItems: List<ChipListItem>
        get() = _items.filter { !it.isEnabled }

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

        _items.add(item)
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
        console.log("ChipList.deleteItem")
        val itemIndex = _items.indexOfFirst { it.id == id }
        return if (itemIndex != -1) {
            val item = _items.removeAt(itemIndex)
            console.log(node.children, node.childNodes, item.node)
            node.removeChild(item.node)
            true
        } else {
            false
        }
    }

    /**
     * Toggles the enabled state of an item
     */
    fun toggleItem(id: String): Boolean {
        val item = _items.find { it.id == id }
        console.log(id, item)
        return if (item != null) {
            item.statusToggle()
            itemToggleCallback?.invoke(id, item.isEnabled)
            true
        } else {
            false
        }
    }

    /**
     * Sets the enabled state of an item
     */
    fun setItemEnabled(id: String, enabled: Boolean): Boolean {
        val item = _items.find { it.id == id }
        return if (item != null) {
            item.setStatus(enabled)
            true
        } else {
            false
        }
    }

    /**
     * Finds an item by ID
     */
    fun findItem(id: String): ChipListItem? {
        return _items.find { it.id == id }
    }

    /**
     * Moves an item from one position to another (for drag and drop sorting)
     */
    fun moveItem(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex < 0 || fromIndex >= _items.size || toIndex < 0 || toIndex >= _items.size) {
            return false
        }

        val item = _items.removeAt(fromIndex)
        _items.add(toIndex, item)
        return true
    }

    /**
     * Moves an item by ID to a new position
     */
    fun moveItemById(itemId: String, toIndex: Int): Boolean {
        val fromIndex = _items.indexOfFirst { it.id == itemId }
        return if (fromIndex != -1) {
            moveItem(fromIndex, toIndex)
        } else {
            false
        }
    }

    /**
     * Clears all items from the list
     */
    fun clear() {
        _items.clear()
    }

    /**
     * Gets the current size of the list
     */
    val size: Int
        get() = _items.size

    /**
     * Checks if the list is empty
     */
    val isEmpty: Boolean
        get() = _items.isEmpty()


    /**
     * Handles the drop event for drag and drop sorting
     * This method would be called from JavaScript
     */
    fun handleDrop(draggedItemId: String, targetItemId: String): Boolean {
        if (!canSort) return false

        val draggedIndex = _items.indexOfFirst { it.id == draggedItemId }
        val targetIndex = _items.indexOfFirst { it.id == targetItemId }

        return if (draggedIndex != -1 && targetIndex != -1 && draggedIndex != targetIndex) {
            moveItem(draggedIndex, targetIndex)
        } else {
            false
        }
    }

}
