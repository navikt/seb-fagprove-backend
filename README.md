# Seb Fagprøve Backend

Backend for fagprøveprosjektet mitt i IT-utviklerfaget. Prosjektet er en forenklet NAV-løsning for behandling av test-søknader om foreldrepenger.

Backend er laget med Kotlin og Ktor, deployes til NAIS dev og bruker ikke database. Søknader hentes fra DigiSIS API-et, mappes til intern domenemodell og vurderes av regelmotoren i backend.

Tilhørende frontend: `seb-fagprove-frontend`

## Brukerveiledning

Backend brukes først og fremst av frontend, men kan også brukes direkte av utvikler eller sensor for å kontrollere API-et og regelmotoren.

For å bruke backend lokalt:

1. Start backend:

```bash
cd seb-fagprove-backend
./gradlew run
```

2. Kontroller at backend svarer:

```bash
curl -sS http://localhost:8080/api/status
```

3. Hent søknader fra DigiSIS via backend:

```bash
curl -sS http://localhost:8080/api/foreldrepenger/soknader
```

4. Vurder en søknad ved å sende en komplett `Soknad` til:

```http
POST /api/foreldrepenger/vurder
```

Frontend gjør dette automatisk for alle søknader når saksbehandlerflaten åpnes.

## Hvordan backend brukes i løsningen

Flyten i løsningen er:

1. Backend henter test-søknader fra DigiSIS API-et.
2. DigiSIS-formatet mappes til intern domenemodell.
3. Frontend henter søknadene fra backend.
4. Frontend sender hver søknad tilbake til backend for vurdering.
5. Backend returnerer et `Vedtak` med resultat, begrunnelse og regelvurderinger.
6. Frontend viser resultatet til saksbehandler.

Dette gir flyten:

```text
DigiSIS API -> backend -> frontend -> backend regelmotor -> frontend
```

## Resultattyper

Regelmotoren kan returnere fire typer vedtak:

- `INNVILGET_FORELDREPENGER`: søknaden oppfyller kravene til foreldrepenger.
- `ENGANGSSTONAD`: søkeren oppfyller forenklet medlemskapskrav, men ikke opptjeningskravet.
- `AVSLAG`: søkeren oppfyller verken kravene til foreldrepenger eller engangsstønad.
- `MANUELL_VURDERING`: søknaden må vurderes av saksbehandler fordi inntektsavviket er for stort.

## Validering og endringsbestilling

Backend validerer søknaden før regelmotoren kjøres. Dette ble lagt til etter endringsbestillingen om at regelmotoren må tåle uventede og ugyldige inndata uten å krasje eller gi feil svar i det stille.

Valideringen dekker blant annet:

- tom søknads-ID
- ugyldig eller tomt fødselsnummer
- negativ oppgitt årsinntekt
- antall barn som er 0 eller negativt
- termindato i fortiden eller med ugyldig datoformat
- negativt beløp i inntektshistorikk
- ugyldig månedsformat i inntektshistorikk
- ukjent `rettsforhold` eller `dekningsgrad` sendt inn via API

Ugyldige søknader returnerer HTTP 400 der det er mulig, slik at feil inndata behandles som klientfeil og ikke som en tilfeldig intern systemfeil.

## Testede API-søknader

Ved lokal test av søknadene fra DigiSIS ga regelmotoren disse resultatene:

| Søknad | Resultat |
| --- | --- |
| FP-001 | Foreldrepenger |
| FP-002 | Avslag |
| FP-003 | Engangsstønad |
| FP-004 | Engangsstønad |
| FP-005 | Manuell vurdering |
| FP-006 | Foreldrepenger |
| FP-007 | Foreldrepenger |
| FP-008 | Foreldrepenger |
| FP-009 | Engangsstønad |
| FP-010 | Foreldrepenger |
| FP-011 | Foreldrepenger |
| FP-012 | Foreldrepenger |

## Endepunkter

```http
GET /internal/isalive
GET /internal/isready
GET /api/status
GET /api/foreldrepenger/soknader
POST /api/foreldrepenger/vurder
```

`/internal/isalive` og `/internal/isready` brukes av NAIS.

`/api/status` brukes for å kontrollere at backend kjører.

`GET /api/foreldrepenger/soknader` henter test-søknader fra DigiSIS API-et, mapper dem til intern modell og returnerer dem til frontend.

`POST /api/foreldrepenger/vurder` tar imot en intern `Soknad` og returnerer et `Vedtak`.

Eksempel på statusrespons:

```json
{
  "status": "ok",
  "app": "seb-fagprove-backend",
  "message": "Backend svarer fra Ktor",
  "timestamp": "2026-06-02T10:00:00Z"
}
```

## Foreldrepenger-logikk

`ForeldrepengerModels.kt` inneholder domenemodellene som brukes i vurderingen:

- `Soknad`
- `Inntektsregistrering`
- `Vedtak`
- `Regelvurdering`
- enum-typer for inntekt, rettsforhold, dekningsgrad og vedtakstype

I intern domenemodell bruker backend tydelige navn, for eksempel `fodselsnummer`.
DigiSIS API-et sender feltet som `fnr`, og dette mappes i `DigisisSoknadDto`.

Regelkjeden består av:

1. `OpptjeningService`: vurderer medlemskap, 6 av 10 måneder med inntekt og inntekt over 1/2G.
2. `BeregningsgrunnlagService`: beregner årsinntekt, avvik og 6G-tak.
3. `StonadsperiodeService`: beregner total stønadsperiode.
4. `KvoteFordelingService`: fordeler uker mellom mor, far, fellesperiode og flerbarnsbonus.
5. `ForeldrepengerService`: samler regelresultatene og lager endelig `Vedtak`.

Hvis inntektsavviket er mer enn 25 prosent, returnerer backend `MANUELL_VURDERING`.

## Bygg og test

```bash
./gradlew test
./gradlew build
```

`./gradlew build` brukes som hovedsjekk fordi den kompilerer prosjektet, kjører tester og sjekker formatering.

## Teknologi

- Kotlin
- Ktor
- kotlinx-serialization
- Gradle med Kotlin DSL
- JDK 21
- Logback
- NAIS
- GitHub Actions

## Prosjektstruktur

```text
src/main/kotlin/no/nav/
├── Application.kt
├── config/
│   └── Routing.kt
├── exception/
│   └── Exceptions.kt
└── foreldrepenger/
    ├── BeregningsgrunnlagService.kt
    ├── DigisisSoknadClient.kt
    ├── DigisisSoknadDto.kt
    ├── ForeldrepengerService.kt
    ├── ForeldrepengerModels.kt
    ├── KvoteFordelingService.kt
    ├── OpptjeningService.kt
    └── StonadsperiodeService.kt

src/test/kotlin/no/nav/foreldrepenger/
├── DigisisSoknadDtoTest.kt
├── ForeldrepengerRoutingTest.kt
├── ForeldrepengerServiceTest.kt
└── OpptjeningServiceTest.kt
```

## NAIS

Prosjektet deployes bare til dev i fagprøven.

- appnavn: `seb-fagprove-backend-dev`
- cluster: `dev-gcp`
- ingress: `https://seb-fagprove-backend-dev.ekstern.dev.nav.no`
- workflow: `.github/workflows/deploy-dev.yaml`
- NAIS-fil: `.nais/deploy-dev.yml`

Dev deployes ved push til `main` eller manuelt fra GitHub Actions.

Repoet må være autorisert i Nais Console for teamet `laerlinger`.

Backend har inbound access policy fra `seb-fagprove-frontend-dev`, slik at frontend kan kalle backend internt i NAIS. Backend har også outbound external access til `api.digisis.org`, fordi søknader hentes fra DigiSIS API-et.

## Avgrensninger

- Backend bruker ikke database.
- Test-søknader hentes fra DigiSIS API-et.
- Regelverket er forenklet for fagprøven og er ikke en full implementasjon av ekte foreldrepengeregelverk.
- Manuell vurdering lagres foreløpig ikke i backend.
