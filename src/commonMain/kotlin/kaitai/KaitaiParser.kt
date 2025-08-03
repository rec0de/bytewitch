package kaitai

import decoders.JsYaml
import kotlinx.serialization.json.Json

object KaitaiParser {
    private val jsonParser = Json {
        //ignoreUnknownKeys = true
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
