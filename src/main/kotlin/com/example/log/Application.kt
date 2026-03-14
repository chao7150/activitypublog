package com.example.log

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.example.log"])
@EntityScan(basePackages = ["com.example.log.infrastructure.persistence.jpa.entity"])
@EnableJpaRepositories(basePackages = ["com.example.log.infrastructure.persistence.jpa.repository"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
