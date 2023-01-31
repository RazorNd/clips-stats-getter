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

import kotlinx.coroutines.reactor.mono
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction

class AuthorizationInterceptor(
    private val authorizationManager: AuthorizationManager
) : ExchangeFilterFunction {
    override fun filter(request: ClientRequest, next: ExchangeFunction) = authorize(request).flatMap(next::exchange)

    private fun authorize(request: ClientRequest) = mono { addAuthorization(request) }

    private suspend fun addAuthorization(request: ClientRequest): ClientRequest {
        val token = authorizationManager.authorizationToken()

        return ClientRequest.from(request)
            .headers {
                it.setBearerAuth(token.value)
                it["Client-Id"] = authorizationManager.client.clientId
            }
            .build()
    }

}

