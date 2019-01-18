/*
 * Copyright 2014–2018 SlamData Inc.
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

package quasar.blobstore.azure

import slamdata.Predef._
import quasar.blobstore.azure.requests.DownloadArgs
import quasar.blobstore.services.GetService

import cats.data.Kleisli
import cats.effect.ConcurrentEffect
import cats.syntax.applicative._
import com.microsoft.azure.storage.blob._
import com.microsoft.rest.v2.Context
import fs2.Stream

object AzureGetService {

  def apply[F[_]: ConcurrentEffect](
      containerURL: ContainerURL,
      mkArgs: BlobURL => DownloadArgs,
      reliableDownloadOptions: ReliableDownloadOptions,
      maxQueueSize: MaxQueueSize)
      : GetService[F] =
    converters.blobPathToBlobURLK(containerURL) andThen
      Kleisli[F, BlobURL, DownloadArgs](mkArgs(_).pure[F]) andThen
      requests.downloadRequestK andThen
      handlers.toByteStreamK(reliableDownloadOptions, maxQueueSize) mapF
      handlers.recoverNotFound[F, Stream[F, Byte]]

  def mk[F[_]: ConcurrentEffect](containerURL: ContainerURL, maxQueueSize: MaxQueueSize): GetService[F] =
    AzureGetService(
      containerURL,
      DownloadArgs(_, BlobRange.DEFAULT, BlobAccessConditions.NONE, false, Context.NONE),
      new ReliableDownloadOptions,
      maxQueueSize)
}
