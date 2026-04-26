package it.schwarz.coupon.service.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import it.schwarz.coupon.service.model.CouponModel
import it.schwarz.coupon.service.repository.CouponRepository

class CouponServiceTest :
    StringSpec({

        val couponRepository = mockk<CouponRepository>()
        val couponService = CouponService(couponRepository)

        val sampleCoupon =
            CouponModel(
                code = "abc123",
                discount = 12.54,
                description = "best coupon",
                applicationCount = 0,
            )

        "getCoupons with no codes returns all coupons" {
            coEvery { couponRepository.findAll() } returns listOf(sampleCoupon)

            val result = couponService.getCoupons(null)

            result shouldHaveSize 1
            result.first().code shouldBe "abc123"
            coVerify { couponRepository.findAll() }
        }

        "getCoupons with empty codes returns all coupons" {
            coEvery { couponRepository.findAll() } returns listOf(sampleCoupon)

            val result = couponService.getCoupons(emptyList())

            result shouldHaveSize 1
            coVerify { couponRepository.findAll() }
        }

        "getCoupons with codes filters by codes" {
            val codes = listOf("abc123")
            coEvery { couponRepository.findByCodes(codes) } returns listOf(sampleCoupon)

            val result = couponService.getCoupons(codes)

            result shouldHaveSize 1
            result.first().code shouldBe "abc123"
            coVerify { couponRepository.findByCodes(codes) }
        }

        "getCoupons with unknown codes returns empty list" {
            val codes = listOf("unknown")
            coEvery { couponRepository.findByCodes(codes) } returns emptyList()

            val result = couponService.getCoupons(codes)

            result.shouldBeEmpty()
            coVerify { couponRepository.findByCodes(codes) }
        }

        "createCoupon delegates to repository" {
            coEvery { couponRepository.save(sampleCoupon) } returns Unit

            couponService.createCoupon(sampleCoupon)

            coVerify { couponRepository.save(sampleCoupon) }
        }
    })
