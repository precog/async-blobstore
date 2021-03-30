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

import quasar.blobstore.services.ListService

import scala.{Some, List}
import scala.Predef.{String}

import argonaut._, Argonaut._
import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, Concurrent, ContextShift, Sync}
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

object GCSListService {

  def apply[F[_]: Concurrent: ContextShift ](
      log: Logger,
      client: Client[F],
      bucket: Bucket): ListService[F] = Kleisli { prefixPath =>

    import GCSListings.gcsListingsErrorEntityDecoder
    //import GCSListings.gcsListingsErrorEntityEncoder

    val listUrl = GoogleCloudStorage.gcsListUrl(bucket)
    val req = Request[F](Method.GET, listUrl)

    for {
      gcsListings <- client.run(req).use { resp =>

        resp.status match {
          case Status.Ok => resp.as[GCSListings]
          case Status.Forbidden => scala.Predef.???
          case _ => scala.Predef.???
        }
      }

    } yield Some(converters.gcsListingsToBlobstorePaths(gcsListings))
  }

  def mk[F[_]: ConcurrentEffect: ContextShift](
      log: Logger,
      client: Client[F],
      bucket: Bucket)
      : ListService[F] =
    GCSListService[F](log, client, bucket)

}

final case class GCSFile(name: String)
final case class GCSListings(list: List[GCSFile])

object GCSListings {
  implicit def gcsListingsErrorEntityDecoder[F[_]: Sync]: EntityDecoder[F, GCSListings] = jsonOf[F, GCSListings]
  implicit def gcsListingsErrorEntityEncoder[F[_]: Sync]: EntityEncoder[F, GCSListings] = jsonEncoderOf[F, GCSListings]

  implicit val codecGCSFile: CodecJson[GCSFile] =
    casecodec1(GCSFile.apply, GCSFile.unapply)("name")

  implicit val codecJsonGCSListings: CodecJson[GCSListings] =
    CodecJson(
      {(gcsl: GCSListings) => {
        ("list" := gcsl) ->: jEmptyObject}
      },{j => {
        for {
          list <- (j --\ "items").as[List[GCSFile]]
        } yield GCSListings(list)
      }})
}
