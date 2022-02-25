import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    application
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
    id("org.jetbrains.dokka") version "1.6.10"
}
apply(plugin = "maven-publish")

group = "com.github.SergeyHSE7"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation("org.postgresql:postgresql:42.3.2")
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.4")

    implementation("org.atteo:evo-inflector:1.3")
    implementation("org.tinylog:tinylog-api:2.4.1")
    implementation("org.tinylog:tinylog-impl:2.4.1")

    testImplementation("io.kotest:kotest-runner-junit5:4.6.0") // Do not upgrade
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.withType<Test> {
    dependsOn("createPostgresDB", "createMariaDB")
    useJUnitPlatform()
    finalizedBy("deletePostgresDB", "deleteMariaDB")
}

task<Exec>("createPostgresDB") {
    commandLine(
        ("docker run --name test_postgres " +
                "-p 5432:5432 -d " +
                "-e POSTGRES_USER=user " +
                "-e POSTGRES_PASSWORD=password " +
                "-e POSTGRES_DB=test_db " +
                "postgres").split(' ')
    )
}

task<Exec>("createMariaDB") {
    commandLine(
        ("docker run --name test_mariadb " +
                "-p 3306:3306 -d " +
                "-e MARIADB_USER=user " +
                "-e MARIADB_PASSWORD=password " +
                "-e MARIADB_ROOT_PASSWORD=password " +
                "-e MARIADB_DATABASE=test_db " +
                "mariadb").split(' ')
    )
}

task<Exec>("deletePostgresDB") {
    commandLine("docker rm -f test_postgres".split(' '))
}
task<Exec>("deleteMariaDB") {
    commandLine("docker rm -f test_mariadb".split(' '))
}
