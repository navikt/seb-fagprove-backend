package no.nav.foreldrepenger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForeldrepengerServiceTest {
    private val service = ForeldrepengerService()

    @Test
    fun `lager innvilget vedtak med beregningsgrunnlag stonadsperiode og kvoter`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-001-happy-path",
                inntekter = List(10) { index ->
                    inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 45_000)
                },
            ),
        )

        assertEquals(VedtakType.INNVILGET_FORELDREPENGER, vedtak.type)
        assertEquals(540_000, vedtak.beregningsgrunnlag?.grunnlagBelop)
        assertEquals(49, vedtak.stonadsperiode?.totalUker)
        assertEquals(
            Kvoter(
                modrekvote = 15,
                fedrekvote = 15,
                fellesperiode = 16,
                forhandskvoteMor = 3,
                flerbarnsbonus = 0,
                totalUker = 49,
            ),
            vedtak.kvoter,
        )
    }

    @Test
    fun `gir engangsstonad naar soker er norsk borger men ikke oppfyller opptjening`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-003-for-fa-mnd",
                oppgittArsinntekt = 240_000,
                inntekter = List(4) { index ->
                    inntekt("2026-${(index + 4).toString().padStart(2, '0')}", 50_000)
                },
            ),
        )

        assertEquals(VedtakType.ENGANGSSTONAD, vedtak.type)
        assertNull(vedtak.beregningsgrunnlag)
        assertNull(vedtak.stonadsperiode)
        assertNull(vedtak.kvoter)
    }

    @Test
    fun `gir avslag naar soker ikke er norsk borger`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-002-ikke-medlem",
                erNorskBorger = false,
                inntekter = List(10) { index ->
                    inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 50_000)
                },
            ),
        )

        assertEquals(VedtakType.AVSLAG, vedtak.type)
    }

    @Test
    fun `gir manuell vurdering naar tre-maneders snitt avviker mer enn 25 prosent`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-005-manuell-vurdering",
                oppgittArsinntekt = 600_000,
                inntekter = listOf(
                    inntekt("2025-10", 40_000),
                    inntekt("2025-11", 40_000),
                    inntekt("2025-12", 40_000),
                    inntekt("2026-01", 40_000),
                    inntekt("2026-02", 40_000),
                    inntekt("2026-03", 40_000),
                    inntekt("2026-04", 40_000),
                    inntekt("2026-05", 80_000),
                    inntekt("2026-06", 80_000),
                    inntekt("2026-07", 80_000),
                ),
            ),
        )

        assertEquals(VedtakType.MANUELL_VURDERING, vedtak.type)
        assertEquals(960_000, vedtak.beregningsgrunnlag?.arssats)
        assertEquals(60.0, vedtak.beregningsgrunnlag?.avvikProsent)
        assertTrue(assertNotNull(vedtak.beregningsgrunnlag).kreverManuellVurdering)
    }

    @Test
    fun `kutter beregningsgrunnlag ved 6G`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-006-6G-tak",
                oppgittArsinntekt = 1_200_000,
                dekningsgrad = Dekningsgrad.ATTI_PROSENT,
                inntekter = List(10) { index ->
                    inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 100_000)
                },
            ),
        )

        assertEquals(VedtakType.INNVILGET_FORELDREPENGER, vedtak.type)
        assertEquals(1_200_000, vedtak.beregningsgrunnlag?.arssats)
        assertEquals(780_960, vedtak.beregningsgrunnlag?.grunnlagBelop)
        assertEquals(61, vedtak.stonadsperiode?.totalUker)
    }

    @Test
    fun `fordeler hele perioden til far naar bare far har rett`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-007-kun-far",
                rettsforhold = Rettsforhold.KUN_FAR,
                inntekter = List(10) { index ->
                    inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 50_000)
                },
            ),
        )

        assertEquals(40, vedtak.stonadsperiode?.totalUker)
        assertEquals(
            Kvoter(
                modrekvote = 0,
                fedrekvote = 40,
                fellesperiode = 0,
                forhandskvoteMor = 0,
                flerbarnsbonus = 0,
                totalUker = 40,
            ),
            vedtak.kvoter,
        )
    }

    @Test
    fun `beregner flerbarnsbonus for tvillinger`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-008-tvillinger",
                antallBarn = 2,
                inntekter = List(10) { index ->
                    inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 45_000)
                },
            ),
        )

        assertEquals(66, vedtak.stonadsperiode?.totalUker)
        assertEquals(17, vedtak.kvoter?.flerbarnsbonus)
        assertEquals(16, vedtak.kvoter?.fellesperiode)
    }

    @Test
    fun `beregner maksimal periode for trillinger med 80 prosent dekningsgrad`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-010-trillinger-80",
                antallBarn = 3,
                dekningsgrad = Dekningsgrad.ATTI_PROSENT,
                oppgittArsinntekt = 720_000,
                inntekter = List(10) { index ->
                    inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 60_000)
                },
            ),
        )

        assertEquals(118, vedtak.stonadsperiode?.totalUker)
        assertEquals(57, vedtak.kvoter?.flerbarnsbonus)
        assertEquals(20, vedtak.kvoter?.fellesperiode)
    }

    @Test
    fun `fordeler perioden til mor naar bare mor har rett`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-011-kun-mor-80",
                rettsforhold = Rettsforhold.KUN_MOR,
                dekningsgrad = Dekningsgrad.ATTI_PROSENT,
                oppgittArsinntekt = 480_000,
                inntekter = List(10) { index ->
                    inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 40_000)
                },
            ),
        )

        assertEquals(61, vedtak.stonadsperiode?.totalUker)
        assertEquals(58, vedtak.kvoter?.modrekvote)
        assertEquals(3, vedtak.kvoter?.forhandskvoteMor)
    }

    @Test
    fun `hopper over avvikssjekk naar oppgitt arsinntekt er null`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-012-oppgitt-arsinntekt-null",
                oppgittArsinntekt = 0,
                antallBarn = 2,
                dekningsgrad = Dekningsgrad.ATTI_PROSENT,
                inntekter = List(10) { index ->
                    inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 35_000)
                },
            ),
        )

        assertEquals(VedtakType.INNVILGET_FORELDREPENGER, vedtak.type)
        assertNull(vedtak.beregningsgrunnlag?.avvikProsent)
        assertEquals(420_000, vedtak.beregningsgrunnlag?.grunnlagBelop)
        assertEquals(82, vedtak.stonadsperiode?.totalUker)
    }

    @Test
    fun `gir engangsstonad naar inntektshistorikk er tom`() {
        val vedtak = service.lagVedtak(
            soknad(
                id = "fp-kanttilfelle-tom-inntektshistorikk",
                inntekter = emptyList(),
            ),
        )

        assertEquals(VedtakType.ENGANGSSTONAD, vedtak.type)
        assertNull(vedtak.beregningsgrunnlag)
        assertNull(vedtak.stonadsperiode)
        assertNull(vedtak.kvoter)
        assertEquals(
            "0 av 10 maneder har godkjent inntekt.",
            vedtak.regelvurderinger.first { it.regel == "Opptjening 6 av 10 maneder" }.begrunnelse,
        )
    }

    @Test
    fun `feiler tydelig naar oppgitt arsinntekt er negativ`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            service.lagVedtak(
                soknad(
                    id = "fp-kanttilfelle-negativ-arsinntekt",
                    oppgittArsinntekt = -1,
                ),
            )
        }

        assertEquals("Oppgitt arsinntekt kan ikke vaere negativ.", feil.message)
    }

    @Test
    fun `feiler tydelig naar termindato er i fortiden`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            service.lagVedtak(
                soknad(
                    id = "fp-kanttilfelle-termindato-fortid",
                    termindato = "2020-01-01",
                ),
            )
        }

        assertEquals("Termindato kan ikke vaere i fortiden.", feil.message)
    }

    @Test
    fun `feiler tydelig naar termindato har ugyldig format`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            service.lagVedtak(
                soknad(
                    id = "fp-kanttilfelle-ugyldig-termindato",
                    termindato = "15.08.2026",
                ),
            )
        }

        assertEquals("Termindato maa vaere en gyldig dato paa formatet yyyy-MM-dd.", feil.message)
    }

    @Test
    fun `feiler tydelig naar inntektshistorikk har negativt belop`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            service.lagVedtak(
                soknad(
                    id = "fp-kanttilfelle-negativ-inntekt",
                    inntekter = listOf(inntekt("2026-01", -5_000)),
                ),
            )
        }

        assertEquals("Inntektshistorikk kan ikke ha negativt belop.", feil.message)
    }

    @Test
    fun `feiler tydelig naar fodselsnummer er tomt`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            service.lagVedtak(
                soknad(
                    id = "fp-kanttilfelle-tomt-fodselsnummer",
                    fodselsnummer = "",
                ),
            )
        }

        assertEquals("Fodselsnummer maa bestaa av 11 siffer.", feil.message)
    }

    @Test
    fun `feiler tydelig naar fodselsnummer har feil lengde`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            service.lagVedtak(
                soknad(
                    id = "fp-kanttilfelle-ugyldig-fodselsnummer",
                    fodselsnummer = "123",
                ),
            )
        }

        assertEquals("Fodselsnummer maa bestaa av 11 siffer.", feil.message)
    }

    @Test
    fun `feiler tydelig naar antall barn er null eller negativt`() {
        listOf(0, -1).forEach { antallBarn ->
            val feil = assertFailsWith<IllegalArgumentException> {
                service.lagVedtak(
                    soknad(
                        id = "fp-kanttilfelle-antall-barn-$antallBarn",
                        antallBarn = antallBarn,
                    ),
                )
            }

            assertEquals("Antall barn maa vaere minst 1.", feil.message)
        }
    }

    @Test
    fun `feiler tydelig naar soknads-id er tom`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            service.lagVedtak(
                soknad(
                    id = "",
                ),
            )
        }

        assertEquals("Soknads-ID kan ikke vaere tom.", feil.message)
    }

    @Test
    fun `feiler tydelig naar inntektsmaned har ugyldig format`() {
        val feil = assertFailsWith<IllegalArgumentException> {
            service.lagVedtak(
                soknad(
                    id = "fp-kanttilfelle-ugyldig-inntektsmaned",
                    inntekter = listOf(inntekt("januar", 45_000)),
                ),
            )
        }

        assertEquals("Inntektsmaned maa vaere paa formatet yyyy-MM.", feil.message)
    }

    private fun soknad(
        id: String = "fp-test",
        erNorskBorger: Boolean = true,
        fodselsnummer: String = "04059012377",
        termindato: String = "2026-08-15",
        oppgittArsinntekt: Int = 540_000,
        antallBarn: Int = 1,
        rettsforhold: Rettsforhold = Rettsforhold.BEGGE,
        dekningsgrad: Dekningsgrad = Dekningsgrad.HUNDRE_PROSENT,
        inntekter: List<Inntektsregistrering> = List(10) { index ->
            inntekt("2026-${(index + 1).toString().padStart(2, '0')}", 45_000)
        },
    ): Soknad =
        Soknad(
            id = id,
            beskrivelse = "Test",
            fodselsnummer = fodselsnummer,
            erNorskBorger = erNorskBorger,
            termindato = termindato,
            oppgittArsinntekt = oppgittArsinntekt,
            inntektshistorikk = inntekter,
            antallBarn = antallBarn,
            rettsforhold = rettsforhold,
            dekningsgrad = dekningsgrad,
        )

    private fun inntekt(maned: String, belop: Int): Inntektsregistrering =
        Inntektsregistrering(maned = maned, type = Inntektstype.ARBEID, belop = belop)
}
