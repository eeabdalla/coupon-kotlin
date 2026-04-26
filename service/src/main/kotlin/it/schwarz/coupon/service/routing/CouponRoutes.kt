package it.schwarz.coupon.service.routing

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import it.schwarz.coupon.service.model.CouponModel
import it.schwarz.coupon.service.service.CouponService
import org.koin.ktor.ext.inject

fun Route.couponRoutes() {
    val couponService by inject<CouponService>()
    val logger = KotlinLogging.logger {}

    route("/coupons") {
        get {
            val codes =
                call.request.queryParameters["codes"]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }

            val coupons = couponService.getCoupons(codes)
            call.respond(HttpStatusCode.OK, coupons)
        }

        post {
            try {
                val coupon = call.receive<CouponModel>()
                couponService.createCoupon(coupon)
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                logger.error(e) { "Failed to create coupon" }
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
