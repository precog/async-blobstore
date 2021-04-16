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

import scala._
import scala.Predef._

import quasar.blobstore.services.ListService
import quasar.blobstore.paths.PrefixPath
import quasar.blobstore.paths.BlobstorePath
import quasar.blobstore.paths.BlobPath
import quasar.blobstore.paths.PathElem

import argonaut._, Argonaut._

import cats.effect.{IO, Resource}
import cats.implicits._
import cats.effect.testing.specs2.CatsIO

import org.slf4s.Logger
import org.slf4s.LoggerFactory
import org.specs2.matcher.{Matcher, MatchResult}
import org.specs2.mutable.Specification

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets.UTF_8

class GCSListServiceSpec extends Specification with CatsIO {

  private val log: Logger = LoggerFactory("quasar.blobstore.gcs.GCSListServiceSpec")

  val AUTH_FILE="precog-ci-275718-9de94866bc77.json"
  val authCfgPath = Paths.get(getClass.getClassLoader.getResource(AUTH_FILE).toURI)
  val authCfgString = new String(Files.readAllBytes(authCfgPath), UTF_8)
  val authCfgJson: Json = Parse.parse(authCfgString) match {
    case Left(value) => Json.obj("malformed" := true)
    case Right(value) => value
  }

  val goodConfig = authCfgJson.as[ServiceAccountConfig].toOption.get

  def mkListService(cfg: ServiceAccountConfig, bucket: Bucket): Resource[IO, ListService[IO]] =
    GoogleCloudStorage.mkContainerClient[IO](cfg).map(client => GCSListService(log, client, bucket))

  def assertList(
      service: Resource[IO, ListService[IO]],
      prefixPath: PrefixPath,
      matcher: Matcher[List[BlobstorePath]])
      : IO[MatchResult[List[BlobstorePath]]] =
    service use { svc =>
      svc(prefixPath).flatMap {
        case Some(value) => value.compile.toList.map(_ must matcher)
        case None => ko("Unexpected None").asInstanceOf[MatchResult[List[BlobstorePath]]].pure[IO]
      }
    }

  "list service" >> {

    "root returns prefixpaths and blobpaths" >> {
      val expected = List[BlobstorePath](
        PrefixPath(List(PathElem("dir1"))),
        BlobPath(List(PathElem("extraSmallZips.data"))),
        PrefixPath(List(PathElem("prefix3"))),
        PrefixPath(List(PathElem("testdata"))))

      assertList(
        mkListService(goodConfig, Bucket("precog-test-bucket")),
        PrefixPath(List()),
        be_===(expected))
    }

    "existing leaf prefix returns blobpaths" >> {
      val expected = List[BlobstorePath](
        BlobPath(List(PathElem("dir1"), PathElem("dir2"), PathElem("dir3"), PathElem("flattenable.data"))))

      assertList(
        mkListService(goodConfig, Bucket("precog-test-bucket")),
        PrefixPath(List(PathElem("dir1"), PathElem("dir2"), PathElem("dir3")) ),
        be_===(expected))
    }

    // returns json where "items" is incomplete and needs data from "prefixes"" too
    "existing root prefix returns prefixpaths and blobpaths" >> {
      val expected = List[BlobstorePath](
        PrefixPath(List(PathElem("dir1"))),
        BlobPath(List(PathElem("extraSmallZips.data"))),
        PrefixPath(List(PathElem("prefix3"))),
        PrefixPath(List(PathElem("testdata"))))

      assertList(
        mkListService(goodConfig, Bucket("precog-test-bucket")),
        PrefixPath(List()),
        be_===(expected))
    }

    "existing non-leaf prefix returns prefixpaths and blobpaths" >> {
      val expected = List[BlobstorePath](
        PrefixPath(List(PathElem("dir1"), PathElem("dir2"))),
        BlobPath(List(PathElem("dir1"), PathElem("lines.json"))))

      assertList(
        mkListService(goodConfig, Bucket("precog-test-bucket")),
        PrefixPath(List(PathElem("dir1"))),
        be_===(expected))
    }

    "non-existing prefix returns empty list" >> {
      val expected = List[BlobstorePath]()

      assertList(
        mkListService(goodConfig, Bucket("precog-test-bucket")),
        PrefixPath(List()),
        be_===(expected))
    }

    // TODO: still fails not sure how to catch and match the thrown error
    // "non-existing bucket returns proper error " >> {
    //   assertList(
    //    mkListService(goodConfig, Bucket("non-existing-bucket")),
    //    PrefixPath(List(PathElem("does"), PathElem("not"), PathElem("exist"))),
    //    throwA[GCSAccessError])
    // }
  }

  "pagination tests" >> {
    "pagination works" >> {
      val expected = List[BlobstorePath]()
      assertList(
        mkListService(goodConfig, Bucket("precog-pagination-test-bucket")),
        PrefixPath(List(PathElem("foo"))),
        be_===(expected))
      //true must_=== false
    }
  }
}
