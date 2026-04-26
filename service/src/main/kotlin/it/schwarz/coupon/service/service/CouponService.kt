package it.schwarz.coupon.service.service

import io.github.oshai.kotlinlogging.KotlinLogging
import it.schwarz.coupon.service.model.CouponModel
import it.schwarz.coupon.service.repository.CouponRepository

class CouponService(
    private val couponRepository: CouponRepository,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun getCoupons(codes: List<String>?): List<CouponModel> =
        if (codes.isNullOrEmpty()) {
            logger.info { "Fetching all coupons" }
            couponRepository.findAll()
        } else {
            logger.info { "Fetching coupons by codes: $codes" }
            couponRepository.findByCodes(codes)
        }

    suspend fun createCoupon(coupon: CouponModel) {
        logger.info { "Creating coupon: ${coupon.code}" }
        couponRepository.save(coupon)
    }

    suspend fun createCoupons(coupons: List<CouponModel>) {
        logger.info { "Creating ${coupons.size} coupons in batch" }
        couponRepository.saveAll(coupons)
    }
}
