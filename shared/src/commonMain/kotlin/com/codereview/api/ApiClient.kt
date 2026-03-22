package com.codereview.api

import com.codereview.httpClient
import com.codereview.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class CodeReviewApiClient(
    private val baseUrl: String,
    private val client: HttpClient = httpClient
) {

    suspend fun reviewCode(code: String, language: String = "Select Language"): ApiResult<ReviewResponse> {
        return try {
            val response: ReviewResponse = client.post("$baseUrl/review") {
                contentType(ContentType.Application.Json)
                setBody(ReviewRequest(code = code, language = language))
            }.body()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }


    suspend fun getHistory(limit: Int = 20): ApiResult<List<HistoryItem>> {
        return try {
            val response: List<HistoryItem> = client.get("$baseUrl/history") {
                parameter("limit", limit)
            }.body()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getReviewById(id: Int): ApiResult<ReviewResponse> {
        return try {
            val response: ReviewResponse = client.get("$baseUrl/history/$id").body()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}
