package com.example.aandi_post_web_server.assignment.dtos

import com.example.aandi_post_web_server.assignment.enum.AssignmentDeliveryStatus
import com.example.aandi_post_web_server.assignment.enum.AssignmentDifficulty
import com.example.aandi_post_web_server.assignment.enum.AssignmentStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

@Schema(description = "레거시 순번-문자열 요청 모델")
data class LegacySeqStringRequest(
    @field:Min(1)
    @field:Schema(description = "순번", example = "1")
    val seq: Int,
    @field:NotBlank
    @field:Schema(description = "내용", example = "함수 분리 필수")
    val content: String,
)

@Schema(description = "레거시 예시 입출력 요청 모델")
data class LegacyExampleIORequest(
    @field:Min(1)
    @field:Schema(description = "예시 순번", example = "1")
    val seq: Int,
    @field:NotBlank
    @field:Schema(description = "입력 예시", example = "ADD 1\\nCLOSE")
    val input: String,
    @field:NotBlank
    @field:Schema(description = "출력 예시", example = "+1")
    val output: String,
)

@Schema(description = "레거시 난이도")
enum class LegacyAssignmentLevel {
    LOW,
    MEDIUM,
    HIGH,
    VERYHIGH,
}

private fun AssignmentDifficulty.toLegacyLevel(): LegacyAssignmentLevel = when (this) {
    AssignmentDifficulty.LOW -> LegacyAssignmentLevel.LOW
    AssignmentDifficulty.MID -> LegacyAssignmentLevel.MEDIUM
    AssignmentDifficulty.HIGH -> LegacyAssignmentLevel.HIGH
    AssignmentDifficulty.VERY_HIGH -> LegacyAssignmentLevel.VERYHIGH
}

@Schema(description = "과제 요구사항 생성 요청")
data class CreateAssignmentRequirementRequest(
    @field:Min(1)
    @field:Schema(description = "요구사항 정렬 순서", example = "1")
    val sortOrder: Int,
    @field:NotBlank
    @field:Schema(description = "요구사항 내용", example = "함수 분리 필수")
    val requirementText: String,
)

@Schema(description = "과제 예시 입출력 생성 요청")
data class CreateAssignmentExampleRequest(
    @field:Min(1)
    @field:Schema(description = "예시 순번", example = "1")
    val seq: Int,
    @field:NotBlank
    @field:Schema(description = "입력 예시", example = "ADD 1\\nCLOSE")
    val inputText: String,
    @field:NotBlank
    @field:Schema(description = "출력 예시", example = "+1")
    val outputText: String,
    @field:Schema(description = "예시 설명", example = "기본 동작")
    val description: String? = null,
)

@Schema(description = "과제 생성 요청")
data class CreateAssignmentRequest(
    @field:Min(1)
    @field:Schema(description = "주차 번호(레거시: week)", example = "1")
    val week: Int,
    @field:Min(1)
    @field:Schema(description = "주차 내 순번(레거시: seq)", example = "1")
    val seq: Int,
    @field:NotBlank
    @field:Schema(description = "과제 제목", example = "터미널 계산기")
    val title: String,
    @field:NotBlank
    @field:Schema(description = "과제 본문(레거시: content)", example = "# 문제 설명")
    val content: String,
    @field:Schema(description = "요구사항 목록(레거시: requirement)")
    val requirement: List<LegacySeqStringRequest> = emptyList(),
    @field:Schema(description = "학습 목표 목록(레거시: objects)")
    val objects: List<LegacySeqStringRequest> = emptyList(),
    @field:Schema(description = "예시 입출력 목록(레거시: exampleIO)")
    val exampleIO: List<LegacyExampleIORequest> = emptyList(),
    @field:Schema(description = "레거시 리포트 타입(현재 저장에 미사용)", example = "CS")
    val reportType: String? = null,
    @field:Schema(description = "오픈 시각(레거시: startAt, UTC)", example = "2026-03-03T00:00:00Z")
    val startAt: Instant,
    @field:Schema(description = "마감 시각(레거시: endAt, UTC)", example = "2026-03-10T23:59:59Z")
    val endAt: Instant,
    @field:Schema(description = "레거시 난이도(level)", example = "VERYHIGH")
    val level: LegacyAssignmentLevel,
    @field:Min(1)
    @field:Schema(description = "제한 시간(분)", example = "60")
    val timeLimitMinutes: Int = 60,
) {
    val weekNo: Int
        get() = week

    val seqInWeek: Int
        get() = seq

    val difficulty: AssignmentDifficulty
        get() = when (level) {
            LegacyAssignmentLevel.LOW -> AssignmentDifficulty.LOW
            LegacyAssignmentLevel.MEDIUM -> AssignmentDifficulty.MID
            LegacyAssignmentLevel.HIGH -> AssignmentDifficulty.HIGH
            LegacyAssignmentLevel.VERYHIGH -> AssignmentDifficulty.VERY_HIGH
        }

    val contentMd: String
        get() {
            val base = content.trim()
            if (objects.isEmpty()) {
                return base
            }
            val goals = objects
                .sortedBy { it.seq }
                .joinToString("\n") { "- ${it.content.trim()}" }
            return "$base\n\n## 학습 정리 목표\n$goals"
        }

    val openAt: Instant
        get() = startAt

    val dueAt: Instant
        get() = endAt

    val requirements: List<CreateAssignmentRequirementRequest>
        get() = requirement
            .sortedBy { it.seq }
            .map {
                CreateAssignmentRequirementRequest(
                    sortOrder = it.seq,
                    requirementText = it.content.trim(),
                )
            }

    val examples: List<CreateAssignmentExampleRequest>
        get() = exampleIO
            .sortedBy { it.seq }
            .map {
                CreateAssignmentExampleRequest(
                    seq = it.seq,
                    inputText = it.input,
                    outputText = it.output,
                    description = null,
                )
            }
}

@Schema(description = "과제 요구사항 응답")
data class AssignmentRequirementResponse(
    @field:Schema(description = "정렬 순서", example = "1")
    val sortOrder: Int,
    @field:Schema(description = "요구사항 내용", example = "함수 분리 필수")
    val requirementText: String,
) {
    @get:Schema(description = "요구사항 순번(레거시 필드)", example = "1")
    val seq: Int
        get() = sortOrder

    @get:Schema(description = "요구사항 내용(레거시 필드)", example = "함수 분리 필수")
    val content: String
        get() = requirementText
}

@Schema(description = "과제 예시 입출력 응답")
data class AssignmentExampleResponse(
    @field:Schema(description = "예시 순번", example = "1")
    val seq: Int,
    @field:Schema(description = "입력 예시", example = "ADD 1\\nCLOSE")
    val inputText: String,
    @field:Schema(description = "출력 예시", example = "+1")
    val outputText: String,
    @field:Schema(description = "예시 설명")
    val description: String?,
) {
    @get:Schema(description = "입력 예시(레거시 필드)", example = "ADD 1\\nCLOSE")
    val input: String
        get() = inputText

    @get:Schema(description = "출력 예시(레거시 필드)", example = "+1")
    val output: String
        get() = outputText
}

@Schema(description = "과제 요약 응답")
data class AssignmentSummaryResponse(
    @field:Schema(description = "과제 ID", example = "assignment-1")
    val id: String,
    @field:Schema(description = "주차 번호", example = "1")
    val weekNo: Int,
    @field:Schema(description = "주차 내 순번", example = "1")
    val seqInWeek: Int,
    @field:Schema(description = "과제 제목", example = "터미널 계산기")
    val title: String,
    @field:Schema(description = "난이도", example = "MID")
    val difficulty: AssignmentDifficulty,
    @field:Schema(description = "오픈 시각(UTC)")
    val openAt: Instant,
    @field:Schema(description = "마감 시각(UTC)")
    val dueAt: Instant,
    @field:Schema(description = "과제 상태", example = "PUBLISHED")
    val status: AssignmentStatus,
) {
    @get:Schema(description = "주차 번호(레거시 필드)", example = "1")
    val week: Int
        get() = weekNo

    @get:Schema(description = "주차 내 순번(레거시 필드)", example = "1")
    val seq: Int
        get() = seqInWeek

    @get:Schema(description = "레거시 난이도", example = "MEDIUM")
    val level: LegacyAssignmentLevel
        get() = difficulty.toLegacyLevel()

    @get:Schema(description = "오픈 시각(레거시: startAt, UTC)")
    val startAt: Instant
        get() = openAt

    @get:Schema(description = "마감 시각(레거시: endAt, UTC)")
    val endAt: Instant
        get() = dueAt
}

@Schema(description = "과제 상세 응답")
data class AssignmentDetailResponse(
    @field:Schema(description = "과제 ID", example = "assignment-1")
    val id: String,
    @field:Schema(description = "코스 슬러그", example = "back-basic")
    val courseSlug: String,
    @field:Schema(description = "주차 번호", example = "1")
    val weekNo: Int,
    @field:Schema(description = "주차 내 순번", example = "1")
    val seqInWeek: Int,
    @field:Schema(description = "과제 제목", example = "터미널 계산기")
    val title: String,
    @field:Schema(description = "난이도", example = "MID")
    val difficulty: AssignmentDifficulty,
    @field:Schema(description = "마크다운 본문")
    val contentMd: String,
    @field:Schema(description = "제한 시간(분)", example = "60")
    val timeLimitMinutes: Int,
    @field:Schema(description = "오픈 시각(UTC)")
    val openAt: Instant,
    @field:Schema(description = "마감 시각(UTC)")
    val dueAt: Instant,
    @field:Schema(description = "과제 상태", example = "DRAFT")
    val status: AssignmentStatus,
    @field:Schema(description = "게시 시각(UTC)")
    val publishedAt: Instant?,
    @field:Schema(description = "요구사항 목록")
    val requirements: List<AssignmentRequirementResponse>,
    @field:Schema(description = "예시 입출력 목록")
    val examples: List<AssignmentExampleResponse>,
) {
    @get:Schema(description = "주차 번호(레거시 필드)", example = "1")
    val week: Int
        get() = weekNo

    @get:Schema(description = "주차 내 순번(레거시 필드)", example = "1")
    val seq: Int
        get() = seqInWeek

    @get:Schema(description = "레거시 난이도", example = "MEDIUM")
    val level: LegacyAssignmentLevel
        get() = difficulty.toLegacyLevel()

    @get:Schema(description = "과제 본문(레거시 필드)")
    val content: String
        get() = contentMd

    @get:Schema(description = "오픈 시각(레거시: startAt, UTC)")
    val startAt: Instant
        get() = openAt

    @get:Schema(description = "마감 시각(레거시: endAt, UTC)")
    val endAt: Instant
        get() = dueAt

    @get:Schema(description = "요구사항 목록(레거시 필드)")
    val requirement: List<LegacySeqStringRequest>
        get() = requirements.map { LegacySeqStringRequest(seq = it.sortOrder, content = it.requirementText) }

    @get:Schema(description = "학습 목표 목록(레거시 필드, 현재 미저장)", example = "[]")
    val objects: List<LegacySeqStringRequest>
        get() = emptyList()

    @get:Schema(description = "예시 입출력 목록(레거시 필드)")
    val exampleIO: List<LegacyExampleIORequest>
        get() = examples.map { LegacyExampleIORequest(seq = it.seq, input = it.inputText, output = it.outputText) }
}

@Schema(description = "과제 게시 응답")
data class PublishAssignmentResponse(
    @field:Schema(description = "과제 ID", example = "assignment-1")
    val assignmentId: String,
    @field:Schema(description = "코스 슬러그", example = "back-basic")
    val courseSlug: String,
    @field:Schema(description = "게시 후 상태", example = "PUBLISHED")
    val status: AssignmentStatus,
    @field:Schema(description = "게시 시각(UTC)")
    val publishedAt: Instant?,
)

@Schema(description = "과제 배포 트리거 응답")
data class TriggerDeliveriesResponse(
    @field:Schema(description = "과제 ID", example = "assignment-1")
    val assignmentId: String,
    @field:Schema(description = "코스 슬러그", example = "back-basic")
    val courseSlug: String,
    @field:Schema(description = "배포 대상 수", example = "42")
    val targetCount: Int,
    @field:Schema(description = "배포 성공 수", example = "40")
    val deliveredCount: Int,
    @field:Schema(description = "배포 실패 수", example = "2")
    val failedCount: Int,
)

@Schema(description = "배포 결과 응답")
data class AssignmentDeliveryResponse(
    @field:Schema(description = "유저 ID", example = "user-1")
    val userId: String,
    @field:Schema(description = "배포 상태", example = "DELIVERED")
    val status: AssignmentDeliveryStatus,
    @field:Schema(description = "배포 시각(UTC)")
    val deliveredAt: Instant?,
    @field:Schema(description = "실패 사유")
    val failureReason: String?,
)
