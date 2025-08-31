import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

object DecoderListManager {
    val builtinKaitaiList : ChipList
    val userKaitaiList : ChipList

    init {
        setupBuiltinDecoder()

        // Builtin Kaitai decoder list
        val builtinKaitaiListElement = document.getElementById("builtin-kaitai-decoder-list") as HTMLDivElement
        builtinKaitaiList = ChipList(builtinKaitaiListElement, canEdit = false, canDelete = false)
        setupBuiltinKaitaiDecoder()

        // User Kaitai decoder list
        val userKaitaiListElement = document.getElementById("user-kaitai-decoder-list") as HTMLDivElement
        userKaitaiList = ChipList(userKaitaiListElement, canEdit = true, canDelete = true)
        setupUserKaitaiDecoder()
    }

    fun setupBuiltinDecoder() {
        val listElement = document.getElementById("builtin-decoder-list") as HTMLDivElement
        val chipList = ChipList(listElement, canEdit = false, canDelete = false)

        chipList.setItemToggleCallback { item, enabled ->
            ByteWitch.builtinDecoderListManager.setDecoderEnabled(item, enabled)
            decode(false, force = true)
        }

        val decoders = ByteWitch.builtinDecoderListManager.getAllDecoderNames()
        decoders.forEach { decoder ->
            chipList.addItem(decoder.first, decoder.second, true)
        }

        val enableAllBtn = document.getElementById("builtin-decoders-enable-all") as HTMLButtonElement
        enableAllBtn.onclick = { event ->
            chipList.setItemStatusForAll(true)
            ByteWitch.builtinDecoderListManager.setAllDecodersEnabled(true)
            decode(false, force = true)
        }

        val disableAllBtn = document.getElementById("builtin-decoders-disable-all") as HTMLButtonElement
        disableAllBtn.onclick = { event ->
            chipList.setItemStatusForAll(false)
            ByteWitch.builtinDecoderListManager.setAllDecodersEnabled(false)
            decode(false, force = true)
        }

        val resetOrderBtn = document.getElementById("builtin-decoders-reset-order") as HTMLButtonElement
        resetOrderBtn.onclick = { event ->
            val currentItemsEnabled = chipList.allItems.associate { item -> item.id to item.isEnabled }.toMutableMap()
            chipList.clear()

            decoders.forEach { decoder ->
                chipList.addItem(decoder.first, decoder.second, currentItemsEnabled[decoder.first]!!)
            }
        }
    }

    fun setupBuiltinKaitaiDecoder() {
        val decoders = ByteWitch.builtinKaitaiDecoderListManager.getAllDecoderNames()
        decoders.forEach { decoder ->
            builtinKaitaiList.addItem(decoder.first, decoder.second, true)
        }

        builtinKaitaiList.setItemToggleCallback { item, enabled ->
            ByteWitch.builtinKaitaiDecoderListManager.setDecoderEnabled(item, enabled)
            decode(false, force = true)
        }

        val enableAllBtn = document.getElementById("builtin-kaitai-decoders-enable-all") as HTMLButtonElement
        enableAllBtn.onclick = { event ->
            builtinKaitaiList.setItemStatusForAll(true)
            ByteWitch.builtinKaitaiDecoderListManager.setAllDecodersEnabled(true)
            decode(false, force = true)
        }

        val disableAllBtn = document.getElementById("builtin-kaitai-decoders-disable-all") as HTMLButtonElement
        disableAllBtn.onclick = { event ->
            builtinKaitaiList.setItemStatusForAll(false)
            ByteWitch.builtinKaitaiDecoderListManager.setAllDecodersEnabled(false)
            decode(false, force = true)
        }
    }

    fun setupUserKaitaiDecoder() {
        userKaitaiList.setItemToggleCallback { item, enabled ->
            ByteWitch.userKaitaiDecoderListManager.setDecoderEnabled(item, enabled)
            decode(false, force = true)
        }

        userKaitaiList.setItemDeleteCallback { item ->
            ByteWitch.userKaitaiDecoderListManager.removeDecoder(item)
            decode(false, force = true)
        }

        val enableAllBtn = document.getElementById("user-kaitai-decoders-enable-all") as HTMLButtonElement
        enableAllBtn.onclick = { event ->
            userKaitaiList.setItemStatusForAll(true)
            ByteWitch.userKaitaiDecoderListManager.setAllDecodersEnabled(true)
            decode(false, force = true)
        }

        val disableAllBtn = document.getElementById("user-kaitai-decoders-disable-all") as HTMLButtonElement
        disableAllBtn.onclick = { event ->
            userKaitaiList.setItemStatusForAll(false)
            ByteWitch.userKaitaiDecoderListManager.setAllDecodersEnabled(false)
            decode(false, force = true)
        }
    }

    fun addBuiltinKaitaiDecoder(id: String, name: String) {
        builtinKaitaiList.addItem(id, name, true)
    }

    fun addUserKaitaiDecoder(id: String, name: String) {
        userKaitaiList.addItem(id, name, true)
    }
}