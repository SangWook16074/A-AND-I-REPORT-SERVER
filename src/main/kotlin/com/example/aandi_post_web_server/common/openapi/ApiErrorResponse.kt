package com.example.aandi_post_web_server.common.openapi

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공통 에러 응답")
data class ApiErrorResponse(
    @field:Schema(description = "HTTP 상태 코드", example = "404")
    val status: Int,
    @field:Schema(description = "에러 이름", example = "Not Found")
    val error: String,
    @field:Schema(description = "상세 메시지", example = "코스를 찾을 수 없습니다: back-basic")
    val message: String? = null,
    @field:Schema(description = "요청 경로", example = "/v1/courses/back-basic")
    val path: String? = null,
)
