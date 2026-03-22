package com.codereview.api

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

class ApiClientTest {

    private fun buildClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine) {
        engine { addHandler(handler) }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    // ── reviewCode ──────────────────────────────────────────────────────────────

    @Test
    fun reviewCode_success_returnsReviewResponse() = runTest {
        val client = buildClient {
            respond(
                content = """{"id":1,"language":"kotlin","summary":"Looks good","issues":[],"suggestions":["Add tests"],"score":8,"created_at":"2024-01-01T00:00:00"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val api = CodeReviewApiClient("http://test", client)
        val result = api.reviewCode("fun main() {}", "kotlin")

        assertIs<ApiResult.Success<*>>(result)
        val data = (result as ApiResult.Success).data
        assertEquals(1, data.id)
        assertEquals("kotlin", data.language)
        assertEquals(8, data.score)
        assertEquals(listOf("Add tests"), data.suggestions)
    }

    @Test
    fun reviewCode_networkError_returnsError() = runTest {
        val client = buildClient { throw Exception("Network failure") }
        val api = CodeReviewApiClient("http://test", client)

        val result = api.reviewCode("fun main() {}", "kotlin")

        assertIs<ApiResult.Error>(result)
        assertEquals("Network failure", (result as ApiResult.Error).message)
    }

    @Test
    fun reviewCode_serverError_returnsError() = runTest {
        val client = buildClient {
            respond(
                content = """{"detail":"Internal server error"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val api = CodeReviewApiClient("http://test", client)

        val result = api.reviewCode("fun main() {}", "kotlin")
        assertIs<ApiResult.Error>(result)
    }

    // ── getHistory ──────────────────────────────────────────────────────────────

    @Test
    fun getHistory_success_returnsItems() = runTest {
        val client = buildClient {
            respond(
                content = """[{"id":1,"language":"python","summary":"OK","score":7,"created_at":"2024-01-01T00:00:00"},{"id":2,"language":"kotlin","summary":"Good","score":9,"created_at":"2024-01-02T00:00:00"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val api = CodeReviewApiClient("http://test", client)
        val result = api.getHistory()

        assertIs<ApiResult.Success<*>>(result)
        val items = (result as ApiResult.Success).data
        assertEquals(2, items.size)
        assertEquals("python", items[0].language)
        assertEquals(7, items[0].score)
    }

    @Test
    fun getHistory_emptyList_returnsEmptySuccess() = runTest {
        val client = buildClient {
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val api = CodeReviewApiClient("http://test", client)
        val result = api.getHistory()

        assertIs<ApiResult.Success<*>>(result)
        assertEquals(0, (result as ApiResult.Success).data.size)
    }

    @Test
    fun getHistory_networkError_returnsError() = runTest {
        val client = buildClient { throw Exception("Timeout") }
        val api = CodeReviewApiClient("http://test", client)

        val result = api.getHistory()
        assertIs<ApiResult.Error>(result)
        assertEquals("Timeout", (result as ApiResult.Error).message)
    }

    // ── getReviewById ───────────────────────────────────────────────────────────

    @Test
    fun getReviewById_success() = runTest {
        val client = buildClient {
            respond(
                content = """{"id":5,"language":"java","summary":"Could be improved","issues":["No generics"],"suggestions":[],"score":6,"created_at":"2024-03-01T00:00:00"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val api = CodeReviewApiClient("http://test", client)
        val result = api.getReviewById(5)

        assertIs<ApiResult.Success<*>>(result)
        assertEquals(5, (result as ApiResult.Success).data.id)
        assertEquals("java", result.data.language)
    }

    @Test
    fun getReviewById_notFound_returnsError() = runTest {
        val client = buildClient {
            respond(
                content = """{"detail":"Review not found"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val api = CodeReviewApiClient("http://test", client)

        val result = api.getReviewById(999)
        assertIs<ApiResult.Error>(result)
    }
}
