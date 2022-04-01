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

import quasar.blobstore.azure.requests.ContainerPropsArgs
import quasar.blobstore.services.StatusService

import cats.effect.{ConcurrentEffect, ContextShift}
import com.azure.storage.blob.BlobContainerAsyncClient
import com.azure.storage.blob.models.BlobContainerProperties

object AzureStatusService {
  def apply[F[_]: ConcurrentEffect: ContextShift](args: ContainerPropsArgs): StatusService[F] =
    (requests.containerPropsRequestK[F] andThen
      converters.responseToBlobstoreStatusK[F, BlobContainerProperties] mapF
      handlers.recoverToBlobstoreStatus[F]
    ).apply(args)

  def mk[F[_]: ConcurrentEffect: ContextShift](containerClient: BlobContainerAsyncClient): StatusService[F] =
    AzureStatusService[F](ContainerPropsArgs(containerClient, null))
}
