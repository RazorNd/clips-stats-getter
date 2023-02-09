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

package ru.razornd.twitch.clips.configuration

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import ru.razornd.twitch.clips.twitch.*
import ru.razornd.twitch.clips.twitch.AuthorizationManager.ClientAuthorization

@Configuration
@EnableConfigurationProperties(TwitchProperties::class)
open class TwitchConfiguration(private val properties: TwitchProperties) {
    @Bean
    open fun twitchClient(builder: WebClient.Builder, interceptor: AuthorizationInterceptor): TwitchClient {
        val webClient = builder.baseUrl(properties.baseUrl)
            .filter(interceptor)
            .build()
        return TwitchClient(webClient)
    }

    @Bean
    open fun authorizationClient(builder: WebClient.Builder): AuthorizationClient {
        val webClient = builder.baseUrl(properties.authorizationUrl).build()

        return AuthorizationClient(webClient)
    }

    @Bean
    open fun tokenStore() = SimpleTokenStore()

    @Bean
    open fun authorizationManager(
        authorizationClient: AuthorizationClient,
        tokenStore: TokenStore,
    ): AuthorizationManager = AuthorizationManager(properties.clientAuthorization, authorizationClient, tokenStore)

    @Bean
    open fun authorizationInterceptor(authorizationManager: AuthorizationManager) =
        AuthorizationInterceptor(authorizationManager)

    private val TwitchProperties.clientAuthorization get() = ClientAuthorization(clientId, secret)
}
