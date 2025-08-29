import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

object TagManager {
    init {
        // Default decoders
        val listElement = document.getElementById("default-decoder-list") as HTMLDivElement
        val chipList = ChipList(listElement, canDelete = false)

        /*
        for (i in 1..4) {
            chipList.addItem("id$i", "VeryLongNameThatWrapsIfItIsLong $i")
        }
         */

        chipList.setItemToggleCallback { item, checked ->
            ByteWitch.DecoderManager.setDefaultDecoderEnabled(item, checked)

            decode(true, force = true)
        }

        val decoders = ByteWitch.DecoderManager.getDefaultDecoderNames()
        val disabledDecoders = ByteWitch.DecoderManager.getDisabledDefaultDecoders()

        decoders.forEach { decoder ->
            chipList.addItem(decoder.first, decoder.second, !disabledDecoders.contains(decoder.first))
        }

        // Bundled Kaitai decoders
    }
}