package no.nav.foreldrepenger

import kotlinx.serialization.Serializable

@Serializable
data class Soknad(
    val id: String,
    val beskrivelse: String,
    val fodselsnummer: String,
    val erNorskBorger: Boolean,
    val termindato: String,
    val oppgittArsinntekt: Int,
    val inntektshistorikk: List<Inntektsregistrering>,
    val antallBarn: Int,
    val rettsforhold: Rettsforhold,
    val dekningsgrad: Dekningsgrad,
)

@Serializable
data class Inntektsregistrering(
    val maned: String,
    val type: Inntektstype,
    val belop: Int,
)

@Serializable
enum class Inntektstype {
    ARBEID,
    SYKEPENGER,
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    DAGPENGER,
    AAP,
    PLEIEPENGER,
    STIPEND_LANEKASSEN,
}

@Serializable
enum class Rettsforhold {
    BEGGE,
    KUN_MOR,
    KUN_FAR,
}

@Serializable
enum class Dekningsgrad {
    HUNDRE_PROSENT,
    ATTI_PROSENT,
}

@Serializable
data class Vedtak(
    val id: String,
    val soknadId: String,
    val type: VedtakType,
    val tittel: String,
    val begrunnelse: String,
    val regelvurderinger: List<Regelvurdering>,
    val beregningsgrunnlag: Beregningsgrunnlag? = null,
    val stonadsperiode: Stonadsperiode? = null,
    val kvoter: Kvoter? = null,
)

@Serializable
enum class VedtakType {
    INNVILGET_FORELDREPENGER,
    ENGANGSSTONAD,
    AVSLAG,
    MANUELL_VURDERING,
}

@Serializable
data class Opptjeningsvurdering(
    val oppfylt: Boolean,
    val erNorskBorger: Boolean,
    val godkjenteManeder: Int,
    val kravTilManeder: Int,
    val annualisertInntekt: Int,
    val kravTilInntekt: Int,
    val regelvurderinger: List<Regelvurdering>,
)

@Serializable
data class Beregningsgrunnlag(
    val arssats: Int,
    val oppgittArsinntekt: Int,
    val avvikProsent: Double?,
    val grunnlagBelop: Int?,
    val kreverManuellVurdering: Boolean,
)

@Serializable
data class Stonadsperiode(
    val totalUker: Int,
    val rettsforhold: Rettsforhold,
    val antallBarn: Int,
    val dekningsgrad: Dekningsgrad,
)

@Serializable
data class Kvoter(
    val modrekvote: Int,
    val fedrekvote: Int,
    val fellesperiode: Int,
    val forhandskvoteMor: Int,
    val flerbarnsbonus: Int,
    val totalUker: Int,
) {
    init {
        require(totalUker == modrekvote + fedrekvote + fellesperiode + forhandskvoteMor + flerbarnsbonus) {
            "Summen av kvotene maa vaere lik total stonadsperiode."
        }
    }
}

@Serializable
data class Regelvurdering(
    val regel: String,
    val resultat: Regelresultat,
    val begrunnelse: String,
)

@Serializable
enum class Regelresultat {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_AKTUELL,
    MANUELL_VURDERING,
}
