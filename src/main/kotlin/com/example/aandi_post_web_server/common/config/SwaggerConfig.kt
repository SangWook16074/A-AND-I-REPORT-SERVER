package com.example.aandi_post_web_server.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig(
    @Value("\${swagger.server.url}") private val serverUrl: String,
) {
    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI()
            .addServersItem(Server().url(serverUrl))
            .info(swaggerInfo())
    }

    private fun swaggerInfo(): Info = Info()
        .title("Report Service API")
        .description("코스/과제 운영 API 문서")
        .version("v1")
        .license(License().name("Proprietary"))
}
