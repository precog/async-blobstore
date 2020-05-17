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

import quasar.blobstore.BlobstoreStatus
import quasar.blobstore.paths.BlobPath
import quasar.blobstore.services.DeleteService

import cats.data.Kleisli
import cats.effect.{Async, ContextShift}
import cats.syntax.functor._
import com.azure.storage.blob.BlobContainerAsyncClient

object AzureDeleteService {

  def mk[F[_]: Async: ContextShift](containerClient: BlobContainerAsyncClient)
      : DeleteService[F] = {

    val res = for {
      blobPath <- Kleisli.ask[F, BlobPath]
      blobClient <- Kleisli.liftF(converters.mkBlobClient(containerClient)(blobPath))
      status <- Kleisli.liftF(reactive.monoToAsync(blobClient.delete()).map(_ => BlobstoreStatus.ok()))
    } yield status

    handlers.recoverToBlobstoreStatus(res)
  }
}
