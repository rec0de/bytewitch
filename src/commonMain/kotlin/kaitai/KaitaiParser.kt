package kaitai

import kotlinx.serialization.json.Json

object KaitaiParser {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    fun parseYaml(kaitaiYaml: String): KTStruct? {
        try {
            val jsObject = JsYaml.load(kaitaiYaml)
            val jsonString = JSON.stringify(jsObject)
            return jsonParser.decodeFromString<KTStruct>(jsonString)
        } catch (e: Exception) {
            console.error("Error parsing Kaitai Struct: $e")
        } catch (e: dynamic) {
            console.error("Error parsing Kaitai Struct: $e")
        }
        return null
    }
}

@JsModule("js-yaml")
@JsNonModule
external object JsYaml {
    fun load(yaml: String): dynamic  // needs to be dynamic, otherwise kotlin thinks all the properties (i.e. completeStruct.meta or completeStruct.seq) don't exist
    fun dump(obj: dynamic): String
}
