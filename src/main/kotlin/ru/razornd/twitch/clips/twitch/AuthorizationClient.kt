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

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.razornd.twitch.clips.twitch.AuthorizationManager.ClientAuthorization
import java.time.Duration

private const val CLIENT_CREDENTIALS = "client_credentials"

class AuthorizationClient(private val webClient: WebClient) {

    @RegisterReflectionForBinding(classes = [AccessToken::class])
    suspend fun authorize(clientAuthorization: ClientAuthorization): AccessToken {
        return webClient.post()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(clientAuthorization.makeGrantRequest(CLIENT_CREDENTIALS))
            .retrieve()
            .awaitBody()
    }

    private fun ClientAuthorization.makeGrantRequest(grantType: String) = LinkedMultiValueMap(
        mapOf(
            "client_id" to listOf(clientId),
            "client_secret" to listOf(clientSecret),
            "grant_type" to listOf(grantType)
        )
    )

    data class AccessToken(
        @JsonProperty("access_token")
        val token: String,
        @JsonProperty("expires_in")
        val expireDuration: Duration
    )

}
