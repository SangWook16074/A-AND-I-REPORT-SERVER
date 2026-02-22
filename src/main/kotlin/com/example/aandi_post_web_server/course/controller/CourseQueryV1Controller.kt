package com.example.aandi_post_web_server.course.controller

import com.example.aandi_post_web_server.assignment.dtos.AssignmentDetailResponse
import com.example.aandi_post_web_server.assignment.dtos.AssignmentSummaryResponse
import com.example.aandi_post_web_server.assignment.enum.AssignmentStatus
import com.example.aandi_post_web_server.course.dtos.CourseResponse
import com.example.aandi_post_web_server.course.dtos.CourseWeekResponse
import com.example.aandi_post_web_server.course.enum.CourseStatus
import com.example.aandi_post_web_server.course.service.CourseV1Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/v1/courses")
class CourseQueryV1Controller(
    private val courseV1Service: CourseV1Service,
) {

    @GetMapping
    fun getCourses(
        @RequestParam(required = false) status: CourseStatus?,
    ): Flux<CourseResponse> {
        return courseV1Service.getCourses(status)
    }

    @GetMapping("/{courseSlug}")
    fun getCourse(
        @PathVariable courseSlug: String,
    ): Mono<CourseResponse> {
        return courseV1Service.getCourse(courseSlug)
    }

    @GetMapping("/{courseSlug}/weeks")
    fun getWeeks(
        @PathVariable courseSlug: String,
    ): Flux<CourseWeekResponse> {
        return courseV1Service.getWeeks(courseSlug)
    }

    @GetMapping("/{courseSlug}/weeks/{weekNo}/assignments")
    fun getAssignmentsByWeek(
        @PathVariable courseSlug: String,
        @PathVariable weekNo: Int,
        @RequestParam(required = false) status: AssignmentStatus?,
    ): Flux<AssignmentSummaryResponse> {
        return courseV1Service.getAssignmentsByWeek(
            courseSlug = courseSlug,
            weekNo = weekNo,
            status = status,
        )
    }

    @GetMapping("/{courseSlug}/assignments")
    fun getAssignments(
        @PathVariable courseSlug: String,
        @RequestParam(required = false) weekNo: Int?,
        @RequestParam(required = false) status: AssignmentStatus?,
    ): Flux<AssignmentSummaryResponse> {
        return courseV1Service.getAssignments(
            courseSlug = courseSlug,
            weekNo = weekNo,
            status = status,
        )
    }

    @GetMapping("/{courseSlug}/assignments/{assignmentId}")
    fun getAssignmentDetail(
        @PathVariable courseSlug: String,
        @PathVariable assignmentId: String,
    ): Mono<AssignmentDetailResponse> {
        return courseV1Service.getAssignmentDetail(
            courseSlug = courseSlug,
            assignmentId = assignmentId,
        )
    }
}
