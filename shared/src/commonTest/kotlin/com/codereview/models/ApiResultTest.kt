package com.codereview.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApiResultTest {

    @Test
    fun success_holdsData() {
        val result: ApiResult<String> = ApiResult.Success("hello")
        assertIs<ApiResult.Success<String>>(result)
        assertEquals("hello", result.data)
    }

    @Test
    fun error_holdsMessage() {
        val result: ApiResult<String> = ApiResult.Error("something went wrong")
        assertIs<ApiResult.Error>(result)
        assertEquals("something went wrong", result.message)
    }

    @Test
    fun loading_isSingleton() {
        val a = ApiResult.Loading
        val b = ApiResult.Loading
        assertTrue(a === b)
    }

    @Test
    fun reviewResponse_dataClassEquality() {
        val r1 = ReviewResponse(1, "kotlin", "Good code", listOf("issue1"), listOf("tip1"), 8, "2024-01-01")
        val r2 = ReviewResponse(1, "kotlin", "Good code", listOf("issue1"), listOf("tip1"), 8, "2024-01-01")
        assertEquals(r1, r2)
    }

    @Test
    fun reviewResponse_differentId_notEqual() {
        val r1 = ReviewResponse(1, "kotlin", "Good", emptyList(), emptyList(), 8, "2024-01-01")
        val r2 = ReviewResponse(2, "kotlin", "Good", emptyList(), emptyList(), 8, "2024-01-01")
        assertTrue(r1 != r2)
    }

    @Test
    fun historyItem_dataClassEquality() {
        val h1 = HistoryItem(1, "python", "OK code", 7, "2024-01-01")
        val h2 = HistoryItem(1, "python", "OK code", 7, "2024-01-01")
        assertEquals(h1, h2)
    }

    @Test
    fun reviewRequest_defaultLanguage() {
        val req = ReviewRequest(code = "fun main() {}")
        assertEquals("auto-detect", req.language)
    }
}
