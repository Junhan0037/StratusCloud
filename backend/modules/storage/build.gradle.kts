plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:3.4.3")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.3")
    implementation(project(":modules:iam"))
    implementation(project(":modules:governance"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.3")
}
