package no.nav.foreldrepenger

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

class ForeldrepengerService(
    private val opptjeningService: OpptjeningService = OpptjeningService(),
    private val beregningsgrunnlagService: BeregningsgrunnlagService = BeregningsgrunnlagService(),
    private val stonadsperiodeService: StonadsperiodeService = StonadsperiodeService(),
    private val kvoteFordelingService: KvoteFordelingService = KvoteFordelingService(),
) {
    private val fodselsnummerRegex = Regex("""\d{11}""")

    fun lagVedtak(soknad: Soknad): Vedtak {
        validerSoknad(soknad)

        val opptjening = opptjeningService.vurder(soknad)
        if (!opptjening.oppfylt) {
            return lagAlternativtVedtak(soknad, opptjening)
        }

        val beregningsgrunnlag = beregningsgrunnlagService.beregn(soknad)
        if (beregningsgrunnlag.beregningsgrunnlag.kreverManuellVurdering) {
            return lagManuellVurdering(soknad, opptjening, beregningsgrunnlag)
        }

        val stonadsperiode = stonadsperiodeService.beregn(soknad)
        val kvotefordeling = kvoteFordelingService.fordel(soknad, stonadsperiode.stonadsperiode)

        return Vedtak(
            id = "vedtak-${soknad.id}",
            soknadId = soknad.id,
            type = VedtakType.INNVILGET_FORELDREPENGER,
            tittel = "Innvilget foreldrepenger",
            begrunnelse = "Soker oppfyller de forenklede vilkarene for foreldrepenger.",
            regelvurderinger = opptjening.regelvurderinger +
                beregningsgrunnlag.regelvurdering +
                stonadsperiode.regelvurdering +
                kvotefordeling.regelvurdering,
            beregningsgrunnlag = beregningsgrunnlag.beregningsgrunnlag,
            stonadsperiode = stonadsperiode.stonadsperiode,
            kvoter = kvotefordeling.kvoter,
        )
    }

    private fun lagAlternativtVedtak(soknad: Soknad, opptjening: Opptjeningsvurdering): Vedtak {
        val alternativVedtakVurdering = if (soknad.erNorskBorger) {
            Regelvurdering(
                regel = "Engangsstonad",
                resultat = Regelresultat.OPPFYLT,
                begrunnelse = "Soker oppfyller forenklet medlemskapskrav og kan fa engangsstonad.",
            )
        } else {
            Regelvurdering(
                regel = "Engangsstonad",
                resultat = Regelresultat.IKKE_OPPFYLT,
                begrunnelse = "Soker oppfyller ikke forenklet medlemskapskrav.",
            )
        }

        val vedtakType = if (soknad.erNorskBorger) VedtakType.ENGANGSSTONAD else VedtakType.AVSLAG
        val tittel = if (soknad.erNorskBorger) "Innvilget engangsstonad" else "Avslag"
        val begrunnelse = if (soknad.erNorskBorger) {
            "Opptjeningskravet for foreldrepenger er ikke oppfylt, men soker kan fa engangsstonad."
        } else {
            "Soker oppfyller verken vilkarene for foreldrepenger eller engangsstonad."
        }

        return Vedtak(
            id = "vedtak-${soknad.id}",
            soknadId = soknad.id,
            type = vedtakType,
            tittel = tittel,
            begrunnelse = begrunnelse,
            regelvurderinger = opptjening.regelvurderinger + alternativVedtakVurdering,
        )
    }

    private fun lagManuellVurdering(
        soknad: Soknad,
        opptjening: Opptjeningsvurdering,
        beregningsgrunnlag: BeregningsgrunnlagVurdering,
    ): Vedtak =
        Vedtak(
            id = "vedtak-${soknad.id}",
            soknadId = soknad.id,
            type = VedtakType.MANUELL_VURDERING,
            tittel = "Manuell vurdering",
            begrunnelse = "Saken ma vurderes manuelt fordi inntektsavviket er for stort.",
            regelvurderinger = opptjening.regelvurderinger + beregningsgrunnlag.regelvurdering,
            beregningsgrunnlag = beregningsgrunnlag.beregningsgrunnlag,
        )

    private fun validerSoknad(soknad: Soknad) {
        require(soknad.id.isNotBlank()) {
            "Soknads-ID kan ikke vaere tom."
        }

        require(fodselsnummerRegex.matches(soknad.fodselsnummer)) {
            "Fodselsnummer maa bestaa av 11 siffer."
        }

        require(soknad.oppgittArsinntekt >= 0) {
            "Oppgitt arsinntekt kan ikke vaere negativ."
        }

        require(soknad.antallBarn > 0) {
            "Antall barn maa vaere minst 1."
        }

        val termindato = try {
            LocalDate.parse(soknad.termindato)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("Termindato maa vaere en gyldig dato paa formatet yyyy-MM-dd.")
        }

        require(!termindato.isBefore(LocalDate.now())) {
            "Termindato kan ikke vaere i fortiden."
        }

        soknad.inntektshistorikk.forEach { inntekt ->
            require(inntekt.belop >= 0) {
                "Inntektshistorikk kan ikke ha negativt belop."
            }

            try {
                YearMonth.parse(inntekt.maned)
            } catch (_: DateTimeParseException) {
                throw IllegalArgumentException("Inntektsmaned maa vaere paa formatet yyyy-MM.")
            }
        }
    }
}
