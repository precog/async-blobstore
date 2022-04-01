/*
 * Copyright 2020 Precog Data
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
 */

package quasar.blobstore.gcs

import scala.concurrent.duration.Duration

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}

import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

import org.slf4s.Logging

object EmberHttpClientBuilder extends Logging {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer]: Resource[F, Client[F]] =
    EmberClientBuilder
      .default[F]
      .withMaxTotal(400)
      .withMaxPerKey(_ => 200) // the underlying pool is keyed by (scheme, host). i.e connection limit per host
      .withTimeout(Duration.Inf)
      .withMaxResponseHeaderSize(262144)
      .withIdleConnectionTime(Duration.Inf)
      .build
}
