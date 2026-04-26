package service

import com.mongodb.client.model.Filters
import io.kotest.core.spec.style.StringSpec
import io.mockk.mockk
import it.schwarz.coupon.cleanup.repository.DocumentRepository
import it.schwarz.coupon.cleanup.service.CleanupRunner
import it.schwarz.coupon.cleanup.service.CollectionCleanupRunner
import kotlinx.coroutines.delay
import org.bson.conversions.Bson
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class CollectionCleanupRunnerTest :
    StringSpec({

        "test do cleanup" {
            val runners =
                listOf(
                    TestCouponCleanupRunner(100.milliseconds),
                    TestCouponCleanupRunner(50.milliseconds),
                    TestCouponCleanupRunner(75.milliseconds),
                    TestCouponCleanupRunner(25.milliseconds),
                )

            val cleanup = CleanupRunner(runners[0], runners[1], runners[2], runners[3])

            cleanup.start()
            // wait to be finished
            while (cleanup.isRunning()) {
                delay(50.milliseconds)
            }

            runners.forEach { assert(it.cleaned) }
        }
    }) {
    class TestCouponCleanupRunner(
        val delayDuration: Duration,
    ) : CollectionCleanupRunner(mockk<DocumentRepository>()) {
        var cleaned = false
            private set

        override fun getCollectionName(): String = "test-collection"

        override fun getFilter(): Bson = Filters.exists("_id")

        override suspend fun doCleanup() {
            delay(delayDuration)
            cleaned = true
        }
    }
}
