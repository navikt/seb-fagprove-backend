package no.nav.foreldrepenger

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

class BeregningsgrunnlagService {
    fun beregn(soknad: Soknad): BeregningsgrunnlagVurdering {
        val sisteTreManeder = soknad.inntektshistorikk
            .sortedBy { it.maned }
            .takeLast(ANTALL_BEREGNINGSMANEDER)

        val gjennomsnitt = if (sisteTreManeder.isEmpty()) {
            0.0
        } else {
            sisteTreManeder.sumOf { it.belop }.toDouble() / sisteTreManeder.size
        }

        val arssats = (gjennomsnitt * 12).roundToInt()
        val avvikProsent = if (soknad.oppgittArsinntekt == 0) {
            null
        } else {
            round(abs(arssats - soknad.oppgittArsinntekt) * 1000.0 / soknad.oppgittArsinntekt) / 10
        }
        val kreverManuellVurdering = avvikProsent != null && avvikProsent > MAKS_AVVIK_PROSENT
        val grunnlagBelop = if (kreverManuellVurdering) null else min(arssats, SEKS_G_2025)

        val beregningsgrunnlag = Beregningsgrunnlag(
            arssats = arssats,
            oppgittArsinntekt = soknad.oppgittArsinntekt,
            avvikProsent = avvikProsent,
            grunnlagBelop = grunnlagBelop,
            kreverManuellVurdering = kreverManuellVurdering,
        )

        val regelvurdering = if (kreverManuellVurdering) {
            Regelvurdering(
                regel = "Beregningsgrunnlag",
                resultat = Regelresultat.MANUELL_VURDERING,
                begrunnelse = "For stort sprik mellom 3-maneders snitt og oppgitt arsinntekt.",
            )
        } else {
            Regelvurdering(
                regel = "Beregningsgrunnlag",
                resultat = Regelresultat.OPPFYLT,
                begrunnelse = "Beregnet arssats er $arssats kr. Beregningsgrunnlag er $grunnlagBelop kr.",
            )
        }

        return BeregningsgrunnlagVurdering(
            beregningsgrunnlag = beregningsgrunnlag,
            regelvurdering = regelvurdering,
        )
    }

    companion object {
        const val SEKS_G_2025 = 780_960
        private const val ANTALL_BEREGNINGSMANEDER = 3
        private const val MAKS_AVVIK_PROSENT = 25.0
    }
}

data class BeregningsgrunnlagVurdering(
    val beregningsgrunnlag: Beregningsgrunnlag,
    val regelvurdering: Regelvurdering,
)
