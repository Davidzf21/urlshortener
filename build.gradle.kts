import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.7.3" apply false
    id("io.spring.dependency-management") version "1.0.13.RELEASE" apply false
    kotlin("jvm") version "1.7.10" apply false
    kotlin("plugin.spring") version "1.7.10" apply false
    kotlin("plugin.jpa") version "1.7.10" apply false
}

group = "es.unizar"
version = "0.2022.1-SNAPSHOT"

var mockitoVersion = "4.0.0"
var bootstrapVersion = "3.4.0"
var jqueryVersion = "3.6.1"
var guavaVersion = "31.1-jre"
var commonsValidatorVersion = "1.6"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
    }
    repositories {
        mavenCentral()
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }
}

project(":core") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-websocket")
        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("ru.chermenin:kotlin-user-agents:0.2.2")
        "implementation"("org.springframework.amqp:spring-rabbit")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":repositories") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    dependencies {
        "implementation"(project(":core"))
        "implementation"(project(":delivery"))
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
        "testImplementation"("org.springframework.boot:spring-boot-starter-web")
        "testImplementation"("org.springframework.boot:spring-boot-starter-jdbc")
        "testImplementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "testImplementation"("org.apache.httpcomponents:httpclient")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":delivery") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"(project(":core"))
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-hateoas")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("commons-validator:commons-validator:$commonsValidatorVersion")
        "implementation"("com.google.guava:guava:$guavaVersion")
        "implementation"("org.springframework.boot:spring-boot-starter-thymeleaf:2.5.3")
        "implementation"("ru.chermenin:kotlin-user-agents:0.2.2")

        "implementation"("org.springdoc:springdoc-openapi-ui:1.6.14")

        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")

        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("io.micrometer:micrometer-registry-prometheus:latest.release")

    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":app") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"(project(":core"))
        "implementation"(project(":delivery"))
        "implementation"(project(":repositories"))
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter")
        "implementation"( "org.webjars:bootstrap:$bootstrapVersion")
        "implementation"("org.webjars:jquery:$jqueryVersion")
        "implementation"("ru.chermenin:kotlin-user-agents:0.2.2")
        "implementation"("org.springframework.amqp:spring-rabbit")
        "runtimeOnly"("org.hsqldb:hsqldb")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.springframework.boot:spring-boot-starter-web")
        "testImplementation"("org.springframework.boot:spring-boot-starter-jdbc")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
        "testImplementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "testImplementation"("org.apache.httpcomponents:httpclient")
    }
}

repositories {
    mavenCentral()
}
