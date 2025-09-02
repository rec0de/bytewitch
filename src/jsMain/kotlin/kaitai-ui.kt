import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLTextAreaElement


@Serializable
data class KaitaiManifest(
    val files: List<String>,
)

object KaitaiUI {
    private val nameInput = TwoWayInputBinding("kaitai-name", "kaitai-name-input")
    private val saveButton = document.getElementById("save-kaitai") as HTMLButtonElement
    private val kaitaiInput = TwoWayTextAreaBinding(document.getElementById("kaitaiinput") as HTMLTextAreaElement, "kaitai-live-struct")
    private val kaitaiValid = document.getElementById("kaitai-valid") as HTMLDivElement
    private val includeLiveStruct = TwoWayCheckboxBinding("kaitai-live", "include-kaitai-live-struct")
    private var changedSinceLastDecode = true

    init {
        saveButton.onclick = {
            val name = nameInput.value.trim()
            val inputValue = getInputValue()
            if (name.isNotEmpty() && inputValue.isNotEmpty()) {
                addParser(name, inputValue)
                // clear fields to indicate successful save
                nameInput.value = ""
                kaitaiInput.value = ""
                // force decode when parser is saved
                decode(false, force = true)
            } else {
                console.warn("Kaitai name and input cannot be empty")
            }
        }

        if (includeLiveStruct.checked && getInputValue().isNotEmpty()) {
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

    fun editUserKaitai(id: String) {
        val ksyContent = KaitaiStorage.loadStruct(id)
        if (ksyContent == null) {
            console.error("User Kaitai $id not found in local storage")
            return;
        }
        kaitaiInput.value = ksyContent
        nameInput.value = id
    }

    fun removeUserKaitai(id: String) {
        KaitaiStorage.deleteStruct(id)
    }

    fun loadKaitaiStructsFromStorage() {
        val names = KaitaiStorage.listStructNames()
        for (kaitaiName in names) {
            // Load file from storage
            val ksyContent = KaitaiStorage.loadStruct(kaitaiName)
            if (ksyContent != null) {
                // Register the Kaitai Struct decoder
                val success = ByteWitch.registerUserKaitaiDecoder(kaitaiName, ksyContent)
                if (!success) {
                    console.error("Failed to register Kaitai Struct: $kaitaiName")
                    continue
                }

                DecoderListManager.addUserKaitaiDecoder(kaitaiName, kaitaiName)
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
            val success = ByteWitch.registerBuiltinKaitaiDecoder(name, ksyContent)
            if (!success) {
                throw Error("Failed to register Kaitai Struct: $name")
            }

            DecoderListManager.addBuiltinKaitaiDecoder(name, name)
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
        val decoderExists = ByteWitch.userKaitaiDecoderListManager.hasDecoder(name)
        val success = ByteWitch.registerUserKaitaiDecoder(name, kaitaiStruct)
        if (success) {
            // Save the new Kaitai decoder to local storage
            val saved = KaitaiStorage.saveStruct(name, kaitaiStruct)
            if (!saved) {
                console.error("Failed to save Kaitai Struct: $name")
            }

            if (!decoderExists) {
                DecoderListManager.addUserKaitaiDecoder(name, name)
            }
        }
    }
}
