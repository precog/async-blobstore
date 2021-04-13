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
import quasar.blobstore.paths.{BlobstorePath, PrefixPath}

import scala._
import scala.Predef._
import scala.collection.immutable.Map
import scala.util.{Left, Right}

import argonaut._, Argonaut._

import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits._
import cats.kernel.{Eq, Hash}
import fs2.Stream

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

  def apply[F[_]: Sync](
      log: Logger,
      client: Client[F],
      bucket: Bucket): ListService[F] = Kleisli { prefixPath =>

    singleReq(log, client, bucket, None, prefixPath)
  }

  def singleReq[F[_]: Sync](
      log: Logger,
      client: Client[F],
      bucket: Bucket,
      pageToken: Option[String],
      prefixPath: PrefixPath): F[Option[Stream[F, BlobstorePath]]] = {

    val listUrl = GoogleCloudStorage.gcsListUrl(bucket)
    val prefix = converters.prefixPathToQueryParamValue(prefixPath)
    val queryParams = Map("prefix" -> prefix) ++ pageToken.fold(Map[String, String]())(t => Map("pageToken" -> t))
    val req = Request[F](Method.GET, listUrl.withQueryParams(queryParams))

    for {
      gcsListings <- client.run(req).use { resp =>
        resp.status match {
          case Status.Ok => resp.as[GCSListings].map(_.asRight[GCSAccessError])
          case Status.Forbidden => resp.as[GCSAccessError].map(_.asLeft[GCSListings])
          case _ => resp.as[GCSAccessError].map(_.asLeft[GCSListings])
        }
      }

    } yield {
      gcsListings match {
        case Left(value) => Some(Stream.raiseError[F](value))
        case Right(value) => Some(converters.gcsListingsToBlobstorePaths(value, _ == converters.gcsFileToBlobstorePath(GCSFile(prefix))))
      }
    }
  }

  def mk[F[_]: Sync](
      log: Logger,
      client: Client[F],
      bucket: Bucket)
      : ListService[F] =
    GCSListService[F](log, client, bucket)

}

final case class PageToken(value: String)
final case class GCSFile(name: String)
final case class GCSListings(list: List[GCSFile], pageToken: Option[PageToken])

object GCSListings {

  implicit val eqGCSFile: Eq[GCSFile] = Eq.by(_.name)
  implicit val hashGCSFile: Hash[GCSFile] = Hash.by(_.name)

  implicit def gcsListingsEntityDecoder[F[_]: Sync]: EntityDecoder[F, GCSListings] = jsonOf[F, GCSListings]
  implicit def gcsListingsEntityEncoder[F[_]: Sync]: EntityEncoder[F, GCSListings] = jsonEncoderOf[F, GCSListings]

  implicit val codecGCSFile: CodecJson[GCSFile] =
    casecodec1(GCSFile.apply, GCSFile.unapply)("name")

  implicit val codecJsonGCSListings: CodecJson[GCSListings] =
    CodecJson(
      {(gcsl: GCSListings) => {
        ("list" := gcsl.list) ->: jEmptyObject
      }
      },{j => {
        val items = (j --\ "items").either
        val prefixes = (j --\ "prefixes").either
        (items, prefixes) match {
          case (Left(_), Left(_)) => DecodeResult.ok(GCSListings(List.empty[GCSFile], None))
          case (Left(_), Right(p)) => for {
            list <- p.as[List[String]]
          } yield GCSListings(list.map(GCSFile(_)), None)
          case (Right(i), Right(p)) => for {
            is <- i.as[List[GCSFile]]
            ps <- p.as[List[String]]
            s = (is ++ ps.map(GCSFile(_))).toSet
          } yield GCSListings(s.toList, None)
          case (Right(i), Left(_)) => for {
            list <- i.as[List[GCSFile]]
          } yield GCSListings(list, None)
        }
      }})
}
