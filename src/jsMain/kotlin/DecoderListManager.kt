import decoders.ByteWitchDecoder
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

object DecoderListManager {
    val builtinList: ChipList
    val builtinKaitaiList: ChipList
    val userKaitaiList: ChipList

    init {
        // Builtin decoder list
        val builtinListElement = document.getElementById("builtin-decoder-list") as HTMLDivElement
        builtinList = ChipList(builtinListElement, canEdit = false, canDelete = false)
        setupDecoderList(
            builtinList,
            ByteWitch.builtinDecoderListManager,
            "builtin",
            deletable = false
        )
        val builtinDecoders = ByteWitch.builtinDecoderListManager.getAllDecoderNames()
        builtinDecoders.forEach { decoder ->
            builtinList.addItem(decoder.first, decoder.second, true)
        }

        // Builtin Kaitai decoder list
        val builtinKaitaiListElement = document.getElementById("builtin-kaitai-decoder-list") as HTMLDivElement
        builtinKaitaiList = ChipList(builtinKaitaiListElement, canEdit = false, canDelete = false)
        setupDecoderList(
            builtinKaitaiList,
            ByteWitch.builtinKaitaiDecoderListManager,
            "builtin-kaitai",
            deletable = false
        )

        // User Kaitai decoder list
        val userKaitaiListElement = document.getElementById("user-kaitai-decoder-list") as HTMLDivElement
        userKaitaiList = ChipList(userKaitaiListElement, canEdit = true, canDelete = true)
        setupDecoderList(
            userKaitaiList,
            ByteWitch.userKaitaiDecoderListManager,
            "user-kaitai",
            deletable = true
        )

        userKaitaiList.addEventListener("itemEdited", { ids ->
            console.log("User kaitai has been edited ${ids.first()}")
            // TODO: implement
        })
    }

    private fun <DecoderType : ByteWitchDecoder> setupDecoderList(
        list: ChipList,
        listManager: ByteWitch.DecoderListManager<DecoderType>,
        prefix: String,
        deletable: Boolean
    ) {
        list.addEventListener("orderChanged", {orderedIds ->
            listManager.setDecoderOrder(orderedIds)
            decode(false, force = true)
        })

        list.addEventListener("itemEnabled") { ids ->
            listManager.setDecoderEnabled(ids.first(), true)
            decode(false, force = true)
        }
        list.addEventListener("itemDisabled") { ids ->
            listManager.setDecoderEnabled(ids.first(), false)
            decode(false, force = true)
        }

        if (deletable) {
            list.addEventListener("itemRemoved") { ids ->
                listManager.removeDecoder(ids.first())
                decode(false, force = true)
            }
        }

        val enableAllBtn = document.getElementById("$prefix-decoders-enable-all") as HTMLButtonElement
        enableAllBtn.onclick = { event ->
            list.setItemStatusForAll(true)
            listManager.setAllDecodersEnabled(true)
            decode(false, force = true)
        }

        val disableAllBtn = document.getElementById("$prefix-decoders-disable-all") as HTMLButtonElement
        disableAllBtn.onclick = { event ->
            list.setItemStatusForAll(false)
            listManager.setAllDecodersEnabled(false)
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