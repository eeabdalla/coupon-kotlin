package it.schwarz.coupon.service.validation

import it.schwarz.coupon.service.model.CouponModel
import it.schwarz.coupon.service.model.FieldError

fun validateCoupon(
    coupon: CouponModel,
    fieldPrefix: String = "",
): List<FieldError> {
    val errors = mutableListOf<FieldError>()

    if (coupon.code.isBlank()) {
        errors.add(
            FieldError(
                field = "${fieldPrefix}code",
                message = "coupon code must be provided",
                rejectedValue = coupon.code.ifEmpty { null },
            ),
        )
    }

    if (coupon.discount <= 0) {
        errors.add(
            FieldError(
                field = "${fieldPrefix}discount",
                message = "discount must be a positive value",
                rejectedValue = coupon.discount.toString(),
            ),
        )
    }

    if (coupon.description.isBlank()) {
        errors.add(
            FieldError(
                field = "${fieldPrefix}description",
                message = "coupon description must be provided",
                rejectedValue = coupon.description.ifEmpty { null },
            ),
        )
    }

    return errors
}

fun validateBatchCoupons(coupons: List<CouponModel>): List<FieldError> =
    coupons.flatMapIndexed { index, coupon ->
        validateCoupon(coupon, fieldPrefix = "coupons[$index].")
    }
