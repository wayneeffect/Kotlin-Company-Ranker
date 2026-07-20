package com.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureSecurity() {
    // 1. Hardened Security Headers
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
    }

    // 2. Restrictive CORS
    install(CORS) {
        val allowedHost = System.getenv("ALLOWED_ORIGIN") ?: "*"
        if (allowedHost == "*") {
            anyHost()
        } else {
            allowHost(allowedHost, schemes = listOf("http", "https"))
        }
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
    }

    // 3. Rate Limiting Strategy
    install(RateLimit) {
        register(RateLimitName("strict_write")) {
            rateLimiter(limit = 10, refillPeriod = 60.seconds) // Max 10 writes per minute
        }
        register(RateLimitName("public_read")) {
            rateLimiter(limit = 100, refillPeriod = 60.seconds) // Max 100 reads per minute
        }
    }
}
