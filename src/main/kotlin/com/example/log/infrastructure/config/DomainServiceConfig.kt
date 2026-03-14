package com.example.log.infrastructure.config

import com.example.log.domain.service.SimpleSummarizer
import com.example.log.domain.service.Summarizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainServiceConfig {

    @Bean
    fun summarizer(): Summarizer {
        // ドメイン層の純粋なクラスを、インフラ層の設定で Bean 化する
        return SimpleSummarizer(maxLength = 500)
    }
}
