package com.example.aandi_post_web_server.course.controller

import com.example.aandi_post_web_server.assignment.dtos.AssignmentDeliveryResponse
import com.example.aandi_post_web_server.assignment.dtos.AssignmentDetailResponse
import com.example.aandi_post_web_server.assignment.dtos.CreateAssignmentRequest
import com.example.aandi_post_web_server.assignment.dtos.PublishAssignmentResponse
import com.example.aandi_post_web_server.assignment.dtos.TriggerDeliveriesResponse
import com.example.aandi_post_web_server.assignment.enum.AssignmentDeliveryStatus
import com.example.aandi_post_web_server.course.dtos.CourseEnrollmentResponse
import com.example.aandi_post_web_server.course.dtos.CourseResponse
import com.example.aandi_post_web_server.course.dtos.CourseWeekResponse
import com.example.aandi_post_web_server.course.dtos.CreateCourseRequest
import com.example.aandi_post_web_server.course.dtos.CreateCourseWeekRequest
import com.example.aandi_post_web_server.course.dtos.EnrollCourseRequest
import com.example.aandi_post_web_server.course.dtos.UpdateCourseRequest
import com.example.aandi_post_web_server.course.dtos.UpdateEnrollmentRequest
import com.example.aandi_post_web_server.course.service.CourseV1Service
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

@RestController
@RequestMapping("/v1/admin/courses")
class CourseV1Controller(
    private val courseV1Service: CourseV1Service,
) {

    @PostMapping
    fun createCourse(
        @Valid @RequestBody request: CreateCourseRequest,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<CourseResponse> {
        requireAdmin(userRole)
        return courseV1Service.createCourse(request)
    }

    @PatchMapping("/{courseSlug}")
    fun updateCourse(
        @PathVariable courseSlug: String,
        @RequestBody request: UpdateCourseRequest,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<CourseResponse> {
        requireAdmin(userRole)
        return courseV1Service.updateCourse(courseSlug, request)
    }

    @DeleteMapping("/{courseSlug}")
    fun archiveCourse(
        @PathVariable courseSlug: String,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<Void> {
        requireAdmin(userRole)
        return courseV1Service.archiveCourse(courseSlug)
    }

    @PostMapping("/{courseSlug}/enrollments")
    fun enrollMember(
        @PathVariable courseSlug: String,
        @Valid @RequestBody request: EnrollCourseRequest,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<CourseEnrollmentResponse> {
        requireAdmin(userRole)
        return courseV1Service.enrollMember(courseSlug, request)
    }

    @PatchMapping("/{courseSlug}/enrollments/{userId}")
    fun updateEnrollmentStatus(
        @PathVariable courseSlug: String,
        @PathVariable userId: String,
        @RequestBody request: UpdateEnrollmentRequest,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<CourseEnrollmentResponse> {
        requireAdmin(userRole)
        return courseV1Service.updateEnrollmentStatus(courseSlug, userId, request)
    }

    @GetMapping("/{courseSlug}/enrollments")
    fun getEnrollments(
        @PathVariable courseSlug: String,
        @RequestHeader("X-User-Role") userRole: String,
    ): Flux<CourseEnrollmentResponse> {
        requireAdmin(userRole)
        return courseV1Service.getEnrollments(courseSlug)
    }

    @PostMapping("/{courseSlug}/weeks")
    fun createWeek(
        @PathVariable courseSlug: String,
        @Valid @RequestBody request: CreateCourseWeekRequest,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<CourseWeekResponse> {
        requireAdmin(userRole)
        return courseV1Service.createWeek(courseSlug, request)
    }

    @PostMapping("/{courseSlug}/assignments")
    fun createAssignment(
        @PathVariable courseSlug: String,
        @Valid @RequestBody request: CreateAssignmentRequest,
        @RequestHeader("X-User-Id") requesterId: String,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<AssignmentDetailResponse> {
        requireAdmin(userRole)
        return courseV1Service.createAssignment(courseSlug, request, requesterId)
    }

    @PostMapping("/{courseSlug}/assignments/{assignmentId}/publish")
    fun publishAssignment(
        @PathVariable courseSlug: String,
        @PathVariable assignmentId: String,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<PublishAssignmentResponse> {
        requireAdmin(userRole)
        return courseV1Service.publishAssignment(courseSlug, assignmentId)
    }

    @PostMapping("/{courseSlug}/assignments/{assignmentId}/deliveries")
    fun triggerDeliveries(
        @PathVariable courseSlug: String,
        @PathVariable assignmentId: String,
        @RequestHeader("X-User-Role") userRole: String,
    ): Mono<TriggerDeliveriesResponse> {
        requireAdmin(userRole)
        return courseV1Service.triggerDeliveries(courseSlug, assignmentId)
    }

    @GetMapping("/{courseSlug}/assignments/{assignmentId}/deliveries")
    fun getDeliveries(
        @PathVariable courseSlug: String,
        @PathVariable assignmentId: String,
        @RequestParam(required = false) status: AssignmentDeliveryStatus?,
        @RequestHeader("X-User-Role") userRole: String,
    ): Flux<AssignmentDeliveryResponse> {
        requireAdmin(userRole)
        return courseV1Service.getDeliveries(courseSlug, assignmentId, status)
    }

    private fun requireAdmin(userRole: String) {
        if (normalizeRole(userRole) != "ADMIN") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "허용되지 않은 권한입니다.")
        }
    }

    private fun normalizeRole(role: String): String = role.trim().uppercase()
}
