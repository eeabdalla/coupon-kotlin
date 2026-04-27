package it.schwarz.coupon.cleanup.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CleanupRunner(
    private vararg val cleanupRunners: CollectionCleanupRunner,
) {
    private val logger = KotlinLogging.logger { }
    private val scope = CoroutineScope(Dispatchers.Default)

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) {
            logger.warn { "CleanupRunner is already running!" }
            return
        }
        job =
            scope.launch {
                cleanupRunners.forEach { it.doCleanup() }
            }
        logger.info { "CleanupRunner started" }
    }

    fun isRunning(): Boolean = job?.isActive == true
}
