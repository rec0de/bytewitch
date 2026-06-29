import bitmage.fromHex
import bitmage.hex
import decoders.*
import kotlin.test.Test

class ASN1Tests {
    @Test
    fun berTlvDetection() {
        val s1 = "5F2A02097882021C00950580800088009A032110149C01009F02060000000020219F03060000000000009F0902008C9F100706010A03A480109F1A0202769F26080123456789ABCDEF9F2701809F3303E0F0C89F34034103029F3501229F3602003E9F37040F00BA209F41030010518407A0000000031010".fromHex()
        val r1 = ByteWitch.analyze(s1, tryhard = false)
        check(r1.any { it.second is ASN1InformalChain })
    }

    @Test
    fun berTlvDetection2() {
        val s1 = "50045649534157131000023100000033D44122011003400000481F".fromHex()
        val r1 = ByteWitch.analyze(s1, tryhard = false)
        check(r1.any { it.second is ASN1InformalChain })
    }

    @Test
    fun berTlvNestedDetection() {
        val s1 = "6F478409A00000005945430100A53A50086769726F636172648701019F38069F02069F1D025F2D046465656EBF0C1A9F4D02190A9F6E07028000003030009F0A080001050100000000".fromHex()
        val r1 = ByteWitch.analyze(s1, tryhard = false)
        check(r1.any { it.second is ASN1Result })
    }

}