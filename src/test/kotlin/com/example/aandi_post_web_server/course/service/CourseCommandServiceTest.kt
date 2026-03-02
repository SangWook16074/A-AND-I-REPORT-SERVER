package com.example.aandi_post_web_server.course.service

import com.example.aandi_post_web_server.assignment.entity.Assignment
import com.example.aandi_post_web_server.assignment.entity.AssignmentDelivery
import com.example.aandi_post_web_server.assignment.enum.AssignmentDifficulty
import com.example.aandi_post_web_server.assignment.enum.AssignmentStatus
import com.example.aandi_post_web_server.assignment.repository.AssignmentDeliveryRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentExampleRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentRequirementRepository
import com.example.aandi_post_web_server.course.dtos.CreateCourseRequest
import com.example.aandi_post_web_server.course.dtos.CreateCourseWeekRequest
import com.example.aandi_post_web_server.course.dtos.UpdateEnrollmentRequest
import com.example.aandi_post_web_server.course.entity.Course
import com.example.aandi_post_web_server.course.entity.CourseEnrollment
import com.example.aandi_post_web_server.course.entity.CourseWeek
import com.example.aandi_post_web_server.course.enum.CoursePhase
import com.example.aandi_post_web_server.course.enum.CourseTrack
import com.example.aandi_post_web_server.course.enum.EnrollmentStatus
import com.example.aandi_post_web_server.course.repository.CourseEnrollmentRepository
import com.example.aandi_post_web_server.course.repository.CourseRepository
import com.example.aandi_post_web_server.course.repository.CourseWeekRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.time.LocalDate

class CourseCommandServiceTest : StringSpec({
    "중복 slug 코스 생성은 CONFLICT를 반환한다" {
        val fixture = CommandFixture()
        Mockito.`when`(fixture.courseRepository.existsBySlug("back-basic")).thenReturn(Mono.just(true))

        StepVerifier.create(
            fixture.service.createCourse(
                CreateCourseRequest(
                    title = "BACK 기초",
                    slug = "back-basic",
                    description = "desc",
                    phase = CoursePhase.BASIC,
                    targetTrack = CourseTrack.FL,
                )
            )
        )
            .expectErrorSatisfies { error ->
                (error as ResponseStatusException).statusCode shouldBe HttpStatus.CONFLICT
            }
            .verify()
    }

    "BANNED 상태 변경은 banReason이 필수다" {
        val fixture = CommandFixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val enrollment = CourseEnrollment(
            id = "enroll-1",
            courseId = "course-1",
            userId = "user-1",
            status = EnrollmentStatus.ENROLLED,
            joinedAt = Instant.parse("2026-02-20T00:00:00Z"),
            updatedAt = Instant.parse("2026-02-20T00:00:00Z"),
        )

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.courseEnrollmentRepository.findByCourseIdAndUserId("course-1", "user-1"))
            .thenReturn(Mono.just(enrollment))

        StepVerifier.create(
            fixture.service.updateEnrollmentStatus(
                courseSlug = "back-basic",
                userId = "user-1",
                request = UpdateEnrollmentRequest(
                    status = EnrollmentStatus.BANNED,
                    banReason = null,
                ),
            )
        )
            .expectErrorSatisfies { error ->
                (error as ResponseStatusException).statusCode shouldBe HttpStatus.BAD_REQUEST
            }
            .verify()
    }

    "ARCHIVED 과제는 publish할 수 없다" {
        val fixture = CommandFixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val archived = commandAssignment(
            id = "assignment-1",
            courseId = "course-1",
            status = AssignmentStatus.ARCHIVED,
        )

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.assignmentRepository.findByIdAndCourseId("assignment-1", "course-1"))
            .thenReturn(Mono.just(archived))

        StepVerifier.create(
            fixture.service.publishAssignment("back-basic", "assignment-1")
        )
            .expectErrorSatisfies { error ->
                (error as ResponseStatusException).statusCode shouldBe HttpStatus.UNPROCESSABLE_ENTITY
            }
            .verify()
    }

    "주차 생성은 weekNo가 1 미만이면 BAD_REQUEST를 반환한다" {
        val fixture = CommandFixture()

        val error = shouldThrow<ResponseStatusException> {
            fixture.service.createWeek(
                courseSlug = "back-basic",
                request = CreateCourseWeekRequest(
                    weekNo = 0,
                    title = "0주차",
                ),
            )
        }

        error.statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    "주차 생성은 endDate가 startDate보다 빠르면 BAD_REQUEST를 반환한다" {
        val fixture = CommandFixture()

        StepVerifier.create(
            fixture.service.createWeek(
                courseSlug = "back-basic",
                request = CreateCourseWeekRequest(
                    weekNo = 1,
                    title = "1주차",
                    startDate = LocalDate.of(2026, 3, 10),
                    endDate = LocalDate.of(2026, 3, 9),
                ),
            )
        )
            .expectErrorSatisfies { error ->
                (error as ResponseStatusException).statusCode shouldBe HttpStatus.BAD_REQUEST
            }
            .verify()
    }

    "주차 업서트는 기존 주차를 수정한다" {
        val fixture = CommandFixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val existing = CourseWeek(
            id = "week-1",
            courseId = "course-1",
            weekNo = 1,
            title = "기존 1주차",
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 3, 7),
            createdAt = Instant.parse("2026-02-20T00:00:00Z"),
            updatedAt = Instant.parse("2026-02-20T00:00:00Z"),
        )

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.courseWeekRepository.findByCourseIdAndWeekNo("course-1", 1)).thenReturn(Mono.just(existing))
        Mockito.`when`(fixture.courseWeekRepository.save(ArgumentMatchers.any(CourseWeek::class.java)))
            .thenAnswer { invocation ->
                val week = invocation.arguments[0] as CourseWeek
                Mono.just(week)
            }

        StepVerifier.create(
            fixture.service.createWeek(
                courseSlug = "back-basic",
                request = CreateCourseWeekRequest(
                    weekNo = 1,
                    title = "수정된 1주차",
                    startDate = LocalDate.of(2026, 3, 2),
                    endDate = LocalDate.of(2026, 3, 8),
                ),
            )
        )
            .assertNext {
                it.id shouldBe "week-1"
                it.title shouldBe "수정된 1주차"
                it.startDate shouldBe LocalDate.of(2026, 3, 2)
                it.endDate shouldBe LocalDate.of(2026, 3, 8)
            }
            .verifyComplete()
    }

    "DRAFT 과제는 배포 트리거할 수 없다" {
        val fixture = CommandFixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val draft = commandAssignment(
            id = "assignment-draft",
            courseId = "course-1",
            status = AssignmentStatus.DRAFT,
        )

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.assignmentRepository.findByIdAndCourseId("assignment-draft", "course-1"))
            .thenReturn(Mono.just(draft))

        StepVerifier.create(
            fixture.service.triggerDeliveries("back-basic", "assignment-draft")
        )
            .expectErrorSatisfies { error ->
                (error as ResponseStatusException).statusCode shouldBe HttpStatus.UNPROCESSABLE_ENTITY
            }
            .verify()
    }

    "배포 트리거는 ENROLLED 대상만 DELIVERED 처리한다" {
        val fixture = CommandFixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val published = commandAssignment(
            id = "assignment-1",
            courseId = "course-1",
            status = AssignmentStatus.PUBLISHED,
        )
        val enrollments = listOf(
            CourseEnrollment(courseId = "course-1", userId = "user-1", status = EnrollmentStatus.ENROLLED),
            CourseEnrollment(courseId = "course-1", userId = "user-2", status = EnrollmentStatus.ENROLLED),
        )

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.assignmentRepository.findByIdAndCourseId("assignment-1", "course-1"))
            .thenReturn(Mono.just(published))
        Mockito.`when`(fixture.courseEnrollmentRepository.findAllByCourseIdAndStatus("course-1", EnrollmentStatus.ENROLLED))
            .thenReturn(Flux.fromIterable(enrollments))
        Mockito.`when`(
            fixture.assignmentDeliveryRepository.findByAssignmentIdAndUserId(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
            )
        ).thenReturn(Mono.empty())
        Mockito.`when`(fixture.assignmentDeliveryRepository.save(ArgumentMatchers.any(AssignmentDelivery::class.java)))
            .thenAnswer { invocation ->
                val delivery = invocation.arguments[0] as AssignmentDelivery
                Mono.just(delivery.copy(id = "${delivery.userId}-delivery"))
            }

        StepVerifier.create(
            fixture.service.triggerDeliveries("back-basic", "assignment-1")
        )
            .assertNext {
                it.targetCount shouldBe 2
                it.deliveredCount shouldBe 2
                it.failedCount shouldBe 0
            }
            .verifyComplete()
    }
})

private class CommandFixture {
    val courseRepository: CourseRepository = Mockito.mock(CourseRepository::class.java)
    val courseEnrollmentRepository: CourseEnrollmentRepository = Mockito.mock(CourseEnrollmentRepository::class.java)
    val courseWeekRepository: CourseWeekRepository = Mockito.mock(CourseWeekRepository::class.java)
    val assignmentRepository: AssignmentRepository = Mockito.mock(AssignmentRepository::class.java)
    val assignmentRequirementRepository: AssignmentRequirementRepository = Mockito.mock(AssignmentRequirementRepository::class.java)
    val assignmentExampleRepository: AssignmentExampleRepository = Mockito.mock(AssignmentExampleRepository::class.java)
    val assignmentDeliveryRepository: AssignmentDeliveryRepository = Mockito.mock(AssignmentDeliveryRepository::class.java)

    val service = CourseCommandService(
        courseRepository = courseRepository,
        courseEnrollmentRepository = courseEnrollmentRepository,
        courseWeekRepository = courseWeekRepository,
        assignmentRepository = assignmentRepository,
        assignmentRequirementRepository = assignmentRequirementRepository,
        assignmentExampleRepository = assignmentExampleRepository,
        assignmentDeliveryRepository = assignmentDeliveryRepository,
    )
}

private fun commandAssignment(
    id: String,
    courseId: String,
    status: AssignmentStatus,
): Assignment {
    val now = Instant.parse("2026-02-20T00:00:00Z")
    return Assignment(
        id = id,
        courseId = courseId,
        createdBy = "admin",
        weekNo = 1,
        seqInWeek = 1,
        title = "테스트 과제",
        difficulty = AssignmentDifficulty.MID,
        contentMd = "content",
        timeLimitMinutes = 60,
        openAt = now,
        dueAt = now.plusSeconds(3600),
        status = status,
        createdAt = now,
        updatedAt = now,
        publishedAt = if (status == AssignmentStatus.PUBLISHED) now else null,
    )
}
