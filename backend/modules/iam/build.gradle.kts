plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:3.4.3")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.3")
    implementation("org.flywaydb:flyway-core:10.22.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.3")
}
