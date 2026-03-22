package com.codereview.repository

import com.codereview.BASE_URL
import com.codereview.api.CodeReviewApiClient
import com.codereview.models.*

class CodeReviewRepository(
    private val api: CodeReviewApiClient = CodeReviewApiClient(BASE_URL)
) {
    suspend fun submitReview(code: String, language: String): ApiResult<ReviewResponse> {
        if (code.isBlank()) return ApiResult.Error("Code cannot be empty")
        return api.reviewCode(code.trim(), language)
    }

    suspend fun loadHistory(): ApiResult<List<HistoryItem>> = api.getHistory()

    suspend fun getReviewDetail(id: Int): ApiResult<ReviewResponse> = api.getReviewById(id)
}
