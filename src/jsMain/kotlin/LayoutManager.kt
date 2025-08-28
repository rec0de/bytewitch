import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

object LayoutManager {
    
    private val settingsBtn = document.getElementById("settings-btn") as HTMLButtonElement
    private val kaitaiBtn = document.getElementById("kaitai-btn") as HTMLButtonElement
    private val container = document.getElementById("container") as HTMLDivElement
    private val settingsCard = document.getElementById("settings") as HTMLDivElement
    private val kaitaiCard = document.getElementById("kaitai") as HTMLDivElement

    private var settingsVisible = !settingsCard.classList.contains("card-hidden")
    private var kaitaiVisible = !kaitaiCard.classList.contains("card-hidden")

    init {
        settingsBtn.onclick = { toggleSettings() }
        kaitaiBtn.onclick = { toggleKaitai() }
        updateLayout()
    }

    fun updateLayout() {
        container.classList.remove("editor-only", "editor-settings", "editor-kaitai", "all-visible")

        when {
            !settingsVisible && !kaitaiVisible -> container.classList.add("editor-only")
            settingsVisible && !kaitaiVisible -> container.classList.add("editor-settings")
            !settingsVisible && kaitaiVisible -> container.classList.add("editor-kaitai")
            else -> container.classList.add("all-visible")
        }
    }

    fun toggleSettings() {
        settingsVisible = !settingsVisible
        settingsBtn.textContent = if (settingsVisible) "hide settings" else "show settings"
        settingsCard.classList.toggle("card-hidden", !settingsVisible)
        updateLayout()
    }

    fun toggleKaitai() {
        kaitaiVisible = !kaitaiVisible
        kaitaiBtn.textContent = if (kaitaiVisible) "hide kaitai" else "show kaitai"
        kaitaiCard.classList.toggle("card-hidden", !kaitaiVisible)
        updateLayout()
    }
}