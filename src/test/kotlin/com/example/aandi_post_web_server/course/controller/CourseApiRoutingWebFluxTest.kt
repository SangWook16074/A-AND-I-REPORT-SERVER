package com.example.aandi_post_web_server.course.controller

import com.example.aandi_post_web_server.assignment.dtos.AssignmentDeliveryResponse
import com.example.aandi_post_web_server.assignment.dtos.AssignmentDetailResponse
import com.example.aandi_post_web_server.assignment.enum.AssignmentDeliveryStatus
import com.example.aandi_post_web_server.assignment.enum.AssignmentDifficulty
import com.example.aandi_post_web_server.assignment.enum.AssignmentStatus
import com.example.aandi_post_web_server.course.dtos.CreateCourseRequest
import com.example.aandi_post_web_server.course.dtos.CourseResponse
import com.example.aandi_post_web_server.course.enum.CoursePhase
import com.example.aandi_post_web_server.course.enum.CourseStatus
import com.example.aandi_post_web_server.course.enum.CourseTrack
import com.example.aandi_post_web_server.course.enum.UserTrack
import com.example.aandi_post_web_server.course.service.CourseV1Service
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@WebFluxTest(controllers = [CourseV1Controller::class, CourseQueryV1Controller::class])
class CourseApiRoutingWebFluxTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var courseV1Service: CourseV1Service

    init {
        beforeTest {
            Mockito.reset(courseV1Service)
        }

        "public 조회 API는 권한 헤더 없이 호출 가능하다" {
            val response = sampleCourseResponse()
            Mockito.`when`(courseV1Service.getCourses(null, null, null)).thenReturn(Flux.just(response))

            webTestClient.get()
                .uri("/v1/courses")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].slug").isEqualTo("back-basic")
        }

        "public 과제 상세 조회 API는 권한 헤더 없이 호출 가능하다" {
            val response = sampleAssignmentDetailResponse()
            Mockito.`when`(courseV1Service.getAssignmentDetail("back-basic", "assignment-1"))
                .thenReturn(Mono.just(response))

            webTestClient.get()
                .uri("/v1/courses/back-basic/assignments/assignment-1")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.id").isEqualTo("assignment-1")
        }

        "과제 ID로 코스 조회 API는 권한 헤더 없이 호출 가능하다" {
            val response = sampleCourseResponse()
            Mockito.`when`(courseV1Service.getAssignmentCourse("assignment-1"))
                .thenReturn(Mono.just(response))

            webTestClient.get()
                .uri("/v1/courses/assignments/assignment-1/course")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.slug").isEqualTo("back-basic")
        }

        "코스 조회 API는 track 쿼리 파라미터로 필터링 호출한다" {
            val response = sampleCourseResponse()
            Mockito.`when`(
                courseV1Service.getCourses(
                    status = null,
                    phase = null,
                    track = UserTrack.FL,
                )
            ).thenReturn(Flux.just(response))

            webTestClient.get()
                .uri("/v1/courses?track=FL")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].targetTrack").isEqualTo("FL")
        }

        "admin API는 ADMIN이 아니면 403을 반환한다" {
            webTestClient.post()
                .uri("/v1/admin/courses")
                .header("X-User-Role", "USER")
                .bodyValue(
                    mapOf(
                        "title" to "BACK 기초",
                        "slug" to "back-basic",
                        "description" to "desc",
                        "phase" to "BASIC",
                        "targetTrack" to "FL",
                    )
                )
                .exchange()
                .expectStatus().isForbidden
        }

        "admin API는 ADMIN 헤더로 호출하면 성공한다" {
            val response = sampleCourseResponse()
            Mockito.`when`(
                courseV1Service.createCourse(
                    CreateCourseRequest(
                        title = "BACK 기초",
                        slug = "back-basic",
                        description = "desc",
                        phase = CoursePhase.BASIC,
                        targetTrack = CourseTrack.FL,
                    )
                )
            ).thenReturn(Mono.just(response))

            webTestClient.post()
                .uri("/v1/admin/courses")
                .header("X-User-Role", "ADMIN")
                .bodyValue(
                    mapOf(
                        "title" to "BACK 기초",
                        "slug" to "back-basic",
                        "description" to "desc",
                        "phase" to "BASIC",
                        "targetTrack" to "FL",
                    )
                )
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.slug").isEqualTo("back-basic")
        }

        "admin 배포 조회 API는 ADMIN이 아니면 403을 반환한다" {
            webTestClient.get()
                .uri("/v1/admin/courses/back-basic/assignments/assignment-1/deliveries?status=DELIVERED")
                .header("X-User-Role", "USER")
                .exchange()
                .expectStatus().isForbidden
        }

        "admin 배포 조회 API는 ADMIN 헤더로 호출하면 성공한다" {
            Mockito.`when`(
                courseV1Service.getDeliveries(
                    "back-basic",
                    "assignment-1",
                    AssignmentDeliveryStatus.DELIVERED,
                )
            ).thenReturn(
                Flux.just(
                    AssignmentDeliveryResponse(
                        userId = "user-1",
                        status = AssignmentDeliveryStatus.DELIVERED,
                        deliveredAt = Instant.parse("2026-03-01T00:00:00Z"),
                        failureReason = null,
                    )
                )
            )

            webTestClient.get()
                .uri("/v1/admin/courses/back-basic/assignments/assignment-1/deliveries?status=DELIVERED")
                .header("X-User-Role", "ADMIN")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].userId").isEqualTo("user-1")
        }
    }
}

private fun sampleCourseResponse(): CourseResponse {
    val now = Instant.parse("2026-02-20T00:00:00Z")
    return CourseResponse(
        id = "course-1",
        title = "BACK 기초",
        slug = "back-basic",
        description = "desc",
        phase = CoursePhase.BASIC,
        targetTrack = CourseTrack.FL,
        status = CourseStatus.ACTIVE,
        createdAt = now,
        updatedAt = now,
    )
}

private fun sampleAssignmentDetailResponse(): AssignmentDetailResponse {
    val now = Instant.parse("2026-03-01T00:00:00Z")
    return AssignmentDetailResponse(
        id = "assignment-1",
        courseSlug = "back-basic",
        weekNo = 1,
        seqInWeek = 1,
        title = "터미널 계산기",
        difficulty = AssignmentDifficulty.MID,
        contentMd = "# 문제 설명",
        timeLimitMinutes = 60,
        openAt = now,
        dueAt = now.plusSeconds(3600),
        status = AssignmentStatus.PUBLISHED,
        publishedAt = now,
        requirements = emptyList(),
        examples = emptyList(),
    )
}
