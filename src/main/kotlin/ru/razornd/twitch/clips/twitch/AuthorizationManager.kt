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

import ru.razornd.twitch.clips.logger
import ru.razornd.twitch.clips.twitch.AuthorizationClient.AccessToken
import java.time.Clock
import java.time.Duration
import java.time.Instant


private val log = logger<AuthorizationManager>()

class AuthorizationManager(
    val client: ClientAuthorization,
    private val authorizationClient: AuthorizationClient,
    private val tokenStore: TokenStore,
    private val clock: Clock = Clock.systemUTC(),
    private val tokenSkewDuration: Duration = Duration.ofSeconds(3)
) {

    suspend fun authorizationToken(): AuthorizationToken = getTokenFromStore() ?: fetchNewTokenAndSave()

    data class AuthorizationToken(val value: String, val expireAt: Instant)

    data class ClientAuthorization(val clientId: String, val clientSecret: String)

    private suspend fun fetchNewTokenAndSave(): AuthorizationToken {
        val token = authorizationClient.authorize(client).convert()

        log.debug("Fetched new token: {}", token)

        tokenStore.storeToken(token)

        return token
    }

    private suspend fun getTokenFromStore(): AuthorizationToken? {
        val token = tokenStore.getCurrentToken()
        if (token == null) {
            log.debug("Store doesn't contain token")
            return null
        }
        if (token.isExpired()) {
            log.debug("Token from store expired: {}", token)
            return null
        }
        return token
    }

    private fun AuthorizationToken.isExpired() = expireAt.isBefore(currentTimeWithSkew())

    private fun AccessToken.convert() = AuthorizationToken(token, clock.instant().plus(expireDuration))

    private fun currentTimeWithSkew(): Instant = clock.instant().plus(tokenSkewDuration)

}
