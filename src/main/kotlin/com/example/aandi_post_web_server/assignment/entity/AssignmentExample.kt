package com.example.aandi_post_web_server.assignment.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "assignment_examples")
@CompoundIndex(name = "ux_assignment_example_seq", def = "{'assignmentId': 1, 'seq': 1}", unique = true)
data class AssignmentExample(
    @Id
    val id: String? = null,
    val assignmentId: String,
    val seq: Int,
    val inputText: String,
    val outputText: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
)
