package com.example

import com.example.models.Company
import com.example.models.CreateCompanyRequest
import com.example.models.RateCompanyRequest
import com.example.plugins.InvalidInputException
import com.example.plugins.ResourceNotFoundException
import com.example.plugins.configureErrorHandling
import com.example.plugins.configureSecurity
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// In-Memory Thread-Safe Data Store
val companyStore = ConcurrentHashMap<String, Company>().apply {
    val initial = listOf(
        Company("1", "Acme Corp", 4.8, "Tech", 120),
        Company("2", "Globex", 4.2, "Logistics", 85),
        Company("3", "Soylent Corp", 3.9, "Biotech", 45),
        Company("4", "Initech", 4.5, "Software", 210)
    )
    initial.forEach { put(it.id, it) }
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(Netty, host = "0.0.0.0", port = port, configure = {
        shutdownGracePeriod = 2000
        shutdownTimeout = 3000
    }) {
        install(ContentNegotiation) {
            json()
        }

        configureErrorHandling()
        configureSecurity()

        routing {
            // Healthcheck Endpoint for Render Uptime Monitoring
            get("/health") {
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
            }

            rateLimit(RateLimitName("public_read")) {
                // GET /api/companies?category=Tech
                get("/api/companies") {
                    val category = call.request.queryParameters["category"]
                    val ranked = companyStore.values
                        .filter { category.isNullOrBlank() || it.category.equals(category, ignoreCase = true) }
                        .sortedByDescending { it.score }
                    
                    call.respond(ranked)
                }
            }

            rateLimit(RateLimitName("strict_write")) {
                // POST /api/companies
                post("/api/companies") {
                    val request = runCatching { call.receive<CreateCompanyRequest>() }
                        .getOrElse { throw InvalidInputException("Malformed request body.") }

                    if (request.name.isBlank()) throw InvalidInputException("Company name cannot be empty.")
                    if (request.score !in 0.0..5.0) throw InvalidInputException("Score must be between 0.0 and 5.0.")

                    val newCompany = Company(
                        id = UUID.randomUUID().toString(),
                        name = request.name.take(100), // Enforce length limits
                        score = (request.score * 10).toInt() / 10.0,
                        category = request.category.take(50),
                        reviewsCount = 1
                    )
                    
                    companyStore[newCompany.id] = newCompany
                    call.respond(HttpStatusCode.Created, newCompany)
                }

                // POST /api/companies/{id}/rate
                post("/api/companies/{id}/rate") {
                    val id = call.parameters["id"] ?: throw InvalidInputException("Missing company ID.")
                    val request = runCatching { call.receive<RateCompanyRequest>() }
                        .getOrElse { throw InvalidInputException("Invalid rating payload.") }

                    if (request.score !in 0.0..5.0) throw InvalidInputException("Score must be between 0.0 and 5.0.")

                    // Atomic Update using ConcurrentHashMap
                    val updated = companyStore.compute(id) { _, existing ->
                        existing ?: throw ResourceNotFoundException("Company with ID $id not found.")
                        val newCount = existing.reviewsCount + 1
                        val newScore = ((existing.score * existing.reviewsCount) + request.score) / newCount
                        existing.copy(
                            score = (newScore * 10).toInt() / 10.0,
                            reviewsCount = newCount
                        )
                    }

                    call.respond(updated!!)
                }
            }
        }
    }.start(wait = true)
}
