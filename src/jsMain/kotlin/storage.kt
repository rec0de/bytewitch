object KaitaiStorage {
    private const val KEY_NAME_LIST = "kaitaiStructNames"
    private const val KEY_STRUCT_PREFIX = "kaitaiStruct-"

    fun listStructNames(): List<String> {
        val names = localStorage.getItem(KEY_NAME_LIST)
        if (names != null) {
            return names.split(";").filter { it.isNotBlank() }
        }
        return emptyList()
    }

    fun saveStruct(name: String, struct: String): Boolean {
        if (name.isBlank() || struct.isBlank()) {
            return false
        }
        val existingNames = listStructNames().toMutableSet()
        existingNames.add(name)
        localStorage.setItem(KEY_STRUCT_PREFIX + name, struct)
        localStorage.setItem(KEY_NAME_LIST, existingNames.joinToString(";"))
        return true
    }

    fun loadStruct(name: String): String? {
        if (name.isBlank()) {
            return null
        }
        return localStorage.getItem(KEY_STRUCT_PREFIX + name)
    }

    fun deleteStruct(name: String): Boolean {
        if (name.isBlank()) {
            return false
        }
        localStorage.removeItem(KEY_STRUCT_PREFIX + name)
        val existingNames = listStructNames().toMutableSet()
        existingNames.remove(name)
        localStorage.setItem(KEY_NAME_LIST, existingNames.joinToString(";"))
        return true
    }

    fun reorderStructNames(newOrder: List<String>): Boolean {
        localStorage.setItem(KEY_NAME_LIST, newOrder.joinToString(";"))
        return true
    }
}

// Code snippet taken from https://slack-chats.kotlinlang.org/t/23109011/heyya-does-anyone-know-if-how-i-can-store-data-on-web-browse (as of 2025-06-26)
external interface BrowserStorage {
    fun setItem(key: String, value: String)
    fun getItem(key: String): String?
    fun removeItem(key: String)
    fun clear()
}

external val localStorage: BrowserStorage
external val sessionStorage: BrowserStorage
// End of snippet
