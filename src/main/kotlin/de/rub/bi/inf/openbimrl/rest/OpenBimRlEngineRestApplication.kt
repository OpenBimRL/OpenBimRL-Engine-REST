package de.rub.bi.inf.openbimrl.rest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class OpenBimRlEngineRestApplication

fun main(args: Array<String>) {
	runApplication<OpenBimRlEngineRestApplication>(*args)
}

/*
@Configuration
class JacksonConfig {

	@Bean
	fun customSerializer(): Module? {
		val module = SimpleModule()
		module.addSerializer(IfcPointerSerializer())
		return module
	}
}
*/