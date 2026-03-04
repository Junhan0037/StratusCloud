package com.stratuscloud.api

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

// API 애플리케이션의 시작점이다.
@SpringBootApplication(scanBasePackages = ["com.stratuscloud"])
@EntityScan(basePackages = ["com.stratuscloud.iam.domain"])
@EnableJpaRepositories(basePackages = ["com.stratuscloud.iam.repository"])
class StratusCloudApiApplication

fun main(args: Array<String>) {
    runApplication<StratusCloudApiApplication>(*args)
}
