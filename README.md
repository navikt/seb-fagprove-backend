# Seb Fagprøve Backend

Backend for fagprøveprosjektet mitt i IT-utviklerfaget. Prosjektet er en forenklet NAV-løsning for behandling av test-søknader om foreldrepenger.

Backend er laget med Kotlin og Ktor, deployes til NAIS, og bruker ikke database. Dataene som skal behandles kommer fra testdata/API og regelvurderingen skjer i applikasjonslogikken.

Tilhørende frontend: `seb-fagprove-frontend`

## Status nå

Dette er foreløpig backend-grunnlaget for foreldrepenger-caset.

Ferdig:

- grunnmodeller for søknad, inntekt, vedtak og regelvurdering
- DTO og mapping fra DigiSIS API-format til intern domenemodell
- henting av test-søknader fra DigiSIS API
- regelmotor for opptjening, engangsstønad, beregningsgrunnlag, stønadsperiode og kvotefordeling
- alternativt vedtak med engangsstønad eller avslag hvis opptjening ikke er oppfylt
- HTTP-endepunkter for å hente og vurdere foreldrepengesøknader
- enhetstester for regelmotor, mapping og HTTP-endepunkt
- helsesjekker og enkel status-rute for fullstack-kobling

Ikke ferdig ennå:

- frontend-visning av søknader og vedtak

## Lokal kjøring

```bash
cd seb-fagprove-backend
./gradlew run
```

Backend kjører lokalt på [http://localhost:8080](http://localhost:8080).

## Bygg og test

```bash
./gradlew build
./gradlew test
```

`./gradlew build` brukes som hovedsjekk fordi den kompilerer prosjektet, kjører tester og sjekker formatering.

## Endepunkter

```http
GET /internal/isalive
GET /internal/isready
GET /api/status
GET /api/foreldrepenger/soknader
POST /api/foreldrepenger/vurder
```

`/api/status` brukes av frontend for å sjekke at frontend og backend er koblet sammen.

`GET /api/foreldrepenger/soknader` henter test-søknader fra DigiSIS API-et, mapper dem til intern domenemodell og returnerer dem til frontend.

`POST /api/foreldrepenger/vurder` tar imot en intern `Soknad` fra frontend og returnerer et `Vedtak`.

Eksempel på statusrespons:

```json
{
  "status": "ok",
  "app": "seb-fagprove-backend",
  "message": "Backend svarer fra Ktor",
  "timestamp": "2026-06-02T10:00:00Z"
}
```

## Brukerveiledning

Dette er en foreløpig brukerveiledning for backend. Den skal oppdateres når integrasjon mot DigiSIS API og resten av vurderingsreglene er ferdig.

Backend brukes ikke direkte av en sluttbruker. Den brukes av frontend og av utvikler/tester som vil kontrollere at vurderingslogikken fungerer.

For å bruke backend lokalt:

1. Start backend med `./gradlew run`.
2. Kontroller at backend svarer med `GET /api/status`.
3. Hent test-søknader med `GET /api/foreldrepenger/soknader`.
4. Send en valgt søknad til `POST /api/foreldrepenger/vurder`.
5. Les responsen som et `Vedtak` med vedtakstype, begrunnelse og regelvurderinger.

Backend vurderer nå de fem forenklede reglene i oppgaven. Responsen kan derfor brukes til å se om en søknad gir foreldrepenger, engangsstønad, avslag eller manuell vurdering.

Når løsningen er ferdig, skal denne delen beskrive:

- hvilke endepunkter frontend bruker
- hvordan en test-søknad vurderes
- hvordan feilsituasjoner håndteres
- hvilke begrensninger backend har sammenlignet med ekte foreldrepengeregelverk

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

## Foreldrepenger-logikk

`ForeldrepengerModels.kt` inneholder domenemodellene som brukes i vurderingen:

- `Soknad`
- `Inntektsregistrering`
- `Vedtak`
- `Regelvurdering`
- enum-typer for inntekt, rettsforhold, dekningsgrad og vedtakstype

I intern domenemodell bruker vi tydelige navn, for eksempel `fodselsnummer`.
DigiSIS API-et sender feltet som `fnr`, og dette mappes i `DigisisSoknadDto`.

`OpptjeningService.kt` vurderer foreløpig bare opptjening:

- søkeren må oppfylle forenklet medlemskapskrav
- søkeren må ha minst 6 av 10 måneder med godkjent inntekt
- annualisert inntekt må være over 1/2G
- søknaden får vedtakstype `INNVILGET_FORELDREPENGER`, `ENGANGSSTONAD` eller `AVSLAG`

Hvis opptjening ikke er oppfylt, lager tjenesten et alternativt vedtak:

- norsk borger: `ENGANGSSTONAD`
- ikke norsk borger: `AVSLAG`

Hvis opptjening er oppfylt, går saken videre i regelkjeden:

1. `BeregningsgrunnlagService` beregner snitt fra de siste 3 månedene, annualiserer inntekten, sjekker avvik mot oppgitt årsinntekt og kutter ved 6G.
2. `StonadsperiodeService` finner total stønadsperiode basert på rettsforhold, antall barn og dekningsgrad.
3. `KvoteFordelingService` fordeler ukene mellom mor, far, fellesperiode, forhåndskvote og flerbarnsbonus.
4. `ForeldrepengerService` samler regelresultatene og lager endelig `Vedtak`.

Hvis beregningsgrunnlaget har mer enn 25 prosent avvik mot oppgitt årsinntekt, returnerer backend `MANUELL_VURDERING`.

Denne logikken er bevisst holdt adskilt fra HTTP-rutene, slik at den kan testes uten å starte serveren.

## NAIS

Prosjektet deployes bare til dev i fagprøven.

- appnavn: `seb-fagprove-backend-dev`
- cluster: `dev-gcp`
- ingress: `https://seb-fagprove-backend-dev.ekstern.dev.nav.no`
- workflow: `.github/workflows/deploy-dev.yaml`
- NAIS-fil: `.nais/deploy-dev.yml`

Dev deployes ved push til `main` eller manuelt fra GitHub Actions.

Repoet må være autorisert i Nais Console for teamet `laerlinger`.

## Dokumentasjon for fagprøven

Dette backend-arbeidet dekker foreløpig:

- oppstart av domenemodell
- alle fem forenklede regler i vurderingsflyten
- enhetstesting av forretningslogikk
- skille mellom frontend, backend og regelvurdering
- enkel NAIS-klar struktur uten database
