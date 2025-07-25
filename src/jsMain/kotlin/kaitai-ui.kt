import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

object KaitaiUI {
    private val nameInput = document.getElementById("kaitai-name") as HTMLInputElement
    private val addButton = document.getElementById("add-kaitai") as HTMLButtonElement
    private val kaitaiInput = document.getElementById("kaitaiinput") as HTMLTextAreaElement
    private val bundledLegendContainer = document.getElementById("kaitai-bundled-legend") as HTMLDivElement
    private val legendContainer = document.getElementById("kaitai-legend") as HTMLDivElement
    private val liveDecode = document.getElementById("kaitai-live") as HTMLInputElement

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

        kaitaiInput.oninput = {
            if (liveKaitaiEnabled) {
                ByteWitch.setKaitaiLiveDecoder(getInputValue())
                if (liveDecodeEnabled)
                    decode(false)
            }
        }

        liveKaitaiEnabled = liveDecode.checked

        liveDecode.onchange = {
            liveKaitaiEnabled = liveDecode.checked
            if (liveKaitaiEnabled) {
                ByteWitch.setKaitaiLiveDecoder(getInputValue())
                decode(false)
            } else {
                ByteWitch.setKaitaiLiveDecoder(null)
            }
            0.0
        }
    }

    fun getInputValue(): String {
        return kaitaiInput.value.trim()
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

    fun loadBundledKaitaiStructs() {
        for ( kaitaiName in bundledKaitaiStructs ) {
            // Load file
            window.fetch("kaitai/$kaitaiName.ksy")
                .then { response ->
                    if (!response.ok) {
                        throw Error("Failed to load Kaitai Struct: ${response.statusText}")
                    }
                    return@then response.text()
                }
                .then { ksyContent ->
                    // Register the Kaitai Struct decoder
                    val success = ByteWitch.registerBundledKaitaiDecoder(kaitaiName, ksyContent)
                    if (!success) {
                        throw Error("Failed to register Kaitai Struct: $kaitaiName")
                    }
                }
                .then {
                    addParserToUI(kaitaiName, bundled = true)
                }
                .catch { error ->
                    console.error("Error loading Kaitai Struct $kaitaiName: ${error.message}")
                }
        }
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
