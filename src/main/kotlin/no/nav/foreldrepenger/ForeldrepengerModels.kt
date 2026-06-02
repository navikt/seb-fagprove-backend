package no.nav.foreldrepenger

import kotlinx.serialization.Serializable

@Serializable
data class Soknad(
    val id: String,
    val beskrivelse: String,
    val fnr: String,
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
