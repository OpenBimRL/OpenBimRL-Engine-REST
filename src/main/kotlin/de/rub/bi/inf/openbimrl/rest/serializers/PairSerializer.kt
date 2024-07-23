package de.rub.bi.inf.openbimrl.rest.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException

class PairSerializer @JvmOverloads constructor(t: Class<Pair<*, *>>? = null) :
    StdSerializer<Pair<*, *>>(t) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(
        value: Pair<*, *>, jgen: JsonGenerator, provider: SerializerProvider
    ) {
        jgen.writeStartObject()

        value.toList().forEach {
            when (it) {
                is String -> jgen.writeStringField(it.javaClass.simpleName, it)
                is Int -> jgen.writeNumberField(it.javaClass.simpleName, it)
                is Long -> jgen.writeNumberField(it.javaClass.simpleName, it)
                is Double -> jgen.writeNumberField(it.javaClass.simpleName, it)
                is Float -> jgen.writeNumberField(it.javaClass.simpleName, it)
                is Map<*, *> -> jgen.writeObjectField("Map", it)
                null -> jgen.writeNullField("null")
                else -> jgen.writeObjectField(it.javaClass.simpleName, it)
            }
        }
        jgen.writeEndObject()
    }

    override fun handledType(): Class<Pair<*, *>> {
        return Pair::class.java
    }
}