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

package quasar.blobstore.azure

import quasar.blobstore.azure.fixtures._
import quasar.blobstore.azure.testImplicits._
import quasar.blobstore.paths._
import quasar.blobstore.services.GetService

import java.nio.charset.StandardCharsets
import scala.{Array, Byte, None, Some, StringContext}
import scala.collection.immutable.List

import cats.effect._
import cats.syntax.applicative._
import org.specs2.matcher.{Matcher, MatchResult}

class AzureGetServiceSpec extends EffectfulSpec {
  import AzureGetServiceSpec._

  "get service" >> {

    "existing blobpath returns expected bytes" >>* {
      val expected = "[1, 2]\n[3, 4]\n".getBytes(StandardCharsets.UTF_8)

      assertGet(
        mkService(PublicConfig),
        BlobPath(List(PathElem("testdata"), PathElem("lines.json"))),
        be_===(expected))
    }

    "non-existing blobpath returns expected bytes" >>*
      assertGetNone(
        mkService(PublicConfig),
        BlobPath(List(PathElem("testdata"), PathElem("notthere"))))
  }
}

object AzureGetServiceSpec extends EffectfulSpec {

  def mkService(cfg: Config): IO[GetService[IO]] =
    Azure.mkContainerClient[IO](cfg) map (c => AzureGetService.mk[IO](c.value))

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
}
