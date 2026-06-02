package no.nav.config

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.serialization.Serializable
import no.nav.exception.BadRequestException
import no.nav.exception.NotFoundException
import no.nav.foreldrepenger.DigisisSoknadDto
import no.nav.foreldrepenger.OpptjeningService
import org.slf4j.LoggerFactory
import java.time.Instant

private val routingLog = LoggerFactory.getLogger("no.nav.config.Routing")

@Serializable
data class ErrorResponse(val message: String)

@Serializable
data class StatusResponse(
    val status: String,
    val app: String,
    val message: String,
    val timestamp: String,
)

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }

    install(SSE)

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            routingLog.warn("Bad request: {}", cause.message)
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request"))
        }
        exception<NotFoundException> { call, cause ->
            routingLog.warn("Not found: {}", cause.message)
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Not found"))
        }
        exception<IllegalArgumentException> { call, cause ->
            routingLog.warn("Invalid input: {}", cause.message)
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid input"))
        }
        exception<Throwable> { call, cause ->
            routingLog.error("Unexpected error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }

    routing {
        get("/internal/isalive") {
            call.respondText("ALIVE")
        }
        get("/internal/isready") {
            call.respondText("READY")
        }

        route("/api") {
            val opptjeningService = OpptjeningService()

            get("/status") {
                call.respond(
                    StatusResponse(
                        status = "ok",
                        app = "seb-fagprove-backend",
                        message = "Backend svarer fra Ktor",
                        timestamp = Instant.now().toString(),
                    ),
                )
            }

            post("/foreldrepenger/vurder") {
                val soknad = call.receive<DigisisSoknadDto>().toDomain()
                call.respond(opptjeningService.lagVedtak(soknad))
            }
        }
    }
}
