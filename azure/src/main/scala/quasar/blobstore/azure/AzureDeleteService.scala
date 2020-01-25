/*
 * Copyright 2014–2019 SlamData Inc.
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

import scala.StringContext

import quasar.blobstore.BlobstoreStatus
import quasar.blobstore.paths.BlobPath
import quasar.blobstore.services.DeleteService

import com.microsoft.azure.storage.blob.ContainerURL

import cats.data.Kleisli
import cats.effect.{Async, ContextShift}

object AzureDeleteService {
  def apply[F[_]: Async: ContextShift](containerURL: ContainerURL)
      : DeleteService[F] =
    for {
      blobPath <- Kleisli.ask[F, BlobPath]
      blobUrl <- Kleisli.liftF(converters.mkBlobUrl(containerURL)(blobPath))
      response <- Kleisli.liftF(rx.singleToAsync(blobUrl.delete()))
    } yield response.statusCode match {
      case 202 => BlobstoreStatus.ok()
      case 403 => BlobstoreStatus.noAccess()
      case 404 => BlobstoreStatus.notFound()
      case other => BlobstoreStatus.notOk(s"Azure returned status $other")
    }

  def mk[F[_]: Async: ContextShift](containerURL: ContainerURL)
      : DeleteService[F] =
    apply[F](containerURL)
}
