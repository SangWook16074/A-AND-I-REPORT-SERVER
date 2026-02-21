package com.example.aandi_post_web_server.assignment.dtos

import com.example.aandi_post_web_server.assignment.enum.AssignmentDeliveryStatus
import com.example.aandi_post_web_server.assignment.enum.AssignmentDifficulty
import com.example.aandi_post_web_server.assignment.enum.AssignmentStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateAssignmentRequirementRequest(
    @field:Min(1)
    val sortOrder: Int,
    @field:NotBlank
    val requirementText: String,
)

data class CreateAssignmentExampleRequest(
    @field:Min(1)
    val seq: Int,
    @field:NotBlank
    val inputText: String,
    @field:NotBlank
    val outputText: String,
    val description: String? = null,
)

data class CreateAssignmentRequest(
    @field:Min(1)
    val weekNo: Int,
    @field:Min(1)
    val seqInWeek: Int,
    @field:NotBlank
    val title: String,
    val difficulty: AssignmentDifficulty,
    @field:NotBlank
    val contentMd: String,
    @field:Min(1)
    val timeLimitMinutes: Int,
    val openAt: Instant,
    val dueAt: Instant,
    val requirements: List<CreateAssignmentRequirementRequest> = emptyList(),
    val examples: List<CreateAssignmentExampleRequest> = emptyList(),
)

data class AssignmentRequirementResponse(
    val sortOrder: Int,
    val requirementText: String,
)

data class AssignmentExampleResponse(
    val seq: Int,
    val inputText: String,
    val outputText: String,
    val description: String?,
)

data class AssignmentSummaryResponse(
    val id: String,
    val weekNo: Int,
    val seqInWeek: Int,
    val title: String,
    val difficulty: AssignmentDifficulty,
    val openAt: Instant,
    val dueAt: Instant,
    val status: AssignmentStatus,
)

data class AssignmentDetailResponse(
    val id: String,
    val courseSlug: String,
    val weekNo: Int,
    val seqInWeek: Int,
    val title: String,
    val difficulty: AssignmentDifficulty,
    val contentMd: String,
    val timeLimitMinutes: Int,
    val openAt: Instant,
    val dueAt: Instant,
    val status: AssignmentStatus,
    val publishedAt: Instant?,
    val requirements: List<AssignmentRequirementResponse>,
    val examples: List<AssignmentExampleResponse>,
)

data class PublishAssignmentResponse(
    val assignmentId: String,
    val courseSlug: String,
    val status: AssignmentStatus,
    val publishedAt: Instant?,
)

data class TriggerDeliveriesResponse(
    val assignmentId: String,
    val courseSlug: String,
    val targetCount: Int,
    val deliveredCount: Int,
    val failedCount: Int,
)

data class AssignmentDeliveryResponse(
    val userId: String,
    val status: AssignmentDeliveryStatus,
    val deliveredAt: Instant?,
    val failureReason: String?,
)
