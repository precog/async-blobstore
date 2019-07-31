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

import quasar.blobstore.BlobstoreStatus
import quasar.blobstore.azure.requests.ContainerPropsArgs
import quasar.blobstore.services.StatusService

import cats.data.Kleisli
import cats.effect.{Async, ContextShift}
import com.microsoft.azure.storage.blob.ContainerURL
import com.microsoft.azure.storage.blob.models.{ContainerGetPropertiesResponse, LeaseAccessConditions}
import com.microsoft.rest.v2.Context

object AzureStatusService {
  def apply[F[_]: Async: ContextShift](args: ContainerPropsArgs): StatusService[F] =
    (requests.containerPropsRequestK[F] andThen
      Kleisli.pure[F, ContainerGetPropertiesResponse, BlobstoreStatus](BlobstoreStatus.ok()) mapF
      handlers.recoverToBlobstoreStatus[F, BlobstoreStatus]).apply(args)

  def mk[F[_]: Async: ContextShift](containerURL: ContainerURL): StatusService[F] =
    AzureStatusService[F](ContainerPropsArgs(containerURL, new LeaseAccessConditions, Context.NONE))
}
