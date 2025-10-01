package decoders

import Date
import ParseCompanion
import bitmage.ByteOrder
import bitmage.hex
import htmlEscape
import kotlin.math.ceil
import kotlin.math.roundToInt

object ModernPGP: ByteWitchDecoder, ParseCompanion() {
    override val name = "PGP"

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        parseOffset = 0
        val blocks = mutableListOf<ByteWitchResult>()
        check(data.size > 10) { "unreasonably short PGP message" }

        while(parseOffset < data.size) {
            val start = sourceOffset + parseOffset
            val ctb = readInt(data, 1)
            check(ctb and 0x80 != 0) { "expecting CTB to have high bit set" }

            var lengthFieldSize: Int
            val type: Int
            val length: Int
            val newFormat: Boolean

            // old format header
            if(ctb and 0x40 == 0) {
                newFormat = false
                lengthFieldSize = 1 shl (ctb and 0x03)
                type = (ctb and 0x7c) shr 2

                length = if(lengthFieldSize == 8) {
                    lengthFieldSize = 0
                    data.size - parseOffset
                }
                else {
                    readUInt(data, lengthFieldSize, ByteOrder.BIG).toInt()
                }
            }
            // new format header
            else {
                newFormat = true
                type = ctb and 0x3F
                val firstLenByte = readUInt(data, 1, ByteOrder.BIG).toInt()
                when(firstLenByte) {
                    in 0..191 -> {
                        lengthFieldSize = 1
                        length = firstLenByte
                    }
                    in 192..223 -> {
                        lengthFieldSize = 2
                        val secondLenByte = readUInt(data, 1, ByteOrder.BIG).toInt()
                        length = ((firstLenByte - 192) shl 8) + secondLenByte + 192
                    }
                    255 -> {
                        lengthFieldSize = 5
                        length = readUInt(data, 4, ByteOrder.BIG).toInt()
                    }
                    else -> throw Exception("illegal first length byte value $firstLenByte")
                }
            }

            Logger.tag("PGP", "reading type $type lengthFieldSize $lengthFieldSize length $length new? $newFormat")

            check(type in 1..19) { "undefined packet type $type" }
            check(data.size - parseOffset >= length) { "incorrect data length in header" }


            val block = handleNewBlock(type, length, lengthFieldSize, data, start, sourceOffset)

            blocks.add(block)
        }

        return PGPfile(blocks, Pair(sourceOffset, sourceOffset+parseOffset))
    }

    private fun handleNewBlock(type: Int, length: Int, lengthFieldSize: Int, data: ByteArray, start: Int, sourceOffset: Int): ByteWitchResult {
        return when(type) {
            2 -> readSignature(length, lengthFieldSize, data, start, sourceOffset)
            13 -> PGPuserid(length, lengthFieldSize, readBytes(data, length).decodeToString(), Pair(start, sourceOffset+parseOffset))
            else -> PGPnewblock(type, length, lengthFieldSize, readBytes(data, length), Pair(start, sourceOffset+parseOffset))
        }
    }

    private fun readSignature(length: Int, lengthFieldSize: Int, data: ByteArray, start: Int, sourceOffset: Int): ByteWitchResult {
        val version = readInt(data, 1)

        if(version == 4) {
            val signatureType = readInt(data, 1)
            val pkc = readInt(data, 1)
            val mda = readInt(data, 1)
            val hashedSubLength = readInt(data, 2, false, ByteOrder.BIG)
            val hashedSub = readBytes(data, hashedSubLength)
            val unhashedSubLength = readInt(data, 2, false, ByteOrder.BIG)
            val unhashedSub = readBytes(data, unhashedSubLength)
            val checksum = readBytes(data, 2)
            val components = mutableListOf<PGPBigInt>()
            while(start-sourceOffset+lengthFieldSize+1+length > parseOffset) {
                components.add(readLengthPrefixedBits(data, sourceOffset))
            }

            return PGPsignatureV4(length, lengthFieldSize, signatureType, pkc, mda, hashedSub, unhashedSub, checksum, components, Pair(start, sourceOffset+parseOffset))
        }
        else {
            val dLength = readInt(data, 1)
            check(dLength == 5) { "unexpected length of field d in signature: $dLength" }

            val signatureType = readInt(data, 1)
            val timestamp = Date(readUInt(data, 4, ByteOrder.BIG).toLong()*1000)
            val keyID = readBytes(data, 8)
            val pkc = readInt(data, 1)
            val mda = readInt(data, 1)
            val checksum = readBytes(data, 2)

            val components = mutableListOf<PGPBigInt>()
            while(start-sourceOffset+lengthFieldSize+1+length > parseOffset) {
                components.add(readLengthPrefixedBits(data, sourceOffset))
            }

            return PGPsignatureV3(length, lengthFieldSize, signatureType, timestamp, keyID, pkc, mda, checksum, components, Pair(start, sourceOffset+parseOffset))
        }
    }

    private fun readLengthPrefixedBits(data: ByteArray, sourceOffset: Int): PGPBigInt {
        val start = parseOffset
        val bitLength = readUInt(data, 2, ByteOrder.BIG).toInt()
        val byteLength = ceil((bitLength.toDouble() / 8)).roundToInt()
        val bytes = readBytes(data, byteLength)
        return PGPBigInt(bitLength, bytes, Pair(sourceOffset+start, sourceOffset+ parseOffset))
    }
}

class PGPnewblock(val type: Int, val length: Int, val lengthSize: Int, val data: ByteArray, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC
    private val typeLookup = mapOf(
        1 to "Public-Key Encrypted Session Key Packet",
        2 to "Signature Packet",
        3 to "Symmetric-Key Encrypted Session Key Packet",
        4 to "One-Pass Signature Packet",
        5 to "Secret-Key Packet",
        6 to "Public-Key Packet",
        7 to "Secret-Subkey Packet",
        8 to "Compressed Data Packet",
        9 to "Symmetrically Encrypted Data Packet",
        10 to "Marker Packet",
        11 to "Literal Data Packet",
        12 to "Trust Packet",
        13 to "User ID Packet",
        14 to "Public-Subkey Packet",
        17 to "User Attribute Packet",
        18 to "Sym. Encrypted and Integrity Protected Data Packet",
        19 to "Modification Detection Code Packet"
    )

    private fun typeToString(type: Int): String {
        return if(typeLookup.containsKey(type)) typeLookup[type]!! else "UNK($type)"
    }

    override fun renderHTML(): String {
        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>Type ${typeToString(type)}</div><div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>Length $length B</div><div class=\"bpvalue data\" ${relativeRangeTags(lengthSize+1, data.size)}>0x${data.hex()}</div></div>"
    }
}

class PGPuserid(val length: Int, val lengthSize: Int, val id: String, override val sourceByteRange: Pair<Int, Int>): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC
    override fun renderHTML(): String {
        return "<div class=\"roundbox generic\" $byteRangeDataTags><div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>User ID Packet</div><div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>Length $length B</div><div class=\"bpvalue stringlit\" ${rangeTagsFor(sourceByteRange.first+lengthSize+1, sourceByteRange.second)}>${htmlEscape(id)}</div></div>"
    }
}

open class PGPsignatureV3(
    val length: Int,
    val lengthSize: Int,
    val signatureType: Int,
    val timestamp: Date,
    val keyID: ByteArray,
    val pkc: Int,
    val mda: Int,
    val checksum: ByteArray,
    val signature: List<PGPBigInt>,
    override val sourceByteRange: Pair<Int, Int>
): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    protected val pkcToString = mapOf(
        1 to "RSA",
        2 to "RSA Encrypt-Only",
        3 to "RSA Sign-Only",
        16 to "Elgamal Encrypt-Only",
        17 to "DSA",
        18 to "Elliptic Curve",
        19 to "ECDSA",
        20 to "Elgamal Encrypt or Sign",
        21 to "Diffie-Hellman"
    )

    protected val mdToString = mapOf(
        1 to "MD5",
        2 to "SHA-1",
        3 to "RIPEMD160",
        8 to "SHA256",
        9 to "SHA384",
        10 to "SHA512",
        11 to "SHA224"
    )

    protected val sigTypeToString = mapOf(
        0x00 to "Signature of a binary document",
        0x01 to "Signature of a canonical text document",
        0x02 to "Standalone signature",
        0x10 to "Generic certification",
        0x11 to "Persona certification",
        0x12 to "Casual certification",
        0x13 to "Positive certification",
        0x18 to "Subkey Binding",
        0x19 to "Primary Key Binding",
        0x1F to "Signature directly on a key",
        0x20 to "Key revocation",
        0x28 to "Subkey revocation",
        0x30 to "Certification revocation",
        0x40 to "Timestamp signature",
        0x50 to "Third-Party Confirmation"
    )

    override fun renderHTML(): String {
        val fields = listOf(
            "<div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>Signature Packet</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>Length $length B</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize+2, 1)}>Type ${sigTypeToString.getOrElse(signatureType){ "unk ($signatureType)" }}</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize+3, 4)}>$timestamp</div>",
            "<div class=\"bpvalue data\" ${relativeRangeTags(1+lengthSize+7, 8)}>KeyID 0x${keyID.hex()}</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize+15, 1)}>PKC: ${pkcToString.getOrElse(pkc) { "unk ($pkc)" }}</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize+16, 1)}>MD: ${mdToString.getOrElse(mda) { "unk ($mda)" }}</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1+lengthSize+17, 2)}>Checksum 0x${checksum.hex()}</div>"
        )
        return "<div class=\"roundbox generic\" $byteRangeDataTags>${fields.joinToString(" ")} ${signature.joinToString(" "){ it.renderHTML() }}</div>"
    }
}

class PGPsignatureV4(
    val length: Int,
    val lengthSize: Int,
    val signatureType: Int,
    val pkc: Int,
    val mda: Int,
    val hashedSub: ByteArray,
    val unhashedSub: ByteArray,
    val checksum: ByteArray,
    val signature: List<PGPBigInt>,
    override val sourceByteRange: Pair<Int, Int>
): ByteWitchResult {
    override val colour = ByteWitchResult.Colour.GENERIC

    protected val pkcToString = mapOf(
        1 to "RSA",
        2 to "RSA Encrypt-Only",
        3 to "RSA Sign-Only",
        16 to "Elgamal Encrypt-Only",
        17 to "DSA",
        18 to "Elliptic Curve",
        19 to "ECDSA",
        20 to "Elgamal Encrypt or Sign",
        21 to "Diffie-Hellman"
    )

    protected val mdToString = mapOf(
        1 to "MD5",
        2 to "SHA-1",
        3 to "RIPEMD160",
        8 to "SHA256",
        9 to "SHA384",
        10 to "SHA512",
        11 to "SHA224"
    )

    protected val sigTypeToString = mapOf(
        0x00 to "Signature of a binary document",
        0x01 to "Signature of a canonical text document",
        0x02 to "Standalone signature",
        0x10 to "Generic certification",
        0x11 to "Persona certification",
        0x12 to "Casual certification",
        0x13 to "Positive certification",
        0x18 to "Subkey Binding",
        0x19 to "Primary Key Binding",
        0x1F to "Signature directly on a key",
        0x20 to "Key revocation",
        0x28 to "Subkey revocation",
        0x30 to "Certification revocation",
        0x40 to "Timestamp signature",
        0x50 to "Third-Party Confirmation"
    )

    override fun renderHTML(): String {
        val fields = listOf(
            "<div class=\"bpvalue\" ${relativeRangeTags(0, 1)}>Signature Packet</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1, lengthSize)}>Length $length B</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1 + lengthSize + 2, 1)}>Type ${sigTypeToString.getOrElse(signatureType) { "unk ($signatureType)" }}</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1 + lengthSize + 3, 1)}>PKC: ${pkcToString.getOrElse(pkc) { "unk ($pkc)" }}</div>",
            "<div class=\"bpvalue\" ${relativeRangeTags(1 + lengthSize + 4, 1)}>MD: ${mdToString.getOrElse(mda) { "unk ($mda)" }}</div>",
            "<div class=\"bpvalue data\" ${relativeRangeTags(1 + lengthSize + 5, hashedSub.size)}>Hashed Substructs 0x${hashedSub.hex()}</div>",
            "<div class=\"bpvalue data\" ${relativeRangeTags(1 + lengthSize + 5 + hashedSub.size, unhashedSub.size)}>Unhashed Substructs 0x${unhashedSub.hex()}</div>",
            "<div class=\"bpvalue data\" ${relativeRangeTags(1 + lengthSize + 5 + hashedSub.size + unhashedSub.size, 2)}>Checksum 0x${checksum.hex()}</div>"
        )
        return "<div class=\"roundbox generic\" $byteRangeDataTags>${fields.joinToString(" ")} ${signature.joinToString(" ") { it.renderHTML() }}</div>"
    }
}