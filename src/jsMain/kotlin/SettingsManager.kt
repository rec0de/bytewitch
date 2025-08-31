import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement

object SettingsManager {

    private val ssfDecodingElement = document.getElementById("ssfdecode") as HTMLInputElement

    init {
        ssfEnabled = ssfDecodingElement.checked

        ssfDecodingElement.onchange = {
            ssfEnabled = ssfDecodingElement.checked
            decode(false, force = true)
        }
    }

}