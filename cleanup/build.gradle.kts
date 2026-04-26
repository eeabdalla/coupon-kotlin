import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val logstashVersion: String by project
val kotestVersion: String by project
val mockkVersion: String by project
val ktlintVersion: String by project
val sonarqubeVersion: String by project
val koinVersion: String by project
val kotlinxDatetimeVersion: String by project
val julToSlfjVersion: String by project
val jsonKotlinSchemaVersion: String by project
val kotlinLoggingVersion: String by project
val mongoVersion: String by project

plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.plugin.serialization")
    jacoco
    id("org.sonarqube")
}

group = "schwarz.it"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf(
        "-Dio.ktor.development=$isDevelopment",
        "--enable-native-access=ALL-UNNAMED",
        "-Dio.netty.transport.noNative=true",
    )
}

kotlin { }

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

ktor {
    fatJar {
        archiveFileName.set("app.jar")
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Force patched jackson-core to fix CVEs in transitive dep from logstash-logback-encoder
    constraints {
        implementation("tools.jackson.core:jackson-databind:3.1.2")
        implementation("tools.jackson.core:jackson-core:3.1.2")
    }

    // KTOR
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-openapi:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")

    // Serialization:
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")

    // Koin:
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    // Mongo DB
    implementation("org.mongodb:mongodb-driver-kotlin-sync:$mongoVersion")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:$mongoVersion")
    implementation("org.mongodb:bson:$mongoVersion")
    implementation("org.mongodb:bson-kotlinx:$mongoVersion")

    // Bridge from java jul logging to slf (logback) logging:
    implementation("org.slf4j:jul-to-slf4j:$julToSlfjVersion")

    // Json schema validation:
    implementation("net.pwall.json:json-kotlin-schema:$jsonKotlinSchemaVersion")

    // Http client
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    // Unit Testing & Mocking
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    // Koin tests:
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit4:$koinVersion")
    // Mock for client requests
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}

jacoco {
    reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco/"))
}

with(tasks) {
    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            csv.required.set(false)
            html.required.set(true)
            xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/report.xml"))
            html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
        }
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(ktlintVersion)
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/generated/**")
        exclude("**/style-violations.kt")
        include("**/kotlin/**")
    }
}

sonarqube {
    val exclusions =
        listOf(
            "**/src/main/kotlin/FileToExclude.kt",
        )
    val cpd = "**/src/main/kotlin/core/domain/*.kt"
    properties {
        property("sonar.coverage.exclusions", exclusions)
        property("sonar.cpd.exclusions", cpd)
        property("sonar.projectName", "kotlin-ktor")
    }
}
