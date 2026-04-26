package it.schwarz.coupon.service.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import it.schwarz.coupon.service.model.CouponModel
import it.schwarz.coupon.service.service.CouponService
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class CouponRoutesTest :
    StringSpec({

        val couponService = mockk<CouponService>()

        val sampleCoupon =
            CouponModel(
                code = "abc123",
                discount = 12.54,
                description = "best coupon",
                applicationCount = 0,
            )

        "GET /coupons returns all coupons" {
            coEvery { couponService.getCoupons(null) } returns listOf(sampleCoupon)

            testApplication {
                application {
                    install(Koin) {
                        modules(module { single { couponService } })
                    }
                    install(ContentNegotiation) { json() }
                    routing { couponRoutes() }
                }

                val response = client.get("/coupons")

                response.status shouldBe HttpStatusCode.OK
                val body = response.bodyAsText()
                val coupons = Json.decodeFromString<List<CouponModel>>(body)
                coupons.size shouldBe 1
                coupons.first().code shouldBe "abc123"
            }
        }

        "GET /coupons?codes=abc123 filters by codes" {
            val codes = listOf("abc123")
            coEvery { couponService.getCoupons(codes) } returns listOf(sampleCoupon)

            testApplication {
                application {
                    install(Koin) {
                        modules(module { single { couponService } })
                    }
                    install(ContentNegotiation) { json() }
                    routing { couponRoutes() }
                }

                val response = client.get("/coupons?codes=abc123")

                response.status shouldBe HttpStatusCode.OK
                val body = response.bodyAsText()
                val coupons = Json.decodeFromString<List<CouponModel>>(body)
                coupons.size shouldBe 1
                coupons.first().code shouldBe "abc123"
            }
        }

        "POST /coupons creates a coupon" {
            coEvery { couponService.createCoupon(sampleCoupon) } returns Unit

            testApplication {
                application {
                    install(Koin) {
                        modules(module { single { couponService } })
                    }
                    install(ContentNegotiation) { json() }
                    routing { couponRoutes() }
                }

                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(sampleCoupon))
                    }

                response.status shouldBe HttpStatusCode.OK
                coVerify { couponService.createCoupon(sampleCoupon) }
            }
        }

        "POST /coupons returns 500 on failure" {
            coEvery { couponService.createCoupon(any()) } throws RuntimeException("DB error")

            testApplication {
                application {
                    install(Koin) {
                        modules(module { single { couponService } })
                    }
                    install(ContentNegotiation) { json() }
                    routing { couponRoutes() }
                }

                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(sampleCoupon))
                    }

                response.status shouldBe HttpStatusCode.InternalServerError
            }
        }
    })
