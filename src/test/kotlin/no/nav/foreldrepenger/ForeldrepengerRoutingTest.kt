package no.nav.foreldrepenger

import io.ktor.client.request.get
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
    fun `get soknader returnerer mapped soknader fra Digisis`() = testApplication {
        application {
            configureRouting(
                hentSoknader = {
                    listOf(gyldigSoknad())
                },
            )
        }

        val response = client.get("/api/foreldrepenger/soknader")

        assertEquals(HttpStatusCode.OK, response.status)

        val soknader = json.decodeFromString<List<Soknad>>(response.bodyAsText())
        assertEquals(1, soknader.size)
        assertEquals("fp-001-happy-path", soknader.first().id)
        assertEquals("04059012377", soknader.first().fodselsnummer)
        assertEquals(Rettsforhold.BEGGE, soknader.first().rettsforhold)
        assertEquals(Dekningsgrad.HUNDRE_PROSENT, soknader.first().dekningsgrad)
    }

    @Test
    fun `post vurder returnerer vedtak for soknad`() = testApplication {
        application {
            configureRouting()
        }

        val response = client.post("/api/foreldrepenger/vurder") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(gyldigSoknad()))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val vedtak = json.decodeFromString<Vedtak>(response.bodyAsText())
        assertEquals("fp-001-happy-path", vedtak.soknadId)
        assertEquals(VedtakType.INNVILGET_FORELDREPENGER, vedtak.type)
        assertEquals(540_000, vedtak.beregningsgrunnlag?.grunnlagBelop)
        assertEquals(49, vedtak.stonadsperiode?.totalUker)
    }

    @Test
    fun `post vurder returnerer bad request ved ukjent rettsforhold`() = testApplication {
        application {
            configureRouting()
        }

        val response = client.post("/api/foreldrepenger/vurder") {
            contentType(ContentType.Application.Json)
            setBody(soknadJson(rettsforhold = "UKJENT"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `post vurder returnerer bad request ved ukjent dekningsgrad`() = testApplication {
        application {
            configureRouting()
        }

        val response = client.post("/api/foreldrepenger/vurder") {
            contentType(ContentType.Application.Json)
            setBody(soknadJson(dekningsgrad = "UKJENT"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    private fun gyldigSoknad(): Soknad =
        Soknad(
            id = "fp-001-happy-path",
            beskrivelse = "Happy path",
            fodselsnummer = "04059012377",
            erNorskBorger = true,
            termindato = "2026-08-15",
            oppgittArsinntekt = 540_000,
            inntektshistorikk = listOf(
                inntekt("2025-08"),
                inntekt("2025-09"),
                inntekt("2025-10"),
                inntekt("2025-11"),
                inntekt("2025-12"),
                inntekt("2026-01"),
                inntekt("2026-02"),
                inntekt("2026-03"),
                inntekt("2026-04"),
                inntekt("2026-05"),
            ),
            antallBarn = 1,
            rettsforhold = Rettsforhold.BEGGE,
            dekningsgrad = Dekningsgrad.HUNDRE_PROSENT,
        )

    private fun inntekt(maned: String): Inntektsregistrering =
        Inntektsregistrering(maned = maned, type = Inntektstype.ARBEID, belop = 45_000)

    private fun soknadJson(
        rettsforhold: String = "BEGGE",
        dekningsgrad: String = "HUNDRE_PROSENT",
    ): String =
        """
        {
          "id": "fp-001-happy-path",
          "beskrivelse": "Happy path",
          "fodselsnummer": "04059012377",
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
          "rettsforhold": "$rettsforhold",
          "dekningsgrad": "$dekningsgrad"
        }
        """.trimIndent()
}
