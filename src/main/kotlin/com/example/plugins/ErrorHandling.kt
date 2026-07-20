package com.example.plugins

import com.example.models.ApiErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

class InvalidInputException(message: String) : RuntimeException(message)
class ResourceNotFoundException(message: String) : RuntimeException(message)

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<InvalidInputException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiErrorResponse(400, "BAD_REQUEST", cause.message ?: "Invalid parameters provided.")
            )
        }

        exception<ResourceNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiErrorResponse(404, "NOT_FOUND", cause.message ?: "Resource not found.")
            )
        }

        // Global fallback for unhandled exceptions
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorResponse(500, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.")
            )
        }

        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ApiErrorResponse(404, "ROUTE_NOT_FOUND", "The requested route does not exist."))
        }
    }
}
