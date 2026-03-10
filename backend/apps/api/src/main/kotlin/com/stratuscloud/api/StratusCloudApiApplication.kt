package com.stratuscloud.api

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// API 애플리케이션의 시작점이다.
@SpringBootApplication(scanBasePackages = ["com.stratuscloud"])
@EntityScan(
    basePackages = [
        "com.stratuscloud.iam.domain",
        "com.stratuscloud.audit.domain",
        "com.stratuscloud.compute.domain",
        "com.stratuscloud.network.domain",
        "com.stratuscloud.storage.domain"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "com.stratuscloud.iam.repository",
        "com.stratuscloud.audit.repository",
        "com.stratuscloud.compute.repository",
        "com.stratuscloud.network.repository",
        "com.stratuscloud.storage.repository"
    ]
)
class StratusCloudApiApplication

fun main(args: Array<String>) {
    runApplication<StratusCloudApiApplication>(*args)
}
