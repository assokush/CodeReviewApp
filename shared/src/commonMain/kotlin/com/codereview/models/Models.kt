package com.codereview.models

import kotlinx.serialization.Serializable

@Serializable
data class ReviewRequest(
    val code: String,
    val language: String = "auto-detect"
)

@Serializable
data class ReviewResponse(
    val id: Int,
    val language: String,
    val summary: String,
    val issues: List<String>,
    val suggestions: List<String>,
    val score: Int,
    val created_at: String
)

@Serializable
data class HistoryItem(
    val id: Int,
    val language: String,
    val summary: String,
    val score: Int,
    val created_at: String
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
