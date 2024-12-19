package decoders

import bitmage.hex
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.math.ceil

// (most) curves from https://safecurves.cr.yp.to/
object ECCurves {
    val modulos = mapOf<String, ECCurveModulus>(
        "M-221" to ModPair(221, 3),
        "E-222" to ModPair(222, 117),
        "NIST P-224" to ModTriple(224, 96, -1),
        "Curve1174" to ModPair(251, 9),
        "Curve25519" to ModPair(255, 19),
        "NIST P-256" to ModPolynomial(listOf(Pair(1, 256), Pair(-1, 224), Pair(1, 192), Pair(1, 96), Pair(-1, 0))),
        "secp256k1" to ModTriple(256, 32, 977),
        "E-382" to ModPair(382, 105),
        "M-383" to ModPair(383, 187),
        "NIST P-384" to ModPolynomial(listOf(Pair(1, 384), Pair(-1, 128), Pair(-1, 96), Pair(1, 32), Pair(-1, 0))),
        "Curve41417" to ModPair(414, 17),
        "Ed448-Goldilocks" to ModTriple(448, 224, 1),
        "M-511" to ModPair(511, 187),
        "E-521" to ModPair(521, 1),
    )
}

class Sec1Ec {
    companion object : ByteWitchDecoder {
        override val name = "SEC1 EC"

        private val sizes = ECCurves.modulos.mapValues { it.value.byteSize }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {

            // this got a bit long... basically: find curves with matching byte sizes and transform into a list of expected sizes
            // with associated curve names, where each size is only represented once even if multiple curves match
            val candidatesCompressed = sizes.filter { data.size == it.value + 1}.toList().groupBy { it.second }.map { candidateSize -> Pair(candidateSize.key, candidateSize.value.joinToString("/"){ it.first }) }
            val candidatesUncompressed = sizes.filter { data.size == 2 * it.value + 1 }.toList().groupBy { it.second }.map { candidateSize -> Pair(candidateSize.key, candidateSize.value.joinToString("/"){ it.first }) }
            val candidates = candidatesCompressed.map { Triple(it.second, it.first, true) } + candidatesUncompressed.map { Triple(it.second, it.first, false) }

            check(candidates.isNotEmpty()) { "size does not match expected SEC1 encodings for any curve" }

            val parsed = candidates.mapNotNull {
                if (it.third)
                    decodeCompressed(data, ceil(it.second.toDouble() / 8).toInt(), it.first, sourceOffset, inlineDisplay)
                else
                    decodeUncompressed(data, ceil(it.second.toDouble() / 8).toInt(), it.first, sourceOffset, inlineDisplay)
            }

            check(parsed.isNotEmpty()) { "size matches expected SEC1 encodings but data is not actually valid" }

            return parsed.first()
        }

        private fun decodeUncompressed(data: ByteArray, elementSize: Int, curve: String, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult? {
            if(data[0].toInt() != 0x04)
                return null

            val x = data.sliceArray(1 until 1+elementSize)
            val y = data.sliceArray(1+elementSize until 1+2*elementSize)

            return EcPoint(x, y, curve, inlineDisplay, Pair(sourceOffset, sourceOffset+1+2*elementSize))
        }

        private fun decodeCompressed(data: ByteArray, elementSize: Int, curve: String, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult? {
            if(data[0].toInt() != 0x02 && data[0].toInt() != 0x03)
                return null

            val x = data.sliceArray(1 until 1+elementSize)
            val y = data.sliceArray(0 until 1)

            return EcPoint(x, y, curve, inlineDisplay, Pair(sourceOffset, sourceOffset+1+elementSize))
        }
    }
}

class EcPoint(private val x: ByteArray, private val y: ByteArray, private val curve: String, val inlineDisplay: Boolean, override val sourceByteRange: Pair<Int, Int>) : ByteWitchResult {
    override fun renderHTML(): String {

        val compressed = if(y.size == 1) "Compressed" else "Uncompressed"

        val identifier = if (inlineDisplay)
            "<div class=\"bpvalue\" ${rangeTagsFor(sourceByteRange.first, sourceByteRange.first+1)}>$compressed SEC1 ECPoint $curve</div>"
        else
            "<div class=\"bpvalue\" ${rangeTagsFor(sourceByteRange.first, sourceByteRange.first+1)}>$compressed $curve</div>"

        val yTags = if(y.size == 1) rangeTagsFor(sourceByteRange.first, sourceByteRange.first+1) else rangeTagsFor(sourceByteRange.second-y.size, sourceByteRange.second)
        val xHtml = "<div class=\"bpvalue data\" ${rangeTagsFor(sourceByteRange.first+1, sourceByteRange.first+1+x.size)}>X: 0x${x.hex()}</div>"
        val yHtml = "<div class=\"bpvalue data\" $yTags>Y: 0x${y.hex()}</div>"

        return "<div class=\"roundbox generic\" $byteRangeDataTags>$identifier$xHtml$yHtml</div>"

    }
}

interface ECCurveModulus {
    val bitSize: Int
    val byteSize: Int
        get() = ceil(bitSize.toDouble() / 8).toInt()

    val exactValue: BigDecimal
}

data class ModPair(override val bitSize: Int, val subtract: Int) : ECCurveModulus {
    init {
        check(subtract >= 0){ "ModPair expects subtract parameter to be non-negative, got $subtract" }
    }

    override val exactValue: BigDecimal
        get() = BigDecimal.TWO.pow(bitSize) - subtract
}

data class ModTriple(override val bitSize: Int, val subtractPow: Int, val subtract: Int): ECCurveModulus {
    init {
        check(subtractPow > 0){ "ModPair expects subtractPow parameter to be positive, got $subtractPow" }
    }

    override val exactValue: BigDecimal
        get() = BigDecimal.TWO.pow(bitSize) - BigDecimal.TWO.pow(subtractPow) - subtract
}

class ModPolynomial(list: List<Pair<Int, Int>>): ECCurveModulus {
    private val exponents = list.sortedBy { -it.second } // ensure largest exponent is first

    override val bitSize: Int
        get() = exponents.first().second

    override val exactValue: BigDecimal
        get() = exponents.map { if(it.first < 0) BigDecimal.TWO.pow(it.second).negate() else BigDecimal.TWO.pow(it.second) }.reduce { sum, elem -> sum + elem }
}