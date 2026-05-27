# Seb Fagprøve Backend

Backend for fagprøveprosjektet med Kotlin, Ktor og NAIS deployment. Denne varianten er API-only og bruker ikke database.

> Tilhørende frontend: `seb-fagprove-frontend`
>
> Denne kopien har en enkel API-rute på `GET /api/status` som frontend kan bruke for å verifisere fullstack-koblingen.

## Bruk

Kopier filene inn i et nytt, tomt repo:

```bash
# Opprett et nytt tomt repo på GitHub
cd seb-fagprove-backend
rm -rf .git
git init
git remote add origin git@github.com:navikt/seb-fagprove-backend.git

# Kjør appen
./gradlew run
```

Appen kjører på [http://localhost:8080](http://localhost:8080).

## Tech Stack

- **Språk**: [Kotlin](https://kotlinlang.org/) 2.2
- **Web-rammeverk**: [Ktor](https://ktor.io/) 3.3
- **Serialisering**: kotlinx-serialization
- **Logging**: Logback
- **Bygg**: Gradle 8.14 med Kotlin DSL
- **JDK**: 21 (Temurin)

### Forutsetninger

- JDK 21+ ([Temurin](https://adoptium.net/))

### Helsesjekk

- `GET /internal/isalive` → "ALIVE"
- `GET /internal/isready` → "READY"
- `GET /api/status` → JSON-respons brukt av frontend

## Prosjektstruktur

```
src/main/kotlin/no/nav/
├── Application.kt           # Applikasjonens inngang
├── config/
│   └── Routing.kt           # HTTP-ruter, CORS, feilhåndtering
└── exception/
    └── Exceptions.kt        # Egendefinerte unntak

src/main/resources/
├── application.yaml          # Ktor-konfigurasjon
└── logback.xml               # Logging-konfigurasjon
```

## Legge til nye domener

Følg dette mønsteret for nye domener (f.eks. `user`):

```
src/main/kotlin/no/nav/domain/user/
├── UserDto.kt        # Data transfer objects (@Serializable)
├── UserService.kt    # Forretningslogikk
└── UserController.kt # HTTP-ruter
```

1. Definer request/response-DTOer i `UserDto.kt`.
2. Legg forretningslogikk i `UserService.kt`.
3. Lag HTTP-ruter i `UserController.kt`.
4. Registrer rutene i `Routing.kt`.

## TODOs etter kloning

1. Bytt `seb-fagprove-backend-dev` og `seb-fagprove-backend` i `.nais/` til endelig appnavn hvis du får et annet navn.
2. Bytt ingress til samme appnavn.
3. Oppdater `accessPolicy.inbound.rules.application` til endelig frontendnavn.
4. Oppdater `image_suffix` i `.github/workflows/` slik at det matcher `metadata.name`.

## Fullstack-kobling

Frontend forventer at backend svarer på:

```http
GET /api/status
```

Lokalt kjører backend på [http://localhost:8080](http://localhost:8080). På NAIS bruker frontend service discovery med `BACKEND_URL=http://seb-fagprove-backend-dev`.

## NAIS

Dev-oppsettet bruker:

- backend-app: `seb-fagprove-backend-dev`
- ingress: `https://seb-fagprove-backend-dev.ekstern.dev.nav.no`
- inbound access policy fra `seb-fagprove-frontend-dev`

Prod-oppsettet bruker tilsvarende navn uten `-dev`.

GitHub Actions deployer dev fra `dev`-branchen og prod fra `main`. Repoet må autoriseres for deploy i Nais Console, og workflowen forventer NAIS secrets/vars som ligger i prosjektoppsettet.

## Bygg

```bash
./gradlew build           # Bygg prosjektet
./gradlew test            # Kjør tester
./gradlew run             # Kjør lokalt
```
