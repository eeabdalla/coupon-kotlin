package it.schwarz.coupon.service.model

import kotlinx.serialization.Serializable

@Serializable
data class CouponModel(
    val code: String,
    val discount: Double,
    val description: String,
    val applicationCount: Int? = null,
)
