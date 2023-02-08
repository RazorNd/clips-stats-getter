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

package ru.razornd.twitch.clips.store

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.r2dbc.core.DatabaseClient
import org.testcontainers.junit.jupiter.Testcontainers
import ru.razornd.twitch.clips.model.ClipInformation
import java.time.Instant
import java.time.LocalDateTime

@Testcontainers(disabledWithoutDocker = true)
@DataR2dbcTest(
    properties = [
        "spring.r2dbc.url=r2dbc:tc:postgresql:///twitch?TC_IMAGE_TAG=14-alpine&timeZone=UTC",
        "spring.sql.init.mode=always"
    ]
)
class ClipInformationStoreTest {

    @Autowired
    lateinit var databaseClient: DatabaseClient

    private lateinit var store: ClipInformationStore

    @BeforeEach
    fun setUp() {
        store = ClipInformationStore(databaseClient)
    }

    @Test
    fun store() {
        val information = ClipInformation(
            "RandomClip1",
            1234,
            123456,
            6137,
            33103,
            "random1",
            10,
            Instant.parse("2017-11-30T22:34:18Z"),
            12.9,
            1957
        )

        val clip = runBlocking {
            store.store(information)

            fetchClip(information.id)
        }


        assertThat(clip)
            .describedAs("Stored data")
            .containsExactly(
                entry("id", "RandomClip1"),
                entry("broadcaster_id", 1234L),
                entry("creator_id", 123456L),
                entry("video_id", 6137L),
                entry("game_id", 33103L),
                entry("title", "random1"),
                entry("view_count", 10),
                entry("created_at", LocalDateTime.parse("2017-11-30T22:34:18")),
                entry("duration", 12.9),
                entry("vod_offset", 1957)
            )
    }

    @Test
    fun `store nullable`() {
        val information = ClipInformation(
            "5RUd3mpcosQvuTTRM",
            392,
            239,
            null,
            null,
            "Your brochure scenarios",
            218,
            Instant.parse("2005-04-20T00:04:12Z"),
            871343.1182,
            null
        )

        val clip = runBlocking {
            store.store(information)

            fetchClip(information.id)
        }

        assertThat(clip)
            .describedAs("Stored data")
            .containsExactly(
                entry("id", "5RUd3mpcosQvuTTRM"),
                entry("broadcaster_id", 392L),
                entry("creator_id", 239L),
                entry("video_id", null),
                entry("game_id", null),
                entry("title", "Your brochure scenarios"),
                entry("view_count", 218),
                entry("created_at", LocalDateTime.parse("2005-04-20T00:04:12")),
                entry("duration", 871343.1182),
                entry("vod_offset", null)
            )
    }

    @Test
    fun `store update`() {
        insertNullableClip()

        val information = ClipInformation(
            "YaqiRsjKQztbQr",
            175967L,
            6452L,
            3427L,
            9740469L,
            "handmade",
            50,
            Instant.parse("2022-08-12T09:37:05Z"),
            711.0,
            6
        )

        val clip = runBlocking {
            store.store(information)

            fetchClip(information.id)
        }

        assertThat(clip)
            .describedAs("Stored data")
            .containsExactly(
                entry("id", "YaqiRsjKQztbQr"),
                entry("broadcaster_id", 175967L),
                entry("creator_id", 6452L),
                entry("video_id", 3427L),
                entry("game_id", 9740469L),
                entry("title", "handmade"),
                entry("view_count", 50),
                entry("created_at", LocalDateTime.parse("2022-08-12T09:37:05")),
                entry("duration", 711.0),
                entry("vod_offset", 6)
            )
    }

    private fun insertNullableClip() {
        databaseClient.sql("INSERT INTO clips VALUES ('YaqiRsjKQztbQr', 175967, 6452, NULL, NULL, 'handmade', 50, '2022-08-12T09:37:05'::TIMESTAMP, 711, NULL)")
            .then().block()
    }

    private suspend fun fetchClip(id: String): MutableMap<String, Any>? =
        databaseClient.sql("SELECT * FROM clips WHERE id=:id")
            .bind("id", id)
            .fetch()
            .all()
            .awaitSingle()
}
