package no.nav

import io.ktor.server.application.*
import no.nav.config.configureRouting
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.Application")

fun main(args: Array<String>) {
    log.info("Starting application")
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    log.info("Initializing application module")

    configureRouting()

    log.info("Application module initialized successfully")
}
