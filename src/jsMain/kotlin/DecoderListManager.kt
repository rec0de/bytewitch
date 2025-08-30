import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

object DecoderListManager {
    init {
        // Default decoders
        setupBuiltinDecoder()
    }

    fun setupBuiltinDecoder() {
        val listElement = document.getElementById("default-decoder-list") as HTMLDivElement
        val chipList = ChipList(listElement, canEdit = false, canDelete = false)

        chipList.setItemToggleCallback { item, enabled ->
            ByteWitch.DecoderManager.setDefaultDecoderEnabled(item, enabled)
            decode(false, force = true)
        }

        val decoders = ByteWitch.DecoderManager.getDefaultDecoderNames()
        decoders.forEach { decoder ->
            chipList.addItem(decoder.first, decoder.second, true)
        }

        val enableAllBtn = document.getElementById("builtin-decoders-enable-all") as HTMLButtonElement
        enableAllBtn.onclick = { event ->
            chipList.allItems.forEach { item -> item.setStatus(true) }
            ByteWitch.DecoderManager.setAllDefaultDecoderEnabled(true)
            decode(false, force = true)
        }

        val disableAllBtn = document.getElementById("builtin-decoders-disable-all") as HTMLButtonElement
        disableAllBtn.onclick = { event ->
            chipList.allItems.forEach { item -> item.setStatus(false) }
            ByteWitch.DecoderManager.setAllDefaultDecoderEnabled(false)
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
}