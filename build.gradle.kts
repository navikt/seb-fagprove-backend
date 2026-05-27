plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
}

group = "no.nav"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

ktor {
    fatJar {
        archiveFileName.set("app.jar")
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.cors)
    implementation(libs.kotlin.reflect)
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
