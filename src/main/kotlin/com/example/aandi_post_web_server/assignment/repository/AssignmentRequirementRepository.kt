package com.example.aandi_post_web_server.assignment.repository

import com.example.aandi_post_web_server.assignment.entity.AssignmentRequirement
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface AssignmentRequirementRepository : ReactiveMongoRepository<AssignmentRequirement, String> {
    fun findAllByAssignmentIdOrderBySortOrder(assignmentId: String): Flux<AssignmentRequirement>
}
