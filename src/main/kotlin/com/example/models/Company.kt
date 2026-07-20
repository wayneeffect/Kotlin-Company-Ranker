package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String,
    val name: String,
    val score: Double,
    val category: String,
    val reviewsCount: Int
)

@Serializable
data class CreateCompanyRequest(
    val name: String,
    val score: Double,
    val category: String
)

@Serializable
data class RateCompanyRequest(
    val score: Double
)

@Serializable
data class ApiErrorResponse(
    val status: Int,
    val code: String,
    val message: String
)
