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
import scala.Predef._

import argonaut._, Argonaut._

import quasar.blobstore.BlobstoreStatus
import quasar.blobstore.services.StatusService

import cats.effect.{Concurrent, ContextShift, Sync}
import cats.implicits._

import org.http4s.{
  EntityEncoder,
  Method,
  Request,
  Status
}
import org.http4s.client.Client

import org.http4s.argonaut._
import org.http4s.client._
import org.http4s.EntityDecoder

import quasar.blobstore.services.StatusService

object GCSStatusService {

  def apply[F[_]: Concurrent: ContextShift](
      client: Client[F],
      bucket: Bucket,
      config: GoogleAuthConfig): StatusService[F] = {

    import StatusResponseError.statusResponseErrorEntityDecoder

    val statusUrl = GoogleCloudStorage.gcsStatusUrl(bucket)
    val req = Request[F](Method.GET, statusUrl)
    for {
      bucketStatus <- client.run(req).use[F, BlobstoreStatus] { resp =>
        resp.status match {
          case Status.Ok => BlobstoreStatus.ok().pure[F]
          case Status.Forbidden => BlobstoreStatus.noAccess().pure[F]
          case e => BlobstoreStatus.notOk(s"Error: ${e.reason}").pure[F]
        }
      }
    } yield bucketStatus
  }
}

final case class StatusResponse(kind: String, resourceId: String, version: Int, etag: String)
final case class StatusResponseError(code: Int, message: String)

object StatusResponse {
  implicit def statusResponseEntityDecoder[F[_]: Sync] : EntityDecoder[F, StatusResponse] = jsonOf[F, StatusResponse]
  implicit def statusResponseEntityEncoder[F[_]: Sync]: EntityEncoder[F, StatusResponse] = jsonEncoderOf[F, StatusResponse]
  implicit val loginCodecJson: CodecJson[StatusResponse] = CodecJson(
    {sr => Json.obj(
      "kind" := sr.kind,
      "resourceId" := sr.resourceId,
      "version" := sr.version,
      "etag" := sr.etag)
    }, {j => {
      for {
        kind <- (j --\ "kind").as[String]
        resourceId <- (j --\ "resourceId").as[String]
        version <- (j --\ "version").as[Int]
        etag <- (j --\ "etag").as[String]
      } yield StatusResponse(kind, resourceId, version, etag)
    }})
}

object StatusResponseError {
  implicit def statusResponseErrorEntityDecoder[F[_]: Sync] : EntityDecoder[F, StatusResponseError] = jsonOf[F, StatusResponseError]
  implicit def statusResponseErrorEntityEncoder[F[_]: Sync]: EntityEncoder[F, StatusResponseError] = jsonEncoderOf[F, StatusResponseError]
  implicit val statusResonseError: CodecJson[StatusResponseError] = CodecJson(
    {sre => Json.obj(
      "code" := sre.code,
      "message" := sre.message)
    }, {j => {
      for {
        code <- (j --\ "code").as[Int]
        message <- (j --\ "message").as[String]
      } yield StatusResponseError(code, message)
    }}

  )
}
