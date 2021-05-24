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

package quasar.blobstore.gcs

import scala._

import quasar.blobstore.BlobstoreStatus
import quasar.blobstore.services.StatusService

import cats.effect.Sync
import cats.implicits._

import org.http4s.{
  Method,
  Request,
  Status
}
import org.http4s.client.Client

import org.slf4s.Logger

object GCSStatusService {

  def apply[F[_]: Sync](
      log: Logger,
      client: Client[F],
      bucket: Bucket): StatusService[F] = {

    val statusUrl = GoogleCloudStorage.gcsStatusUrl(bucket)
    val req = Request[F](Method.GET, statusUrl)
    val res = for {
      bucketStatus <- client.run(req).use[F, BlobstoreStatus] { resp =>
        resp.status match {
          case Status.Ok => BlobstoreStatus.ok().pure[F]
          case Status.NotFound => BlobstoreStatus.notFound().pure[F]
          case Status.Forbidden => BlobstoreStatus.noAccess().pure[F]
          case e => BlobstoreStatus.notOk(s"Error: ${e.reason}").pure[F]
        }
      }
    } yield bucketStatus

    handlers.recoverToBlobstoreStatus(res)
  }
}
