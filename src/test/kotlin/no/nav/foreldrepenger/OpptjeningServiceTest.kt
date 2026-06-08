package no.nav.foreldrepenger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpptjeningServiceTest {
    private val service = OpptjeningService()

    @Test
    fun `oppfyller opptjening naar soker har medlemskap og nok godkjent inntekt`() {
        val soknad = soknad(
            inntekter = List(10) { index ->
                inntekt("2026-${(index + 1).toString().padStart(2, '0')}", Inntektstype.ARBEID, 45_000)
            },
        )
        val vurdering = service.vurder(soknad)

        assertTrue(vurdering.oppfylt)
        assertEquals(10, vurdering.godkjenteManeder)
        assertEquals(540_000, vurdering.annualisertInntekt)
    }

    @Test
    fun `oppfyller ikke opptjening naar soker har for faa inntektsmaneder`() {
        val soknad = soknad(
            inntekter = List(4) { index ->
                inntekt("2026-${(index + 4).toString().padStart(2, '0')}", Inntektstype.ARBEID, 50_000)
            },
        )
        val vurdering = service.vurder(soknad)

        assertFalse(vurdering.oppfylt)
        assertEquals(4, vurdering.godkjenteManeder)
    }

    @Test
    fun `oppfyller ikke opptjening naar soker ikke er norsk borger`() {
        val soknad = soknad(
            erNorskBorger = false,
            inntekter = List(10) { index ->
                inntekt("2026-${(index + 1).toString().padStart(2, '0')}", Inntektstype.ARBEID, 50_000)
            },
        )
        val vurdering = service.vurder(soknad)

        assertFalse(vurdering.oppfylt)
        assertFalse(vurdering.erNorskBorger)
    }

    @Test
    fun `regner ikke stipend fra lanekassen som godkjent inntekt`() {
        val vurdering = service.vurder(
            soknad(
                inntekter = List(10) { index ->
                    inntekt(
                        "2026-${(index + 1).toString().padStart(2, '0')}",
                        Inntektstype.STIPEND_LANEKASSEN,
                        12_000,
                    )
                },
            ),
        )

        assertFalse(vurdering.oppfylt)
        assertEquals(0, vurdering.godkjenteManeder)
        assertEquals(0, vurdering.annualisertInntekt)
    }

    private fun soknad(
        godkjenteManeder: Int = 10,
        erNorskBorger: Boolean = true,
        inntekter: List<Inntektsregistrering> = List(godkjenteManeder) { index ->
            inntekt("2026-${(index + 1).toString().padStart(2, '0')}", Inntektstype.ARBEID, 45_000)
        },
    ): Soknad =
        Soknad(
            id = "fp-test",
            beskrivelse = "Test",
            fodselsnummer = "04059012377",
            erNorskBorger = erNorskBorger,
            termindato = "2026-08-15",
            oppgittArsinntekt = 540_000,
            inntektshistorikk = inntekter,
            antallBarn = 1,
            rettsforhold = Rettsforhold.BEGGE,
            dekningsgrad = Dekningsgrad.HUNDRE_PROSENT,
        )

    private fun inntekt(maned: String, type: Inntektstype, belop: Int): Inntektsregistrering =
        Inntektsregistrering(maned = maned, type = type, belop = belop)
}
