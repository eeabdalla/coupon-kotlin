package it.schwarz.coupon.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import it.schwarz.coupon.service.configuration.configureKoin
import it.schwarz.coupon.service.configuration.configureRouting
import it.schwarz.coupon.service.configuration.configureSerialization

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

@Suppress("unused")
fun Application.module() {
    val logger = KotlinLogging.logger {}

    configureKoin()
    configureSerialization()
    configureRouting()

    with(monitor) {
        subscribe(ApplicationStarted) {
            logger.info { "Application started" }
        }
        subscribe(ApplicationStopped) {
            logger.info { "Stopping application" }
        }
    }
}
