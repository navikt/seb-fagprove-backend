package no.nav.foreldrepenger

class StonadsperiodeService {
    fun beregn(soknad: Soknad): StonadsperiodeVurdering {
        val totalUker = when (soknad.rettsforhold) {
            Rettsforhold.BEGGE,
            Rettsforhold.KUN_MOR,
            -> beregnForBeggeEllerKunMor(soknad.antallBarn, soknad.dekningsgrad)

            Rettsforhold.KUN_FAR -> beregnForKunFar(soknad.antallBarn, soknad.dekningsgrad)
        }

        val stonadsperiode = Stonadsperiode(
            totalUker = totalUker,
            rettsforhold = soknad.rettsforhold,
            antallBarn = soknad.antallBarn,
            dekningsgrad = soknad.dekningsgrad,
        )

        return StonadsperiodeVurdering(
            stonadsperiode = stonadsperiode,
            regelvurdering = Regelvurdering(
                regel = "Stonadsperiode",
                resultat = Regelresultat.OPPFYLT,
                begrunnelse = "Total stonadsperiode er $totalUker uker.",
            ),
        )
    }

    private fun beregnForBeggeEllerKunMor(antallBarn: Int, dekningsgrad: Dekningsgrad): Int =
        when (dekningsgrad) {
            Dekningsgrad.HUNDRE_PROSENT -> when {
                antallBarn <= 1 -> 49
                antallBarn == 2 -> 66
                else -> 95
            }

            Dekningsgrad.ATTI_PROSENT -> when {
                antallBarn <= 1 -> 61
                antallBarn == 2 -> 82
                else -> 118
            }
        }

    private fun beregnForKunFar(antallBarn: Int, dekningsgrad: Dekningsgrad): Int =
        when (dekningsgrad) {
            Dekningsgrad.HUNDRE_PROSENT -> when {
                antallBarn <= 1 -> 40
                antallBarn == 2 -> 57
                else -> 86
            }

            Dekningsgrad.ATTI_PROSENT -> when {
                antallBarn <= 1 -> 52
                antallBarn == 2 -> 73
                else -> 109
            }
        }
}

data class StonadsperiodeVurdering(
    val stonadsperiode: Stonadsperiode,
    val regelvurdering: Regelvurdering,
)
