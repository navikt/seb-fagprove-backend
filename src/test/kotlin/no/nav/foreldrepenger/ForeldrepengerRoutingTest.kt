package no.nav.foreldrepenger

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import no.nav.config.configureRouting
import kotlin.test.Test
import kotlin.test.assertEquals

class ForeldrepengerRoutingTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `post vurder returnerer vedtak for soknad`() = testApplication {
        application {
            configureRouting()
        }

        val response = client.post("/api/foreldrepenger/vurder") {
            contentType(ContentType.Application.Json)
            setBody(gyldigSoknadJson())
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val vedtak = json.decodeFromString<Vedtak>(response.bodyAsText())
        assertEquals("fp-001-happy-path", vedtak.soknadId)
        assertEquals(VedtakType.INNVILGET_FORELDREPENGER, vedtak.type)
    }

    private fun gyldigSoknadJson(): String =
        """
        {
          "id": "fp-001-happy-path",
          "beskrivelse": "Happy path",
          "fnr": "04059012377",
          "erNorskBorger": true,
          "termindato": "2026-08-15",
          "oppgittArsinntekt": 540000,
          "inntektshistorikk": [
            { "maned": "2025-08", "type": "ARBEID", "belop": 45000 },
            { "maned": "2025-09", "type": "ARBEID", "belop": 45000 },
            { "maned": "2025-10", "type": "ARBEID", "belop": 45000 },
            { "maned": "2025-11", "type": "ARBEID", "belop": 45000 },
            { "maned": "2025-12", "type": "ARBEID", "belop": 45000 },
            { "maned": "2026-01", "type": "ARBEID", "belop": 45000 },
            { "maned": "2026-02", "type": "ARBEID", "belop": 45000 },
            { "maned": "2026-03", "type": "ARBEID", "belop": 45000 },
            { "maned": "2026-04", "type": "ARBEID", "belop": 45000 },
            { "maned": "2026-05", "type": "ARBEID", "belop": 45000 }
          ],
          "antallBarn": 1,
          "rettsforhold": "begge",
          "dekningsgrad": 100
        }
        """.trimIndent()
}
