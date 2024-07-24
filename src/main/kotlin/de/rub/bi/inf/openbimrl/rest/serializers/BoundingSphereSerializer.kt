package de.rub.bi.inf.openbimrl.rest.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException
import javax.media.j3d.BoundingSphere
import javax.vecmath.Point3d

class BoundingSphereSerializer @JvmOverloads constructor(t: Class<BoundingSphere>? = null) :
    StdSerializer<BoundingSphere>(t) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(
        value: BoundingSphere, jgen: JsonGenerator, provider: SerializerProvider
    ) {
        jgen.writeStartObject()
        jgen.writeObjectField("center", Point3d().apply { value.getCenter(this) })
        jgen.writeNumberField("radius", value.radius)
        jgen.writeEndObject()
    }

    override fun handledType(): Class<BoundingSphere> {
        return BoundingSphere::class.java
    }
}