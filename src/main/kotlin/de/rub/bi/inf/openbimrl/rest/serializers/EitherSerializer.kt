package de.rub.bi.inf.openbimrl.rest.serializers

import arrow.core.Either
import arrow.core.right
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException

class EitherSerializer @JvmOverloads constructor(t: Class<Either<*, *>>? = null) :
    StdSerializer<Either<*, *>>(t) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(
        value: Either<*, *>, jgen: JsonGenerator, provider: SerializerProvider
    ) {
        when(val containedValue: Any? = value.leftOrNull() ?: value.right()) {
            is String -> jgen.writeString(containedValue)
            is Int -> jgen.writeNumber(containedValue)
            is Long -> jgen.writeNumber(containedValue)
            is Double -> jgen.writeNumber(containedValue)
            is Float -> jgen.writeNumber(containedValue)
            null -> jgen.writeNull()
            else -> jgen.writeObject(containedValue)
        }
    }
    override fun handledType(): Class<Either<*, *>> {
        return Either::class.java
    }
}