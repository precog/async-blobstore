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

import scala.Unit
import scala.Predef.String

import monocle.Prism

sealed trait BlobstoreStatus

object BlobstoreStatus {

  final case class NotOk(msg: String) extends BlobstoreStatus
  case object NotFound extends BlobstoreStatus
  case object NoAccess extends BlobstoreStatus
  case object Ok extends BlobstoreStatus

  def notOk: Prism[BlobstoreStatus, String] =
    Prism.partial[BlobstoreStatus, String] {
      case NotOk(msg) => msg
    } (NotOk(_))

  def notFound: Prism[BlobstoreStatus, Unit] =
    Prism.partial[BlobstoreStatus, Unit] {
      case NotFound => ()
    } (_ => NotFound)

  def noAccess: Prism[BlobstoreStatus, Unit] =
    Prism.partial[BlobstoreStatus, Unit] {
      case NoAccess => ()
    } (_ => NoAccess)

  def ok: Prism[BlobstoreStatus, Unit] =
    Prism.partial[BlobstoreStatus, Unit] {
      case Ok => ()
    } (_ => Ok)
}
