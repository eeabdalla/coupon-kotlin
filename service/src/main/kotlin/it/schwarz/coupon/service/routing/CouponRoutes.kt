package it.schwarz.coupon.service.routing

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import it.schwarz.coupon.service.model.BatchCouponRequest
import it.schwarz.coupon.service.model.CouponModel
import it.schwarz.coupon.service.model.ErrorResponse
import it.schwarz.coupon.service.model.FieldError
import it.schwarz.coupon.service.service.CouponService
import it.schwarz.coupon.service.validation.validateBatchCoupons
import it.schwarz.coupon.service.validation.validateCoupon
import org.koin.ktor.ext.inject
import java.time.Instant

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
            val path = call.request.uri
            try {
                val coupon =
                    try {
                        call.receive<CouponModel>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            malformedRequestError(path),
                        )
                        return@post
                    }

                val fieldErrors = validateCoupon(coupon)
                if (fieldErrors.isNotEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        validationError(path, fieldErrors),
                    )
                    return@post
                }

                couponService.createCoupon(coupon)
                call.respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                logger.error(e) { "Failed to create coupon" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    serverError(path),
                )
            }
        }

        post("/batch") {
            val path = call.request.uri
            try {
                val batchRequest =
                    try {
                        call.receive<BatchCouponRequest>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            malformedRequestError(path),
                        )
                        return@post
                    }

                val coupons = batchRequest.coupons
                if (coupons.isNullOrEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        validationError(
                            path,
                            listOf(
                                FieldError(
                                    field = "coupons",
                                    message = "coupon list must not be empty",
                                ),
                            ),
                        ),
                    )
                    return@post
                }

                if (coupons.size > MAX_BATCH_SIZE) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        validationError(
                            path,
                            listOf(
                                FieldError(
                                    field = "coupons",
                                    message = "batch size must not exceed $MAX_BATCH_SIZE",
                                    rejectedValue = coupons.size.toString(),
                                ),
                            ),
                        ),
                    )
                    return@post
                }

                val fieldErrors = validateBatchCoupons(coupons)
                if (fieldErrors.isNotEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        validationError(path, fieldErrors),
                    )
                    return@post
                }

                couponService.createCoupons(coupons)
                call.respond(HttpStatusCode.Created, coupons)
            } catch (e: Exception) {
                logger.error(e) { "Failed to create coupons in batch" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    serverError(path),
                )
            }
        }
    }
}

private const val MAX_BATCH_SIZE = 1000

private fun validationError(
    path: String,
    fieldErrors: List<FieldError>,
) = ErrorResponse(
    timestamp = Instant.now().toString(),
    status = 400,
    error = "Validation Failed",
    message = "One or more fields failed validation",
    path = path,
    fieldErrors = fieldErrors,
)

private fun malformedRequestError(path: String) =
    ErrorResponse(
        timestamp = Instant.now().toString(),
        status = 400,
        error = "Malformed Request",
        message = "Request body is missing or contains malformed JSON",
        path = path,
    )

private fun serverError(path: String) =
    ErrorResponse(
        timestamp = Instant.now().toString(),
        status = 500,
        error = "Internal Server Error",
        message = "An unexpected error occurred",
        path = path,
    )
