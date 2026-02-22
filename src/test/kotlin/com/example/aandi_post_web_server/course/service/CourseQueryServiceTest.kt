package com.example.aandi_post_web_server.course.service

import com.example.aandi_post_web_server.assignment.entity.Assignment
import com.example.aandi_post_web_server.assignment.entity.AssignmentDelivery
import com.example.aandi_post_web_server.assignment.enum.AssignmentDeliveryStatus
import com.example.aandi_post_web_server.assignment.enum.AssignmentDifficulty
import com.example.aandi_post_web_server.assignment.enum.AssignmentStatus
import com.example.aandi_post_web_server.assignment.repository.AssignmentDeliveryRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentExampleRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentRequirementRepository
import com.example.aandi_post_web_server.course.entity.Course
import com.example.aandi_post_web_server.course.repository.CourseEnrollmentRepository
import com.example.aandi_post_web_server.course.repository.CourseRepository
import com.example.aandi_post_web_server.course.repository.CourseWeekRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant

class CourseQueryServiceTest : StringSpec({
    "배포 조회는 deliveredAt 기준 내림차순 정렬된다" {
        val fixture = QueryFixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val assignment = queryAssignment(id = "assignment-1", courseId = "course-1")
        val old = AssignmentDelivery(
            id = "d1",
            assignmentId = "assignment-1",
            userId = "user-1",
            status = AssignmentDeliveryStatus.DELIVERED,
            deliveredAt = Instant.parse("2026-03-01T00:00:00Z"),
        )
        val latest = AssignmentDelivery(
            id = "d2",
            assignmentId = "assignment-1",
            userId = "user-2",
            status = AssignmentDeliveryStatus.DELIVERED,
            deliveredAt = Instant.parse("2026-03-02T00:00:00Z"),
        )

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.assignmentRepository.findByIdAndCourseId("assignment-1", "course-1"))
            .thenReturn(Mono.just(assignment))
        Mockito.`when`(fixture.assignmentDeliveryRepository.findAllByAssignmentId("assignment-1"))
            .thenReturn(Flux.just(old, latest))

        StepVerifier.create(
            fixture.service.getDeliveries("back-basic", "assignment-1", null).map { it.userId }.collectList()
        )
            .assertNext { orderedUserIds ->
                orderedUserIds.shouldContainExactly("user-2", "user-1")
            }
            .verifyComplete()
    }

    "배포 조회는 status 필터가 있으면 필터 저장소 메서드를 사용한다" {
        val fixture = QueryFixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val assignment = queryAssignment(id = "assignment-1", courseId = "course-1")
        val failed = AssignmentDelivery(
            id = "d3",
            assignmentId = "assignment-1",
            userId = "user-3",
            status = AssignmentDeliveryStatus.FAILED,
            deliveredAt = Instant.parse("2026-03-02T01:00:00Z"),
            failureReason = "network",
        )

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.assignmentRepository.findByIdAndCourseId("assignment-1", "course-1"))
            .thenReturn(Mono.just(assignment))
        Mockito.`when`(
            fixture.assignmentDeliveryRepository.findAllByAssignmentIdAndStatus(
                "assignment-1",
                AssignmentDeliveryStatus.FAILED,
            )
        ).thenReturn(Flux.just(failed))

        StepVerifier.create(
            fixture.service.getDeliveries("back-basic", "assignment-1", AssignmentDeliveryStatus.FAILED)
        )
            .assertNext {
                it.userId shouldBe "user-3"
                it.status shouldBe AssignmentDeliveryStatus.FAILED
            }
            .verifyComplete()
    }
})

private class QueryFixture {
    val courseRepository: CourseRepository = Mockito.mock(CourseRepository::class.java)
    val courseEnrollmentRepository: CourseEnrollmentRepository = Mockito.mock(CourseEnrollmentRepository::class.java)
    val courseWeekRepository: CourseWeekRepository = Mockito.mock(CourseWeekRepository::class.java)
    val assignmentRepository: AssignmentRepository = Mockito.mock(AssignmentRepository::class.java)
    val assignmentRequirementRepository: AssignmentRequirementRepository = Mockito.mock(AssignmentRequirementRepository::class.java)
    val assignmentExampleRepository: AssignmentExampleRepository = Mockito.mock(AssignmentExampleRepository::class.java)
    val assignmentDeliveryRepository: AssignmentDeliveryRepository = Mockito.mock(AssignmentDeliveryRepository::class.java)

    val service = CourseQueryService(
        courseRepository = courseRepository,
        courseEnrollmentRepository = courseEnrollmentRepository,
        courseWeekRepository = courseWeekRepository,
        assignmentRepository = assignmentRepository,
        assignmentRequirementRepository = assignmentRequirementRepository,
        assignmentExampleRepository = assignmentExampleRepository,
        assignmentDeliveryRepository = assignmentDeliveryRepository,
    )
}

private fun queryAssignment(
    id: String,
    courseId: String,
): Assignment {
    val now = Instant.parse("2026-02-20T00:00:00Z")
    return Assignment(
        id = id,
        courseId = courseId,
        createdBy = "admin",
        weekNo = 1,
        seqInWeek = 1,
        title = "배포 조회 테스트 과제",
        difficulty = AssignmentDifficulty.MID,
        contentMd = "content",
        timeLimitMinutes = 60,
        openAt = now,
        dueAt = now.plusSeconds(3600),
        status = AssignmentStatus.PUBLISHED,
        createdAt = now,
        updatedAt = now,
        publishedAt = now,
    )
}
