package com.example.aandi_post_web_server.course.controller

import com.example.aandi_post_web_server.assignment.dtos.AssignmentDeliveryResponse
import com.example.aandi_post_web_server.assignment.dtos.AssignmentDetailResponse
import com.example.aandi_post_web_server.assignment.dtos.CreateAssignmentRequest
import com.example.aandi_post_web_server.assignment.dtos.PublishAssignmentResponse
import com.example.aandi_post_web_server.assignment.dtos.TriggerDeliveriesResponse
import com.example.aandi_post_web_server.assignment.enum.AssignmentDeliveryStatus
import com.example.aandi_post_web_server.common.openapi.ApiErrorResponse
import com.example.aandi_post_web_server.course.dtos.CourseEnrollmentResponse
import com.example.aandi_post_web_server.course.dtos.CourseResponse
import com.example.aandi_post_web_server.course.dtos.CourseWeekResponse
import com.example.aandi_post_web_server.course.dtos.CreateCourseRequest
import com.example.aandi_post_web_server.course.dtos.CreateCourseWeekRequest
import com.example.aandi_post_web_server.course.dtos.EnrollCourseRequest
import com.example.aandi_post_web_server.course.dtos.UpdateCourseRequest
import com.example.aandi_post_web_server.course.dtos.UpdateEnrollmentRequest
import com.example.aandi_post_web_server.course.service.CourseV1Service
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "코스 관리자 API", description = "관리자 전용 코스/수강/과제 관리 API")
@RestController
@RequestMapping("/v1/admin/courses")
class CourseV1Controller(
    private val courseV1Service: CourseV1Service,
) {

    @Operation(summary = "코스 생성", description = "관리자가 코스를 생성합니다. phase와 targetTrack은 필수입니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "생성 성공", content = [Content(schema = Schema(implementation = CourseResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "slug 중복", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping
    fun createCourse(
        @Valid @RequestBody request: CreateCourseRequest,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<CourseResponse> {
        requireAdmin(userRole)
        return courseV1Service.createCourse(request)
    }

    @Operation(summary = "코스 수정", description = "코스 제목/설명/상태/phase/targetTrack을 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", content = [Content(schema = Schema(implementation = CourseResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PatchMapping("/{courseSlug}")
    fun updateCourse(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @RequestBody request: UpdateCourseRequest,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<CourseResponse> {
        requireAdmin(userRole)
        return courseV1Service.updateCourse(courseSlug, request)
    }

    @Operation(summary = "코스 아카이브", description = "코스를 ARCHIVED 상태로 변경합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "아카이브 성공"),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @DeleteMapping("/{courseSlug}")
    fun archiveCourse(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<Void> {
        requireAdmin(userRole)
        return courseV1Service.archiveCourse(courseSlug)
    }

    @Operation(summary = "수강생 등록/복구", description = "코스에 수강생을 ENROLLED 상태로 등록합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "등록 성공", content = [Content(schema = Schema(implementation = CourseEnrollmentResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping("/{courseSlug}/enrollments")
    fun enrollMember(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Valid @RequestBody request: EnrollCourseRequest,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<CourseEnrollmentResponse> {
        requireAdmin(userRole)
        return courseV1Service.enrollMember(courseSlug, request)
    }

    @Operation(summary = "수강 상태 변경", description = "ENROLLED/DROPPED/BANNED로 상태를 변경합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", content = [Content(schema = Schema(implementation = CourseEnrollmentResponse::class))]),
            ApiResponse(responseCode = "400", description = "요청값 오류(예: BANNED + banReason 누락)", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스 또는 수강생을 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PatchMapping("/{courseSlug}/enrollments/{userId}")
    fun updateEnrollmentStatus(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Parameter(description = "유저 ID", example = "user-1")
        @PathVariable userId: String,
        @RequestBody request: UpdateEnrollmentRequest,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<CourseEnrollmentResponse> {
        requireAdmin(userRole)
        return courseV1Service.updateEnrollmentStatus(courseSlug, userId, request)
    }

    @Operation(summary = "수강생 목록 조회", description = "해당 코스의 수강생 목록을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = CourseEnrollmentResponse::class)))],
            ),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @GetMapping("/{courseSlug}/enrollments")
    fun getEnrollments(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Flux<CourseEnrollmentResponse> {
        requireAdmin(userRole)
        return courseV1Service.getEnrollments(courseSlug)
    }

    @Operation(summary = "주차 생성/수정", description = "주차를 생성하거나 같은 weekNo가 있으면 갱신합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "처리 성공", content = [Content(schema = Schema(implementation = CourseWeekResponse::class))]),
            ApiResponse(responseCode = "400", description = "요청값 오류", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping("/{courseSlug}/weeks")
    fun createWeek(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Valid @RequestBody request: CreateCourseWeekRequest,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<CourseWeekResponse> {
        requireAdmin(userRole)
        return courseV1Service.createWeek(courseSlug, request)
    }

    @Operation(summary = "과제 생성", description = "코스 내부에 과제를 생성합니다. 생성 시 courseId/courseSlug가 함께 저장됩니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "생성 성공", content = [Content(schema = Schema(implementation = AssignmentDetailResponse::class))]),
            ApiResponse(responseCode = "400", description = "요청값 오류", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "동일 코스/주차/순번 중복", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping("/{courseSlug}/assignments")
    fun createAssignment(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Valid @RequestBody request: CreateAssignmentRequest,
        @Parameter(description = "요청자 유저 ID", example = "admin-user")
        @RequestHeader("X-User-Id") requesterId: String,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<AssignmentDetailResponse> {
        requireAdmin(userRole)
        return courseV1Service.createAssignment(courseSlug, request, requesterId)
    }

    @Operation(summary = "과제 게시", description = "DRAFT 과제를 PUBLISHED 상태로 게시합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "게시 성공", content = [Content(schema = Schema(implementation = PublishAssignmentResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스 또는 과제를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "422", description = "상태 전이 불가(예: ARCHIVED)", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping("/{courseSlug}/assignments/{assignmentId}/publish")
    fun publishAssignment(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Parameter(description = "과제 ID", example = "assignment-1")
        @PathVariable assignmentId: String,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<PublishAssignmentResponse> {
        requireAdmin(userRole)
        return courseV1Service.publishAssignment(courseSlug, assignmentId)
    }

    @Operation(summary = "과제 배포 트리거", description = "ENROLLED 대상에게 과제 배포를 수행합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "배포 처리 성공", content = [Content(schema = Schema(implementation = TriggerDeliveriesResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스 또는 과제를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "422", description = "PUBLISHED 상태가 아닌 과제", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping("/{courseSlug}/assignments/{assignmentId}/deliveries")
    fun triggerDeliveries(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Parameter(description = "과제 ID", example = "assignment-1")
        @PathVariable assignmentId: String,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Mono<TriggerDeliveriesResponse> {
        requireAdmin(userRole)
        return courseV1Service.triggerDeliveries(courseSlug, assignmentId)
    }

    @Operation(summary = "배포 결과 조회", description = "과제 배포 결과를 상태별로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = AssignmentDeliveryResponse::class)))],
            ),
            ApiResponse(responseCode = "400", description = "잘못된 status 파라미터", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "ADMIN 권한 아님", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "코스 또는 과제를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @GetMapping("/{courseSlug}/assignments/{assignmentId}/deliveries")
    fun getDeliveries(
        @Parameter(description = "코스 슬러그", example = "back-basic")
        @PathVariable courseSlug: String,
        @Parameter(description = "과제 ID", example = "assignment-1")
        @PathVariable assignmentId: String,
        @Parameter(description = "배포 상태 필터", example = "DELIVERED")
        @RequestParam(required = false) status: AssignmentDeliveryStatus?,
        @Parameter(description = "사용자 권한", example = "ADMIN")
        @RequestHeader("X-Roles") userRole: String,
    ): Flux<AssignmentDeliveryResponse> {
        requireAdmin(userRole)
        return courseV1Service.getDeliveries(courseSlug, assignmentId, status)
    }

    private fun requireAdmin(rolesHeader: String) {
        val roles = rolesHeader
            .split(",")
            .map(::normalizeRole)
            .filter { it.isNotBlank() }
            .toSet()

        if ("ADMIN" !in roles) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "허용되지 않은 권한입니다.")
        }
    }

    private fun normalizeRole(role: String): String = role.trim().uppercase().removePrefix("ROLE_")
}
