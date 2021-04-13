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

import scala.{Option, List}
import scala.Predef.println

import cats.effect.{Sync}
import cats.implicits._

import quasar.blobstore.paths.{BlobstorePath, PrefixPath}

import fs2.Stream

import org.http4s.{Method, Request, Status}
import org.http4s.client.Client
import scala.util.Left
import scala.util.Right

object children {

  def apply[F[_]: Sync: cats.Applicative](
      client: Client[F],
      bucket: Bucket)
      : F[Option[Stream[F, BlobstorePath]]] = { 

    val listUrl = GoogleCloudStorage.gcsListUrl(bucket)
    val prefix = converters.prefixPathToQueryParamValue(PrefixPath(List()))
    val req = Request[F](Method.GET, listUrl.withQueryParam("prefix", prefix))

    for {
      response <- client.run(req).use {resp =>
        resp.status match {
          case Status.Ok => resp.as[GCSListings].map(_.asRight[GCSAccessError])
          case _ => resp.as[GCSAccessError].map(_.asLeft[GCSListings])
        }
      }
    } yield {
      response match {
        case Left(value) => scala.Predef.???
        // expecting to see response printed to screen
        case Right(value) => println("response: " + value)
      }
    }

  def x: F[Option[Stream[F, BlobstorePath]]] = Stream[F, BlobstorePath](PrefixPath(List())).asRight.toOption.pure[F]
  x

  }

}