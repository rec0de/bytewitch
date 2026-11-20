package decoders.apple

import bitmage.ByteOrder
import bitmage.hex
import bitmage.untilIndex
import BWString
import ByteWitchDecoder
import ByteWitchResult
import decoders.QuickDirtyDecoder

class MachO : QuickDirtyDecoder() {

    companion object : ByteWitchDecoder {
        override val name = "MachO"
        private const val HEADER_LEN = 32
        private val magics = setOf("feedface", "feedfacf", "cffaedfe", "cefaedfe")

        // mappings from https://en.wikipedia.org/wiki/Mach-O
        private val cpuTypes: Map<Int, String> = mapOf(
            0x00000001 to "VAX",
            0x00000002 to "ROMP",
            0x00000004 to "NS32032",
            0x00000005 to "NS32332",
            0x00000006 to "MC680x0",
            0x00000007 to "x86",
            0x00000008 to "MIPS",
            0x00000009 to "NS32352",
            0x0000000B to "HP-PA",
            0x0000000C to "ARM",
            0x0000000D to "MC88000",
            0x0000000E to "SPARC",
            0x0000000F to "i860 (big-endian)",
            0x00000010 to "i860 (little-endian) / DEC Alpha",
            0x00000011 to "RS/6000",
            0x00000012 to "PowerPC / MC98000",
            0x00000018 to "RISC-V"
        )

        private val armSubTypes: Map<Int, String> = mapOf(
            0x00000000 to "All ARM processors",
            0x00000001 to "ARM-A500 ARCH or newer",
            0x00000002 to "ARM-A500 or newer",
            0x00000003 to "ARM-A440 or newer",
            0x00000004 to "ARM-M4 or newer",
            0x00000005 to "ARM-V4T or newer",
            0x00000006 to "ARM-V6 or newer",
            0x00000007 to "ARM-V5TEJ or newer",
            0x00000008 to "ARM-XSCALE or newer",
            0x00000009 to "ARM-V7 or newer",
            0x0000000A to "ARM-V7F (Cortex A9) or newer",
            0x0000000B to "ARM-V7S (Swift) or newer",
            0x0000000C to "ARM-V7K (Kirkwood40) or newer",
            0x0000000D to "ARM-V8 or newer",
            0x0000000E to "ARM-V6M or newer",
            0x0000000F to "ARM-V7M or newer",
            0x00000010 to "ARM-V7EM or newer"
        )

        private val x86SubTypes: Map<Int, String> = mapOf(
            0x00000003 to "All x86 processors",
            0x00000004 to "486 or newer",
            0x00000084 to "486SX or newer",
            0x00000056 to "Pentium M5 or newer",
            0x00000067 to "Celeron or newer",
            0x00000077 to "Celeron Mobile",
            0x00000008 to "Pentium 3 or newer",
            0x00000018 to "Pentium 3-M or newer",
            0x00000028 to "Pentium 3-XEON or newer",
            0x0000000A to "Pentium-4 or newer",
            0x0000000B to "Itanium or newer",
            0x0000001B to "Itanium-2 or newer",
            0x0000000C to "XEON or newer",
            0x0000001C to "XEON-MP or newer"
        )

        val fileTypes: Map<Int, String> = mapOf(
            0x00000001 to "Relocatable object file",
            0x00000002 to "Demand paged executable file",
            0x00000003 to "Fixed VM shared library file",
            0x00000004 to "Core file",
            0x00000005 to "Preloaded executable file",
            0x00000006 to "Dynamically bound shared library file",
            0x00000007 to "Dynamic link editor",
            0x00000008 to "Dynamically bound bundle file",
            0x00000009 to "Shared library stub for static linking only, no section contents",
            0x0000000A to "Companion file with only debug sections",
            0x0000000B to "x86_64 kexts",
            0x0000000C to "File composed of other Mach-Os to be run in the same userspace sharing a single linkedit"
        )

        val loadCommandNames: Map<UInt, String> = mapOf(
            0x00000001u to "Segment",
            0x00000002u to "Symbol Table",
            0x00000003u to "Symbol Segment",
            0x00000004u to "Thread",
            0x00000005u to "Unix Thread",
            0x00000006u to "Load Fixed VM Library",
            0x00000007u to "Fixed VM Library ID",
            0x00000008u to "Identity",
            0x00000009u to "Fixed VM File",
            0x0000000Au to "Pre-Page",
            0x0000000Bu to "Dynamic Symbol Table",
            0x0000000Cu to "Load Dynamic Library",
            0x0000000Du to "Dynamic Library ID",
            0x0000000Eu to "Load Dynamic Linker",
            0x0000000Fu to "Dynamic Linker ID",
            0x00000010u to "Prebound Dynamic Library",
            0x00000011u to "Routines",
            0x00000012u to "Sub-Framework",
            0x00000013u to "Sub-Umbrella",
            0x00000014u to "Sub-Client",
            0x00000015u to "Sub-Library",
            0x00000016u to "Two-Level Namespace Hints",
            0x00000017u to "Prebind Checksum",
            0x80000018u to "Load Weak Dynamic Library",
            0x00000019u to "Segment (64-bit)",
            0x0000001Au to "Routines (64-bit)",
            0x0000001Bu to "UUID",
            0x8000001Cu to "Run Path",
            0x0000001Du to "Code Signature",
            0x0000001Eu to "Segment Split Info",
            0x8000001Fu to "Re-export Dynamic Library",
            0x00000020u to "Lazy Load Dynamic Library",
            0x00000021u to "Encryption Info",
            0x00000022u to "Dyld Info",
            0x80000022u to "Dyld Info (Only)",
            0x80000023u to "Load Upward Dynamic Library",
            0x00000024u to "Minimum OS Version (macOS)",
            0x00000025u to "Minimum OS Version (iOS)",
            0x00000026u to "Function Starts",
            0x00000027u to "Dyld Environment",
            0x80000028u to "Entry Point (Main)",
            0x00000029u to "Data in Code",
            0x0000002Au to "Source Version",
            0x0000002Bu to "Dynamic Library Code Signing DRs",
            0x0000002Cu to "Encryption Info (64-bit)",
            0x0000002Du to "Linker Option",
            0x0000002Eu to "Linker Optimization Hint",
            0x0000002Fu to "Minimum OS Version (tvOS)",
            0x00000030u to "Minimum OS Version (watchOS)",
            0x00000031u to "Note",
            0x00000032u to "Build Version",
            0x80000033u to "Dyld Exports Trie",
            0x80000034u to "Dyld Chained Fixups",
            0x80000035u to "Fileset Entry",
            0x00000036u to "Atom Info",
        )

        override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
            if(data.size < HEADER_LEN)
                return Pair(0.0, null)

            val magic = data.untilIndex(4).reversed().toByteArray().hex()
            return if(magic in magics)
                Pair(1.0, null)
            else
                Pair(0.0, null)
        }

        override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean) = MachO().decode(data, sourceOffset)
    }


    private var byteOrder = ByteOrder.LITTLE

    fun decode(data: ByteArray, sourceOffset: Int): ByteWitchResult {
        start(data, sourceOffset, name)

        var is64b: Boolean = false
        processBytes(4){ magic ->
            val hexmagic = magic.hex()
            check(hexmagic in magics){ "unexpected MachO magic 0x$hexmagic" }
            byteOrder = if(hexmagic.endsWith("edfe"))
                ByteOrder.LITTLE
            else
                ByteOrder.BIG

            is64b = (hexmagic == "feedfacf" || hexmagic == "cffaedfe")
            val is32or64 = if(is64b) "64" else "32"
            "MachO magic ($is32or64 bit)"
        }

        var cpu: Int = 0
        processUInt(4, byteOrder){ cpuType ->
            val abi = cpuType and 0xff000000U
            cpu = (cpuType and 0x00ffffffU).toInt()
            check(abi == 0U || (abi != 0U) == is64b) // 64b ABI should be combined with a 64b binary
            "CPU: ${cpuTypes.getOrElse(cpu){ "Unknown($cpu)" }}"
        }

       processUInt(4, byteOrder){ cpuSubtype ->
           val subtype = cpuSubtype and 0x00ffffffU // top byte has 64b flag again or something similar? unclear
           val subTypeString = when(cpu) {
               0x07 -> x86SubTypes.getOrElse(subtype.toInt()){ "Unknown($subtype)" }
               0x0c -> armSubTypes.getOrElse(subtype.toInt()){ "Unknown($subtype)" }
               else -> "Unknown($subtype)"
           }
           "CPU Subtype: $subTypeString"
        }

        processInt(4, byteOrder, "Filetype", Format.LOOKUP, lookupMap = fileTypes)

        val numLoads = processInt(4, byteOrder, "Load commands", Format.DECIMAL)

        processInt(4, byteOrder, "Load commands size", Format.BYTES)
        processInt(4, byteOrder, "Flags", Format.HEX)

        if(is64b)
            processUInt(4, byteOrder, "Reserved", Format.HEX)

        (0 until numLoads).forEach { _ ->
            readLoadCommand()
        }

        return finish()
    }

    private fun readLoadCommand() {
        enterSubstruct()

        val type = processUInt(4, byteOrder, "", Format.LOOKUP, loadCommandNames)
        val size = processUInt(4, byteOrder, "Size", Format.BYTES)

        if(type == 0x19U) {
            readLoadSegment()
        }
        else {
            processBytes(size.toInt() - 8, "") // size includes 'header'
        }

        leaveSubstruct()
    }

    private fun readLoadSegment() {
        enterSubstruct()

        processString(16, "Name:")
        processULong(8, byteOrder, "Address", Format.HEX)
        processULong(8, byteOrder, "Address size", Format.DECIMAL)
        processULong(8, byteOrder, "Offset in image", Format.HEX)
        processULong(8, byteOrder, "Segment size", Format.BYTES)

        processUInt(4, byteOrder){ maxProtections ->
            val maxR = if((maxProtections.toInt() and 0x01) != 0) "R" else ""
            val maxW = if((maxProtections.toInt() and 0x02) != 0) "W" else ""
            val maxX = if((maxProtections.toInt() and 0x04) != 0) "X" else ""
            "Maximum protections: $maxR$maxW$maxX"
        }

        processUInt(4, byteOrder){ initProtections ->
            val iniR = if((initProtections.toInt() and 0x01) != 0) "R" else ""
            val iniW = if((initProtections.toInt() and 0x02) != 0) "W" else ""
            val iniX = if((initProtections.toInt() and 0x04) != 0) "X" else ""
            "Initial protections: $iniR$iniW$iniX"
        }

        val numSections = processUInt(4, byteOrder, "Number of sections", Format.DECIMAL).toInt()
        processUInt(4, byteOrder, "Flags", Format.HEX)

        (0 until numSections).forEach { _ ->
            readSection()
        }

        leaveSubstruct()
    }

    private fun readSection() {
        enterSubstruct()

        processString(16, "Section")

        // 16B segment name field is duplicated from surrounding segment, ignore
        parseOffset += 16

        processULong(8, byteOrder, "Address", Format.HEX)
        processULong(8, byteOrder, "Size", Format.BYTES)
        processUInt(4, byteOrder, "File offset", Format.HEX)

        processBytes(28, "Unparsed")

        leaveSubstruct()
    }
}