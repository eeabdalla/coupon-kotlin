package it.schwarz.coupon.service.configuration

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.install
import it.schwarz.coupon.service.repository.CouponRepository
import it.schwarz.coupon.service.repository.CouponRepositoryImpl
import it.schwarz.coupon.service.service.CouponService
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.KoinApplicationStarted
import org.koin.ktor.plugin.KoinApplicationStopPreparing
import org.koin.ktor.plugin.KoinApplicationStopped
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    val logger = KotlinLogging.logger {}

    logger.debug { "configuring dependency injection" }

    install(Koin) {
        slf4jLogger()

        val appModule =
            module(createdAtStart = true) {
                val uri = requireNotNull(System.getenv("MONGODB_URI")) { "database URI not configured" }
                val name = requireNotNull(System.getenv("DATABASE_NAME")) { "database name not configured" }

                val mongoDatabase = Database().configureDatabase(uri, name)

                single<CouponRepository> {
                    CouponRepositoryImpl(mongoDatabase)
                }

                single {
                    CouponService(get<CouponRepository>())
                }
            }
        modules(appModule)
    }

    @Suppress("UNUSED")
    with(monitor) {
        subscribe(KoinApplicationStarted) { logger.info { "Koin started" } }
        subscribe(KoinApplicationStopPreparing) { logger.info { "Koin stopping" } }
        subscribe(KoinApplicationStopped) { logger.info { "Koin stopped" } }
    }
}
