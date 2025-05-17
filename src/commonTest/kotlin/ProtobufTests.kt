import bitmage.fromHex
import bitmage.hex
import decoders.*
import kotlin.test.Test

class ProtobufTests {
    @Test
    fun directDecode1() {
        // this sample is decoded incorrectly by https://protobuf-decoder.netlify.app/
        val sample = "0800121c0a1163757272656e74557365724c6f63616c6512070a05656e5f444512280a127072656665727265644c616e67756167657312123a070a05656e2d44453a070a0564652d444519de3aafe67bcbc641".fromHex()
        val res = ProtobufParser.decode(sample, 0)
        val entries = res.objs

        check((entries[1]!!.first() as ProtoVarInt).value == 0L)
        val dicts = entries[2]!!

        val currentLocale = dicts[0]
        val preferred = dicts[1]

        check(currentLocale is ProtoBuf)
        check(preferred is ProtoBuf)

        check((currentLocale.objs[1]!!.first() as ProtoString).stringValue == "currentUserLocale")
        check((preferred.objs[1]!!.first() as ProtoString).stringValue == "preferredLanguages")

        val langs = preferred.objs[2]!!.first()
        check(langs is ProtoBuf)

        val langEntries = langs.objs[7]!!
        check(langEntries.size == 2)

        val strings = langEntries.map { ((it as ProtoBuf).objs[1]!!.first() as ProtoString).stringValue }
        check(strings[0] == "en-DE")
        check(strings[1] == "de-DE")
    }



    @Test
    fun detection() {
        val s1 = "0800121c0a1163757272656e74557365724c6f63616c6512070a05656e5f444512280a127072656665727265644c616e67756167657312123a070a05656e2d44453a070a0564652d444519de3aafe67bcbc641".fromHex()
        val r1 = ByteWitch.analyze(s1, tryhard = false)
        check(r1.any { it.second is ProtoBuf })
    }

}