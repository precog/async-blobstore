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
import quasar.blobstore.paths.{BlobPath, PathElem}
import quasar.blobstore.services.PropsService

import scala.Option
import scala.collection.immutable.{List, Nil}

import cats.effect._
import com.azure.storage.blob.models.BlobProperties
import org.specs2.matcher.Matcher
import org.specs2.matcher.MatchResult

class AzurePropsServiceSpec extends EffectfulSpec {

  def mkService(cfg: Config): IO[PropsService[IO, BlobProperties]] =
    Azure.mkContainerClient[IO](cfg) map (c => AzurePropsService.mk[IO](c.value))

  def assertProps[A](service: IO[PropsService[IO, A]], blobPath: BlobPath, matcher: Matcher[Option[A]])
      : IO[MatchResult[Option[A]]] =
    service flatMap { svc =>
      svc.apply(blobPath).map(_ must matcher)
    }

  "props service" >> {

    "existing blobpath returns some" >>*
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(List(PathElem("testdata"), PathElem("lines.json"))),
        beSome)

    "non existing blobpath returns none" >>*
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(List(PathElem("doesnotexist"))),
        beNone)

    "nil blobpath returns none" >>*
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(Nil),
        beNone)

    "empty string blobpath returns none" >>*
      assertProps[BlobProperties](
        mkService(PublicConfig),
        BlobPath(List(PathElem(""))),
        beNone)

    "blobpath in non existing container returns none" >>*
      assertProps[BlobProperties](
        mkService(NonExistingConfig),
        BlobPath(List(PathElem("something"))),
        beNone)

    "invalid container throws exception" >>*
      assertProps[BlobProperties](
        mkService(InvalidConfig),
        BlobPath(List(PathElem("something"))),
        beNone)
  }
}
