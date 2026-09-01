package decoders.web

import ParseCompanion
import bitmage.ByteOrder
import bitmage.fromHex
import bitmage.hex
import bitmage.indicesOfAllSubsequences
import bitmage.readShortAtOffset
import decoders.BWAnnotatedData
import decoders.BWGenericData
import decoders.BWGenericSequence
import decoders.BWString
import decoders.ByteWitchDecoder
import decoders.ByteWitchResult

object TLS12: ByteWitchDecoder, ParseCompanion() {
    override val name = "TLS (stub)"

    val recordTypes = mapOf(
        0x14 to "change cipher spec",
        0x16 to "handshake",
        0x17 to "application data"
    )

    val handshakeRecordTypes = mapOf(
        0x01 to "client hello",
        0x02 to "server hello",
        0x0b to "certificate",
        0x0c to "server key exchange",
        0x0e to "server hello done",
        0x10 to "client key exchange"
    )

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0

        val recordType = data[0].toInt()
        val version1 = data[1].toInt()
        val version2 = data[2].toInt()
        parseOffset += 3

        check(version1 == 0x03){ "TLS1.2 should set major version 3, got $version1" }
        check(version2 == 0x01 || version2 == 0x03){ "TLS1.2 should set minor version to 1 or 3, got $version2" }
        check(recordType in recordTypes.keys){ "TLS1.2 unknown record type $recordType"}

        val length = readInt(data, 2)
        check(length == data.size - parseOffset){ "TLS1.2 unexpected length: $length (expected ${data.size-parseOffset})"}



        val recordHeader = BWString("TLS ${recordTypes[recordType]}", Pair(sourceOffset, sourceOffset+3))
        val lengthField = BWString("Length ${length}B", Pair(sourceOffset+3, sourceOffset+5))

        val subresults = mutableListOf<ByteWitchResult>(recordHeader, lengthField)

        // detect unencrypted handshake records
        if(recordType == 0x16 && data[parseOffset].toInt() in handshakeRecordTypes.keys && data.readShortAtOffset(parseOffset+2, ByteOrder.BIG) == length - 4) {
            val handshakeRecordType = readInt(data, 1)
            subresults.add(BWString("Handshake Header: ${handshakeRecordTypes[handshakeRecordType]}", Pair(sourceOffset+parseOffset-1, sourceOffset+parseOffset)))
            val handshakeRecordLength = readInt(data, 3)
            subresults.add(BWString("Length ${handshakeRecordLength}B", Pair(sourceOffset+parseOffset-3, sourceOffset+parseOffset)))


            when(handshakeRecordType) {
                0x01 -> { // Client Hello
                    val version = readBytes(data, 2)
                    subresults.add(BWString("TLS Version 0x${version.hex()}", Pair(sourceOffset+parseOffset-2, sourceOffset+parseOffset)))
                    val random = readBytes(data, 32)
                    subresults.add(BWAnnotatedData("Client Random", random, Pair(sourceOffset+parseOffset-32, sourceOffset+parseOffset)))

                    val sessionIdLen = readInt(data, 1)
                    if(sessionIdLen > 0) {
                        subresults.add(BWString("Session Length ${sessionIdLen}B", Pair(sourceOffset+parseOffset-1, sourceOffset+parseOffset)))
                        val sessionID = readBytes(data, sessionIdLen)
                        subresults.add(BWAnnotatedData("Session ID", sessionID, Pair(sourceOffset+parseOffset-sessionIdLen, sourceOffset+parseOffset)))
                    }
                    else
                        subresults.add(BWString("Empty Session ID", Pair(sourceOffset+parseOffset-1, sourceOffset+parseOffset)))

                    val cipherSuiteLen = readInt(data, 2)
                    val cipherSuiteData = readBytes(data, cipherSuiteLen)
                    subresults.add(BWAnnotatedData("Cipher Suites (${cipherSuiteLen}B)", cipherSuiteData, Pair(sourceOffset+parseOffset-cipherSuiteLen-2, sourceOffset+parseOffset)))

                    val compressionLen = readInt(data, 1)
                    val compressionData = readBytes(data, compressionLen)
                    subresults.add(BWAnnotatedData("Compression Methods (${compressionLen}B)", compressionData, Pair(sourceOffset+parseOffset-compressionLen-1, sourceOffset+parseOffset)))


                    val extensionLength = readInt(data, 2)
                    subresults.add(BWString("Extensions Length ${extensionLength}B", Pair(sourceOffset+parseOffset-2, sourceOffset+parseOffset)))

                    val extensionData = readBytes(data, extensionLength)
                    subresults.add(BWGenericData(extensionData, Pair(sourceOffset+parseOffset-extensionLength, sourceOffset+parseOffset)))
                }
                0x02 -> { // Server Hello
                    val version = readBytes(data, 2)
                    subresults.add(BWString("TLS Version 0x${version.hex()}", Pair(sourceOffset+parseOffset-2, sourceOffset+parseOffset)))
                    val random = readBytes(data, 32)
                    subresults.add(BWAnnotatedData("Server Random", random, Pair(sourceOffset+parseOffset-32, sourceOffset+parseOffset)))

                    val sessionIdLen = readInt(data, 1)
                    if(sessionIdLen > 0) {
                        subresults.add(BWString("Session Length ${sessionIdLen}B", Pair(sourceOffset+parseOffset-1, sourceOffset+parseOffset)))
                        val sessionID = readBytes(data, sessionIdLen)
                        subresults.add(BWAnnotatedData("Session ID", sessionID, Pair(sourceOffset+parseOffset-sessionIdLen, sourceOffset+parseOffset)))
                    }
                    else
                        subresults.add(BWString("Empty Session ID", Pair(sourceOffset+parseOffset-1, sourceOffset+parseOffset)))

                    val cipherSuite = readBytes(data, 2)
                    subresults.add(BWString("Cipher Suite: 0x${cipherSuite.hex()}", Pair(sourceOffset+parseOffset-2, sourceOffset+parseOffset)))

                    val compression = readBytes(data, 1)
                    subresults.add(BWString("Compression Method: 0x${compression.hex()}", Pair(sourceOffset+parseOffset-1, sourceOffset+parseOffset)))

                    val extensionLength = readInt(data, 2)
                    subresults.add(BWString("Extensions Length ${extensionLength}B", Pair(sourceOffset+parseOffset-2, sourceOffset+parseOffset)))

                    val extensionData = readBytes(data, extensionLength)
                    subresults.add(BWGenericData(extensionData, Pair(sourceOffset+parseOffset-extensionLength, sourceOffset+parseOffset)))
                }
                else -> {
                    val payloadBytes = readBytes(data, handshakeRecordLength)
                    val payload = BWGenericData(payloadBytes, Pair(sourceOffset+parseOffset-payloadBytes.size, sourceOffset+parseOffset))
                    subresults.add(payload)
                }
            }
        }
        else {
            val payloadBytes = readBytes(data, length)
            val payload = BWGenericData(payloadBytes, Pair(sourceOffset+5, sourceOffset+parseOffset))
            subresults.add(payload)
        }



        return BWGenericSequence(subresults, Pair(sourceOffset, sourceOffset+parseOffset))
    }

    override fun findDecodableSegments(data: ByteArray): List<Pair<Int, Int>> {
        val handshakeRecords10 = data.indicesOfAllSubsequences("160301".fromHex())
        val handshakeRecords13 = data.indicesOfAllSubsequences("160303".fromHex())
        val applicationDataRecords = data.indicesOfAllSubsequences("140303".fromHex())

        val candidates = handshakeRecords10 + handshakeRecords13 + applicationDataRecords
        val lengthMapped = candidates.map{ startOffset -> Pair(startOffset, data.readShortAtOffset(startOffset+3, ByteOrder.BIG)) }
        val feasible = lengthMapped.filter {
            val start = it.first
            val length = it.second
            length > 32 && length < data.size - start - 5 // three byte record header plus two byte length plus payload has to fit data present
        }

        return feasible.map { Pair(it.first, it.first + it.second + 5) } // convert from start, length to start, end
    }
}