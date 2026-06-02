# Seb Fagprøve Backend

Backend for fagprøveprosjektet mitt i IT-utviklerfaget. Prosjektet er en forenklet NAV-løsning for behandling av test-søknader om foreldrepenger.

Backend er laget med Kotlin og Ktor, deployes til NAIS, og bruker ikke database. Dataene som skal behandles kommer fra testdata/API og regelvurderingen skjer i applikasjonslogikken.

Tilhørende frontend: `seb-fagprove-frontend`

## Status nå

Dette er foreløpig backend-grunnlaget for foreldrepenger-caset.

Ferdig:

- grunnmodeller for søknad, inntekt, vedtak og regelvurdering
- første del av regelmotoren: opptjeningsvurdering
- fallback til engangsstønad eller avslag hvis opptjening ikke er oppfylt
- enhetstester for opptjening og fallback
- helsesjekker og enkel status-rute for fullstack-kobling

Ikke ferdig ennå:

- integrasjon mot DigiSIS API
- HTTP-endepunkt for å vurdere en foreldrepengesøknad
- resten av reglene for beregningsgrunnlag, stønadsperiode og fordeling av uker
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
```

`/api/status` brukes av frontend for å sjekke at frontend og backend er koblet sammen.

Eksempel på statusrespons:

```json
{
  "status": "ok",
  "app": "seb-fagprove-backend",
  "message": "Backend svarer fra Ktor",
  "timestamp": "2026-06-02T10:00:00Z"
}
```

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
    ├── ForeldrepengerModels.kt
    └── OpptjeningService.kt

src/test/kotlin/no/nav/foreldrepenger/
└── OpptjeningServiceTest.kt
```

## Foreldrepenger-logikk

`ForeldrepengerModels.kt` inneholder domenemodellene som brukes i vurderingen:

- `Soknad`
- `Inntektsregistrering`
- `Vedtak`
- `Regelvurdering`
- enum-typer for inntekt, rettsforhold, dekningsgrad og vedtakstype

`OpptjeningService.kt` vurderer foreløpig bare opptjening:

- søkeren må oppfylle forenklet medlemskapskrav
- søkeren må ha minst 6 av 10 måneder med godkjent inntekt
- annualisert inntekt må være over 1/2G

Hvis opptjening ikke er oppfylt, lager tjenesten et fallback-vedtak:

- norsk borger: `ENGANGSSTONAD`
- ikke norsk borger: `AVSLAG`

Denne logikken er bevisst holdt adskilt fra HTTP-rutene, slik at den kan testes uten å starte serveren.

## NAIS

Dev:

- appnavn: `seb-fagprove-backend-dev`
- cluster: `dev-gcp`
- ingress: `https://seb-fagprove-backend-dev.ekstern.dev.nav.no`
- workflow: `.github/workflows/deploy-dev.yaml`
- NAIS-fil: `.nais/deploy-dev.yml`

Prod:

- appnavn: `seb-fagprove-backend`
- cluster: `prod-gcp`
- workflow: `.github/workflows/deploy-prod.yaml`
- NAIS-fil: `.nais/deploy-prod.yml`

Dev deployes ved push til `main` eller manuelt fra GitHub Actions. Prod deployes manuelt.

Repoet må være autorisert i Nais Console for teamet `laerlinger`.

## Dokumentasjon for fagprøven

Dette backend-arbeidet dekker foreløpig:

- oppstart av domenemodell
- første regel i vurderingsflyten
- enhetstesting av forretningslogikk
- skille mellom frontend, backend og regelvurdering
- enkel NAIS-klar struktur uten database
