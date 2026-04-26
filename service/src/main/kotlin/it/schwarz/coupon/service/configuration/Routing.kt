package it.schwarz.coupon.service.configuration

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import it.schwarz.coupon.service.routing.couponRoutes

fun Application.configureRouting() {
    routing {
        couponRoutes()
    }
}
