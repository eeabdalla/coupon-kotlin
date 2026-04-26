package it.schwarz.coupon.service.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import it.schwarz.coupon.service.model.ErrorResponse
import it.schwarz.coupon.service.service.CouponService
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val jsonConfig =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

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

        fun configureApp(block: suspend (io.ktor.client.HttpClient) -> Unit) {
            testApplication {
                application {
                    install(Koin) {
                        modules(module { single { couponService } })
                    }
                    install(ContentNegotiation) { json(jsonConfig) }
                    routing { couponRoutes() }
                }
                block(client)
            }
        }

        // ---- GET /coupons ----

        "GET /coupons returns all coupons" {
            coEvery { couponService.getCoupons(null) } returns listOf(sampleCoupon)

            configureApp { client ->
                val response = client.get("/coupons")
                response.status shouldBe HttpStatusCode.OK
                val coupons = Json.decodeFromString<List<CouponModel>>(response.bodyAsText())
                coupons.size shouldBe 1
                coupons.first().code shouldBe "abc123"
            }
        }

        "GET /coupons?codes=abc123 filters by codes" {
            coEvery { couponService.getCoupons(listOf("abc123")) } returns listOf(sampleCoupon)

            configureApp { client ->
                val response = client.get("/coupons?codes=abc123")
                response.status shouldBe HttpStatusCode.OK
                val coupons = Json.decodeFromString<List<CouponModel>>(response.bodyAsText())
                coupons.size shouldBe 1
                coupons.first().code shouldBe "abc123"
            }
        }

        // ---- POST /coupons ----

        "POST /coupons creates a coupon with 201" {
            coEvery { couponService.createCoupon(sampleCoupon) } returns Unit

            configureApp { client ->
                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(sampleCoupon))
                    }
                response.status shouldBe HttpStatusCode.Created
                coVerify { couponService.createCoupon(sampleCoupon) }
            }
        }

        "POST /coupons returns 400 for blank code" {
            configureApp { client ->
                val invalid = sampleCoupon.copy(code = "  ")
                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(invalid))
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.error shouldBe "Validation Failed"
                error.fieldErrors!!.size shouldBe 1
                error.fieldErrors!!.first().field shouldBe "code"
                error.fieldErrors!!.first().message shouldBe "coupon code must be provided"
            }
        }

        "POST /coupons returns 400 for non-positive discount" {
            configureApp { client ->
                val invalid = sampleCoupon.copy(discount = 0.0)
                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(invalid))
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.fieldErrors!!.first().field shouldBe "discount"
                error.fieldErrors!!.first().message shouldBe "discount must be a positive value"
            }
        }

        "POST /coupons returns 400 for blank description" {
            configureApp { client ->
                val invalid = sampleCoupon.copy(description = "")
                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(invalid))
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.fieldErrors!!.first().field shouldBe "description"
            }
        }

        "POST /coupons returns all validation errors at once" {
            configureApp { client ->
                val invalid = CouponModel(code = "", discount = -1.0, description = "")
                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(invalid))
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.fieldErrors!!.size shouldBe 3
            }
        }

        "POST /coupons returns 400 for malformed JSON" {
            configureApp { client ->
                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody("not json")
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.error shouldBe "Malformed Request"
                error.fieldErrors shouldBe null
            }
        }

        "POST /coupons returns 500 on unexpected error" {
            coEvery { couponService.createCoupon(any()) } throws RuntimeException("DB error")

            configureApp { client ->
                val response =
                    client.post("/coupons") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(sampleCoupon))
                    }
                response.status shouldBe HttpStatusCode.InternalServerError
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.error shouldBe "Internal Server Error"
                error.fieldErrors shouldBe null
            }
        }

        // ---- POST /coupons/batch ----

        "POST /coupons/batch creates multiple coupons with 201" {
            val coupons = listOf(sampleCoupon, sampleCoupon.copy(code = "xyz789"))
            coEvery { couponService.createCoupons(coupons) } returns Unit

            configureApp { client ->
                val body = """{"coupons":${Json.encodeToString(coupons)}}"""
                val response =
                    client.post("/coupons/batch") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                response.status shouldBe HttpStatusCode.Created
                val result = Json.decodeFromString<List<CouponModel>>(response.bodyAsText())
                result.size shouldBe 2
                coVerify { couponService.createCoupons(coupons) }
            }
        }

        "POST /coupons/batch returns 400 for empty coupons list" {
            configureApp { client ->
                val response =
                    client.post("/coupons/batch") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"coupons":[]}""")
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.fieldErrors!!.first().message shouldBe "coupon list must not be empty"
            }
        }

        "POST /coupons/batch returns 400 for missing coupons field" {
            configureApp { client ->
                val response =
                    client.post("/coupons/batch") {
                        contentType(ContentType.Application.Json)
                        setBody("""{}""")
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.fieldErrors!!.first().message shouldBe "coupon list must not be empty"
            }
        }

        "POST /coupons/batch returns 400 for nested validation errors with indexed paths" {
            configureApp { client ->
                val invalid = CouponModel(code = "", discount = -1.0, description = "ok")
                val body = """{"coupons":[${Json.encodeToString(sampleCoupon)},${Json.encodeToString(invalid)}]}"""
                val response =
                    client.post("/coupons/batch") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.fieldErrors!!.any { it.field == "coupons[1].code" } shouldBe true
                error.fieldErrors!!.any { it.field == "coupons[1].discount" } shouldBe true
            }
        }

        "POST /coupons/batch returns 400 for exceeding max batch size" {
            configureApp { client ->
                val bigList = (1..1001).map { sampleCoupon.copy(code = "code$it") }
                val body = """{"coupons":${Json.encodeToString(bigList)}}"""
                val response =
                    client.post("/coupons/batch") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.fieldErrors!!.first().message shouldContain "must not exceed 1000"
            }
        }

        "POST /coupons/batch returns 400 for malformed JSON" {
            configureApp { client ->
                val response =
                    client.post("/coupons/batch") {
                        contentType(ContentType.Application.Json)
                        setBody("not valid json")
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.error shouldBe "Malformed Request"
            }
        }

        "POST /coupons/batch returns 500 on unexpected error" {
            coEvery { couponService.createCoupons(any()) } throws RuntimeException("DB error")

            configureApp { client ->
                val body = """{"coupons":[${Json.encodeToString(sampleCoupon)}]}"""
                val response =
                    client.post("/coupons/batch") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                response.status shouldBe HttpStatusCode.InternalServerError
                val error = jsonConfig.decodeFromString<ErrorResponse>(response.bodyAsText())
                error.error shouldBe "Internal Server Error"
            }
        }
    })
