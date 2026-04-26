rootProject.name = "service"

pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        val ktorVersion: String by settings
        val sonarqubeVersion: String by settings
        val ktlintPluginVersion: String by settings
        kotlin("jvm") version kotlinVersion
        id("io.ktor.plugin") version ktorVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        jacoco
        id("org.sonarqube") version sonarqubeVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintPluginVersion
    }
}
