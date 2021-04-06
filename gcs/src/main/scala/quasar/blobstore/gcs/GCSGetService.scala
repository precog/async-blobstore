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

import quasar.blobstore.services.GetServiceResource

import java.lang.RuntimeException
import scala.{Byte, None, Some, StringContext}
import scala.Predef.String

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import fs2.Stream
import org.http4s.{Method, Request, Status}
import org.http4s.client.Client
import org.slf4s.Logger

object GCSGetService {

  case class GetError(message: String) extends RuntimeException(message)

  def mk[F[_]: ConcurrentEffect: ContextShift](
      log: Logger,
      client: Client[F],
      bucket: Bucket)
      : GetServiceResource[F] = Kleisli { blobPath =>

    val filepath = converters.blobPathToString(blobPath)
    val downloadUrl = GoogleCloudStorage.gcsDownloadUrl(bucket, filepath)
    val req = Request[F](Method.GET, downloadUrl)

    for {
      props <- Resource.liftF(GCSPropsService[F](log, client, bucket).apply(blobPath))
      res <-
        props match {
          case None =>
            none[Stream[F, Byte]].pure[Resource[F, *]]
          case Some(_) =>
            val r = client.run(req).evalMap[F, Stream[F, Byte]] { resp =>
              resp.status match {
                case Status.Ok => resp.body.pure[F]
                case s => ConcurrentEffect[F].raiseError(GetError(s"Unable to get $filepath. Got status $s"))
              }
            }
            r.map(Some(_))
        }
    } yield res
  }
}
