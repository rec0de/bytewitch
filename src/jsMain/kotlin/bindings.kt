import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.KeyboardEvent
import kotlin.properties.Delegates

class TwoWayInputBinding(val element: HTMLInputElement, storageKey: String? = null) {

    var value: String by Delegates.observable(element.value) { _, _, newValue ->
        element.value = newValue
        // TODO: Should this be debounced?
        storageKey?.also {
            sessionStorage.setItem(it, newValue)
        }
    }

    var onInput: ((value: String) -> Unit)? = null
    var onEnterPressed: ((value: String) -> Unit)? = null

    init {
        storageKey?.also { key ->
            sessionStorage.getItem(key)?.let { storedValue ->
                value = storedValue
            } ?: sessionStorage.setItem(key, value)
        }

        element.addEventListener("input", {
            value = element.value
            onInput?.invoke(value)
        })

        element.addEventListener("keydown", { event ->
            if ((event as KeyboardEvent).key == "Enter") {
                onEnterPressed?.invoke(value)
            }
        })
    }
}

class TwoWayTextAreaBinding(val element: HTMLTextAreaElement, storageKey: String? = null) {

    var value: String by Delegates.observable(element.value) { _, _, newValue ->
        element.value = newValue
        // TODO: Should this be debounced?
        storageKey?.also {
            sessionStorage.setItem(it, newValue)
        }
    }

    var onInput: ((value: String) -> Unit)? = null

    init {
        storageKey?.also { key ->
            sessionStorage.getItem(key)?.let { storedValue ->
                value = storedValue
            } ?: sessionStorage.setItem(key, value)
        }

        // use addEventListener to avoid overwriting (or being overwritten by) other listeners
        element.addEventListener("input", {
            value = element.value
            onInput?.invoke(value)
        })
    }
}

class TwoWayCheckboxBinding(val element: HTMLInputElement, storageKey: String? = null) {

    var checked: Boolean by Delegates.observable(element.checked) { _, _, newValue ->
        element.checked = newValue
        storageKey?.also {
            sessionStorage.setItem(it, newValue.toString())
        }
    }

    var onChange: ((checked: Boolean) -> Unit)? = null

    init {
        storageKey?.also { key ->
            sessionStorage.getItem(key)?.toBoolean()?.let { storedValue ->
                checked = storedValue
            } ?: sessionStorage.setItem(key, checked.toString())
        }

        element.addEventListener("change", {
            checked = element.checked
            onChange?.invoke(checked)
        })
    }
}
