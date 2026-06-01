plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.example"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// --- Coverage (JaCoCo) ---------------------------------------------------------------------
// 100% LINE gate over the MF *logic* (services, controllers, mappers, security, config). JPA
// entities, Spring Data repositories, request/response DTOs and the bootstrap are excluded.
jacoco {
    toolVersion = "0.8.13"
}

val coverageIncludes = listOf("com/example/mfservice/**")
val coverageExcludes = listOf(
    "**/MfServiceApplication*",            // @SpringBootApplication + main()
    "com/example/mfservice/domain/**",      // JPA entities
    "com/example/mfservice/repository/**",  // Spring Data repositories (generated)
    "com/example/mfservice/web/dto/**",     // request/response data classes
)

fun filteredCoverageDirs(): FileCollection =
    files(sourceSets.main.get().output.classesDirs.files.map { dir ->
        fileTree(dir) {
            include(coverageIncludes)
            exclude(coverageExcludes)
        }
    })

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(filteredCoverageDirs())
    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(filteredCoverageDirs())
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
