package de.rub.bi.inf.openbimrl.rest.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import de.rub.bi.inf.nativelib.IfcPointer
import java.io.IOException

class IfcPointerSerializer @JvmOverloads constructor(t: Class<IfcPointer>? = null) :
    StdSerializer<IfcPointer>(t) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(
        value: IfcPointer, jgen: JsonGenerator, provider: SerializerProvider
    ) {
        jgen.writeStartObject()
        jgen.writeStringField("type", value.type)
        jgen.writeStringField("guid", value.guid)
        jgen.writeObjectField("properties", value.properties)
        jgen.writeObjectField("quantities", value.quantities)
        jgen.writeEndObject()
    }
    override fun handledType(): Class<IfcPointer> {
        return IfcPointer::class.java
    }
}