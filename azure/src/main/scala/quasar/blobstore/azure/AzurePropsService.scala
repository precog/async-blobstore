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

import quasar.blobstore.azure.requests.BlobPropsArgs
import quasar.blobstore.services.PropsService

import cats.data.Kleisli
import cats.effect.{Async, ContextShift}
import cats.syntax.applicative._
import com.microsoft.azure.storage.blob.{BlobAccessConditions, BlobURL, ContainerURL}
import com.microsoft.azure.storage.blob.models.BlobGetPropertiesResponse
import com.microsoft.rest.v2.Context

object AzurePropsService {
  def apply[F[_]: Async: ContextShift](
      containerURL: ContainerURL,
      mkArgs: BlobURL => BlobPropsArgs)
      : PropsService[F, BlobGetPropertiesResponse] =
    converters.blobPathToBlobURLK(containerURL) andThen
      Kleisli[F, BlobURL, BlobPropsArgs](mkArgs(_).pure[F]) andThen
      requests.blobPropsRequestK mapF
      handlers.recoverNotFound[F, BlobGetPropertiesResponse]

  def mk[F[_]: Async: ContextShift](containerURL: ContainerURL)
      : PropsService[F, BlobGetPropertiesResponse] =
    AzurePropsService[F](
      containerURL,
      BlobPropsArgs(_, BlobAccessConditions.NONE, Context.NONE))
}
