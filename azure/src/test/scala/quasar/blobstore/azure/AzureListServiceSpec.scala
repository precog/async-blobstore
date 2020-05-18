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
import quasar.blobstore.services.ListService

import scala.{None, Some, StringContext}

import cats.effect._
import cats.syntax.applicative._
import org.specs2.matcher.{Matcher, MatchResult}
import scala.collection.immutable.List

class AzureListServiceSpec extends EffectfulSpec {

  def mkService(cfg: Config): IO[ListService[IO]] =
    Azure.mkContainerClient[IO](cfg) map (c => AzureListService.mk[IO](c.value))

  def assertList(
      service: IO[ListService[IO]],
      prefixPath: PrefixPath,
      matcher: Matcher[List[BlobstorePath]])
      : IO[MatchResult[List[BlobstorePath]]] =
    service flatMap { svc =>
      svc(prefixPath).flatMap {
        case Some(s) => s.compile.toList.map(_ must matcher)
        case None => ko("Unexpected None").asInstanceOf[MatchResult[List[BlobstorePath]]].pure[IO]
      }
    }

  def assertNone(
      service: IO[ListService[IO]],
      prefixPath: PrefixPath) =
    service flatMap { svc =>
      svc(prefixPath).flatMap {
        case Some(s) => ko(s"Unexpected Some: $s").pure[IO]
        case None => ok.pure[IO]
      }
    }

  "list service" >> {

    "existing leaf prefix returns blobpaths" >>* {
      val expected = List[BlobstorePath](
        BlobPath(List(PathElem("prefix3"), PathElem("subprefix5"), PathElem("cars2.data"))))

      assertList(
        mkService(PublicConfig),
        PrefixPath(List(PathElem("prefix3"), PathElem("subprefix5"))),
        be_===(expected))
    }

    "existing non-leaf prefix returns prefixpaths and blobpaths" >>* {
      val expected = List[BlobstorePath](
        BlobPath(List(PathElem("dir1"), PathElem("arrayProcessing.data"))),
        PrefixPath(List(PathElem("dir1"), PathElem("dir1"))),
        PrefixPath(List(PathElem("dir1"), PathElem("dir2"))))

      assertList(
        mkService(PublicConfig),
        PrefixPath(List(PathElem("dir1"))),
        be_===(expected))
    }

    "non-existing prefix returns none" >>*
      assertNone(
        mkService(PublicConfig),
        PrefixPath(List(PathElem("does"), PathElem("not"), PathElem("exist"))))
  }
}
