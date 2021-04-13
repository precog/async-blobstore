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

import quasar.blobstore.gcs.GCSFileProperties._
import quasar.blobstore.services.PropsService
import quasar.blobstore.paths.BlobPath
import quasar.blobstore.paths.PathElem

import cats.effect.{IO, Resource}
import cats.effect.testing.specs2.CatsIO

import org.slf4s.Logger
import org.slf4s.LoggerFactory
import org.specs2.matcher.{Matcher, MatchResult}
import org.specs2.mutable.Specification

object GCSPropsServiceSpec extends Specification with CatsIO {

  private val log: Logger = LoggerFactory("quasar.blobstore.gcs.GCSPropsServiceSpec")

  val AUTH_FILE="precog-ci-275718-9de94866bc77.json"
  val BAD_AUTH_FILE="bad-auth-file.json"
  val goodConfig = common.getAuthFileAsJson(AUTH_FILE)
  val badConfig = common.getAuthFileAsJson(BAD_AUTH_FILE)
  val bucketName = "precog-test-bucket"

  def mkService(cfg: ServiceAccountConfig, bucket: Bucket): Resource[IO, PropsService[IO, GCSFileProperties]] =
    GoogleCloudStorage.mkContainerClient[IO](cfg).map(client => GCSPropsService(log, client, bucket))

  def assertProps[A](
      service: Resource[IO, PropsService[IO, A]],
      blobPath: BlobPath,
      matcher: Matcher[Option[A]])
      : IO[MatchResult[Option[A]]] =
    service use { svc =>
      svc(blobPath).flatMap(a => IO(a must matcher))
    }

  "props service" >> {
    "existing file returns correct props" >> {
      assertProps[GCSFileProperties](
        mkService(goodConfig, Bucket(bucketName)),
        BlobPath(List(PathElem("extraSmallZips.data"))),
        beSome)
    }

    "existing blobpath returns some (2)" >> {
      assertProps[GCSFileProperties](
        mkService(goodConfig, Bucket(bucketName)),
        BlobPath(List(PathElem("prefix3"), PathElem("subprefix5"), PathElem("cars2.data"))),
        beSome)
    }

    "non-existing file returns none" >> {
      val nonfile = "i-am-not-a-real-file.json"
      assertProps[GCSFileProperties](
        mkService(goodConfig, Bucket(bucketName)),
        BlobPath(List(PathElem(nonfile))),
        beNone)
    }

    "blobpath that only exists as prefix returns none" >> {
      assertProps[GCSFileProperties](
        mkService(goodConfig, Bucket(bucketName)),
        BlobPath(List(PathElem("testdata"))),
        beNone)
    }

    "nil blobpath returns none" >> {
      assertProps[GCSFileProperties](
        mkService(goodConfig, Bucket(bucketName)),
        BlobPath(Nil),
        beNone)
    }

    "empty string blobpath returns none" >> {
      assertProps[GCSFileProperties](
        mkService(goodConfig, Bucket(bucketName)),
        BlobPath(List(PathElem(""))),
        beNone)
    }

    "blobpath in non existing container returns none" >> {
      assertProps[GCSFileProperties](
        mkService(goodConfig, Bucket("non-existing-bucket")),
        BlobPath(List(PathElem("something"))),
        beNone)
    }

    "invalid config returns none" >> {
      assertProps[GCSFileProperties](
        mkService(badConfig, Bucket(bucketName)),
        BlobPath(List(PathElem("something"))),
        beNone)
    }

  }

}
