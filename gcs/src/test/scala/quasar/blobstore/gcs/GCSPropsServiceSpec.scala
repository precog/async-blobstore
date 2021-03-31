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

import quasar.blobstore.gcs.GCSFileProperties._
import quasar.blobstore.services.PropsService
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

object GCSPropsServiceSpec extends Specification with CatsIO {

  private val log: Logger = LoggerFactory("quasar.blobstore.gcs.GCSPropsServiceSpec")

  val AUTH_FILE="precog-ci-275718-9de94866bc77.json"
  val authCfgPath = Paths.get(getClass.getClassLoader.getResource(AUTH_FILE).toURI)
  val authCfgString = new String(Files.readAllBytes(authCfgPath), UTF_8)
  val authCfgJson: Json = Parse.parse(authCfgString) match {
    case Left(value) => Json.obj("malformed" := true)
    case Right(value) => value
  }

  val googleAuthCfg = Json.obj("authCfg" := authCfgJson)
  val goodConfig = googleAuthCfg.as[GoogleAuthConfig].toOption.get

  def mkPropsService(cfg: GoogleAuthConfig, bucket: Bucket): Resource[IO, PropsService[IO, GCSFileProperties]] =
    GoogleCloudStorage.mkContainerClient[IO](cfg).map(client => GCSPropsService(log, client, bucket))

  def assertProps(
      service: Resource[IO, PropsService[IO, GCSFileProperties]],
      prefixPath: BlobPath,
      matcher: Matcher[GCSFileProperties])
      : IO[MatchResult[GCSFileProperties]] =
    service use { svc =>
      svc(prefixPath).flatMap {
        case Some(value) => IO(value must matcher)
        case None => ko("Unexpected None").asInstanceOf[MatchResult[GCSFileProperties]].pure[IO]
      }
    }

  "props service" >> {
    "existing file returns correct props" >> {
      val bucketName = "bucket-8168b20d-a6f0-427f-a21b-232a2e8742e1"
      val expected = GCSFileProperties.GCSFileProperties("zips.csv", bucketName, 1188150, "2020-08-19T16:52:01.093Z")
      assertProps(
        mkPropsService(goodConfig, Bucket(bucketName)),
        BlobPath(List(PathElem("zips.csv"))),
        be_===(expected))
    }

    //TODO: match on a throwable
    "non-existing file returns error" >> {
      val bucketName = "bucket-8168b20d-a6f0-427f-a21b-232a2e8742e1"
      val nonfile = "i-am-not-a-real-file.json"
      val expected = GCSError.GCSAccessError("No such object: " + bucketName +"/" + nonfile)

      assertProps(
        mkPropsService(goodConfig, Bucket(bucketName)),
        BlobPath(List(PathElem(nonfile))),
        throwA(expected))
    }

  }

}
