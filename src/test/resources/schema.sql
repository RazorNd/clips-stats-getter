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

CREATE TABLE clips
(
    id             VARCHAR(255)  NOT NULL UNIQUE PRIMARY KEY,
    broadcaster_id BIGINT        NOT NULL,
    creator_id     BIGINT        NOT NULL,
    video_id       BIGINT,
    game_id        BIGINT,
    title          VARCHAR(2048) NOT NULL,
    view_count     INTEGER       NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    duration       FLOAT         NOT NULL,
    vod_offset     INTEGER
);
