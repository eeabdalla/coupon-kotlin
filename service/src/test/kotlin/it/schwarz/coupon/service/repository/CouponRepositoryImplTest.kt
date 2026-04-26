package it.schwarz.coupon.service.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.result.InsertOneResult
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import it.schwarz.coupon.service.model.CouponModel
import kotlinx.coroutines.flow.FlowCollector
import org.bson.BsonObjectId
import org.bson.Document
import org.bson.conversions.Bson

class CouponRepositoryImplTest :
    StringSpec({

        val collection = mockk<MongoCollection<Document>>()
        val mongoDatabase = mockk<MongoDatabase>()

        beforeSpec {
            every { mongoDatabase.getCollection<Document>("coupons") } returns collection
        }

        val repository by lazy { CouponRepositoryImpl(mongoDatabase) }

        val sampleDocument =
            Document()
                .append("code", "abc123")
                .append("discount", 12.54)
                .append("description", "best coupon")
                .append("applicationCount", 0)

        val expectedCoupon =
            CouponModel(
                code = "abc123",
                discount = 12.54,
                description = "best coupon",
                applicationCount = 0,
            )

        fun mockFindFlow(vararg docs: Document): FindFlow<Document> {
            val findFlow = mockk<FindFlow<Document>>()
            coEvery { findFlow.collect(any()) } coAnswers {
                val collector = firstArg<FlowCollector<Document>>()
                docs.forEach { collector.emit(it) }
            }
            return findFlow
        }

        "findAll returns all coupons mapped from documents" {
            every { collection.find() } returns mockFindFlow(sampleDocument)

            val result = repository.findAll()

            result shouldHaveSize 1
            result.first() shouldBe expectedCoupon
        }

        "findAll returns empty list when no documents" {
            every { collection.find() } returns mockFindFlow()

            val result = repository.findAll()

            result.shouldBeEmpty()
        }

        "findByCodes returns filtered coupons" {
            val codes = listOf("abc123", "xyz789")
            val filterSlot = slot<Bson>()

            every { collection.find(capture(filterSlot)) } returns mockFindFlow(sampleDocument)

            val result = repository.findByCodes(codes)

            result shouldHaveSize 1
            result.first().code shouldBe "abc123"
            filterSlot.captured shouldBe Filters.`in`("code", codes)
        }

        "findByCodes returns empty list when no matches" {
            every { collection.find(any<Bson>()) } returns mockFindFlow()

            val result = repository.findByCodes(listOf("unknown"))

            result.shouldBeEmpty()
        }

        "save inserts document with correct fields" {
            val coupon =
                CouponModel(
                    code = "new123",
                    discount = 9.99,
                    description = "new coupon",
                    applicationCount = 5,
                )

            val documentSlot = slot<Document>()
            coEvery { collection.insertOne(capture(documentSlot), any()) } returns
                InsertOneResult.acknowledged(BsonObjectId())

            repository.save(coupon)

            val savedDoc = documentSlot.captured
            savedDoc.getString("code") shouldBe "new123"
            savedDoc.getDouble("discount") shouldBe 9.99
            savedDoc.getString("description") shouldBe "new coupon"
            savedDoc.getInteger("applicationCount") shouldBe 5
            savedDoc.containsKey("creationDateTime") shouldBe true
        }

        "save inserts document with null applicationCount" {
            val coupon =
                CouponModel(
                    code = "nullcount",
                    discount = 5.0,
                    description = "no count",
                    applicationCount = null,
                )

            val documentSlot = slot<Document>()
            coEvery { collection.insertOne(capture(documentSlot), any()) } returns
                InsertOneResult.acknowledged(BsonObjectId())

            repository.save(coupon)

            val savedDoc = documentSlot.captured
            savedDoc.getString("code") shouldBe "nullcount"
            savedDoc["applicationCount"] shouldBe null
        }
    })
