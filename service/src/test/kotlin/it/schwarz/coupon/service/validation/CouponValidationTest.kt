package it.schwarz.coupon.service.validation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import it.schwarz.coupon.service.model.CouponModel

class CouponValidationTest :
    StringSpec({

        val validCoupon =
            CouponModel(
                code = "abc123",
                discount = 12.54,
                description = "best coupon",
                applicationCount = 0,
            )

        "valid coupon produces no errors" {
            validateCoupon(validCoupon).shouldBeEmpty()
        }

        "blank code produces error" {
            val errors = validateCoupon(validCoupon.copy(code = "   "))
            errors shouldHaveSize 1
            errors.first().field shouldBe "code"
            errors.first().message shouldBe "coupon code must be provided"
        }

        "empty code produces error" {
            val errors = validateCoupon(validCoupon.copy(code = ""))
            errors shouldHaveSize 1
            errors.first().field shouldBe "code"
        }

        "zero discount produces error" {
            val errors = validateCoupon(validCoupon.copy(discount = 0.0))
            errors shouldHaveSize 1
            errors.first().field shouldBe "discount"
            errors.first().message shouldBe "discount must be a positive value"
        }

        "negative discount produces error" {
            val errors = validateCoupon(validCoupon.copy(discount = -5.0))
            errors shouldHaveSize 1
            errors.first().field shouldBe "discount"
        }

        "blank description produces error" {
            val errors = validateCoupon(validCoupon.copy(description = ""))
            errors shouldHaveSize 1
            errors.first().field shouldBe "description"
            errors.first().message shouldBe "coupon description must be provided"
        }

        "multiple invalid fields produce multiple errors" {
            val invalid = CouponModel(code = "", discount = -1.0, description = "")
            val errors = validateCoupon(invalid)
            errors shouldHaveSize 3
        }

        "null applicationCount is valid" {
            val errors = validateCoupon(validCoupon.copy(applicationCount = null))
            errors.shouldBeEmpty()
        }

        "validateCoupon with prefix prepends to field names" {
            val errors = validateCoupon(validCoupon.copy(code = ""), fieldPrefix = "coupons[0].")
            errors.first().field shouldBe "coupons[0].code"
        }

        "validateBatchCoupons uses indexed paths" {
            val coupons =
                listOf(
                    validCoupon,
                    validCoupon.copy(code = "", discount = -1.0),
                )
            val errors = validateBatchCoupons(coupons)
            errors shouldHaveSize 2
            errors.any { it.field == "coupons[1].code" } shouldBe true
            errors.any { it.field == "coupons[1].discount" } shouldBe true
        }

        "validateBatchCoupons with all valid coupons produces no errors" {
            val coupons = listOf(validCoupon, validCoupon.copy(code = "xyz"))
            validateBatchCoupons(coupons).shouldBeEmpty()
        }
    })
