package no.nav.foreldrepenger

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DigisisSoknadDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `mapper soknad fra Digisis API-format til domenemodell`() {
        val dto = json.decodeFromString<DigisisSoknadDto>(
            """
            {
              "id": "fp-001-happy-path",
              "beskrivelse": "Happy path",
              "fnr": "04059012377",
              "erNorskBorger": true,
              "termindato": "2026-08-15",
              "oppgittArsinntekt": 540000,
              "inntektshistorikk": [
                { "maned": "2025-10", "type": "ARBEID", "belop": 45000 },
                { "maned": "2025-11", "type": "SYKEPENGER", "belop": 45000 }
              ],
              "antallBarn": 1,
              "rettsforhold": "begge",
              "dekningsgrad": 100
            }
            """.trimIndent(),
        )

        val soknad = dto.toDomain()

        assertEquals("fp-001-happy-path", soknad.id)
        assertEquals("04059012377", soknad.fodselsnummer)
        assertEquals(Rettsforhold.BEGGE, soknad.rettsforhold)
        assertEquals(Dekningsgrad.HUNDRE_PROSENT, soknad.dekningsgrad)
        assertEquals(Inntektstype.ARBEID, soknad.inntektshistorikk[0].type)
        assertEquals(Inntektstype.SYKEPENGER, soknad.inntektshistorikk[1].type)
    }

    @Test
    fun `mapper alle rettsforhold og dekningsgrader fra Digisis`() {
        val mappinger = listOf(
            MappingCase("begge", 100, Rettsforhold.BEGGE, Dekningsgrad.HUNDRE_PROSENT),
            MappingCase("begge", 80, Rettsforhold.BEGGE, Dekningsgrad.ATTI_PROSENT),
            MappingCase("kun-mor", 100, Rettsforhold.KUN_MOR, Dekningsgrad.HUNDRE_PROSENT),
            MappingCase("kun-far", 80, Rettsforhold.KUN_FAR, Dekningsgrad.ATTI_PROSENT),
        )

        mappinger.forEach { mapping ->
            val soknad = dto(
                rettsforhold = mapping.rettsforholdFraApi,
                dekningsgrad = mapping.dekningsgradFraApi,
            ).toDomain()

            assertEquals(mapping.forventetRettsforhold, soknad.rettsforhold)
            assertEquals(mapping.forventetDekningsgrad, soknad.dekningsgrad)
        }
    }

    @Test
    fun `feiler tydelig ved ukjent rettsforhold`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            dto(rettsforhold = "ukjent").toDomain()
        }

        assertEquals("Ukjent rettsforhold fra DigiSIS: ukjent", feil.message)
    }

    @Test
    fun `feiler tydelig ved ukjent dekningsgrad`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            dto(dekningsgrad = 60).toDomain()
        }

        assertEquals("Ukjent dekningsgrad fra DigiSIS: 60", feil.message)
    }

    @Test
    fun `feiler tydelig ved ukjent inntektstype`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            dto(
                inntektshistorikk = listOf(
                    DigisisInntektsregistreringDto("2025-10", "UKJENT", 45_000),
                ),
            ).toDomain()
        }

        assertEquals("Ukjent inntektstype fra DigiSIS: UKJENT", feil.message)
    }

    private fun dto(
        rettsforhold: String = "begge",
        dekningsgrad: Int = 100,
        inntektshistorikk: List<DigisisInntektsregistreringDto> = listOf(
            DigisisInntektsregistreringDto("2025-10", "ARBEID", 45_000),
        ),
    ): DigisisSoknadDto =
        DigisisSoknadDto(
            id = "fp-test",
            beskrivelse = "Test",
            fodselsnummer = "04059012377",
            erNorskBorger = true,
            termindato = "2026-08-15",
            oppgittArsinntekt = 540_000,
            inntektshistorikk = inntektshistorikk,
            antallBarn = 1,
            rettsforhold = rettsforhold,
            dekningsgrad = dekningsgrad,
        )

    private data class MappingCase(
        val rettsforholdFraApi: String,
        val dekningsgradFraApi: Int,
        val forventetRettsforhold: Rettsforhold,
        val forventetDekningsgrad: Dekningsgrad,
    )
}
