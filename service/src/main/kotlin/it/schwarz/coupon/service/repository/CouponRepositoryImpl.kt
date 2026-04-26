package it.schwarz.coupon.service.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import it.schwarz.coupon.service.model.CouponModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.Document
import java.time.Instant

class CouponRepositoryImpl(
    mongoDatabase: MongoDatabase,
) : CouponRepository {
    private val logger = KotlinLogging.logger {}
    private val collection = mongoDatabase.getCollection<Document>(COLLECTION_NAME)

    companion object {
        const val COLLECTION_NAME = "coupons"
    }

    override suspend fun findAll(): List<CouponModel> {
        logger.debug { "Finding all coupons" }
        return collection
            .find()
            .map { it.toCouponModel() }
            .toList()
    }

    override suspend fun findByCodes(codes: List<String>): List<CouponModel> {
        logger.debug { "Finding coupons by codes: $codes" }
        return collection
            .find(Filters.`in`("code", codes))
            .map { it.toCouponModel() }
            .toList()
    }

    override suspend fun save(coupon: CouponModel) {
        logger.debug { "Saving coupon: ${coupon.code}" }
        val document =
            Document()
                .append("code", coupon.code)
                .append("discount", coupon.discount)
                .append("description", coupon.description)
                .append("applicationCount", coupon.applicationCount)
                .append("creationDateTime", Instant.now())
        collection.insertOne(document)
    }

    private fun Document.toCouponModel(): CouponModel =
        CouponModel(
            code = getString("code"),
            discount = getDouble("discount"),
            description = getString("description"),
            applicationCount = getInteger("applicationCount"),
        )
}
