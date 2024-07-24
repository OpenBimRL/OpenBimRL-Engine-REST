package de.rub.bi.inf.openbimrl.rest

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.module.SimpleModule
import de.rub.bi.inf.openbimrl.rest.serializers.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@SpringBootApplication
class OpenBimRlEngineRestApplication

fun main(args: Array<String>) {
	runApplication<OpenBimRlEngineRestApplication>(*args)
}


@Configuration
class JacksonConfig {

	@Bean
	fun customSerializer(): Module? {
		val module = SimpleModule()
		module.addSerializer(IfcPointerSerializer())
		module.addSerializer(EitherSerializer())
		module.addSerializer(PairSerializer())
		module.addSerializer(BoundingBoxSerializer())
		module.addSerializer(BoundingSphereSerializer())
		return module
	}
}
