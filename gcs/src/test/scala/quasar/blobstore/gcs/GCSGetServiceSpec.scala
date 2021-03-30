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

import scala.{Array, Byte, Some, StringContext, None}

import quasar.blobstore.paths.BlobPath
import quasar.blobstore.services.GetService

import cats.effect.{IO, Resource}
import cats.effect.testing.specs2.CatsIO
import cats.syntax.applicative._


import org.specs2.matcher.{Matcher, MatchResult}
import org.specs2.mutable.Specification


class GCSGetServiceSpec extends Specification with CatsIO {

  def mkListService(cfg: GoogleAuthConfig, bucket: Bucket): Resource[IO, GetService[IO]] =
    GoogleCloudStorage.mkContainerClient[IO](cfg).map(client => GCSGetService.mk[IO](client, bucket, cfg))

  def assertGet(
      service: IO[GetService[IO]],
      blobPath: BlobPath,
      matcher: Matcher[Array[Byte]])
      : IO[MatchResult[Array[Byte]]] =
    service flatMap { svc =>
      svc(blobPath).flatMap {
        case Some(s) => s.compile.to(Array).map(_ must matcher)
        case None => ko("Unexpected None").asInstanceOf[MatchResult[Array[Byte]]].pure[IO]
      }
    }

  def assertGetNone(
      service: IO[GetService[IO]],
      blobPath: BlobPath) =
    service flatMap { svc =>
      svc(blobPath).map {
        case Some(s) => ko(s"Unexpected Some: $s")
        case None => ok
      }
    }

    // "get service" >> {

    //   "existing blobpath returns expected bytes" >> {
    //     val expected = "[1, 2]\n[3, 4]\n".getBytes(StandardCharsets.UTF_8)

    //     assertGet(
    //         mkService(PublicConfig),
    //         BlobPath(List(PathElem("testdata"), PathElem("lines.json"))),
    //         be_===(expected))
    //     }

    //   "non-existing blobpath returns none" >> {
    //     assertGetNone(
    //       mkService(PublicConfig),
    //       BlobPath(List(PathElem("testdata"), PathElem("notthere"))))
    //     }

    // }

}
