package com.example.aandi_post_web_server.course.dtos

import com.example.aandi_post_web_server.course.enum.CourseStatus
import com.example.aandi_post_web_server.course.enum.EnrollmentStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.time.LocalDate

data class CreateCourseRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val slug: String,
    val description: String? = null,
)

data class UpdateCourseRequest(
    val title: String? = null,
    val description: String? = null,
    val status: CourseStatus? = null,
)

data class CourseResponse(
    val id: String,
    val title: String,
    val slug: String,
    val description: String?,
    val status: CourseStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class EnrollCourseRequest(
    @field:NotBlank
    val userId: String,
)

data class UpdateEnrollmentRequest(
    val status: EnrollmentStatus,
    val banReason: String? = null,
)

data class CourseEnrollmentResponse(
    val id: String,
    val userId: String,
    val status: EnrollmentStatus,
    val joinedAt: Instant,
    val droppedAt: Instant?,
    val bannedAt: Instant?,
    val banReason: String?,
    val updatedAt: Instant,
)

data class CreateCourseWeekRequest(
    @field:Min(1)
    val weekNo: Int,
    @field:NotBlank
    val title: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

data class CourseWeekResponse(
    val id: String,
    val weekNo: Int,
    val title: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
