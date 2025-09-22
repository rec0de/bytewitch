import decoders.ByteWitchDecoder
import kotlinx.browser.document
import kotlinx.browser.window
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
        )
        val builtinDecoders = ByteWitch.builtinDecoderListManager.getAllDecoderNames()
        builtinDecoders.forEach { decoder ->
            builtinList.addItem(decoder.first, decoder.second, true)
        }
        builtinList.saveDefaultOrder()
        builtinList.addEventListener("orderChanged", { ids ->
            BuiltinDecoderStorage.reorderDecoderNames(ids)
        })

        val storedBuiltinDecoders = BuiltinDecoderStorage.listDecoderNames()
        if (storedBuiltinDecoders.isNotEmpty()) {
            builtinList.setOrder(storedBuiltinDecoders)
        }
        BuiltinDecoderStorage.reorderDecoderNames(builtinList.getItemIdsOrdered())

        // Builtin Kaitai decoder list
        val builtinKaitaiListElement = document.getElementById("builtin-kaitai-decoder-list") as HTMLDivElement
        builtinKaitaiList = ChipList(builtinKaitaiListElement, canEdit = true, canDelete = false)
        setupDecoderList(
            builtinKaitaiList,
            ByteWitch.builtinKaitaiDecoderListManager,
            "builtin-kaitai",
        )
        builtinKaitaiList.addEventListener("orderChanged", { ids ->
            BuiltinKaitaiDecoderStorage.reorderDecoderNames(ids)
        })
        builtinKaitaiList.addEventListener("itemEdited", { ids ->
            KaitaiUI.cloneBuiltinKaitai(ids.first())
        })

        // User Kaitai decoder list
        val userKaitaiListElement = document.getElementById("user-kaitai-decoder-list") as HTMLDivElement
        val userKaitaiDeleteConfirmationCallback = { id: String ->
            window.confirm("Are you sure you want to delete the decoder '$id'?")
        }
        userKaitaiList = ChipList(
            userKaitaiListElement, canEdit = true, canDelete = true,
            deleteConfirmationCallback = userKaitaiDeleteConfirmationCallback
        )
        setupDecoderList(
            userKaitaiList,
            ByteWitch.userKaitaiDecoderListManager,
            "user-kaitai",
        )

        userKaitaiList.addEventListener("itemRemoved") { ids ->
            ByteWitch.userKaitaiDecoderListManager.removeDecoder(ids.first())
            KaitaiUI.removeUserKaitai(ids.first())
            mainDecode(false, force = true)
        }
        userKaitaiList.addEventListener("itemEdited", { ids ->
            KaitaiUI.editUserKaitai(ids.first())
        })
        userKaitaiList.addEventListener("orderChanged", { ids ->
            KaitaiStorage.reorderStructNames(ids)
        })
    }

    private fun <DecoderType : ByteWitchDecoder> setupDecoderList(
        list: ChipList,
        listManager: ByteWitch.DecoderListManager<DecoderType>,
        prefix: String
    ) {
        list.addEventListener("orderChanged", { orderedIds ->
            listManager.setDecoderOrder(orderedIds)
            mainDecode(false, force = true)
        })

        list.addEventListener("itemEnabled") { ids ->
            listManager.setDecoderEnabled(ids.first(), true)
            mainDecode(false, force = true)
        }
        list.addEventListener("itemDisabled") { ids ->
            listManager.setDecoderEnabled(ids.first(), false)
            mainDecode(false, force = true)
        }

        val enableAllBtn = document.getElementById("$prefix-decoders-enable-all") as? HTMLButtonElement
        enableAllBtn?.onclick = { event ->
            if (!list.disabledItems.isEmpty()) {
                list.setItemStatusForAll(true)
                listManager.setAllDecodersEnabled(true)
                mainDecode(false, force = true)
            }
        }

        val disableAllBtn = document.getElementById("$prefix-decoders-disable-all") as? HTMLButtonElement
        disableAllBtn?.onclick = { event ->
            if (!list.enabledItems.isEmpty()) {
                list.setItemStatusForAll(false)
                listManager.setAllDecodersEnabled(false)
                mainDecode(false, force = true)
            }
        }

        val resetOrderBtn = document.getElementById("$prefix-decoders-reset-order") as? HTMLButtonElement
        resetOrderBtn?.onclick = { event ->
            if (list.getDefaultOrder() != list.getItemIdsOrdered()) {
                list.resetDefaultOrder()
            }
            // no need to update the order of decoders in the backend or force a decode,
            // as resetting the order triggers the orderChanged event
        }
    }

    fun addBuiltinKaitaiDecoder(id: String, name: String) {
        builtinKaitaiList.addItem(id, name, true)
    }

    fun addUserKaitaiDecoder(id: String, name: String) {
        userKaitaiList.addItem(id, name, true)
    }

    fun finishBuiltinKaitaiDecoderSetup() {
        console.log("finishBuiltinKaitaiDecoderSetup()")
        builtinKaitaiList.saveDefaultOrder()

        val storedBuiltinKaitaiDecoders = BuiltinKaitaiDecoderStorage.listDecoderNames()
        if (storedBuiltinKaitaiDecoders.isNotEmpty()) {
            builtinKaitaiList.setOrder(storedBuiltinKaitaiDecoders)
        }
        BuiltinKaitaiDecoderStorage.reorderDecoderNames(builtinKaitaiList.getItemIdsOrdered())
    }
}