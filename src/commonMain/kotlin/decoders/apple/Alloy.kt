package decoders.apple

import AnnotatedResult
import AnnotatedResult.Type
import ByteWitchDecoder
import ByteWitchResult
import bitmage.ByteOrder
import decoders.QuickDirtyDecoder

class Alloy : QuickDirtyDecoder() {
    companion object : ByteWitchDecoder {
        override val name = "Alloy"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return Alloy().decode(data, sourceOffset)
        }

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
    }

    private fun decode(data: ByteArray, sourceOffset: Int): AnnotatedResult {
        start(data, sourceOffset, name)

        val type = processInt(1, ByteOrder.BIG, "Type", Format.LOOKUP, lookupMap = typeMap, interpretation = Type.TYPE_FLAG)

        // ServiceMap is special
        if(type == 0x27) {
            processUInt(4, ByteOrder.BIG, "Length", interpretation = Type.LENGTH_FLAG_BE)
            while(parseOffset < data.size) {
                val tlvType = processUInt(1, ByteOrder.BIG, "Type", interpretation = Type.TYPE_FLAG).toInt()
                val tlvLen = processUInt(2, ByteOrder.BIG, "Length", interpretation = Type.LENGTH_FLAG_BE).toInt()
                when(tlvType) {
                    1 -> processUInt(tlvLen, ByteOrder.BIG, "Reason")
                    2 -> processUInt(tlvLen, ByteOrder.BIG, "Stream ID")
                    3 -> processString(tlvLen, "Service:")
                    else -> throw Exception("Alloy/ServiceMap: unsupported TLV type $tlvType")
                }
            }
            return finish()
        }

        val length = processUInt(4, ByteOrder.BIG, "Length", Format.BYTES, interpretation = Type.LENGTH_FLAG_BE)
        val sequence = processUInt(4, ByteOrder.BIG, "Sequence")

        check(length < data.size.toUInt()) { "Alloy: implausible message length" }
        check(length > 0u || sequence > 0u) { "Alloy: rejecting all-zero payload" }

        // Special cases: Ack, KeepAlive, Handshake, AppAck, Fragmented, ServiceMap

        when(type) {
            // Ack, KeepAlive, Handshake, ExpiredAck
            0x01, 0x02, 0x04, 0x25 -> {
                check(parseOffset == data.size)
            }
            // AppAck
            0x07 -> {
                processUInt(2, ByteOrder.BIG, "Stream ID")
                processLengthPrefixedString(4, ByteOrder.BIG, "Response ID:")
                if(data.size - parseOffset > 3)
                    processLengthPrefixedString(4, ByteOrder.BIG, "Topic:")
            }
            // Fragmented
            0x15 -> {
                processUInt(4, ByteOrder.BIG, "Fragment index")
                processUInt(4, ByteOrder.BIG, "Fragment count")
                processBytes(data.size-parseOffset, "Payload")
            }
            // Data and Dict messages
            0x00, 0x06, 0x16 -> {
                processUInt(2, ByteOrder.BIG, "Stream ID")
                val flags = processUInt(1, ByteOrder.BIG, "Flags", interpretation = Type.FLAGS).toInt()
                processLengthPrefixedString(4, ByteOrder.BIG, "Response ID")
                processLengthPrefixedString(4, ByteOrder.BIG, "Message ID")

                val hasExpiryDate = ((flags shr 3) and 0x01) == 1
                val hasTopic = ((flags shr 4) and 0x01) == 1

                if(hasTopic)
                    processLengthPrefixedString(4, ByteOrder.BIG, "Topic:")

                val payloadLength = if(hasExpiryDate) data.size - parseOffset - 4 else data.size - parseOffset

                // resource transfer message
                if(type == 0x16) {
                    if(data[parseOffset].toInt() == 1) {
                        processUInt(1, ByteOrder.BIG, "first data chunk")
                        processBytes(payloadLength - 1, "Data chunk", Type.BYTES)
                    }
                    else {
                        processULong(8, ByteOrder.BIG, "Offset")
                        processBytes(payloadLength - 8, "Data chunk", Type.BYTES)
                    }
                }
                else
                    processBytes(payloadLength, "Payload", Type.BYTES)

                if(hasExpiryDate)
                    processUInt(4, ByteOrder.BIG, "Expiry Date", interpretation = Type.DATE)
            }
            // Protobuf
            0x03 -> {
                processUInt(2, ByteOrder.BIG, "Stream ID")
                val flags = processUInt(1, ByteOrder.BIG, "Flags", interpretation = Type.FLAGS).toInt()
                processLengthPrefixedString(4, ByteOrder.BIG, "Response ID")
                processLengthPrefixedString(4, ByteOrder.BIG, "Message ID")

                val hasExpiryDate = ((flags shr 3) and 0x01) == 1
                val hasTopic = ((flags shr 4) and 0x01) == 1

                if(hasTopic)
                    processLengthPrefixedString(4, ByteOrder.BIG, "Topic:")

                processUInt(2, ByteOrder.BIG, "Type", interpretation = Type.TYPE_FLAG)
                processUInt(2, ByteOrder.BIG, "is Response")
                val payloadLength = processUInt(4, ByteOrder.BIG, "Payload length").toInt()

                processBytes(payloadLength, "Payload", Type.BYTES)

                if(hasExpiryDate)
                    processUInt(4, ByteOrder.BIG, "Expiry Date", interpretation = Type.DATE)
            }
            else -> throw Exception("Alloy: unsupported message type $type")
        }

        return finish()
    }


}