import bitmage.fromHex
import bitmage.hex
import decoders.ECCurves
import kotlin.test.Test

class EcTests {
    @Test
    fun uncompressedDetection() {
        // uncompressed secp256r1 key
        val bytes = "40514acb2ae8fe1164c96310e5023acf7723fb57e8baccf7f9f06800d56ee973fd0961621b5f95690b7c36e53a1350ff08b8b8a4757f3f9d33e1701e99a7a02e".fromHex()
        val guesses = ECCurves.whichCurves(bytes, includeCompressed = false, includeUncompressed = true, errorOnInvalidSize = true)

        check("secp256r1" in guesses && guesses.size == 1) { "uncompressed secp256r1 key should be detected unambiguously" }
    }

    @Test
    fun compressedDetectionC25519() {
        val pubkeys = listOf(
            // test vectors from RFC7748 https://datatracker.ietf.org/doc/html/rfc7748#section-6.2
            "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a".fromHex().reversedArray(),
            "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f".fromHex().reversedArray(),
            // random pubkeys encoded from BouncyCastle
            "f759d1a94cec9e3aa2ab0e053b6e77813ccd1ce79c24f4ab4bbb5fdef579650d".fromHex().reversedArray(),
            "50dc397f043b7bbd8ce216f95553d84e8e7dc0c42ba1acf7afe49e518a923c69".fromHex().reversedArray(),
            "41c9115e68b5fdc88bd8e67c5f908a9d2912b3e1a4af2878c4eea4867a969c42".fromHex().reversedArray(),
            "b187cf1d16aa79dc5c272f5a402913ec48c06ce71e21575ad3c623f92e4c4f6f".fromHex().reversedArray(),
            "1fb95b29fe00cdd834bd9b4e8702aaf3dd7a1392cf2f567eac2e8592e07bd217".fromHex().reversedArray(),
            "3ef9830301cfddbb9df9208ca9ce42b816a897fc568c3d06df522199276b7944".fromHex().reversedArray(),
            "c6a5ded41960340cbb60cfae3584f21bec912fca0a77c0a601a33b7cedbca125".fromHex().reversedArray(),
        ).shuffled().subList(0, 2)

        pubkeys.forEach {
            val guesses = ECCurves.whichCurves(it, includeCompressed = true, includeUncompressed = false, errorOnInvalidSize = true)

            check("Curve25519" in guesses) { "compressed curve25519 points should be detected as curve25519: ${it.hex()}" }
        }
    }

    @Test
    fun compressedDetectionC448() {
        // putting more keys in here results in a test timeout so let's keep this slim
        val pubkeys = listOf(
            // test vectors from RFC7748 https://datatracker.ietf.org/doc/html/rfc7748#section-6.2
            "9b08f7cc31b7e3e67d22d5aea121074a273bd2b83de09c63faa73d2c22c5d9bbc836647241d953d40c5b12da88120d53177f80e532c41fa0".fromHex().reversedArray(),
            "3eb7a829b0cd20f5bcfc0b599b6feccf6da4627107bdb0d4f345b43027d8b972fc3e34fb4232a13ca706dcb57aec3dae07bdc1c67bf33609".fromHex().reversedArray(),
            // random pubkeys encoded by BouncyCastle
            "49cf8bc5d16e98ed2c2b52d1076281dffca823020a0b816a02091437e08aada262c7996364a5160d2499a7e3de1425e5f4c2bfa755c14c20".fromHex().reversedArray(),
            "6238cade48932f5f19ce3e101737e61ceceff08097bed36314c55e0278a34ff1e56c06e6874f05051e3cd21e205e2959efd60fb71a094afb".fromHex().reversedArray(),
            "0152de090c2ed1408f6f4c61293b79fcdcb37022642b2a3e236152bfd504acabcb67f8aa15e9e52736ea3b944ed0e1789101219a5eafb6e4".fromHex().reversedArray(),
        ).shuffled().subList(0, 2)

        pubkeys.forEach {
            val guesses = ECCurves.whichCurves(it, includeCompressed = true, includeUncompressed = false, errorOnInvalidSize = true)
            check("Curve448" in guesses) { "compressed curve448 points should be detected as curve448: ${it.hex()}" }
        }
    }

    @Test
    fun compressedDetectionEd25519() {
        // putting more keys in here results in a test timeout so let's keep this slim
        val pubkeys = listOf(
            // test vectors from RFC8032 https://www.rfc-editor.org/rfc/rfc8032.html
            "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a".fromHex().reversedArray(),
            "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c".fromHex().reversedArray(),
            "fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025".fromHex().reversedArray(),
            "278117fc144c72340f67d0f2316e8386ceffbf2b2428c9c51fef7c597f1d426e".fromHex().reversedArray(),
            "ec172b93ad5e563bf4932c70e1245034c35467ef2efd4d64ebf819683467e23f".fromHex().reversedArray() // changed last 0xbf to 0x3f to remove encoded sign bit

        ).shuffled().subList(0, 2)

        pubkeys.forEach {
            val guesses = ECCurves.whichCurves(it, includeCompressed = true, includeUncompressed = false, errorOnInvalidSize = true)
            check("Ed25519" in guesses) { "compressed Ed25519 points should be detected as Ed25519: ${it.hex()}" }
        }
    }

    @Test
    fun compressedDetectionEd448() {
        // putting more keys in here results in a test timeout so let's keep this slim
        val pubkeys = listOf(
            // test vectors from RFC8032 https://www.rfc-editor.org/rfc/rfc8032.html
            "5fd7449b59b461fd2ce787ec616ad46a1da1342485a70e1f8a0ea75d80e96778edf124769b46c7061bd6783df1e50f6cd1fa1abeafe82561".fromHex().reversedArray(),
            "43ba28f430cdff456ae531545f7ecd0ac834a55d9358c0372bfa0c6c6798c0866aea01eb00742802b8438ea4cb82169c235160627b4c3a94".fromHex().reversedArray(),
            "dcea9e78f35a1bf3499a831b10b86c90aac01cd84b67a0109b55a36e9328b1e365fce161d71ce7131a543ea4cb5f7e9f1d8b006964470014".fromHex().reversedArray(),
            "3ba16da0c6f2cc1f30187740756f5e798d6bc5fc015d7c63cc9510ee3fd44adc24d8e968b6e46e6f94d19b945361726bd75e149ef09817f5".fromHex().reversedArray(),
        ).shuffled().subList(0, 2)

        pubkeys.forEach {
            val guesses = ECCurves.whichCurves(it, includeCompressed = true, includeUncompressed = false, errorOnInvalidSize = true)
            check("Ed448-Goldilocks" in guesses) { "compressed Ed448 points should be detected as Ed448: ${it.hex()}" }
        }
    }
}