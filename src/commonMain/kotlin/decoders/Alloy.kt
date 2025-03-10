package decoders

import ByteWitch
import Date
import ParseCompanion
import bitmage.hex

object AlloyMessage: ParseCompanion(), ByteWitchDecoder {

    override val name = "Alloy Data"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val type = data[0].toInt()
        parseOffset = 1

        val length = readInt(data, 4).toUInt()
        val sequence = readInt(data, 4)

        if(length != (data.size - 5).toUInt())
            throw Exception("Unexpected UTunMsg size ${data.size-5}, expected $length")

        val streamID = readInt(data, 2)
        val flags = readInt(data, 1)
        val responseIdentifier = readLengthPrefixedString(data, 4)
        val messageUUID = readLengthPrefixedString(data, 4)!!

        // compute some flags
        val hasExpiryDate = ((flags shr 3) and 0x01) == 1
        val hasTopic = ((flags shr 4) and 0x01) == 1

        val topic = if(hasTopic) readLengthPrefixedString(data, 4) else null

        val payloadLength = if(hasExpiryDate) data.size - parseOffset - 4 else data.size - parseOffset
        val payload = readBytes(data, payloadLength)

        // expiry is seconds since 00:00:00 UTC on 1 January 2001
        val expiryDate = if(hasExpiryDate) readInt(data, 4).toLong() else null

        return AlloyCommonMessage(type, sequence, streamID, flags, responseIdentifier, messageUUID, topic, expiryDate, payload, Pair(sourceOffset, sourceOffset+data.size))
    }
}

class AlloyCommonMessage(val type: Int, val sequence: Int, val streamID: Int, var flags: Int, val responseIdentifier: String?, val messageUUID: String, var topic: String?, val expiryDate: Long?, val data: ByteArray,
                         override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {

    // message types from jump table in IDSSocketPairMessage::messageWithCommand:data:
    val typeMap = mapOf(
        0x00 to "DataMessage",                // superclass: SocketPairMessage
        0x01 to "AckMessage",                 // superclass: SocketPairMessage
        0x02 to "KeepAliveMessage",           // superclass: SocketPairMessage
        0x03 to "ProtobufMessage",            // superclass: SocketPairMessage
        0x04 to "Handshake",                  // superclass: SocketPairMessage
        0x05 to "EncryptedMessage",           // superclass: SocketPairMessage
        0x06 to "DictionaryMessage",          // superclass: DataMessage
        0x07 to "AppAckMessage",              // superclass: SocketPairMessage
        0x08 to "SessionInvitationMessage",   // superclass: DataMessage
        0x09 to "SessionAcceptMessage",       // superclass: DataMessage
        0x0a to "SessionDeclineMessage",      // superclass: DataMessage
        0x0b to "SessionCancelMessage",       // superclass: DataMessage
        0x0c to "SessionMessage",             // superclass: DataMessage
        0x0d to "SessionEndMessage",          // superclass: DataMessage
        0x0e to "SMSTextMessage",             // superclass: DataMessage
        0x0f to "SMSTextDownloadMessage",     // superclass: DataMessage
        0x10 to "SMSOutgoing",                // superclass: DataMessage
        0x11 to "SMSDownloadOutgoing",        // superclass: DataMessage
        0x12 to "SMSDeliveryReceipt",         // superclass: DataMessage
        0x13 to "SMSReadReceipt",             // superclass: DataMessage
        0x14 to "SMSFailure",                 // superclass: DataMessage
        0x15 to "FragmentedMessage",          // superclass: SocketPairMessage
        0x16 to "ResourceTransferMessage",    // superclass: DataMessage
        0x17 to "OTREncryptedMessage",        // superclass: SocketPairMessage
        0x18 to "OTRMessage",                 // superclass: SocketPairMessage
        0x19 to "ProxyOutgoingNiceMessage",   // superclass: DataMessage
        0x1a to "ProxyIncomingNiceMessage",   // superclass: DataMessage
        0x1b to "TextMessage",                // superclass: DataMessage
        0x1c to "DeliveryReceipt",            // superclass: DataMessage
        0x1d to "ReadReceipt",                // superclass: DataMessage
        0x1e to "AttachmentMessage",          // superclass: DataMessage
        0x1f to "PlayedReceipt",              // superclass: DataMessage
        0x20 to "SavedReceipt",               // superclass: DataMessage
        0x21 to "ReflectedDeliveryReceipt",   // superclass: DataMessage
        0x22 to "GenericCommandMessage",      // superclass: DataMessage
        0x23 to "GenericGroupMessageCommand", // superclass: DataMessage
        0x24 to "LocationShareOfferCommand",  // superclass: DataMessage
        0x25 to "ExpiredAckMessage",          // superclass: AckMessage
        0x26 to "ErrorMessage",               // superclass: DataMessage
        0x27 to "ServiceMapMessage",          // superclass: SocketPairMessage
        // 0x28 missing?
        0x29 to "SessionReinitiateMessage",   // superclass: DataMessage
        0x2a to "SyndicationAction",          // new in iOS 17.5
        0x2b to "RetractMessage",             // new in iOS 17.5
        0x2c to "EditMessage",                // new in iOS 17.5
        0x2d to "RecoverSyncMessage",         // new in iOS 17.5
        0x2e to "MarkAsUnreadMessage",        // new in iOS 17.5
        0x2f to "DeliveredQuietlyMessage",    // new in iOS 17.5
        0x30 to "NotifyRecipientMessage",     // new in iOS 17.5
        0x31 to "RecoverJunkMessage",         // new in iOS 17.5
        0x32 to "SMSFilteringSettingsMessage" // new in iOS 17.5

    )

    val expectsPeerResponse: Boolean
        get() = (flags and 0x01) == 1
    val compressed: Boolean
        get() = ((flags shr 1) and 0x01) == 1
    val wantsAppAck: Boolean
        get() = ((flags shr 2) and 0x01) == 1
    val hasExpiryDate: Boolean
        get() = ((flags shr 3) and 0x01) == 1
    val hasTopic: Boolean
        get() = ((flags shr 4) and 0x01) == 1
    val normalizedExpiryDate: Date?
        // convert apple's reference date (Jan 1st 2001, 00:00 UTC) to standard millisecond unix timestamp
        get() = if(hasExpiryDate) Date((expiryDate!! + 978307200) * 1000 ) else null

    override fun renderHTML(): String {
        val dataOffset = if(hasExpiryDate) sourceByteRange.second - data.size - 4 else sourceByteRange.second - data.size
        val quickDecode = ByteWitch.quickDecode(data, dataOffset)?.renderHTML() ?: "<div class=\"bpvalue data\" ${rangeTagsFor(dataOffset, dataOffset+data.size)}>0x${data.hex()}</div>"

        var props = "<div class=\"bpvalue\">$messageUUID</div>"
        if(hasTopic)
            props += "<div class=\"bpvalue\">topic $topic</div>"
        if(responseIdentifier != null)
            props += "<div class=\"bpvalue\">response to $responseIdentifier</div>"
        if(hasExpiryDate)
            props += "<div class=\"bpvalue\">expires $normalizedExpiryDate</div>"

        return "<div class=\"generic roundbox\" $byteRangeDataTags><div class=\"bpvalue\">${typeMap[type]} seq $sequence stream $streamID</div> $props $quickDecode</div>"
    }
}