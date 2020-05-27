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

package quasar.blobstore.azure

import quasar.blobstore.azure.requests.{BlobPropsArgs, DownloadArgs}
import quasar.blobstore.services.GetService

import scala.{Byte, None, Some}

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift}
import cats.implicits._
import com.azure.storage.blob.BlobContainerAsyncClient
import fs2.Stream

object AzureGetService {

  def mk[F[_]: ConcurrentEffect: ContextShift](
      containerClient: BlobContainerAsyncClient)
      : GetService[F] =
    Kleisli { blobPath =>
      for {
        blobClient <- converters.mkBlobClient[F](containerClient)(blobPath)
        props <- AzurePropsService.fromBlobPropsArgs[F].apply(BlobPropsArgs(blobClient, null))
        res <-
          props match {
            case None =>
              none.pure[F]
            case Some(_) =>
              (requests.downloadRequestK[F] mapF
                (handlers.recoverToNone[F, Stream[F, Byte]] _)
              ).apply(DownloadArgs(blobClient))
          }
      } yield res
    }

}
