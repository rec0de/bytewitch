import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.properties.Delegates

// TODO: Convert the storage to a session storage
class TwoWayTextAreaBinding(elementId: String, storageKey: String? = null) {
    private val inputElement = document.getElementById(elementId) as HTMLTextAreaElement

    var value: String by Delegates.observable(inputElement.value) { _, _, newValue ->
        inputElement.value = newValue
        // TODO: Should this be debounced?
        storageKey?.also {
            localStorage.setItem(it, newValue)
        }
    }

    var onInput: ((value: String) -> Unit)? = null

    init {
        storageKey?.also { key ->
            localStorage.getItem(key)?.also { storedValue ->
                value = storedValue
            }
        }

        inputElement.oninput = {
            value = inputElement.value
            onInput?.invoke(value)
        }
    }
}

// TODO: Convert the storage to a session storage
class TwoWayCheckboxBinding(elementId: String, storageKey: String? = null) {
    private val checkboxElement = document.getElementById(elementId) as HTMLInputElement

    var checked: Boolean by Delegates.observable(checkboxElement.checked) { _, _, newValue ->
        checkboxElement.checked = newValue
        storageKey?.also {
            localStorage.setItem(it, newValue.toString())
        }
    }

    var onChange: ((checked: Boolean) -> Unit)? = null

    init {
        storageKey?.also { key ->
            localStorage.getItem(key)?.toBoolean()?.also { storedValue ->
                checked = storedValue
            }
        }

        checkboxElement.onchange = {
            checked = checkboxElement.checked
            onChange?.invoke(checked)
        }
    }
}
