package com.example.aandi_post_web_server.common.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class TokenValidatorsTest : StringSpec({
    "AccessTokenClaimsValidator는 정상 ACCESS 토큰을 통과시킨다" {
        val now = Instant.parse("2026-03-05T00:00:00Z")
        val validator = AccessTokenClaimsValidator(
            clockSkew = Duration.ofSeconds(30),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        val jwt = buildJwt(
            tokenType = "ACCESS",
            role = "USER",
            subject = UUID.randomUUID().toString(),
            jti = "jti-1",
            issuedAt = now,
            audience = listOf("aandi-gateway"),
        )

        val result = validator.validate(jwt)
        result.hasErrors() shouldBe false
    }

    "AccessTokenClaimsValidator는 token_type이 ACCESS가 아니면 실패한다" {
        val now = Instant.parse("2026-03-05T00:00:00Z")
        val validator = AccessTokenClaimsValidator(Duration.ofSeconds(30), Clock.fixed(now, ZoneOffset.UTC))
        val jwt = buildJwt(tokenType = "REFRESH", issuedAt = now)

        val result = validator.validate(jwt)
        result.hasErrors() shouldBe true
    }

    "AccessTokenClaimsValidator는 미래 iat를 거부한다" {
        val now = Instant.parse("2026-03-05T00:00:00Z")
        val validator = AccessTokenClaimsValidator(Duration.ofSeconds(30), Clock.fixed(now, ZoneOffset.UTC))
        val jwt = buildJwt(issuedAt = now.plusSeconds(31))

        val result = validator.validate(jwt)
        result.hasErrors() shouldBe true
    }

    "RequiredAudienceValidator는 필수 aud가 없으면 실패한다" {
        val validator = RequiredAudienceValidator("aandi-gateway")
        val jwt = buildJwt(audience = listOf("another-aud"))

        val result = validator.validate(jwt)
        result.hasErrors() shouldBe true
    }

    "RequiredAudienceValidator는 필수 aud가 있으면 통과한다" {
        val validator = RequiredAudienceValidator("aandi-gateway")
        val jwt = buildJwt(audience = listOf("another-aud", "aandi-gateway"))

        val result = validator.validate(jwt)
        result.hasErrors() shouldBe false
    }
}) {
    companion object {
        private fun buildJwt(
            tokenType: String = "ACCESS",
            role: String = "USER",
            subject: String = UUID.randomUUID().toString(),
            jti: String = "jti-default",
            issuedAt: Instant = Instant.parse("2026-03-05T00:00:00Z"),
            audience: List<String> = listOf("aandi-gateway"),
        ): Jwt =
            Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .claim("token_type", tokenType)
                .claim("role", role)
                .claim("sub", subject)
                .claim("jti", jti)
                .claim("aud", audience)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600))
                .build()
    }
}
