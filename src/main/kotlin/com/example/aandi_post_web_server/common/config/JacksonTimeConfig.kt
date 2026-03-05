package com.example.aandi_post_web_server.common.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Configuration
class JacksonTimeConfig {

    @Bean
    fun kstInstantJacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.serializerByType(Instant::class.java, KstInstantSerializer)
        }

    private object KstInstantSerializer : JsonSerializer<Instant>() {
        private val zoneId: ZoneId = ZoneId.of("Asia/Seoul")
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

        override fun serialize(value: Instant?, gen: JsonGenerator, serializers: SerializerProvider) {
            if (value == null) {
                gen.writeNull()
                return
            }
            gen.writeString(formatter.format(value.atZone(zoneId)))
        }
    }
}
