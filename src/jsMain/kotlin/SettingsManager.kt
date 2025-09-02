import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList

object SettingsManager {

    private val ssfDecodingCheckbox = document.getElementById("ssfdecode") as HTMLInputElement
    private val showInstancesCheckbox = document.getElementById("show-instances") as HTMLInputElement

    init {
        ssfEnabled = ssfDecodingCheckbox.checked
        ssfDecodingCheckbox.onchange = {
            ssfEnabled = ssfDecodingCheckbox.checked
            decode(false, force = true)
        }

        showInstances = showInstancesCheckbox.checked
        showInstancesCheckbox.onchange = {
            showInstances = showInstancesCheckbox.checked
            // TODO: hide/show all displayed instances with display:none/block
            document.getElementsByClassName("kaitai-instance").asList().forEach {
                (it as? HTMLDivElement)?.style?.display = if (showInstances) "block" else "none"
            }
        }
    }

}