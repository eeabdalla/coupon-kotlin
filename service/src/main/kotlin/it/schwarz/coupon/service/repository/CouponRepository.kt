package it.schwarz.coupon.service.repository

import it.schwarz.coupon.service.model.CouponModel

interface CouponRepository {
    suspend fun findAll(): List<CouponModel>

    suspend fun findByCodes(codes: List<String>): List<CouponModel>

    suspend fun save(coupon: CouponModel)
}
