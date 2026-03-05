package com.example.aandi_post_web_server.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class JacksonTimeConfigTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    init {
        "Instant는 KST 오프셋(+09:00)으로 직렬화된다" {
            val payload = mapOf("submittedAt" to Instant.parse("2026-03-01T00:00:00Z"))

            val serialized = objectMapper.writeValueAsString(payload)

            serialized shouldContain "\"submittedAt\":\"2026-03-01T09:00:00+09:00\""
            serialized shouldNotContain "2026-03-01T00:00:00Z"
        }
    }
}
