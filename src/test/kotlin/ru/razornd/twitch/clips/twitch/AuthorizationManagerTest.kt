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
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.razornd.twitch.clips.twitch.AuthorizationClient.AccessToken
import ru.razornd.twitch.clips.twitch.AuthorizationManager.AuthorizationToken
import ru.razornd.twitch.clips.twitch.AuthorizationManager.ClientAuthorization
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset.UTC

class AuthorizationManagerTest {

    private val client = mockk<AuthorizationClient>()

    private val tokenStore = mockk<TokenStore>(relaxUnitFun = true)

    private val clientAuthorization = ClientAuthorization("5042007", "pPIX8PnkXj")

    private val currentTime = Instant.parse("2023-01-30T18:00:00Z")

    private val manager = AuthorizationManager(
        clientAuthorization,
        client,
        tokenStore,
        Clock.fixed(currentTime, UTC)
    )

    @Test
    fun `active token in store`() {
        val expected = AuthorizationToken(
            "bjxXPZAOOGmzlNVaRtD8tajUOWpcdXr2gv7qAiJS",
            Instant.parse("2023-01-30T18:30:00Z")
        )

        coEvery { tokenStore.getCurrentToken() } returns expected

        val token = runBlocking { manager.authorizationToken() }

        assertThat(token).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `fetch new token`() {
        val accessToken = AccessToken("prlKC8TYlOyT23ktXmyWT9", Duration.ofMinutes(10))
        val expected = AuthorizationToken(accessToken.token, currentTime.plus(accessToken.expireDuration))

        coEvery { tokenStore.getCurrentToken() } returns null
        coEvery { client.authorize(clientAuthorization) } returns accessToken

        val token = runBlocking { manager.authorizationToken() }

        assertThat(token).usingRecursiveComparison().isEqualTo(expected)

        coVerify { tokenStore.storeToken(expected) }
    }

    @Test
    fun `expired token in store`() {
        val accessToken = AccessToken("prlKC8TYlOyT23ktXmyWT9", Duration.ofMinutes(10))
        val expected = AuthorizationToken(accessToken.token, currentTime.plus(accessToken.expireDuration))

        coEvery { tokenStore.getCurrentToken() } returns AuthorizationToken(
            "xh0ibggbY3gWj8fELt",
            Instant.parse("2023-01-30T17:50:00Z")
        )
        coEvery { client.authorize(clientAuthorization) } returns accessToken

        val token = runBlocking { manager.authorizationToken() }

        assertThat(token).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `token that expired at skew duration`() {
        val accessToken = AccessToken("prlKC8TYlOyT23ktXmyWT9", Duration.ofMinutes(10))
        val expected = AuthorizationToken(accessToken.token, currentTime.plus(accessToken.expireDuration))

        coEvery { tokenStore.getCurrentToken() } returns AuthorizationToken(
            "xh0ibggbY3gWj8fELt",
            Instant.parse("2023-01-30T18:05:00Z")
        )
        coEvery { client.authorize(clientAuthorization) } returns accessToken

        val token = runBlocking {
            AuthorizationManager(
                clientAuthorization,
                client,
                tokenStore,
                Clock.fixed(currentTime, UTC),
                Duration.ofMinutes(10)
            ).authorizationToken()
        }

        assertThat(token).usingRecursiveComparison().isEqualTo(expected)
    }
}
