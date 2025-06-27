object KaitaiStorage {
    val keyNameList = "kaitaiStructNames"
    val keyStructPrefix = "kaitaiStruct-"

    fun listStructNames(): List<String> {
        val names = localStorage.getItem(keyNameList)
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
        localStorage.setItem(keyStructPrefix + name, struct)
        localStorage.setItem(keyNameList, existingNames.joinToString(";"))
        return true
    }

    fun loadStruct(name: String): String? {
        if (name.isBlank()) {
            return null
        }
        return localStorage.getItem(keyStructPrefix + name)
    }

    fun deleteStruct(name: String): Boolean {
        if (name.isBlank()) {
            return false
        }
        localStorage.removeItem(keyStructPrefix + name)
        val existingNames = listStructNames().toMutableSet()
        existingNames.remove(name)
        localStorage.setItem(keyNameList, existingNames.joinToString(";"))
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
