package com.codereview

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

actual val httpClient: HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    engine {
        https {
            trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate?>, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate?>, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOf()
            }
        }
    }
}
