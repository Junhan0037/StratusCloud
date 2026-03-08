plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":modules:iam"))
    implementation("org.springframework.boot:spring-boot-starter:3.4.3")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.3")
}
