import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.0.4"
}

group = "com.vaibhav"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.aspectj:aspectjweaver:1.9.24")

    // Fault tolerance (Java 21-native Resilience4j core).
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.4.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.4.0")
    implementation("io.github.resilience4j:resilience4j-bulkhead:2.4.0")
    implementation("io.github.resilience4j:resilience4j-timelimiter:2.4.0")
    implementation("io.github.resilience4j:resilience4j-micrometer:2.4.0")

    // JWT (access tokens). Nimbus is the library Spring Security itself uses.
    implementation("com.nimbusds:nimbus-jose-jwt:10.0.1")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-restclient")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.wiremock:wiremock-standalone:3.13.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/**
 * Exports the OpenAPI document to build/openapi/openapi.json by booting the
 * application on a random port against an ephemeral Postgres container.
 * Consumed by scripts/generate-api-client to produce the TypeScript client,
 * which is why the frontend never hand-writes API types.
 */
val exportOpenApi by
    tasks.registering(Test::class) {
        description = "Writes the OpenAPI document to build/openapi/openapi.json"
        group = "documentation"
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform()
        filter { includeTestsMatching("com.vaibhav.runbookos.OpenApiDocumentExportTest") }
        systemProperty("runbookos.openapi.export", "true")
        outputs.file(layout.buildDirectory.file("openapi/openapi.json"))
    }

tasks.register<Copy>("syncApiContract") {
    description = "Exports Springdoc and updates the generated frontend API contract input"
    group = "documentation"
    dependsOn(exportOpenApi)
    from(layout.buildDirectory.file("openapi/openapi.json"))
    into(layout.projectDirectory.dir("../../packages/api-client"))
}

spotless {
    java {
        googleJavaFormat("1.25.2")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

tasks.named<Test>("test") {
    filter { excludeTestsMatching("com.vaibhav.runbookos.OpenApiDocumentExportTest") }
}
