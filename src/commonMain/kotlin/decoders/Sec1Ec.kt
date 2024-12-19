package decoders

import bitmage.fromIndex
import bitmage.hex
import bitmage.untilIndex
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import com.ionspin.kotlin.bignum.integer.toBigInteger
import com.ionspin.kotlin.bignum.modular.ModularBigInteger
import kotlin.math.ceil

/*
 * Elliptic curve parameters from https://safecurves.cr.yp.to/
 * Curve point detection heuristics based on "Elligator: Elliptic-curve points indistinguishable from uniform random strings"
 * see https://dl.acm.org/doi/pdf/10.1145/2508859.2516734
 */
object ECCurves {
    val modulos = mapOf<String, ECCurveModulus>(
        "M-221" to ModPair(221, 3),
        "E-222" to ModPair(222, 117),
        "NIST P-224" to ModTriple(224, 96, -1),
        "Curve1174" to ModPair(251, 9),
        "Curve25519" to ModPair(255, 19),
        "secp256r1" to ModPolynomial(listOf(Pair(1, 256), Pair(-1, 224), Pair(1, 192), Pair(1, 96), Pair(-1, 0))),
        "secp256k1" to ModTriple(256, 32, 977),
        "E-382" to ModPair(382, 105),
        "M-383" to ModPair(383, 187),
        "NIST P-384" to ModPolynomial(listOf(Pair(1, 384), Pair(-1, 128), Pair(-1, 96), Pair(1, 32), Pair(-1, 0))),
        "Curve41417" to ModPair(414, 17),
        "Ed448-Goldilocks" to ModTriple(448, 224, 1),
        "M-511" to ModPair(511, 187),
        "E-521" to ModPair(521, 1),
    )

    // largely untested - here be typos
    private val equations = mapOf<String, EcEqEquation?>(
        // y^2 = x^3+117050x^2+x
        "M-221" to EcEqYSquaredEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 3),
                Triple(117050, EcEqX, 2),
                Triple(1, EcEqX, 1)
            )
        ),
        // x^2+y^2 = 1+160102x^2y^2
        "E-222" to EcEqEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 2),
                Triple(1, EcEqY, 2)
            ),
            EcEqPolynomial(
                Triple(1, EcEqConstant((1).toBigInteger()), 1),
                Triple(160102, EcEqProduct(EcEqXSquare, EcEqYSquare), 1),
            )
        ),
        // y^2 = x^3-3x+18958286285566608000408668544493926415504680968679321075787234672564
        "NIST P-224" to EcEqYSquaredEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 3),
                Triple(-3, EcEqX, 1),
                Triple(1, EcEqConstant(BigInteger.parseString("18958286285566608000408668544493926415504680968679321075787234672564")), 1)
            )
        ),
        // x^2+y^2 = 1-1174x^2y^2
        "Curve1174" to EcEqEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 2),
                Triple(1, EcEqY, 2)
            ),
            EcEqPolynomial(
                Triple(1, EcEqConstant((1).toBigInteger()), 1),
                Triple(-1174, EcEqProduct(EcEqXSquare, EcEqYSquare), 1),
            )
        ),
        // y^2 = x^3+486662x^2+x
        "Curve25519" to EcEqYSquaredEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 3),
                Triple(486662, EcEqX, 2),
                Triple(1, EcEqX, 1),
            )
        ),
        // y^2 = x^3-3x+41058363725152142129326129780047268409114441015993725554835256314039467401291
        "secp256r1" to EcEqYSquaredEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 3),
                Triple(-3, EcEqX, 1),
                Triple(1, EcEqConstant(BigInteger.parseString("41058363725152142129326129780047268409114441015993725554835256314039467401291")), 1),
            )
        ),
        // y^2 = x^3+0x+7
        "secp256k1" to EcEqYSquaredEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 3),
                Triple(1, EcEqConstant((7).toBigInteger()), 1),
            )
        ),
        // x^2+y^2 = 1-67254x^2y^2
        "E-382" to EcEqEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 2),
                Triple(1, EcEqY, 2)
            ),
            EcEqPolynomial(
                Triple(1, EcEqConstant((1).toBigInteger()), 1),
                Triple(-67254, EcEqProduct(EcEqXSquare, EcEqYSquare), 1),
            )
        ),
        // y^2 = x^3+2065150x^2+x
        "M-383" to EcEqYSquaredEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 3),
                Triple(2065150, EcEqX, 2),
                Triple(1, EcEqX, 1),
            )
        ),
        // y^2 = x^3-3x+27580193559959705877849011840389048093056905856361568521428707301988689241309860865136260764883745107765439761230575
        "NIST P-384" to EcEqYSquaredEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 3),
                Triple(-3, EcEqX, 1),
                Triple(1, EcEqConstant(BigInteger.parseString("27580193559959705877849011840389048093056905856361568521428707301988689241309860865136260764883745107765439761230575")), 1),
            )
        ),
        // x^2+y^2 = 1+3617x^2y^2
        "Curve41417" to EcEqEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 2),
                Triple(1, EcEqY, 2)
            ),
            EcEqPolynomial(
                Triple(1, EcEqConstant((1).toBigInteger()), 1),
                Triple(3617, EcEqProduct(EcEqXSquare, EcEqYSquare), 1),
            )
        ),
        // x^2+y^2 = 1-39081x^2y^2
        "Ed448-Goldilocks" to EcEqEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 2),
                Triple(1, EcEqY, 2)
            ),
            EcEqPolynomial(
                Triple(1, EcEqConstant((1).toBigInteger()), 1),
                Triple(-39081, EcEqProduct(EcEqXSquare, EcEqYSquare), 1),
            )
        ),
        // y^2 = x^3+530438x^2+x
        "M-511" to EcEqYSquaredEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 3),
                Triple(530438, EcEqX, 2),
                Triple(1, EcEqX, 1),
            )
        ),
        // x^2+y^2 = 1-376014x^2y^2
        "E-521" to EcEqEquals(
            EcEqPolynomial(
                Triple(1, EcEqX, 2),
                Triple(1, EcEqY, 2)
            ),
            EcEqPolynomial(
                Triple(1, EcEqConstant((1).toBigInteger()), 1),
                Triple(-376014, EcEqProduct(EcEqXSquare, EcEqYSquare), 1),
            )
        ),
    )

    // find curves that could plausibly generate the supplied data as a curve point
    fun whichCurves(data: ByteArray, includeCompressed: Boolean = true, includeUncompressed: Boolean = true): Set<String> {
        var compressedCandidates = if(includeCompressed) modulos.filter { data.size == it.value.byteSize } else emptyMap()
        var uncompressedCandidates = if(includeUncompressed) modulos.filter { data.size == 2 * it.value.byteSize } else emptyMap()

        // first: naive quick check - see if encoded points are small enough to be plausible curve points

        if(compressedCandidates.isNotEmpty()) {
            Logger.log("Compressed candidates (matching bytesize): ${compressedCandidates.keys}")
            val x = BigInteger.fromByteArray(data, Sign.POSITIVE)
            compressedCandidates = compressedCandidates.filter {
                checkPlausibleQuick(x, null, it.value)
            }

            Logger.log("Compressed candidates (below modulo): ${compressedCandidates.keys}")

            // second: for compressed points, try to compute y^2 and see if it is actually square
            compressedCandidates = compressedCandidates.filter { checkPlausible(x, it.key) }
            Logger.log("Compressed candidates (y^2 is square): ${compressedCandidates.keys}")
        }

        // implies data length is even
        if(uncompressedCandidates.isNotEmpty()) {
            val x = BigInteger.fromByteArray(data.untilIndex(data.size/2), Sign.POSITIVE)
            val y = BigInteger.fromByteArray(data.fromIndex(data.size/2), Sign.POSITIVE)
            uncompressedCandidates = uncompressedCandidates.filter { checkPlausibleQuick(x, y, it.value) }

            // second: for uncompressed points, evaluate the curve polynomial and check if valid
            uncompressedCandidates = uncompressedCandidates.filter { checkPlausible(x, y, it.key) }
        }


        val guesses = (compressedCandidates.keys + uncompressedCandidates.keys)
        return guesses.ifEmpty { setOf("unknown curve") }
    }

    private fun checkPlausibleQuick(x: BigInteger, y: BigInteger?, modulus: ECCurveModulus): Boolean {
        if(x > modulus.exactValue)
            return false

        if(y != null && y > modulus.exactValue)
            return false

        return true
    }

    private fun checkPlausible(x: BigInteger, y: BigInteger, curve: String): Boolean {
        val mod = modulos[curve]!!
        val eq = equations[curve] ?: return true
        return eq.test(x, y, mod.exactValue)
    }

    private fun checkPlausible(x: BigInteger, curve: String): Boolean {
        val mod = modulos[curve]!!
        val eq = equations[curve] ?: return true

        if(eq !is EcEqYSquaredEquals)
            return true

        val ysq = eq.computeYSquare(x, mod.exactValue)

        // assuming our curve modulus is prime (does that actually always hold? hope so)
        // we can check if our computed y^2 is actually a square using the Legendre symbol
        // https://en.wikipedia.org/wiki/Legendre_symbol
        val legendre = ysq.pow((mod.exactValue-1)/2) // expect 1 for quadratic residue

        return legendre.toBigInteger() == BigInteger.ONE
    }
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
                    decodeCompressed(data, it.second, sourceOffset, inlineDisplay)
                else
                    decodeUncompressed(data, it.second, sourceOffset, inlineDisplay)
            }

            check(parsed.isNotEmpty()) { "size matches expected SEC1 encodings but data is not actually valid. candidates: ${candidates.joinToString(" ")}" }

            return parsed.first()
        }

        private fun decodeUncompressed(data: ByteArray, elementSize: Int, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult? {
            if(data[0].toInt() != 0x04)
                return null

            val x = data.sliceArray(1 until 1+elementSize)
            val y = data.sliceArray(1+elementSize until 1+2*elementSize)

            val curves = ECCurves.whichCurves(data.fromIndex(1), includeCompressed = false).joinToString("/")

            return EcPoint(x, y, curves, inlineDisplay, Pair(sourceOffset, sourceOffset+1+2*elementSize))
        }

        private fun decodeCompressed(data: ByteArray, elementSize: Int, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult? {
            if(data[0].toInt() != 0x02 && data[0].toInt() != 0x03)
                return null

            val x = data.sliceArray(1 until 1+elementSize)
            val y = data.sliceArray(0 until 1)

            val curves = ECCurves.whichCurves(data.fromIndex(1), includeUncompressed = false).joinToString("/")

            return EcPoint(x, y, curves, inlineDisplay, Pair(sourceOffset, sourceOffset+1+elementSize))
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

    val exactValue: BigInteger
}

data class ModPair(override val bitSize: Int, val subtract: Int) : ECCurveModulus {
    init {
        check(subtract >= 0){ "ModPair expects subtract parameter to be non-negative, got $subtract" }
    }

    override val exactValue: BigInteger
        get() = BigInteger.TWO.pow(bitSize) - subtract
}

data class ModTriple(override val bitSize: Int, val subtractPow: Int, val subtract: Int): ECCurveModulus {
    init {
        check(subtractPow > 0){ "ModPair expects subtractPow parameter to be positive, got $subtractPow" }
    }

    override val exactValue: BigInteger
        get() = BigInteger.TWO.pow(bitSize) - BigInteger.TWO.pow(subtractPow) - subtract
}

class ModPolynomial(list: List<Pair<Int, Int>>): ECCurveModulus {
    private val exponents = list.sortedBy { -it.second } // ensure largest exponent is first

    override val bitSize: Int
        get() = exponents.first().second

    override val exactValue: BigInteger
        get() = exponents.map { if(it.first < 0) BigInteger.TWO.pow(it.second).negate() else BigInteger.TWO.pow(it.second) }.reduce { sum, elem -> sum + elem }
}

interface EcEqEquation {
    fun test(x: BigInteger, y: BigInteger, mod: BigInteger): Boolean
}

class EcEqEquals(val a: EcExpression, val b: EcExpression) : EcEqEquation {
    override fun test(x: BigInteger, y: BigInteger, mod: BigInteger): Boolean {
        val modCreator = ModularBigInteger.creatorForModulo(mod)
        val xm = modCreator.fromBigInteger(x)
        val ym = modCreator.fromBigInteger(y)

        val ra = a.evaluate(xm, ym)
        val rb = b.evaluate(xm, ym)
        return ra == rb
    }
}

class EcEqYSquaredEquals(val a: EcExpression) : EcEqEquation {
    override fun test(x: BigInteger, y: BigInteger, mod: BigInteger): Boolean {
        val modCreator = ModularBigInteger.creatorForModulo(mod)
        val xm = modCreator.fromBigInteger(x)
        val ym = modCreator.fromBigInteger(y)

        val b = EcEqYSquare
        val ra = a.evaluate(xm, ym)
        val rb = b.evaluate(xm, ym)
        return ra == rb
    }

    fun computeYSquare(x: BigInteger, mod: BigInteger): ModularBigInteger {
        val modCreator = ModularBigInteger.creatorForModulo(mod)
        val xm = modCreator.fromBigInteger(x)

        return a.evaluate(xm, xm)
    }
}

interface EcExpression {
    fun evaluate(x: ModularBigInteger, y: ModularBigInteger): ModularBigInteger
}

class EcEqPolynomial(private vararg val parts: Triple<Int, EcExpression, Int>) : EcExpression {
    override fun evaluate(x: ModularBigInteger, y: ModularBigInteger): ModularBigInteger {
         return parts.map {
            val exponentiated = it.second.evaluate(x, y).pow(it.third)
            exponentiated * it.first
        }.reduce { sum, elem -> sum + elem }
    }
}

class EcEqProduct(private vararg val parts: EcExpression) : EcExpression {
    override fun evaluate(x: ModularBigInteger, y: ModularBigInteger): ModularBigInteger {
        return parts.map { it.evaluate(x, y) }.reduce { sum, elem -> sum * elem }
    }
}

class EcEqConstant(val value: BigInteger) : EcExpression {
    override fun evaluate(x: ModularBigInteger, y: ModularBigInteger) = value.toModularBigInteger(x.modulus)
}

object EcEqX : EcExpression {
    override fun evaluate(x: ModularBigInteger, y: ModularBigInteger) = x
}

object EcEqY : EcExpression {
    override fun evaluate(x: ModularBigInteger, y: ModularBigInteger) = y
}

object EcEqYSquare : EcExpression {
    override fun evaluate(x: ModularBigInteger, y: ModularBigInteger) = y.pow(2)
}

object EcEqXSquare : EcExpression {
    override fun evaluate(x: ModularBigInteger, y: ModularBigInteger) = x.pow(2)
}