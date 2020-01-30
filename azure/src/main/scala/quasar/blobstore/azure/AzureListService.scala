/*
 * Copyright 2014–2020 SlamData Inc.
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
import cats.effect.{Async, ContextShift}
import com.microsoft.azure.storage.blob.{ContainerURL, ListBlobsOptions}
import com.microsoft.rest.v2.Context

object AzureListService {
  def apply[F[_]: Async: ContextShift](
      toListBlobsOption: Kleisli[F, PrefixPath, ListBlobsOptions],
      mkArgs: ListBlobsOptions => ListBlobHierarchyArgs)
      : ListService[F] =
    toListBlobsOption map
      mkArgs andThen
      requests.listRequestK andThen
      converters.toBlobstorePathsK


  def mk[F[_]: Async: ContextShift](containerURL: ContainerURL): ListService[F] =
    AzureListService[F](
      converters.prefixPathToListBlobOptionsK(details = None, maxResults = Some(Integer.valueOf(5000))),
      ListBlobHierarchyArgs(containerURL, None, "/", _, Context.NONE))
}
