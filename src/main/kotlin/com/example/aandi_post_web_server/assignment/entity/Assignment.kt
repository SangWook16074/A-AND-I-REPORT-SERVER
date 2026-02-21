package com.example.aandi_post_web_server.assignment.entity

import com.example.aandi_post_web_server.assignment.enum.AssignmentDifficulty
import com.example.aandi_post_web_server.assignment.enum.AssignmentStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "assignments")
@CompoundIndex(name = "ux_assignment_course_week_seq", def = "{'courseId': 1, 'weekNo': 1, 'seqInWeek': 1}", unique = true)
data class Assignment(
    @Id
    val id: String? = null,
    val courseId: String,
    val createdBy: String,
    val weekNo: Int,
    val seqInWeek: Int,
    val title: String,
    val difficulty: AssignmentDifficulty,
    val contentMd: String,
    val timeLimitMinutes: Int,
    val openAt: Instant,
    val dueAt: Instant,
    val status: AssignmentStatus = AssignmentStatus.DRAFT,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val publishedAt: Instant? = null,
)
