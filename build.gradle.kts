plugins {
    java
    kotlin("jvm") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "com.alekseysamoylov.dealer.MainKt"
}

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.nats:jnats:2.6.6")
    implementation("com.google.protobuf:protobuf-java:3.10.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}
