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

import quasar.blobstore.azure.requests.ListBlobHierarchyArgs
import quasar.blobstore.paths.PrefixPath
import quasar.blobstore.services.ListService

import java.lang.Integer

import scala.{None, Some}

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift}
import com.azure.storage.blob.BlobContainerAsyncClient
import com.azure.storage.blob.models.ListBlobsOptions

object AzureListService {

  def apply[F[_]: ConcurrentEffect: ContextShift](
      toListBlobsOption: Kleisli[F, PrefixPath, ListBlobsOptions],
      mkArgs: ListBlobsOptions => ListBlobHierarchyArgs)
      : ListService[F] =
    toListBlobsOption map
      mkArgs andThen
      requests.listRequestK andThen
      converters.toBlobstorePathsK


  def mk[F[_]: ConcurrentEffect: ContextShift](containerClient: BlobContainerAsyncClient): ListService[F] =
    AzureListService[F](
      converters.prefixPathToListBlobOptionsK(details = None, maxResults = Some(Integer.valueOf(5000))),
      ListBlobHierarchyArgs(containerClient, "/", _))
}
