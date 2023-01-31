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

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriBuilder
import ru.razornd.twitch.clips.logger
import java.net.URI
import java.time.Instant


private val log = logger<TwitchClient>()

private const val PAGE_SIZE = 100

class TwitchClient(private val webClient: WebClient) {

    @RegisterReflectionForBinding(
        classes = [Response::class, Pagination::class, ClipInformation::class, SnakeCaseStrategy::class]
    )
    fun getClips(
        broadcasterId: String,
        startedAt: Instant? = null,
        endedAt: Instant? = null
    ): Flow<ClipInformation> = flow {
        var cursor: String? = null
        do {
            val (data, pagination) = webClient.get()
                .uri { builder ->
                    builder.path("/clips")
                        .queryParam("broadcaster_id", broadcasterId)
                        .queryParamIfNonNull("started_at", startedAt)
                        .queryParamIfNonNull("ended_at", endedAt)
                        .queryParamIfNonNull("after", cursor)
                        .queryParam("first", PAGE_SIZE)
                        .build().also { logRequestUri(it) }
                }
                .retrieve()
                .awaitBody<Response<ClipInformation>>()

            emitAll(data)

            cursor = pagination?.cursor
            logCursor(cursor)
        } while (cursor != null)
    }

    private suspend fun <T> FlowCollector<T>.emitAll(data: Collection<T>) = data.forEach { emit(it) }

    private fun logCursor(cursor: String?) = if (cursor != null) {
        log.debug("Response contains cursor to next page: {}", cursor)
    } else {
        log.debug("Response contains last page")
    }

    private fun logRequestUri(uri: URI) = log.debug("Make request to: {}", uri)

    private data class Response<T>(val data: Collection<T>, val pagination: Pagination?)

    private data class Pagination(val cursor: String?)

    @JsonNaming(SnakeCaseStrategy::class)
    data class ClipInformation(
        val id: String,
        val broadcasterId: String,
        val creatorId: String,
        val videoId: String,
        val gameId: String,
        val title: String,
        val viewCount: Int,
        val createdAt: Instant,
        val duration: Int,
        val vodOffset: Int
    )

    private fun UriBuilder.queryParamIfNonNull(name: String, value: Any?) =
        if (value != null) queryParam(name, value) else this
}
