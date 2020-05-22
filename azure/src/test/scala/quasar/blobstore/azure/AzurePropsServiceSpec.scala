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
import quasar.blobstore.paths.{BlobPath, PathElem}
import quasar.blobstore.services.PropsService

import scala.Option
import scala.collection.immutable.{List, Nil}

import cats.effect._
import cats.effect.testing.specs2.CatsIO
import com.azure.storage.blob.models.BlobProperties
import org.specs2.matcher.Matcher
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

class AzurePropsServiceSpec extends Specification with CatsIO {

  def mkService(cfg: Config): IO[PropsService[IO, BlobProperties]] =
    Azure.mkContainerClient[IO](cfg) map (c => AzurePropsService.mk[IO](c.value))

  def assertProps[A](service: IO[PropsService[IO, A]], blobPath: BlobPath, matcher: Matcher[Option[A]])
      : IO[MatchResult[Option[A]]] =
    service flatMap { svc =>
      svc.apply(blobPath).map(_ must matcher)
    }

  "props service" >> {

    "existing blobpath returns some" in IO {
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(List(PathElem("testdata"), PathElem("lines.json"))),
        beSome)
    }

    "existing blobpath returns some (2)" in IO {
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(List(PathElem("prefix3"), PathElem("subprefix5"), PathElem("cars2.data"))),
        beSome)
    }

    "non existing blobpath returns none" in IO {
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(List(PathElem("doesnotexist"))),
        beNone)
    }

    "blobpath that only exists as prefix returns none" in IO {
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(List(PathElem("testdata"))),
        beNone)
    }

    "nil blobpath returns none" in IO {
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(Nil),
        beNone)
    }

    "empty string blobpath returns none" in IO {
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(List(PathElem(""))),
        beNone)
    }

    "blobpath in non existing container returns none" in IO {
      assertProps[BlobProperties](
        mkService(NonExistingConfig),
        BlobPath(List(PathElem("something"))),
        beNone)
    }

    "invalid container throws exception" in IO {
      assertProps[BlobProperties](
        mkService(InvalidConfig),
        BlobPath(List(PathElem("something"))),
        beNone)
    }
  }
}
