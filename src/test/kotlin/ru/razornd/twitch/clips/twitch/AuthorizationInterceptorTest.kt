/*
 * Copyright 2023 Daniil Razorenov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ru.razornd.twitch.clips.twitch

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import ru.razornd.twitch.clips.twitch.AuthorizationManager.AuthorizationToken
import ru.razornd.twitch.clips.twitch.AuthorizationManager.ClientAuthorization
import java.net.URI
import java.time.Instant

class AuthorizationInterceptorTest {

    private val clientId = "962860801"

    private val token = "QCMf15wzpGjSHwCPIWH6N9l8"

    private val authorizationManager = mockk<AuthorizationManager>()

    private val interceptor = AuthorizationInterceptor(authorizationManager)

    @Test
    fun filter() {
        val request = ClientRequest.create(HttpMethod.GET, URI("https://api.twitch.com")).build()
        val response = ClientResponse.create(HttpStatus.OK).build()
        val slot = slot<ClientRequest>()

        val exchangeFunction = mockk<ExchangeFunction>()

        @Suppress("ReactiveStreamsUnusedPublisher")
        every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(response)
        every { authorizationManager.client } returns ClientAuthorization(clientId, "none")
        coEvery { authorizationManager.authorizationToken() } returns AuthorizationToken(
            token,
            Instant.parse("2023-01-30T18:00:00Z")
        )

        assertThat(interceptor.filter(request, exchangeFunction).block()).isSameAs(response)

        assertThat(slot.captured)
            .describedAs("Modified request")
            .extracting { it.headers() }
            .satisfies(this::assertAuthorizationHeader, this::assertClientIdHeader)

    }

    private fun assertAuthorizationHeader(headers: HttpHeaders) {
        assertThat(headers["Authorization"])
            .describedAs("Authorization Header")
            .isEqualTo(listOf("Bearer $token"))
    }

    private fun assertClientIdHeader(headers: HttpHeaders) {
        assertThat(headers["Client-Id"])
            .describedAs("Client-Id Header")
            .isEqualTo(listOf(clientId))
    }
}
