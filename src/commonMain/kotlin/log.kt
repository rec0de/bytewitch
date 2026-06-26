expect object Logger {
    fun tag(tag: String, msg: String)
    fun log(vararg o: Any?)
    fun showUserVisibleMessage(msg: String)
    fun clearUserVisibleMessage()
}