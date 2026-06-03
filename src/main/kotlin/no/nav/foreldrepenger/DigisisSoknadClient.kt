package no.nav.foreldrepenger

import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class DigisisSoknadClient(
    private val apiUrl: URI = URI.create(
        System.getenv("DIGISIS_FORELDREPENGER_URL")
            ?: "https://api.digisis.org/api/foreldrepenger/soknader",
    ),
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun hentSoknader(): List<Soknad> {
        val request = HttpRequest.newBuilder(apiUrl)
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("DigiSIS svarte med HTTP ${response.statusCode()}.")
        }

        return json.decodeFromString<List<DigisisSoknadDto>>(response.body())
            .map { it.toDomain() }
    }
}
