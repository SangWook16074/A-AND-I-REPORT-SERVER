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
import com.example.aandi_post_web_server.course.dtos.CreateCourseWeekRequest
import com.example.aandi_post_web_server.course.entity.Course
import com.example.aandi_post_web_server.course.entity.CourseWeek
import com.example.aandi_post_web_server.course.repository.CourseEnrollmentRepository
import com.example.aandi_post_web_server.course.repository.CourseRepository
import com.example.aandi_post_web_server.course.repository.CourseWeekRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.time.LocalDate

class CourseV1ServiceTest : StringSpec({
    "USER는 delivery된 과제만 조회한다" {
        val fixture = Fixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val visibleAssignment = assignment(id = "a-1", courseId = "course-1", weekNo = 1, status = AssignmentStatus.PUBLISHED)
        val hiddenByCourse = assignment(id = "a-2", courseId = "course-2", weekNo = 1, status = AssignmentStatus.PUBLISHED)

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(
            fixture.assignmentDeliveryRepository.findAllByUserIdAndStatus("user-1", AssignmentDeliveryStatus.DELIVERED)
        ).thenReturn(
            Flux.just(
                AssignmentDelivery(assignmentId = "a-1", userId = "user-1", status = AssignmentDeliveryStatus.DELIVERED),
                AssignmentDelivery(assignmentId = "a-2", userId = "user-1", status = AssignmentDeliveryStatus.DELIVERED),
            )
        )
        Mockito.`when`(fixture.assignmentRepository.findAllByIdIn(ArgumentMatchers.anyCollection<String>()))
            .thenReturn(Flux.just(visibleAssignment, hiddenByCourse))

        StepVerifier.create(
            fixture.service.getAssignments(
                courseSlug = "back-basic",
                weekNo = null,
                status = AssignmentStatus.PUBLISHED,
                requesterId = "user-1",
                isPrivileged = false,
            )
        )
            .assertNext {
                it.id shouldBe "a-1"
                it.weekNo shouldBe 1
            }
            .verifyComplete()
    }

    "delivery가 없는 USER는 과제 상세를 조회할 수 없다" {
        val fixture = Fixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")
        val assignment = assignment(id = "a-1", courseId = "course-1", weekNo = 1, status = AssignmentStatus.PUBLISHED)

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.assignmentRepository.findByIdAndCourseId("a-1", "course-1")).thenReturn(Mono.just(assignment))
        Mockito.`when`(fixture.assignmentDeliveryRepository.findByAssignmentIdAndUserId("a-1", "user-1"))
            .thenReturn(Mono.empty())

        StepVerifier.create(
            fixture.service.getAssignmentDetail(
                courseSlug = "back-basic",
                assignmentId = "a-1",
                requesterId = "user-1",
                isPrivileged = false,
            )
        )
            .expectErrorSatisfies { error ->
                val status = (error as ResponseStatusException).statusCode
                status shouldBe HttpStatus.FORBIDDEN
            }
            .verify()
    }

    "코스 주차 생성 API는 신규 주차를 저장한다" {
        val fixture = Fixture()
        val course = Course(id = "course-1", title = "BACK 기초", slug = "back-basic")

        Mockito.`when`(fixture.courseRepository.findBySlug("back-basic")).thenReturn(Mono.just(course))
        Mockito.`when`(fixture.courseWeekRepository.findByCourseIdAndWeekNo("course-1", 1)).thenReturn(Mono.empty())
        Mockito.`when`(fixture.courseWeekRepository.save(ArgumentMatchers.any(CourseWeek::class.java)))
            .thenAnswer { invocation ->
                val week = invocation.arguments[0] as CourseWeek
                Mono.just(week.copy(id = "week-1"))
            }

        StepVerifier.create(
            fixture.service.createWeek(
                courseSlug = "back-basic",
                request = CreateCourseWeekRequest(
                    weekNo = 1,
                    title = "1주차 - 기본 문법",
                    startDate = LocalDate.of(2026, 3, 2),
                    endDate = LocalDate.of(2026, 3, 8),
                ),
            )
        )
            .assertNext {
                it.id shouldBe "week-1"
                it.weekNo shouldBe 1
                it.title shouldBe "1주차 - 기본 문법"
            }
            .verifyComplete()
    }
})

private class Fixture {
    val courseRepository: CourseRepository = Mockito.mock(CourseRepository::class.java)
    val courseEnrollmentRepository: CourseEnrollmentRepository = Mockito.mock(CourseEnrollmentRepository::class.java)
    val courseWeekRepository: CourseWeekRepository = Mockito.mock(CourseWeekRepository::class.java)
    val assignmentRepository: AssignmentRepository = Mockito.mock(AssignmentRepository::class.java)
    val assignmentRequirementRepository: AssignmentRequirementRepository = Mockito.mock(AssignmentRequirementRepository::class.java)
    val assignmentExampleRepository: AssignmentExampleRepository = Mockito.mock(AssignmentExampleRepository::class.java)
    val assignmentDeliveryRepository: AssignmentDeliveryRepository = Mockito.mock(AssignmentDeliveryRepository::class.java)

    private val courseCommandService = CourseCommandService(
        courseRepository = courseRepository,
        courseEnrollmentRepository = courseEnrollmentRepository,
        courseWeekRepository = courseWeekRepository,
        assignmentRepository = assignmentRepository,
        assignmentRequirementRepository = assignmentRequirementRepository,
        assignmentExampleRepository = assignmentExampleRepository,
        assignmentDeliveryRepository = assignmentDeliveryRepository,
    )

    private val courseQueryService = CourseQueryService(
        courseRepository = courseRepository,
        courseEnrollmentRepository = courseEnrollmentRepository,
        courseWeekRepository = courseWeekRepository,
        assignmentRepository = assignmentRepository,
        assignmentRequirementRepository = assignmentRequirementRepository,
        assignmentExampleRepository = assignmentExampleRepository,
        assignmentDeliveryRepository = assignmentDeliveryRepository,
    )

    val service = CourseV1Service(
        courseCommandService = courseCommandService,
        courseQueryService = courseQueryService,
    )
}

private fun assignment(
    id: String,
    courseId: String,
    weekNo: Int,
    status: AssignmentStatus,
): Assignment {
    val now = Instant.parse("2026-02-20T00:00:00Z")
    return Assignment(
        id = id,
        courseId = courseId,
        createdBy = "admin",
        weekNo = weekNo,
        seqInWeek = 1,
        title = "테스트 과제",
        difficulty = AssignmentDifficulty.LOW,
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
