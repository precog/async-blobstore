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

import quasar.blobstore.services.PropsService

import scala.{Int, Some}
import scala.Predef.String

import argonaut._, Argonaut._
import cats.data.Kleisli
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Sync}
import cats.implicits._

import org.http4s.{
  EntityDecoder,
  EntityEncoder,
  Method,
  Request,
  Status
}
import org.http4s.argonaut._
import org.http4s.client.Client
import org.slf4s.Logger

object GCSPropsService {

  import GCSFileProperties._

  def apply[F[_]: Concurrent: ContextShift](
      log: Logger,
      client: Client[F],
      bucket: Bucket): PropsService[F, GCSFileProperties] = Kleisli { blobPath =>

    val prefix = converters.blobPathToString(blobPath)
    val listUrl = GoogleCloudStorage.gcsPropsUrl(bucket, prefix)
    val req = Request[F](Method.GET, listUrl.withQueryParam("prefix", prefix))

    val resp: F[scala.Option[GCSFileProperties]] = client.run(req).use { resp =>
        resp.status match {
          case Status.Ok => resp.as[GCSFileProperties].map(Some(_))
          case _ => none[GCSFileProperties].pure[F]
        }
      }

    handlers.recoverToNone(resp)
  }

  def mk[F[_]: ConcurrentEffect: ContextShift](
      log: Logger,
      client: Client[F],
      bucket: Bucket)
      : PropsService[F, GCSFileProperties] =
    GCSPropsService[F](log, client, bucket)

}

object GCSFileProperties {
  final case class GCSFileProperties(name: String, bucket: String, size: Int, created: String)

  implicit def gcsAccessErrorEntityDecoder[F[_]: Sync]: EntityDecoder[F, GCSFileProperties] = jsonOf[F, GCSFileProperties]
  implicit def gcsAccessErrorEntityEncoder[F[_]: Sync]: EntityEncoder[F, GCSFileProperties] = jsonEncoderOf[F, GCSFileProperties]

  implicit val codecJsonGCSFileProperties: CodecJson[GCSFileProperties] =
    casecodec4(GCSFileProperties.apply, GCSFileProperties.unapply)("name", "bucket", "size", "timeCreated")
}
