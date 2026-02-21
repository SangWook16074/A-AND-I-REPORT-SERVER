package com.example.aandi_post_web_server.course.domain

@JvmInline
value class CourseSlug private constructor(val value: String) {
    companion object {
        fun from(raw: String): CourseSlug {
            val normalized = raw.trim().lowercase()
            require(normalized.isNotBlank()) { "courseSlug는 비어 있을 수 없습니다." }
            return CourseSlug(normalized)
        }
    }
}

@JvmInline
value class CourseId private constructor(val value: String) {
    companion object {
        fun from(raw: String): CourseId {
            val normalized = raw.trim()
            require(normalized.isNotBlank()) { "courseId는 비어 있을 수 없습니다." }
            return CourseId(normalized)
        }
    }
}

@JvmInline
value class UserId private constructor(val value: String) {
    companion object {
        fun from(raw: String): UserId {
            val normalized = raw.trim()
            require(normalized.isNotBlank()) { "userId는 비어 있을 수 없습니다." }
            return UserId(normalized)
        }
    }
}

@JvmInline
value class WeekNo private constructor(val value: Int) {
    companion object {
        fun from(raw: Int): WeekNo {
            require(raw > 0) { "weekNo는 1 이상이어야 합니다." }
            return WeekNo(raw)
        }
    }
}

@JvmInline
value class AssignmentId private constructor(val value: String) {
    companion object {
        fun from(raw: String): AssignmentId {
            val normalized = raw.trim()
            require(normalized.isNotBlank()) { "assignmentId는 비어 있을 수 없습니다." }
            return AssignmentId(normalized)
        }
    }
}
