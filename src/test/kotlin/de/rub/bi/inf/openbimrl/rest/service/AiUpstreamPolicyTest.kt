package de.rub.bi.inf.openbimrl.rest.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AiUpstreamPolicyTest {
    private val policy = AiUpstreamPolicy(
        "https://chat.kiconnect.nrw/api/v1",
        listOf("chat.kiconnect.nrw"),
    )

    @Test
    fun `uses the default KI connect root`() {
        assertEquals(
            "https://chat.kiconnect.nrw/api/v1/models",
            policy.resolve(null, "models").toString(),
        )
        assertEquals(
            "https://chat.kiconnect.nrw/api/v1/chat/completions",
            policy.resolve("  ", "chat/completions").toString(),
        )
    }

    @Test
    fun `accepts the same host with a trailing slash`() {
        assertEquals(
            "https://chat.kiconnect.nrw/api/v1/models",
            policy.resolve("https://chat.kiconnect.nrw/api/v1/", "models").toString(),
        )
    }

    @Test
    fun `rejects other hosts, insecure urls, and unexpected paths`() {
        assertThrows(AiUpstreamRejected::class.java) {
            policy.resolve("https://example.com/api/v1", "models")
        }
        assertThrows(AiUpstreamRejected::class.java) {
            policy.resolve("http://chat.kiconnect.nrw/api/v1", "models")
        }
        assertThrows(AiUpstreamRejected::class.java) {
            policy.resolve("https://user:secret@chat.kiconnect.nrw/api/v1", "models")
        }
        assertThrows(AiUpstreamRejected::class.java) {
            policy.resolve(null, "../admin")
        }
    }
}
