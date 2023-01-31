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
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import ru.razornd.twitch.clips.configuration.TwitchConfiguration
import ru.razornd.twitch.clips.twitch.TwitchClient.ClipInformation
import java.time.Instant

// language=JSON
private val responseWithCursor = """
    {
        "data": [
        {
          "id": "AwkwardHelplessSalamanderSwiftRage",
          "url": "https://clips.twitch.tv/AwkwardHelplessSalamanderSwiftRage",
          "embed_url": "https://clips.twitch.tv/embed?clip=AwkwardHelplessSalamanderSwiftRage",
          "broadcaster_id": "67955580",
          "broadcaster_name": "ChewieMelodies",
          "creator_id": "53834192",
          "creator_name": "BlackNova03",
          "video_id": "205586603",
          "game_id": "488191",
          "language": "en",
          "title": "babymetal",
          "view_count": 10,
          "created_at": "2017-11-30T22:34:18Z",
          "thumbnail_url": "https://clips-media-assets.twitch.tv/157589949-preview-480x272.jpg",
          "duration": 60,
          "vod_offset": 480
        }
      ],
      "pagination": {
        "cursor": "R2NPJuG4vCvycWtA"
      }
    }
""".trimIndent()

// language=JSON
private val response = """
    {
      "data": [
        {
          "id": "AwkwardHelplessSalamanderSwiftRage",
          "url": "https://clips.twitch.tv/AwkwardHelplessSalamanderSwiftRage",
          "embed_url": "https://clips.twitch.tv/embed?clip=AwkwardHelplessSalamanderSwiftRage",
          "broadcaster_id": "67955580",
          "broadcaster_name": "ChewieMelodies",
          "creator_id": "53834192",
          "creator_name": "BlackNova03",
          "video_id": "205586603",
          "game_id": "488191",
          "language": "en",
          "title": "babymetal",
          "view_count": 10,
          "created_at": "2017-11-30T22:34:18Z",
          "thumbnail_url": "https://clips-media-assets.twitch.tv/157589949-preview-480x272.jpg",
          "duration": 60,
          "vod_offset": 480
        }
      ],
      "paginator": {}
    }
""".trimIndent()


@AutoConfigureWireMock(port = 0)
@RestClientTest(
    value = [TwitchConfiguration::class],
    properties = [
        "twitch.base-url=http://localhost:\${wiremock.server.port}/helix/",
        "twitch.authorization-url=http://localhost:\${wiremock.server.port}/oauth2/token",
        "twitch.client-id=TestClient",
        "twitch.secret=none",
        "logging.level.ru.razornd.twitch.clips.twitch.TwitchClient=debug"
    ]
)
class TwitchClientTest {

    private val token = "5tsILNIEUJ95jZq21ISf"

    private val broadcasterId = "5504256d-e8cd-4cb1-83ad-1cff2216b320"

    @Autowired
    lateinit var client: TwitchClient

    private val clipInformation = ClipInformation(
        id = "AwkwardHelplessSalamanderSwiftRage",
        broadcasterId = "67955580",
        creatorId = "53834192",
        videoId = "205586603",
        gameId = "488191",
        title = "babymetal",
        viewCount = 10,
        createdAt = Instant.parse("2017-11-30T22:34:18Z"),
        duration = 60,
        vodOffset = 480
    )

    init {
        stubFor(post("/oauth2/token").willReturn(okJson("""{"access_token": "$token", "expires_in": 10000}""")))

        stubFor(
            get(urlPathEqualTo("/helix/clips"))
                .withQueryParam("broadcaster_id", equalTo(broadcasterId))
                .withHeader("Authorization", equalTo("Bearer $token"))
                .withHeader("Client-Id", equalTo("TestClient"))
                .willReturn(okJson(response))
        )

        stubFor(
            get(urlPathEqualTo("/helix/clips"))
                .withQueryParam("broadcaster_id", equalTo(broadcasterId))
                .withQueryParam("started_at", equalTo("2022-12-20T00:00:00Z"))
                .willReturn(okJson(responseWithCursor))
        )

        stubFor(
            get(urlPathEqualTo("/helix/clips"))
                .withQueryParam("broadcaster_id", equalTo(broadcasterId))
                .withQueryParam("started_at", equalTo("2022-12-20T00:00:00Z"))
                .withQueryParam("after", equalTo("R2NPJuG4vCvycWtA"))
                .willReturn(okJson(response))
        )

        stubFor(
            get(urlPathEqualTo("/helix/clips"))
                .withQueryParam("broadcaster_id", equalTo(broadcasterId))
                .withQueryParam("started_at", equalTo("2022-05-01T00:00:00Z"))
                .willReturn(okJson(responseWithCursor))
        )
    }

    @Test
    fun `getClips single page`() {
        val clips = runBlocking { client.getClips(broadcasterId).toList() }

        assertThat(clips)
            .singleElement()
            .usingRecursiveComparison()
            .ignoringFields()
            .isEqualTo(clipInformation)
    }

    @Test
    fun `getClips start end date must be present`() {
        val startedAt = Instant.parse("2023-01-28T00:00:00Z")
        val endedAt = Instant.parse("2023-01-31T18:00:00Z")

        runBlocking { client.getClips(broadcasterId, startedAt, endedAt).collect() }

        verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlPathEqualTo("/helix/clips"))
                .withQueryParam("started_at", equalTo(startedAt.toString()))
                .withQueryParam("ended_at", equalTo(endedAt.toString()))
        )
    }

    @Test
    fun `getClips multi page response`() {
        val startedAt = Instant.parse("2022-12-20T00:00:00Z")
        val clips = runBlocking {
            client.getClips(broadcasterId, startedAt).toList()
        }

        assertThat(clips).hasSize(2)

        verify(
            2,
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlPathEqualTo("/helix/clips"))
                .withQueryParam("started_at", equalTo(startedAt.toString()))
        )
    }

    @Test
    fun `getClips limit request`() {
        val startedAt = Instant.parse("2022-05-01T00:00:00Z")
        val clips = runBlocking {
            client.getClips(broadcasterId, startedAt).take(10).toList()
        }

        assertThat(clips).hasSize(10)

        verify(
            10,
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlPathEqualTo("/helix/clips"))
                .withQueryParam("started_at", equalTo(startedAt.toString()))
        )
    }
}
