package decoders.web

import ParseCompanion
import bitmage.ByteOrder
import bitmage.fromHex
import bitmage.indicesOfAllSubsequences
import bitmage.readShortAtOffset
import decoders.BWAnnotatedData
import decoders.BWGenericData
import decoders.BWGenericSequence
import decoders.BWString
import decoders.BWStringCollection
import decoders.ByteWitchDecoder
import decoders.ByteWitchResult
import decoders.bwvalue
import preprocessing.BytewiseCalc

object TLS12: ByteWitchDecoder, ParseCompanion() {
    override val name = "TLS (stub)"

    val recordTypes = mapOf(
        0x14 to "change cipher spec",
        0x16 to "handshake",
        0x17 to "application data"
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

        val data = readBytes(data, length)

        val recordHeader = BWString("TLS ${recordTypes[recordType]}", Pair(sourceOffset, sourceOffset+3))
        val lengthField = BWString("Length ${length}B", Pair(sourceOffset+3, sourceOffset+5))

        val payload = BWGenericData(data, Pair(sourceOffset+5, sourceOffset+parseOffset))

        return BWGenericSequence(listOf(recordHeader, lengthField, payload), Pair(sourceOffset, sourceOffset+parseOffset))
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