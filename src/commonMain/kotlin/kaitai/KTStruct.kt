package kaitai

import bitmage.ByteOrder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class KTStruct(
    val meta: KTMeta? = null,

    val doc: String? = null,
    @Serializable(with = StringOrArraySerializer::class)
    @SerialName("doc-ref")
    val docRef: List<String>? = null,

    val seq: List<KTSeq> = emptyList(),

    val types: Map<String, KTStruct> = emptyMap(),

    val instances: Map<String, KTInstance> = emptyMap(),
)

@Serializable
data class KTMeta(
    val id: String? = null,
    val title: String? = null,
    val endian: KTEndian? = null,

    val doc: String? = null,
    @Serializable(with = StringOrArraySerializer::class)
    @SerialName("doc-ref")
    val docRef: List<String>? = null,
)

@Serializable
data class KTSeq(
    val id: String,

    @Serializable(with = KTTypeSerializer::class)
    val type: KTType? = null,

    @Serializable(with = StringOrIntSerializer::class)
    val size: StringOrInt? = null,
    @SerialName("size-eos")
    val sizeEos: Boolean? = null,

    @Serializable(with = StringOrArraySerializer::class)
    val contents: List<String>? = null,

    val doc: String? = null,
    @Serializable(with = StringOrArraySerializer::class)
    @SerialName("doc-ref")
    val docRef: List<String>? = null,

    val repeat: KTRepeat? = null,
    @SerialName("repeat-expr")
    val repeatExpr: String? = null,
    @SerialName("repeat-until")
    val repeatUntil: String? = null,

    val terminator: Int? = null,

    @Serializable(with = KTValidSerializer::class)
    val valid: KTValid? = null,

    val value: String? = null,
)

@Serializable
data class KTInstance(
    val id: String? = null,

    @Serializable(with = KTTypeSerializer::class)
    val type: KTType? = null,

    @Serializable(with = StringOrIntSerializer::class)
    val size: StringOrInt? = null,
    @SerialName("size-eos")
    val sizeEos: Boolean? = null,

    @Serializable(with = StringOrArraySerializer::class)
    val contents: List<String>? = null,

    val doc: String? = null,
    @Serializable(with = StringOrArraySerializer::class)
    @SerialName("doc-ref")
    val docRef: List<String>? = null,

    val repeat: KTRepeat? = null,
    @SerialName("repeat-expr")
    val repeatExpr: String? = null,
    @SerialName("repeat-until")
    val repeatUntil: String? = null,

    val terminator: Int? = null,

    @Serializable(with = KTValidSerializer::class)
    val valid: KTValid? = null,

    val value: String? = null,
)

@Serializable
sealed class KTType {
    @Serializable
    data class Primitive(val type: String) : KTType() {
        override fun toString(): String = type
    }

    @Serializable
    data class Switch(
        @SerialName("switch-on")
        val switchOn: String,
        val cases: Map<String, String>,
    ) : KTType()
}

@Serializable
enum class KTEndian {
    @SerialName("le")
    LE,

    @SerialName("be")
    BE,
}

@Serializable
enum class KTRepeat {
    @SerialName("eos")
    EOS,

    @SerialName("expr")
    EXPR,

    @SerialName("until")
    UNTIL,
}

@Serializable
data class KTValid(
    val eq: String? = null,
    val min: String? = null,
    val max: String? = null,
    @SerialName("any-of")
    val anyOf: List<String>? = null,
    val expr: String? = null,
)

@Serializable
sealed class StringOrInt {
    @Serializable
    data class StringValue(val value: String) : StringOrInt() {
        override fun toString(): String = value
    }

    @Serializable
    data class IntValue(val value: Int) : StringOrInt() {
        override fun toString(): String = value.toString()
    }
}


// Custom serializers
object StringOrArraySerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrArray", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as JsonDecoder

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> element.map { it.jsonPrimitive.content }
            is JsonPrimitive -> listOf(element.content)
            else -> throw SerializationException("Expected string or array")
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        if (value.size == 1) {
            encoder.encodeString(value.first())
        } else {
            encoder.encodeSerializableValue(ListSerializer(String.serializer()), value)
        }
    }
}

object KTTypeSerializer : KSerializer<KTType> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("KTType") {
        element<String>("switch-on", isOptional = true)
        element<Map<String, String>>("cases", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): KTType {
        val jsonDecoder = decoder as JsonDecoder

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                KTType.Primitive(element.jsonPrimitive.content)
            }

            is JsonObject -> {
                val switchOn = element["switch-on"]?.jsonPrimitive?.content
                val cases = element["cases"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()

                if (switchOn != null && cases.isNotEmpty()) {
                    KTType.Switch(switchOn, cases)
                } else {
                    throw SerializationException("Invalid KTType format: missing 'switch-on' or 'cases'")
                }
            }

            else -> throw SerializationException("Invalid KTType format")
        }
    }

    override fun serialize(encoder: Encoder, value: KTType) {
        when (value) {
            is KTType.Primitive -> encoder.encodeString(value.type)
            is KTType.Switch -> encoder.encodeSerializableValue(KTType.Switch.serializer(), value)
        }
    }
}

object KTValidSerializer : KSerializer<KTValid> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("KTValid") {
        element<String?>("eq")
        element<String?>("min")
        element<String?>("max")
        element<List<String>?>("any-of")
        element<String?>("expr")
    }

    override fun deserialize(decoder: Decoder): KTValid {
        val jsonDecoder = decoder as JsonDecoder

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonObject -> Json.decodeFromJsonElement(element)
            // If it's a primitive, we assume it's a single string value for `eq`
            is JsonPrimitive -> KTValid(eq = element.content)
            else -> throw SerializationException("Expected object or primitive for KTValid")
        }
    }

    override fun serialize(encoder: Encoder, value: KTValid) {
        encoder.encodeSerializableValue(KTValid.serializer(), value)
    }
}

object StringOrIntSerializer : KSerializer<StringOrInt> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): StringOrInt {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()

        return when {
            element is JsonPrimitive && element.isString ->
                StringOrInt.StringValue(element.content)

            element is JsonPrimitive && !element.isString ->
                StringOrInt.IntValue(element.int)

            else -> throw SerializationException("Expected string or int")
        }
    }

    override fun serialize(encoder: Encoder, value: StringOrInt) {
        when (value) {
            is StringOrInt.StringValue -> encoder.encodeString(value.value)
            is StringOrInt.IntValue -> encoder.encodeInt(value.value)
        }
    }
}


// Extension functions
fun KTEndian.toByteOrder(): ByteOrder {
    return when (this) {
        KTEndian.LE -> ByteOrder.LITTLE
        KTEndian.BE -> ByteOrder.BIG
    }
}
