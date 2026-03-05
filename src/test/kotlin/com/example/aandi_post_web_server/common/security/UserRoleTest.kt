package com.example.aandi_post_web_server.common.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class UserRoleTest : StringSpec({
    "fromClaim은 대소문자/공백을 정규화한다" {
        UserRole.fromClaim(" admin ") shouldBe UserRole.ADMIN
    }

    "fromClaim은 null/blank/unknown이면 null을 반환한다" {
        UserRole.fromClaim(null).shouldBeNull()
        UserRole.fromClaim(" ").shouldBeNull()
        UserRole.fromClaim("guest").shouldBeNull()
    }

    "권한 매핑은 상위 역할이 하위 권한을 포함한다" {
        UserRole.USER.grantedAuthorities().map { it.authority } shouldContainExactly listOf("ROLE_USER")
        UserRole.ORGANIZER.grantedAuthorities().map { it.authority } shouldContainExactly listOf("ROLE_ORGANIZER", "ROLE_USER")
        UserRole.ADMIN.grantedAuthorities().map { it.authority } shouldContainExactly listOf("ROLE_ADMIN", "ROLE_ORGANIZER", "ROLE_USER")
    }
})
