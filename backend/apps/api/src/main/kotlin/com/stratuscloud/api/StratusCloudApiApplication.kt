package com.stratuscloud.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// API 애플리케이션의 시작점이다.
@SpringBootApplication(scanBasePackages = ["com.stratuscloud"])
class StratusCloudApiApplication

fun main(args: Array<String>) {
    runApplication<StratusCloudApiApplication>(*args)
}
