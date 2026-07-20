package com.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Company(val id: String, val name: String, val score: Double, val category: String, val reviewsCount: Int)

@Serializable
data class CreateCompanyRequest(val name: String, val score: Double, val category: String)

@Serializable
data class RateCompanyRequest(val score: Double)

@Serializable
data class ApiErrorResponse(val status: Int, val code: String, val message: String)

val companyStore = ConcurrentHashMap<String, Company>().apply {
    put("1", Company("1", "Acme Corp", 4.8, "Tech", 120))
    put("2", Company("2", "Globex", 4.2, "Logistics", 85))
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }

        install(DefaultHeaders) {
            header("X-Content-Type-Options", "nosniff")
            header("X-Frame-Options", "DENY")
        }

        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
        }

        install(RateLimit) {
            register(RateLimitName("public_read")) { rateLimiter(limit = 100, refillPeriod = 60.seconds) }
            register(RateLimitName("strict_write")) { rateLimiter(limit = 10, refillPeriod = 60.seconds) }
        }

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, ApiErrorResponse(500, "ERROR", cause.message ?: "Server error"))
            }
            status(HttpStatusCode.NotFound) { call, status ->
                call.respond(status, ApiErrorResponse(404, "NOT_FOUND", "Route not found"))
            }
        }

        routing {
            // SERVE THE DASHBOARD AT ROOT '/'
            singlePageApplication {
                useResources = true
                filesPath = "static"
                defaultPage = "index.html"
            }

            get("/health") {
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
            }

            rateLimit(RateLimitName("public_read")) {
                get("/api/companies") {
                    val category = call.request.queryParameters["category"]
                    val ranked = companyStore.values
                        .filter { category.isNullOrBlank() || it.category.equals(category, ignoreCase = true) }
                        .sortedByDescending { it.score }
                    call.respond(ranked)
                }
            }

            rateLimit(RateLimitName("strict_write")) {
                post("/api/companies") {
                    val req = call.receive<CreateCompanyRequest>()
                    val newCompany = Company(
                        id = UUID.randomUUID().toString(),
                        name = req.name,
                        score = req.score,
                        category = req.category,
                        reviewsCount = 1
                    )
                    companyStore[newCompany.id] = newCompany
                    call.respond(HttpStatusCode.Created, newCompany)
                }

                post("/api/companies/{id}/rate") {
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val req = call.receive<RateCompanyRequest>()
                    
                    val updated = companyStore.compute(id) { _, existing ->
                        existing ?: return@compute null
                        val newCount = existing.reviewsCount + 1
                        val newScore = ((existing.score * existing.reviewsCount) + req.score) / newCount
                        existing.copy(score = (newScore * 10).toInt() / 10.0, reviewsCount = newCount)
                    }

                    if (updated == null) {
                        call.respond(HttpStatusCode.NotFound, ApiErrorResponse(404, "NOT_FOUND", "Company not found"))
                    } else {
                        call.respond(updated)
                    }
                }
            }
        }
    }.start(wait = true)
}
