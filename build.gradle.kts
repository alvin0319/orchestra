import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    kotlin("jvm") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jmailen.kotlinter") version "3.13.0"
    kotlin("kapt") version "1.8.10"
}

group = "dev.minjae.orchestra"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        name = "m2-dv8tion"
        url = uri("https://m2.dv8tion.net/releases")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("net.dv8tion:JDA:5.0.0-beta.3")
    implementation("com.github.walkyst:lavaplayer-fork:e833a69a10c852dfeaecf1c43b3ca0732c703baa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("com.fasterxml.jackson.module:jackson-module-blackbird:2.14.2")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("com.github.minndevelopment:jda-ktx:0.10.0-beta.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.requery:requery:1.6.0")
    kapt("io.requery:requery-processor:1.6.0")
    implementation("io.requery:requery-kotlin:1.6.0")
    implementation("mysql:mysql-connector-java:8.0.32")
    implementation("org.json:json:20220924")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

tasks.create<LintTask>("ktLint") {
    group = "verification"
    source(files("src"))
}

tasks.create<FormatTask>("ktFormat") {
    group = "formatting"
    source(files("src"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        dependsOn("ktLint")
        manifest {
            attributes(mapOf("Main-Class" to "dev.minjae.orchestra.BootstrapKt"))
        }
    }
}
