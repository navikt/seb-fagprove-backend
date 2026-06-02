package no.nav.foreldrepenger

import kotlin.math.roundToInt

class OpptjeningService {
    fun vurder(soknad: Soknad): Opptjeningsvurdering {
        val opptjeningsperiode = soknad.inntektshistorikk
            .sortedBy { it.maned }
            .takeLast(ANTALL_OPPTJENINGSMANEDER)

        val godkjentInntekt = opptjeningsperiode
            .filter { it.type in GODKJENTE_INNTEKTSTYPER }
            .filter { it.belop > 0 }

        val godkjenteManeder = godkjentInntekt.map { it.maned }.distinct().size
        val annualisertInntekt = (godkjentInntekt.sumOf { it.belop } * 12.0 / ANTALL_OPPTJENINGSMANEDER).roundToInt()
        val harNokManeder = godkjenteManeder >= KRAV_TIL_OPPTJENINGSMANEDER
        val harNokInntekt = annualisertInntekt >= HALV_G_2025
        val oppfylt = soknad.erNorskBorger && harNokManeder && harNokInntekt

        return Opptjeningsvurdering(
            oppfylt = oppfylt,
            erNorskBorger = soknad.erNorskBorger,
            godkjenteManeder = godkjenteManeder,
            kravTilManeder = KRAV_TIL_OPPTJENINGSMANEDER,
            annualisertInntekt = annualisertInntekt,
            kravTilInntekt = HALV_G_2025,
            regelvurderinger = listOf(
                Regelvurdering(
                    regel = "Medlemskap i folketrygden",
                    resultat = if (soknad.erNorskBorger) Regelresultat.OPPFYLT else Regelresultat.IKKE_OPPFYLT,
                    begrunnelse = if (soknad.erNorskBorger) {
                        "Soker er norsk borger i testdataene."
                    } else {
                        "Soker er ikke norsk borger i testdataene."
                    },
                ),
                Regelvurdering(
                    regel = "Opptjening 6 av 10 maneder",
                    resultat = if (harNokManeder) Regelresultat.OPPFYLT else Regelresultat.IKKE_OPPFYLT,
                    begrunnelse = "$godkjenteManeder av $ANTALL_OPPTJENINGSMANEDER maneder har godkjent inntekt.",
                ),
                Regelvurdering(
                    regel = "Inntekt over 1/2G",
                    resultat = if (harNokInntekt) Regelresultat.OPPFYLT else Regelresultat.IKKE_OPPFYLT,
                    begrunnelse = "Annualisert inntekt er $annualisertInntekt kr. Kravet er $HALV_G_2025 kr.",
                ),
            ),
        )
    }

    fun lagFallbackVedtakHvisOpptjeningFeiler(soknad: Soknad): Vedtak? {
        val opptjening = vurder(soknad)
        if (opptjening.oppfylt) {
            return null
        }

        val fallbackVurdering = if (soknad.erNorskBorger) {
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
            regelvurderinger = opptjening.regelvurderinger + fallbackVurdering,
        )
    }

    companion object {
        const val HALV_G_2025 = 65_080
        private const val ANTALL_OPPTJENINGSMANEDER = 10
        private const val KRAV_TIL_OPPTJENINGSMANEDER = 6

        private val GODKJENTE_INNTEKTSTYPER = setOf(
            Inntektstype.ARBEID,
            Inntektstype.SYKEPENGER,
            Inntektstype.FORELDREPENGER,
            Inntektstype.SVANGERSKAPSPENGER,
            Inntektstype.DAGPENGER,
            Inntektstype.AAP,
            Inntektstype.PLEIEPENGER,
        )
    }
}
