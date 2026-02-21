package com.example.aandi_post_web_server.course.repository

import com.example.aandi_post_web_server.course.entity.Course
import com.example.aandi_post_web_server.course.enum.CourseStatus
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CourseRepository : ReactiveMongoRepository<Course, String> {
    fun findBySlug(slug: String): Mono<Course>
    fun existsBySlug(slug: String): Mono<Boolean>
    fun findAllByStatus(status: CourseStatus): Flux<Course>
}
