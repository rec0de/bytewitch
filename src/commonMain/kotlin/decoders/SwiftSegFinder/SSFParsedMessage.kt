package decoders.SwiftSegFinder

data class SSFParsedMessage(
    val segments: List<SSFSegment>,
    val bytes: ByteArray,
    val msgIndex: Int
)

data class SSFSegment(
    val offset: Int,
    val fieldType: SSFField
)

enum class SSFField {
    UNKNOWN,
    STRING,
    PAYLOAD_LENGTH_LITTLE_ENDIAN,
    PAYLOAD_LENGTH_BIG_ENDIAN,
    STRING_PAYLOAD // currently the payload after a payload_length field can only be a string
}