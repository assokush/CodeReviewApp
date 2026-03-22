package com.codereview.repository

import com.codereview.api.CodeReviewApiClient
import com.codereview.models.ApiResult
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RepositoryTest {

    private fun makeRepository(handler: MockRequestHandler): CodeReviewRepository {
        val httpClient = HttpClient(MockEngine) {
            engine { addHandler(handler) }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        return CodeReviewRepository(CodeReviewApiClient("http://test", httpClient))
    }

    private val successHandler: MockRequestHandler = {
        respond(
            content = """{"id":1,"language":"kotlin","summary":"Good","issues":[],"suggestions":[],"score":9,"created_at":"2024-01-01T00:00:00"}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )
    }

    // ── submitReview ────────────────────────────────────────────────────────────

    @Test
    fun submitReview_blankCode_returnsError_withoutCallingApi() = runTest {
        var apiCalled = false
        val repo = makeRepository {
            apiCalled = true
            error("API should not be called for blank code")
        }
        val result = repo.submitReview("   ", "kotlin")

        assertIs<ApiResult.Error>(result)
        assertEquals("Code cannot be empty", (result as ApiResult.Error).message)
        assertEquals(false, apiCalled)
    }

    @Test
    fun submitReview_emptyString_returnsError() = runTest {
        val repo = makeRepository { error("Should not be called") }
        val result = repo.submitReview("", "kotlin")
        assertIs<ApiResult.Error>(result)
        assertEquals("Code cannot be empty", (result as ApiResult.Error).message)
    }

    @Test
    fun submitReview_validCode_returnsSuccess() = runTest {
        val repo = makeRepository(successHandler)
        val result = repo.submitReview("fun main() {}", "kotlin")

        assertIs<ApiResult.Success<*>>(result)
        assertEquals(9, (result as ApiResult.Success).data.score)
    }

    @Test
    fun submitReview_apiError_propagatesError() = runTest {
        val repo = makeRepository { throw Exception("Connection refused") }
        val result = repo.submitReview("fun main() {}", "kotlin")

        assertIs<ApiResult.Error>(result)
    }

    // ── loadHistory ─────────────────────────────────────────────────────────────

    @Test
    fun loadHistory_success_returnsItems() = runTest {
        val repo = makeRepository {
            respond(
                content = """[{"id":1,"language":"python","summary":"OK","score":7,"created_at":"2024-01-01T00:00:00"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val result = repo.loadHistory()

        assertIs<ApiResult.Success<*>>(result)
        assertEquals(1, (result as ApiResult.Success).data.size)
    }

    @Test
    fun loadHistory_networkError_returnsError() = runTest {
        val repo = makeRepository { throw Exception("No internet") }
        val result = repo.loadHistory()

        assertIs<ApiResult.Error>(result)
    }

    // ── getReviewDetail ─────────────────────────────────────────────────────────

    @Test
    fun getReviewDetail_success() = runTest {
        val repo = makeRepository(successHandler)
        val result = repo.getReviewDetail(1)

        assertIs<ApiResult.Success<*>>(result)
        assertEquals(1, (result as ApiResult.Success).data.id)
    }

    @Test
    fun getReviewDetail_notFound_returnsError() = runTest {
        val repo = makeRepository {
            respond(
                content = """{"detail":"Review not found"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val result = repo.getReviewDetail(999)
        assertIs<ApiResult.Error>(result)
    }
}
