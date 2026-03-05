package com.example.aandi_post_web_server.common.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.Duration
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtPolicyProperties::class)
class SecurityConfig {

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        jwtDecoder: ReactiveJwtDecoder,
    ): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange {
                it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.pathMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-ui/index.html",
                ).permitAll()
                it.pathMatchers("/v1/admin/**").hasRole("ADMIN")
                it.pathMatchers("/v1/report/**", "/v1/courses/**").hasAnyRole("USER", "ORGANIZER", "ADMIN")
                it.anyExchange().permitAll()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { exchange, _ ->
                    val response = exchange.response
                    response.statusCode = HttpStatus.UNAUTHORIZED
                    response.headers.contentType = MediaType.APPLICATION_JSON
                    val body = "{\"message\":\"Unauthorized\"}".toByteArray()
                    response.writeWith(Mono.just(response.bufferFactory().wrap(body)))
                }
                exceptions.accessDeniedHandler { exchange, _ ->
                    val response = exchange.response
                    response.statusCode = HttpStatus.FORBIDDEN
                    response.headers.contentType = MediaType.APPLICATION_JSON
                    val body = "{\"message\":\"Forbidden\"}".toByteArray()
                    response.writeWith(Mono.just(response.bufferFactory().wrap(body)))
                }
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtDecoder(jwtDecoder)
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .build()

    @Bean
    fun jwtDecoder(jwtPolicy: JwtPolicyProperties): ReactiveJwtDecoder {
        val secret = jwtPolicy.secret
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "security.jwt.secret must be at least 32 bytes"
        }

        val secretKey = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()

        val timestampValidator = JwtTimestampValidator(Duration.ofSeconds(jwtPolicy.clockSkewSeconds))
        val issuerValidator = JwtIssuerValidator(jwtPolicy.issuer)
        val audienceValidator = RequiredAudienceValidator(jwtPolicy.audience)
        val claimsValidator = AccessTokenClaimsValidator(Duration.ofSeconds(jwtPolicy.clockSkewSeconds))

        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(timestampValidator, issuerValidator, audienceValidator, claimsValidator),
        )
        return decoder
    }

    private fun jwtAuthenticationConverter(): Converter<Jwt, Mono<AbstractAuthenticationToken>> =
        Converter { jwt ->
            val role = UserRole.fromClaim(jwt.getClaimAsString("role"))
            val authorities = role?.grantedAuthorities() ?: UserRole.USER.grantedAuthorities()
            Mono.just(JwtAuthenticationToken(jwt, authorities, jwt.subject))
        }
}
