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

import scala.Predef.println

import quasar.blobstore.paths.BlobstorePath
import quasar.blobstore.paths.BlobPath
//import quasar.blobstore.paths.Path
import quasar.blobstore.paths.PathElem
import quasar.blobstore.paths.PrefixPath

import scala.{Option, Some, List}
import scala.Predef.{String}

import argonaut._, Argonaut._

import quasar.blobstore.services.ListService
//import quasar.blobstore.paths.PrefixPath

//import cats.Applicative
import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, Concurrent, ContextShift, Sync}
import cats.implicits._

import org.http4s.{
  AuthScheme,
  Credentials,
  EntityEncoder,
  Method,
  Request
}
import org.http4s.client.Client
import org.http4s.headers.{Authorization}
import org.http4s.argonaut._
import org.http4s.EntityDecoder

import fs2.Stream

object GCSListService {

  def apply[F[_]: Concurrent: ContextShift ](
    client: Client[F],
    bucket: Bucket,
    config: GoogleAuthConfig): ListService[F] = Kleisli { prefixPath => {

      import GCSListings.gcsListingsErrorEntityDecoder
      //import GCSListings.gcsListingsErrorEntityEncoder

      for {
        accessToken <- GoogleCloudStorage.getAccessToken(config.serviceAccountAuthBytes)
        _ = println("accessToken: " + accessToken)
        bearerToken = Authorization(Credentials.Token(AuthScheme.Bearer, accessToken.getTokenValue))
        _ = println("bearerToken: " + bearerToken)
        listUrl <- GoogleCloudStorage.gcsListUrl(bucket).pure[F]
        _ = println("listUrl: " + listUrl)
        destReq = Request[F](Method.GET, listUrl).withHeaders(bearerToken)
        _ = println("destReq: " + destReq)
        x <- client.run(destReq).use {r => r.pure[F] }
        // x <- client.run(destReq).use[F, Unit] { resp =>
        //    resp.status match {
        //      case Status.Ok => {println("resp status ok: " + resp.status); ().pure[F]} //resp.as[GCSListings]
        //      case Status.Forbidden => println("resp forbidden: " + resp.status).pure[F]
        //      case e => println("resp error: " + e.reason).pure[F]
        //    }
        // }
        _ = println("STATUS: " + x.status)
      //} yield Some(Stream[F, BlobstorePath](x.list))
      } yield Some( Stream[F, BlobstorePath](BlobPath(List(PathElem("stuff")))) )
      //Kleisli(PrefixPath => f)
    }
  }

  def mk[F[_]: ConcurrentEffect: ContextShift](
      client: Client[F], 
      bucket: Bucket, 
      config: GoogleAuthConfig)
    : ListService[F] =
    GCSListService[F](client, bucket, config)

}

final case class GCSFile(name: String)
final case class GCSListings(list: BlobstorePath)

object GCSListings {
    implicit def gcsListingsErrorEntityDecoder[F[_]: Sync] : EntityDecoder[F, GCSListings] = jsonOf[F, GCSListings]
  implicit def gcsListingsErrorEntityEncoder[F[_]: Sync]: EntityEncoder[F, GCSListings] = jsonEncoderOf[F, GCSListings]

  implicit val codecPathElem: CodecJson[PathElem] = CodecJson.derive[PathElem]

  implicit val codecJsonGCSListings: CodecJson[GCSListings] = CodecJson(
    {(gcsl: GCSListings) => {
      ("list" := gcsl.list.path.map(x => x)) ->: jEmptyObject}
    },{j => {
      for {
        list <- (j --\ "items").as[List[PathElem]]
      } yield GCSListings(BlobPath(list))
    }})
}