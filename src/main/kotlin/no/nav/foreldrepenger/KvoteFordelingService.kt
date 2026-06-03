package no.nav.foreldrepenger

class KvoteFordelingService {
    fun fordel(soknad: Soknad, stonadsperiode: Stonadsperiode): KvoteFordelingVurdering {
        val flerbarnsbonus = beregnFlerbarnsbonus(soknad.antallBarn, soknad.dekningsgrad)
        val kvoter = when (soknad.rettsforhold) {
            Rettsforhold.BEGGE -> fordelNarBeggeHarRett(soknad, stonadsperiode, flerbarnsbonus)
            Rettsforhold.KUN_MOR -> fordelNarKunMorHarRett(stonadsperiode, flerbarnsbonus)
            Rettsforhold.KUN_FAR -> fordelNarKunFarHarRett(stonadsperiode, flerbarnsbonus)
        }

        return KvoteFordelingVurdering(
            kvoter = kvoter,
            regelvurdering = Regelvurdering(
                regel = "Kvotefordeling",
                resultat = Regelresultat.OPPFYLT,
                begrunnelse = "Kvotene summerer til ${kvoter.totalUker} uker.",
            ),
        )
    }

    private fun fordelNarBeggeHarRett(
        soknad: Soknad,
        stonadsperiode: Stonadsperiode,
        flerbarnsbonus: Int,
    ): Kvoter {
        val foreldrekvote = when (soknad.dekningsgrad) {
            Dekningsgrad.HUNDRE_PROSENT -> 15
            Dekningsgrad.ATTI_PROSENT -> 19
        }
        val forhandskvoteMor = 3
        val fellesperiode = stonadsperiode.totalUker - foreldrekvote - foreldrekvote - forhandskvoteMor - flerbarnsbonus

        return Kvoter(
            modrekvote = foreldrekvote,
            fedrekvote = foreldrekvote,
            fellesperiode = fellesperiode,
            forhandskvoteMor = forhandskvoteMor,
            flerbarnsbonus = flerbarnsbonus,
            totalUker = stonadsperiode.totalUker,
        )
    }

    private fun fordelNarKunMorHarRett(stonadsperiode: Stonadsperiode, flerbarnsbonus: Int): Kvoter {
        val forhandskvoteMor = 3

        return Kvoter(
            modrekvote = stonadsperiode.totalUker - forhandskvoteMor - flerbarnsbonus,
            fedrekvote = 0,
            fellesperiode = 0,
            forhandskvoteMor = forhandskvoteMor,
            flerbarnsbonus = flerbarnsbonus,
            totalUker = stonadsperiode.totalUker,
        )
    }

    private fun fordelNarKunFarHarRett(stonadsperiode: Stonadsperiode, flerbarnsbonus: Int): Kvoter =
        Kvoter(
            modrekvote = 0,
            fedrekvote = stonadsperiode.totalUker - flerbarnsbonus,
            fellesperiode = 0,
            forhandskvoteMor = 0,
            flerbarnsbonus = flerbarnsbonus,
            totalUker = stonadsperiode.totalUker,
        )

    private fun beregnFlerbarnsbonus(antallBarn: Int, dekningsgrad: Dekningsgrad): Int =
        when (dekningsgrad) {
            Dekningsgrad.HUNDRE_PROSENT -> when {
                antallBarn <= 1 -> 0
                antallBarn == 2 -> 17
                else -> 46
            }

            Dekningsgrad.ATTI_PROSENT -> when {
                antallBarn <= 1 -> 0
                antallBarn == 2 -> 21
                else -> 57
            }
        }
}

data class KvoteFordelingVurdering(
    val kvoter: Kvoter,
    val regelvurdering: Regelvurdering,
)
