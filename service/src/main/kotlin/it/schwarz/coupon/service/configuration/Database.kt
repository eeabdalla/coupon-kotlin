package it.schwarz.coupon.service.configuration

import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.jsr310.LocalDateCodec

class Database {
    private val logger = KotlinLogging.logger {}

    fun configureDatabase(
        dbURI: String,
        dbName: String,
    ): MongoDatabase {
        logger.debug { "configuring database" }

        val codecRegistry =
            CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(LocalDateCodec()),
            )

        val client = MongoClient.create(dbURI)
        val database =
            client
                .getDatabase(dbName)
                .withCodecRegistry(codecRegistry)

        runBlocking {
            val doc =
                database.runCommand(
                    Document("ping", 1),
                )
            logger.trace { "$doc received as Ping-result" }
        }
        return database
    }
}
