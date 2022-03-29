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

import quasar.blobstore.azure.requests.BlobPropsArgs
import quasar.blobstore.services.PropsService

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift}
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.azure.storage.blob.{BlobAsyncClient, BlobContainerAsyncClient}
import com.azure.storage.blob.models.BlobProperties
import scala.Option


object AzurePropsService {

  def fromBlobPropsArgs[F[_]: ConcurrentEffect: ContextShift]
      : Kleisli[F, BlobPropsArgs, Option[BlobProperties]] =
    handlers.recoverToNone(
      requests.blobPropsRequestK.map(_.getValue()))

  def apply[F[_]: ConcurrentEffect: ContextShift](
      containerClient: BlobContainerAsyncClient,
      mkArgs: BlobAsyncClient => BlobPropsArgs)
      : PropsService[F, BlobProperties] =
    Kleisli { blobPath =>
      for {
        blobClient <- converters.mkBlobClient[F](containerClient)(blobPath)
        res <- fromBlobPropsArgs[F].run(BlobPropsArgs(blobClient, null))
      } yield res
    }

  def mk[F[_]: ConcurrentEffect: ContextShift](containerClient: BlobContainerAsyncClient)
      : PropsService[F, BlobProperties] =
    AzurePropsService[F](
      containerClient,
      BlobPropsArgs(_, null))
}
