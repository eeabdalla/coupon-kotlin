package it.schwarz.coupon.service.model

import kotlinx.serialization.Serializable

@Serializable
data class BatchCouponRequest(
    val coupons: List<CouponModel>? = null,
)
