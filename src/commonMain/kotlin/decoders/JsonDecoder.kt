package decoders

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import looksLikeUtf8String

object JsonDecoder : ByteWitchDecoder {
    override val name = "Json"

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        val string = data.decodeToString().trim()
        val startValid = string.startsWith("[") || string.startsWith("{")
        val endValid = string.endsWith("]") || string.endsWith("}")

        return if(startValid && endValid) {
            val stringScore = looksLikeUtf8String(data)
            Pair(stringScore, null)
        }
        else
            Pair(0.0, null)
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val decoded : JsonElement = Json.decodeFromString(data.decodeToString())

        return transform(decoded)
    }

    private fun transform(element: JsonElement): OpackObject {
        return when (element) {
            is JsonPrimitive -> transformPrimitive(element.jsonPrimitive)
            is JsonObject -> transformObject(element.jsonObject)
            is JsonArray -> transformArray(element.jsonArray)
        }
    }

    private fun transformArray(arr: JsonArray): OPArray {
        return OPArray(arr.map { transform(it) }, Pair(-1, -1))
    }

    private fun transformObject(obj: JsonObject): OPDict {
        val map = obj.entries.associate {
            val key = OPString(it.key, Pair(-1, -1)) as OpackObject
            val value = transform(it.value)
            Pair(key, value)
        }

        return OPDict(map, Pair(-1, -1))
    }

    private fun transformPrimitive(primitive: JsonPrimitive): OpackObject {
        return when {
            primitive is JsonNull -> OPNull(-1)
            primitive.booleanOrNull != null -> if(primitive.boolean) OPTrue(-1) else OPFalse(-1)
            primitive.longOrNull != null -> OPInt(primitive.long, Pair(-1, -1))
            primitive.doubleOrNull != null -> OPReal(primitive.double, Pair(-1, -1))
            else -> OPString(primitive.content, Pair(-1, -1))
        }
    }
}