package no.nav.foreldrepenger

import kotlinx.serialization.Serializable

@Serializable
data class DigisisSoknadDto(
    val id: String,
    val beskrivelse: String,
    val fnr: String,
    val erNorskBorger: Boolean,
    val termindato: String,
    val oppgittArsinntekt: Int,
    val inntektshistorikk: List<DigisisInntektsregistreringDto>,
    val antallBarn: Int,
    val rettsforhold: String,
    val dekningsgrad: Int,
) {
    fun toDomain(): Soknad =
        Soknad(
            id = id,
            beskrivelse = beskrivelse,
            fnr = fnr,
            erNorskBorger = erNorskBorger,
            termindato = termindato,
            oppgittArsinntekt = oppgittArsinntekt,
            inntektshistorikk = inntektshistorikk.map { it.toDomain() },
            antallBarn = antallBarn,
            rettsforhold = rettsforhold.toRettsforhold(),
            dekningsgrad = dekningsgrad.toDekningsgrad(),
        )
}

@Serializable
data class DigisisInntektsregistreringDto(
    val maned: String,
    val type: String,
    val belop: Int,
) {
    fun toDomain(): Inntektsregistrering =
        Inntektsregistrering(
            maned = maned,
            type = type.toInntektstype(),
            belop = belop,
        )
}

private fun String.toRettsforhold(): Rettsforhold =
    when (this) {
        "begge" -> Rettsforhold.BEGGE
        "kun-mor" -> Rettsforhold.KUN_MOR
        "kun-far" -> Rettsforhold.KUN_FAR
        else -> throw IllegalArgumentException("Ukjent rettsforhold fra DigiSIS: $this")
    }

private fun Int.toDekningsgrad(): Dekningsgrad =
    when (this) {
        100 -> Dekningsgrad.HUNDRE_PROSENT
        80 -> Dekningsgrad.ATTI_PROSENT
        else -> throw IllegalArgumentException("Ukjent dekningsgrad fra DigiSIS: $this")
    }

private fun String.toInntektstype(): Inntektstype =
    runCatching { Inntektstype.valueOf(this) }
        .getOrElse { throw IllegalArgumentException("Ukjent inntektstype fra DigiSIS: $this") }
