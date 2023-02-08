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

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec
import org.springframework.stereotype.Component
import ru.razornd.twitch.clips.model.ClipInformation

private const val INSERT_STRING =
    """
INSERT INTO clips(id,
                  broadcaster_id,
                  creator_id,
                  video_id,
                  game_id,
                  title,
                  view_count,
                  created_at,
                  duration,
                  vod_offset)
VALUES (:id,
        :broadcasterId,
        :creatorId,
        :videoId,
        :gameId,
        :title,
        :viewCount,
        :createdAt,
        :duration,
        :vodOffset)
ON CONFLICT (id) DO UPDATE
    SET video_id=:videoId,
        game_id=:gameId,
        view_count=:viewCount,
        vod_offset=:vodOffset
"""

@Component
class ClipInformationStore(private val databaseClient: DatabaseClient) {
    suspend fun store(clipInformation: ClipInformation) {
        databaseClient.sql(INSERT_STRING)
            .bind("id", clipInformation.id)
            .bind("broadcasterId", clipInformation.broadcasterId)
            .bind("creatorId", clipInformation.creatorId)
            .bind("videoId", clipInformation.videoId)
            .bind("gameId", clipInformation.gameId)
            .bind("title", clipInformation.title)
            .bind("viewCount", clipInformation.viewCount)
            .bind("createdAt", clipInformation.createdAt)
            .bind("duration", clipInformation.duration)
            .bind("vodOffset", clipInformation.vodOffset)
            .then()
            .awaitSingleOrNull()
    }
}

private inline fun <reified T> GenericExecuteSpec.bind(name: String, value: T?) =
    if (value != null) bind(name, value) else bindNull(name, T::class.java)
