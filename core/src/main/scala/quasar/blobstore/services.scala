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

package quasar.blobstore

import quasar.blobstore.paths.{BlobPath, BlobstorePath, PrefixPath}

import scala.{Byte, Option, Int}

import cats.data.Kleisli
import fs2.Stream

object services {

  type DeleteService[F[_]] = Kleisli[F, BlobPath, BlobstoreStatus]

  type GetService[F[_]] = Kleisli[F, BlobPath, Option[Stream[F, Byte]]]

  type ListService[F[_]] = Kleisli[F, PrefixPath, Option[Stream[F, BlobstorePath]]]

  type PropsService[F[_], P] = Kleisli[F, BlobPath, Option[P]]

  type PutService[F[_]] = Kleisli[F, (BlobPath, Stream[F, Byte]), Int]

  type StatusService[F[_]] = F[BlobstoreStatus]

}
