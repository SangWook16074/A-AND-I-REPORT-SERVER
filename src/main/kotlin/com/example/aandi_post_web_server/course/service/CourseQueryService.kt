package com.example.aandi_post_web_server.course.service

import com.example.aandi_post_web_server.assignment.domain.DeliveredAssignmentIds
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
import com.example.aandi_post_web_server.course.domain.UserId
import com.example.aandi_post_web_server.course.domain.WeekNo
import com.example.aandi_post_web_server.course.dtos.CourseEnrollmentResponse
import com.example.aandi_post_web_server.course.dtos.CourseResponse
import com.example.aandi_post_web_server.course.dtos.CourseWeekResponse
import com.example.aandi_post_web_server.course.entity.Course
import com.example.aandi_post_web_server.course.entity.CourseEnrollment
import com.example.aandi_post_web_server.course.entity.CourseWeek
import com.example.aandi_post_web_server.course.enum.CourseStatus
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

    fun getCourses(status: CourseStatus?): Flux<CourseResponse> =
        loadCourses(status).map(::toCourseResponse)

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
        requesterId: String,
        isPrivileged: Boolean,
    ): Flux<AssignmentSummaryResponse> {
        return getAssignments(
            courseSlug = courseSlug,
            weekNo = weekNo,
            status = status,
            requesterId = requesterId,
            isPrivileged = isPrivileged,
        )
    }

    fun getAssignments(
        courseSlug: String,
        weekNo: Int?,
        status: AssignmentStatus?,
        requesterId: String,
        isPrivileged: Boolean,
    ): Flux<AssignmentSummaryResponse> {
        val slug = parseCourseSlug(courseSlug)
        val parsedRequesterId = parseUserId(requesterId)
        val parsedWeekNo = weekNo?.let { parseWeekNo(it) }

        return findCourseBySlug(slug)
            .flatMapMany { course ->
                val courseId = parseCourseId(requireNotNull(course.id))
                if (isPrivileged) {
                    return@flatMapMany findAssignmentsByFilter(courseId, parsedWeekNo, status)
                }
                findDeliveredAssignmentsByFilter(courseId, parsedRequesterId, parsedWeekNo, status)
            }
            .sort(compareBy<Assignment> { it.weekNo }.thenBy { it.seqInWeek })
            .map(::toAssignmentSummaryResponse)
    }

    fun getAssignmentDetail(
        courseSlug: String,
        assignmentId: String,
        requesterId: String,
        isPrivileged: Boolean,
    ): Mono<AssignmentDetailResponse> {
        val slug = parseCourseSlug(courseSlug)
        val parsedAssignmentId = parseAssignmentId(assignmentId)
        val parsedRequesterId = parseUserId(requesterId)

        return findCourseBySlug(slug)
            .flatMap { course ->
                val courseId = parseCourseId(requireNotNull(course.id))
                assignmentRepository.findByIdAndCourseId(parsedAssignmentId.value, courseId.value)
                    .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다: ${parsedAssignmentId.value}")))
                    .flatMap { assignment ->
                        val authorizationMono = authorizeDetailAccess(isPrivileged, parsedAssignmentId, parsedRequesterId)
                        authorizationMono.then(
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
                        )
                    }
            }
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

    private fun findDeliveredAssignmentsByFilter(
        courseId: CourseId,
        requesterId: UserId,
        weekNo: WeekNo?,
        status: AssignmentStatus?,
    ): Flux<Assignment> {
        return assignmentDeliveryRepository.findAllByUserIdAndStatus(requesterId.value, AssignmentDeliveryStatus.DELIVERED)
            .collectList()
            .flatMapMany { deliveries ->
                val deliveredAssignmentIds = DeliveredAssignmentIds.fromDeliveries(deliveries)
                if (deliveredAssignmentIds.isEmpty()) {
                    return@flatMapMany Flux.empty()
                }
                assignmentRepository.findAllByIdIn(deliveredAssignmentIds.asCollection())
                    .filter { it.courseId == courseId.value }
                    .filter { weekNo == null || it.weekNo == weekNo.value }
                    .filter { status == null || it.status == status }
            }
    }

    private fun authorizeDetailAccess(
        isPrivileged: Boolean,
        assignmentId: AssignmentId,
        requesterId: UserId,
    ): Mono<Void> {
        if (isPrivileged) {
            return Mono.empty()
        }
        return ensureDelivered(assignmentId, requesterId)
    }

    private fun ensureDelivered(assignmentId: AssignmentId, requesterId: UserId): Mono<Void> {
        return assignmentDeliveryRepository.findByAssignmentIdAndUserId(assignmentId.value, requesterId.value)
            .filter { it.status == AssignmentDeliveryStatus.DELIVERED }
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN, "배포되지 않은 과제입니다.")))
            .then()
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

    private fun loadCourses(status: CourseStatus?): Flux<Course> {
        if (status == null) {
            return courseRepository.findAll()
        }
        return courseRepository.findAllByStatus(status)
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

    private fun parseUserId(raw: String): UserId =
        parseOrBadRequest { UserId.from(raw) }

    private fun parseAssignmentId(raw: String): AssignmentId =
        parseOrBadRequest { AssignmentId.from(raw) }

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
