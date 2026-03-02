package com.example.aandi_post_web_server.course.entity

import com.example.aandi_post_web_server.course.enum.CoursePhase
import com.example.aandi_post_web_server.course.enum.CourseStatus
import com.example.aandi_post_web_server.course.enum.CourseTrack
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "courses")
data class Course(
    @Id
    val id: String? = null,
    val title: String,
    @Indexed(unique = true)
    val slug: String,
    val description: String? = null,
    val phase: CoursePhase = CoursePhase.BASIC,
    val targetTrack: CourseTrack = CourseTrack.FL,
    val status: CourseStatus = CourseStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
