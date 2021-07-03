import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.5.10"
}

group = "me.sergey"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.0")
    implementation("org.postgresql:postgresql:42.2.10")
    implementation(kotlin("reflect"))

    implementation("org.atteo:evo-inflector:1.2.2")

    implementation("org.tinylog:tinylog-api:2.4.0-M1")
    implementation("org.tinylog:tinylog-impl:2.4.0-M1")

    testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
