package com.example.aandi_post_web_server.course.entity

import com.example.aandi_post_web_server.course.enum.CourseStatus
import com.example.aandi_post_web_server.course.enum.CourseTrack
import com.example.aandi_post_web_server.course.enum.CoursePhase
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate

data class CourseMetadata(
    val title: String,
    val description: String? = null,
    val phase: CoursePhase? = null,
    val attributes: Map<String, Any?> = emptyMap(),
)

@Document(collection = "courses")
data class Course(
    @Id
    val id: String? = null,
    @Indexed(unique = true)
    val slug: String,
    val fieldTag: CourseTrack,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val metadata: CourseMetadata,
    val status: CourseStatus = CourseStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    constructor(
        id: String? = null,
        title: String,
        slug: String,
        description: String? = null,
        phase: CoursePhase = CoursePhase.BASIC,
        targetTrack: CourseTrack = CourseTrack.FL,
        status: CourseStatus = CourseStatus.ACTIVE,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
    ) : this(
        id = id,
        slug = slug,
        fieldTag = targetTrack,
        startDate = LocalDate.of(1970, 1, 1),
        endDate = LocalDate.of(2099, 12, 31),
        metadata = CourseMetadata(
            title = title,
            description = description,
            phase = phase,
            attributes = emptyMap(),
        ),
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
