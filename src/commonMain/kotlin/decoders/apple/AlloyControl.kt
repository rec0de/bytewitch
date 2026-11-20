package decoders.apple

import AnnotatedResult
import AnnotatedResult.Type
import ByteWitchDecoder
import ByteWitchResult
import bitmage.ByteOrder
import decoders.QuickDirtyDecoder

class AlloyControl : QuickDirtyDecoder() {
    companion object : ByteWitchDecoder {
        override val name = "AlloyControl"

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
            return AlloyControl().decode(data, sourceOffset)
        }

        private val typeStrings = mapOf(
            1 to "Hello",
            2 to "SetupChannel",
            3 to "CloseChannel",
            4 to "CompressionRequest",
            5 to "CompressionResponse",
            6 to "SetupEncryptedChannel",
            7 to "FairplayHostSessionInfo",
            8 to "FairplayDeviceInfo",
            9 to "FairplayDeviceSessionInfo",
            10 to "OTRNegotiationMessage",
            11 to "EncryptControlChannel",
            12 to "SuspendOTRNegotiationMessage",
            13 to "SetupChannelForDirectMsg",
            14 to "DirectMsgInfo"
        )
    }

    private fun decode(data: ByteArray, sourceOffset: Int): AnnotatedResult {
        start(data, sourceOffset, name)

        val type = processInt(1, ByteOrder.BIG, "Type", Format.LOOKUP, lookupMap = typeStrings, interpretation = Type.TYPE_FLAG)

        when(type) {
            0x01 -> parseHello()
            0x02 -> parseSetupChannel()
            0x03 -> parseCloseChannel()
            0x06 -> parseSetupEncryptedChannel()
            else -> throw Exception("AlloyControl: unsupported message type $type")
        }

        return finish()
    }

    private fun parseHello() {
        processLengthPrefixedString(2, ByteOrder.BIG, "Control channel version:")
        processLengthPrefixedString(2, ByteOrder.BIG, "Product name:")
        processLengthPrefixedString(2, ByteOrder.BIG, "Version:")
        processLengthPrefixedString(2, ByteOrder.BIG, "Build:")
        processLengthPrefixedString(2, ByteOrder.BIG, "Model:")

        processUInt(4, ByteOrder.BIG, "Protocol version")

        // read TLVs
        while(parseOffset < data.size) {
            enterSubstruct()
            val type = processUInt(1, ByteOrder.BIG, "type", Format.HEX, interpretation = Type.TYPE_FLAG).toInt()
            val length = processUInt(2, ByteOrder.BIG, "length", Format.BYTES, interpretation = Type.LENGTH_FLAG_BE).toInt()

            when(type) {
                0 -> processUInt(length, ByteOrder.BIG, "minimum compatible version")
                1 -> processUInt(length, ByteOrder.BIG, "maximum compatible version")
                2 -> processBytes(length, "instance uuid", Type.BYTES)
                3 -> processULong(length, ByteOrder.BIG, "capability flags", Format.HEX, interpretation = Type.FLAGS)
                4 -> processUInt(length, ByteOrder.BIG, "service compatibility version")
                5 -> processBytes(length, "device uuid", Type.BYTES)
                else -> throw Exception("AlloyControl/Hello: unknown tlv type $type")
            }

            leaveSubstruct()
        }
    }

    private fun parseSetupChannel() {
        processUInt(1, ByteOrder.BIG, "Protocol")

        processUInt(2, ByteOrder.BIG, "Remote port")
        processUInt(2, ByteOrder.BIG, "Local port")

        val rUUIDlen = processUInt(2, ByteOrder.BIG, "Remote UUID length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val lUUIDlen = processUInt(2, ByteOrder.BIG, "Local UUID length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val accLen = processUInt(2, ByteOrder.BIG, "Account length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val serviceLen = processUInt(2, ByteOrder.BIG, "Service length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val nameLen = processUInt(2, ByteOrder.BIG, "Name length", interpretation = Type.LENGTH_FLAG_BE).toInt()

        if(rUUIDlen > 0)
            processString(rUUIDlen, "Remote UUID:")
        if(lUUIDlen > 0)
            processString(lUUIDlen, "Local UUID:")

        processString(accLen, "Account:")
        processString(serviceLen, "Service:")
        processString(nameLen, "Name:")
    }

    private fun parseCloseChannel() {
        val rUUIDlen = processUInt(2, ByteOrder.BIG, "Remote UUID length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val lUUIDlen = processUInt(2, ByteOrder.BIG, "Local UUID length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val accLen = processUInt(2, ByteOrder.BIG, "Account length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val serviceLen = processUInt(2, ByteOrder.BIG, "Service length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val nameLen = processUInt(2, ByteOrder.BIG, "Name length", interpretation = Type.LENGTH_FLAG_BE).toInt()

        if(rUUIDlen > 0)
            processString(rUUIDlen, "Remote UUID:")
        if(lUUIDlen > 0)
            processString(lUUIDlen, "Local UUID:")

        processString(accLen, "Account:")
        processString(serviceLen, "Service:")
        processString(nameLen, "Name:")
    }

    fun parseSetupEncryptedChannel() {
        processUInt(1, ByteOrder.BIG, "Protocol")

        processUInt(2, ByteOrder.BIG, "Remote port")
        processUInt(2, ByteOrder.BIG, "Local port")

        val rUUIDlen = processUInt(2, ByteOrder.BIG, "Remote UUID length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val lUUIDlen = processUInt(2, ByteOrder.BIG, "Local UUID length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val accLen = processUInt(2, ByteOrder.BIG, "Account length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val serviceLen = processUInt(2, ByteOrder.BIG, "Service length", interpretation = Type.LENGTH_FLAG_BE).toInt()
        val nameLen = processUInt(2, ByteOrder.BIG, "Name length", interpretation = Type.LENGTH_FLAG_BE).toInt()

        processInt(4, ByteOrder.BIG, "SSRC", format = Format.HEX)
        processUInt(2, ByteOrder.BIG, "Initial sequence number")

        val keyLen = processUInt(2, ByteOrder.BIG, "Key length", interpretation = Type.LENGTH_FLAG_BE).toInt()

        if(rUUIDlen > 0)
            processString(rUUIDlen, "Remote UUID:")
        if(lUUIDlen > 0)
            processString(lUUIDlen, "Local UUID:")

        processString(accLen, "Account:")
        processString(serviceLen, "Service:")
        processString(nameLen, "Name:")

        processBytes(keyLen, "Key:", interpretation = Type.BYTES_HIGH_ENTROPY)
    }
}