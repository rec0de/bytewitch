import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList

object SettingsManager {

    private val ssfDecodingBinding = TwoWayCheckboxBinding(
        document.getElementById("ssf-decode") as HTMLInputElement,
        "ssf-decode"
    )
    private val showInstancesBinding = TwoWayCheckboxBinding(
        document.getElementById("show-instances") as HTMLInputElement,
        "show-instances"
    )

    init {
        ssfEnabled = ssfDecodingBinding.checked
        ssfDecodingBinding.onChange = { checked ->
            ssfEnabled = checked
            decode(false, force = true)
        }

        val outputDiv = document.getElementById("output") as HTMLDivElement
        if (showInstancesBinding.checked) {
            outputDiv.classList.remove("kaitai-instances-hidden")
        } else {
            outputDiv.classList.add("kaitai-instances-hidden")
        }
        showInstancesBinding.onChange = { checked ->
            when (checked) {
                true -> outputDiv.classList.remove("kaitai-instances-hidden")
                false -> outputDiv.classList.add("kaitai-instances-hidden")
            }
        }
    }

}