plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("kapt") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "comp5331"
version = "1.0-SNAPSHOT"

val appEntryPoint = "comp5331.MainKt"

repositories {
    mavenCentral()
}

dependencies {
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.11.0")

    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.moshi:moshi:1.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
}

tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}.jar")

        manifest {
            attributes.apply {
                this["Main-Class"] = appEntryPoint
            }
        }
    }
    wrapper {
        gradleVersion = "6.7"
    }
}
