import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement


@Serializable
data class KaitaiManifest(
    val files: List<String>,
)

object KaitaiUI {
    private val nameInput = document.getElementById("kaitai-name") as HTMLInputElement
    private val addButton = document.getElementById("add-kaitai") as HTMLButtonElement
    private val kaitaiInput = TwoWayTextAreaBinding("kaitaiinput")
    private val kaitaiValid = document.getElementById("kaitai-valid") as HTMLDivElement
    private val bundledLegendContainer = document.getElementById("kaitai-bundled-legend") as HTMLDivElement
    private val legendContainer = document.getElementById("kaitai-legend") as HTMLDivElement
    private val includeLiveStruct = TwoWayCheckboxBinding("kaitai-live")
    private var changedSinceLastDecode = true

    init {
        addButton.onclick = {
            val name = nameInput.value.trim()
            val inputValue = getInputValue()
            if (name.isNotEmpty() && inputValue.isNotEmpty()) {
                addParser(name, inputValue)
            } else {
                console.warn("Kaitai name and input cannot be empty")
            }
        }

        if (getInputValue().isNotEmpty()) {
            updateLiveDecoder(getInputValue())
        }

        kaitaiInput.onInput = {
            if (includeLiveStruct.checked) {
                updateLiveDecoder(getInputValue())
                if (liveDecodeEnabled)
                    decode(true)
            }
        }

        includeLiveStruct.onChange = { liveStructEnabled ->
            if (liveStructEnabled) {
                updateLiveDecoder(getInputValue())
            } else {
                updateLiveDecoder(null)
            }
            if (liveDecodeEnabled) {
                decode(true)
            }
            0.0
        }
    }

    fun getInputValue(): String {
        return kaitaiInput.value.trim()
    }

    fun isLiveDecodeEnabled(): Boolean {
        return includeLiveStruct.checked
    }

    fun updateLiveDecoder(kaitaiStruct: String?) {
        val success = ByteWitch.setKaitaiLiveDecoder(kaitaiStruct)
        kaitaiValid.style.display = if (success) "none" else "block"
        setChangedSinceLastDecode(true)
    }

    fun hasChangedSinceLastDecode(): Boolean {
        return changedSinceLastDecode
    }

    fun setChangedSinceLastDecode(value: Boolean) {
        changedSinceLastDecode = value
    }

    fun loadKaitaiStructsFromStorage() {
        val names = KaitaiStorage.listStructNames()
        for (kaitaiName in names) {
            // Load file from storage
            val ksyContent = KaitaiStorage.loadStruct(kaitaiName)
            if (ksyContent != null) {
                // Register the Kaitai Struct decoder
                val success = ByteWitch.registerKaitaiDecoder(kaitaiName, ksyContent)
                if (!success) {
                    console.error("Failed to register Kaitai Struct: $kaitaiName")
                    continue
                }

                addParserToUI(kaitaiName)
            } else {
                console.warn("Kaitai Struct $kaitaiName not found in storage")
            }
        }
    }

    suspend fun loadBundledStructs() {
        val manifest = loadManifest()

        for (path in manifest.files) {
            // Load file
            val response = window.fetch("kaitai/$path").await()
            if (!response.ok) {
                throw Error("Failed to load Kaitai Struct: ${response.statusText}")
            }
            val ksyContent = response.text().await()

            // Register the Kaitai Struct decoder
            val name = path.substringBeforeLast(".")
            val success = ByteWitch.registerBundledKaitaiDecoder(name, ksyContent)
            if (!success) {
                throw Error("Failed to register Kaitai Struct: $name")
            }

            addParserToUI(name, bundled = true)
        }
    }

    private suspend fun loadManifest(): KaitaiManifest {
        val response = window.fetch("kaitai-manifest.json").await()
        if (!response.ok) {
            throw Error("Failed to load Kaitai manifest: ${response.statusText}")
        }
        val manifestContent = response.text().await()
        return Json.decodeFromString(manifestContent)
    }

    private fun addParser(name: String, kaitaiStruct: String) {
        val success = ByteWitch.registerKaitaiDecoder(name, kaitaiStruct)
        if (success) {
            // Save the new Kaitai decoder to local storage
            val saved = KaitaiStorage.saveStruct(name, kaitaiStruct)
            if (!saved) {
                console.error("Failed to save Kaitai Struct: $name")
            }

            addParserToUI(name)
        }
    }

    private fun addParserToUI(name: String, bundled: Boolean = false) {
        val parserDiv = document.createElement("DIV") as HTMLDivElement
        parserDiv.classList.add("kaitai")
        parserDiv.innerHTML = name

        if (bundled) {
            bundledLegendContainer.appendChild(parserDiv)
        } else {
            legendContainer.appendChild(parserDiv)
        }
    }
}
