package de.rub.bi.inf.openbimrl.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenBimRlEngineRestApplicationTests {

	@Autowired
	private lateinit var mockMvc: MockMvc

	private lateinit var modelId: String
	private val graphContent: String = Files.readString(
		Paths.get("../OpenBimRL-Engine/src/test/resources/show_distances.openbimrl"),
	)
	private val ifcPath = Paths.get("../OpenBimRL-Engine/src/test/resources/pathfinding_minimal.ifc")

	@BeforeAll
	fun uploadModel() {
		require(Files.exists(ifcPath)) { "Missing test IFC at $ifcPath" }

		val modelResponse = mockMvc.multipart("/model") {
			file(
				MockMultipartFile(
					"file",
					"pathfinding_minimal.ifc",
					"application/octet-stream",
					Files.readAllBytes(ifcPath),
				),
			)
		}.andReturn().response.contentAsString

		modelId = Regex(""""content"\s*:\s*"([^"]+)"""")
			.find(modelResponse)?.groupValues?.get(1)
			?: error("Could not parse model upload response: $modelResponse")
	}

	@Test
	fun contextLoads() {
	}

	@Test
	fun `DELETE model removes uploaded IFC`() {
		val uploadResponse = mockMvc.multipart("/model") {
			file(
				MockMultipartFile(
					"file",
					"to_delete.ifc",
					"application/octet-stream",
					Files.readAllBytes(ifcPath),
				),
			)
		}.andReturn().response.contentAsString

		val id = Regex(""""content"\s*:\s*"([^"]+)"""")
			.find(uploadResponse)?.groupValues?.get(1)
			?: error("Could not parse model upload response: $uploadResponse")

		val deleteResponse = mockMvc.delete("/model/$id").andReturn().response
		assertEquals(200, deleteResponse.status)
		assertTrue(deleteResponse.contentAsString.contains(""""content":true"""))

		val getResponse = mockMvc.get("/model/$id").andReturn().response
		assertEquals(404, getResponse.status)

		val missingResponse = mockMvc.delete("/model/${UUID.randomUUID()}").andReturn().response
		assertEquals(404, missingResponse.status)
		assertTrue(missingResponse.contentAsString.contains("Model Not Found"))
	}

	@Test
	fun `POST check then GET json and visuals roundtrip`() {
		val graphId = mockMvc.multipart("/graph") {
			param("file", graphContent)
		}.andReturn().response.contentAsString.let { body ->
			Regex(""""content"\s*:\s*"([^"]+)"""")
				.find(body)?.groupValues?.get(1)
				?: error("Could not parse graph upload response: $body")
		}

		val checkBody = """{"graphIds":["$graphId"]}"""
		val checkResponse = mockMvc.post("/check/$modelId") {
			contentType = MediaType.APPLICATION_JSON
			content = checkBody
		}.andReturn().response

		assertEquals(200, checkResponse.status)
		val resultId = Regex(""""resultId"\s*:\s*"([^"]+)"""")
			.find(checkResponse.contentAsString)?.groupValues?.get(1)
			?: error("Could not parse check response: ${checkResponse.contentAsString}")

		val jsonResponse = mockMvc.get("/results/$resultId/json").andReturn().response
		assertEquals(200, jsonResponse.status)
		assertTrue(jsonResponse.contentAsString.contains("calculateDistancesFromElement"))

		val visualsResponse = mockMvc.get("/results/$resultId/visuals").andReturn().response
		assertEquals(200, visualsResponse.status)
		val glb = visualsResponse.contentAsByteArray
		assertTrue(glb.size > 12)
		assertEquals('g'.code.toByte(), glb[0])
		assertNotNull(glb)
	}
}
