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

package ru.razornd.twitch.clips

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.razornd.twitch.clips.model.ClipInformation
import ru.razornd.twitch.clips.store.ClipInformationStore
import ru.razornd.twitch.clips.twitch.TwitchClient
import java.time.Clock
import java.time.Instant
import java.time.Period
import java.time.ZoneOffset

class RunnerTest {

    private val now = Instant.parse("2023-02-08T13:00:00Z")

    private val client: TwitchClient = mockk()
    private val store: ClipInformationStore = mockk(relaxUnitFun = true)
    private val configuration = FetchConfiguration(listOf(3593082L, 790564L, 11772L), Period.ofDays(3))

    private val runner = Runner(client, store, configuration)

    init {
        runner.clock = Clock.fixed(now, ZoneOffset.UTC)
    }

    @Test
    fun run() {
        val clipInformation = createClipInformation()

        val broadcastersIds = mutableListOf<Long>()

        every {
            client.getClips(
                capture(broadcastersIds),
                eq(now.minus(configuration.period)),
                eq(now)
            )
        } returns listOf(clipInformation).asFlow()


        runBlocking {
            runner.run()
        }

        coVerify { store.store(clipInformation) }
        assertThat(broadcastersIds)
            .describedAs("Broadcaster IDs that passed to Twitch Client")
            .isEqualTo(configuration.broadcastersIds)
    }

    private fun createClipInformation() = ClipInformation(
        "Ym5ZxPNFWDAX7JUO2ruPPTJ4",
        175L,
        9043719L,
        657L,
        83606L,
        "convertible",
        296914,
        Instant.parse("2023-02-08T17:00:00Z"),
        90.0,
        9741750
    )
}
