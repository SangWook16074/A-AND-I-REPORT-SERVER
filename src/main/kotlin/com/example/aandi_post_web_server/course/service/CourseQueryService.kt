package com.example.aandi_post_web_server.course.service

import com.example.aandi_post_web_server.assignment.dtos.AssignmentDeliveryResponse
import com.example.aandi_post_web_server.assignment.dtos.AssignmentDetailResponse
import com.example.aandi_post_web_server.assignment.dtos.AssignmentExampleResponse
import com.example.aandi_post_web_server.assignment.dtos.AssignmentRequirementResponse
import com.example.aandi_post_web_server.assignment.dtos.AssignmentSummaryResponse
import com.example.aandi_post_web_server.assignment.entity.Assignment
import com.example.aandi_post_web_server.assignment.entity.AssignmentDelivery
import com.example.aandi_post_web_server.assignment.enum.AssignmentDeliveryStatus
import com.example.aandi_post_web_server.assignment.enum.AssignmentStatus
import com.example.aandi_post_web_server.assignment.repository.AssignmentDeliveryRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentExampleRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentRepository
import com.example.aandi_post_web_server.assignment.repository.AssignmentRequirementRepository
import com.example.aandi_post_web_server.course.domain.AssignmentId
import com.example.aandi_post_web_server.course.domain.CourseId
import com.example.aandi_post_web_server.course.domain.CourseSlug
import com.example.aandi_post_web_server.course.domain.WeekNo
import com.example.aandi_post_web_server.course.dtos.CourseEnrollmentResponse
import com.example.aandi_post_web_server.course.dtos.CourseResponse
import com.example.aandi_post_web_server.course.dtos.CourseWeekResponse
import com.example.aandi_post_web_server.course.entity.Course
import com.example.aandi_post_web_server.course.entity.CourseEnrollment
import com.example.aandi_post_web_server.course.entity.CourseWeek
import com.example.aandi_post_web_server.course.enum.CoursePhase
import com.example.aandi_post_web_server.course.enum.CourseStatus
import com.example.aandi_post_web_server.course.enum.CourseTrack
import com.example.aandi_post_web_server.course.enum.UserTrack
import com.example.aandi_post_web_server.course.repository.CourseEnrollmentRepository
import com.example.aandi_post_web_server.course.repository.CourseRepository
import com.example.aandi_post_web_server.course.repository.CourseWeekRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class CourseQueryService(
    private val courseRepository: CourseRepository,
    private val courseEnrollmentRepository: CourseEnrollmentRepository,
    private val courseWeekRepository: CourseWeekRepository,
    private val assignmentRepository: AssignmentRepository,
    private val assignmentRequirementRepository: AssignmentRequirementRepository,
    private val assignmentExampleRepository: AssignmentExampleRepository,
    private val assignmentDeliveryRepository: AssignmentDeliveryRepository,
) {

    fun getCourse(courseSlug: String): Mono<CourseResponse> {
        val slug = parseCourseSlug(courseSlug)
        return findCourseBySlug(slug).map(::toCourseResponse)
    }

    fun getCourses(
        status: CourseStatus?,
        phase: CoursePhase?,
        track: UserTrack?,
    ): Flux<CourseResponse> {
        if (track == UserTrack.NO) {
            return Flux.empty()
        }
        val targetTrack = toCourseTrack(track)
        return loadCourses(status, phase, targetTrack).map(::toCourseResponse)
    }

    fun getEnrollments(courseSlug: String): Flux<CourseEnrollmentResponse> {
        val slug = parseCourseSlug(courseSlug)
        return findCourseBySlug(slug)
            .flatMapMany { course ->
                val courseId = parseCourseId(requireNotNull(course.id))
                courseEnrollmentRepository.findAllByCourseId(courseId.value)
            }
            .sort(compareByDescending<CourseEnrollment> { it.updatedAt })
            .map(::toEnrollmentResponse)
    }

    fun getWeeks(courseSlug: String): Flux<CourseWeekResponse> {
        val slug = parseCourseSlug(courseSlug)
        return findCourseBySlug(slug)
            .flatMapMany { course ->
                val courseId = parseCourseId(requireNotNull(course.id))
                courseWeekRepository.findAllByCourseId(courseId.value)
            }
            .sort(compareBy<CourseWeek> { it.weekNo })
            .map(::toWeekResponse)
    }

    fun getAssignmentsByWeek(
        courseSlug: String,
        weekNo: Int,
        status: AssignmentStatus?,
    ): Flux<AssignmentSummaryResponse> {
        return getAssignments(
            courseSlug = courseSlug,
            weekNo = weekNo,
            status = status,
        )
    }

    fun getAssignments(
        courseSlug: String,
        weekNo: Int?,
        status: AssignmentStatus?,
    ): Flux<AssignmentSummaryResponse> {
        val slug = parseCourseSlug(courseSlug)
        val parsedWeekNo = weekNo?.let { parseWeekNo(it) }

        return findCourseBySlug(slug)
            .flatMapMany { course ->
                val courseId = parseCourseId(requireNotNull(course.id))
                findAssignmentsByFilter(courseId, parsedWeekNo, status)
            }
            .sort(compareBy<Assignment> { it.weekNo }.thenBy { it.seqInWeek })
            .map(::toAssignmentSummaryResponse)
    }

    fun getAssignmentDetail(
        courseSlug: String,
        assignmentId: String,
    ): Mono<AssignmentDetailResponse> {
        val slug = parseCourseSlug(courseSlug)
        val parsedAssignmentId = parseAssignmentId(assignmentId)

        return findCourseBySlug(slug)
            .flatMap { course ->
                val courseId = parseCourseId(requireNotNull(course.id))
                assignmentRepository.findByIdAndCourseId(parsedAssignmentId.value, courseId.value)
                    .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다: ${parsedAssignmentId.value}")))
                    .flatMap { assignment ->
                        Mono.defer {
                            assignmentRequirementRepository
                                .findAllByAssignmentIdOrderBySortOrder(parsedAssignmentId.value)
                                .map { AssignmentRequirementResponse(it.sortOrder, it.requirementText) }
                                .collectList()
                                .zipWith(
                                    assignmentExampleRepository
                                        .findAllByAssignmentIdOrderBySeq(parsedAssignmentId.value)
                                        .map { AssignmentExampleResponse(it.seq, it.inputText, it.outputText, it.description) }
                                        .collectList()
                                )
                                .map { tuple ->
                                    toAssignmentDetailResponse(course.slug, assignment, tuple.t1, tuple.t2)
                                }
                            }
                    }
            }
    }

    fun getAssignmentCourse(assignmentId: String): Mono<CourseResponse> {
        val parsedAssignmentId = parseAssignmentId(assignmentId)
        return assignmentRepository.findById(parsedAssignmentId.value)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다: ${parsedAssignmentId.value}")))
            .flatMap { assignment ->
                courseRepository.findById(assignment.courseId)
                    .switchIfEmpty(
                        Mono.error(
                            ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "코스를 찾을 수 없습니다: ${assignment.courseId}",
                            )
                        )
                    )
            }
            .map(::toCourseResponse)
    }

    fun getDeliveries(
        courseSlug: String,
        assignmentId: String,
        status: AssignmentDeliveryStatus?,
    ): Flux<AssignmentDeliveryResponse> {
        val slug = parseCourseSlug(courseSlug)
        val parsedAssignmentId = parseAssignmentId(assignmentId)

        return findCourseBySlug(slug)
            .flatMapMany { course ->
                val courseId = parseCourseId(requireNotNull(course.id))
                assignmentRepository.findByIdAndCourseId(parsedAssignmentId.value, courseId.value)
                    .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다: ${parsedAssignmentId.value}")))
                    .flatMapMany { loadDeliveries(parsedAssignmentId, status) }
            }
            .sort(compareByDescending<AssignmentDelivery> { it.deliveredAt ?: Instant.EPOCH })
            .map {
                AssignmentDeliveryResponse(
                    userId = it.userId,
                    status = it.status,
                    deliveredAt = it.deliveredAt,
                    failureReason = it.failureReason,
                )
            }
    }

    private fun findAssignmentsByFilter(
        courseId: CourseId,
        weekNo: WeekNo?,
        status: AssignmentStatus?,
    ): Flux<Assignment> {
        if (weekNo != null && status != null) {
            return assignmentRepository.findAllByCourseIdAndWeekNoAndStatus(courseId.value, weekNo.value, status)
        }
        if (weekNo != null) {
            return assignmentRepository.findAllByCourseIdAndWeekNo(courseId.value, weekNo.value)
        }
        if (status != null) {
            return assignmentRepository.findAllByCourseIdAndStatus(courseId.value, status)
        }
        return assignmentRepository.findAllByCourseId(courseId.value)
    }

    private fun loadDeliveries(
        assignmentId: AssignmentId,
        status: AssignmentDeliveryStatus?,
    ): Flux<AssignmentDelivery> {
        if (status == null) {
            return assignmentDeliveryRepository.findAllByAssignmentId(assignmentId.value)
        }
        return assignmentDeliveryRepository.findAllByAssignmentIdAndStatus(assignmentId.value, status)
    }

    private fun loadCourses(
        status: CourseStatus?,
        phase: CoursePhase?,
        targetTrack: CourseTrack?,
    ): Flux<Course> {
        return courseRepository.findAll()
            .filter { course -> status == null || course.status == status }
            .filter { course -> phase == null || course.phase == phase }
            .filter { course -> targetTrack == null || course.targetTrack == targetTrack }
    }

    private fun findCourseBySlug(slug: CourseSlug): Mono<Course> {
        return courseRepository.findBySlug(slug.value)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다: ${slug.value}")))
    }

    private fun parseCourseSlug(raw: String): CourseSlug =
        parseOrBadRequest { CourseSlug.from(raw) }

    private fun parseCourseId(raw: String): CourseId =
        parseOrBadRequest { CourseId.from(raw) }

    private fun parseWeekNo(raw: Int): WeekNo =
        parseOrBadRequest { WeekNo.from(raw) }

    private fun parseAssignmentId(raw: String): AssignmentId =
        parseOrBadRequest { AssignmentId.from(raw) }

    private fun toCourseTrack(userTrack: UserTrack?): CourseTrack? {
        if (userTrack == null) {
            return null
        }
        if (userTrack == UserTrack.FL) {
            return CourseTrack.FL
        }
        if (userTrack == UserTrack.SP) {
            return CourseTrack.SP
        }
        return null
    }

    private fun <T> parseOrBadRequest(block: () -> T): T {
        return runCatching(block).getOrElse { error ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, error.message ?: "잘못된 요청입니다.")
        }
    }

    private fun toCourseResponse(course: Course): CourseResponse = CourseResponse(
        id = requireNotNull(course.id),
        title = course.title,
        slug = course.slug,
        description = course.description,
        phase = course.phase,
        targetTrack = course.targetTrack,
        status = course.status,
        createdAt = course.createdAt,
        updatedAt = course.updatedAt,
    )

    private fun toEnrollmentResponse(enrollment: CourseEnrollment): CourseEnrollmentResponse = CourseEnrollmentResponse(
        id = requireNotNull(enrollment.id),
        userId = enrollment.userId,
        status = enrollment.status,
        joinedAt = enrollment.joinedAt,
        droppedAt = enrollment.droppedAt,
        bannedAt = enrollment.bannedAt,
        banReason = enrollment.banReason,
        updatedAt = enrollment.updatedAt,
    )

    private fun toWeekResponse(week: CourseWeek): CourseWeekResponse = CourseWeekResponse(
        id = requireNotNull(week.id),
        weekNo = week.weekNo,
        title = week.title,
        startDate = week.startDate,
        endDate = week.endDate,
        createdAt = week.createdAt,
        updatedAt = week.updatedAt,
    )

    private fun toAssignmentSummaryResponse(assignment: Assignment): AssignmentSummaryResponse = AssignmentSummaryResponse(
        id = requireNotNull(assignment.id),
        weekNo = assignment.weekNo,
        seqInWeek = assignment.seqInWeek,
        title = assignment.title,
        difficulty = assignment.difficulty,
        openAt = assignment.openAt,
        dueAt = assignment.dueAt,
        status = assignment.status,
    )

    private fun toAssignmentDetailResponse(
        courseSlug: String,
        assignment: Assignment,
        requirements: List<AssignmentRequirementResponse>,
        examples: List<AssignmentExampleResponse>,
    ): AssignmentDetailResponse = AssignmentDetailResponse(
        id = requireNotNull(assignment.id),
        courseSlug = courseSlug,
        weekNo = assignment.weekNo,
        seqInWeek = assignment.seqInWeek,
        title = assignment.title,
        difficulty = assignment.difficulty,
        contentMd = assignment.contentMd,
        timeLimitMinutes = assignment.timeLimitMinutes,
        openAt = assignment.openAt,
        dueAt = assignment.dueAt,
        status = assignment.status,
        publishedAt = assignment.publishedAt,
        requirements = requirements,
        examples = examples,
    )
}
