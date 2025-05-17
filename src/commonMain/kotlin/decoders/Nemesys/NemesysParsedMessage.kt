package decoders.Nemesys

data class NemesysParsedMessage(
    val segments: List<NemesysSegment>,
    val bytes: ByteArray,
    val msgIndex: Int
)

data class NemesysSegment(
    val offset: Int,
    val fieldType: NemesysField
)

enum class NemesysField {
    UNKNOWN,
    STRING,
    PAYLOAD_LENGTH
}