package de.rub.bi.inf.openbimrl.rest.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import de.rub.bi.inf.extensions.lower
import de.rub.bi.inf.extensions.upper
import java.io.IOException
import javax.media.j3d.BoundingBox

class BoundingBoxSerializer @JvmOverloads constructor(t: Class<BoundingBox>? = null) :
    StdSerializer<BoundingBox>(t) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(
        value: BoundingBox, jgen: JsonGenerator, provider: SerializerProvider
    ) {
        jgen.writeStartObject()

        jgen.writeObjectField("lower", value.lower())
        jgen.writeObjectField("upper", value.upper())

        jgen.writeEndObject()
    }

    override fun handledType(): Class<BoundingBox> {
        return BoundingBox::class.java
    }
}