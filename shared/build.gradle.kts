val kotlinVersion: String by project
val mongoVersion: String by project
val kotlinLoggingVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm")
}

group = "schwarz.it"
version = "0.0.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:$mongoVersion")
    implementation("org.mongodb:bson:$mongoVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}

