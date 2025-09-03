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

        showInstances = showInstancesBinding.checked
        showInstancesBinding.onChange = { checked ->
            showInstances = checked
            document.getElementsByClassName("kaitai-instance").asList().forEach {
                (it as? HTMLDivElement)?.style?.display = if (showInstances) "block" else "none"
            }
        }
    }

}