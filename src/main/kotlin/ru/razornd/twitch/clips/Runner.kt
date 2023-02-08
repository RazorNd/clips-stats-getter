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

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.stereotype.Component
import ru.razornd.twitch.clips.store.ClipInformationStore
import ru.razornd.twitch.clips.twitch.TwitchClient
import java.time.Clock
import java.time.Instant
import java.time.Period

@ConfigurationProperties("fetch")
data class FetchConfiguration(val broadcasterId: Long, @DefaultValue("1w") val period: Period)

@Component
class Runner(
    private val client: TwitchClient,
    private val store: ClipInformationStore,
    private val configuration: FetchConfiguration
) {

    var clock: Clock = Clock.systemUTC()

    suspend fun run() {
        val endDate = Instant.now(clock)

        val clips = client.getClips(configuration.broadcasterId, endDate.minus(configuration.period), endDate)

        clips.collect { store.store(it) }
    }

}
