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

import cats.data.{EitherT, Kleisli}
import cats.effect.Sync
import cats.implicits._
import cats.kernel.{Eq, Hash}
import fs2.Stream

import org.http4s.{
  EntityDecoder,
  EntityEncoder,
  Method,
  Request
}
import org.http4s.argonaut._
import org.http4s.client.Client
import org.slf4s.Logger

object GCSListService {

  def apply[F[_]: Sync](
      log: Logger,
      client: Client[F],
      bucket: Bucket): ListService[F] = Kleisli { prefixPath =>

    val msg = "Unexpected failure when streaming a multi-page response for ListBuckets"

    val stream0 = EitherT(singleReq(log, client, bucket, None, prefixPath)).toOption map {results =>
      Stream.iterateEval(results) { tup => tup._2 match {
        case None => 
          EitherT(singleReq(log, client, bucket, None, prefixPath)).getOrElseF(Sync[F].raiseError(new Exception(msg)))
        case Some(value) => {
          EitherT(singleReq(log, client, bucket, value.some, prefixPath)).getOrElseF(Sync[F].raiseError(new Exception(msg)))
        }
      }}
    }
    stream0.map(_.takeThrough(_._2.isDefined).flatMap(_._1)).value
  }

  def singleReq[F[_]: Sync](
      log: Logger,
      client: Client[F],
      bucket: Bucket,
      pageToken: Option[PageToken],
      prefixPath: PrefixPath)
      : F[Either[GCSAccessError,(Stream[F,BlobstorePath], Option[PageToken])]] = {

    import GCSListings._

    val listUrl = GoogleCloudStorage.gcsListUrl(bucket)
    val prefix = converters.prefixPathToQueryParamValue(prefixPath)
    val queryParams = Map("prefix" -> prefix) ++ pageToken.fold(Map[String, String]())(t => Map("pageToken" -> t.value))
    val req = Request[F](Method.GET, listUrl.withQueryParams(queryParams))

    println("req: " + req)

    for {
      l <- client.expect[GCSListings](req).map(_.asRight).map(e => e match {
        case Left(_) => Left(GCSAccessError("error"))
        case Right(value) => {
          val t = value.pageToken
          Right((value, t))
        }
      })
    } yield l match {
      case Right(tup) => { tup._2 match {
        case Some(pt) => 
          Right((
          (converters.gcsListingsToBlobstorePaths(tup._1, _ == converters.gcsFileToBlobstorePath(GCSFile(prefix)))),
          pt.some))
        case None => 
          Right((
            (converters.gcsListingsToBlobstorePaths(tup._1, _ == converters.gcsFileToBlobstorePath(GCSFile(prefix)))),
            None: Option[PageToken] ))
      }}
      case Left(e) => Left(e)
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

  implicit val codecPageToken: CodecJson[PageToken] =
    casecodec1(PageToken.apply, PageToken.unapply)("value")

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
        val token = (j --\ "nextPageToken").either

        //TODO: there's probably a better way to do this
        (items, prefixes, token) match {
          case (Left(_), Left(_), _) => DecodeResult.ok(GCSListings(List.empty[GCSFile], None))

          case (Left(_), Right(p), Left(_)) => for {
            list <- p.as[List[String]]
          } yield GCSListings(list.map(GCSFile(_)), None)

          case (Left(_), Right(p), Right(t)) => for {
            list <- p.as[List[String]]
            pageToken <- t.as[String]
          } yield GCSListings(list.map(GCSFile(_)), PageToken(pageToken).some)
        
          case (Right(i), Left(_), Right(t)) => for {
            list <- i.as[List[GCSFile]]
            pageToken <- t.as[String]
          } yield GCSListings(list, PageToken(pageToken).some)

          case (Right(i), Right(p), Right(t)) => for {
            is <- i.as[List[GCSFile]]
            ps <- p.as[List[String]]
            pageToken <- t.as[String]
            s = (is ++ ps.map(GCSFile(_))).toSet
          } yield GCSListings(s.toList, PageToken(pageToken).some)

          case (Right(i), Right(p), Left(_)) => for {
            is <- i.as[List[GCSFile]]
            ps <- p.as[List[String]]
            s = (is ++ ps.map(GCSFile(_))).toSet
          } yield GCSListings(s.toList, None)

          case (Right(i), Left(_), Left(_)) => for {
            list <- i.as[List[GCSFile]]
          } yield GCSListings(list, None)
        }
      }})
}
