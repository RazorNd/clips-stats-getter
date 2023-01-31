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

import com.github.tomakehurst.wiremock.client.WireMock.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import ru.razornd.twitch.clips.configuration.TwitchConfiguration
import ru.razornd.twitch.clips.twitch.AuthorizationClient.AccessToken
import java.time.Duration


@AutoConfigureWireMock(port = 0)
@RestClientTest(
    value = [TwitchConfiguration::class],
    properties = [
        "twitch.authorization-url=http://localhost:\${wiremock.server.port}/oauth2/token",
        "twitch.client-id=none",
        "twitch.secret=none"
    ]
)
class AuthorizationClientTest {

    @Autowired
    lateinit var client: AuthorizationClient

    @Test
    fun authorize() {
        val clientId = "7936495"
        val secret = "oxDidjp6tSFjms74lv8iquw"
        val clientAuthorization = AuthorizationManager.ClientAuthorization(clientId, secret)

        stubFor(
            post("/oauth2/token")
                .withHeader(CONTENT_TYPE, containing("application/x-www-form-urlencoded"))
                .withRequestBody(equalTo("client_id=$clientId&client_secret=$secret&grant_type=client_credentials"))
                .willReturn(
                    okJson(
                        """
                            {
                              "access_token": "u33afg6v1yszd78a4jippu80j8ixzh",
                              "expires_in": 4845032,
                              "token_type": "bearer"
                            }
                        """.trimIndent()
                    )
                )
        )


        val token = runBlocking { client.authorize(clientAuthorization) }

        assertThat(token)
            .usingRecursiveComparison()
            .isEqualTo(AccessToken("u33afg6v1yszd78a4jippu80j8ixzh", Duration.ofSeconds(4845032)))
    }
}
